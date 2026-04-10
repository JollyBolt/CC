# Data Layer & Worker Guidelines

## 1. Room Rules
- Every table must have a UUID `TEXT PRIMARY KEY` generated on the device — never use auto-increment integers
- Always define `createdAt` and `updatedAt` as `Long` (epoch milliseconds) on every entity
- Use `@TypeConverter` for enums — store them as strings, not integers
- Never expose `Entity` classes above the repository layer — always map to domain models
- DAOs must return `Flow<T>` for queries the UI observes, and `suspend` functions for writes

```kotlin
// ✅ Correct — UUID primary key, timestamps, string enum
@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val status: String = CardStatus.PENDING.name,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// ✅ Correct DAO
@Dao
interface CardDao {
    @Query("SELECT * FROM cards WHERE userId = :userId")
    fun getCards(userId: String): Flow<List<CardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CardEntity)
}
```

## 2. Firestore Rules
- All Firestore paths must be scoped to the user: `users/{userId}/cards/{cardId}`
- Never read from Firestore on the main thread
- Always wrap Firestore calls in `try/catch` — sync failures must never crash the app
- Firestore writes are fire-and-forget after Room is written — do not await them in the UI flow
- Apply conflict resolution before writing remote data to Room

```kotlin
// ✅ Correct — Room first, Firestore second, error swallowed
suspend fun markAsPaid(cardId: String, type: PaymentType, platform: String) {
    cardDao.updateStatus(cardId, CardStatus.PAID)        // Room first
    try {
        cardRemoteDataSource.updateStatus(cardId, ...)   // Firestore second
    } catch (e: Exception) {
        // Log to Crashlytics, do not rethrow
        Crashlytics.recordException(e)
    }
}
```

## 3. WorkManager Rules
- Every scheduled job is a class extending `CoroutineWorker`, never `Worker`
- Name pattern: `{Action}Worker` (e.g., `CycleResetWorker`, `DueSoonWorker`)
- Workers read from and write to Room only — never directly to Firestore
- After a Worker updates Room, it must call `GlanceAppWidgetManager.updateAll()` to refresh the widget
- Always set retry policy with exponential backoff on Workers
- Workers must be idempotent — running the same Worker twice must produce the same result
