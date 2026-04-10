# Architecture Document: Settled

---

## 1. Overview

Settled follows a **local-first, offline-capable** architecture. The device is the primary source of truth — all user actions are committed locally before anything reaches the cloud. The cloud exists purely for persistence and cross-device sync, not for real-time computation.

The architecture is built on the **MVVM + Repository pattern**, the recommended Android architecture for Jetpack Compose applications.

---

## 2. High-Level System Architecture

```
┌─────────────────────────────────────────────────────────┐
│                        DEVICE                           │
│                                                         │
│   ┌─────────────┐        ┌─────────────────────────┐   │
│   │   Compose   │◄──────►│       ViewModel         │   │
│   │     UI      │        │  (Business Logic +      │   │
│   └─────────────┘        │   State Management)     │   │
│                          └────────────┬────────────┘   │
│   ┌─────────────┐                     │                 │
│   │   Glance    │                     ▼                 │
│   │   Widget    │◄──────►  ┌──────────────────────┐    │
│   └─────────────┘          │      Repository       │    │
│                            │  (Single Source of    │    │
│   ┌─────────────┐          │       Truth)          │    │
│   │ WorkManager │          └────────┬─────┬────────┘    │
│   │ (Scheduler) │                   │     │             │
│   └─────────────┘                   │     │             │
│                            ┌────────┘     └────────┐    │
│                            ▼                       ▼    │
│                   ┌─────────────┐       ┌──────────────┐│
│                   │    Room     │       │  Firestore   ││
│                   │ (Local DB)  │       │    SDK       ││
│                   └─────────────┘       └──────┬───────┘│
└─────────────────────────────────────────────────│───────┘
                                                  │
                                          ┌───────▼────────┐
                                          │    CLOUD        │
                                          │                 │
                                          │ Firebase        │
                                          │ Firestore       │
                                          │ (Remote DB)     │
                                          │                 │
                                          │ Firebase        │
                                          │ Anonymous Auth  │
                                          │                 │
                                          │ Firebase        │
                                          │ Crashlytics     │
                                          │                 │
                                          │ FCM             │
                                          │                 │
                                          │ PostHog         │
                                          └─────────────────┘
```

---

## 3. Architecture Layers

Settled is divided into four distinct layers. Each layer has one responsibility and communicates only with the layer directly below it.

```
┌──────────────────────────────────────────┐
│              UI Layer                    │  Compose Screens + Glance Widget
├──────────────────────────────────────────┤
│           ViewModel Layer                │  State holders, user action handlers
├──────────────────────────────────────────┤
│          Repository Layer                │  Data orchestration, sync logic
├──────────────────────────────────────────┤
│           Data Sources                   │  Room (local) + Firestore (remote)
└──────────────────────────────────────────┘
```

### Layer Responsibilities

| Layer | Responsibility | Knows About |
|---|---|---|
| UI | Render state, capture user input | ViewModel only |
| ViewModel | Hold UI state, handle events, call repository | Repository only |
| Repository | Decide where data comes from/goes, handle sync | Room + Firestore |
| Data Sources | Raw read/write operations | Their own storage only |

---

## 4. Component Deep Dive

### 4.1 UI Layer — Jetpack Compose

The UI layer is a collection of `@Composable` screens that observe state from the ViewModel and emit user events back to it. The UI never directly touches the database or any business logic.

**Screens:**
```
MainActivity
│
├── HomeScreen          ← Card list, traffic light status
├── AddCardScreen       ← Bank selection, nickname, last 4, statement date
├── CardDetailScreen    ← Mark as Paid, payment type, platform selection
├── HistoryScreen       ← 6/12 month payment graph per card
├── SettingsScreen      ← Data export, account deletion
└── PaywallScreen       ← Pro tier upsell bottom sheet
```

**State flow (unidirectional):**
```
User Action
    │
    ▼
ViewModel.onEvent()
    │
    ▼
Repository (read/write)
    │
    ▼
ViewModel updates UiState
    │
    ▼
Compose recomposes UI
```

