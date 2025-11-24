# QZone — Location-Based Survey App (Android, Kotlin)

> A mobile-first survey platform that uses **geo-targeting** and **rewards system** to boost response rate and sample diversity. Built with **Jetpack Compose + MVVM**.

---

## Team
#### Xuetong Fu – Backend / API
- Designed and documented the Java API (auth, survey CRUD, response history, rewards).
- Implemented Firebase token exchange, login/register logic, and unified local storage using Room.

#### Yifei Bao – Survey & Rewards
- Built the survey flow (Feed → Detail → Submit), question UI, answer saving, and shake-to-refresh.
- Implemented rewards flow: points updates, catalog UI, wallet view, and basic redemption logic.

#### Zhenyang Qian – UI/UX & Account
- Created the Compose design system (theme, typography, card components) and overall UI polish.
- Implemented history filters, profile editing and avatar upload, dark mode, and navigation structure.

> As a collaborative team of three, we will share all design and architecture decisions. To ensure progress, we have assigned primary focus areas, but we will all contribute to the entire codebase through pair programming and code reviews.

---

## Project Description
**QZone** lets users discover and complete surveys tied to their **current location**. Users open the app to see nearby surveys, answer with a **modern multimedia interface**, and earn **points** upon completion.  
For creators (future web portal), QZone enables **precise geo-targeting** to collect diverse and context-relevant responses.

**Why now?** Existing tools (e.g., web forms) are clunky on mobile and lack location awareness or instant incentives, causing high drop-off and low engagement.

---

## Features

### Onboarding & Authentication
- Supports email/password sign-up and login using Firebase Auth.
- Google Sign-In is integrated and follows the same authentication flow.
- All authenticated requests use refreshed access tokens from Firebase.

### Location-Based Survey Feed
- Uses the device’s GPS to request nearby surveys from the `/api/location/nearby` endpoint.
- Show whether a survey is partially completed or fully completed in different sections.
- Users can shake the phone (via Accelerometer detection) to refresh the feed quickly.

### Rewards & Points
- Completing a survey adds reward points to the user’s account.
- The rewards screen lets users redeem points for available coupons.

### User Profile
- The profile page displays rank, point balance, redemption wallet, and recent activity.
- Users can upload an avatar, which is stored remotely and cached locally for offline use.

### UI / UX
- Built with Jetpack Compose for consistent styling and layout across all screens.
- Includes simple animations and supports light/dark mode automatically.
- Uses clean layouts and readable typography to make the app easy to use.

## Testing Strategy

- **Manual smoke testing (emulator + physical devices)**  
  Every feature branch is exercised end-to-end on a Pixel 7 emulator and a physical realmeGT neo android 11: sign-in/register flows, GPS permission prompts, shake-to-refresh, survey answering, reward redemption stubs, and profile editing. We rely on verbose Logcat + in-app snackbars to verify success paths and failure fallbacks.

- **Sensor & location sanity checks**  
  For features tied to hardware, we toggle GPS on/off and simulate accelerometer input (via the Android Studio “Virtual Sensors” panel) to confirm graceful degradation: feed refresh falls back to cached surveys when GPS is unavailable, and the shake detector respects its cooldown/threshold so it does not spam refreshes.

---

## Tech Stack
- **External APIs:** Google Identity Services, Firebase Authentication / Firestore, Google Maps SDK for Android, Our own backend in API_DOCUMENTATION.
- **Onboard Sensors:** GPS / Network Location, Accelerometer
- **Data Storage:** Proto DataStore, Firebase Firestore.


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

---

## Recap of Proposal (What's changed)

- **Authentication**
  - Email/password registration and login via Firebase.
  - Backend token exchange (access + refresh) logged to Logcat (`QzoneAuth`).
  
- **Survey Feed**
  - Jetpack Navigation drives transitions between sign-in, feed, survey, and profile.
  - Feed screen displays a mock “Campus Dining Satisfaction” survey; payload logged by `PlaceholderSurveyRepo`.
  
- **Survey Flow**
  - `SurveyViewModel` fetches survey details, tracks answers, and submits responses to the backend (`submitResponses`).
  - Supports single choice, multiple choice, and text questions; required questions flagged in state.
  
- **Profile & History**
  - `ProfileViewModel` combines `UserRepository.currentUser` with reward inventory to show progress and next reward cost.
  - History screen filters survey completions by query.
  
- **Rewards**
  - Placeholder reward catalog rendered with Compose cards; reward detail screen displays terms and redeem CTA stub.
  - Full redemption workflow deferred to next milestone; exploring partnership model as part of the business iteration.
  
- **Architecture**
  - MVVM with `StateFlow` in every `ViewModel`, Compose UI collects state.
  - Repositories decoupled behind domain interfaces; Firebase user repo already integrated, surveys/rewards still mocked.
  - Retrofit + Moshi client ready for survey/reward APIs when backend endpoints solidify.
  
- **Backend**
  - Since the proposal the Java backend now exposes login, register, survey CRUD, and “get nearby surveys” endpoints.
  - Mobile app already consumes `/api/user/login`, `/api/user/register`, and `/api/survey/submit`; feed currently uses mock data but the Retrofit client is ready to swap in the live `nearby` endpoint.
