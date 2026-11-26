package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Navy900,
    onPrimary = PureWhite,
    primaryContainer = Navy800,
    onPrimaryContainer = PureWhite,
    secondary = BlueAccent,
    onSecondary = PureWhite,
    secondaryContainer = BlueSoft,
    onSecondaryContainer = Navy900,
    tertiary = AmberGold,
    onTertiary = PureWhite,
    tertiaryContainer = AmberLight,
    onTertiaryContainer = Neutral900,
    background = Neutral50,
    onBackground = Neutral900,
    surface = PureWhite,
    onSurface = Neutral900,
    surfaceVariant = Neutral100,
    onSurfaceVariant = Neutral700,
    outline = Neutral200,
    outlineVariant = Neutral400,
    error = StatusError,
    onError = PureWhite,
    errorContainer = StatusErrorBg,
    onErrorContainer = StatusError
)

private val DarkColorScheme = darkColorScheme(
    primary = BlueLight,
    onPrimary = PureWhite,
    primaryContainer = Navy800,
    onPrimaryContainer = PureWhite,
    secondary = BlueCyan,
    onSecondary = PureWhite,
    secondaryContainer = Navy700,
    onSecondaryContainer = PureWhite,
    tertiary = AmberGold,
    onTertiary = PureWhite,
    background = Navy900,
    onBackground = Neutral50,
    surface = Navy800,
    onSurface = Neutral50,
    surfaceVariant = Navy700,
    onSurfaceVariant = Neutral200,
    outline = Navy600,
    error = StatusError,
    onError = PureWhite
)

@Composable
fun MouryaTheme(
    themeOption: AppThemeOption = AppThemeOptions[0],
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val customizedScheme = baseScheme.copy(
        primary = themeOption.primary,
        primaryContainer = themeOption.primaryContainer,
        tertiary = themeOption.accent
    )

    MaterialTheme(
        colorScheme = customizedScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    themeOption: AppThemeOption = AppThemeOptions[0],
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MouryaTheme(themeOption = themeOption, darkTheme = darkTheme, content = content)
}
