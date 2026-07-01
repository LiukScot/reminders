# Mappa feature Apple Reminders

Checklist di tutto quello che fa Apple Reminders, con proposta di priorità.
`[MVP]` = necessario per la prima versione usabile, `[v2]` = aggiungibile dopo,
`[skip]` = deliberatamente escluso con motivo.

## Organizzazione

- [MVP] Liste — contenitore base dei task
- [MVP] Sotto-task (subtask annidati sotto un reminder)
- [v2] Cartelle — raggruppano più liste (utile solo quando le liste diventano tante)
- [v2] Gruppi di liste in sidebar — Apple li tiene distinti dalle cartelle, si copia
  questa separazione per ora; se in uso risultano ridondanti si valuta unirli dopo
- [MVP] Tag — cross-lista, per ritrovare task senza dipendere dalla lista in cui sono
- [MVP] Priorità (bassa/media/alta)
- [MVP] Flag — asse di importanza separato dalla priorità (es. priorità = urgenza,
  flag = "guarda qui"), tenuto come feature distinta
- [MVP] Smart list predefinite: Oggi, Programmati, Flaggati, Tutti, Completati
- [v2] Smart list custom (filtri combinati: tag + data + priorità + posizione)
- [v2] Sezioni dentro una lista (stile colonne To Do / In corso / Fatto) — utile per
  progetti, meno per reminder quotidiani; verificare se ti serve davvero
- [v2] Vista a colonna (kanban) per le sezioni

## Date e ricorrenza

- [MVP] Data/ora di scadenza + notifica
- [MVP] Ricorrenza (giornaliera/settimanale/mensile/custom)
- [MVP] Reminder basati su posizione (arrivo/partenza da un luogo — richiede permessi
  GPS always-on, consumo batteria da monitorare, va gestito con attenzione)
- [skip] Reminder basati su contatto (si attiva quando messaggi una persona) — feature
  di nicchia, integrazione profonda col sistema di messaggistica iOS che su Android
  non ha un equivalente diretto e pulito

## Contenuto

- [MVP] Note/descrizione testuale sul task
- [v2] Allegati (foto, scansione documenti, link)
- [skip] Integrazione con app Note — non hai un'app Note propria, verrebbe scollegato
  da tutto; riconsiderare solo se costruisci anche quella

## Collaborazione

- [skip] Liste condivise multi-utente con aggiornamento realtime — deciso: **solo
  storage locale per ora**, niente backend/sync multi-dispositivo. Da riconsiderare
  se in futuro serve sync (a quel punto questa feature torna in gioco)
- [skip] Assegnazione task a un altro utente ("Assigned to Me") — dipende dal punto
  sopra, stesso motivo

## Dati e backup

- [MVP] Export/backup locale (es. file JSON su storage del telefono) — **attenzione**:
  con solo storage locale, se perdi/rompi il telefono perdi tutti i reminder. Senza
  sync cloud serve almeno un modo manuale di fare backup/ripristino, altrimenti è un
  rischio reale di perdita dati che Apple Reminders (con iCloud) non ha
- [v2] Import da altre app (CSV/JSON) — utile se un giorno migri da TickTick/altro

## AI / smart

- [MVP] Linguaggio naturale nel titolo → data/ora/ricorrenza (già in requirements.md)
- [MVP] Voce → task strutturato via Gemini API (già deciso)
- [v2] Auto-categorizzazione ML (mette da solo un task in "Lavoro"/"Personale"/
  "Spesa" in base a parole chiave) — carino ma non essenziale, e con l'NLU via Gemini
  potresti ottenerlo quasi gratis chiedendo anche la categoria nello stesso prompt
  invece di costruire un classificatore separato
- [v2] Liste della spesa con raggruppamento automatico per reparto (latticini,
  ortofrutta...) — stesso discorso: se hai già Gemini in mezzo, chiediglielo nel
  prompt invece di un sistema dedicato

## Altro

- [MVP] Ricerca full-text su tutti i reminder
- [MVP] Widget home screen (lista di oggi)
- [skip] Siri / assistente esterno — deciso in requirements.md: nessun assistente di
  sistema affidabile, si usa il microfono in-app

## Deciso in questa iterazione

- Sync: solo storage locale per ora, niente backend (vedi "Dati e backup" per il
  rischio di perdita dati che questo comporta)
- Flag: tenuto come feature MVP separata da Priorità
- Reminder basati su posizione: promossi a MVP
- Cartelle e Gruppi di liste: si copiano entrambi come fa Apple, si personalizza dopo
  se in uso risultano ridondanti
