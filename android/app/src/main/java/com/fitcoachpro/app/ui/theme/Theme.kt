package com.fitcoachpro.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = CoachGreen,
    secondary = CoachAmber,
    background = SurfaceLight
)

private val DarkColors = darkColorScheme(
    primary = CoachGreenLight,
    secondary = CoachAmber
)

@Composable
fun FitCoachProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
