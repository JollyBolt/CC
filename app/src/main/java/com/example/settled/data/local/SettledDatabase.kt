package com.example.settled.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CardEntity::class, PaymentLogEntity::class],
    version = 3,
    exportSchema = false
)
abstract class SettledDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Rename statementDate to statementDay
                db.execSQL("ALTER TABLE cards RENAME COLUMN statementDate TO statementDay")
                
                // Add dueDay column
                db.execSQL("ALTER TABLE cards ADD COLUMN dueDay INTEGER NOT NULL DEFAULT 0")
                
                // Migrate existing data: set dueDay = statementDay + 20 (capped at 28)
                db.execSQL("""
                    UPDATE cards 
                    SET dueDay = CASE 
                        WHEN (statementDay + 20) > 28 THEN 28 
                        ELSE (statementDay + 20) 
                    END
                """.trimIndent())
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add unique index on bankName and cardName
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_cards_bankName_cardName ON cards(bankName, cardName)")
            }
        }
    }
}
