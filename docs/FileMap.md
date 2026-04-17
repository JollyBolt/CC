# Project File Map

This document provides a directory of all files in the project under `app/src/main/java/com/example/settled`, along with brief descriptions of their purpose.

## Root Package: `com.example.settled`
Core application entry points.

- `MainActivity.kt`: The main host Activity for the Compose UI and navigation.
- `SettledApp.kt`: Custom `Application` class, used for Hilt initialization.

---

## Package: `core`
General cross-cutting concerns and shared utilities.

- `Result.kt`: Standard wrapper for network or database operations (Success/Error).

---

## Package: `data`
Implementation of the data layer, including local storage and repositories.

### Directory: `data/local`
Room Database implementation and local storage.

- `SettledDatabase.kt`: Room database configuration, versioning, and migrations.
- `CardEntity.kt`: Database entity representing a credit card.
- `PaymentLogEntity.kt`: Database entity representing a payment entry.
- `CardDao.kt`: Data Access Object (DAO) for querying cards and logs.

### Directory: `data/repository`
Concrete implementations of domain repository interfaces.

- `CardRepositoryImpl.kt`: Orchestrates data flow between Room and the UI, handles billing cycle logic.

---

## Package: `di`
Dependency Injection modules using Dagger Hilt.

- `AppModule.kt`: Provides singletons like the Room database instance.
- `RepositoryModule.kt`: Binds repository interfaces to their concrete implementations.

---

## Package: `domain`
Pure business logic and environment-agnostic entities.

### Directory: `domain/model`
Plain data models and enums used across the app.

- `Card.kt`: Domain model representing a Credit Card.
- `CardStatus.kt`: Enum defining payment states (PAID, DUE, SOON, OVERDUE).
- `PaymentLog.kt`: Domain model representing a payment transaction.
- `SupportedCardsRegistry.kt`: Static registry of supported banks and card variants.

### Directory: `domain/repository`
Interfaces defining the contracts for data access.

- `CardRepository.kt`: Interface for managing cards and payment logs.

---

## Package: `ui`
UI layer implemented with Jetpack Compose.

### Directory: `ui/navigation`
Navigation routing and graph definitions.

- `SettledNavGraph.kt`: Defines the app's navigation structure and screen destinations.
- **`ui/navigation/components`**:
    - `BottomBar.kt`: The main tab bar for switching between Home, History, and Settings.

### Directory: `ui/theme`
Design system tokens and theme configuration.

- `Theme.kt`: Main theme wrapper (SettledTheme).
- `Color.kt`: Color palette definitions.
- `Type.kt`: Typography and font configurations.

### Directory: `ui/screens`
Screen-level composables, ViewModels, and UI state models.

#### `ui/screens/home`
The main dashboard showing card summaries.
- `HomeScreen.kt`: Dashboard UI.
- `HomeViewModel.kt`: Business logic for the home screen.
- `HomeUiState.kt` / `HomeEvent.kt` / `HomeUiEvent.kt`: MVI/MVVM plumbing.
- **`ui/screens/home/components`**:
    - `CardListItem.kt`: Individual card row item on the home screen.

#### `ui/screens/addcard`
The multi-step onboarding flow for new cards.
- `BankSelectionScreen.kt`: Step 1 - Choosing a bank logo.
- `CardSelectionScreen.kt`: Step 2 - Choosing a card variant.
- `CardDetailsEntryScreen.kt`: Step 3 - Entering nickname, digits, and dates.
- `AddCardSuccessScreen.kt`: Confirmation screen.
- `AddCardViewModel.kt` / `AddCardUiState.kt` etc.: Logic for the onboarding flow.

#### `ui/screens/details`
Detailed view for a specific card.
- `CardDetailsScreen.kt`: Main detail view with history and payment button.
- `CardDetailsViewModel.kt` / `CardDetailsUiState.kt` etc.: Detail-specific logic.
- **`ui/screens/details/components`**:
    - `CardDatesBox.kt`: Display for cycle dates.
    - `CardStatusSection.kt`: Payment status text and colors.
    - `PaymentBottomSheet.kt`: UI for logging a new payment.
    - `PaymentHistorySection.kt`: List of historical payments.
    - `PaymentLogItem.kt`: Row item for payment history.
    - `PaymentPlatformRegistry.kt`: Logic and colors for payment platforms (CRED, etc.).

#### `ui/screens/settings`
Configuration and app info.
- `SettingsScreen.kt`: Simple settings and export controls.

#### `ui/screens/splash`
Initial loading screen.
- `SplashScreen.kt`: Displays the logo on app startup.
