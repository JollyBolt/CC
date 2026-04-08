# Launch Checklist: Settled

> This checklist covers everything required before Settled goes live on the Google Play Store.
> Work through it in order — each section builds on the previous one.
> Mark each item ✅ when complete.

---

## 1. Core Functionality

### Card Management
- [ ] User can add a card by selecting bank, card name, last 4 digits, and statement date
- [ ] Due date is auto-calculated and displayed correctly for all supported banks
- [ ] User can edit an existing card
- [ ] User can delete a card — payment logs cascade delete correctly
- [ ] Free tier hard cap at 3 cards — upsell bottom sheet appears on 4th attempt
- [ ] Pro tier allows unlimited cards

### Bill Tracking
- [ ] "Mark as Paid" flow works — Full and Minimum Due options both function
- [ ] Platform selection saves correctly (CRED, Amazon Pay, MobiKwik, Bank App, Other)
- [ ] "Paid on [Date] via [Platform]" confirmation displays immediately after marking
- [ ] ⚠️ Minimum Due carry-over flag shows correctly on next cycle
- [ ] Retroactive entry works for past cycles within 6 months

### State Machine
- [ ] PENDING → DUE_SOON transition fires correctly 48hrs before due date
- [ ] DUE_SOON / PENDING → OVERDUE transition fires correctly on due date if unpaid
- [ ] PAID → PENDING cycle reset fires at 00:00 on statement date
- [ ] Missed payment is recorded as MISSED in payment_logs on cycle reset
- [ ] Any state → PAID works at any time including retroactively

### Widget
- [ ] Widget installs and renders correctly on Android home screen
- [ ] Free tier widget shows single most urgent card
- [ ] Pro tier widget shows all cards
- [ ] Traffic light colours are correct — Green (PAID), Yellow (DUE_SOON), Grey (PENDING), Red (OVERDUE)
- [ ] "Last paid: [Date] via [App]" text renders correctly
- [ ] Widget updates within 60 seconds of a state change in the app
- [ ] Widget shows "Add a card" placeholder when no cards exist
- [ ] Widget renders correctly across common Android launcher sizes (2x1, 4x1, 4x2)

### Payment History
- [ ] History graph renders correctly for Free tier (3 months)
- [ ] History graph renders correctly for Pro tier (12 months)
- [ ] FULL payments show as green bars
- [ ] MINIMUM payments show as yellow bars
- [ ] MISSED payments show as red bars
- [ ] Retroactive entries are reflected correctly in the graph

### Sync & Offline
- [ ] All card data syncs correctly to Firestore
- [ ] App works fully offline — no crashes or blank states without internet
- [ ] Changes made offline sync correctly when connectivity returns
- [ ] "Paid Always Wins" conflict resolution works across two devices
- [ ] Last Write Wins works correctly for non-payment fields

### Pro Tier
- [ ] Google Play Billing integration works end-to-end in a test environment
- [ ] Subscription status is checked on every app launch
- [ ] Pro features unlock immediately after successful purchase
- [ ] Pro features lock correctly if subscription lapses
- [ ] Biometric lock works on supported devices (fingerprint and face)
- [ ] CSV export generates a valid, readable file
- [ ] Upsell bottom sheet displays correctly and links to Play Store subscription

---

## 2. Notifications

- [ ] Single Due Date notification fires correctly for PENDING / DUE_SOON / OVERDUE cards
- [ ] Notification does NOT fire if card is already PAID
- [ ] Notification is silenced after the user marks the card paid
- [ ] Notification tapping opens the correct card detail screen
- [ ] Notifications work with the app closed (WorkManager survives app kill)
- [ ] Notification permission is requested correctly on Android 13+ (POST_NOTIFICATIONS)

---

## 3. Data & Privacy

- [ ] App requests zero permissions except POST_NOTIFICATIONS (Android 13+)
- [ ] Android manifest contains no SMS, Email, Contacts, or Location permissions
- [ ] No full card numbers, CVV, or expiry are stored anywhere — local or cloud
- [ ] PostHog events contain no PII — only anonymous UUIDs and event names
- [ ] Crashlytics reports contain no user-identifiable data
- [ ] Account deletion removes all data from Room AND Firestore
- [ ] Data export (CSV) generates correct data and works on Pro tier
- [ ] EncryptedSharedPreferences used for auth tokens and biometric settings
- [ ] Firestore security rules tested — users cannot read or write other users' data

---

## 4. Performance & Stability

- [ ] "Mark as Paid" action completes in under 100ms (test on a mid-range device)
- [ ] Home screen loads in under 300ms with 6 cards
- [ ] No ANRs (App Not Responding) under normal usage
- [ ] App tested on Android 8.0 (API 26) minimum — confirm minimum SDK in build.gradle
- [ ] App tested on at least 3 different screen sizes
- [ ] App tested on at least one low-end device (2GB RAM)
- [ ] No memory leaks — run LeakCanary in debug build and resolve all leaks
- [ ] ProGuard / R8 minification enabled for release build
- [ ] Release APK size is reasonable (target under 20MB)

---

## 5. Firebase Setup

- [ ] Firebase project created with correct package name (`com.settled.app`)
- [ ] `google-services.json` added to the project (not committed to GitHub)
- [ ] Firebase Anonymous Auth enabled in Firebase console
- [ ] Firestore database created in production mode (not test mode)
- [ ] Firestore security rules deployed and verified
- [ ] Crashlytics confirmed receiving crash reports from a test crash
- [ ] FCM confirmed delivering test notifications to a physical device