The UI never pulls data — it only reacts to state pushed from the ViewModel. This is enforced by exposing data as `StateFlow` from the ViewModel.

---

### 4.2 ViewModel Layer

Each screen has a dedicated ViewModel. The ViewModel holds the UI state as a `StateFlow<UiState>` and exposes event handlers that the UI calls on user interaction.

**ViewModels:**

| ViewModel | Manages |
|---|---|
| `HomeViewModel` | Card list, cycle status computation, widget trigger |
| `AddCardViewModel` | Bank list, due date auto-calculation, card creation |
| `CardDetailViewModel` | Mark as Paid flow, payment logging |
| `HistoryViewModel` | Payment graph data aggregation |
| `SettingsViewModel` | Data export, deletion |

**Due Date Auto-Calculation (AddCardViewModel):**
```
User selects Bank + Statement Date
         │
         ▼
ViewModel looks up BankDueDateConfig (local bundled map)
         │
         ▼
DueDate = StatementDate + bank.dueDateGapDays
         │
         ▼
Displayed to user before saving ("Your due date will be the 15th")
```

**Bank due date gap map (bundled locally, never fetched from network):**
```kotlin
val bankDueDateGapDays = mapOf(
    "HDFC"          to 18,
    "SBI"           to 15,
    "ICICI"         to 18,
    "Axis"          to 15,
    "Kotak"         to 15,
    "American Express" to 21,
    // ... all supported banks
)
```

---

### 4.3 Repository Layer

The Repository is the most critical layer. It acts as the **single source of truth** — the UI and ViewModel never decide where data comes from. The Repository makes that call.

**Core principle:**
- All **reads** come from Room (local). The UI never waits for a network call.
- All **writes** go to Room first, then sync to Firestore in the background.
- If offline, writes are queued by the Firestore SDK and flushed automatically on reconnect.

```
CardRepository
│
├── getCards()              → Flow from Room (live, reactive)
├── addCard()               → Write to Room → Write to Firestore
├── markAsPaid()            → Write to Room → Write to Firestore (with Paid Always Wins logic)
├── resetCycle()            → Triggered by WorkManager at 00:00 on Statement Date
├── getPastCycles()         → Read from Room (history screen)
└── syncFromCloud()         → On launch, pull latest Firestore state into Room
```

**Conflict Resolution — "Paid Always Wins":**
```
On sync, for each card cycle:

  if (localStatus == "Paid" OR remoteStatus == "Paid")
      → resolve to "Paid"
  else
      → Last Write Wins (use whichever has the later timestamp)
```

This logic runs in the Repository before any write reaches Firestore, ensuring the safer state always persists.

---

### 4.4 Data Layer — Room (Local Database)

Room is the offline-first local database. Every piece of app data lives here first.

**Schema:**

```
┌──────────────────────────────────────────────────────┐
│                     cards                            │
├──────────────┬───────────────────────────────────────┤
│ id           │ String (UUID, Primary Key)            │
│ userId       │ String (Firebase Anonymous UID)       │
│ bankName     │ String                                │
│ nickname     │ String                                │
│ lastFourDigits│ String                               │
│ statementDay │ Int (1–31)                            │
│ dueDateDay   │ Int (calculated, stored for display)  │
│ status       │ Enum: PAID, PENDING, DUE_SOON, OVERDUE│
│ createdAt    │ Long (epoch ms)                       │
│ updatedAt    │ Long (epoch ms)                       │
└──────────────┴───────────────────────────────────────┘

┌──────────────────────────────────────────────────────┐
│                   payment_logs                       │
├──────────────┬───────────────────────────────────────┤
│ id           │ String (UUID, Primary Key)            │
│ cardId       │ String (Foreign Key → cards.id)       │
│ cycleMonth   │ Int (1–12)                            │
│ cycleYear    │ Int                                   │
│ paymentType  │ Enum: FULL, MINIMUM, MISSED           │
│ platform     │ String (CRED, Amazon Pay, etc.)       │
│ paidAt       │ Long (epoch ms, nullable)             │
│ isRetroactive│ Boolean                               │
│ updatedAt    │ Long (epoch ms)                       │
└──────────────┴───────────────────────────────────────┘
```

