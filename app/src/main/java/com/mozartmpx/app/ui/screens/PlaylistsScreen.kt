package com.mozartmpx.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mozartmpx.app.data.model.PlaylistSummary
import com.mozartmpx.app.data.model.Track
import com.mozartmpx.app.player.PlaybackUiState
import com.mozartmpx.app.ui.components.CreatePlaylistDialog
import com.mozartmpx.app.ui.components.MozartArtwork
import com.mozartmpx.app.ui.components.TrackRow

const val VIRTUAL_PLAYLIST_FAVORITES = -1L
const val VIRTUAL_PLAYLIST_MOST_PLAYED = -2L

@Composable
fun PlaylistsScreen(
    playlists: List<PlaylistSummary>,
    favoritesCount: Int,
    mostPlayedCount: Int,
    onCreatePlaylist: (String, String) -> Unit,
    onRenamePlaylist: (Long, String, String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    contentBottomPadding: PaddingValues = PaddingValues(bottom = 100.dp)
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingPlaylist by remember { mutableStateOf<PlaylistSummary?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = contentBottomPadding.calculateBottomPadding() + 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Listas",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                FilledTonalIconButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("playlists_create_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Nueva lista")
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SmartCollectionTile(
                    title = "Favoritas",
                    subtitle = "$favoritesCount",
                    icon = Icons.Rounded.Favorite,
                    onClick = { onOpenPlaylist(VIRTUAL_PLAYLIST_FAVORITES) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("playlist_favorites_tile")
                )
                SmartCollectionTile(
                    title = "Top",
                    subtitle = "$mostPlayedCount",
                    icon = Icons.Rounded.TrendingUp,
                    onClick = { onOpenPlaylist(VIRTUAL_PLAYLIST_MOST_PLAYED) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("playlist_most_played_tile")
                )
            }
        }

        if (playlists.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.PlaylistPlay,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                            modifier = Modifier.size(44.dp)
                        )
                        Text("Toca + para crear una lista", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(playlists, key = { it.id }) { pl ->
                PlaylistRowCard(
                    playlist = pl,
                    onClick = { onOpenPlaylist(pl.id) },
                    onRename = { editingPlaylist = pl },
                    onDelete = { onDeletePlaylist(pl.id) }
                )
            }
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, desc ->
                onCreatePlaylist(name, desc)
                showCreateDialog = false
            }
        )
    }

    editingPlaylist?.let { target ->
        CreatePlaylistDialog(
            initialName = target.name,
            initialDescription = target.description,
            title = "Renombrar lista",
            confirmLabel = "Guardar",
            onDismiss = { editingPlaylist = null },
            onConfirm = { newName, newDesc ->
                onRenamePlaylist(target.id, newName, newDesc)
                editingPlaylist = null
            }
        )
    }
}

@Composable
private fun SmartCollectionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PlaylistRowCard(
    playlist: PlaylistSummary,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("playlist_card_${playlist.id}"),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MozartArtwork(
                artworkUri = playlist.coverTrack?.artworkUri,
                trackUri = playlist.coverTrack?.uri,
                contentDescription = playlist.name,
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(12.dp),
                sizePx = 150
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(playlist.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${playlist.trackCount} pistas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Opciones")
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    DropdownMenuItem(
                        text = { Text("Renombrar") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Eliminar") },
                        leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    title: String,
    tracks: List<Track>,
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
    onRemoveFromPlaylist: ((String) -> Unit)? = null,
    contentBottomPadding: PaddingValues = PaddingValues(bottom = 100.dp)
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = contentBottomPadding.calculateBottomPadding() + 20.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("playlist_detail_back")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${tracks.size} pistas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (tracks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { onPlayTracks(tracks, 0, title, false) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play", maxLines = 1, softWrap = false)
                        }
                        FilledTonalButton(
                            onClick = { onPlayTracks(tracks, 0, title, true) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mezclar", maxLines = 1, softWrap = false)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        if (tracks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (playlistId == VIRTUAL_PLAYLIST_FAVORITES) "Sin favoritas aún" else "Lista vacía",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            itemsIndexed(tracks, key = { _, t -> "pl_${playlistId}_${t.id}" }) { index, track ->
                TrackRow(
                    track = track,
                    isCurrent = playbackState.currentTrack?.id == track.id,
                    isPlaying = playbackState.isPlaying,
                    onClick = { onPlayTracks(tracks, index, title, false) },
                    onPlayNext = { onPlayNext(track) },
                    onAddToQueue = { onAddToQueue(track) },
                    onAddToPlaylist = { onAddToPlaylist(track) },
                    onToggleFavorite = { onToggleFavorite(track) },
                    onInspectHiFi = { onInspectHiFi(track) },
                    onHideTrack = { onHideTrack(track) },
                    onDeleteTrack = { onDeleteTrack(track) },
                    onRemoveFromPlaylist = onRemoveFromPlaylist?.let { fn -> { fn(track.id) } },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}
