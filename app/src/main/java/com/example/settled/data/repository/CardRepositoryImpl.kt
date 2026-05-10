package com.example.settled.data.repository

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.example.settled.core.Result
import com.example.settled.data.auth.AuthManager
import com.example.settled.data.local.CardDao
import com.example.settled.data.local.CardEntity
import com.example.settled.data.local.PaymentLogEntity
import com.example.settled.data.remote.FirestoreDataSource
import com.example.settled.domain.model.Card
import com.example.settled.domain.model.CardStatus
import com.example.settled.domain.model.PaymentLog
import com.example.settled.domain.repository.CardRepository
import com.example.settled.ui.widget.SettledDashboardWidget
import com.example.settled.ui.widget.SettledMiniWidget
import com.example.settled.ui.widget.SettledStatusWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class CardRepositoryImpl @Inject constructor(
    private val cardDao: CardDao,
    private val firestoreDataSource: FirestoreDataSource,
    private val authManager: AuthManager,
    @ApplicationContext private val context: Context
) : CardRepository {

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun syncAsync(block: suspend (uid: String) -> Unit) {
        val uid = authManager.currentUid() ?: return
        syncScope.launch { runCatching { block(uid) } }
    }

    suspend fun initialSyncFromFirestore() {
        val uid = authManager.currentUid() ?: return
        val isEmpty = (getAllCards().first() as? Result.Success)?.data?.isEmpty() ?: true
        if (!isEmpty) return
        val remoteCards = firestoreDataSource.fetchAllCards(uid)
        val remoteLogs  = firestoreDataSource.fetchAllLogs(uid)
        remoteCards.forEach { cardDao.insertCard(it) }
        remoteLogs.forEach { cardDao.insertPaymentLogIgnore(it) }
    }

    private fun calculateStatus(entity: CardEntity, logs: List<PaymentLogEntity>): Card {
        val now = LocalDate.now()
        val currentMonth = now.monthValue
        val currentYear = now.year
        
        // Active statement is either this month's or last month's
        val safeStatementDay = entity.statementDay.coerceAtMost(YearMonth.of(currentYear, currentMonth).lengthOfMonth())
        val statementThisMonth = LocalDate.of(currentYear, currentMonth, safeStatementDay)
        
        val activeStatementDate: LocalDate
        val activeDueDate: LocalDate
        
        if (now.isBefore(statementThisMonth)) {
            // We are before this month's statement, so the previous month's statement is the one we are tracking
            val prevMonthDate = now.minusMonths(1)
            val prevMonthYear = prevMonthDate.year
            val prevMonthValue = prevMonthDate.monthValue
            val prevSafeDay = entity.statementDay.coerceAtMost(YearMonth.of(prevMonthYear, prevMonthValue).lengthOfMonth())
            activeStatementDate = LocalDate.of(prevMonthYear, prevMonthValue, prevSafeDay)
        } else {
            // We are on or after this month's statement
            activeStatementDate = statementThisMonth
        }

        // Calculate active due date based on manual dueDay
        // If dueDay < statementDay, the due date is in the month following the statement
        if (entity.dueDay >= entity.statementDay) {
            // Due date is in the same month as statement
            val safeDueDay = entity.dueDay.coerceAtMost(YearMonth.of(activeStatementDate.year, activeStatementDate.monthValue).lengthOfMonth())
            activeDueDate = LocalDate.of(activeStatementDate.year, activeStatementDate.monthValue, safeDueDay)
        } else {
            // Due date is in the month following the statement
            val nextMonth = activeStatementDate.plusMonths(1)
            val safeDueDay = entity.dueDay.coerceAtMost(YearMonth.of(nextMonth.year, nextMonth.monthValue).lengthOfMonth())
            activeDueDate = LocalDate.of(nextMonth.year, nextMonth.monthValue, safeDueDay)
        }

        // Check if any payment log exists for the cycle defined by activeStatementDate
        val latestLog = logs.filter { 
            it.cardId == entity.id && 
            it.cycleMonth == activeStatementDate.monthValue && 
            it.cycleYear == activeStatementDate.year 
        }.maxByOrNull { it.timestamp }
        
        val daysUntilDue = ChronoUnit.DAYS.between(now, activeDueDate).toInt()
        
        val status = if (latestLog != null) {
            CardStatus.PAID
        } else if (now.isAfter(activeDueDate)) {
            CardStatus.OVERDUE
        } else {
            CardStatus.DUE
        }
        
        val minimumDue = latestLog?.type == "MINIMUM"

        val lastPaymentInfo = latestLog?.let {
            PaymentLog(
                id = it.id,
                type = it.type,
                platform = it.platform,
                timestamp = it.timestamp,
                cycleMonth = it.cycleMonth,
                cycleYear = it.cycleYear
            )
        }

        return Card(
            id = entity.id,
            bankName = entity.bankName,
            cardName = entity.cardName,
            lastFourDigits = entity.lastFourDigits,
            statementDay = entity.statementDay,
            dueDay = entity.dueDay,
            status = status,
            minimumDueLastCycle = minimumDue,
            daysUntilDue = daysUntilDue,
            activeStatementDate = activeStatementDate,
            activeDueDate = activeDueDate,
            isLocked = false,
            lastPaymentInfo = lastPaymentInfo
        )
    }

    override fun getAllCards(): Flow<Result<List<Card>>> {
        return combine(cardDao.getAllCards(), cardDao.getAllPaymentLogs()) { entities, logs ->
            try {
                val domainCards = entities.map { entity -> calculateStatus(entity, logs) }
                // Dynamic sorting: OVERDUE first, then DUE by nearest due date, then PAID.
                val sortedCards = domainCards.sortedWith(
                    compareBy<Card> { it.status.ordinal }.thenBy { it.daysUntilDue }
                )
                Result.Success(sortedCards)
            } catch (e: Exception) {
                Result.Error("Failed to map cards", e)
            }
        }
    }

    override suspend fun addCard(bankName: String, cardName: String, lastFourDigits: String, statementDay: Int, dueDay: Int): Result<Unit> {
        return try {
            val existing = cardDao.getCardByVariant(bankName, cardName)
            if (existing != null) {
                return Result.Error("You already have this $cardName card from $bankName.")
            }
            
            val entity = CardEntity(
                bankName = bankName,
                cardName = cardName,
                lastFourDigits = lastFourDigits,
                statementDay = statementDay,
                dueDay = dueDay
            )
            cardDao.insertCard(entity)
            syncAsync { uid -> firestoreDataSource.syncCard(uid, entity) }
            refreshWidgets()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to add card", e)
        }
    }

    override suspend fun deleteCard(cardId: String): Result<Unit> {
        return try {
            cardDao.deleteCard(cardId)
            syncAsync { uid -> firestoreDataSource.deleteCard(uid, cardId) }
            refreshWidgets()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to delete card", e)
        }
    }

    override fun getCardDetails(cardId: String): Flow<Result<Card>> {
        return combine(cardDao.getCardById(cardId), cardDao.getLogsForCard(cardId)) { entity, logs ->
            if (entity == null) {
                Result.Error("Card not found")
            } else {
                Result.Success(calculateStatus(entity, logs))
            }
        }
    }

    override fun getPaymentLogs(cardId: String): Flow<Result<List<PaymentLog>>> {
        return cardDao.getLogsForCard(cardId).map { entities ->
            try {
                val logs = entities.map { 
                    PaymentLog(
                        id = it.id,
                        type = it.type,
                        platform = it.platform,
                        timestamp = it.timestamp,
                        cycleMonth = it.cycleMonth,
                        cycleYear = it.cycleYear
                    )
                }
                Result.Success(logs)
            } catch (e: Exception) {
                Result.Error("Failed to map logs", e)
            }
        }
    }

    override suspend fun logPayment(cardId: String, amountType: String, platform: String, date: Long): Result<Unit> {
        return try {
            val paymentDate = java.time.Instant.ofEpochMilli(date)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            cardDao.deleteLogForCycle(cardId, paymentDate.monthValue, paymentDate.year)
            val logEntity = PaymentLogEntity(
                cardId = cardId,
                type = amountType,
                platform = platform,
                timestamp = date,
                cycleMonth = paymentDate.monthValue,
                cycleYear = paymentDate.year
            )
            cardDao.insertPaymentLog(logEntity)
            syncAsync { uid ->
                firestoreDataSource.deleteLogForCycle(uid, cardId, paymentDate.monthValue, paymentDate.year)
                firestoreDataSource.syncPaymentLog(uid, logEntity)
            }
            refreshWidgets()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to log payment", e)
        }
    }

    private suspend fun refreshWidgets() {
        val manager = GlanceAppWidgetManager(context)
        listOf(
            SettledMiniWidget()     to manager.getGlanceIds(SettledMiniWidget::class.java),
            SettledStatusWidget()   to manager.getGlanceIds(SettledStatusWidget::class.java),
            SettledDashboardWidget() to manager.getGlanceIds(SettledDashboardWidget::class.java)
        ).forEach { (widget, ids) ->
            ids.forEach { id -> widget.update(context, id) }
        }
    }

    override suspend fun insertDummyData() {
        val dummies = listOf(
            CardEntity(id = "test-icici-amazon", bankName = "ICICI", cardName = "Amazon Pay", lastFourDigits = "3456", statementDay = 26, dueDay = 15),
            CardEntity(id = "test-sbi-cashback", bankName = "SBI", cardName = "Cashback Card", lastFourDigits = "9012", statementDay = 22, dueDay = 10)
        )
        dummies.forEach { cardDao.insertCard(it) }
    }
}
