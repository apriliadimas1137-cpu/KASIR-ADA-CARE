package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = BrandTealLight,
    onPrimary = InkDark,
    primaryContainer = BrandTealDark,
    onPrimaryContainer = BrandTealSoft,
    secondary = AccentOrange,
    onSecondary = SurfaceLight,
    secondaryContainer = Color(0xFF3F2F10),
    onSecondaryContainer = AccentOrangeSoft,
    background = Color(0xFF0B191C),
    onBackground = Color(0xFFE5F1F2),
    surface = Color(0xFF12272B),
    onSurface = Color(0xFFE5F1F2),
    surfaceVariant = Color(0xFF193338),
    onSurfaceVariant = Color(0xFF8FAAAE),
    outline = Color(0xFF26444A),
    error = Color(0xFFF27A6D)
)

private val LightColorScheme = lightColorScheme(
    primary = BrandTeal,
    onPrimary = SurfaceLight,
    primaryContainer = BrandTealContainer,
    onPrimaryContainer = BrandTealDark,
    secondary = AccentOrange,
    onSecondary = SurfaceLight,
    secondaryContainer = AccentOrangeSoft,
    onSecondaryContainer = Color(0xFF5A2004),
    background = BgLight,
    onBackground = InkDark,
    surface = SurfaceLight,
    onSurface = InkDark,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = InkMuted,
    outline = LineBorder,
    error = ErrorRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
