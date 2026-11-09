package com.lumix.pro.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LumixDarkScheme = darkColorScheme(
    primary = LumixCyan,
    onPrimary = Color.Black,
    primaryContainer = LumixPanel,
    onPrimaryContainer = LumixTextPrimary,
    secondary = LumixAmber,
    onSecondary = Color.Black,
    error = LumixRed,
    background = LumixObsidian,
    onBackground = LumixTextPrimary,
    surface = LumixGraphite,
    onSurface = LumixTextPrimary,
    surfaceVariant = LumixPanel,
    onSurfaceVariant = LumixTextSecondary,
    outline = LumixBorder
)

@Composable
fun LumixProTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }
    MaterialTheme(
        colorScheme = LumixDarkScheme,
        typography = LumixTypography,
        content = content
    )
}
