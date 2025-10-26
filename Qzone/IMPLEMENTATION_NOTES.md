# QZone Android Implementation Notes

## Overview
- **Tech stack**: Android SDK 34, Kotlin, Jetpack Compose (Material 3), Navigation Compose, ViewModel + StateFlow, Coil.
- **Architecture**: MVVM with a lightweight service locator (`AppContainer`). Placeholder repositories (`Placeholder*Repository`) feed static data until Java backend APIs are ready.

## Module Layout
- `app/src/main/java/com/qzone` — Application entry (`QzoneApp`, `MainActivity`).
- `data/model` — Parcelable data classes for surveys, rewards, user profile, history.
- `data/placeholder` — In-memory seed data for UI scaffolding.
- `data/repository` & `domain/repository` — Contracts and placeholder implementations; real network/data-store code slots in here.
- `feature/*` — Feature-scoped view models and Compose UI (auth, feed, survey, profile, history, rewards).
- `ui/navigation` — Navigation graph, app state, bottom navigation wiring.
- `ui/theme` — Material theme definitions aligned with design mock.

## Backend Integration TODOs
- **Authentication**: Replace `PlaceholderUserRepository.signIn/register` with Java-backed API calls; keep `AuthViewModel` interface unchanged.
- **Survey submission**: Implement request pipeline inside `SurveyViewModel.submit()` and swap placeholder survey data with remote paging in `SurveyRepository`.
- **Rewards**: Wire `RewardRepository` to backend endpoints for catalog + redemption; connect `RewardDetailScreen` `onRedeem` to new use case.
- **Profile persistence**: Update `UserRepository.updateProfile` to call backend + persist to local cache (Proto DataStore placeholder added in Gradle deps).

## Build & Tooling
- Compose compiler 1.5.10 with Kotlin 1.9.24; Gradle plugin 8.5.2. Generate a Gradle wrapper (`gradle wrapper`) before first build if the wrapper jar is absent.
- Recommended verification: `./gradlew lintKotlin`, `./gradlew testDebugUnitTest`, `./gradlew connectedDebugAndroidTest` (requires emulator).

## Design Alignment
- Sign-in, registration, feed, survey flow, profile, history, rewards, and reward detail screens mirror provided mock. Bottom navigation routes: Home, History, Rewards, Profile.
- UI uses brand palette from proposal; adapt typography and spacing to Compose defaults while preserving structure.

## Interaction Updates
- Feed "Top" chip now animates the list back to index 0 and a dedicated refresh chip keeps sample data in sync.
- Submitting a survey marks it complete, removes it from the feed, and adds the run to the profile/history placeholder data with updated point totals.

- Auth screens restyled with rounded fields/buttons to match the monochrome mock; Profile reflects signed-in credentials.
