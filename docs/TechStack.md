# Tech Stack Document: Settled

---

## 1. Overview

This document defines the tools, libraries, and services used to build Settled. Every choice prioritises:
- **Zero fixed cost** until meaningful user base is established
- **Android-native quality** — no cross-platform compromises
- **Developer familiarity** — leveraging AOSP background

---

## 2. Tech Stack at a Glance

| Category | Tool | Cost |
|---|---|---|
| Language | Kotlin | Free |
| UI Framework | Jetpack Compose | Free |
| Local Database | Room | Free |
| Cloud Database | Firebase Firestore | Free tier |
| Authentication | Firebase Anonymous Auth | Free tier |
| Widget | Jetpack Glance | Free |
| Notifications | Firebase Cloud Messaging (FCM) | Free |
| Analytics | PostHog | Free up to 1M events/month |
| Crash & Error Tracking | Firebase Crashlytics | Free, unlimited |
| Payments | Google Play Billing | Free (15% revenue share) |
| CI/CD | GitHub Actions | Free up to 2,000 mins/month |
| Design | Figma | Free tier |
| Version Control | GitHub | Free |

**Estimated fixed monthly cost at launch: ₹0**
The only one-time cost is the Google Play Developer account — $25 (~₹2,100), paid once.

---

## 3. Language & UI Framework

### Kotlin
- **What it is:** Official language for Android development. A modern, concise, null-safe language that compiles to the JVM.
- **Why:** You have a Java/AOSP background — Kotlin is Java with the pain removed. Null safety alone will save hours of debugging. Coroutines replace callbacks and make async code look synchronous.

### Jetpack Compose
- **What it is:** Android's modern declarative UI toolkit, replacing XML layouts.
- **Why:** Compose uses a component-based, reactive model — UI is a function of state. `@Composable` functions define UI components. `remember {}` manages local state. `LaunchedEffect` handles side effects tied to the composition lifecycle.

---

## 4. Local Database

### Room
- **What it is:** Google's official ORM (Object-Relational Mapper) for SQLite on Android.
- **Why:** Settled is local-first — all reads and writes hit Room first, and sync to the cloud in the background. This gives sub-100ms UI response (NFR2) regardless of network state.
- **How it works:** You define data classes (entities), a DAO (Data Access Object) with query methods, and a database class. Room generates all the boilerplate.

```kotlin
@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey val id: String,
    val bankName: String,
    val nickname: String,
    val lastFourDigits: String,
    val statementDate: Int,
    val dueDate: Int,
    val status: String
)
```

---

## 5. Cloud Database

### Firebase Firestore
- **What it is:** A NoSQL cloud database with real-time sync, offline support, and anonymous auth integration.
- **Why Firestore over Supabase:** Firestore has built-in offline-first sync — the SDK automatically queues writes when offline and flushes them when connectivity returns. This is a core NFR for Settled and would require custom engineering on Supabase.
- **Free tier limits:** 1GB storage, 50,000 reads/day, 20,000 writes/day. Sufficient for tens of thousands of active users before any cost is incurred.
- **Conflict resolution:** "Paid Always Wins" logic (defined in PRD) is implemented at the application layer before writes reach Firestore.

---

## 6. Authentication

### Firebase Anonymous Auth
- **What it is:** Generates a unique user token silently, with no sign-up, no email, no password.
- **Why:** Users never have to create an account. On first launch, a token is generated and stored. This token is used to scope all Firestore data to that user. If the user reinstalls or switches devices, they can optionally link a Google account to recover their data.
- **Future upgrade path:** Anonymous auth can be upgraded to Google Sign-In non-destructively — the user's existing data is preserved and migrated to the linked account automatically.
- **Cost:** Free. Anonymous auth has no usage limits on Firebase's free tier.

---

## 7. Widget

### Jetpack Glance
- **What it is:** Google's official API for building Android home screen widgets using Compose-like syntax.
- **Why:** Glance uses the same declarative, composable patterns as Jetpack Compose. Given your AOSP background, you already understand the AppWidget framework that Glance sits on top of — Glance just removes the XML boilerplate.
- **How it works:** Glance reads from Room (local DB) directly. When the app updates a card's state, it triggers a Glance update, which re-renders the widget within 60 seconds (NFR3).
- **Important distinction:** Glance widgets cannot access the internet directly. They read from local storage only — which is exactly the right pattern for Settled's local-first architecture.

---

