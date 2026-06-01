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
    primary = LuxuryIndigo,
    secondary = LuxuryCyan,
    tertiary = LuxuryPurple,
    background = DarkBg,
    surface = DarkSurface,
    onPrimary = Color(0xFF381E72), // Elegant Dark text on lavender background
    onSecondary = Color(0xFF1C1B1F),
    onTertiary = Color.White,
    onBackground = Color(0xFFE6E1E5), // Text Color #E6E1E5
    onSurface = Color(0xFFE6E1E5),     // Text Color #E6E1E5
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = Color(0xFF938F99) // Secondary details #938F99
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4F46E5),        // Royal Indigo - authoritative and modern
    secondary = Color(0xFF06B6D4),      // Vibrant Cyan - for fresh accents
    tertiary = Color(0xFF8B5CF6),       // Amethyst Violet - for highlighting features
    background = Color(0xFFF1F5F9),     // Soft slate background - reduces eye strain
    surface = Color(0xFFFFFFFF),        // Pure white for cards/surfaces
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF0F172A),   // Deep navy for high-contrast titles
    onSurface = Color(0xFF334155),      // Slate charcoal for content readability
    surfaceVariant = Color(0xFFE2E8F0),  // Light cloud grey for secondary elements
    onSurfaceVariant = Color(0xFF64748B) // Subtle slate for descriptions and borders
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disable default dynamic wallpaper tones to maintain the boutique design core
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
