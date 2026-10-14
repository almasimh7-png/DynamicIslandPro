package com.ziba.dynamicislandpro.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Black = Color(0xFF000000)
private val DarkSurface = Color(0xFF121212)
private val Accent = Color(0xFF7C4DFF)
private val AccentDark = Color(0xFFB388FF)

private val LightColors = lightColorScheme(primary = Accent, secondary = AccentDark)
private val DarkColors = darkColorScheme(
    primary = AccentDark,
    background = Black,
    surface = DarkSurface,
    onSurface = Color.White
)

@Composable
fun DynamicIslandTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
