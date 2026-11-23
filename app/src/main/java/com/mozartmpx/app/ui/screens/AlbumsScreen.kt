package com.mozartmpx.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mozartmpx.app.data.model.AlbumGroup
import com.mozartmpx.app.data.model.Track
import com.mozartmpx.app.data.model.formatDuration
import com.mozartmpx.app.player.PlaybackUiState
import com.mozartmpx.app.ui.components.HiFiBadge
import com.mozartmpx.app.ui.components.MozartArtwork
import com.mozartmpx.app.ui.components.TrackRow

@Composable
fun AlbumGridCard(
    album: AlbumGroup,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("album_card_${album.albumId}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            MozartArtwork(
                artworkUri = album.artworkUri,
                trackUri = album.sampleTrackUri,
                contentDescription = "Portada de ${album.name}",
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(16.dp),
                sizePx = 300
            )
            if (album.isHiFi) {
                HiFiBadge(
                    label = album.primaryFormat,
                    emphasized = true,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = album.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = buildString {
                append(album.artist)
                if (album.year > 0) append(" · ${album.year}")
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AlbumDetailScreen(
    album: AlbumGroup?,
    playbackState: PlaybackUiState,
    onBack: () -> Unit,
    onPlayTracks: (List<Track>, Int, String, Boolean?) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onInspectHiFi: (Track) -> Unit,
    onHideTrack: (Track) -> Unit,
    onDeleteTrack: (Track) -> Unit,
    contentBottomPadding: PaddingValues = PaddingValues(bottom = 100.dp)
) {
    if (album == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Álbum no encontrado")
        }
        return
    }

    val topTint = lerp(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.primaryContainer, 0.45f)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = contentBottomPadding.calculateBottomPadding() + 20.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(colors = listOf(topTint, MaterialTheme.colorScheme.background)))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("album_detail_back")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (album.isHiFi) {
                        HiFiBadge(label = album.primaryFormat, emphasized = true)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                MozartArtwork(
                    artworkUri = album.artworkUri,
                    trackUri = album.sampleTrackUri,
                    contentDescription = "Portada del álbum ${album.name}",
                    modifier = Modifier.size(204.dp),
                    shape = RoundedCornerShape(22.dp),
                    sizePx = 480
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = album.name,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${album.artist} · ${album.trackCount} pistas · ${formatDuration(album.totalDurationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onPlayTracks(album.tracks, 0, album.name, false) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("album_play_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play", maxLines = 1, softWrap = false)
                    }
                    FilledTonalButton(
                        onClick = { onPlayTracks(album.tracks, 0, album.name, true) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("album_shuffle_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mezclar", maxLines = 1, softWrap = false)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        itemsIndexed(album.tracks, key = { _, t -> "alb_track_${t.id}" }) { index, track ->
            val trackNum = if (track.trackNumber > 0) track.trackNumber.toString() else (index + 1).toString()
            TrackRow(
                track = track,
                isCurrent = playbackState.currentTrack?.id == track.id,
                isPlaying = playbackState.isPlaying,
                trackNumberLabel = trackNum,
                onClick = { onPlayTracks(album.tracks, index, album.name, false) },
                onPlayNext = { onPlayNext(track) },
                onAddToQueue = { onAddToQueue(track) },
                onAddToPlaylist = { onAddToPlaylist(track) },
                onToggleFavorite = { onToggleFavorite(track) },
                onInspectHiFi = { onInspectHiFi(track) },
                onHideTrack = { onHideTrack(track) },
                onDeleteTrack = { onDeleteTrack(track) },
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}
