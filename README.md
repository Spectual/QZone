# QZone — Location-Based Survey App (Android, Kotlin)

> A mobile-first survey platform that uses **geo-targeting** and **rewards system** to boost response rate and sample diversity. Built with **Jetpack Compose + MVVM**.

---

## Team

**Xuetong Fu – Backend / API**

Xuetong designed and documented the comprehensive Java backend REST API, including user authentication endpoints, survey CRUD operations, response submission, rewards management, and location-based services. He implemented the Firebase token exchange mechanism that seamlessly bridges Firebase Authentication with the backend's Bearer token system, enabling secure API access for all authenticated requests. Additionally, he built the unified local storage layer using Room database for offline survey caching and implemented the interactive map view using Google Maps SDK to display nearby survey locations with markers and distance calculations.

**Yifei Bao – Survey & Rewards**

Yifei built the complete survey experience flow from feed discovery through submission, including the survey detail screen, multi-question UI supporting single choice, multiple choice, and text input types, with progress tracking and answer persistence. He implemented the shake-to-refresh feature using accelerometer detection with threshold-based gesture recognition and cooldown mechanisms. For the rewards system, he developed the points calculation logic, reward catalog UI, redemption wallet view, and the complete redemption flow that integrates with the backend coupon system to track user rewards and point balances.

**Zhenyang Qian – UI/UX & Account**

Zhenyang created the entire Compose design system including Material 3 theme definitions, custom typography, color schemes for light/dark modes, reusable card components, and shape definitions that provide a cohesive visual experience across all screens. He implemented the comprehensive profile management system including profile editing, avatar upload with remote storage and local caching, survey history with filtering and search capabilities, and the wallet view for tracking redemptions. He also built the navigation structure using Navigation Compose with type-safe routes, bottom navigation bar, and seamless screen transitions throughout the app.

> As a collaborative team of three, we will share all design and architecture decisions. To ensure progress, we have assigned primary focus areas, but we will all contribute to the entire codebase through pair programming and code reviews.

---

## Project Description

**QZone** lets users discover and complete surveys tied to their **current location**. Users open the app to see nearby surveys, answer with a **modern multimedia interface**, and earn **points** upon completion.  
For creators (future web portal), QZone enables **precise geo-targeting** to collect diverse and context-relevant responses.

### Problem

Traditional survey tools face critical limitations:
- **Poor mobile experience** — Web forms are clunky on smartphones, leading to high abandonment rates
- **Lack of location context** — Surveys cannot target users based on their current location, missing opportunities for context-relevant data collection
- **Low engagement** — No instant incentives or gamification, resulting in low response rates and sample bias
- **Limited reach** — Static distribution methods fail to capture diverse, real-time responses from mobile-first users

### User Need

**For survey respondents:**
- A seamless, mobile-native interface for completing surveys on-the-go
- Location-based discovery of relevant surveys in their immediate area
- Instant rewards and points system to incentivize participation
- Quick access to survey history and earned rewards

**For survey creators:**
- Precise geo-targeting to reach users at specific locations (e.g., campus dining halls, retail stores)
- Higher response rates through mobile optimization and instant incentives
- Diverse, context-relevant sample data tied to real-world locations
- Real-time feedback from engaged mobile users

### Why It Matters

Mobile-first users increasingly expect location-aware, gamified experiences. By combining **geo-targeting**, **instant rewards**, and **modern UI**, QZone addresses the engagement gap that plagues traditional survey tools. This matters because:
- **Better data quality** — Location context ensures responses are relevant and timely
- **Higher participation** — Mobile-native design and rewards reduce drop-off rates
- **Diverse samples** — Location-based distribution reaches users who traditional methods miss
- **Real-world impact** — Businesses and researchers can collect actionable feedback tied to specific physical locations

---

## Build & Run

1. **Prerequisites**

   - Android Studio Ladybug or newer (ships with JDK 17).
   - Android SDK 34 installed (`Tools ▸ SDK Manager`).
   - Firebase project with Email/Password auth enabled.
   
