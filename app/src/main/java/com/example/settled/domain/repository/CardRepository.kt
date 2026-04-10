package com.example.settled.domain.repository

import com.example.settled.core.Result
import com.example.settled.domain.model.Card
import com.example.settled.domain.model.PaymentLog
import kotlinx.coroutines.flow.Flow

interface CardRepository {
    fun getAllCards(): Flow<Result<List<Card>>>
    fun getCardDetails(cardId: String): Flow<Result<Card>>
    fun getPaymentLogs(cardId: String): Flow<Result<List<PaymentLog>>>
    suspend fun addCard(bankName: String, cardName: String, lastFourDigits: String, statementDate: Int): Result<Unit>
    suspend fun logPayment(cardId: String, amountType: String, platform: String): Result<Unit>
    suspend fun insertDummyData() // For UI testing purposes
}
