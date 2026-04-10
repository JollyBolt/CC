# Architecture & Project Guidelines

## 1. Project Identity
- **App name:** Settled
- **Platform:** Android only (no cross-platform, no web)
- **Language:** Kotlin
- **UI framework:** Jetpack Compose
- **Architecture:** MVVM + Repository Pattern
- **Database:** Room (local) + Firebase Firestore (cloud sync)
- **Widget:** Jetpack Glance

The app is local-first. All reads come from Room. Firestore is a sync target, never the primary data source.

## 2. Architecture Rules
### Non-negotiable structure
Every feature must follow this exact layer separation. Do not skip or merge layers.
```text
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

## 3. File & Package Structure
```text
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
