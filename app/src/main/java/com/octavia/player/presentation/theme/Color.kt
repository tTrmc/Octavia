package com.octavia.player.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Custom color scheme for Octavia Hi-Fi Music Player
 * Designed for optimal readability and aesthetic appeal in both light and dark modes
 * Used as fallback for devices that don't support dynamic colors (Android < 12)
 */

// Light theme colors
private val md_theme_light_primary = Color(0xFF1B5E20)
private val md_theme_light_onPrimary = Color(0xFFFFFFFF)
private val md_theme_light_primaryContainer = Color(0xFFA7F3A0)
private val md_theme_light_onPrimaryContainer = Color(0xFF002204)
private val md_theme_light_secondary = Color(0xFF52634F)
private val md_theme_light_onSecondary = Color(0xFFFFFFFF)
private val md_theme_light_secondaryContainer = Color(0xFFD5E8CF)
private val md_theme_light_onSecondaryContainer = Color(0xFF0F1F0F)
private val md_theme_light_tertiary = Color(0xFF38656A)
private val md_theme_light_onTertiary = Color(0xFFFFFFFF)
private val md_theme_light_tertiaryContainer = Color(0xFFBBEBF1)
private val md_theme_light_onTertiaryContainer = Color(0xFF002023)
private val md_theme_light_error = Color(0xFFBA1A1A)
private val md_theme_light_errorContainer = Color(0xFFFFDAD6)
private val md_theme_light_onError = Color(0xFFFFFFFF)
private val md_theme_light_onErrorContainer = Color(0xFF410002)
private val md_theme_light_background = Color(0xFFFCFDF7)
private val md_theme_light_onBackground = Color(0xFF1A1C18)
private val md_theme_light_surface = Color(0xFFFCFDF7)
private val md_theme_light_onSurface = Color(0xFF1A1C18)
private val md_theme_light_surfaceVariant = Color(0xFFDFE4D7)
private val md_theme_light_onSurfaceVariant = Color(0xFF43483F)
private val md_theme_light_outline = Color(0xFF73796E)
private val md_theme_light_inverseOnSurface = Color(0xFFF1F1EB)
private val md_theme_light_inverseSurface = Color(0xFF2F312D)
private val md_theme_light_inversePrimary = Color(0xFF8CD686)

// Dark theme colors
private val md_theme_dark_primary = Color(0xFF8CD686)
private val md_theme_dark_onPrimary = Color(0xFF003909)
private val md_theme_dark_primaryContainer = Color(0xFF0F4A13)
private val md_theme_dark_onPrimaryContainer = Color(0xFFA7F3A0)
private val md_theme_dark_secondary = Color(0xFFB9CCB4)
private val md_theme_dark_onSecondary = Color(0xFF253423)
private val md_theme_dark_secondaryContainer = Color(0xFF3B4B38)
private val md_theme_dark_onSecondaryContainer = Color(0xFFD5E8CF)
private val md_theme_dark_tertiary = Color(0xFFA0CFD4)
private val md_theme_dark_onTertiary = Color(0xFF003539)
private val md_theme_dark_tertiaryContainer = Color(0xFF1E4D51)
private val md_theme_dark_onTertiaryContainer = Color(0xFFBBEBF1)
private val md_theme_dark_error = Color(0xFFFFB4AB)
private val md_theme_dark_errorContainer = Color(0xFF93000A)
private val md_theme_dark_onError = Color(0xFF690005)
private val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)
private val md_theme_dark_background = Color(0xFF1A1C18)
private val md_theme_dark_onBackground = Color(0xFFE2E3DD)
private val md_theme_dark_surface = Color(0xFF1A1C18)
private val md_theme_dark_onSurface = Color(0xFFE2E3DD)
private val md_theme_dark_surfaceVariant = Color(0xFF43483F)
private val md_theme_dark_onSurfaceVariant = Color(0xFFC3C8BC)
private val md_theme_dark_outline = Color(0xFF8D9387)
private val md_theme_dark_inverseOnSurface = Color(0xFF1A1C18)
private val md_theme_dark_inverseSurface = Color(0xFFE2E3DD)
private val md_theme_dark_inversePrimary = Color(0xFF1B5E20)

// Fallback color schemes for devices that don't support dynamic colors (Android < 12)
internal val LightColors = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    errorContainer = md_theme_light_errorContainer,
    onError = md_theme_light_onError,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inverseSurface = md_theme_light_inverseSurface,
    inversePrimary = md_theme_light_inversePrimary,
)

internal val DarkColors = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    errorContainer = md_theme_dark_errorContainer,
    onError = md_theme_dark_onError,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inverseSurface = md_theme_dark_inverseSurface,
    inversePrimary = md_theme_dark_inversePrimary,
)
