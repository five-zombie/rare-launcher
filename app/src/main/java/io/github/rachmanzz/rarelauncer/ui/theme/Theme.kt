package io.github.rachmanzz.rarelauncer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AetherColorScheme = darkColorScheme(
    primary = AetherPrimary,
    onPrimary = AetherOnPrimary,
    primaryContainer = AetherPrimaryDim,
    onPrimaryContainer = AetherSurface,
    secondary = AetherSecondaryFixed,
    onSecondary = AetherSurface,
    secondaryContainer = AetherSecondary,
    tertiary = AetherTertiary,
    background = AetherSurface,
    onBackground = AetherOnSurface,
    surface = AetherSurface,
    onSurface = AetherOnSurface,
    surfaceVariant = AetherSurfaceContainerHigh,
    onSurfaceVariant = AetherOnSurfaceVariant
)

@Composable
fun RareLauncerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AetherColorScheme,
        typography = Typography,
        content = content
    )
}