**Key relationships:**
- One card → many payment logs (one per billing cycle)
- Payment logs are the source of truth for the History Graph
- `cycleMonth` + `cycleYear` + `cardId` together form a unique cycle identifier

---

### 4.5 Data Layer — Firebase Firestore (Remote Database)

Firestore mirrors the Room schema in the cloud. The structure is scoped per user using their anonymous UID.

**Firestore Collection Structure:**
```
users/
└── {userId}/
    ├── cards/
    │   └── {cardId}/          ← mirrors cards table
    │       ├── bankName
    │       ├── nickname
    │       ├── lastFourDigits
    │       ├── statementDay
    │       ├── dueDateDay
    │       ├── status
    │       └── updatedAt
    │
    └── payment_logs/
        └── {logId}/           ← mirrors payment_logs table
            ├── cardId
            ├── cycleMonth
            ├── cycleYear
            ├── paymentType
            ├── platform
            ├── paidAt
            └── updatedAt
```

**Security Rules:**
```
match /users/{userId}/{document=**} {
  allow read, write: if request.auth != null
                     && request.auth.uid == userId;
}
```
Users can only read and write their own data. No cross-user data access is possible.

---

### 4.6 Widget — Jetpack Glance

The Glance widget is a read-only view into Room. It does not go through the ViewModel or Repository — it reads Room directly for performance and simplicity.

**Widget data flow:**
```
Card status changes in app
         │
         ▼
Repository writes to Room
         │
         ▼
GlanceAppWidgetManager.updateAll() called
         │
         ▼
Widget reads latest card states from Room
         │
         ▼
Widget re-renders with updated Traffic Light status
```

**Widget variants:**

| Tier | Widget content |
|---|---|
| Free | Single most urgent card (highest priority status: Overdue > Due Soon > Pending > Paid) |
| Pro | All cards in a scrollable grid with traffic light rings |

**Traffic Light priority:**
```
OVERDUE   → Red ring    (highest urgency)
DUE_SOON  → Yellow ring
PENDING   → Grey ring
PAID      → Green ring  (lowest urgency)
```

---

### 4.7 Billing Cycle Engine — WorkManager

WorkManager is responsible for automated cycle resets. It runs in the background even when the app is closed.

**Scheduled jobs:**

| Job | Trigger | Action |
|---|---|---|
| `CycleResetWorker` | 00:00 on each card's Statement Day | Flip status from PAID → PENDING. If previous cycle was unpaid, write MISSED log. |
| `DueSoonWorker` | 48hrs before each card's Due Date | Flip status from PENDING → DUE_SOON |
| `OverdueWorker` | 00:00 on each card's Due Date (if unpaid) | Flip status to OVERDUE |
| `NotificationWorker` | Due Date, if status ≠ PAID | Send single FCM local notification |

**How jobs are scheduled:**
```
User adds/edits a card
         │
         ▼
Repository saves card to Room
         │
         ▼
WorkManager schedules all 4 workers for next cycle
  using card's statementDay and dueDateDay
         │
         ▼
Workers fire at correct times, update Room
         │
         ▼
Widget and UI re-render from updated Room state
```

Workers are rescheduled automatically every cycle after `CycleResetWorker` fires.

---

### 4.8 Authentication Flow

```
App Launch
    │
    ▼
Check: does local anonymous UID exist?
    │
    ├── YES → Use existing UID, skip auth
    │
    └── NO  → Firebase Anonymous Sign-In (silent, ~200ms)
                    │
                    ▼
              Store UID locally
                    │
                    ▼
              Scope all Firestore reads/writes to this UID
```

**Optional Google Sign-In (for data recovery):**
```
User reinstalls / new device
    │
    ▼
New anonymous UID generated (data appears lost)
    │
    ▼
User taps "Restore my data" in Settings
    │
    ▼
Google Sign-In prompt
    │
    ▼
Anonymous account linked to Google account
    │
    ▼
Previous Firestore data pulled down into Room
    │
    ▼
All cards and history restored
```

