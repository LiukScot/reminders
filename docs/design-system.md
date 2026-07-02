# Design system

## Language

Material Design 3 (Material You), implemented natively via Jetpack Compose
(`androidx.compose.material3`).

## Color

- Primary color: orange.
- Palette generated from the seed color via the Material 3 scheme (primary,
  on-primary, primary-container, etc. derived automatically, not hand-picked one
  by one).
- **Dynamic color (Material You)**: deferred to after the MVP. Once enabled, the
  palette no longer starts from the fixed orange but from `dynamicColorScheme()`,
  which generates it from the user's wallpaper (Android 12+, falls back to the
  orange palette on devices that don't support it or on older Android).

## Typography, shapes, components

Using Material 3's default type scale and shapes (rounded corners, elevations) —
no customization until a concrete reason to change them emerges.

## To define later

- Dark mode: Material 3 handles it by default with separate color schemes
  (light/dark), to be checked only if contrast issues emerge with the chosen
  orange.
- Icons: icon set to be chosen (Material Symbols is the default option, consistent
  with the rest).
