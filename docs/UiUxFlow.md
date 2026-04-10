# UI/UX Flow Document: Settled

---

## 1. Overview

This document defines every screen in Settled, how they connect, what they contain, and how edge cases are handled. It is the single reference for UI implementation decisions.

**Design principles:**
- **Clarity over cleverness** — every screen answers one question or does one thing
- **Zero friction on the critical path** — marking a bill paid must never take more than 2 taps
- **Status is always visible** — the user should never have to dig for "did I pay?"
- **Privacy by design** — no screen ever asks for or displays sensitive financial information

---

## 2. Screen Map

```
┌─────────────────────────────────────────────────────────────────┐
│                         App Entry                               │
│                      (Splash / Auth)                            │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Home Screen                                │
│                   (Card List + Status)                          │
└───┬──────────┬──────────────┬──────────────┬────────────────────┘
    │          │              │              │
    ▼          ▼              ▼              ▼
Add Card   Card Detail    History        Settings
Screen     Screen         Screen         Screen
    │          │                            │
    │          ▼                            ▼
    │     Mark Paid                     Paywall
    │     Bottom Sheet                  Screen
    │
    ▼
Bank Selection
    │
    ▼
Card Name Selection
    │
    ▼
Card Details Entry
```

---

## 3. Screen Inventory

| # | Screen | Purpose |
|---|---|---|
| S1 | Splash / Auth | Silent auth, first-launch detection |
| S2 | Home Screen | Card list, status overview |
| S3 | Add Card — Bank Selection | Step 1 of card setup |
| S4 | Add Card — Card Name Selection | Step 2 of card setup |
| S5 | Add Card — Card Details | Step 3 of card setup |
| S6 | Card Detail Screen | Card info, mark as paid action |
| S7 | Mark as Paid Bottom Sheet | Payment logging |
| S8 | History Screen | Payment graph per card |
| S9 | Settings Screen | App preferences, account |
| S10 | Paywall Screen | Pro tier upsell |
| S12 | Data Export Screen | CSV export (Pro) |
| S13 | Account Deletion Screen | Permanent data removal |

---

## 4. Screen Flows

---

### S1 — Splash / Auth Screen

**Purpose:** Silent Firebase Anonymous Auth on first launch. Returns user to home on subsequent launches.

**Flow:**
```
App opens
    │
    ▼
Show app logo + "Settled" wordmark (centre screen)
    │
    ▼
Check: first launch?
                    │
                    ├── YES → Silent anonymous auth (~200ms)
                    │              │
                    │              ▼
                    │         Go to S2 (Home — empty state)
                    │
                    └── NO  → Go to S2 (Home — with cards)
```

**Screen elements:**
- App logo (centred)
- App name "Settled" below logo
- No loading spinner — transition should feel instant (target <400ms total)

**Edge cases:**
- Auth fails (no internet on very first launch) → proceed to Home in offline mode, retry auth silently in background
- Auth takes >2 seconds → show subtle loading indicator below logo

---

### S2 — Home Screen

**Purpose:** The primary screen. Shows all cards with their current payment status. Entry point to every other flow.

**Layout:**
```
┌─────────────────────────────────┐
│  Settled            [+ Add]  ⚙️ │  ← Top app bar
├─────────────────────────────────┤
│                                 │
│  ┌───────────────────────────┐  │
│  │  🔴  HDFC Regalia  •••1234│  │  ← Card item (OVERDUE)
│  │  Due: 15th · Overdue      │  │
│  └───────────────────────────┘  │
│                                 │
│  ┌───────────────────────────┐  │
│  │  🟡  Axis Magnus   •••5678│  │  ← Card item (DUE_SOON)
│  │  Due: 18th · Due in 1 day │  │
│  └───────────────────────────┘  │
│                                 │
│  ┌───────────────────────────┐  │
│  │  ⚪  SBI Card Elite •••9012│  │  ← Card item (PENDING)
│  │  Due: 22nd · 5 days left  │  │
│  └───────────────────────────┘  │
│                                 │
│  ┌───────────────────────────┐  │
│  │  🟢  ICICI Amazon  •••3456│  │  ← Card item (PAID)
│  │  Paid 2nd via CRED        │  │
│  └───────────────────────────┘  │
│                                 │
└─────────────────────────────────┘
```

