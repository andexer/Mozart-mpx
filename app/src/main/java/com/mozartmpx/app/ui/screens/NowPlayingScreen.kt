package com.mozartmpx.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mozartmpx.app.data.model.RepeatMode
import com.mozartmpx.app.data.model.Track
import com.mozartmpx.app.data.model.formatDuration
import com.mozartmpx.app.player.PlaybackUiState
import com.mozartmpx.app.ui.components.HiFiBadge
import com.mozartmpx.app.ui.components.MozartArtwork
import com.mozartmpx.app.ui.theme.LocalAlbumPalette
import com.mozartmpx.app.ui.theme.MonoTimerStyle
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowPlayingScreen(
    playbackState: PlaybackUiState,
    positionMsFlow: StateFlow<Long>,
    eqActive: Boolean,
    onDismiss: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onHideTrack: (Track) -> Unit,
    onDeleteTrack: (Track) -> Unit
) {
    BackHandler(onBack = onDismiss)

    val track = playbackState.currentTrack ?: return
    val palette = LocalAlbumPalette.current
    var moreMenuOpen by remember { mutableStateOf(false) }

    // 100% OPAQUE gradient blended against background — ZERO bleed-through from behind!
    val baseBg = MaterialTheme.colorScheme.background
    val animatedDominant by animateColorAsState(
        targetValue = palette.dominant,
        animationSpec = tween(320),
        label = "np_dominant"
    )
    val animatedDark by animateColorAsState(
        targetValue = palette.darkMuted,
        animationSpec = tween(320),
        label = "np_dark"
    )
    val topOpaqueColor = lerp(
        baseBg,
        animatedDominant,
        if (palette.isFromArtwork) 0.44f else 0.16f
    )
    val midOpaqueColor = lerp(
        baseBg,
        animatedDark,
        if (palette.isFromArtwork) 0.34f else 0.10f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(baseBg)
            .background(
                Brush.verticalGradient(
                    colors = listOf(topOpaqueColor, midOpaqueColor, baseBg)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("now_playing_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Clean Minimal Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("now_playing_collapse")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Minimizar",
                        modifier = Modifier.size(30.dp)
                    )
                }

                Text(
                    text = playbackState.activeSessionLabel.ifBlank { track.album },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                Box {
                    IconButton(
                        onClick = { moreMenuOpen = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("now_playing_more_menu")
                    ) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Más acciones")
                    }
                    DropdownMenu(
                        expanded = moreMenuOpen,
                        onDismissRequest = { moreMenuOpen = false },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        DropdownMenuItem(
                            text = { Text("Añadir a lista") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null) },
                            onClick = {
                                moreMenuOpen = false
                                onAddToPlaylist(track)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Ecualizador y Hi-Fi") },
                            leadingIcon = { Icon(Icons.Rounded.GraphicEq, contentDescription = null) },
                            onClick = {
                                moreMenuOpen = false
                                onOpenEqualizer()
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        DropdownMenuItem(
                            text = { Text("Ocultar pista") },
                            leadingIcon = { Icon(Icons.Rounded.VisibilityOff, contentDescription = null) },
                            onClick = {
                                moreMenuOpen = false
                                onHideTrack(track)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar del dispositivo", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.DeleteForever,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                moreMenuOpen = false
                                onDeleteTrack(track)
                            }
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    .pointerInput(onDismiss) {
                        detectVerticalDragGestures { _, dy ->
                            if (dy > 12f) onDismiss()
                        }
                    }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Crisp Hero Artwork — double tap opens the full metadata sheet
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .aspectRatio(1f)
                    .combinedClickable(
                        onClick = {},
                        onDoubleClick = onOpenEqualizer,
                        onLongClick = onOpenEqualizer
                    )
            ) {
                MozartArtwork(
                    artworkUri = track.artworkUri,
                    trackUri = track.uri,
                    contentDescription = "Portada de ${track.album}",
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(24.dp)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    sizePx = 720,
                    allowEmbeddedExtraction = true
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Compact Technical Pill
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        RoundedCornerShape(50)
                    )
                    .clickable(onClick = onOpenEqualizer)
                    .testTag("now_playing_hifi_pill"),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HiFiBadge(label = track.hiFiShortBadge, emphasized = true)
                    Text(
                        text = buildString {
                            if (track.bitrateKbps > 0) append("${track.bitrateKbps} kbps")
                            if (playbackState.crossfadeSeconds == 0) append(" · Gapless")
                            else append(" · ${playbackState.crossfadeSeconds}s")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title + Artist + Punctual Action Icons (Favorite, Hide, Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .combinedClickable(
                            onClick = {},
                            onDoubleClick = onOpenEqualizer,
                            onLongClick = onOpenEqualizer
                        )
                ) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Doble toque: ficha técnica completa",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                IconButton(
                    onClick = { onToggleFavorite(track) },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("now_playing_favorite")
                ) {
                    Icon(
                        imageVector = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Favorita",
                        tint = if (track.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Isolated Scrubber (ONLY this composable recomposes while playing!)
            NowPlayingScrubber(
                trackId = track.id,
                positionMsFlow = positionMsFlow,
                durationMs = playbackState.durationMs,
                onSeekTo = onSeekTo
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Transport Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("now_playing_shuffle")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Aleatorio",
                        tint = if (playbackState.shuffleEnabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onSkipPrevious,
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("now_playing_previous")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Anterior",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Surface(
                    onClick = onTogglePlayPause,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(68.dp)
                        .testTag("now_playing_play_pause")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pausar" else "Reproducir",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("now_playing_next")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Siguiente",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(
                    onClick = onCycleRepeat,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("now_playing_repeat")
                ) {
                    Icon(
                        imageVector = if (playbackState.repeatMode == RepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = "Repetir",
                        tint = if (playbackState.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Compact Bottom Dock: Queue + EQ + Hide + Delete
            val nextAuto = playbackState.manualQueue.firstOrNull()
                ?: playbackState.upcomingSessionQueue.firstOrNull()
                ?: playbackState.smartQueue.firstOrNull()?.track

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onOpenQueue)
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                            .testTag("now_playing_queue_dock"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (playbackState.manualQueue.isEmpty() && playbackState.upcomingSessionQueue.isEmpty())
                                Icons.Rounded.AutoAwesome else Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = "Cola",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = nextAuto?.title ?: "Cola",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onOpenEqualizer,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("now_playing_eq_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = "Ecualizador",
                            tint = if (eqActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { onHideTrack(track) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("now_playing_hide_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VisibilityOff,
                            contentDescription = "Ocultar pista",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { onDeleteTrack(track) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("now_playing_delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteForever,
                            contentDescription = "Eliminar del dispositivo",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun NowPlayingScrubber(
    trackId: String,
    positionMsFlow: StateFlow<Long>,
    durationMs: Long,
    onSeekTo: (Long) -> Unit
) {
    val livePosMs by positionMsFlow.collectAsStateWithLifecycle()
    var isScrubbing by remember(trackId) { mutableStateOf(false) }
    var scrubPosition by remember(trackId) { mutableFloatStateOf(0f) }

    val safeDuration = durationMs.coerceAtLeast(1L)
    val displayPosMs = if (isScrubbing) (scrubPosition * safeDuration).toLong() else livePosMs
    val sliderFraction = if (isScrubbing) scrubPosition else (displayPosMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    Slider(
        value = sliderFraction,
        onValueChange = { value ->
            isScrubbing = true
            scrubPosition = value
        },
        onValueChangeFinished = {
            onSeekTo((scrubPosition * safeDuration).toLong())
            isScrubbing = false
        },
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("now_playing_slider")
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = formatDuration(displayPosMs),
            style = MonoTimerStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "-${formatDuration((safeDuration - displayPosMs).coerceAtLeast(0L))}",
            style = MonoTimerStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
