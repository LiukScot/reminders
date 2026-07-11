# Material Flow — Design System

**Material Flow** is a dark-only mobile design system **shared across a family of apps**: modern Material Design (Material 3, Pixel-style) with one defining difference — the accent is never a single color. It is a **two-color gradient that moves**. Large accent surfaces drift slowly while idle and *surge* forward when pressed, so the interface feels like it is flowing. Everything else follows M3 conventions: pill buttons, grouped list rows, bottom sheets, compact bottom navigation.

**Each app brings its own gradient.** The ink surfaces, text tones, type, spacing and components are shared; only the accent pair re-themes, via a `data-flow` attribute (see *Flow themes* below).

The foundations were extracted from **Sonari** (an ambient background-sound mixer for Android, the first app on the system). Only what that app actually uses is here; dead CSS from earlier iterations was intentionally ignored.

**Source:** https://github.com/LiukScot/sonari (Kotlin · Jetpack Compose · Media3). Explore it for ground truth — the theme lives in `app/src/main/java/io/github/liukscot/sonari/ui/theme/` (`Color.kt`, `Type.kt`, `Shape.kt`, `Spacing.kt`) and the screens in `ui/mixer/`, `ui/presets/`, `ui/settings/`. Related apps by the same author: `LiukScot/health`, `LiukScot/dashboard`, `LiukScot/money`, `LiukScot/notes`.

---

## FLOW THEMES

Set `data-flow="<theme>"` on `<html>` or any subtree; every token-driven component re-themes. Default (no attribute) = twilight.

| Theme | Gradient | Solid accent | App |
|---|---|---|---|
| `twilight` | `#9b8cff → #ff8fb1` (violet→pink) | `#c69bff` | Sonari |
| `tide` | `#7ab3ff → #6fe8c8` (blue→teal) | `#82c7f2` | *proposed* |
| `ember` | `#ffc27f → #ff8f7f` (amber→coral) | `#ffa98f` | *proposed* |
| `meadow` | `#7fe0a3 → #c8ef7f` (green→lime) | `#a3e891` | *proposed* |

Only twilight is grounded in shipped code; the other three are harmonized proposals (same lightness band, ~60–70° hue separation) awaiting assignment to real apps. To add an app's theme, define `--flow-a`, `--flow-b`, `--accent-solid`, `--accent-soft`, `--text-on-accent` under a new `[data-flow="…"]` scope in `tokens/colors.css`.

## CONTENT FUNDAMENTALS

- **Sentence case everywhere.** Titles, buttons, labels: "New preset from current mix", "Fade in / out", "Sleep timer". Never Title Case, never ALL CAPS — except **overlines** (section eyebrows like GENERAL, PLAYBACK), which are uppercase 11px with wide tracking.
- **No emoji in product UI.** Ever.
- **Calm, plain, brief.** Copy reads like a quiet assistant: "Save a mix, then start it from here. Long-press to delete." · "Lower audio during calls" · "Fade out and stop after…" No exclamation points, no marketing adjectives.
- **First person is the developer's voice** in About sections ("Follow me", "Send me a message"); the app otherwise addresses the user with implicit "you".
- **Numbers are mono.** Levels (72), countdowns (0:45), and OFF states render in JetBrains Mono caption style.
- **Durations use tilde-abbreviations**: "~1 s when you play or pause".
- English only.

## VISUAL FOUNDATIONS

