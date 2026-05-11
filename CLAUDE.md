# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Settled** is an Android credit card payment tracker for Indian banks. Users add their credit cards, and the app tracks payment due dates, records payments (full/minimum), and shows a 3-state status per card.

## Commit Style

- Never add a `Co-Authored-By` trailer to commit messages.

## Build Commands

```bash
# Debug build
./gradlew :app:assembleDebug

# Release build (minified)
./gradlew :app:assembleRelease

# Unit tests
./gradlew :app:testDebugUnitTest

# Single test class
./gradlew :app:testDebugUnitTest --tests "com.example.settled.ExampleUnitTest"

# Instrumented tests (requires device/emulator)
./gradlew :app:connectedDebugAndroidTest
```

> **Build restriction**: Never run `./gradlew` or `git push` directly. Propose the command to the user and wait for them to run it. Assume success unless the user pastes an error.

## Architecture

Clean Architecture + MVVM, single `:app` module.

```
domain/       — pure Kotlin: models, CardRepository interface, SupportedCardsRegistry
data/         — Room entities/DAO, CardRepositoryImpl (all business logic lives here)
ui/           — Compose screens grouped by feature (home, details, addcard, settings, splash)
di/           — Hilt modules (AppModule: Room DB; RepositoryModule: binds CardRepositoryImpl)
core/         — sealed Result<T> (Success / Error / Loading)
```

**Key tech**: Kotlin, Jetpack Compose, Hilt, Room (v3), Compose Navigation, Coroutines + Flow.

### Layer Rules (non-negotiable)

```
UI (Composable screens)
    ↓ events / ↑ state
ViewModel (state holder)
    ↓ calls
Repository (data orchestration)
    ↓               ↓
Room (local)    Firestore SDK (remote, future)
```

- **UI**: Composables are stateless — state from `StateFlow` only, never call repository/DB directly
- **ViewModel**: one per screen, never reference Android `Context`, never import from `ui` package
- **Repository**: write Room first, then Firestore; return `Flow<T>` for observed data, `Result<T>` for one-shots
- **Data**: DAOs are interfaces; never expose Room entities above the repository layer

### Naming Conventions

| Element | Pattern | Example |
|---|---|---|
| Composable | PascalCase | `CardListItem()` |
| ViewModel | `{Screen}ViewModel` | `HomeViewModel` |
| UiState | `{Screen}UiState` | `HomeUiState` |
| Event sealed class | `{Screen}Event` | `HomeEvent` |
| Repository | `{Entity}Repository` | `CardRepository` |
| Room Entity | `{Entity}Entity` | `CardEntity` |
| Room DAO | `{Entity}Dao` | `CardDao` |
| Domain model | PascalCase, no suffix | `Card`, `PaymentLog` |
| Worker | `{Action}Worker` | `CycleResetWorker` |
| Constants | SCREAMING_SNAKE_CASE | `DUE_SOON_THRESHOLD_HOURS` |

### Status Engine

`CardRepositoryImpl.calculateStatus()` computes card status on every DB read using `statementDay` and `dueDay` stored per card. The 3 states:

| Status | Condition |
|--------|-----------|
| `PAID` | A payment log exists for the active billing cycle |
| `DUE` | No payment and today is on or before the due date |
| `OVERDUE` | No payment and today is strictly after the due date |

The active cycle is determined relative to `statementDay`: if today is before this month's statement date, the previous month's statement is the active one. When `dueDay < statementDay`, the due date falls in the month _after_ the statement month.

Payment logs are keyed by `(cycleMonth, cycleYear)` matching the `activeStatementDate`, not the payment timestamp.

### ViewModel Pattern

Every screen follows the same three-type pattern:
- `XxxUiState` — `StateFlow`, drives the UI
- `XxxEvent` — user actions sent via `onEvent()`
- `XxxUiEvent` — one-shot side effects via `Channel` (navigation, snackbars)

### Navigation

`SettledNavGraph.kt` owns all routes. Add Card is a nested nav graph (`add_card_flow`) with a shared `AddCardViewModel` scoped to the parent back-stack entry (bank select → card type → details entry → success). The bottom bar is hidden only on the Splash screen.

### Supported Cards

`SupportedCardsRegistry` (domain layer) is the static source of truth for which bank+card combinations can be added. Duplicate prevention is enforced at both the DAO level (unique index on `bankName`+`cardName`) and the repository level.

### Resource Organization

Image assets are split across three extra resource directories declared in `build.gradle.kts`:
- `res-organized/payment-apps` — payment platform logos
- `res-organized/bank-horizontal` — wide bank logos
- `res-organized/bank-square` — square bank logos

All user-facing strings go in `strings.xml`. No hardcoded string literals in Kotlin UI files.

### Database Migrations

DB is at version 3. Migrations are defined in `SettledDatabase` companion:
- v1→v2: renamed `statementDate` → `statementDay`, added `dueDay`
- v2→v3: added unique index on `(bankName, cardName)`

When adding columns or tables, add a new `MIGRATION_X_Y` and register it in `AppModule.provideDatabase()`.

## Safety & Privacy Rules (non-negotiable)

- Never log full card numbers, CVV, or expiry — not even in debug logs
- Never send last four digits to analytics — events must contain only card IDs (UUIDs)
- Never request SMS, Email, Contacts, or Location permissions
- Never store sensitive data in plain `SharedPreferences` — use `EncryptedSharedPreferences`
- Never use `XMLLayouts` or `ViewBinding` — Compose only
- Never use `LiveData` — use `StateFlow` and `Flow`
- Never use `AsyncTask` or `GlobalScope` — use coroutines with `viewModelScope`
- Never use `!!` — use `?.`, `?:`, or `requireNotNull()` with a message
- Never hardcode colours, font sizes, or spacing outside `Theme.kt` / `Color.kt`
- Never add a third-party library without flagging it first

---

## MCP Tools: code-review-graph

**IMPORTANT: This project has a knowledge graph. ALWAYS use the
code-review-graph MCP tools BEFORE using Grep/Glob/Read to explore
the codebase.** The graph is faster, cheaper (fewer tokens), and gives
you structural context (callers, dependents, test coverage) that file
scanning cannot.

### When to use graph tools FIRST

- **Exploring code**: `semantic_search_nodes` or `query_graph` instead of Grep
- **Understanding impact**: `get_impact_radius` instead of manually tracing imports
- **Code review**: `detect_changes` + `get_review_context` instead of reading entire files
- **Finding relationships**: `query_graph` with callers_of/callees_of/imports_of/tests_for
- **Architecture questions**: `get_architecture_overview` + `list_communities`

Fall back to Grep/Glob/Read **only** when the graph doesn't cover what you need.

### Key Tools

| Tool | Use when |
|------|----------|
| `detect_changes` | Reviewing code changes — gives risk-scored analysis |
| `get_review_context` | Need source snippets for review — token-efficient |
| `get_impact_radius` | Understanding blast radius of a change |
| `get_affected_flows` | Finding which execution paths are impacted |
| `query_graph` | Tracing callers, callees, imports, tests, dependencies |
| `semantic_search_nodes` | Finding functions/classes by name or keyword |
| `get_architecture_overview` | Understanding high-level codebase structure |
| `refactor_tool` | Planning renames, finding dead code |

### Workflow

1. The graph auto-updates after every Edit/Write/Bash tool call (PostToolUse hook).
2. Use `detect_changes` for code review.
3. Use `get_affected_flows` to understand impact.
4. Use `query_graph` pattern="tests_for" to check coverage.
