# Wear OS companion app

Scope of the first watch release, and the screens it needs mockups for.

Tracked by [#39](https://github.com/LiukScot/reminders/issues/39) (v1) and
[#40](https://github.com/LiukScot/reminders/issues/40) (v2, daily view).

## What v1 does

Three things, nothing else:

1. **AI voice capture** — the same flow as the phone's in-app microphone
   (see [architecture.md](architecture.md) → Voice / AI): speak, live
   transcript, transcript text goes to the configured AI provider, a
   structured task comes back and is saved.
2. **Snooze a reminder from the watch**, with the *same* options the phone
   offers — no reduced set. The options are defined once in `SnoozeOption.kt`
   (`snoozeChoices`): +15m, +1h, Later, Tomorrow, This weekend, the user's
   Quick snooze delay, and Pick date & time.
3. **A watch face complication that launches voice capture** — one tap from
   the watch face to the microphone, without opening the app first.

Explicitly not in v1: browsing lists, reading the day's reminders, editing a
task, any settings screen. The watch reads the phone's AI-provider choice and
quick-snooze delay; it never asks for them itself.

## Screens needing a mockup

Nine. The app has no home screen of its own — the launcher icon opens
straight into Voice / Idle, because idle voice *is* the app in v1.

### Voice capture

| # | Screen | Content |
|---|---|---|
| 1 | Voice — Idle | Large mic button, one-line hint. Entry point from launcher and from the complication. |
| 2 | Voice — Listening | Live partial transcript, audio-level indicator, tap to finish. |
| 3 | Voice — Processing | Transcript frozen, provider call in flight. |
| 4 | Voice — Result | Parsed title, due date/time, target list. Save / Retry / Discard. |
| 5 | Voice — Blocked | One layout covering the dead ends: mic permission denied, no AI key set on the phone, provider error, no connectivity. Each states what to do and offers Retry where retrying can help. |

States 1–5 mirror `VoiceCaptureState` on the phone (`Idle`, `Listening`,
`Processing`, `Result`, `MissingKey`, `Error`) so the watch does not invent a
second state machine — the last two collapse into screen 5.

### Snooze

| # | Screen | Content |
|---|---|---|
| 6 | Reminder notification | Title, due time, actions: Complete, Snooze… |
| 7 | Snooze picker | Scrollable list of every `snoozeChoices` option with its resulting time, same labels as the phone. |
| 8 | Snooze — pick date & time | Only reached from the "Pick date & time" option. Wear date picker then time picker. |
| 9 | Snooze — confirmation | Brief "Snoozed to …" before dismissing. |

### Complication

No mockup screen — a complication is a data slot, not a page. It needs
rendered assets for the types it supports (icon-only, and short-text with a
mic glyph), which the mockup set should include as a small sheet rather than
a full screen.

## Design

Wear Material (`androidx.wear.compose.material3`), not the phone's Material 3
components — round screens, rotary input and the swipe-to-dismiss gesture are
not things the phone layouts survive. The colours, type scale and radii still
come from [design-system.md](design-system.md); only the components change.

Rotary (crown/bezel) scrolling is mandatory on the snooze picker and the
result screen. A Wear list that only responds to touch is broken on half the
hardware.

## Notifications and sync

The phone currently posts reminder notifications that Wear OS bridges to the
watch automatically. That is enough for a Complete action, which needs no UI,
but not for snooze: a bridged action runs on the *phone*, so tapping Snooze…
would open the picker there and defeat the point.

So v1 stops bridging reminder notifications (`setLocalOnly` on the phone
side, plus a `BridgingConfig` in the watch app keyed by `setBridgeTag`) and
the watch app posts its own notification, driven by a Data Layer message the
phone sends when a reminder fires. Its actions then target watch activities
and the picker runs on the watch.

Everything else — the task created by voice, the new due time chosen by
snooze — travels over the **Wear OS Data Layer API**. There is no backend
(see [architecture.md](architecture.md) → Data), so phone↔watch is the only
link available, and the phone's Room database stays the single source of
truth. The watch holds no database: it sends intents and receives what it
needs to display.

**The watch does not call the AI provider itself — it relays through the
phone.** The API key stays where it already is, in the phone's
`EncryptedSharedPreferences`, instead of being copied into a second encrypted
store on the watch. The message to the phone is needed regardless, since the
resulting task has to reach Room, so relaying the transcript adds no extra
round trip. The one case this loses — an LTE watch used away from the phone —
does not apply to the target hardware, which has no LTE.

**The watch transcribes; the phone parses.** Checked on the target hardware
(Pixel Watch 4, Wear OS on Android 17): one `RecognitionService` is
registered and set as the system default —
`com.google.android.tts/…GoogleTTSRecognitionService`, a Wear-specific build
of the Google speech package, privileged and part of the system image. The
microphone feature is present. So `SpeechRecognizer` is available and the
watch does not need to ship audio to the phone just to get text.

Note that the watch has no Android System Intelligence
(`com.google.android.as`) and `on_device_speech_recognition_service` is
unset, so `createOnDeviceSpeechRecognizer()` — the phone's explicitly
on-device path — is not expected to work there. Use plain
`createSpeechRecognizer()`, which resolves to the default service above.
Whether that service transcribes locally or reaches the network is not
established; if it turns out to need connectivity, nothing about the
architecture changes, only the offline story.

## Build

A separate `:wear` Gradle module. Shared with `:app`: the pure data logic
that has no Android or UI dependency — `SnoozeOption.kt` above all, so the
two snooze pickers cannot drift apart. Not shared: any composable. That
sharing is what forces the first extraction of a common module in this
project; keep it to the smallest set of files that genuinely need it.
