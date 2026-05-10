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

    override suspend fun initialSyncFromFirestore() {
        val uid = authManager.currentUid() ?: return
        val isEmpty = (getAllCards().first() as? Result.Success)?.data?.isEmpty() ?: true
        if (!isEmpty) return
        val remoteCards = firestoreDataSource.fetchAllCards(uid)
        val remoteLogs  = firestoreDataSource.fetchAllLogs(uid)
        remoteCards.forEach { cardDao.insertCard(it) }
        remoteLogs.forEach { cardDao.insertPaymentLogIgnore(it) }
    }


    override fun getAllCards(): Flow<Result<List<Card>>> {
        return combine(cardDao.getAllCards(), cardDao.getAllPaymentLogs()) { entities, logs ->
            try {
                val domainCards = entities.map { entity -> calculateCardStatus(entity, logs) }
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
                Result.Success(calculateCardStatus(entity, logs))
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
            val cardEntity = cardDao.getCardById(cardId).first()
                ?: return Result.Error("Card not found")
            // Key the log by the active billing cycle, not the payment date
            val activeCycle = calculateCardStatus(cardEntity, emptyList()).activeStatementDate
            cardDao.deleteLogForCycle(cardId, activeCycle.monthValue, activeCycle.year)
            val logEntity = PaymentLogEntity(
                cardId = cardId,
                type = amountType,
                platform = platform,
                timestamp = date,
                cycleMonth = activeCycle.monthValue,
                cycleYear = activeCycle.year
            )
            cardDao.insertPaymentLog(logEntity)
            syncAsync { uid ->
                firestoreDataSource.deleteLogForCycle(uid, cardId, activeCycle.monthValue, activeCycle.year)
                firestoreDataSource.syncPaymentLog(uid, logEntity)
            }
            refreshWidgets()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to log payment", e)
        }
    }

    override suspend fun clearAllData(): Result<Unit> {
        return try {
            cardDao.deleteAllCards()
            cardDao.deleteAllPaymentLogs()
            syncAsync { uid -> firestoreDataSource.deleteAllUserData(uid) }
            refreshWidgets()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to clear data", e)
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
            // dueDay=5, statementDay=22 → active=Apr 22, due=May 5, today May 10 → OVERDUE
            CardEntity(id = "test-sbi-cashback", bankName = "SBI", cardName = "Cashback Card", lastFourDigits = "9012", statementDay = 22, dueDay = 5),
            // statementDay=1 → active=May 1, pre-seeded as PAID
            CardEntity(id = "test-induind-legend", bankName = "IndusInd Bank", cardName = "Legend", lastFourDigits = "7777", statementDay = 1, dueDay = 20)
        )
        dummies.forEach { cardDao.insertCard(it) }

        // Pre-seed IndusInd as PAID for the current cycle (May 2026)
        cardDao.insertPaymentLog(
            PaymentLogEntity(
                cardId = "test-induind-legend",
                type = "FULL",
                platform = "CRED",
                timestamp = System.currentTimeMillis(),
                cycleMonth = 5,
                cycleYear = 2026
            )
        )
    }
}
