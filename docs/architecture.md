# Architettura tecnica

## Linguaggio e UI

- **Kotlin**, unico linguaggio del progetto.
- **Jetpack Compose** per tutta la UI — nessuna View/XML legacy.
- Material 3 (`androidx.compose.material3`) come libreria di componenti — vedi
  [design-system.md](design-system.md) per il tema.

## Dati

- **Solo storage locale** per ora (deciso in [requirements.md](requirements.md)),
  niente backend/sync multi-dispositivo.
- Persistenza: **Room** (libreria ufficiale AndroidX sopra SQLite) — standard de
  facto per storage locale strutturato su Android, nessun motivo per scrivere un
  wrapper SQLite a mano o introdurre un DB esterno per un'app mono-utente offline.
- Conseguenza pratica: serve una feature di export/backup manuale (vedi
  [features.md](features.md) → "Dati e backup") perché senza cloud sync i dati
  vivono solo sul dispositivo.

## Voce / AI

- Gemini API per voice-to-task (audio → JSON strutturato titolo/data/ora/ricorrenza),
  motivazione completa in [requirements.md](requirements.md).
- Chiamata diretta dall'app al momento; da rivalutare se serve un layer di
  backend/proxy (es. per non esporre l'API key nel client — **punto da approfondire
  prima di scrivere il codice della feature voce**, non ancora deciso).

## Da definire più avanti

- Struttura moduli (single-module vs multi-module Gradle) — rimandato: con un'app di
  queste dimensioni un singolo modulo `app` basta finché non diventa scomodo.
- Dependency injection: valutare se serve Hilt o se basta injection manuale data la
  scala del progetto.
