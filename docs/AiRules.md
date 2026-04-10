# AI Rules: Settled

This file defines how AI coding assistants should generate, suggest, and modify code for the Settled Android app. Always read this file before writing any code.

---

## 1. Project Identity

- **App name:** Settled
- **Platform:** Android only (no cross-platform, no web)
- **Language:** Kotlin
- **UI framework:** Jetpack Compose
- **Architecture:** MVVM + Repository Pattern
- **Database:** Room (local) + Firebase Firestore (cloud sync)
- **Widget:** Jetpack Glance

The app is local-first. All reads come from Room. Firestore is a sync target, never the primary data source.

---

## 2. Architecture Rules

### Non-negotiable structure
Every feature must follow this exact layer separation. Do not skip or merge layers.

```
UI (Composable screens)
    ↓ events / ↑ state
ViewModel (state holder)
    ↓ calls
Repository (data orchestration)
    ↓               ↓
Room (local)    Firestore SDK (remote)
```

### Rules per layer

**UI Layer:**
- Composables are stateless — they only receive state and emit events
- Never call repository or database directly from a Composable
- Never use `mutableStateOf` for data that comes from the database — use `StateFlow` collected via `collectAsStateWithLifecycle()`
- One screen = one top-level `@Composable` function named `{ScreenName}Screen`
- Break large screens into smaller private composables within the same file

**ViewModel Layer:**
- One ViewModel per screen
- Name pattern: `{ScreenName}ViewModel`
- Expose a single `uiState: StateFlow<{ScreenName}UiState>` to the UI
- Define a sealed class or data class `{ScreenName}UiState` for each ViewModel
- Handle all user events through a single `onEvent(event: {ScreenName}Event)` function
- Define a sealed class `{ScreenName}Event` for all user actions
- Never reference Android `Context` inside a ViewModel
- Never import anything from the UI layer

**Repository Layer:**
- One repository per domain entity: `CardRepository`, `PaymentLogRepository`
- Always write to Room first, then Firestore — never the other way around
- Return `Flow<T>` for data the UI observes, `Result<T>` for one-shot operations
- Handle Firestore errors silently — never crash because of a sync failure
- Apply "Paid Always Wins" conflict logic here, not in the ViewModel or UI

**Data Layer:**
- Room DAOs are interfaces, never classes
- Name pattern: `{Entity}Dao`
- Firestore data sources are classes named `{Entity}RemoteDataSource`
- Never expose Room entities to the ViewModel — map them to domain models first

---

## 3. Naming Conventions

| Element | Convention | Example |
|---|---|---|
| Composable functions | PascalCase | `CardListItem()` |
| ViewModel | PascalCase + ViewModel | `HomeViewModel` |
| UiState | PascalCase + UiState | `HomeUiState` |
| Event sealed class | PascalCase + Event | `HomeEvent` |
| Repository | PascalCase + Repository | `CardRepository` |
| Room Entity | PascalCase + Entity | `CardEntity` |
| Room DAO | PascalCase + Dao | `CardDao` |
| Firestore data source | PascalCase + RemoteDataSource | `CardRemoteDataSource` |
| Domain model | PascalCase, no suffix | `Card`, `PaymentLog` |
| Worker | PascalCase + Worker | `CycleResetWorker` |
| Constants | SCREAMING_SNAKE_CASE | `DUE_SOON_THRESHOLD_HOURS` |
| Private composables | PascalCase, prefixed with screen name | `HomeCardItem()` |

---

## 4. Kotlin Code Style

- Use `data class` for all models and state objects
- Prefer `val` over `var` everywhere — only use `var` when mutation is unavoidable
- Use Kotlin coroutines and `Flow` for all async operations — no callbacks, no RxJava
- Use `suspend` functions for one-shot async operations in repositories
- Use `viewModelScope` for coroutines launched in ViewModels
- Never use `GlobalScope`
- Prefer `when` over `if-else` chains for state/enum handling
- Use extension functions to keep code readable, but don't overuse them
- Null safety: avoid `!!` at all costs — use `?.`, `?:`, or `requireNotNull()` with a message
- Use `sealed class` for events and results, `enum class` for fixed states like card status

```kotlin
// ✅ Correct — sealed class for UI events
sealed class HomeEvent {
    data class MarkAsPaid(val cardId: String) : HomeEvent()
    data object RefreshCards : HomeEvent()
}

// ✅ Correct — enum for status
enum class CardStatus { PAID, PENDING, DUE_SOON, OVERDUE }

// ❌ Wrong — raw strings for status
val status = "PAID"
```

---

## 5. Jetpack Compose Rules

- Always use `MaterialTheme` tokens for colours, typography, and spacing — no hardcoded hex values or raw `dp` values outside the theme file
- Define the app's colour palette and typography in a single `Theme.kt` file
- Use `Modifier` as the first optional parameter after required parameters in every composable
- Always pass `modifier = Modifier` as default — never apply modifiers inside composables that should be controlled by the caller
- Use `LazyColumn` / `LazyRow` for any list that could exceed 5 items
- Avoid business logic inside composables — move it to the ViewModel
- Preview every non-trivial composable with `@Preview`

