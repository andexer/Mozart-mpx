package com.mozartmpx.app.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp

val LocalAlbumPalette = compositionLocalOf { DefaultAlbumPalette }

val MozartShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * Animates only the 3 root palette anchors (dominant, darkMuted, vibrant) instead of
 * 18 individual color tokens to keep UI transitions silky-smooth and cool on real devices.
 */
@Composable
fun MozartMpxTheme(
    albumPalette: AlbumPalette = DefaultAlbumPalette,
    content: @Composable () -> Unit
) {
    val dominant by animateColorAsState(
        targetValue = albumPalette.dominant,
        animationSpec = tween(durationMillis = 320),
        label = "palette_dominant"
    )
    val darkMuted by animateColorAsState(
        targetValue = albumPalette.darkMuted,
        animationSpec = tween(durationMillis = 320),
        label = "palette_dark_muted"
    )
    val vibrant by animateColorAsState(
        targetValue = albumPalette.vibrant,
        animationSpec = tween(durationMillis = 320),
        label = "palette_vibrant"
    )

    val animatedPalette = remember(dominant, darkMuted, vibrant, albumPalette.isFromArtwork) {
        albumPalette.copy(
            dominant = dominant,
            darkMuted = darkMuted,
            vibrant = vibrant
        )
    }

    val colorScheme = remember(animatedPalette) {
        buildAlbumTintedColorScheme(animatedPalette)
    }

    CompositionLocalProvider(LocalAlbumPalette provides animatedPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MozartTypography,
            shapes = MozartShapes,
            content = content
        )
    }
}