2. **Clone & Configure**

   ```bash
   git clone https://github.com/Spectual/QZone.git
   cd Qzone/Qzone
   ```

   - Download Firebase `google-services.json` and place it in `Qzone/app/`.
   
3. **Gradle Sync**
   
   - Open the `Qzone` module in Android Studio.
   - Let Gradle sync; if prompted, accept the Android SDK licenses.

4. **Run / Build**

   - Select the `app` configuration.
   - Use a device/emulator running Android 8.0 (API 26) or higher.
   - Press **Run** (or build via `./gradlew assembleDebug`). First launch lands on the sign-in screen—use an existing Firebase user or create one via the register flow.

## Features

### 🔐 Authentication & Onboarding
- **Multiple sign-in options** — Email/password registration and login via Firebase Auth, Google Sign-In OAuth, and phone number authentication
- **Seamless token management** — Automatic Firebase ID token exchange for backend access tokens with refresh token support
- **Session persistence** — Cached authentication state enables auto-login on app restart
- **Secure backend integration** — All authenticated API requests include Bearer tokens via `AuthTokenProvider`

### 📍 Location-Based Discovery
- **GPS-powered feed** — Automatically fetches nearby surveys from `/api/location/nearby` based on user's current location
- **Interactive map view** — Google Maps integration displays survey locations as markers with titles, descriptions, and distance calculations
- **Smart categorization** — Feed separates partially completed and fully completed surveys into distinct sections
- **Shake-to-refresh** — Accelerometer-based gesture detection (1500 m/s² threshold) with cooldown to quickly refresh the survey feed
- **Offline fallback** — Gracefully falls back to cached surveys when GPS is unavailable or location services are disabled
- **Coordinate conversion** — Automatic WGS-84 to GCJ-02 conversion for accurate location display in China

### 📝 Survey Experience
- **Multi-question types** — Supports single choice, multiple choice, and text input questions with required field validation
- **Progress tracking** — Visual progress indicator shows completion status and saves answers incrementally
- **Navigation controls** — Previous/Next buttons for easy question navigation with validation before submission
- **Answer persistence** — Automatically caches survey progress to Room database for offline completion
- **Submission flow** — Validates required questions, submits responses to backend, and displays earned points upon completion
- **Survey history** — View past completions with search and filter capabilities

### 🎁 Rewards & Gamification
- **Points system** — Earn points for each completed survey, displayed prominently in profile
- **Reward catalog** — Browse available coupons and rewards with point costs and redemption terms
- **Redemption wallet** — Track redeemed rewards with redemption timestamps in the profile wallet view
- **Reward details** — Detailed view for each reward showing terms, conditions, and redemption options

### 👤 User Profile & Settings
- **Comprehensive profile** — Displays rank, total points, current balance
- **Avatar management** — Upload and update profile pictures stored remotely with local caching for offline display
- **Profile editing** — Edit display name, email, and country/region with backend synchronization
- **Activity history** — View survey completion history with filtering and search functionality
- **Redemption tracking** — Wallet view shows all redeemed rewards with redemption dates
- **Dark mode toggle** — Manual theme switching with automatic system preference detection

### 🎨 Modern UI/UX
- **Material 3 design** — Custom theme with typography, color schemes, and shape definitions following Material Design 3 guidelines
- **Responsive layouts** — Adaptive UI that works seamlessly across different screen sizes using Compose layouts
- **Dark mode support** — Automatic theme switching with dynamic color support on Android 12+ devices
- **Smooth animations** — Transitions and state changes with Compose's built-in animation APIs
- **Accessibility** — Clean layouts, readable typography, and proper content descriptions for screen readers
- **Bottom navigation** — Intuitive navigation bar with Home, Map, History, Rewards, and Profile sections

