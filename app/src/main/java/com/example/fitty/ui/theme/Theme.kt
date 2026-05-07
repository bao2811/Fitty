package com.example.fitty.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = FittyPinkLight,
    onPrimary = FittyOnPrimary,
    primaryContainer = Color(0xFF3D1A2E),
    onPrimaryContainer = FittyPinkLight,
    secondary = FittyTeal,
    onSecondary = FittyOnPrimary,
    secondaryContainer = Color(0xFF1A3D35),
    onSecondaryContainer = FittyTeal,
    tertiary = FittyGold,
    onTertiary = Color(0xFF1C1B2B),
    tertiaryContainer = Color(0xFF3D3520),
    onTertiaryContainer = FittyGold,
    error = FittyCoral,
    background = FittyBackgroundDark,
    onBackground = Color(0xFFE8E8F0),
    surface = FittySurfaceDark,
    onSurface = Color(0xFFE8E8F0),
    onSurfaceVariant = Color(0xFF9E9EB0),
    outline = Color(0xFF3A3A4A),
    outlineVariant = Color(0xFF2A2A38),
    surfaceVariant = Color(0xFF252530)
)

private val LightColorScheme = lightColorScheme(
    primary = FittyPink,
    onPrimary = FittyOnPrimary,
    primaryContainer = Color(0xFFFCE4EC),
    onPrimaryContainer = FittyPinkDark,
    secondary = FittyTeal,
    onSecondary = FittyOnPrimary,
    secondaryContainer = Color(0xFFE0F7FA),
    onSecondaryContainer = Color(0xFF00695C),
    tertiary = FittyGold,
    onTertiary = Color(0xFF1C1B2B),
    tertiaryContainer = Color(0xFFFFF8E1),
    onTertiaryContainer = Color(0xFFF57F17),
    error = FittyCoral,
    background = FittyBackgroundLight,
    onBackground = FittyOnSurface,
    surface = FittySurfaceLight,
    onSurface = FittyOnSurface,
    onSurfaceVariant = FittyOnSurfaceVariant,
    outline = FittyOutline,
    outlineVariant = Color(0xFFF0F0F5),
    surfaceVariant = Color(0xFFF8F8FF)
)

@Composable
fun FittyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
