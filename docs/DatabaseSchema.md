# Database Schema: Settled

---

## 1. Overview

Settled uses a **dual-database architecture**:
- **Room (SQLite)** — local, on-device, offline-first. This is the primary database the app reads from.
- **Firebase Firestore** — cloud mirror of Room. Used for persistence and cross-device sync only.

Both databases hold the same logical data. Room is always written to first. Firestore is eventually consistent.

This document defines the complete schema for both, along with the reasoning behind every significant design decision.

---

## 2. Entity Relationship Diagram

```
┌─────────────────────┐         ┌──────────────────────────┐
│        cards        │         │       payment_logs        │
├─────────────────────┤         ├──────────────────────────┤
│ id (PK)             │────┐    │ id (PK)                  │
│ userId              │    └───►│ cardId (FK → cards.id)   │
│ bankName            │         │ cycleMonth               │
│ bankLogoSlug        │         │ cycleYear                │
│ cardName            │         │ paymentType              │
│ lastFourDigits      │         │ platform                 │
│ statementDay        │         │ paidAt                   │
│ dueDateDay          │         │ isRetroactive            │
│ status              │         │ updatedAt                │
│ minimumDueLastCycle │         └──────────────────────────┘
│ createdAt           │
│ updatedAt           │         ┌──────────────────────────┐
└─────────────────────┘         │       bank_config        │
                                ├──────────────────────────┤
                                │ bankName (PK)            │
                                │ displayName              │
                                │ logoSlug                 │
                                │ dueDateGapDays           │
                                └──────────────────────────┘
```

---

## 3. Tables

---

### 3.1 `cards`

The core table. One row per credit card added by the user.

```sql
CREATE TABLE cards (
    id                   TEXT PRIMARY KEY NOT NULL,
    userId               TEXT NOT NULL,
    bankName             TEXT NOT NULL,
    bankLogoSlug         TEXT NOT NULL,
    cardName             TEXT NOT NULL,
    lastFourDigits       TEXT NOT NULL,
    statementDay         INTEGER NOT NULL,
    dueDateDay           INTEGER NOT NULL,
    status               TEXT NOT NULL DEFAULT 'PENDING',
    minimumDueLastCycle  INTEGER NOT NULL DEFAULT 0,
    createdAt            INTEGER NOT NULL,
    updatedAt            INTEGER NOT NULL,

    CONSTRAINT chk_statementDay      CHECK (statementDay BETWEEN 1 AND 31),
    CONSTRAINT chk_dueDateDay        CHECK (dueDateDay BETWEEN 1 AND 31),
    CONSTRAINT chk_lastFourDigits    CHECK (length(lastFourDigits) = 4),
    CONSTRAINT chk_status            CHECK (status IN (
                                         'PAID',
                                         'PENDING',
                                         'DUE_SOON',
                                         'OVERDUE'
                                     )),
    CONSTRAINT chk_minimumDue        CHECK (minimumDueLastCycle IN (0, 1))
);
```

**Indexes:**
```sql
-- Most common query: fetch all cards for the current user
CREATE INDEX idx_cards_userId ON cards(userId);

-- Widget and WorkManager query: find cards by status
CREATE INDEX idx_cards_status ON cards(userId, status);

-- Cycle reset: find cards by statement day
CREATE INDEX idx_cards_statementDay ON cards(statementDay);
```

**Column decisions:**

| Column | Type | Decision |
|---|---|---|
| `id` | TEXT (UUID) | UUID over auto-increment because IDs are generated on-device before Firestore sync. Auto-increment integers would collide across devices. |
| `userId` | TEXT | Firebase anonymous UID. Scopes all data to the user without requiring PII. |
| `bankName` | TEXT | Stored as a canonical key (e.g., "HDFC") matching `bank_config.bankName`. Not a foreign key — bank_config is a static bundled table, not user data. |
| `bankLogoSlug` | TEXT | Filename slug for the locally bundled logo asset (e.g., "hdfc"). Stored to avoid re-deriving it from bankName on every render. |
| `cardName` | TEXT | Official card name selected from the bundled card list (e.g., "HDFC Regalia", "Axis Magnus"). Not user-defined — chosen from a pre-populated list per bank. |
| `lastFourDigits` | TEXT | Stored as TEXT not INTEGER to preserve leading zeros and enforce exactly 4 characters via constraint. |
| `statementDay` | INTEGER | Day of month (1–31). The month is not stored — the cycle engine always applies this to the current or next month. |
| `dueDateDay` | INTEGER | Calculated from `statementDay + bank_config.dueDateGapDays` at card creation. Stored to avoid re-computing it on every WorkManager run. |
| `status` | TEXT (Enum) | Stored as TEXT with a CHECK constraint rather than INTEGER codes. Readable in debug logs and Firestore console without a lookup table. |
| `minimumDueLastCycle` | INTEGER (Boolean) | 1 if last cycle was paid as Minimum Due, 0 otherwise. Stored directly on the card for fast widget rendering — avoids a JOIN to payment_logs just to show the ⚠️ flag. |
| `createdAt` / `updatedAt` | INTEGER (epoch ms) | Milliseconds since epoch. Used for Last Write Wins conflict resolution during Firestore sync. |