---

## 6. Analytics Setup (PostHog)

- [ ] PostHog project created and Android SDK integrated
- [ ] All key events firing correctly and visible in PostHog dashboard:
  - [ ] `card_added`
  - [ ] `bill_marked_paid`
  - [ ] `bill_marked_minimum`
  - [ ] `widget_interacted`
  - [ ] `upgrade_prompt_shown`
  - [ ] `pro_upgraded`
  - [ ] `account_deleted`
  - [ ] `csv_exported`
- [ ] No PII is sent in any event property
- [ ] Anonymous distinct ID confirmed — not using device ID or any identifier

---

## 7. Play Store Setup

### Developer Account
- [ ] Google Play Developer account created ($25 one-time fee paid)
- [ ] Account identity verified by Google

### App Listing
- [ ] App name: **Settled**
- [ ] Short description written (80 characters max)
- [ ] Full description written (4,000 characters max) — focus on privacy angle and "did I pay?" problem
- [ ] App icon designed and exported in all required sizes
- [ ] Feature graphic created (1024 x 500px)
- [ ] Minimum 4 screenshots captured on a phone
- [ ] At least 1 screenshot showing the widget
- [ ] Content rating questionnaire completed
- [ ] App category set: Finance
- [ ] Tags / keywords added for discoverability

### Privacy & Legal
- [ ] Privacy Policy written and hosted at a public URL (e.g., GitHub Pages or Notion)
- [ ] Privacy Policy URL added to Play Store listing
- [ ] Privacy Policy URL added inside the app (Settings screen)
- [ ] Data Safety section in Play Store filled out accurately:
  - [ ] No data collected from users (last 4 digits are not classified as financial info under Play policy — verify this)
  - [ ] Data not shared with third parties declared accurately
  - [ ] Data encryption in transit declared
- [ ] Terms of Service written and accessible from within the app

### Release
- [ ] App signed with a release keystore (keystore file backed up securely — losing it means you can never update the app)
- [ ] Release keystore password stored in a password manager
- [ ] `minSdkVersion` set to 26 (Android 8.0)
- [ ] `targetSdkVersion` set to latest stable Android API level
- [ ] `versionCode` and `versionName` set for v1.0
- [ ] Release build tested end-to-end (not just debug build)
- [ ] App uploaded to Internal Testing track first
- [ ] Internal testing completed with at least 3 real devices
- [ ] Promoted to Closed Testing (Beta) before Open Testing / Production

---

## 8. Monetization Setup

- [ ] Google Play Billing subscription product created in Play Console:
  - Product ID: `settled_pro_monthly`
  - Price: ₹50/month
  - Free trial: consider 7-day free trial to reduce friction
- [ ] Subscription tested end-to-end using a test account
  - [ ] Purchase flow completes successfully
  - [ ] Pro features unlock after purchase
  - [ ] Cancellation flow tested — Pro locks correctly after grace period
- [ ] Grace period configured in Play Console (recommend 3 days)
- [ ] Subscription management page accessible from Settings screen

---

## 9. Pre-Launch Testing

### Functional Testing
- [ ] Full user journey tested from install to first card marked paid
- [ ] All 4 card states (PAID, PENDING, DUE_SOON, OVERDUE) manually verified
- [ ] Cycle reset tested by temporarily setting statement date to today
- [ ] Widget tested after fresh install, after app update, and after device restart
- [ ] Offline mode tested by enabling Airplane Mode mid-session
- [ ] Sync tested across two physical devices with the same account

### Edge Cases
- [ ] User adds card with statement day 29, 30, or 31 (months with fewer days handled correctly)
- [ ] User with 0 cards — empty state shown correctly on home screen and widget
- [ ] User deletes all cards — app does not crash
- [ ] User opens app for first time with no internet — does not crash
- [ ] User with lapsed Pro subscription — free tier limits enforced without data loss

### Devices
- [ ] Tested on Android 8.0 (minimum supported)
- [ ] Tested on Android 12 or 13 (most common current version)
- [ ] Tested on Android 14 or 15 (latest)
- [ ] Tested on a Samsung device (One UI — most common Android skin in India)
- [ ] Tested on a stock Android / Pixel device

---

## 10. Post-Launch (First 48 Hours)

- [ ] Monitor Crashlytics for any new crash types
- [ ] Monitor PostHog for drop-off in the card setup funnel
- [ ] Check Play Store reviews daily for the first week
- [ ] Confirm WorkManager cycle reset jobs are firing correctly in production
- [ ] Confirm widget is updating correctly for early users (check for NFR3 complaints)
- [ ] Have a hotfix release process ready — know how to push an update quickly if a critical bug surfaces

---

## Launch Blockers

These items must be ✅ before submitting to the Play Store. No exceptions.

| # | Blocker |
|---|---|
| 1 | All P0 functional requirements from PRD working correctly |
| 2 | Zero permissions in manifest except POST_NOTIFICATIONS |
| 3 | Privacy Policy hosted and linked |
| 4 | Data Safety section in Play Console completed |
| 5 | Release keystore created and backed up |
| 6 | Firestore security rules deployed |
| 7 | App does not crash on fresh install with no internet |
| 8 | Widget renders correctly on at least 3 devices |
| 9 | Google Play Billing subscription product created and tested |
| 10 | Internal testing completed on at least 3 real devices |