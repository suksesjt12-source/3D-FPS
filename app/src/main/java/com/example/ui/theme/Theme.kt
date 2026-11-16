package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ShadowDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = ObsidianVoid,
    primaryContainer = PanelSteel,
    onPrimaryContainer = TextPrimary,
    secondary = TacticalEmerald,
    onSecondary = ObsidianVoid,
    secondaryContainer = TacticalSlate,
    onSecondaryContainer = TextPrimary,
    tertiary = WarningAmber,
    onTertiary = ObsidianVoid,
    error = AlertCrimson,
    onError = TextPrimary,
    background = ObsidianVoid,
    onBackground = TextPrimary,
    surface = TacticalSlate,
    onSurface = TextPrimary,
    surfaceVariant = PanelSteel,
    onSurfaceVariant = TextSecondary,
    outline = PanelBorder
)

@Composable
fun OperationShadowTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ShadowDarkColorScheme,
        typography = OperationShadowTypography,
        content = content
    )
}