- **Theme:** single dark "twilight" ink base for all apps. No light mode, no switcher. `color-scheme: dark`.
- **Colors:** layered ink surfaces — app `#030304`, card `#1c1c22`, raised `#232329`, hover `#2a2a31`, pressed `#323239`. Text in four steps: strong `#f6f5fb`, body `#d9d7e3`, muted `#b0aebf`, faint `#7e7c8e`. Accent = the app's flow pair at 135°; **solid** accent for text/icons/small details (gradients on small things hurt legibility). Text on gradient is a dark ink derived from the pair. Semantic: success `#7fd6a6`, warning `#f2c879`, danger `#ff8b8b` — quiet, twilight-toned, shared by all apps.
- **THE FLOW (signature):** the gradient is applied ONLY to large elements — play button, active card fill, slider fill, selected tile, primary CTA. On the web it *moves*: `--flow-gradient` at `--flow-size` with `flow-drift` (slow 9s idle travel) and `flow-surge` (900ms forward push on press). One flowing element per view is the ideal; never two competing CTAs.
- **Type:** Manrope for everything (variable, 400–800); JetBrains Mono for numeric readouts. Titles are heavy (700–800) with tight negative tracking; body stays 400. Scale: display 34/38·800, title 24/30·700, headline 19/24·700, body 16/23·400, label 14/18·600, body-sm 13/18, overline 11/14·700·+0.12em UPPER.
- **Spacing:** 4px grid (`--sp-1`…`--sp-8`), card padding 14px, screen edge 18px, grid gap 8px.
- **Radii:** nothing sharp. 10 chips/inputs, 14 cards/rows, 18 sheets/dialogs, 24 bottom bar, pill for buttons/sliders. Grouped rows share **4px inner corners** so a stack reads as one card (2px gap between rows).
- **Elevation:** soft dark shadows (`--shadow-1/2/3`); flowing elements add a theme-tinted glow (`--glow-accent`). No borders on cards — hairlines (`--line-1` .06 white, `--line-2` .10 white) only for dividers and outlined inputs.
- **Backgrounds:** flat ink, no imagery, no texture. Scrollable areas fade out under floating controls with a bottom scrim (transparent→surface-app).
- **Animation:** Material emphasized easing (`cubic-bezier(0.3,0,0,1)`); press = scale ~0.97 + gradient surge; the play button **morphs shape** circle↔20px squircle when toggling (M3 media-controls behavior); switches grow their thumb on check; sheets slide up with decelerate easing. Nothing bounces except deliberate springs.
- **Hover** (web): surfaces lighten one ink step; solid accent lifts to `--accent-soft`. **Press:** darkest ink step, or gradient surge on flowing elements.
- **Transparency/blur:** minimal — translucent white (0.18) for active icon chips, 50% black scrims under sheets/dialogs. No glassmorphism.

## ICONOGRAPHY

- **Lucide** icons, stroke style (2px, round caps/joins), 24px grid — ported directly from the source app's vector drawables to `assets/icons/*.svg` (34 glyphs, drawn with `currentColor` so they tint via CSS `color` / the `Icon` component's mask rendering).
- Available glyphs — ambient/media: cloud-rain, cloud-lightning, wind, waves, droplet, bird, moon-star, flame, train-front, sailboat, building-2, coffee, audio-waveform, radio. UI: play, pause, plus, pencil, trash-2, settings, sliders-horizontal, layers, timer, moon, zap, chevron-right, external-link, grip, folder-plus, volume-2, audio-lines, send, github, instagram. Need more? Pull from Lucide (CDN or copied SVGs) at the same 2px stroke.
- Icons are typically 22px inside a 42–44px rounded chip; 14–18px as inline hints. Tint follows state: accent-solid (active/branded), muted (resting), faint (off/hint). **No emoji, no unicode-as-icon.**
- **No shared logo.** The system deliberately ships no brand mark — each app brings its own. Where a mark would go, render the app name in Manrope 800 with tight tracking.

---

## Index

| Path | What |
|---|---|
| `styles.css` | Global entry — `@import`s all tokens + fonts |
| `tokens/` | `colors.css` (ink + flow themes), `typography.css`, `spacing.css`, `motion.css` (the flow), `fonts.css` |
| `assets/icons/` | 34 Lucide SVGs (currentColor) |
| `assets/fonts/` | Manrope variable, JetBrains Mono (OFL) |
| `components/` | React primitives (below) |
| `templates/flow-screen/` | Starting-point template: phone-framed app screen with a theme switch |
| `guidelines/foundations/` | Specimen cards for the Design System tab |
| `SKILL.md` | Agent-skill entry point |

### Components

- **core/** — `Icon` *(intentional addition: wrapper for the SVG glyph set)*
- **actions/** — `Button` (gradient/tonal/text), `IconButton` (neutral/gradient, circle/rounded/morph)
- **forms/** — `Slider`, `Switch`, `TextField`, `SelectTile` (gradient/tint)
- **content/** — `SoundCard` (signature flowing level tile, from the mixer app), `IconChip`, `ListRow` (grouped)
- **navigation/** — `NavBar`

Each folder holds `<Name>.jsx`, `<Name>.d.ts` (props), `<Name>.prompt.md` (usage) and a `@dsCard` demo HTML. All components read only tokens, so they re-theme with `data-flow` automatically.

### Rules of thumb

1. One flowing gradient element per view; everything else neutral ink.
2. Small = solid `--accent-solid`; large = `--flow-gradient`.
3. Group related rows with 2px gaps + 4px inner corners.
4. One `data-flow` theme per app — don't mix gradients in a view.
5. Respect `prefers-reduced-motion`: flow falls back to the static gradient.

### Intentional additions

- `Icon` — a web wrapper for the app's drawable set (the source uses Android vector drawables; the web needs a tinting mechanism).
- Flow themes `tide` / `ember` / `meadow` — proposed palettes for future apps (only `twilight` ships in real code).
- Web hover states (the source app is touch-only): documented in Visual Foundations, derived from the ink-step system.