**Card item elements:**
- Status ring / dot (colour coded)
- Bank logo (small, left)
- Card name
- Last 4 digits (masked: •••XXXX)
- Status line:
  - PAID → "Paid [date] via [platform]"
  - PENDING → "Due [date] · [N] days left"
  - DUE_SOON → "Due [date] · Due in [N] day(s)"
  - OVERDUE → "Due [date] · Overdue"
- ⚠️ icon if minimumDueLastCycle is true (shown below card name)

**Card sort order:**
```
1. OVERDUE   (top — most urgent)
2. DUE_SOON
3. PENDING
4. PAID      (bottom — least urgent)

Within same status → sort by due date ascending
```

**Tap behaviour:**
- Tap card → go to S6 (Card Detail Screen)
- Tap [+ Add] → go to S3 (Add Card — Bank Selection)
- Tap ⚙️ → go to S9 (Settings Screen)

**Empty state (no cards added):**
```
┌─────────────────────────────────┐
│  Settled            [+ Add]  ⚙️ │
├─────────────────────────────────┤
│                                 │
│         [Card illustration]     │
│                                 │
│      No cards added yet.        │
│   Add your first credit card    │
│      to get started.            │
│                                 │
│       [ + Add your first card ] │
│                                 │
└─────────────────────────────────┘
```

**Free tier card limit state (3 cards, add button still visible):**
- Tapping [+ Add] triggers S10 (Paywall Screen) instead of S3

**Downgrade/Lapsed state (user has >3 cards but subscription lapsed):**
- All data is preserved and historic logic runs correctly in the background.
- The 3 oldest cards (by creation date) remain fully active on the Home screen.
- Cards 4+ are greyed out with a lock icon. Tapping them or trying to mark them paid opens S10 (Paywall) instead of S6.

---

### S3 — Add Card: Bank Selection

**Purpose:** Step 1 of 3 in card setup. User selects their bank from a pre-populated list.

**Layout:**
```
┌─────────────────────────────────┐
│  ←  Add Card          (1 of 3)  │
├─────────────────────────────────┤
│  Which bank issued your card?   │
│                                 │
│  ┌─────────────────────────┐    │
│  │ 🔍 Search banks...      │    │  ← Search field
│  └─────────────────────────┘    │
│                                 │
│  ┌──────┐ ┌──────┐ ┌──────┐    │
│  │ HDFC │ │ SBI  │ │ICICI │    │  ← Quick-pick grid (top banks)
│  └──────┘ └──────┘ └──────┘    │
│  ┌──────┐ ┌──────┐ ┌──────┐    │
│  │ Axis │ │Kotak │ │ AMEX │    │
│  └──────┘ └──────┘ └──────┘    │
│                                 │
│  All Banks                      │
│  ─────────────────────────────  │
│  [HDFC logo]  HDFC Bank     >   │
│  [SBI logo]   State Bank    >   │
│  [ICICI logo] ICICI Bank    >   │
│  [Axis logo]  Axis Bank     >   │
│  ...                            │
└─────────────────────────────────┘
```

**Behaviour:**
- Search filters the list in real time
- Tapping a bank → go to S4 (Card Name Selection)
- Back arrow → go back to S2 (Home)

**Edge cases:**
- Search returns no results → "Bank not found. We're adding more banks regularly." with no further action (cannot proceed without selecting a supported bank)

---

### S4 — Add Card: Card Name Selection

**Purpose:** Step 2 of 3. User selects the specific card product from the chosen bank.

