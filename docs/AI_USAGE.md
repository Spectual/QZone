# AI Usage & Reflection

## A. AI Reflection Report
- **Tools used**
  - GitHub Copilot (inline completions, chat) inside Android Studio/VS Code for Kotlin/Compose.
  - ChatGPT (architectural reasoning, API usage comparisons, copy refinement).

- **What they were used for (project-specific)**
  - Code (UI): drafted the Compose bottom-sheet map experience in `com.qzone.feature.feed.ui.FeedScreen` (the "View my location on map" chip, `MapViewContent`, markers, camera centering), then hand-tuned padding, shapes, and permission overlays. Also suggested copy for the location chips and map hints.
  - Code (logic): assisted with permission + location flow in `FeedViewModel.refreshWithLocation` and `loadNearbyLocationsWithCoordinates` (guarding null/permission-denied cases, sequencing refresh before map). Helped adjust navigation wiring for location-enabled feed entry in `QzoneNavHost`.
  - Refactors: proposed state hoisting and `remember`/`LaunchedEffect` usage for the map sheet and shake-to-refresh areas; trimmed unused imports and tightened nullability checks around `currentLocation` and nearby lists.
  - Testing/debug: used AI to reason through potential crashes when location permission is missing while opening the map sheet, and to add guards/fallback camera targets; asked AI for test ideas on permission denial, empty nearby list, and stale location to validate the feed/map behavior. AI suggestions were reviewed and only safe changes were applied.
  - Documentation: seeded drafts for README/architecture phrasing and this AI usage note; Copilot provided inline KDoc/comment stubs that were rewritten to match actual logic and tone.

- **Where AI was helpful vs. misleading**
  - Helpful: quick Compose scaffolds with sensible defaults (layout spacing, `remember` patterns), Retrofit/Flow wiring templates, sample permission request flows, and wording for UX hints.
  - Misleading/pitfalls: suggested deprecated Compose APIs; location flows without runtime permission checks; examples that bypassed app logging (`QLog`) or our naming; overly generic network error handling that hid exceptions; occasional China-geo edge cases ignored in location handling (we kept our converter).

- **Review & refactor practices**
  - AI-generated block was manually reviewed, trimmed, or rewritten to match Kotlin/Compose idioms (`remember`, `LaunchedEffect`, state hoisting) and project layering (UI -> ViewModel -> Repository).
  - Security/privacy: ensured location use stays minimal (no unnecessary persistence), kept permission gating, and avoided logging sensitive lat/lng beyond what was already in app logs; checked that map API keys stay in resources, not hardcoded.
  - Quality: reran lint/compile after AI edits; removed dead parameters/imports; enforced nullability checks and default fallbacks; reworded UX strings to be specific and concise.
  - Style: aligned with `QLog` usage, spacing/shape choices consistent with existing Compose styling; comments added only for non-obvious flows (e.g., permission gating in the map sheet).

