# PRD: Settled

---

## 1. Executive Summary

Settled is a privacy-focused credit card management app designed to solve **"Confirmation Anxiety."** It serves as the definitive **Source of Truth** for users with multiple credit cards. The app takes a strict "no-scraping, no-permissions" approach — all data is entered manually by the user — backed by secure cloud synchronization to ensure data is never lost across devices or reinstalls.

---

## 2. Problem Statement

- **Permission Fatigue:** Users distrust apps that scrape SMS or Email.
- **The "Did I Pay?" Loop:** Fragmentation across payment apps (CRED, MobiKwik, Amazon Pay, etc.) creates mental uncertainty about bill payment status. Users end up cross-checking multiple apps to get an answer they should have in one place.
- **Data Loss Risk:** Purely offline apps lose all user data upon uninstallation or device change.

---

## 3. Product Vision

To provide a lightweight tool that offers **100% confidence in credit card payment status** through manual orchestration — backed by secure, encrypted cloud persistence. Settled doesn't track your spending, read your messages, or connect to your bank. It just answers one question: *Did I pay?*

---

## 4. Target Audience

- **Power Users:** 5+ credit cards, multiple payment platforms.
- **Privacy-Conscious Users:** Want to own their data without granting inbox access.

---

## 5. Core Features (MVP)

### 5.1 Zero-Permission Card Vault

- **Minimal Data Collection:** Collects ONLY — Bank Name (selected from a curated list), Card Nickname, Last 4 Digits, and Statement Date.
- **Auto Due Date Calculation:** Each bank has a publicly known, fixed gap between the Statement Date and the Due Date (e.g., HDFC = 18 days, SBI = 15 days). This mapping is bundled locally within the app. The Due Date is calculated automatically — the user never has to enter it.
- **Bank Selection:** Users pick their bank from a pre-populated list. Bank logos and statement-to-due-date gap values are bundled locally. No IIN lookup, no network calls for card identification.

### 5.2 The "Loop-Killer" Workflow

- **Manual Toggle:** "Mark as Paid" action for each billing cycle.
- **Payment Types:** Log "Full Payment" vs "Minimum Due."
- **Payment Platform:** User selects which app/platform they used (e.g., CRED, Amazon Pay, MobiKwik, Bank App, Other).
- **Memory Reinforcement:** Displays "Paid on [Date] via [Platform]" immediately after marking — providing instant cognitive relief.
- **Minimum Due Flag:** If a card was marked "Minimum Due" in the previous cycle, the next cycle's card view shows a ⚠️ *Partial payment last cycle* warning, reminding the user that a balance may have carried over.
- **Retroactive Entry:** Users can update a previous cycle's payment status (mark as paid, change payment type, add notes) to keep the history accurate in case they forgot to log it at the time.

### 5.3 Billing Cycle State Machine

Each card moves through the following states in a defined sequence:

```
[Paid] ──(Statement Date 00:00)──► [Pending]
                                        │
                              (48hrs before Due Date)
                                        │
                                        ▼
                                   [Due Soon]
                                        │
                              (Due Date passes, unpaid)
                                        │
                                        ▼
                                    [Overdue]
```

- **Paid → Pending:** At 00:00 on the Statement Date, the cycle resets automatically. If the previous cycle was never marked Paid, it is recorded as a missed payment in the Payment History graph (shown as a red bar for that month).
- **Pending → Due Soon:** 48 hours before the Due Date, status escalates.
- **Due Soon / Pending → Overdue:** If the Due Date passes without a "Mark as Paid" action, the card flips to Overdue. The user can still mark it paid retroactively.
- **Any State → Paid:** The user can mark a card as Paid from any state at any time.

### 5.4 "Settled" Dashboard Widget

- **Traffic Light Logic:** Card logos display colored rings — Green (Paid), Yellow (Due Soon), Red (Pending/Overdue).
- **Instant Confirmation:** Shows *"Last paid: [Date] via [App]"* directly on the home screen widget.
- **Free Tier:** Widget shows the single most urgent card.
- **Pro Tier:** Full multi-card widget dashboard.

### 5.5 Secure Persistence

- **Encrypted Cloud Sync:** Data is synced to a cloud database for cross-device persistence.
- **Silent Authentication:** Anonymous / token-based login — no sign-up required.
- **Offline-First:** All actions are applied locally first. Changes are queued and synced when connectivity is restored.
- **Conflict Resolution — "Paid Always Wins":** If a sync conflict is detected between two devices (e.g., one has "Paid" and the other has "Pending" for the same cycle), the **Paid state always takes precedence**, regardless of timestamp. A false "Unpaid" is more harmful to the user than a false "Paid." The conflict resolution logic is: `if (localState === 'Paid' || remoteState === 'Paid') → resolve to 'Paid'`. All other states use Last Write Wins.

---