---

## 5. Data Flow Diagrams

### 5.1 Mark as Paid Flow

```
User taps "Mark as Paid" on CardDetailScreen
    │
    ▼
Selects: Full / Minimum Due
Selects: Platform (CRED, Amazon Pay, etc.)
    │
    ▼
CardDetailViewModel.onMarkAsPaid(type, platform)
    │
    ▼
CardRepository.markAsPaid(cardId, type, platform)
    │
    ├──► Room: UPDATE cards SET status = PAID
    │         INSERT INTO payment_logs (cardId, type, platform, paidAt)
    │
    ├──► Firestore SDK: write same data
    │         (queued if offline, flushed on reconnect)
    │
    └──► GlanceAppWidgetManager.updateAll()
              │
              ▼
         Widget reads Room → Green ring renders
```

### 5.2 App Launch Sync Flow

```
App opens
    │
    ▼
Auth check (silent, <200ms)
    │
    ▼
Room loads all cards → HomeScreen renders immediately
    │
    ▼ (background, non-blocking)
Firestore pulls latest remote state
    │
    ▼
Conflict resolution runs per card per cycle
("Paid Always Wins" + Last Write Wins)
    │
    ▼
Room updated with any remote changes
    │
    ▼
UI recomposes if state changed
```

The user sees their data instantly. The sync is invisible.

### 5.3 Retroactive Entry Flow

```
User opens History Screen
    │
    ▼
Taps a past month's entry (e.g., "March — Missed")
    │
    ▼
Edit bottom sheet opens
User selects: Full / Minimum Due, Platform, Date
    │
    ▼
HistoryViewModel.onRetroactiveUpdate(cycleMonth, cycleYear, ...)
    │
    ▼
CardRepository.updatePastCycle(...)
    │
    ├──► Room: UPDATE payment_logs SET paymentType, platform, paidAt, isRetroactive = true
    │
    └──► Firestore: same update
```

---

## 6. Offline Behaviour

| Scenario | Behaviour |
|---|---|
| No internet on launch | Room loads instantly. Firestore sync skipped silently. |
| Mark as Paid while offline | Written to Room immediately. Firestore SDK queues the write. |
| Connectivity restored | Firestore SDK flushes queued writes automatically. Conflict resolution runs. |
| Two devices, both offline | Each writes locally. On reconnect, "Paid Always Wins" resolves conflicts. |
| Offline for entire billing cycle | All state transitions (cycle reset, due soon, overdue) happen via WorkManager locally. No cloud dependency. |

---

## 7. Pro Tier Gating Architecture

Pro status is checked via Google Play Billing on every app launch and cached locally.

```
App Launch
    │
    ▼
Query Google Play Billing API for active subscription
    │
    ├── Active subscription found
    │       │
    │       └──► Set ProStatus = true in local DataStore
    │
    └── No active subscription
            │
            └──► Set ProStatus = false in local DataStore

At runtime, all Pro-gated features check ProStatus from DataStore.
No server call is needed mid-session.
```

**Gated features and their check points:**

| Feature | Where gate is enforced |
|---|---|
| 4th card addition | `AddCardViewModel` checks ProStatus before saving |
| Card limit gating | `HomeViewModel` locks cards 4+ (sorted by creation date) if Free |
| Full widget dashboard | `GlanceWidget` renders single card if Free |
| 12-month history | `HistoryViewModel` caps data at 3 months if Free |
| CSV export | `SettingsViewModel` shows upsell if Free |

---

## 8. Security Summary

| Concern | Mitigation |
|---|---|
| Unauthorised data access | Firestore security rules enforce UID-scoped access |
| Sensitive data in DB | Only last 4 digits stored — no full PAN, CVV, or expiry |
| Data in transit | All Firestore communication uses TLS by default |
| Analytics data | PostHog receives only anonymous event names, no user identifiers |
| Crash reports | Crashlytics receives stack traces only — no user data is attached |