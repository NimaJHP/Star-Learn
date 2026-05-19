package com.starlearn

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NeonDarkScheme = darkColorScheme(
    primary = Color(0xFF00AEEF),
    onPrimary = Color(0xFF001018),
    secondary = Color(0xFF2BD9FF),
    background = Color(0xFF05070A),
    onBackground = Color(0xFFE7F6FF),
    surface = Color(0xFF0A1118),
    onSurface = Color(0xFFE7F6FF),
    error = Color(0xFFFF667A)
)

@Composable
fun StarLearnTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) NeonDarkScheme else NeonDarkScheme
    MaterialTheme(
        colorScheme = colors,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