**Layout:**
```
┌─────────────────────────────────┐
│  ←  Add Card          (2 of 3)  │
├─────────────────────────────────┤
│  [HDFC logo]  HDFC Bank         │  ← Selected bank (tappable to go back)
│                                 │
│  Select your card               │
│                                 │
│  ┌─────────────────────────┐    │
│  │ 🔍 Search cards...      │    │
│  └─────────────────────────┘    │
│                                 │
│  [Card art]  Regalia            │
│  [Card art]  Infinia            │
│  [Card art]  Millennia          │
│  [Card art]  MoneyBack+         │
│  [Card art]  Diners Club Black  │
│  [Card art]  Swiggy             │
│  ...                            │
│                                 │
│  Don't see your card?           │
│  [Select "Other HDFC Card"]     │
└─────────────────────────────────┘
```

**Behaviour:**
- Each card item shows card art (or placeholder) + card name
- Tapping a card → go to S5 (Card Details Entry)
- "Other HDFC Card" fallback → go to S5 with generic card selected
- Tapping bank name at top → go back to S3

---

### S5 — Add Card: Card Details Entry

**Purpose:** Step 3 of 3. User enters last 4 digits and statement date.

**Layout:**
```
┌─────────────────────────────────┐
│  ←  Add Card          (3 of 3)  │
├─────────────────────────────────┤
│  [HDFC logo]  HDFC Regalia      │  ← Selected card (read-only)
│                                 │
│  Last 4 digits                  │
│  ┌─────────────────────────┐    │
│  │  •  •  •  [_ _ _ _]    │    │  ← Numeric input, max 4 digits
│  └─────────────────────────┘    │
│                                 │
│  Statement date                 │
│  ┌─────────────────────────┐    │
│  │  5th of every month  ▼  │    │  ← Dropdown (1–31)
│  └─────────────────────────┘    │
│                                 │
│  ┌─────────────────────────┐    │
│  │ ℹ️ Your due date will be │    │  ← Auto-calculated, informational
│  │    the 23rd of the month│    │
│  └─────────────────────────┘    │
│                                 │
│                                 │
│       [ Add Card → ]            │  ← Disabled until all fields filled
└─────────────────────────────────┘
```

**Behaviour:**
- Last 4 digits: numeric keyboard, max 4 characters
- Statement date: scrollable dropdown showing "1st", "2nd", ... "31st"
- Due date auto-calculates as soon as statement date is selected and shows in the info banner
- "Add Card" button is disabled until both fields are filled
- On submit → card saved to Room + Firestore → navigate to S2 (Home) with new card visible
- Success feedback: brief snackbar "HDFC Regalia added ✓"

**Edge cases:**
- Statement day 29/30/31 → info banner adds a note: "For months with fewer days, your statement will be on the last day of that month"
- Duplicate card (same bank + last 4 digits already exists) → show inline warning "This card looks like it's already added" but allow proceeding

---

### S6 — Card Detail Screen

**Purpose:** Full view of a single card. Primary action point for marking bills paid.

**Layout:**
```
┌─────────────────────────────────┐
│  ←  HDFC Regalia          [···] │  ← Overflow: Edit / Delete
├─────────────────────────────────┤
│                                 │
│   ┌─────────────────────────┐   │
│   │                         │   │
│   │    [Card visual]        │   │  ← Card art with status ring
│   │    HDFC Regalia         │   │
│   │    •••• •••• •••• 1234  │   │
│   │                         │   │
│   └─────────────────────────┘   │
│                                 │
│   Status        🔴 OVERDUE      │
│   Due date      15th (was 3     │
│                 days ago)       │
│   Statement     28th            │
│                                 │
│  ─────────────────────────────  │
│                                 │
│  ⚠️ Partial payment last cycle  │  ← Only shown if minimumDueLastCycle
│                                 │
│  ─────────────────────────────  │
│                                 │
│  Last payment                   │
│  Minimum Due · 28 Feb · MobiKwik│  ← Previous cycle's payment log
│                                 │
│       [ ✓ Mark as Paid ]        │  ← Primary CTA — always visible
│                                 │
└─────────────────────────────────┘
```

