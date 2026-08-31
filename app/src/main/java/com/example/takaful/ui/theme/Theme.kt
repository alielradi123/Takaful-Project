package com.example.takaful.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Light Color Scheme ────────────────────────────────────────────────────
private val TakafulLightColors = lightColorScheme(
    primary              = Brand600,
    onPrimary            = Color.White,
    primaryContainer     = Brand100,
    onPrimaryContainer   = Brand900,
    secondary            = Gold500,
    onSecondary          = Color.White,
    secondaryContainer   = Gold100,
    onSecondaryContainer = Gold700,
    tertiary             = Teal600,
    onTertiary           = Color.White,
    tertiaryContainer    = Teal100,
    background           = Neutral50,
    onBackground         = Neutral900,
    surface              = Color.White,
    onSurface            = Neutral900,
    surfaceVariant       = Neutral100,
    onSurfaceVariant     = Neutral600,
    outline              = Neutral200,
    outlineVariant       = Neutral300,
    error                = SemanticError,
    onError              = Color.White,
    errorContainer       = Color(0xFFFEE2E2),
    onErrorContainer     = Color(0xFF7F1D1D),
    inverseSurface       = Neutral900,
    inverseOnSurface     = Neutral50,
    inversePrimary       = Brand400,
)

// ── Dark Color Scheme ─────────────────────────────────────────────────────
private val TakafulDarkColors = darkColorScheme(
    primary              = Brand400,
    onPrimary            = Brand900,
    primaryContainer     = Brand800,
    onPrimaryContainer   = Brand100,
    secondary            = Gold400,
    onSecondary          = Neutral900,
    secondaryContainer   = Gold700,
    onSecondaryContainer = Gold100,
    tertiary             = Teal400,
    onTertiary           = Neutral900,
    background           = Color(0xFF121212),
    onBackground         = Neutral100,
    surface              = Color(0xFF1C1C1C),
    onSurface            = Neutral100,
    surfaceVariant       = Color(0xFF2A2A2A),
    onSurfaceVariant     = Neutral400,
    outline              = Neutral600,
    outlineVariant       = Neutral500,
    error                = Color(0xFFF87171),
    onError              = Neutral900,
)

@Composable
fun TakafulTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) TakafulDarkColors else TakafulLightColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = if (darkTheme) Color(0xFF121212).toArgb() else Brand800.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
