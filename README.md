# QZone — Location-Based Survey App (Android, Kotlin)

> A mobile-first survey platform that uses **geo-targeting** and **rewards system** to boost response rate and sample diversity. Built with **Jetpack Compose + MVVM**.

> **Note**: Initial structure and Build & Run instructions were AI-drafted, then manually refined to match project specifics.

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

## AI Usage

This project utilized AI tools (GitHub Copilot and ChatGPT) during development for code generation, refactoring assistance, and documentation. To maintain transparency, **AI markers have been added throughout the codebase and documentation** to indicate where AI assistance was used:

- **KDoc comments**: Class and method-level documentation with `[AI-assisted]` tags explaining AI contributions
- **Code comments**: Inline comments marking AI-suggested patterns, values, or structures that were tested and validated
- **Documentation**: This README and other project documentation include notes about AI-assisted content

All AI-generated code was manually reviewed, tested, and refined to match project requirements and coding standards. See `deliverables/final_report.md` for detailed information about AI usage in development.

---

Final Report is under ./deliverables folder.