**Behaviour:**
- "Mark as Paid" → opens S7 (Mark as Paid Bottom Sheet)
- [···] overflow → shows Edit and Delete options
  - Edit → opens pre-filled S5 (Card Details Entry)
  - Delete → confirmation dialog → delete card + logs → back to S2
- Back arrow → S2 (Home)

**States:**
- PAID: "Mark as Paid" button replaced with "Paid ✓ [date] via [platform]" — still tappable to re-log if needed
- OVERDUE: Status row highlighted in red
- DUE_SOON: Status row highlighted in yellow

---

### S7 — Mark as Paid Bottom Sheet

**Purpose:** Quick action sheet for logging a payment. Opens from S6. Designed to complete in 2 taps.

**Layout:**
```
┌─────────────────────────────────┐
│  ▬                              │  ← Drag handle
│  Mark as Paid                   │
│  HDFC Regalia                   │
│                                 │
│  Payment type                   │
│  ┌────────────┐ ┌─────────────┐ │
│  │ ✓ Full     │ │  Minimum    │ │  ← Toggle chips
│  │   Payment  │ │  Due        │ │
│  └────────────┘ └─────────────┘ │
│                                 │
│  Paid using                     │
│  ┌──────┐ ┌────────┐ ┌───────┐ │
│  │ CRED │ │Amazon  │ │Mobikwik│ │  ← Platform chips
│  └──────┘ └────────┘ └───────┘ │
│  ┌──────────┐ ┌───────────────┐ │
│  │ Bank App │ │     Other     │ │
│  └──────────┘ └───────────────┘ │
│                                 │
│       [ Confirm Payment ]       │  ← Disabled until both selections made
└─────────────────────────────────┘
```

**Behaviour:**
- Default selection: "Full Payment" pre-selected
- Platform: nothing pre-selected — user must choose
- "Confirm Payment" → disabled until payment type AND platform are selected
- On confirm:
  - Card status → PAID in Room + Firestore
  - Payment log written
  - Widget triggers update
  - Bottom sheet closes
  - S6 updates to show "Paid ✓" state
  - Haptic feedback on confirm

**Edge cases:**
- User opens bottom sheet then dismisses without confirming → no change
- User selects "Minimum Due" → brief inline note appears: "A balance may carry over to your next cycle"

---

### S8 — History Screen

**Purpose:** Monthly payment history graph per card. Shows payment health over time.

**Layout:**
```
┌─────────────────────────────────┐
│  ←  Payment History             │
├─────────────────────────────────┤
│                                 │
│  ┌──────────────────────────┐   │
│  │ HDFC Regalia         ▼   │   │  ← Card selector dropdown
│  └──────────────────────────┘   │
│                                 │
│  Last 3 months (Free)           │  ← "Last 12 months" for Pro
│                                 │
│     Jan   Feb   Mar             │
│      │     │     │              │
│  ████│  ███│  ███│              │  ← Bar graph
│  ████│  ███│  ███│              │
│  ────┴─────┴─────┴──            │
│                                 │
│  Legend:                        │
│  🟢 Full   🟡 Minimum  🔴 Missed │
│                                 │
│  ─────────────────────────────  │
│                                 │
│  Mar 2025   Full · CRED         │  ← Log list below graph
│  Feb 2025   Minimum · MobiKwik  │
│  Jan 2025   Full · Bank App     │
│                                 │
│  [+ Update a past entry]        │  ← Retroactive entry trigger
└─────────────────────────────────┘
```

