# Design system

## Linguaggio

Material Design 3 (Material You), implementato nativamente via Jetpack Compose
(`androidx.compose.material3`).

## Colore

- Colore primario: arancione.
- Palette generata dal colore seed tramite lo schema Material 3 (primary,
  on-primary, primary-container, ecc. derivati automaticamente, non scelti a mano
  uno per uno).
- **Dynamic color (Material You)**: rimandato a dopo l'MVP. Quando attivato, la
  palette non parte più dall'arancione fisso ma da `dynamicColorScheme()`, che la
  genera dal wallpaper dell'utente (Android 12+, fallback alla palette arancione sui
  dispositivi che non lo supportano o su Android più vecchio).

## Tipografia, forme, componenti

Si usa la scala tipografica e le forme (angoli arrotondati, elevazioni) di default di
Material 3 — nessuna personalizzazione finché non emerge un motivo concreto per
cambiarle.

## Da definire più avanti

- Dark mode: Material 3 lo gestisce di default con schemi colore separati
  (light/dark), da verificare solo se emergono problemi di contrasto con l'arancione
  scelto.
- Icone: set da scegliere (Material Symbols è l'opzione di default, coerente con il
  resto).