### 🖥️ Java Backend API
- **RESTful architecture** — Custom Java backend server providing comprehensive REST APIs for all app functionality (documented in `API_DOCUMENTATION.md`)
- **Unified response format** — Standardized `Result<T>` response structure with consistent error handling across all endpoints
- **Authentication & authorization** — Bearer token-based authentication with Firebase token exchange, supporting email/password, Google Sign-In, and phone authentication
- **User management** — Complete user lifecycle management including registration, login, profile updates, avatar upload, and session management
- **Survey CRUD operations** — Full survey management with create, read, update, delete operations, question/option management, and status control
- **Response submission** — Secure answer submission with automatic point calculation and reward distribution upon survey completion
- **Location services** — Nearby survey discovery API (`/api/location/nearby`) with radius-based filtering and location-based survey retrieval
- **Rewards & coupons** — Coupon catalog management, redemption tracking, and point balance synchronization
- **Admin interface** — Separate admin endpoints for survey creators to manage surveys, view analytics, and control system settings
- **Swagger documentation** — Interactive API documentation available at `/swagger-ui/index.html` for easy testing and integration
- **Points system** — Automatic point calculation based on survey completion

## Debugging & Testing Strategy

### Debugging Strategy

We follow a **systematic debugging loop** instead of relying on `println` statements, which are unreliable in Compose's declarative UI and coroutine-based async flows.

**1. Reproduce consistently** — Document exact steps to trigger the issue across devices/network states.

**2. Localize the problem** — Use breakpoints and strategic logging (`QLog`) to narrow down the failure scope. Our `ApiCallLoggingInterceptor` tracks network requests, while ViewModels log state transitions and coroutine launches.

**3. Generate hypothesis** — Ask targeted questions: Why is state invalid? Why did recomposition fire twice? Why is the list index out of bounds?

**4. Test with debugger** — Use Android Studio's debugger to inspect variables, call stack, and coroutine dispatcher panels. `QLog` provides context, but the debugger reveals root causes.

**5. Fix root cause** — Address the underlying data flow issue (e.g., why state became invalid) rather than adding defensive null checks everywhere.

**6. Verify & prevent regression** — Re-run reproduction steps and consider adding unit/UI tests for critical paths.

### Crash Logging

Stack traces point to the exact line where crashes occur. We log:
- **API calls** — Method, URL, status, latency via `ApiCallLoggingInterceptor`
- **ViewModel state changes** — Flow emissions, loading/success/error transitions
- **Navigation** — Route transitions logged in `QzoneNavHost`
- **Coroutines** — Launch/completion/exception paths in async flows
- **Sensors** — Location permission checks, GPS state, accelerometer thresholds

We avoid logging sensitive data (passwords, tokens, PII) and keep payloads concise.

### Logging & Diagnostics

- **Central helper** — `util/QLog` gates verbose logs using the app’s `ApplicationInfo.FLAG_DEBUGGABLE` flag and lazy lambdas, so we can sprinkle diagnostics freely without impacting release APK size or CPU.
- **API calls** — `ApiCallLoggingInterceptor` emits one-line entries for every Retrofit request (method, URL, status, latency) before OkHttp’s body logger runs, so we can correlate failures quickly.
- **Database lookups** — `LocalSurveyRepository` now logs each Room insert/query/delete (including counts and IDs), giving us a clear audit trail for cache hits and migrations.
- **Navigation & ViewModels** — `QzoneNavHost` plus `QzoneAppState.navigateTopLevel` log every route transition, and key ViewModels (auth, feed, survey, profile, history, rewards) log Flow emissions, coroutine launches, and terminal states (success/error) for easier triage.
- **Sensors & permissions** — `LocationRepositoryImpl` reports permission checks, provider state, and raw/converted coordinates, while `ShakeDetector` logs acceleration thresholds/cooldowns so we know when shake-to-refresh fires.
- **Async flows** — Feed/Profile/History/Rewards all log when their `Flow` collectors deliver data or when background refreshes (location, survey history, reward redemption) start/finish, giving visibility into coroutine scheduling.

### Testing Strategy

