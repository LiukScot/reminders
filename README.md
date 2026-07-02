# Reminders

Android app for managing reminders/tasks, inspired by Apple Reminders but with
AI-native voice input and day/week timeline views.

Born after research into existing alternatives (TickTick, Todoist, Any.do, Vikunja) —
none covered all of: drag&drop timeline UI, natural language in Italian, no paywall
for basic features. Details in [docs/requirements.md](docs/requirements.md).

## Stack

- Kotlin + Jetpack Compose (native Android)
- Material 3, dynamic color (Material You)
- Voice-to-task via Gemini API (audio → structured JSON: title/date/time/recurrence)

## Status

Initial setup. No code yet.
