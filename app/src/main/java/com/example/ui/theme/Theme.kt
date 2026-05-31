package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FantasyColorScheme = darkColorScheme(
    primary = RichWizardPurple,
    onPrimary = DeepObsidian,
    secondary = AlchemistGreen,
    onSecondary = DeepObsidian,
    tertiary = LegendaryGold,
    onTertiary = DeepObsidian,
    background = DeepObsidian,
    onBackground = CommonWhite,
    surface = CharcoalDark,
    onSurface = CommonWhite,
    surfaceVariant = SumpCardColor,
    onSurfaceVariant = CommonWhite,
    error = ErrorCrimson,
    onError = Color.Black
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    // We enforce our highly immersive dark fantasy theme as default 
    // to match the gorgeous deep violet/obsidian alchemist vibe.
    MaterialTheme(
        colorScheme = FantasyColorScheme,
        typography = Typography,
        content = content
    )
}
