package com.example.nsutconnect.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppleDarkColorScheme = darkColorScheme(
    primary = AppleSystemBlue,
    onPrimary = Color.White,
    secondary = AppleSystemGreen,
    onSecondary = Color.White,
    tertiary = AppleSystemOrange,
    background = AppleBackground,
    surface = AppleCardSurface,
    surfaceVariant = AppleElevatedSurface,
    outline = AppleBorder,
    outlineVariant = AppleBorderLight,
    onBackground = AppleLabel,
    onSurface = AppleLabel,
    onSurfaceVariant = AppleSecondaryLabel
)

@Composable
fun NSUTConnectTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppleDarkColorScheme,
        typography = Typography,
        content = content
    )
}
