package com.example.settled.data.repository

import com.example.settled.core.Result
import com.example.settled.data.local.CardDao
import com.example.settled.data.local.CardEntity
import com.example.settled.data.local.PaymentLogEntity
import com.example.settled.domain.model.Card
import com.example.settled.domain.model.CardStatus
import com.example.settled.domain.model.PaymentLog
import com.example.settled.domain.repository.CardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class CardRepositoryImpl @Inject constructor(
    private val cardDao: CardDao
) : CardRepository {

    private fun calculateStatus(entity: CardEntity, logs: List<PaymentLogEntity>): Card {
        val now = LocalDate.now()
        val currentMonth = now.monthValue
        val currentYear = now.year
        
        // Check if any payment log exists for the current cycle
        val latestLog = logs.filter { it.cardId == entity.id && it.cycleMonth == currentMonth && it.cycleYear == currentYear }
            .maxByOrNull { it.timestamp }
            
        // Calculate due date assuming 20 days after statement date
        val safeStatementDay = entity.statementDate.coerceAtMost(now.lengthOfMonth())
        val statementDateThisMonth = LocalDate.of(currentYear, currentMonth, safeStatementDay)
        val dueDateThisMonth = statementDateThisMonth.plusDays(20)
        
        val dueDate = if (now.isBefore(statementDateThisMonth)) {
            // Previous month's statement governs the active due date right now
            val prevMonth = now.minusMonths(1)
            val prevSafeDay = entity.statementDate.coerceAtMost(prevMonth.lengthOfMonth())
            val prevMonthStatement = LocalDate.of(prevMonth.year, prevMonth.monthValue, prevSafeDay)
            prevMonthStatement.plusDays(20)
        } else {
            dueDateThisMonth
        }
        
        val daysUntilDue = ChronoUnit.DAYS.between(now, dueDate).toInt()
        
        val status = if (latestLog != null) {
            CardStatus.PAID
        } else {
            if (daysUntilDue <= 2) {
                CardStatus.SOON
            } else {
                CardStatus.DUE
            }
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
            statementDate = entity.statementDate,
            dueDate = dueDate.dayOfMonth,
            status = status,
            minimumDueLastCycle = minimumDue,
            daysUntilDue = daysUntilDue,
            isLocked = false,
            lastPaymentInfo = lastPaymentInfo
        )
    }

    override fun getAllCards(): Flow<Result<List<Card>>> {
        return combine(cardDao.getAllCards(), cardDao.getAllPaymentLogs()) { entities, logs ->
            try {
                val domainCards = entities.map { entity -> calculateStatus(entity, logs) }
                // Dynamic sorting: SOON forms cluster top, then nearest due date.
                val sortedCards = domainCards.sortedWith(
                    compareBy<Card> { it.status.ordinal }.thenBy { it.daysUntilDue }
                )
                Result.Success(sortedCards)
            } catch (e: Exception) {
                Result.Error("Failed to map cards", e)
            }
        }
    }

    override suspend fun addCard(bankName: String, cardName: String, lastFourDigits: String, statementDate: Int): Result<Unit> {
        return try {
            val entity = CardEntity(
                bankName = bankName,
                cardName = cardName,
                lastFourDigits = lastFourDigits,
                statementDate = statementDate
            )
            cardDao.insertCard(entity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to add card", e)
        }
    }

    override suspend fun deleteCard(cardId: String): Result<Unit> {
        return try {
            cardDao.deleteCard(cardId)
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
            val logEntity = PaymentLogEntity(
                cardId = cardId,
                type = amountType,
                platform = platform,
                timestamp = date,
                cycleMonth = paymentDate.monthValue,
                cycleYear = paymentDate.year
            )
            cardDao.insertPaymentLog(logEntity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to log payment", e)
        }
    }

    override suspend fun insertDummyData() {
        val dummies = listOf(
            CardEntity(id = "test-icici-amazon", bankName = "ICICI", cardName = "Amazon Pay", lastFourDigits = "3456", statementDate = 26),
            CardEntity(id = "test-sbi-cashback", bankName = "SBI", cardName = "Cashback Card", lastFourDigits = "9012", statementDate = 22)
        )
        dummies.forEach { cardDao.insertCard(it) }
    }
}