**Manual smoke testing** — Every feature branch is exercised end-to-end on a Pixel 7 emulator and a physical realmeGT neo android 11 device. We test:
- **Authentication flows** — Email/password sign-in, Google Sign-In, phone authentication, and registration with error handling
- **Survey workflow** — Feed loading, survey detail navigation, question answering (single/multiple choice, text input), progress saving, and submission with point rewards
- **Location features** — GPS permission requests, nearby survey fetching, map view with markers, and location-based feed refresh
- **Rewards system** — Point balance updates, reward catalog browsing, redemption flow, and wallet tracking
- **Profile management** — Profile viewing, editing (name, email, country), avatar upload, history filtering, and settings
- **Navigation** — Bottom navigation transitions, deep linking, and back stack management
- **Error handling** — Network failures, API errors, permission denials, and offline scenarios

We rely on verbose Logcat output (via `QLog` and `ApiCallLoggingInterceptor`) and in-app snackbars to verify success paths and failure fallbacks. All state transitions, API calls, and navigation events are logged for easy debugging.

**Sensor & hardware testing** — For features tied to device hardware, we perform targeted checks:
- **GPS/Location** — Toggle GPS on/off, test with network location only, verify coordinate conversion (WGS-84 to GCJ-02), and confirm graceful fallback to cached surveys when location is unavailable
- **Accelerometer** — Simulate shake gestures via Android Studio's "Virtual Sensors" panel to verify shake-to-refresh threshold (1500 m/s²) and cooldown period (1500ms) prevent spam triggers
- **Permission flows** — Test location permission denial, re-request flows, and app behavior when permissions are revoked

**API integration testing** — We verify backend integration by:
- Testing all authenticated endpoints with valid/invalid tokens
- Verifying Firebase token exchange and refresh token handling
- Confirming survey submission, response history, and point calculation
- Testing location-based API calls with various coordinates and radius values
- Validating error responses and network timeout scenarios

---

## Technical Implementation

### Architecture

QZone follows a **clean MVVM architecture** with unidirectional data flow and clear separation of concerns.

**ViewModels & StateFlow** — Each feature (Auth, Survey, Feed, Profile, Rewards, History, Map) has a dedicated ViewModel that manages UI state using `StateFlow<UiState>`. ViewModels expose immutable `StateFlow` that Compose UI observes via `collectAsState()`. For example, `SurveyViewModel` maintains `SurveyUiState` with survey data, current question index, answers map, and submission status. State changes flow unidirectionally: user actions trigger ViewModel methods (e.g., `onAnswerChanged()`, `submit()`), which update internal `MutableStateFlow`, causing UI to recompose automatically.

**Unidirectional Data Flow** — The architecture enforces a strict one-way data flow: UI → ViewModel → Repository → Data Source. ViewModels never directly mutate UI state; instead, they update their internal state, which flows down to Compose UI. This eliminates state synchronization issues and makes the app predictable and testable.

**Repository Pattern** — ViewModels depend on repository interfaces (`SurveyRepository`, `UserRepository`, `LocationRepository`, `RewardRepository`) defined in the `domain` layer, not concrete implementations. The `data` layer provides implementations (`ApiSurveyRepository`, `FirebaseUserRepository`, `LocationRepositoryImpl`). This abstraction enables easy testing with fake repositories and clean separation between business logic and data access.

**Dependency Injection** — A lightweight `AppContainer` (defined in `QzoneApp`) provides repository instances to ViewModels via factory methods. `MainActivity` accesses the container and passes repositories to `rememberQzoneAppState()`, which wires them into the navigation graph. This approach keeps the architecture testable and maintainable without heavy DI frameworks.

**Coroutines & Flow** — All async operations use Kotlin Coroutines with `viewModelScope`. ViewModels collect from repository `Flow`s (e.g., `userRepository.currentUser`, `surveyRepository.nearbySurveys`) and combine multiple flows when needed (e.g., `ProfileViewModel` combines user and rewards flows). Error handling uses `runCatching` and proper exception propagation.

### Integration

**External APIs:**

- **Firebase Authentication** — `FirebaseUserRepository` handles email/password and Google Sign-In authentication. After Firebase authentication succeeds, it exchanges the Firebase ID token for backend access/refresh tokens via `/api/user/login` or `/api/user/third-party`. Tokens are cached locally using `UserLocalStorage` and refreshed automatically via `AuthTokenProvider`.

