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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mozartmpx.app.data.local.HiddenTrackEntity
import com.mozartmpx.app.data.model.FolderGroup
import com.mozartmpx.app.data.model.Track
import com.mozartmpx.app.data.model.formatDuration
import com.mozartmpx.app.player.PlaybackUiState
import com.mozartmpx.app.ui.components.HiFiBadge
import com.mozartmpx.app.ui.components.TrackRow

@Composable
fun FoldersScreen(
    folders: List<FolderGroup>,
    hiddenTracks: List<HiddenTrackEntity>,
    onOpenFolder: (String) -> Unit,
    onPlayFolder: (FolderGroup) -> Unit,
    onPickFolder: () -> Unit,
    onImportFiles: () -> Unit,
    onUnhideTrack: (String) -> Unit,
    contentBottomPadding: PaddingValues = PaddingValues(bottom = 100.dp)
) {
    var showHiddenDialog by remember { mutableStateOf(false) }

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
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Carpetas",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (hiddenTracks.isNotEmpty()) {
                        FilledTonalIconButton(
                            onClick = { showHiddenDialog = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("folders_hidden_button")
                        ) {
                            Icon(Icons.Rounded.VisibilityOff, contentDescription = "Pistas ocultas (${hiddenTracks.size})")
                        }
                    }
                    FilledTonalIconButton(
                        onClick = onPickFolder,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("folders_add_folder_button")
                    ) {
                        Icon(Icons.Rounded.CreateNewFolder, contentDescription = "Añadir carpeta")
                    }
                    FilledTonalIconButton(
                        onClick = onImportFiles,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("folders_import_files_button")
                    ) {
                        Icon(Icons.Rounded.AudioFile, contentDescription = "Importar archivos")
                    }
                }
            }
        }

        if (folders.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                        Text("Sin carpetas", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        } else {
            items(folders, key = { it.folderPath }) { folder ->
                FolderCard(
                    folder = folder,
                    onClick = { onOpenFolder(folder.folderPath) },
                    onPlay = { onPlayFolder(folder) }
                )
            }
        }
    }

    if (showHiddenDialog) {
        AlertDialog(
            onDismissRequest = { showHiddenDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("Ocultas (${hiddenTracks.size})", style = MaterialTheme.typography.headlineSmall) },
            text = {
                if (hiddenTracks.isEmpty()) {
                    Text("No hay pistas ocultas.")
                } else {
                    LazyColumn(
                        modifier = Modifier.height((hiddenTracks.size.coerceAtMost(6) * 56).dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(hiddenTracks, key = { it.trackId }) { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(item.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                                IconButton(
                                    onClick = { onUnhideTrack(item.trackId) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Rounded.Visibility, contentDescription = "Mostrar de nuevo", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHiddenDialog = false }) { Text("Cerrar") }
            }
        )
    }
}

@Composable
private fun FolderCard(
    folder: FolderGroup,
    onClick: () -> Unit,
    onPlay: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("folder_card_${folder.folderName}"),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = folder.folderName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (folder.hasHiFi) {
                        HiFiBadge(label = "HI-FI", emphasized = true)
                    }
                }
                Text(
                    text = "${folder.trackCount} · ${formatDuration(folder.totalDurationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                    .testTag("folder_play_${folder.folderName}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Reproducir carpeta",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun FolderDetailScreen(
    folder: FolderGroup?,
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
    if (folder == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Carpeta no encontrada")
        }
        return
    }

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
                            .testTag("folder_detail_back")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(folder.folderName, style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(folder.folderPath, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onPlayTracks(folder.tracks, 0, folder.folderName, false) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play", maxLines = 1, softWrap = false)
                    }
                    FilledTonalButton(
                        onClick = { onPlayTracks(folder.tracks, 0, folder.folderName, true) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mezclar", maxLines = 1, softWrap = false)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        itemsIndexed(folder.tracks, key = { _, t -> "fld_track_${t.id}" }) { idx, track ->
            TrackRow(
                track = track,
                isCurrent = playbackState.currentTrack?.id == track.id,
                isPlaying = playbackState.isPlaying,
                onClick = { onPlayTracks(folder.tracks, idx, folder.folderName, false) },
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
