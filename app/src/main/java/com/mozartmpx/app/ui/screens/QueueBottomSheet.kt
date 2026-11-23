package com.mozartmpx.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mozartmpx.app.data.model.Track
import com.mozartmpx.app.data.model.formatDuration
import com.mozartmpx.app.player.PlaybackUiState
import com.mozartmpx.app.ui.components.MozartArtwork
import com.mozartmpx.app.ui.components.PlayingBarsIndicator
import com.mozartmpx.app.ui.theme.MonoTimerStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    playbackState: PlaybackUiState,
    onDismiss: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onRemoveManual: (String) -> Unit,
    onRemoveSession: (String) -> Unit,
    onClearManual: () -> Unit,
    onToggleSmartAutoplay: (Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("queue_bottom_sheet")
        ) {
            item {
                Text(
                    text = "Cola inteligente",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Active Track
            playbackState.currentTrack?.let { current ->
                item {
                    Text(
                        text = "REPRODUCIENDO AHORA",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MozartArtwork(
                                artworkUri = current.artworkUri,
                                trackUri = current.uri,
                                contentDescription = current.title,
                                modifier = Modifier.size(46.dp),
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = current.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = current.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            PlayingBarsIndicator(isPlaying = playbackState.isPlaying)
                        }
                    }
                }
            }

            // Manual Queue
            if (playbackState.manualQueue.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SIGUIENTE EN COLA (${playbackState.manualQueue.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = onClearManual) {
                            Text("Limpiar")
                        }
                    }
                }
                items(playbackState.manualQueue, key = { "manual_${it.id}" }) { track ->
                    QueueTrackItem(
                        track = track,
                        subtitle = track.artist,
                        onClick = { onPlayTrack(track) },
                        onRemove = { onRemoveManual(track.id) }
                    )
                }
            }

            // Upcoming Session Queue
            if (playbackState.upcomingSessionQueue.isNotEmpty()) {
                item {
                    Text(
                        text = "DE LA SESIÓN ACTUAL (${playbackState.upcomingSessionQueue.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                items(playbackState.upcomingSessionQueue.take(15), key = { "sess_${it.id}" }) { track ->
                    QueueTrackItem(
                        track = track,
                        subtitle = "${track.artist} · ${track.album}",
                        onClick = { onPlayTrack(track) },
                        onRemove = { onRemoveSession(track.id) }
                    )
                }
            }

            // Smart Autoplay Section
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-reproducción inteligente",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Mantiene género, ritmo y artistas afines sin pausas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = playbackState.smartAutoplayEnabled,
                            onCheckedChange = onToggleSmartAutoplay,
                            modifier = Modifier.testTag("smart_autoplay_switch")
                        )
                    }
                }
            }

            if (playbackState.smartAutoplayEnabled && playbackState.smartQueue.isNotEmpty()) {
                items(playbackState.smartQueue, key = { "smart_${it.track.id}" }) { rec ->
                    QueueTrackItem(
                        track = rec.track,
                        subtitle = "${rec.track.artist} · ${rec.reason}",
                        onClick = { onPlayTrack(rec.track) },
                        onRemove = null,
                        highlightReason = true
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueTrackItem(
    track: Track,
    subtitle: String,
    onClick: () -> Unit,
    onRemove: (() -> Unit)?,
    highlightReason: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.65f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MozartArtwork(
            artworkUri = track.artworkUri,
            trackUri = track.uri,
            contentDescription = track.title,
            modifier = Modifier.size(42.dp),
            shape = RoundedCornerShape(9.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (highlightReason) MaterialTheme.colorScheme.primary.copy(alpha = 0.88f)
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = formatDuration(track.durationMs),
            style = MonoTimerStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (onRemove != null) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Quitar de la cola",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