- **Google Identity Services** — Integrated for seamless Google Sign-In OAuth flow. The app requests Google ID token, which is then sent to Firebase for authentication, followed by backend token exchange.

- **Google Maps SDK for Android** — `NearbySurveyMapScreen` uses `maps-compose` library to display an interactive map with nearby survey locations. `GoogleMap` composable shows user location and markers for each nearby location fetched from `/api/location/nearby`. The map camera animates to the user's current location when available, and markers display survey titles and distances.

- **Backend REST API** — Custom Java backend (documented in `API_DOCUMENTATION.md`) integrated via Retrofit + Moshi. `QzoneApiClient` configures OkHttp with `ApiCallLoggingInterceptor` for request/response logging. Key endpoints: `/api/user/login`, `/api/user/register`, `/api/survey/submit`, `/api/location/nearby`, `/api/user/user/me`. All authenticated requests include `Authorization: Bearer {accessToken}` header via `AuthTokenProvider`.

**Onboard Sensors:**

- **GPS / Network Location** — `LocationRepositoryImpl` uses Google Play Services `FusedLocationProviderClient` to fetch user coordinates with high accuracy. It handles permission requests, provider state changes (GPS enabled/disabled), and gracefully falls back to cached surveys when location is unavailable. The implementation includes coordinate conversion (WGS-84 to GCJ-02 for China) and reverse geocoding via Android `Geocoder` to get human-readable addresses.

- **Accelerometer** — `ShakeDetector` monitors device acceleration via `SensorManager` to trigger feed refresh. It implements threshold-based detection (1500 m/s²) with a cooldown period (1500ms) to prevent multiple triggers from a single shake. The detector is registered in `FeedScreen` using `DisposableEffect` and automatically unregisters when the screen is disposed.

**Data Storage:**

- **Room Database** — `QzoneDatabase` provides local caching of surveys, questions, options, and nearby locations via DAOs. `LocalSurveyRepository` wraps Room operations and logs all insert/query/delete operations for debugging. The database enables offline access to cached surveys and supports location-based queries.

- **Proto DataStore** — User preferences and local settings (planned for future use).

- **Firebase Firestore** — Remote user profile and authentication state managed by Firebase.

### Jetpack Compose Usage

The entire UI is built with **Jetpack Compose** following Material 3 design principles and best practices.

**Material 3 Design System** — Custom theme defined in `ui/theme/Theme.kt` with typography (`QzoneTypography`), color schemes (light/dark with dynamic color support on Android 12+), and shape definitions. The theme automatically adapts to system dark mode preferences.

**State-Driven Recomposition** — UI automatically updates when ViewModel `StateFlow` emits new values. Composables like `SurveyScreen`, `FeedScreen`, and `ProfileScreen` use `collectAsState()` to observe state changes and recompose only when necessary. This eliminates manual UI updates and ensures UI always reflects the current state.

**Navigation Compose** — Type-safe navigation graph managed by `QzoneNavHost` with route definitions in `QzoneDestinations`. The navigation handles screen transitions, argument passing (e.g., survey ID), and bottom navigation bar visibility. `QzoneAppState` manages navigation state and provides methods like `navigateTopLevel()` for programmatic navigation.

**Responsive Layouts** — Adaptive UI works across different screen sizes using `Column`, `Row`, `LazyColumn`, and constraint-based layouts. `LazyColumn` is used for scrollable lists (feed, history, rewards) with proper item keys for efficient recomposition. Window insets are handled via `WindowInsets.statusBars` for proper padding.

**Composable Architecture** — Feature-scoped composables (e.g., `SurveyScreen`, `FeedScreen`, `NearbySurveyMapScreen`) with clear separation of concerns. Reusable components like `QzoneElevatedSurface` and `QzoneTag` are defined in `ui/components` for consistency. Each screen composable receives state and callbacks as parameters, making them testable and independent of ViewModel implementation details.

**Dark Mode Support** — Automatic theme switching based on system preferences with manual toggle option. The theme uses Material 3's dynamic color system on supported devices, providing a cohesive visual experience.