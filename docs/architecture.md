# Technical architecture

## Language and UI

- **Kotlin**, the project's only language.
- **Jetpack Compose** for all UI — no legacy View/XML.
- Material 3 (`androidx.compose.material3`) as the component library — see
  [design-system.md](design-system.md) for the theme.

## Data

- **Local storage only** for v1 (decided in [requirements.md](requirements.md)),
  no backend/multi-device sync. A self-hosted webapp with a local/server sync
  toggle is planned for v2 (see [features.md](features.md) → "Collaboration") —
  not yet designed, avoid modeling the local store in a way that assumes a
  particular sync protocol until that's scoped.
- Persistence: **Room** (official AndroidX library on top of SQLite) — the de
  facto standard for structured local storage on Android, no reason to hand-write
  a SQLite wrapper or bring in an external DB for a single-user offline app.
- Practical consequence: a manual export/backup feature is needed (see
  [features.md](features.md) → "Data and backup") because without cloud sync the
  data lives only on the device.

## Voice / AI

- Gemini API for voice-to-task (audio → structured JSON with title/date/time/
  recurrence), full reasoning in [requirements.md](requirements.md).
- Called directly from the app for now; to be reevaluated if a backend/proxy layer
  is needed (e.g. to avoid exposing the API key in the client — **point to
  investigate before writing the voice feature's code**, not yet decided).

## To define later

- Module structure (single-module vs multi-module Gradle) — deferred: for an app
  of this size a single `app` module is enough until it becomes inconvenient.
- Dependency injection: evaluate whether Hilt is needed or manual injection is
  enough given the project's scale.
