package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA6C8FF),
    onPrimary = Color(0xFF003063),
    primaryContainer = UnesaBluePrimary,
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = UnesaYellow,
    onSecondary = Color(0xFF412D00),
    secondaryContainer = Color(0xFF5D4200),
    onSecondaryContainer = UnesaYellowContainer,
    tertiary = Color(0xFF81D4FA),
    background = Color(0xFF111318),
    surface = Color(0xFF191C20),
    onBackground = Color(0xFFE2E2E9),
    onSurface = Color(0xFFE2E2E9),
    error = Color(0xFFFFB4AB)
)

private val LightColorScheme = lightColorScheme(
    primary = UnesaBluePrimary,
    onPrimary = Color.White,
    primaryContainer = UnesaBlueContainer,
    onPrimaryContainer = UnesaOnBlueContainer,
    secondary = Color(0xFF855300),
    onSecondary = Color.White,
    secondaryContainer = UnesaYellowContainer,
    onSecondaryContainer = UnesaOnYellow,
    tertiary = UnesaBlueSecondary,
    background = SurfaceLight,
    surface = SurfaceCard,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = LostRed,
    outline = OutlineBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep UNESA distinct branding consistent
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
