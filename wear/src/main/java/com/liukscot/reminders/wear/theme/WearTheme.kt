package com.liukscot.reminders.wear.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import com.liukscot.reminders.ui.theme.EmberAccentSoft
import com.liukscot.reminders.ui.theme.EmberAccentSolid
import com.liukscot.reminders.ui.theme.EmberFlowA
import com.liukscot.reminders.ui.theme.EmberFlowB
import com.liukscot.reminders.ui.theme.EmberTextOnAccent
import com.liukscot.reminders.ui.theme.InkBg
import com.liukscot.reminders.ui.theme.InkCard
import com.liukscot.reminders.ui.theme.InkHover
import com.liukscot.reminders.ui.theme.InkRaised
import com.liukscot.reminders.ui.theme.StatusDanger
import com.liukscot.reminders.ui.theme.TextBody
import com.liukscot.reminders.ui.theme.TextFaint
import com.liukscot.reminders.ui.theme.TextMuted
import com.liukscot.reminders.ui.theme.TextStrong

// The same Material Flow ember tokens the phone uses — Color.kt is shared with :app rather than
// copied (see wear/build.gradle.kts). Only the mapping differs: Wear's ColorScheme has no
// surface/surfaceVariant pair, it has three surfaceContainer tiers instead, and every accent role
// carries a "Dim" variant for the pressed state.
private val EmberWearColors = ColorScheme(
    primary = EmberAccentSolid,
    primaryDim = EmberFlowB,
    primaryContainer = EmberAccentSoft,
    onPrimary = EmberTextOnAccent,
    onPrimaryContainer = EmberTextOnAccent,
    secondary = TextMuted,
    secondaryDim = TextFaint,
    secondaryContainer = InkRaised,
    onSecondary = InkBg,
    onSecondaryContainer = TextBody,
    tertiary = EmberFlowA,
    tertiaryDim = EmberFlowB,
    tertiaryContainer = InkHover,
    onTertiary = EmberTextOnAccent,
    onTertiaryContainer = TextBody,
    surfaceContainerLow = InkBg,
    surfaceContainer = InkCard,
    surfaceContainerHigh = InkRaised,
    onSurface = TextStrong,
    onSurfaceVariant = TextMuted,
    outline = TextFaint,
    outlineVariant = InkHover,
    background = InkBg,
    onBackground = TextBody,
    error = StatusDanger,
    errorDim = StatusDanger,
    errorContainer = InkRaised,
    onError = InkBg,
    onErrorContainer = StatusDanger,
)

@Composable
fun RemindersWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = EmberWearColors, content = content)
}
