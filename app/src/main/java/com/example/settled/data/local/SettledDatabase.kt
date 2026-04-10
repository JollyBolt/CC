package com.example.settled.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CardEntity::class, PaymentLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SettledDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
}
