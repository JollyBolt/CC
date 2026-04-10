package com.example.settled.domain.repository

import com.example.settled.core.Result
import com.example.settled.domain.model.Card
import kotlinx.coroutines.flow.Flow

interface CardRepository {
    fun getAllCards(): Flow<Result<List<Card>>>
    suspend fun addCard(bankName: String, cardName: String, lastFourDigits: String, statementDate: Int): Result<Unit>
    suspend fun insertDummyData() // For UI testing purposes
}
