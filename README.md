# Reminders

App Android per la gestione di promemoria/task, ispirata a Apple Reminders ma con
input vocale AI-native e viste a timeline per giorno/settimana.

Nato dopo una ricerca su alternative esistenti (TickTick, Todoist, Any.do, Vikunja) —
nessuna copriva insieme: UI a timeline drag&drop, linguaggio naturale in italiano,
niente paywall per feature base. Dettagli in [docs/requirements.md](docs/requirements.md).

## Stack

- Kotlin + Jetpack Compose (nativo Android)
- Material 3, dynamic color (Material You)
- Voice-to-task via Gemini API (audio → JSON strutturato: titolo/data/ora/ricorrenza)

## Stato

Setup iniziale. Nessun codice ancora.
