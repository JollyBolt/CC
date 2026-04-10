package com.example.settled.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM cards WHERE isDeleted = 0 ORDER BY createdAt ASC")
    fun getAllCards(): Flow<List<CardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CardEntity)

    @Query("UPDATE cards SET isDeleted = 1 WHERE id = :cardId")
    suspend fun deleteCard(cardId: String)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentLog(log: PaymentLogEntity)
    
    @Query("SELECT * FROM payment_logs WHERE cardId = :cardId ORDER BY timestamp DESC")
    fun getLogsForCard(cardId: String): Flow<List<PaymentLogEntity>>
}
