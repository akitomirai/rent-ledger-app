package com.aki.rentledger.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = PearlPrimary,
    onPrimary = Color.White,
    primaryContainer = PearlPrimaryContainer,
    secondaryContainer = PearlSecondaryContainer,
    background = White,
    surface = PearlSurface,
    surfaceVariant = PearlSurfaceVariant,
    onSurface = PearlOnSurface,
    onSurfaceVariant = PearlOnSurfaceVariant,
    outline = PearlOutline
)

@Composable
fun RentLedgerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content
    )
}

