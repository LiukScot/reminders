# Requisiti

## Contesto

Obiettivo: un'esperienza simile a Apple Reminders su Android, con personalizzazione
e input vocale AI. Prima di scrivere codice è stata fatta una ricerca su alternative
esistenti (Todoist, TickTick, Any.do, Vikunja, Tasks.org) — nessuna soddisfa insieme
i requisiti sotto, quindi si costruisce da zero.

Punti emersi dalla ricerca che hanno guidato le scelte:
- Google Assistant è in dismissione (marzo 2026), sostituito da Gemini — Gemini
  supporta solo una lista chiusa di app collegate ("Connected Apps"), non MCP generico.
- Nessuna app third-party è collegabile come assistente vocale di sistema su
  Wear OS/Pixel Watch in modo verificato.
- TickTick ha un MCP ufficiale gratuito (`mcp.ticktick.com`) ma l'interfaccia e le
  automazioni (es. riconoscimento smart di data/ora) sono limitate e in inglese.
- Vikunja (self-hosted, MCP community) risolve il paywall ma l'interfaccia non
  soddisfa.

Conclusione: dato che nessun assistente di sistema è raggiungibile in modo affidabile,
l'input vocale va implementato **dentro l'app** (microfono in-app), non tramite
integrazione con Siri/Gemini esterni.

## Viste

- **Vista giorno**: fasce mattina / pomeriggio / sera, drag&drop dei task tra fasce.
- **Vista settimana**: drag&drop dei task tra giorni.
- Altre feature di Apple Reminders: da mappare una per una (liste, tag, priorità,
  sub-task, allegati, posizione geografica, condivisione — verificare quali servono
  davvero prima di implementarle).

## Linguaggio naturale

- Parsing smart di data/ora/ricorrenza dal testo del titolo.
- Priorità: italiano prima, inglese poi.

## Voce

- Nessun assistente di sistema (Siri/Gemini) raggiungibile in modo affidabile →
  microfono in-app.
- **Scelta**: Gemini API (audio → JSON strutturato con titolo/data/ora/ricorrenza
  in un'unica chiamata), free tier per uso personale.
- Alternativa scartata per ora: self-hosting di un modello piccolo (es. Whisper) —
  richiederebbe due pipeline separate (trascrizione + parsing NLU) da mantenere,
  overkill per un'app mono-utente. Da riconsiderare solo se emergono problemi di
  privacy o costi.

## Design

- Material Design (Android), Material 3.
- Colore base: arancione.
- Dynamic color (Material You) da sistema — feature futura, non nel primo giro.

## Stack tecnico

- Kotlin + Jetpack Compose, nativo Android (no Flutter/React Native).
- Motivo: Material You dynamic color è API nativa Compose; drag&drop più diretto;
  nessun plugin cross-platform da mantenere. Trade-off accettato: se in futuro serve
  iOS, si riparte da zero su quella piattaforma.
