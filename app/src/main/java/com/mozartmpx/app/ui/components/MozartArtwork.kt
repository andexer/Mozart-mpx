package com.mozartmpx.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mozartmpx.app.R
import com.mozartmpx.app.ui.theme.MonoSpecStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private object EmbeddedArtCache {
    private val cache = LruCache<String, Bitmap>(24)
    fun get(key: String): Bitmap? = cache.get(key)
    fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }
}

/**
 * Lightweight, scroll-optimized album artwork composable.
 * Uses fast Coil AsyncImage with downsampling & instant fallback for list items.
 * Only extracts raw embedded bytes via MediaMetadataRetriever when explicitly enabled
 * (e.g., NowPlayingScreen hero artwork), preventing CPU spikes during scrolling.
 */
@Composable
fun MozartArtwork(
    artworkUri: String?,
    trackUri: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    sizePx: Int = 256,
    allowEmbeddedExtraction: Boolean = false
) {
    val context = LocalContext.current
    var useEmbeddedFallback by remember(artworkUri, trackUri) { mutableStateOf(artworkUri.isNullOrBlank()) }
    var embeddedBitmap by remember(trackUri) {
        mutableStateOf(if (allowEmbeddedExtraction && trackUri != null) EmbeddedArtCache.get(trackUri) else null)
    }

    LaunchedEffect(useEmbeddedFallback, trackUri, allowEmbeddedExtraction) {
        if (allowEmbeddedExtraction && useEmbeddedFallback && embeddedBitmap == null && !trackUri.isNullOrBlank()) {
            embeddedBitmap = loadEmbeddedBitmap(context, trackUri)
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        when {
            !useEmbeddedFallback && !artworkUri.isNullOrBlank() -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(artworkUri)
                        .size(sizePx)
                        .crossfade(160)
                        .error(R.drawable.img_mozart_artwork_clean)
                        .placeholder(R.drawable.img_mozart_artwork_clean)
                        .build(),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    onError = {
                        if (allowEmbeddedExtraction) useEmbeddedFallback = true
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            embeddedBitmap != null -> {
                Image(
                    bitmap = embeddedBitmap!!.asImageBitmap(),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Image(
                    painter = painterResource(id = R.drawable.img_mozart_artwork_clean),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

private suspend fun loadEmbeddedBitmap(context: Context, trackUri: String): Bitmap? =
    withContext(Dispatchers.IO) {
        EmbeddedArtCache.get(trackUri)?.let { return@withContext it }
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.parse(trackUri))
            val bytes = retriever.embeddedPicture ?: return@withContext null
            val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)?.also {
                EmbeddedArtCache.put(trackUri, it)
            }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

@Composable
fun MozartBrandMark(
    modifier: Modifier = Modifier,
    iconSize: Dp = 36.dp
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_mozart_icon_clean),
            contentDescription = "Logotipo de Mozart mpx",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(iconSize)
                .clip(CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "Mozart",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "mpx",
                style = MonoSpecStyle,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}

@Composable
fun HiFiBadge(
    label: String,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false
) {
    val bg = if (emphasized) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f)
    }
    val fg = if (emphasized) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(
                width = 0.75.dp,
                color = fg.copy(alpha = if (emphasized) 0.45f else 0.25f),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MonoSpecStyle,
            color = fg,
            maxLines = 1
        )
    }
}

@Composable
fun PlayingBarsIndicator(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    if (!isPlaying) {
        Row(
            modifier = modifier.height(16.dp),
            horizontalArrangement = Arrangement.spacedBy(2.5.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            listOf(0.35f, 0.55f, 0.35f).forEach { fraction ->
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height((14f * fraction).dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(barColor)
                )
            }
        }
        return
    }

    val transition = rememberInfiniteTransition(label = "playing_bars")
    val bar1 by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar1"
    )
    val bar2 by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(340, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar2"
    )
    val bar3 by transition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(480, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar3"
    )

    Row(
        modifier = modifier.height(16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(bar1, bar2, bar3).forEach { fraction ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((14f * fraction).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
        }
    }
}
