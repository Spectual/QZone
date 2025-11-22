# QZone — Code Overview (For Architecture Diagram Generation)

This document summarizes the main modules, key classes and file paths, component responsibilities, and typical data flows in the current repository. The goal is to provide all necessary information and text for generating architecture diagrams (for PPT use).

---

## 1. Project Overview
- **Name**: QZone (Android)
- **Tech Stack**: Kotlin, Jetpack Compose, Navigation Compose, MVVM (ViewModel + StateFlow), Retrofit + Moshi, Firebase Auth, Play Services Location
- **Build**: Gradle (Kotlin DSL), AGP 8.5.2, Kotlin 1.9.24, Compose BOM
- **Module**: Single `:app` module (project root `settings.gradle.kts` includes ":app")

## 2. Key Configuration Files and Entry Points
- **Root**: `Qzone/build.gradle.kts` (plugin version declarations)
- **Module**: `Qzone/app/build.gradle.kts` (compileSdk, dependencies, Compose settings)
- **Manifest**: `Qzone/app/src/main/AndroidManifest.xml` (permissions, Application, MainActivity)
- **Application**: `Qzone/app/src/main/java/com/qzone/QzoneApp.kt` (Application subclass + AppContainer)
- **Entry Activity**: `Qzone/app/src/main/java/com/qzone/MainActivity.kt` (sets Compose content, gets container)

## 3. Main Packages and Files (by Responsibility)

### Application / DI
- `com.qzone.QzoneApp` — Initializes in `onCreate()`:
  - Calls `FirebaseUserRepository.ensureFirebaseInitialized(this)`
  - Creates `AppContainer` and injects implementations:
    - `surveyRepository = PlaceholderSurveyRepository()`
    - `rewardRepository = PlaceholderRewardRepository()`
    - `userRepository = FirebaseUserRepository()`
    - `locationRepository = LocationRepositoryImpl(this)`
- `AppContainer` — Lightweight service locator that holds repository instances and serves as a global access point (`(application as QzoneApp).container`)

### UI Layer (Compose)
- `com.qzone.ui.navigation.*` — Contains top-level `QzoneApp` Composable, navigation, `QzoneDestinations.kt` (route definitions)
- `MainActivity` calls `rememberQzoneAppState(...)` and passes repositories from the container to provide dependencies for top-level UI
- Screen Composables are located in `feature/*` or `ui/*` (layered according to IMPLEMENTATION_NOTES suggestions), e.g., Feed, Survey, Profile, Rewards, Auth

### ViewModel (MVVM)
- Each feature (Auth, Survey, Reward, Profile, Location) should have a corresponding ViewModel that exposes UI state using StateFlow
- ViewModels get data from repositories provided by `AppContainer` and execute business logic

### Data Layer (domain / data)
- `domain.repository` — Defines interfaces: `SurveyRepository`, `RewardRepository`, `UserRepository`, `LocationRepository`
- `data.repository` — Provides implementations:
  - `PlaceholderSurveyRepository` — Placeholder implementation (local seed data)
  - `PlaceholderRewardRepository` — Placeholder implementation
  - `FirebaseUserRepository` — Handles Firebase Auth (and contains `ensureFirebaseInitialized` static method)
  - `LocationRepositoryImpl` — Provides location information via Play Services
- Network stack (dependencies): Retrofit, OkHttp, Moshi (but repositories are currently mostly placeholder implementations, to be replaced with actual Retrofit services)

### Resources / Tools
- `app/src/main/res` — Color/style/string resources
- DataStore (preferences) dependency added for local persistence of placeholder data or caching

### Documentation
- `QZone/API_DOCUMENTATION.md` — Backend REST API documentation (unified response format, user/admin interfaces, etc.)
- `QZone/README.md`, `QZone/Qzone/IMPLEMENTATION_NOTES.md` — Product requirements and implementation notes

## 4. Typical Initialization and Runtime Call Sequence (for Architecture Diagram Timeline)

1. Android starts the process and creates `Application` instance: `QzoneApp.onCreate()`
   - Initializes Firebase (`FirebaseUserRepository.ensureFirebaseInitialized(this)`)
   - Creates and populates `AppContainer` (places repository instances)
2. System starts `MainActivity` (declared in Manifest)
   - In `MainActivity.onCreate()`:
     - `val container = (application as QzoneApp).container`
     - `setContent { QzoneTheme { val appState = rememberQzoneAppState(...container.surveyRepository, ...) QzoneApp(appState) }}`
