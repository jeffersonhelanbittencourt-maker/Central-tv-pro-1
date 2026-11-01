package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color.Black,
    primaryContainer = CinemaSlate,
    onPrimaryContainer = NeonCyanLight,
    secondary = NeonAmber,
    onSecondary = Color.Black,
    secondaryContainer = CinemaSurfaceElevated,
    onSecondaryContainer = NeonAmber,
    tertiary = NeonPurple,
    onTertiary = Color.White,
    background = CinemaBlack,
    onBackground = TextPrimary,
    surface = CinemaSurface,
    onSurface = TextPrimary,
    surfaceVariant = CinemaSlate,
    onSurfaceVariant = TextSecondary,
    outline = CinemaSlate,
    outlineVariant = Color(0xFF22314E),
    error = LiveBadgeRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force premium dark cinema styling for TV & streaming
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
