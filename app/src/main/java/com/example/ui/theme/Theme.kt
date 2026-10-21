package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CleanWhiteColorScheme = lightColorScheme(
    primary = ProfNavy,
    onPrimary = PureWhite,
    primaryContainer = GradeInfoBg,
    onPrimaryContainer = ProfNavyDark,
    secondary = ProfTeal,
    onSecondary = PureWhite,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF115E59),
    tertiary = ProfNavyLight,
    onTertiary = PureWhite,
    background = PureWhite,
    onBackground = TextPrimary,
    surface = CardSurface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceOffWhite,
    onSurfaceVariant = TextSecondary,
    outline = BorderMedium,
    outlineVariant = BorderSubtle,
    error = GradeDanger,
    onError = PureWhite,
    errorContainer = GradeDangerBg,
    onErrorContainer = GradeDanger
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Clean white theme preferred for academic clarity & high contrast
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CleanWhiteColorScheme,
        typography = Typography,
        content = content
    )
}