---

### 3.2 `payment_logs`

One row per billing cycle per card. This is the historical record of all payment activity.

```sql
CREATE TABLE payment_logs (
    id             TEXT PRIMARY KEY NOT NULL,
    cardId         TEXT NOT NULL,
    cycleMonth     INTEGER NOT NULL,
    cycleYear      INTEGER NOT NULL,
    paymentType    TEXT NOT NULL DEFAULT 'MISSED',
    platform       TEXT,
    paidAt         INTEGER,
    isRetroactive  INTEGER NOT NULL DEFAULT 0,
    updatedAt      INTEGER NOT NULL,

    FOREIGN KEY (cardId) REFERENCES cards(id) ON DELETE CASCADE,

    CONSTRAINT chk_cycleMonth     CHECK (cycleMonth BETWEEN 1 AND 12),
    CONSTRAINT chk_paymentType    CHECK (paymentType IN ('FULL', 'MINIMUM', 'MISSED')),
    CONSTRAINT chk_isRetroactive  CHECK (isRetroactive IN (0, 1)),
    CONSTRAINT uq_cycle           UNIQUE (cardId, cycleMonth, cycleYear)
);
```

**Indexes:**
```sql
-- History screen: fetch all logs for a card ordered by time
CREATE INDEX idx_logs_cardId ON payment_logs(cardId, cycleYear DESC, cycleMonth DESC);

-- Graph aggregation: fetch logs within a date range
CREATE INDEX idx_logs_cycle ON payment_logs(cardId, cycleYear, cycleMonth);
```

**Column decisions:**

| Column | Type | Decision |
|---|---|---|
| `id` | TEXT (UUID) | Same reasoning as cards — device-generated for offline safety. |
| `cardId` | TEXT (FK) | `ON DELETE CASCADE` ensures logs are cleaned up automatically when a card is deleted. No orphaned history records. |
| `cycleMonth` + `cycleYear` | INTEGER | Stored as separate integers rather than a date string. Simpler to query ("give me all logs where cycleYear = 2025 AND cycleMonth <= 6"), no date parsing needed. |
| `paymentType` | TEXT (Enum) | Three states: FULL (paid entire bill), MINIMUM (paid minimum due), MISSED (cycle closed without payment). MISSED is the default — a log is created at cycle reset, and only updated if the user marks it paid. |
| `platform` | TEXT (nullable) | NULL when paymentType is MISSED. Optional even for paid cycles — user may not remember which platform. |
| `paidAt` | INTEGER (nullable) | NULL for MISSED cycles. Epoch ms timestamp of when the user tapped "Mark as Paid." |
| `isRetroactive` | INTEGER (Boolean) | Flags entries edited after the cycle closed. Useful for analytics — distinguishes real-time logging from backfilled data. |
| `UNIQUE (cardId, cycleMonth, cycleYear)` | Constraint | Enforces one log per card per cycle at the database level. Prevents duplicate entries from sync conflicts or race conditions. |

---

### 3.3 `bank_config`

A static, read-only table bundled with the app. Never written to by the user or sync engine. Updated only via app updates.

```sql
CREATE TABLE bank_config (
    bankName        TEXT PRIMARY KEY NOT NULL,
    displayName     TEXT NOT NULL,
    logoSlug        TEXT NOT NULL,
    dueDateGapDays  INTEGER NOT NULL,

    CONSTRAINT chk_dueDateGap CHECK (dueDateGapDays BETWEEN 1 AND 45)
);
```

**Pre-populated data (sample):**
```sql
INSERT INTO bank_config VALUES ('HDFC',    'HDFC Bank',         'hdfc',    18);
INSERT INTO bank_config VALUES ('SBI',     'State Bank of India','sbi',    15);
INSERT INTO bank_config VALUES ('ICICI',   'ICICI Bank',        'icici',   18);
INSERT INTO bank_config VALUES ('AXIS',    'Axis Bank',         'axis',    15);
INSERT INTO bank_config VALUES ('KOTAK',   'Kotak Mahindra',    'kotak',   15);
INSERT INTO bank_config VALUES ('AMEX',    'American Express',  'amex',    21);
INSERT INTO bank_config VALUES ('YES',     'Yes Bank',          'yes',     15);
INSERT INTO bank_config VALUES ('IDFC',    'IDFC First Bank',   'idfc',    18);
INSERT INTO bank_config VALUES ('INDUSIND','IndusInd Bank',     'indusind',21);
INSERT INTO bank_config VALUES ('RBL',     'RBL Bank',          'rbl',     14);
```