3. Compose top-level `QzoneApp` renders navigation graph and initial Screen (Feed / SignIn, etc.)
4. UI layer triggers events (e.g., loading Feed) → Corresponding ViewModel calls repository (interface)
5. Repository (placeholder or network) returns data → ViewModel updates StateFlow → UI recomposes and renders
6. User actions trigger submission (e.g., submitting survey) → Repository handles persistence/sync to backend (or placeholder implementation)

On the architecture diagram, these points can be represented as: Application -> AppContainer -> Repositories ; MainActivity -> AppState -> UI -> ViewModel -> Repository -> External Services

## 5. External Dependencies and Services
- Firebase (Auth / Firestore) — `FirebaseUserRepository` (user authentication and profile)
- Backend API (Retrofit) — Not yet fully integrated, API documentation in `API_DOCUMENTATION.md`
- Google Play Services Location — Used by `LocationRepositoryImpl` (location permissions require runtime request)

## 6. Important File Path Index (for diagram annotations)

### Application / DI
- `Qzone/app/src/main/java/com/qzone/QzoneApp.kt` (Application + AppContainer)

### Activity
- `Qzone/app/src/main/java/com/qzone/MainActivity.kt`

### Navigation / Destinations
- `Qzone/app/src/main/java/com/qzone/ui/navigation/QzoneDestinations.kt`
- `Qzone/app/src/main/java/com/qzone/ui/navigation/QzoneApp.kt` (top-level Composable, if exists)

### Repositories
- `Qzone/app/src/main/java/com/qzone/data/repository/PlaceholderSurveyRepository.kt`
- `Qzone/app/src/main/java/com/qzone/data/repository/PlaceholderRewardRepository.kt`
- `Qzone/app/src/main/java/com/qzone/data/repository/FirebaseUserRepository.kt`
- `Qzone/app/src/main/java/com/qzone/data/repository/LocationRepositoryImpl.kt`

### Network / API docs
- `QZone/API_DOCUMENTATION.md`

### Gradle
- `Qzone/settings.gradle.kts`
- `Qzone/build.gradle.kts`
- `Qzone/app/build.gradle.kts`

(If any path name differs slightly in the repository, please let me know and I will help locate it precisely)

## 7. Diagram Suggestions (diagram elements and explanatory text, directly usable for PPT)

Suggest drawing the following shapes and connections in the architecture diagram for easy understanding:

- **Box**: Application (`QzoneApp`) — Note: Initializes AppContainer (annotation: Firebase initialization)
- **Box**: MainActivity — Points to Application.container (arrow/annotation: get container)
- **Box**: UI (Compose) — Top-level `QzoneApp`, NavGraph, Screens (Feed/Survey/Profile)
- **Box**: ViewModels — Feature ViewModels (Auth/Survey/Reward)
- **Box**: AppContainer — Service locator, holds repository instances
- **Row**: Repositories — PlaceholderSurveyRepository, PlaceholderRewardRepository, FirebaseUserRepository, LocationRepositoryImpl
- **Row**: External Services — Backend API (Retrofit), Firebase, Google Play Services (Location)

Arrow and label examples:
- MainActivity -> UI (setContent)
- UI -> ViewModel (events/rendering)
- ViewModel -> Repository (call interface)
- Repository -> External Services (Retrofit / Firebase / Play Services)
- Application -> AppContainer (initialization)
- AppContainer -> Repositories (inject/provide instances)

Additional notes (below PPT or in notes):
- "Placeholder implementation" means currently using local seed data, to be replaced with Retrofit/backend implementation later.
- Permission note: Location permission is required to display nearby surveys (please handle denial cases in UI).

## 8. Short Text for Architecture Diagram (can be directly placed in slides)
- QZone uses a single-module Android architecture (Kotlin + Compose).
- Global `AppContainer` is initialized via `QzoneApp`, and the container provides `Repository` instances to ViewModels/top-level state.
- Business flow: UI → ViewModel → Repository → (Backend / Firebase / Play Services).
- Currently has placeholder repositories for rapid UI development, planning to gradually replace with Retrofit-based backend implementations.

## 9. Future Work Suggestions (for future versions of architecture diagram)
- In `AppContainer`, select wiring based on build variant (debug/release) (placeholder vs actual implementation).
- Add unit tests and contract tests for key interfaces (SurveyRepository, UserRepository).
- Add monitoring/logging layer (e.g., OkHttp logging, Crashlytics) for debugging and error tracking.

---

If needed, I can:
1) Generate a more detailed architecture diagram (SVG / PNG / PPTX) based on the above;
2) Compress this file content into slide notes or directly generate a PPTX page (insert previously generated SVG);
3) Scan the code repository to precisely list all ViewModel and Composable file paths (if you want a more specific class-level diagram).

Please choose the next step (e.g., generate PPTX / auto-draw / list ViewModel files, etc.), and I will continue.
