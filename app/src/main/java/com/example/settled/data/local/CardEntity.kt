package com.example.settled.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val bankName: String,
    val cardName: String,
    val lastFourDigits: String,
    val statementDate: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)
