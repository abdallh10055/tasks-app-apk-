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
    primary = NinjaEmeraldPrimary,
    onPrimary = NinjaEmeraldOn,
    primaryContainer = NinjaEmeraldContainer,
    onPrimaryContainer = NinjaEmeraldOnContainer,
    secondary = NinjaCyanSecondary,
    onSecondary = NinjaCyanOn,
    secondaryContainer = NinjaCyanContainer,
    onSecondaryContainer = NinjaCyanOnContainer,
    tertiary = NinjaCrimsonTertiary,
    onTertiary = NinjaCrimsonOn,
    tertiaryContainer = NinjaCrimsonContainer,
    onTertiaryContainer = NinjaCrimsonOnContainer,
    background = NinjaDarkBackground,
    onBackground = NinjaDarkOnSurface,
    surface = NinjaDarkSurface,
    onSurface = NinjaDarkOnSurface,
    surfaceVariant = NinjaDarkSurfaceVariant,
    onSurfaceVariant = NinjaDarkOnSurfaceVariant,
    outline = NinjaDarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF059669),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = Color(0xFF0891B2),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF164E63),
    tertiary = Color(0xFFE11D48),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE4E6),
    onTertiaryContainer = Color(0xFF881337),
    background = NinjaLightBackground,
    onBackground = NinjaLightOnSurface,
    surface = NinjaLightSurface,
    onSurface = NinjaLightOnSurface,
    surfaceVariant = NinjaLightSurfaceVariant,
    onSurfaceVariant = NinjaLightOnSurfaceVariant,
    outline = NinjaLightOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep ninja branding distinct
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