**Behaviour:**
- Card selector shows all user's cards — defaults to most recently viewed
- Free tier: 3 months visible, older months greyed out with lock icon + "Upgrade to Pro"
- Pro tier: 12 months visible
- Tapping a bar or log entry → opens retroactive edit bottom sheet
- "[+ Update a past entry]" → same retroactive edit bottom sheet
- Retroactive edit bottom sheet: same as S7 but with a date field pre-filled with the cycle month

**Empty state (new card, no history yet):**
- "No payment history yet. History will appear after your first billing cycle."

---

### S9 — Settings Screen

**Purpose:** App preferences and account management.

**Layout:**
```
┌─────────────────────────────────┐
│  ←  Settings                    │
├─────────────────────────────────┤
│                                 │
│  Account                        │
│  ─────────────────────────────  │
│  Plan          Free  [Upgrade]  │  ← Pro shows "Pro ✓"
│  Restore data            >      │  ← Google Sign-In for data recovery
│                                 │
│  Data                           │
│  ─────────────────────────────  │
│  Export as CSV           >      │  ← Pro only. Greyed if Free.
│  Delete all data         >      │  ← Opens S13
│                                 │
│  About                          │
│  ─────────────────────────────  │
│  Privacy Policy          >      │  ← Opens in-app browser
│  Terms of Service        >      │  ← Opens in-app browser
│  Version              1.0.0     │
│                                 │
└─────────────────────────────────┘
```

**Behaviour:**
- [Upgrade] → opens S10 (Paywall)
- Export as CSV (Pro) → opens S12 (Data Export)
- Greyed Pro features (Free tier) → tapping shows tooltip "Available in Pro"
- Delete all data → opens S13 (Account Deletion)
- Privacy Policy / Terms → opens in Chrome Custom Tab (in-app browser)

---

### S10 — Paywall Screen

**Purpose:** Pro tier upsell. Appears as a bottom sheet when a Free user hits a gated feature.

**Layout:**
```
┌─────────────────────────────────┐
│  ▬                              │
│  Upgrade to Settled Pro         │
│                                 │
│  ✓  Track unlimited cards       │
│  ✓  Full widget dashboard       │
│  ✓  12 months payment history   │
│  ✓  Export to CSV               │
│                                 │
│  ┌─────────────────────────┐    │
│  │  ◉ ₹50/month            │    │
│  │  ○ ₹550/year (1 month free)│    │
│  │                         │    │
│  │     [ Subscribe Now ]   │    │  ← Direct purchase CTA
│  └─────────────────────────┘    │
│                                 │
│  Cancel anytime. Billed        │
│  through Google Play.          │
│                                 │
│        [ Not now ]              │
└─────────────────────────────────┘
```

**Behaviour:**
- "Subscribe Now" → triggers Google Play Billing purchase flow
- On successful purchase → sheet closes, feature unlocks immediately
- "Not now" → dismisses sheet, user stays on Free tier
- Shown as bottom sheet over whatever screen triggered it — does not navigate away

---

### S12 — Data Export Screen

**Purpose:** CSV export of all card and payment data. Pro tier only.

**Layout:**
```
┌─────────────────────────────────┐
│  ←  Export Data                 │
├─────────────────────────────────┤
│                                 │
│  Export your payment history    │
│  as a CSV file.                 │
│                                 │
│  Includes:                      │
│  · All cards                    │
│  · Payment logs (12 months)     │
│  · Payment type and platform    │
│                                 │
│  Does not include:              │
│  · Full card numbers            │
│  · Any bank credentials         │
│                                 │
│       [ Export CSV ]            │
│                                 │
└─────────────────────────────────┘
```

**Behaviour:**
- "Export CSV" → generates file → triggers Android share sheet (user can save to Files, share via WhatsApp, email, etc.)
- File name: `settled_export_YYYY-MM-DD.csv`

---

### S13 — Account Deletion Screen

**Purpose:** Permanent data deletion. Requires explicit confirmation.

