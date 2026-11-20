package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val PantryLightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = EmeraldOnPrimaryContainer,
    secondary = SageSecondary,
    onSecondary = SageOnSecondary,
    secondaryContainer = SageSecondaryContainer,
    onSecondaryContainer = SageOnSecondaryContainer,
    tertiary = CitrusTertiary,
    onTertiary = CitrusOnTertiary,
    tertiaryContainer = CitrusTertiaryContainer,
    onTertiaryContainer = CitrusOnTertiaryContainer,
    background = CanvasLight,
    onBackground = CharcoalInk,
    surface = SurfaceWhite,
    onSurface = CharcoalInk,
    surfaceVariant = SurfaceSoftMint,
    onSurfaceVariant = SlateSecondary,
    surfaceContainerLowest = SurfaceWhite,
    surfaceContainerLow = SurfaceElevatedLight,
    surfaceContainer = SurfaceSoftMint,
    surfaceContainerHigh = Color(0xFFE3EFE6),
    outline = BorderSoft,
    outlineVariant = BorderSubtle,
    error = UrgentRedFgLight,
    errorContainer = UrgentRedBgLight,
    onError = Color.White,
    onErrorContainer = UrgentRedFgLight
)

private val PantryDarkColorScheme = darkColorScheme(
    primary = EmeraldDarkPrimary,
    onPrimary = EmeraldDarkOnPrimary,
    primaryContainer = EmeraldDarkContainer,
    onPrimaryContainer = EmeraldDarkOnContainer,
    secondary = Color(0xFF8CC5A3),
    onSecondary = Color(0xFF0A331C),
    secondaryContainer = Color(0xFF1F3B2B),
    onSecondaryContainer = Color(0xFFCDEED9),
    tertiary = WarningAmberFgDark,
    onTertiary = Color(0xFF422800),
    tertiaryContainer = WarningAmberBgDark,
    onTertiaryContainer = WarningAmberFgDark,
    background = CanvasDark,
    onBackground = InkDarkPrimary,
    surface = SurfaceDark,
    onSurface = InkDarkPrimary,
    surfaceVariant = SurfaceDarkVariant,
    onSurfaceVariant = InkDarkSecondary,
    surfaceContainerLowest = Color(0xFF0B120E),
    surfaceContainerLow = SurfaceDark,
    surfaceContainer = SurfaceDarkVariant,
    surfaceContainerHigh = Color(0xFF26372D),
    outline = BorderDark,
    outlineVariant = Color(0xFF233229),
    error = UrgentRedFgDark,
    errorContainer = UrgentRedBgDark,
    onError = Color(0xFF690005),
    onErrorContainer = UrgentRedFgDark
)

data class PantrySemanticColors(
    val urgentBg: Color,
    val urgentFg: Color,
    val urgentAccent: Color,
    val warningBg: Color,
    val warningFg: Color,
    val warningAccent: Color,
    val freshBg: Color,
    val freshFg: Color,
    val freshAccent: Color
)

val LocalPantryColors = staticCompositionLocalOf {
    PantrySemanticColors(
        urgentBg = UrgentRedBgLight,
        urgentFg = UrgentRedFgLight,
        urgentAccent = UrgentRedAccent,
        warningBg = WarningAmberBgLight,
        warningFg = WarningAmberFgLight,
        warningAccent = WarningAmberAccent,
        freshBg = FreshGreenBgLight,
        freshFg = FreshGreenFgLight,
        freshAccent = FreshGreenAccent
    )
}

val PantryFlowShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun PantryFlowTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) PantryDarkColorScheme else PantryLightColorScheme
    val semanticColors = if (darkTheme) {
        PantrySemanticColors(
            urgentBg = UrgentRedBgDark,
            urgentFg = UrgentRedFgDark,
            urgentAccent = UrgentRedAccent,
            warningBg = WarningAmberBgDark,
            warningFg = WarningAmberFgDark,
            warningAccent = WarningAmberAccent,
            freshBg = FreshGreenBgDark,
            freshFg = FreshGreenFgDark,
            freshAccent = FreshGreenAccent
        )
    } else {
        PantrySemanticColors(
            urgentBg = UrgentRedBgLight,
            urgentFg = UrgentRedFgLight,
            urgentAccent = UrgentRedAccent,
            warningBg = WarningAmberBgLight,
            warningFg = WarningAmberFgLight,
            warningAccent = WarningAmberAccent,
            freshBg = FreshGreenBgLight,
            freshFg = FreshGreenFgLight,
            freshAccent = FreshGreenAccent
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalPantryColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PantryFlowTypography,
            shapes = PantryFlowShapes,
            content = content
        )
    }
}
