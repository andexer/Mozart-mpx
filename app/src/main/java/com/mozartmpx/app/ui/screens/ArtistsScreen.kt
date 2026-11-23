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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mozartmpx.app.data.model.AlbumGroup
import com.mozartmpx.app.data.model.ArtistGroup
import com.mozartmpx.app.data.model.Track
import com.mozartmpx.app.player.PlaybackUiState
import com.mozartmpx.app.ui.components.HiFiBadge
import com.mozartmpx.app.ui.components.MozartArtwork
import com.mozartmpx.app.ui.components.TrackRow

/**
 * Spotify / Deezer inspired library screen prioritizing Artists first, then Albums.
 */
@Composable
fun ArtistsAndAlbumsScreen(
    artists: List<ArtistGroup>,
    albums: List<AlbumGroup>,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onOpenSearch: () -> Unit,
    contentBottomPadding: PaddingValues = PaddingValues(bottom = 100.dp)
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Artistas (default), 1 = Álbumes

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    leadingIcon = {
                        Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    label = { Text("Artistas (${artists.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("tab_artists_chip")
                )
                FilterChip(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    leadingIcon = {
                        Icon(Icons.Rounded.Album, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    label = { Text("Álbumes (${albums.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("tab_albums_chip")
                )
            }

            IconButton(
                onClick = onOpenSearch,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("library_search_button")
            ) {
                Icon(Icons.Rounded.Search, contentDescription = "Buscar")
            }
        }

        if (selectedTab == 0) {
            if (artists.isEmpty()) {
                EmptyLibrarySectionIcon(icon = Icons.Rounded.Person, label = "Sin artistas")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 112.dp),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 6.dp,
                        bottom = contentBottomPadding.calculateBottomPadding() + 20.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    items(artists, key = { it.id }) { artist ->
                        ArtistCircleTile(
                            artist = artist,
                            onClick = { onOpenArtist(artist.id) }
                        )
                    }
                }
            }
        } else {
            if (albums.isEmpty()) {
                EmptyLibrarySectionIcon(icon = Icons.Rounded.Album, label = "Sin álbumes")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 152.dp),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 6.dp,
                        bottom = contentBottomPadding.calculateBottomPadding() + 20.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    items(albums, key = { it.albumId }) { album ->
                        AlbumGridCard(
                            album = album,
                            onClick = { onOpenAlbum(album.albumId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistCircleTile(
    artist: ArtistGroup,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(2.dp)
            .testTag("artist_tile_${artist.id}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MozartArtwork(
            artworkUri = artist.artworkUri,
            trackUri = artist.sampleTrackUri,
            contentDescription = artist.name,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape),
            shape = CircleShape,
            sizePx = 220
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            text = "${artist.albumCount} álb · ${artist.trackCount}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
fun ArtistDetailScreen(
    artist: ArtistGroup?,
    playbackState: PlaybackUiState,
    onBack: () -> Unit,
    onOpenAlbum: (Long) -> Unit,
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
    if (artist == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Artista no encontrado")
        }
        return
    }

    val topTint = lerp(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.primaryContainer, 0.5f)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = contentBottomPadding.calculateBottomPadding() + 20.dp)
    ) {
        // Artist Hero Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(topTint, MaterialTheme.colorScheme.background)
                        )
                    )
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
                            .testTag("artist_detail_back")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    FilledIconButton(
                        onClick = { onPlayTracks(artist.tracks, 0, artist.name, false) },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Reproducir artista")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalIconButton(
                        onClick = { onPlayTracks(artist.tracks, 0, artist.name, true) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = "Aleatorio")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                MozartArtwork(
                    artworkUri = artist.artworkUri,
                    trackUri = artist.sampleTrackUri,
                    contentDescription = artist.name,
                    modifier = Modifier
                        .size(156.dp)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), CircleShape),
                    shape = CircleShape,
                    sizePx = 360
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${artist.albumCount} álbumes · ${artist.trackCount} canciones",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Artist's Albums FIRST (Spotify / Deezer pattern!)
        if (artist.albums.isNotEmpty()) {
            item {
                Text(
                    text = "Álbumes",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(artist.albums, key = { it.albumId }) { album ->
                        Column(
                            modifier = Modifier
                                .width(136.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onOpenAlbum(album.albumId) }
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
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (album.year > 0) "${album.year} · ${album.trackCount}" else "${album.trackCount} pistas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Artist's Songs
        item {
            Text(
                text = "Canciones",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        itemsIndexed(artist.tracks, key = { _, t -> "art_track_${t.id}" }) { idx, track ->
            TrackRow(
                track = track,
                isCurrent = playbackState.currentTrack?.id == track.id,
                isPlaying = playbackState.isPlaying,
                onClick = { onPlayTracks(artist.tracks, idx, artist.name, false) },
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

@Composable
private fun EmptyLibrarySectionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), modifier = Modifier.size(48.dp))
            Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