**Layout:**
```
┌─────────────────────────────────┐
│  ←  Delete All Data             │
├─────────────────────────────────┤
│                                 │
│  ⚠️  This cannot be undone       │
│                                 │
│  This will permanently delete:  │
│  · All your cards               │
│  · All payment history          │
│  · Your cloud backup            │
│                                 │
│  Your data cannot be recovered  │
│  after deletion.                │
│                                 │
│  Type DELETE to confirm:        │
│  ┌─────────────────────────┐    │
│  │                         │    │  ← Text input
│  └─────────────────────────┘    │
│                                 │
│    [ Delete Everything ]        │  ← Red button, disabled until
│                                 │     "DELETE" is typed exactly
└─────────────────────────────────┘
```

**Behaviour:**
- "Delete Everything" only enabled when user types "DELETE" exactly (case-sensitive)
- On confirm:
  1. Delete all Firestore documents
  2. Clear Room database
  3. Clear auth token
  4. Reset app to first-launch state → navigate to S1
- Show loading state during deletion (can take a few seconds)
- If deletion fails mid-way → show error and allow retry

---

## 5. Navigation Map

```
S1 (Splash)
    └──► S2 (Home)
              │
              ├──► S3 (Bank Selection)
              │         └──► S4 (Card Name)
              │                   └──► S5 (Card Details) ──► S2
              │
              ├──► S6 (Card Detail)
              │         ├──► S7 (Mark Paid) ──► S6
              │         ├──► S5 (Edit Card) ──► S2
              │         └──► S2 (after delete)
              │
              ├──► S8 (History)
              │
              └──► S9 (Settings)
                        ├──► S10 (Paywall) ──► S9
                        ├──► S12 (Export)
                        └──► S13 (Delete) ──► S1
```

---

## 6. Empty States Summary

| Screen | Trigger | Message |
|---|---|---|
| S2 Home | No cards added | "No cards added yet. Add your first credit card to get started." |
| S8 History | Card has no payment logs | "No payment history yet. History will appear after your first billing cycle." |
| S3 Bank Search | Search returns no results | "Bank not found. We're adding more banks regularly." |
| Widget | No cards | "Add a card in Settled to get started." |

---

## 7. Error States Summary

| Scenario | Behaviour |
|---|---|
| No internet on first launch | Proceed offline, retry auth silently |
| Firestore sync fails | Swallow silently — local data unaffected |
| Card deletion fails | Show snackbar "Couldn't delete card. Try again." |
| Play Billing unavailable | Show "Purchase unavailable. Try again later." |
| CSV export fails | Show snackbar "Export failed. Try again." |
| Account deletion fails mid-way | Show error screen with retry option |

---

## 8. Loading States Summary

| Action | Loading treatment |
|---|---|
| App launch / auth | Splash screen held (max 2s) |
| Adding a card | Button shows spinner, disabled during save |
| Mark as Paid | Haptic + instant local update; Firestore sync is background |
| History graph loading | Skeleton bars shown while Room query runs |
| CSV export generating | Button shows spinner |
| Account deletion | Full-screen loading overlay with "Deleting your data..." |

---

## 9. Key UX Decisions

| Decision | Rationale |
|---|---|
| 3-step card add flow (bank → card → details) | Reduces cognitive load per step. User makes one decision at a time. |
| Platform chips instead of dropdown in Mark Paid sheet | Faster to tap a chip than open a dropdown. Reduces the action to 2 taps. |
| "Full Payment" pre-selected in Mark Paid sheet | Most users pay in full. Defaults to the happy path. |
| Card sorted by urgency (OVERDUE first) | The most important card is always at the top without the user having to scan. |
| Type "DELETE" to confirm account deletion | Irreversible action requires deliberate effort. Prevents accidental deletion. |
| Due date shown as auto-calculated info, not input | Removes a step, builds trust that the app knows the right date. |
| No success screen after marking paid | Widget and card status update is the confirmation. No extra tap needed. |