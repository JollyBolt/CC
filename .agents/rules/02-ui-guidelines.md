# UI & Code Style Guidelines

## 1. Jetpack Compose Rules
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

## 2. Kotlin Code Style
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

## 3. Jetpack Glance (Widget) Rules
- The widget reads from Room directly — it does not go through the ViewModel or Repository
- Never make network calls from a Glance composable
- Widget state is a simple data class passed to `GlanceAppWidget.provideGlance()`
- Always handle the case where Room returns an empty list gracefully — show an "Add a card" placeholder
- Free tier: render only the single most urgent card (priority: OVERDUE > DUE_SOON > PENDING > PAID)
- Pro tier: render all cards in a grid
