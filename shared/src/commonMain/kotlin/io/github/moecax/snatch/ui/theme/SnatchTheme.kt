package io.github.moecax.snatch.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Values from snatch_color_palette.json (repo root), the source of truth for brand colors.
internal object SnatchColors {
    val ElectricLime = Color(0xFFC8FF00)
    val PressedLime = Color(0xFFA8D600)
    val DeepNavy = Color(0xFF050A1C)
    val NavySurface = Color(0xFF0B1228)
    val ElevatedNavy = Color(0xFF121B35)
    val SoftWhite = Color(0xFFF5F5F5)
    val CoolGray = Color(0xFFAAB2C5)
    val MutedNavyGray = Color(0xFF68718A)
    val SubtleBorder = Color(0xFF202A46)
    val Error = Color(0xFFFF5C6C)
    val InfoCyan = Color(0xFF5DD6FF)
}

private val DarkColors = darkColorScheme(
    primary = SnatchColors.ElectricLime,
    onPrimary = SnatchColors.DeepNavy,
    primaryContainer = SnatchColors.PressedLime,
    onPrimaryContainer = SnatchColors.DeepNavy,
    secondary = SnatchColors.CoolGray,
    onSecondary = SnatchColors.DeepNavy,
    secondaryContainer = SnatchColors.SubtleBorder,
    onSecondaryContainer = SnatchColors.ElectricLime,
    tertiary = SnatchColors.InfoCyan,
    onTertiary = SnatchColors.DeepNavy,
    background = SnatchColors.DeepNavy,
    onBackground = SnatchColors.SoftWhite,
    surface = SnatchColors.DeepNavy,
    onSurface = SnatchColors.SoftWhite,
    surfaceVariant = SnatchColors.ElevatedNavy,
    onSurfaceVariant = SnatchColors.CoolGray,
    surfaceContainerLowest = SnatchColors.DeepNavy,
    surfaceContainerLow = SnatchColors.NavySurface,
    surfaceContainer = SnatchColors.NavySurface,
    surfaceContainerHigh = SnatchColors.ElevatedNavy,
    surfaceContainerHighest = SnatchColors.ElevatedNavy,
    outline = SnatchColors.MutedNavyGray,
    outlineVariant = SnatchColors.SubtleBorder,
    error = SnatchColors.Error,
    onError = SnatchColors.DeepNavy,
)

/**
 * Dark-only on purpose: the palette's light-theme primary (#A8D600) is unreadable as text on white,
 * and Material uses `primary` as the content color for TextButtons.
 */
@Composable
fun SnatchTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
