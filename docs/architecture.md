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

- Voice capture is **on-device speech recognition (Android `SpeechRecognizer`)**
  for a live transcript (the mockup shows the text growing as you speak), then the
  finished transcript **text** is sent to the AI provider for structured parsing
  (title/date/time/recurrence). This replaces the earlier audio-native plan
  (Voxtral/Gemini audio-in): `SpeechRecognizer` is a free OS service with no model
  to host, so it doesn't reintroduce the maintenance burden the audio-native choice
  was avoiding, and it's the only way to get the live transcript the mockup wants.
- The AI provider is user-configurable (Settings → AI model), not hardcoded —
  full reasoning in [requirements.md](requirements.md). Default: Mistral. Second
  option: Google (Gemini). Both take the transcript text and return the same
  JSON shape, so the app-side integration is a single interface with two
  implementations, not two divergent code paths.
- Selection persisted the same way as the default-list setting (DataStore
  Preferences, see `SettingsRepository`) — a single preference key, no new
  storage mechanism.
- **Decided (#19): called directly from the app, no backend proxy.** This is a
  single-user personal app: the key is the user's own free-tier key on the user's
  own device, not a shared secret embedded in a widely-distributed binary, so the
  "exposed key in client" risk that motivates a proxy does not apply. A proxy would
  mean standing up and maintaining a server that otherwise doesn't exist, for no
  concrete gain. Keys are stored on-device with `EncryptedSharedPreferences`
  (androidx.security-crypto), one entry per provider. Revisit only if the app ever
  ships to third parties or a shared server appears (see self-hosting, #33).

## To define later

- Module structure (single-module vs multi-module Gradle) — deferred: for an app
  of this size a single `app` module is enough until it becomes inconvenient.
- Dependency injection: evaluate whether Hilt is needed or manual injection is
  enough given the project's scale.