```kotlin
// ✅ Correct composable signature
@Composable
fun CardListItem(
    card: Card,
    onMarkPaid: () -> Unit,
    modifier: Modifier = Modifier
) { ... }

// ❌ Wrong — modifier not exposed, hardcoded colour
@Composable
fun CardListItem(card: Card) {
    Box(modifier = Modifier.background(Color(0xFF1A73E8))) { ... }
}
```

---

## 6. Room Rules

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

---

## 7. Firestore Rules

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

---

## 8. WorkManager Rules

- Every scheduled job is a class extending `CoroutineWorker`, never `Worker`
- Name pattern: `{Action}Worker` (e.g., `CycleResetWorker`, `DueSoonWorker`)
- Workers read from and write to Room only — never directly to Firestore
- After a Worker updates Room, it must call `GlanceAppWidgetManager.updateAll()` to refresh the widget
- Always set retry policy with exponential backoff on Workers
- Workers must be idempotent — running the same Worker twice must produce the same result

---

## 9. Jetpack Glance (Widget) Rules

- The widget reads from Room directly — it does not go through the ViewModel or Repository
- Never make network calls from a Glance composable
- Widget state is a simple data class passed to `GlanceAppWidget.provideGlance()`
- Always handle the case where Room returns an empty list gracefully — show an "Add a card" placeholder
- Free tier: render only the single most urgent card (priority: OVERDUE > DUE_SOON > PENDING > PAID)
- Pro tier: render all cards in a grid

---

## 10. Data & Privacy Rules

These are non-negotiable and must never be violated by generated code.

- **Never log full card numbers, CVV, or expiry** — not even in debug logs
- **Never send last four digits to analytics** — PostHog events must contain only card IDs (UUIDs)
- **Never request SMS, Email, Contacts, or Location permissions** — if generated code adds any of these to the manifest, remove them immediately
- **Never store sensitive data in SharedPreferences** — use `EncryptedSharedPreferences` for anything sensitive (auth tokens)
- The only card identifier ever stored or transmitted is the last 4 digits + bank name

---

## 11. Error Handling Rules

- Use a sealed `Result<T>` wrapper for repository operations that can fail
- Never show raw exception messages to the user — map errors to user-friendly strings
- Firestore errors are logged to Crashlytics and swallowed — they do not surface to the UI
- Room errors are critical — they should be logged and surfaced to the user with a generic error state
- WorkManager failures retry with exponential backoff — do not show notifications on failure

```kotlin
// ✅ Correct Result wrapper
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Result<Nothing>()
}
```

---

## 12. What NOT to Generate

The AI must never generate code that:

- Reads SMS or email content
- Requests any runtime permissions not already in the manifest
- Connects to any API not listed in the Tech Stack document
- Uses `XMLLayouts` or `ViewBinding` — Settled is Compose-only
- Uses `AsyncTask` — use coroutines
- Uses `LiveData` — use `StateFlow` and `Flow`
- Uses RxJava — use coroutines and Flow
- Hardcodes any user-facing string — all strings go in `strings.xml`
- Hardcodes colours, font sizes, or spacing values outside `Theme.kt`
- Adds any third-party library not already in the approved tech stack without flagging it first

---

## 13. File & Package Structure

```
com.settled.app/
│
├── ui/
│   ├── theme/                  ← Theme.kt, Color.kt, Type.kt
│   ├── home/                   ← HomeScreen.kt, HomeViewModel.kt
│   ├── addcard/                ← AddCardScreen.kt, AddCardViewModel.kt
│   ├── carddetail/             ← CardDetailScreen.kt, CardDetailViewModel.kt
│   ├── history/                ← HistoryScreen.kt, HistoryViewModel.kt
│   ├── settings/               ← SettingsScreen.kt, SettingsViewModel.kt
│   └── components/             ← Shared composables (CardChip, StatusRing, etc.)
│
├── domain/
│   ├── model/                  ← Card.kt, PaymentLog.kt, BankConfig.kt
│   └── repository/             ← CardRepository.kt, PaymentLogRepository.kt (interfaces)
│
├── data/
│   ├── local/
│   │   ├── db/                 ← AppDatabase.kt
│   │   ├── entity/             ← CardEntity.kt, PaymentLogEntity.kt
│   │   └── dao/                ← CardDao.kt, PaymentLogDao.kt
│   ├── remote/
│   │   └── firestore/          ← CardRemoteDataSource.kt, PaymentLogRemoteDataSource.kt
│   └── repository/             ← CardRepositoryImpl.kt, PaymentLogRepositoryImpl.kt
│
├── worker/                     ← CycleResetWorker.kt, DueSoonWorker.kt, OverdueWorker.kt
├── widget/                     ← SettledWidget.kt, SettledWidgetReceiver.kt
└── di/                         ← AppModule.kt (dependency injection)
```

---

## 14. Quick Reference Checklist

Before submitting any generated code, verify:

- [ ] UI does not call repository or database directly
- [ ] ViewModel does not import anything from the `ui` package
- [ ] Repository writes to Room before Firestore
- [ ] All entities have UUID primary keys and timestamps
- [ ] No hardcoded colours, strings, or dimensions
- [ ] No `!!` operator used
- [ ] No permissions added to the manifest
- [ ] No sensitive data in logs or analytics events
- [ ] Firestore errors are caught and logged, not rethrown
- [ ] New Workers are idempotent and use `CoroutineWorker`