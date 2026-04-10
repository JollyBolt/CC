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
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import javax.inject.Inject

class CardRepositoryImpl @Inject constructor(
    private val cardDao: CardDao
) : CardRepository {

    override fun getAllCards(): Flow<Result<List<Card>>> {
        return cardDao.getAllCards().map { entities ->
            try {
                val domainCards = entities.map { entity ->
                    // Simplified status computation for the UI skeleton phase
                    val simulatedStatus = when (entity.cardName) {
                        "HDFC Regalia" -> CardStatus.OVERDUE
                        "Axis Magnus" -> CardStatus.DUE_SOON
                        "ICICI Amazon" -> CardStatus.PAID
                        else -> CardStatus.PENDING
                    }
                    
                    val minimumDue = entity.cardName == "Axis Magnus"

                    Card(
                        id = entity.id,
                        bankName = entity.bankName,
                        cardName = entity.cardName,
                        lastFourDigits = entity.lastFourDigits,
                        statementDate = entity.statementDate,
                        dueDate = (entity.statementDate + 20) % 31,
                        status = simulatedStatus,
                        minimumDueLastCycle = minimumDue,
                        daysUntilDue = when (simulatedStatus) {
                            CardStatus.OVERDUE -> -3
                            CardStatus.DUE_SOON -> 1
                            else -> 12
                        },
                        isLocked = false,
                        lastPaymentInfo = null
                    )
                }
                Result.Success(domainCards)
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

    override fun getCardDetails(cardId: String): Flow<Result<Card>> {
        return cardDao.getCardById(cardId).map { entity ->
            if (entity == null) {
                Result.Error("Card not found")
            } else {
                val simulatedStatus = when (entity.cardName) {
                    "HDFC Regalia" -> CardStatus.OVERDUE
                    "Axis Magnus" -> CardStatus.DUE_SOON
                    "ICICI Amazon" -> CardStatus.PAID
                    else -> CardStatus.PENDING
                }
                val minimumDue = entity.cardName == "Axis Magnus"

                val domainCard = Card(
                    id = entity.id,
                    bankName = entity.bankName,
                    cardName = entity.cardName,
                    lastFourDigits = entity.lastFourDigits,
                    statementDate = entity.statementDate,
                    dueDate = (entity.statementDate + 20) % 31,
                    status = simulatedStatus,
                    minimumDueLastCycle = minimumDue,
                    daysUntilDue = when (simulatedStatus) {
                        CardStatus.OVERDUE -> -3
                        CardStatus.DUE_SOON -> 1
                        else -> 12
                    },
                    isLocked = false,
                    lastPaymentInfo = null
                )
                Result.Success(domainCard)
            }
        }
    }

    override fun getPaymentLogs(cardId: String): Flow<Result<List<PaymentLog>>> {
        return cardDao.getLogsForCard(cardId).map { logs ->
            Result.Success(
                logs.map {
                    PaymentLog(
                        id = it.id,
                        type = it.type,
                        platform = it.platform,
                        timestamp = it.timestamp,
                        cycleMonth = it.cycleMonth,
                        cycleYear = it.cycleYear
                    )
                }
            )
        }
    }

    override suspend fun logPayment(cardId: String, amountType: String, platform: String): Result<Unit> {
        return try {
            val now = YearMonth.now()
            val logEntity = PaymentLogEntity(
                cardId = cardId,
                type = amountType,
                platform = platform,
                timestamp = System.currentTimeMillis(),
                cycleMonth = now.monthValue,
                cycleYear = now.year
            )
            cardDao.insertPaymentLog(logEntity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to log payment", e)
        }
    }

    override suspend fun insertDummyData() {
        val dummies = listOf(
            CardEntity(bankName = "HDFC", cardName = "HDFC Regalia", lastFourDigits = "1234", statementDate = 10),
            CardEntity(bankName = "Axis", cardName = "Axis Magnus", lastFourDigits = "5678", statementDate = 15),
            CardEntity(bankName = "SBI", cardName = "SBI Elite", lastFourDigits = "9012", statementDate = 20),
            CardEntity(bankName = "ICICI", cardName = "ICICI Amazon", lastFourDigits = "3456", statementDate = 2)
        )
        dummies.forEach { cardDao.insertCard(it) }
    }
}
