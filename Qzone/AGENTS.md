# Repository Guidelines

## Project Structure & Module Organization
- Root contains the README for product context, `imgs/` for UX references, `deliverables/` for proposal PDFs, and `Qzone/` reserved for the Android Studio project.  
- When the app module is added, commit the Gradle wrapper and keep the main source under `Qzone/app/src/main/java/com/qzone/...`. Compose UI lives in `.../ui`, domain logic in `.../domain`, and data access in `.../data`.  
- Store shared resources (colors, strings, themes) in `app/src/main/res`. Add helper scripts or mock payloads under a `tools/` folder to avoid cluttering the app module. Example: `Qzone/app/src/main/java/com/qzone/survey/SurveyViewModel.kt`.

## Build, Test, and Development Commands
- `./gradlew assembleDebug` — build the debug APK; verify output in `app/build/outputs/apk/debug/`.  
- `./gradlew lintKotlin detekt` — run formatting and static analysis once lint configs are committed; pair with Android Studio's "Inspect Code" for quick feedback.  
- `./gradlew testDebugUnitTest` — execute JVM unit tests.  
- `./gradlew connectedDebugAndroidTest` — launch instrumentation tests on an emulator or device; run `adb devices` first to confirm connectivity.

## Coding Style & Naming Conventions
- Kotlin uses 4-space indentation, trailing commas, and ≤100 character lines. Prefer expression bodies, immutability (`val`), and descriptive parameter names.  
- Compose composables follow `VerbNoun` naming (e.g., `ShowSurveyCard`). ViewModels end with `ViewModel`; repositories with `Repository`.  
- Structure packages by layer (`ui`, `domain`, `data`) and keep resource IDs in `snake_case`, prefixed by screen (`survey_submit_cta`).  
- Run `./gradlew ktlintFormat` before committing once the task exists; never edit generated build files manually.

## Testing Guidelines
- Place unit tests in `app/src/test/...`, mirroring package names (`com.qzone.survey`). Name files `<Class>Test` and methods `should...`.  
- Instrumentation tests live in `app/src/androidTest/...`; tag location-aware scenarios with `@SdkSuppress`.  
- Aim for ≥80% coverage on domain logic and attach coverage notes or screenshots to pull requests when functionality shifts.

## Commit & Pull Request Guidelines
- Current history uses short, imperative subjects (`update README`). Continue with ≤72 character headlines and add Conventional Commit prefixes when clear (`feat:`, `fix:`, `chore:`).  
- Each PR needs a concise summary, evidence of testing, and references to roadmap issues (`#12`). Include screenshots or screen captures for UI-impacting changes.  
- Request review from at least one teammate and wait for CI results before merging; rebase instead of merge commits to keep history linear.
