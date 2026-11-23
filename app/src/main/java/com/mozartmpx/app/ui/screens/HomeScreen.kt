package com.mozartmpx.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mozartmpx.app.R
import com.mozartmpx.app.data.model.AlbumGroup
import com.mozartmpx.app.data.model.SmartSession
import com.mozartmpx.app.data.model.Track
import com.mozartmpx.app.player.PlaybackUiState
import com.mozartmpx.app.ui.components.HiFiBadge
import com.mozartmpx.app.ui.components.MozartArtwork
import com.mozartmpx.app.ui.components.MozartBrandMark
import com.mozartmpx.app.ui.components.TrackRow
import com.mozartmpx.app.ui.viewmodel.LibraryUiState

@Composable
fun HomeScreen(
    libraryState: LibraryUiState,
    playbackState: PlaybackUiState,
    onOpenSearch: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onRequestPermission: () -> Unit,
    onPickFolder: () -> Unit,
    onImportFiles: () -> Unit,
    onRescan: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onPlaySession: (SmartSession) -> Unit,
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
    val bg = MaterialTheme.colorScheme.background
    val topGlow = lerp(bg, MaterialTheme.colorScheme.primaryContainer, 0.28f)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(topGlow, bg, bg))),
        contentPadding = PaddingValues(
            top = 10.dp,
            bottom = contentBottomPadding.calculateBottomPadding() + 18.dp
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Minimal Header Bar with punctual icons
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MozartBrandMark()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (libraryState.isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            IconButton(
                                onClick = onRescan,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("home_rescan_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Actualizar biblioteca",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = onOpenEqualizer,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("home_equalizer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = "Ecualizador",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onOpenSearch,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("home_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Buscar",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        if (libraryState.allTracks.isEmpty()) {
            item {
                EmptyLocalLibraryCard(
                    hasPermission = libraryState.hasAudioPermission,
                    isScanning = libraryState.isScanning,
                    onRequestPermission = onRequestPermission,
                    onPickFolder = onPickFolder,
                    onImportFiles = onImportFiles,
                    onRescan = onRescan
                )
            }
        } else {
            // Compact Icon-First Quick Action Row (No verbose text, zero text wrapping!)
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        QuickActionPill(
                            icon = Icons.Rounded.Shuffle,
                            label = "Mezclar",
                            primary = true,
                            onClick = { onPlayTracks(libraryState.allTracks, 0, "Sesión inteligente", true) },
                            testTag = "home_smart_shuffle_button"
                        )
                    }
                    item {
                        QuickActionPill(
                            icon = Icons.Rounded.PlayArrow,
                            label = "Todo (${libraryState.allTracks.size})",
                            primary = false,
                            onClick = { onPlayTracks(libraryState.allTracks, 0, "Biblioteca local", false) },
                            testTag = "home_play_all_button"
                        )
                    }
                    item {
                        QuickActionPill(
                            icon = Icons.Rounded.GraphicEq,
                            label = if (playbackState.crossfadeSeconds == 0) "Gapless" else "Fade ${playbackState.crossfadeSeconds}s",
                            primary = false,
                            onClick = onOpenEqualizer,
                            testTag = "home_eq_pill"
                        )
                    }
                    item {
                        QuickActionPill(
                            icon = Icons.Rounded.CreateNewFolder,
                            label = "Carpeta",
                            primary = false,
                            onClick = onPickFolder,
                            testTag = "home_folder_pill"
                        )
                    }
                }
            }

            // 1. ARTISTS FIRST (Spotify & Deezer style!)
            if (libraryState.artists.isNotEmpty()) {
                item {
                    SectionHeader(title = "Artistas")
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(libraryState.artists.take(15), key = { it.id }) { artist ->
                            ArtistCircleTile(
                                artist = artist,
                                onClick = { onOpenArtist(artist.id) },
                                modifier = Modifier.width(102.dp)
                            )
                        }
                    }
                }
            }

            // 2. RECENTLY PLAYED
            if (libraryState.recentlyPlayed.isNotEmpty()) {
                item {
                    SectionHeader(title = "Recientes")
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(libraryState.recentlyPlayed, key = { _, t -> "recent_${t.id}" }) { idx, track ->
                            RecentTrackCard(
                                track = track,
                                isCurrent = playbackState.currentTrack?.id == track.id,
                                onClick = {
                                    onPlayTracks(libraryState.recentlyPlayed, idx, "Recientes", false)
                                }
                            )
                        }
                    }
                }
            }

            // 3. ALBUMS (after Artists)
            if (libraryState.albums.isNotEmpty()) {
                item {
                    SectionHeader(title = "Álbumes")
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(libraryState.albums.take(14), key = { it.albumId }) { album ->
                            AlbumCompactCard(
                                album = album,
                                onClick = { onOpenAlbum(album.albumId) }
                            )
                        }
                    }
                }
            }

            // 4. SMART AMBIENT SESSIONS
            if (libraryState.smartSessions.isNotEmpty()) {
                item {
                    SectionHeader(title = "Ambientes")
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(libraryState.smartSessions, key = { it.id }) { session ->
                            SmartSessionCard(
                                session = session,
                                onClick = { onPlaySession(session) }
                            )
                        }
                    }
                }
            }

            // 5. QUICK TRACKS / LOSSLESS HI-FI
            val previewTracks = if (libraryState.hiFiTracks.isNotEmpty()) {
                libraryState.hiFiTracks.take(10)
            } else {
                libraryState.allTracks.take(10)
            }
            val previewTitle = if (libraryState.hiFiTracks.isNotEmpty()) "Hi-Fi Lossless" else "Pistas"

            item { SectionHeader(title = previewTitle) }

            itemsIndexed(previewTracks, key = { _, t -> "home_track_${t.id}" }) { index, track ->
                TrackRow(
                    track = track,
                    isCurrent = playbackState.currentTrack?.id == track.id,
                    isPlaying = playbackState.isPlaying,
                    onClick = { onPlayTracks(previewTracks, index, previewTitle, false) },
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
}

@Composable
private fun QuickActionPill(
    icon: ImageVector,
    label: String,
    primary: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .height(48.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun SmartSessionCard(
    session: SmartSession,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(214.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("smart_session_${session.id}"),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MozartArtwork(
                artworkUri = session.coverTrack?.artworkUri,
                trackUri = session.coverTrack?.uri,
                contentDescription = session.title,
                modifier = Modifier.size(50.dp),
                shape = RoundedCornerShape(10.dp),
                sizePx = 150
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = session.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${session.tracks.size} pistas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun RecentTrackCard(
    track: Track,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(126.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            MozartArtwork(
                artworkUri = track.artworkUri,
                trackUri = track.uri,
                contentDescription = track.title,
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(14.dp),
                sizePx = 256
            )
            if (track.isLossless) {
                HiFiBadge(
                    label = track.formatLabel,
                    emphasized = isCurrent,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = track.title,
            style = MaterialTheme.typography.titleSmall,
            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AlbumCompactCard(
    album: AlbumGroup,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("home_album_${album.albumId}")
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            MozartArtwork(
                artworkUri = album.artworkUri,
                trackUri = album.sampleTrackUri,
                contentDescription = album.name,
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(14.dp),
                sizePx = 280
            )
            if (album.isHiFi) {
                HiFiBadge(
                    label = album.primaryFormat,
                    emphasized = true,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = album.name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = album.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyLocalLibraryCard(
    hasPermission: Boolean,
    isScanning: Boolean,
    onRequestPermission: () -> Unit,
    onPickFolder: () -> Unit,
    onImportFiles: () -> Unit,
    onRescan: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_mozart_artwork_clean),
            contentDescription = "Mozart mpx",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(180.dp)
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), CircleShape)
        )

        Text(
            text = "Música local",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        if (!hasPermission) {
            Button(
                onClick = onRequestPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("grant_audio_permission_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Rounded.LibraryMusic, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Permitir audio", maxLines = 1, softWrap = false)
            }
        } else {
            Button(
                onClick = onRescan,
                enabled = !isScanning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("empty_rescan_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isScanning) "Buscando…" else "Escanear", maxLines = 1, softWrap = false)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledTonalButton(
                onClick = onPickFolder,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("empty_pick_folder_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Rounded.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Carpeta", maxLines = 1, softWrap = false)
            }
            OutlinedButton(
                onClick = onImportFiles,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("empty_import_files_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Importar", maxLines = 1, softWrap = false)
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(horizontal = 16.dp)
    )
}
