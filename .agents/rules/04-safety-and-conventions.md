# Safety, Privacy, and Conventions

## 1. Naming Conventions

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

## 2. Data & Privacy Rules
These are non-negotiable and must never be violated by generated code.
- **Never log full card numbers, CVV, or expiry** — not even in debug logs
- **Never send last four digits to analytics** — PostHog events must contain only card IDs (UUIDs)
- **Never request SMS, Email, Contacts, or Location permissions** — if generated code adds any of these to the manifest, remove them immediately
- **Never store sensitive data in SharedPreferences** — use `EncryptedSharedPreferences` for anything sensitive (auth tokens)
- The only card identifier ever stored or transmitted is the last 4 digits + bank name

## 3. Error Handling Rules
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

## 4. What NOT to Generate
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

## 5. Quick Reference Checklist
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
