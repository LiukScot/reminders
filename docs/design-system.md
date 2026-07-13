# Design system

## Source of truth

Reminders shares a design system with the author's other apps:
[Material-Flow-Design-System](https://github.com/LiukScot/Material-Flow-Design-System).
Any frontend work must check that repo (and the pixel-accurate reference at
`Reminders App Mockup/` in this repo) before touching UI — see AGENTS.md.

Material Flow is Material Design 3 (Pixel-style), implemented natively via
Jetpack Compose (`androidx.compose.material3`), with one shared difference: the
accent is a two-color gradient ("the flow") instead of a single hue, and the
system is **dark-only** (no light theme, `color-scheme: dark`, shared across
all apps in the family — this supersedes the original plan below to ship a
light scheme).

## Color

- Flow theme: **ember** (amber → coral, `#ffc27f → #ff8f7f`), the closest of
  the four shared flow themes to the "orange" direction from requirements.md.
  Chosen because it was the only unassigned theme that fit; `twilight` already
  belongs to Sonari.
- Ink surfaces, text tones, and semantic colors (success/warning/danger) are
  shared verbatim across the whole design system — not re-derived per app.
- Every Material 3 `ColorScheme` role is mapped from these exact design-system
  tokens (see `Color.kt`/`Theme.kt`), not generated algorithmically from a
  seed — the design system already hand-specifies every value needed.
- **Dynamic color (Material You)**: still deferred to after the MVP (tracked
  as a v2 issue). When it lands, it must stay within the dark-only constraint —
  i.e. use `dynamicDarkColorScheme()` only, never the light variant — since the
  design system has no light theme to fall into.

## Typography, shapes, components

Using Material 3's default type scale and shapes (rounded corners, elevations),
overridden only where the Material Flow tokens (`typography.css`,
`spacing.css`) specify a different value.

## To define later

- Icons: icon set to be chosen (the mockup ships Lucide SVGs under
  `Reminders App Mockup/assets/icons/` — check there first before picking a
  different set).
