package com.mozartmpx.app.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import com.mozartmpx.app.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ArtworkPaletteExtractor(private val context: Context) {
    private val cache = LruCache<String, AlbumPalette>(80)

    suspend fun extractForTrack(track: Track?): AlbumPalette {
        if (track == null) return DefaultAlbumPalette
        val cacheKey = "${track.albumId}:${track.artworkUri ?: track.uri}"
        cache.get(cacheKey)?.let { return it }

        val palette = withContext(Dispatchers.Default) {
            val bitmap = decodeArtworkBitmap(track) ?: return@withContext DefaultAlbumPalette
            try {
                val p = Palette.from(bitmap).maximumColorCount(16).generate()
                val dominant = p.dominantSwatch?.rgb
                val darkMuted = p.darkMutedSwatch?.rgb ?: p.mutedSwatch?.rgb ?: dominant
                val vibrant = p.vibrantSwatch?.rgb ?: p.lightVibrantSwatch?.rgb ?: dominant
                val lightVibrant = p.lightVibrantSwatch?.rgb ?: vibrant ?: dominant

                if (dominant == null && vibrant == null) {
                    DefaultAlbumPalette
                } else {
                    AlbumPalette(
                        dominant = Color(dominant ?: 0xFF232733.toInt()),
                        darkMuted = Color(darkMuted ?: 0xFF151821.toInt()),
                        vibrant = Color(vibrant ?: 0xFFE4C580.toInt()),
                        lightVibrant = Color(lightVibrant ?: 0xFFF0D9A3.toInt()),
                        isFromArtwork = true
                    )
                }
            } catch (_: Exception) {
                DefaultAlbumPalette
            } catch (_: OutOfMemoryError) {
                DefaultAlbumPalette
            } finally {
                bitmap.recycle()
            }
        }
        cache.put(cacheKey, palette)
        return palette
    }

    private fun decodeArtworkBitmap(track: Track): Bitmap? {
        val opts = BitmapFactory.Options().apply {
            inSampleSize = 4
            inPreferredConfig = Bitmap.Config.RGB_565
        }

        // 1. Try MediaStore album artwork URI first
        track.artworkUri?.let { artUriString ->
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(artUriString))?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, opts)
                }
            }.getOrNull()?.let { return it }
        }

        // 2. Fallback to embedded cover art inside the audio file (FLAC/MP3/M4A/OGG)
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, Uri.parse(track.uri))
            val bytes = retriever.embeddedPicture ?: return null
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        } catch (_: Exception) {
            null
        } catch (_: OutOfMemoryError) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }
}
