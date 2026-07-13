package com.liukscot.reminders.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Material Flow is dark-only by design (see docs/design-system.md) — there is
// deliberately no light ColorScheme to switch to.
private val EmberDarkColors = darkColorScheme(
    primary = EmberAccentSolid,
    onPrimary = EmberTextOnAccent,
    primaryContainer = EmberAccentSoft,
    onPrimaryContainer = EmberTextOnAccent,
    secondary = TextMuted,
    onSecondary = InkBg,
    secondaryContainer = InkRaised,
    onSecondaryContainer = TextBody,
    tertiary = EmberFlowB,
    onTertiary = EmberTextOnAccent,
    tertiaryContainer = InkHover,
    onTertiaryContainer = TextBody,
    error = StatusDanger,
    onError = InkBase,
    errorContainer = InkRaised,
    onErrorContainer = StatusDanger,
    background = InkBg,
    onBackground = TextBody,
    surface = InkCard,
    onSurface = TextStrong,
    surfaceVariant = InkRaised,
    onSurfaceVariant = TextMuted,
    outline = TextFaint,
    outlineVariant = InkHover,
    inverseSurface = TextStrong,
    inverseOnSurface = InkBg,
    inversePrimary = EmberFlowA,
    scrim = InkBase,
    surfaceTint = EmberAccentSolid,
)

@Composable
fun RemindersTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EmberDarkColors,
        typography = Typography,
        content = content,
    )
}