## 6. Functional Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| FR1 | **Card Management:** Users must be able to add, edit, and delete card profiles containing Bank Name (from list), Card Nickname, Last 4 Digits, and Statement Date. Due Date must be calculated automatically using a locally bundled bank-specific statement-to-due-date gap table. | P0 |
| FR2 | **State Management:** The system must track four states per billing cycle: Paid, Pending, Due Soon (within 48hrs of Due Date), and Overdue (Due Date passed, unpaid). | P0 |
| FR3 | **Manual Override:** Users must be able to mark a bill as Paid (Full / Minimum Due) and specify the platform used (Dropdown: CRED, Amazon Pay, MobiKwik, Bank App, Other). | P0 |
| FR4 | **Automated Cycle Reset:** At 00:00 on the Statement Date, the card status resets to Pending for the new cycle. If the previous cycle was not marked Paid, it is recorded as a missed payment. | P0 |
| FR5 | **Retroactive Entry:** Users must be able to update payment status and details for any past cycle within the last 6 months. | P0 |
| FR6 | **Minimum Due Carry-Over Flag:** If a card was marked "Minimum Due" in the previous cycle, the current cycle's card view must display a ⚠️ Partial payment last cycle warning. | P0 |
| FR7 | **Cloud Sync:** Data must sync to a cloud database using "Paid Always Wins" for payment state conflicts, and Last Write Wins for all other fields. | P0 |
| FR8 | **Interactive Widget:** A home screen widget must display card status without opening the app. Free Tier shows the most urgent card; Pro Tier shows all cards. | P0 |
| FR9 | **Payment History Graph:** A dedicated screen showing payment outcomes per card — Full (green), Minimum Due (yellow), Missed (red). Free Tier shows up to 3 months of history; Pro Tier shows up to 12 months. | P1 |
| FR10 | **Basic Notification:** A single notification on the Due Date if the card is still in Pending, Due Soon, or Overdue state. | P1 |
| FR11 | **Pro Tier Upsell Gate:** When a Free Tier user attempts to add a 4th card, the app must display an upsell bottom sheet explaining the Pro Tier benefit — not a hard error block. Existing 3 cards remain fully functional. | P1 |
| FR12 | **Data Export:** Users must be able to export their full card and payment history as a CSV file from within the app. | P2 |
| FR13 | **Account Deletion:** Users must be able to permanently delete all their data (local + cloud) from within the app settings. | P2 |

---

## 7. Non-Functional Requirements

| ID | Requirement | Description |
|----|-------------|-------------|
| NFR1 | **Privacy** | The app must request ZERO permissions — no SMS, Email, Contacts, or Location. The manifest must be clean. |
| NFR2 | **Latency** | UI transitions and "Mark as Paid" actions must be sub-100ms. Local-first architecture is mandatory. |
| NFR3 | **Widget Reliability** | The widget must update its Traffic Light status within 60 seconds of a state change in the app or an automated cycle reset. |
| NFR4 | **Offline Availability** | If offline, the app must queue changes locally and sync silently when connectivity returns. No user action required. |
| NFR5 | **Security** | Pro users must have the option to gate the app behind system biometric authentication (Fingerprint / Face ID). |
| NFR6 | **Data Minimalism** | Last 4 digits and Bank Name are the only card identifiers. No full card numbers, no PII, must ever touch the database. |

---

## 8. Platform

- **Phase 1 (MVP):** Android only.
  - Home screen widgets are a first-class citizen on Android and are central to the product's value.
- **Phase 2:** iOS (post-MVP, pending traction).
  - iOS widgets (WidgetKit) have tighter restrictions — interactive widget functionality will need to be re-evaluated for iOS.

---

## 9. User Journey

```
Install App
     │
     ▼
Add First Card → Select Bank from List → Enter Last 4 Digits, Nickname,
                                         Statement Date → Due Date auto-calculated
     │
     ▼
Home Screen → Card shows "Pending" status
     │
     ▼ (Statement Date arrives)
Automated Reset → New cycle begins, card stays "Pending"
     │
     ▼ (48hrs before Due Date)
Status → "Due Soon"
     │
     ▼ (User pays bill via CRED / bank / etc.)
Opens App → Taps "Mark as Paid" → Selects "Full Payment" → Selects "CRED"
     │
     ▼
Card shows "Paid on [Date] via CRED" → Widget turns Green ✅
     │
     ▼ (Next Statement Date)
Cycle Resets → Back to "Pending" → If previous was "Minimum Due", ⚠️ flag shown
```

---

## 10. Monetization (Freemium Model)

| Feature | Free Tier | Pro Tier (₹50/month) |
|---------|-----------|----------------------|
| Cards tracked | Up to 3 | Unlimited |
| Home screen widget | Most urgent card only | Full multi-card dashboard |
| Payment History Graph | ✅ 3-month history | ✅ 12-month history |
| Biometric Lock | ❌ | ✅ |
| Data Export (CSV) | ❌ | ✅ |

**Pro Tier Upsell UX:** When a Free Tier user tries to add a 4th card, a bottom sheet appears — *"You've reached the 3-card limit on the free plan. Upgrade to Pro for ₹50/month to track unlimited cards."* — with options to Upgrade or Dismiss. No hard block. Existing cards are unaffected.

---

## 11. Out of Scope

The following will **never** be part of Settled:

- SMS or email scraping of any kind
- Bank integration or Open Banking APIs
- Spend tracking or transaction history
- Credit score monitoring or suggestions
- Auto-detection of bill amounts
- In-app bill payment
- Ads of any kind

---

## 12. Success Metrics

| Metric | Target (End of Month 3) |
|--------|------------------------|
| D7 Retention | ≥ 40% |
| D30 Retention | ≥ 20% |
| Users who mark ≥1 card paid per cycle | ≥ 60% of active users |
| Widget adoption rate | ≥ 50% of installs |
| Free → Pro conversion rate | ≥ 5% |
| App Store rating | ≥ 4.3 |

---

## 13. Data & Privacy

- **What is stored:** Bank Name, Card Nickname, Last 4 Digits, Statement Date, Due Date, payment logs (type + platform + timestamp).
- **What is never stored:** Full card numbers, CVV, expiry, bank login credentials, SMS content, email content.
- **Cloud storage:** Encrypted cloud database with anonymous auth. No name, phone, or email is ever collected.
- **Data export:** Users can export all their data as CSV at any time (Pro Tier).
- **Account deletion:** Users can permanently delete all data — local and cloud — from Settings. Deletion is irreversible and confirmed with a prompt.
- **Compliance:** Designed to be compliant with India's Digital Personal Data Protection (DPDP) Act. No PII is collected, so data processing obligations are minimal.