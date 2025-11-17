# QZone — Location-Based Survey App (Android, Kotlin)

> A mobile-first survey platform that uses **geo-targeting** and **rewards system** to boost response rate and sample diversity. Built with **Jetpack Compose + MVVM**.

---

## Team
\- **Yifei Bao** — Product & UX; user onboarding/auth; profile & basic recommendation design

\- **Xuetong Fu** — Survey engine & data; questionnaire flow; storage & submission

\- **Zhenyang Qian** — Location & geofencing; rewards/points; platform integrations

> As a collaborative team of three, we will share all design and architecture decisions. To ensure progress, we have assigned primary focus areas, but we will all contribute to the entire codebase through pair programming and code reviews.

---

## Project Description
**QZone** lets users discover and complete surveys tied to their **current location**. Users open the app to see nearby surveys, answer with a **modern multimedia interface**, and earn **points** upon completion.  
For creators (future web portal), QZone enables **precise geo-targeting** to collect diverse and context-relevant responses.

**Why now?** Existing tools (e.g., web forms) are clunky on mobile and lack location awareness or instant incentives, causing high drop-off and low engagement.

---

## MVP Features
* **Onboarding & Authentication:** Effortless user registration and login, including third-party support for Google, Meta, and X to minimize friction.
* **Location-Based Survey Feed:** Use the device's GPS to fetch and display a list of surveys relevant to the user's current area. This is the central feature of our app.

* **Reward & Points System:** Users earn a base number of points for every completed survey, with the potential for bonus points set by the survey creator.

* **Multimedia Survey Interface:** A native, intuitive interface for answering various question types, with support for embedded text, images, audio, and video content.

* **User Profile Management:** A section for users to manage basic information, such as their profile picture, region, and personal interests.

* **Modern & Personalized UI/UX:** Built with a focus on beauty and usability, ensuring a branded, consistent design.

**Stretch (if time allows)**: Map explore, profile-based matching, **shake-to-refresh** (gyroscope), in-app redemption placeholder, color-blind friendly theme.

---

## Tech Stack
- **External APIs:** Google Identity Services, Firebase Authentication / Firestore, Google Maps SDK for Android.
- **Onboard Sensors:** GPS / Network Location (additional sensors such as camera/microphone planned).
- **Data Storage:** Proto DataStore, Firebase Firestore, in-memory placeholders for interim demo.


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
  
---

## UI Overview

![overview](./imgs/overview.png)
