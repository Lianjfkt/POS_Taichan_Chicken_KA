package com.kapos.mobile.core.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = SurfaceWhite,
    primaryContainer = PrimarySoft,
    onPrimaryContainer = PrimaryDark,
    secondary = Info,
    onSecondary = SurfaceWhite,
    secondaryContainer = InfoBg,
    onSecondaryContainer = Info,
    background = BgTop,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceTint,
    onSurfaceVariant = TextSecondary,
    outline = OutlineSoft,
    outlineVariant = OutlineSoft,
    error = Danger,
    onError = SurfaceWhite,
    errorContainer = DangerBg,
    onErrorContainer = Danger
)

@Composable
fun KaPosTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = BgTop.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AppTypography,
        shapes = MaterialShapes,
        content = content
    )
}
