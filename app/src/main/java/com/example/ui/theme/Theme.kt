package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ExtendedColors(
    val cardBg: Color,
    val mutedText: Color,
    val border: Color,
    val accent: Color,
    val onAccent: Color,
    val accentText: Color,
    val priorityHigh: Color,
    val priorityMedium: Color,
    val priorityLow: Color,
    val isDark: Boolean
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        cardBg = LightCard,
        mutedText = LightMutedText,
        border = LightBorder,
        accent = AccentAmber,
        onAccent = OnAccentAmber,
        accentText = LightAccentText,
        priorityHigh = PriorityHigh,
        priorityMedium = PriorityMedium,
        priorityLow = PriorityLow,
        isDark = false
    )
}

object NotifyTheme {
    val colors: ExtendedColors
        @Composable
        get() = LocalExtendedColors.current
}

@Composable
fun NotifyAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: Color = AccentAmber,
    onAccentColor: Color = OnAccentAmber,
    content: @Composable () -> Unit
) {
    val extendedColors = if (darkTheme) {
        ExtendedColors(
            cardBg = DarkCard,
            mutedText = DarkMutedText,
            border = DarkBorder,
            accent = accentColor,
            onAccent = onAccentColor,
            accentText = DarkAccentText,
            priorityHigh = PriorityHigh,
            priorityMedium = PriorityMedium,
            priorityLow = PriorityLow,
            isDark = true
        )
    } else {
        ExtendedColors(
            cardBg = LightCard,
            mutedText = LightMutedText,
            border = LightBorder,
            accent = accentColor,
            onAccent = onAccentColor,
            accentText = LightAccentText,
            priorityHigh = PriorityHigh,
            priorityMedium = PriorityMedium,
            priorityLow = PriorityLow,
            isDark = false
        )
    }

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = accentColor,
            onPrimary = onAccentColor,
            background = DarkBg,
            onBackground = DarkText,
            surface = DarkCard,
            onSurface = DarkText,
            outline = DarkBorder,
            surfaceVariant = DarkCard
        )
    } else {
        lightColorScheme(
            primary = accentColor,
            onPrimary = onAccentColor,
            background = LightBg,
            onBackground = LightText,
            surface = LightCard,
            onSurface = LightText,
            outline = LightBorder,
            surfaceVariant = LightCard
        )
    }

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
