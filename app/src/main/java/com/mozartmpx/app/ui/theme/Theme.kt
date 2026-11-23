package com.mozartmpx.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
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
 * The palette is applied statically here. Screen-specific transitions (such as the
 * Now Playing gradient) animate locally instead of invalidating the whole theme
 * 19 times per track change.
 */
@Composable
fun MozartMpxTheme(
    albumPalette: AlbumPalette = DefaultAlbumPalette,
    content: @Composable () -> Unit
) {
    val colorScheme = remember(albumPalette) { buildAlbumTintedColorScheme(albumPalette) }

    CompositionLocalProvider(LocalAlbumPalette provides albumPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MozartTypography,
            shapes = MozartShapes,
            content = content
        )
    }
}