**Column decisions:**

| Column | Decision |
|---|---|
| `bankName` | Canonical key. Used as the join reference in the `cards` table. Short, uppercase, no spaces — safe to use as a Firestore document field and a filename slug prefix. |
| `displayName` | Human-readable name shown in the UI. Kept separate from `bankName` so the canonical key never changes even if the display name does. |
| `logoSlug` | Maps to a bundled drawable asset: `R.drawable.logo_{logoSlug}`. Decoupled from `bankName` in case asset filenames differ from the canonical key. |
| `dueDateGapDays` | The number of days between Statement Date and Due Date for this bank. Source: publicly available bank documentation. Updated via app releases. |

---

## 4. Firestore Mirror Structure

Firestore holds the same data as Room but in a document/collection structure. The schema is intentionally flat — no nested subcollections — to keep reads simple and cheap.

```
users/
└── {userId}/                        ← document per user (anonymous UID)
    │
    ├── cards/
    │   └── {cardId}/                ← one document per card
    │       ├── userId               : string
    │       ├── bankName             : string
    │       ├── bankLogoSlug         : string
    │       ├── cardName             : string
    │       ├── lastFourDigits       : string
    │       ├── statementDay         : number
    │       ├── dueDateDay           : number
    │       ├── status               : string
    │       ├── minimumDueLastCycle  : boolean
    │       ├── createdAt            : timestamp
    │       └── updatedAt            : timestamp
    │
    └── payment_logs/
        └── {logId}/                 ← one document per cycle log
            ├── cardId               : string
            ├── cycleMonth           : number
            ├── cycleYear            : number
            ├── paymentType          : string
            ├── platform             : string | null
            ├── paidAt               : timestamp | null
            ├── isRetroactive        : boolean
            └── updatedAt            : timestamp
```

**What is NOT in Firestore:**
- `bank_config` — static, bundled with the app. No reason to store it remotely.
- Any computed fields — due date calculations, status transitions — happen on-device only.

---

## 5. Sync Strategy

### Room → Firestore (writes)
Every write to Room is immediately followed by a mirrored write to Firestore via the SDK. If offline, the Firestore SDK queues the write and flushes it automatically on reconnect.

### Firestore → Room (reads)
On app launch, the Repository fetches the latest Firestore snapshot and runs conflict resolution against Room before committing any remote changes locally.

### Conflict Resolution Rules

```
For each card document:
  if (local.status == "PAID" OR remote.status == "PAID")
      → write "PAID" to Room (Paid Always Wins)
  else
      → write whichever has the later updatedAt (Last Write Wins)

For each payment_log document:
  if (local.paymentType == "FULL" OR remote.paymentType == "FULL")
      → write "FULL" (most complete payment wins)
  else if (local.paymentType == "MINIMUM" OR remote.paymentType == "MINIMUM")
      → write "MINIMUM"
  else
      → Last Write Wins
```

---

## 6. Data Retention & Deletion

| Data | Retention | On Card Delete | On Account Delete |
|---|---|---|---|
| `cards` row | Until user deletes card | Deleted | All deleted |
| `payment_logs` rows | 12 months rolling | Cascade deleted | All deleted |
| `bank_config` rows | Permanent (app lifetime) | Unaffected | Unaffected |
| Firestore `cards` doc | Until user deletes card | Deleted | All deleted |
| Firestore `payment_logs` doc | 12 months rolling | Cascade deleted | All deleted |

**Account deletion flow:**
1. Delete all Firestore documents under `users/{userId}/`
2. Delete all Room tables (cards, payment_logs)
3. Clear Firebase Anonymous Auth token from local storage
4. Clear PostHog anonymous identity
5. Reset app to first-launch state

---

## 7. Data Size Estimates

| Table | Rows per user | Estimated size per row | Total per user |
|---|---|---|---|
| `cards` | ~6 (average) | ~300 bytes | ~1.8 KB |
| `payment_logs` | ~72 (6 cards × 12 months) | ~200 bytes | ~14.4 KB |
| `bank_config` | ~20 (static) | ~100 bytes | ~2 KB (shared) |

**Total per user: ~16 KB**
Firestore free tier limit: 1 GB = ~62,500 users before any storage cost.