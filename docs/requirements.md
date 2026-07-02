# Requirements

## Context

Goal: an experience similar to Apple Reminders on Android, with customization
and AI voice input. Before writing any code, research was done into existing
alternatives (Todoist, TickTick, Any.do, Vikunja, Tasks.org) — none satisfy the
requirements below all at once, hence building from scratch.

Findings from the research that shaped the choices:
- Google Assistant is being discontinued (March 2026), replaced by Gemini — Gemini
  only supports a closed list of linked apps ("Connected Apps"), not generic MCP.
- No third-party app can be reliably registered as the system voice assistant on
  Wear OS/Pixel Watch.
- TickTick has an official free MCP (`mcp.ticktick.com`) but the interface and
  automations (e.g. smart date/time recognition) are limited and English-only.
- Vikunja (self-hosted, community MCP) solves the paywall issue but the interface
  isn't satisfactory.

Conclusion: since no system assistant can be reliably reached, voice input must be
implemented **inside the app** (in-app microphone), not through integration with
external Siri/Gemini assistants.

## Views

- **Day view**: morning / afternoon / evening slots, drag&drop tasks between slots.
- **Week view**: drag&drop tasks between days.
- Other Apple Reminders features: to be mapped one by one (lists, tags, priority,
  sub-tasks, attachments, geolocation, sharing — verify which are actually needed
  before implementing them).

## Natural language

- Smart parsing of date/time/recurrence from the title text.
- Priority: Italian first, English second.

## Voice

- No system assistant (Siri/Gemini) can be reliably reached → in-app microphone.
- **Choice**: Gemini API (audio → structured JSON with title/date/time/recurrence
  in a single call), free tier for personal use.
- Alternative discarded for now: self-hosting a small model (e.g. Whisper) —
  would require two separate pipelines (transcription + NLU parsing) to maintain,
  overkill for a single-user app. Reconsider only if privacy or cost issues emerge.

## Design

- Material Design (Android), Material 3.
- Base color: orange.
- Dynamic color (Material You) from the system — future feature, not in the first
  iteration.

## Tech stack

- Kotlin + Jetpack Compose, native Android (no Flutter/React Native).
- Reason: Material You dynamic color is a native Compose API; drag&drop is more
  direct; no cross-platform plugin to maintain. Accepted trade-off: if iOS is
  needed in the future, it starts from scratch on that platform.
