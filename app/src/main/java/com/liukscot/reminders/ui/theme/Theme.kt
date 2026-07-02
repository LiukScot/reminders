package com.liukscot.reminders.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Orange40,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = Orange80,
    onPrimaryContainer = Orange20,
)

private val DarkColors = darkColorScheme(
    primary = Orange80,
    onPrimary = Orange20,
    primaryContainer = Orange40,
    onPrimaryContainer = Orange80,
)

@Composable
fun RemindersTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