## 8. Push Notifications

### Firebase Cloud Messaging (FCM)
- **What it is:** Google's push notification service for Android.
- **Why:** FCM is the standard for Android push notifications, free with no limits, and integrates natively with Firebase.
- **Usage in Settled:** Used only for the single Due Date notification (FR10). Since reminders are not a priority feature, FCM usage will be minimal — scheduled locally via `WorkManager` rather than server-side triggers, which means no backend is needed to send notifications.
- **WorkManager:** Android's built-in job scheduler. Schedules the Due Date notification locally on the device at card setup time. No server involved.

---

## 9. Analytics

### PostHog
- **What it is:** An open-source product analytics platform with a generous free tier.
- **Why over Firebase Analytics:** PostHog gives you funnels, retention graphs, session recordings, and feature flags — all on the free tier. Firebase Analytics gives you basic event counts. For tracking the Free → Pro conversion funnel, PostHog is significantly more powerful.
- **Why over Umami:** Umami is a web analytics tool built for page views and traffic. PostHog is built for product events — exactly what's needed to understand how users interact with Settled.
- **Free tier:** 1 million events/month. More than sufficient until significant scale.
- **Key events to track:**
  - `card_added`
  - `bill_marked_paid`
  - `widget_interacted`
  - `upgrade_prompt_shown`
  - `pro_upgraded`

---

## 10. Crash & Error Tracking

### Firebase Crashlytics
- **Why over Sentry:** Crashlytics is free with no event limits. Sentry's free tier caps at 5,000 errors/month — which can run out quickly during early buggy builds. Crashlytics gives you stack traces, device info, and ANR (App Not Responding) reports for free, forever.
- **Migration path:** If Sentry's richer context (breadcrumbs, non-crash error tracking) becomes necessary later, switching is straightforward. Start free, migrate when there's revenue to justify it.

---

## 11. Payments

### Google Play Billing
- **What it is:** Google's mandatory in-app purchase and subscription API for Android apps.
- **Why not Razorpay:** Google Play Store policy mandates that all digital goods and subscriptions sold inside an Android app must go through Google Play Billing. Third-party processors like Razorpay cannot be used for in-app subscriptions. Violating this results in app removal.
- **Cost:** No upfront cost. Google takes 15% of subscription revenue for the first $1M earned per year (reduced from the standard 30%).
- **Implementation:** The app queries Play Billing for the user's subscription status on launch. Pro features are gated locally based on this status.

---

## 12. CI/CD

### GitHub Actions
- **What it is:** Automated build, test, and release pipeline integrated directly into GitHub.
- **Why:** Free tier provides 2,000 minutes/month — sufficient for running builds and lint checks on every push. No separate CI service needed.
- **Initial pipeline:**
  - On every push → Run lint + unit tests
  - On merge to `main` → Build release APK
  - On tag → Build and upload to Play Store internal testing track

---

## 13. Design

### Figma
- **What it is:** Browser-based UI design and prototyping tool.
- **Why:** Free tier supports unlimited files for individual users. All screen designs, widget mockups, and component libraries will live here.
- **Workflow:** Design in Figma → Export assets → Implement in Jetpack Compose. Figma's Dev Mode can generate code hints for spacing, colours, and typography.

---

## 14. Version Control

### GitHub
- **Free tier:** Unlimited private repositories.
- **Branching strategy:** `main` (production) → `develop` (integration) → `feature/*` (individual features).

---

## 15. Architecture Pattern

### MVVM + Repository Pattern
- **What it is:** Model-View-ViewModel, the recommended Android architecture pattern.
- **Why:** Separates UI (Compose), business logic (ViewModel), and data (Repository → Room + Firestore).

```
UI (Compose)
    │
    ▼
ViewModel  ←── observes state, handles user actions
    │
    ▼
Repository  ←── single source of truth for data
    │         ├── Room (local, offline-first)
    │         └── Firestore (cloud sync)
    ▼
Glance Widget  ←── reads from Room directly
```

---

## 16. Cost Scaling Thresholds

When the app grows, here is when costs begin:

| Milestone | What triggers cost |
|---|---|
| ~50,000 DAU | Firestore free tier read limits approached |
| >1M events/month | PostHog free tier exceeded |
| >$1M revenue/year | Google Play revenue share increases to 30% |
| High crash volume | No cost — Crashlytics is unlimited |

Until the Firestore threshold, **monthly infrastructure cost = ₹0**.