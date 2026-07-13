package com.sarrows.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController

private val SarrowsDarkColorScheme = darkColorScheme(
    primary          = SarrowsRed,
    onPrimary        = SarrowsWhite,
    primaryContainer = SarrowsRedDark,
    onPrimaryContainer = SarrowsWhite,
    secondary        = SarrowsGold,
    onSecondary      = SarrowsBlack,
    secondaryContainer = Color(0xFF3D2B00),
    onSecondaryContainer = SarrowsGoldLight,
    tertiary         = SarrowsGreen,
    onTertiary       = SarrowsBlack,
    background       = SarrowsBlack,
    onBackground     = SarrowsWhite,
    surface          = SarrowsDark,
    onSurface        = SarrowsWhite,
    surfaceVariant   = SarrowsDarkCard,
    onSurfaceVariant = SarrowsWhite60,
    outline          = SarrowsDarkBorder,
    outlineVariant   = SarrowsDarkBorder,
    error            = SarrowsRedLight,
    onError          = SarrowsBlack,
    errorContainer   = Color(0xFF4D0000),
    onErrorContainer = SarrowsRedLight,
    inverseSurface   = SarrowsWhite,
    inverseOnSurface = SarrowsBlack,
    scrim            = Color(0x80000000)
)

@Composable
fun SarrowsTheme(content: @Composable () -> Unit) {
    val systemUiController = rememberSystemUiController()

    SideEffect {
        systemUiController.setSystemBarsColor(
            color = SarrowsBlack,
            darkIcons = false
        )
    }

    MaterialTheme(
        colorScheme = SarrowsDarkColorScheme,
        typography  = SarrowsTypography,
        content     = content
    )
}
