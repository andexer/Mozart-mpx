package com.mozartmpx.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

val MozartObsidian = Color(0xFF0B0C10)
val MozartSurface = Color(0xFF12141B)
val MozartSurfaceContainer = Color(0xFF1A1D26)
val MozartSurfaceContainerHigh = Color(0xFF232733)
val MozartGold = Color(0xFFE4C580)
val MozartGoldDeep = Color(0xFF3E331A)
val MozartIvory = Color(0xFFF4F5F7)
val MozartMutedText = Color(0xFFA2A8B8)
val MozartOutline = Color(0xFF343A4B)
val MozartError = Color(0xFFF28B82)

@Immutable
data class AlbumPalette(
    val dominant: Color = Color(0xFF232733),
    val darkMuted: Color = Color(0xFF151821),
    val vibrant: Color = MozartGold,
    val lightVibrant: Color = Color(0xFFF0D9A3),
    val isFromArtwork: Boolean = false
)

val DefaultAlbumPalette = AlbumPalette()

/**
 * Generates a coherent Material 3 dark ColorScheme whose surfaces are visibly tinted
 * with the dominant / darkMuted hues extracted from the active album artwork, while
 * guaranteeing crisp WCAG contrast for typography and controls.
 */
fun buildAlbumTintedColorScheme(palette: AlbumPalette): ColorScheme {
    if (!palette.isFromArtwork) {
        return darkColorScheme(
            primary = MozartGold,
            onPrimary = Color(0xFF1E1707),
            primaryContainer = MozartGoldDeep,
            onPrimaryContainer = Color(0xFFF9E6B8),
            secondary = Color(0xFFC8CEDF),
            onSecondary = Color(0xFF161A24),
            secondaryContainer = Color(0xFF282D3B),
            onSecondaryContainer = MozartIvory,
            tertiary = Color(0xFF9EC5CE),
            onTertiary = Color(0xFF0F2024),
            background = MozartObsidian,
            onBackground = MozartIvory,
            surface = MozartSurface,
            onSurface = MozartIvory,
            surfaceVariant = MozartSurfaceContainer,
            onSurfaceVariant = MozartMutedText,
            surfaceContainerLowest = Color(0xFF08090D),
            surfaceContainerLow = Color(0xFF11131A),
            surfaceContainer = MozartSurfaceContainer,
            surfaceContainerHigh = MozartSurfaceContainerHigh,
            surfaceContainerHighest = Color(0xFF2B303F),
            outline = MozartOutline,
            outlineVariant = Color(0xFF242834),
            error = MozartError,
            onError = Color(0xFF3B0B07)
        )
    }

    val tintAnchor = lerp(palette.darkMuted, palette.dominant, 0.45f)
    val baseBg = lerp(MozartObsidian, tintAnchor, 0.24f)
    val surface = lerp(MozartSurface, tintAnchor, 0.30f)
    val containerLow = lerp(Color(0xFF11131A), tintAnchor, 0.28f)
    val container = lerp(MozartSurfaceContainer, tintAnchor, 0.36f)
    val containerHigh = lerp(MozartSurfaceContainerHigh, tintAnchor, 0.42f)
    val containerHighest = lerp(Color(0xFF2D3242), tintAnchor, 0.46f)

    // Ensure the primary accent is luminous enough against tinted dark surfaces
    val rawAccent = if (palette.vibrant.luminance() > 0.22f) palette.vibrant else palette.lightVibrant
    val primary = if (rawAccent.luminance() < 0.28f) {
        lerp(rawAccent, Color(0xFFF5EFE2), 0.48f)
    } else {
        rawAccent
    }
    val onPrimary = if (primary.luminance() > 0.42f) Color(0xFF14120F) else Color(0xFFFAF9F7)
    val primaryContainer = lerp(Color(0xFF181A22), palette.dominant, 0.52f)
    val onPrimaryContainer = lerp(primary, Color.White, 0.65f)

    val secondary = lerp(MozartIvory, primary, 0.24f)
    val secondaryContainer = lerp(containerHigh, palette.dominant, 0.35f)
    val onSurfaceVariant = lerp(MozartMutedText, primary, 0.16f)
    val outline = lerp(MozartOutline, primary, 0.26f)

    return darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = Color(0xFF12141A),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = MozartIvory,
        tertiary = lerp(palette.lightVibrant, Color(0xFFB5D6DE), 0.35f),
        onTertiary = Color(0xFF10191C),
        background = baseBg,
        onBackground = MozartIvory,
        surface = surface,
        onSurface = MozartIvory,
        surfaceVariant = container,
        onSurfaceVariant = onSurfaceVariant,
        surfaceContainerLowest = lerp(Color(0xFF07080B), tintAnchor, 0.16f),
        surfaceContainerLow = containerLow,
        surfaceContainer = container,
        surfaceContainerHigh = containerHigh,
        surfaceContainerHighest = containerHighest,
        outline = outline,
        outlineVariant = lerp(Color(0xFF232734), tintAnchor, 0.38f),
        error = MozartError,
        onError = Color(0xFF3B0B07)
    )
}
