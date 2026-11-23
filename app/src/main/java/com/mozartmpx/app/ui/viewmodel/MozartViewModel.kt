package com.mozartmpx.app.ui.viewmodel

import android.content.IntentSender
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mozartmpx.app.MozartContainer
import com.mozartmpx.app.data.local.HiddenTrackEntity
import com.mozartmpx.app.data.model.AlbumGroup
import com.mozartmpx.app.data.model.ArtistGroup
import com.mozartmpx.app.data.model.AudioFxState
import com.mozartmpx.app.data.model.FolderGroup
import com.mozartmpx.app.data.model.PlaylistSummary
import com.mozartmpx.app.data.model.SmartSession
import com.mozartmpx.app.data.model.Track
import com.mozartmpx.app.data.repository.MusicRepository
import com.mozartmpx.app.data.scanner.DeviceDeleteResult
import com.mozartmpx.app.domain.SmartQueueEngine
import com.mozartmpx.app.player.AudioFxController
import com.mozartmpx.app.player.MozartPlaybackEngine
import com.mozartmpx.app.player.PlaybackUiState
import com.mozartmpx.app.ui.theme.AlbumPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

@Immutable
data class LibraryUiState(
    val allTracks: List<Track> = emptyList(),
    val artists: List<ArtistGroup> = emptyList(),
    val albums: List<AlbumGroup> = emptyList(),
    val folders: List<FolderGroup> = emptyList(),
    val playlists: List<PlaylistSummary> = emptyList(),
    val favorites: List<Track> = emptyList(),
    val recentlyPlayed: List<Track> = emptyList(),
    val mostPlayed: List<Track> = emptyList(),
    val smartSessions: List<SmartSession> = emptyList(),
    val hiFiTracks: List<Track> = emptyList(),
    val hiddenCount: Int = 0,
    val isScanning: Boolean = false,
    val hasAudioPermission: Boolean = false
)

data class PendingDeviceDelete(
    val track: Track,
    val intentSender: IntentSender
)

class MozartViewModel(
    private val repository: MusicRepository,
    private val smartQueueEngine: SmartQueueEngine,
    private val playbackEngine: MozartPlaybackEngine,
    private val audioFxController: AudioFxController
) : ViewModel() {

    private val _isScanning = MutableStateFlow(false)
    private val _hasAudioPermission = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    private val _deleteConfirmationEvents = MutableSharedFlow<PendingDeviceDelete>(extraBufferCapacity = 4)
    val deleteConfirmationEvents: SharedFlow<PendingDeviceDelete> = _deleteConfirmationEvents.asSharedFlow()

    val playbackState: StateFlow<PlaybackUiState> = playbackEngine.playbackState
    val playbackPositionMs: StateFlow<Long> = playbackEngine.positionMs
    val albumPalette: StateFlow<AlbumPalette> = playbackEngine.albumPalette
    val audioFxState: StateFlow<AudioFxState> = audioFxController.state

    val hiddenTracks: StateFlow<List<HiddenTrackEntity>> = repository.hiddenTracks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private val derivedSessionsAndHiFi = repository.allTracks
        .map { tracks ->
            DerivedCatalog(
                smartSessions = smartQueueEngine.buildSmartSessions(tracks),
                hiFiTracks = tracks.filter { it.isHiFi }
            )
        }
        .flowOn(Dispatchers.Default)

    private val catalogFlow = combine(
        repository.allTracks,
        repository.artists,
        repository.albums,
        repository.folders,
        repository.playlists
    ) { tracks, artists, albums, folders, playlists ->
        CatalogBundle(tracks, artists, albums, folders, playlists)
    }.flowOn(Dispatchers.Default)

    private val userFlow = combine(
        repository.favoriteTracks,
        repository.recentlyPlayed,
        repository.mostPlayed,
        repository.hiddenTracks.map { it.size },
        derivedSessionsAndHiFi
    ) { favorites, recent, most, hiddenCount, derived ->
        UserBundle(favorites, recent, most, hiddenCount, derived)
    }.flowOn(Dispatchers.Default)

    val libraryState: StateFlow<LibraryUiState> = combine(
        catalogFlow,
        userFlow,
        _isScanning,
        _hasAudioPermission
    ) { cat, user, scanning, perm ->
        LibraryUiState(
            allTracks = cat.tracks,
            artists = cat.artists,
            albums = cat.albums,
            folders = cat.folders,
            playlists = cat.playlists,
            favorites = user.favorites,
            recentlyPlayed = user.recent,
            mostPlayed = user.most,
            smartSessions = user.derived.smartSessions,
            hiFiTracks = user.derived.hiFiTracks,
            hiddenCount = user.hiddenCount,
            isScanning = scanning,
            hasAudioPermission = perm
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState()
    )

    val filteredSearchTracks: StateFlow<List<Track>> = combine(
        repository.allTracks,
        _searchQuery
    ) { tracks, query ->
        val q = query.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) emptyList()
        else tracks.filter { t ->
            t.title.lowercase(Locale.ROOT).contains(q) ||
                t.artist.lowercase(Locale.ROOT).contains(q) ||
                t.album.lowercase(Locale.ROOT).contains(q) ||
                t.genre.lowercase(Locale.ROOT).contains(q) ||
                t.folderName.lowercase(Locale.ROOT).contains(q) ||
                t.formatLabel.lowercase(Locale.ROOT).contains(q)
        }.take(100)
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun observePlaylistTracks(playlistId: Long): Flow<List<Track>> =
        repository.observePlaylistTracks(playlistId)

    fun setAudioPermissionGranted(granted: Boolean, triggerScan: Boolean = true) {
        val changed = _hasAudioPermission.value != granted
        _hasAudioPermission.value = granted
        if (granted && (triggerScan || changed)) {
            rescanLibrary(silent = true)
        }
    }

    fun rescanLibrary(silent: Boolean = false) {
        if (_isScanning.value) return
        viewModelScope.launch {
            val hadCache = repository.hasCachedTracks()
            // Only show scanning indicator if user requested manually or cache is empty
            if (!silent || !hadCache) {
                _isScanning.value = true
            }
            try {
                val count = repository.scanDeviceLibrary(includeSafFolders = true)
                if (!silent) {
                    _snackbarEvents.tryEmit(
                        if (count > 0) "$count pistas actualizadas"
                        else "Sin archivos de audio locales"
                    )
                }
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun addLocalFolder(treeUri: Uri) {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val count = repository.addSafFolder(treeUri)
                _snackbarEvents.tryEmit(
                    if (count > 0) "$count pistas añadidas"
                    else "Sin audio compatible en esa carpeta"
                )
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun importAudioFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val count = repository.importLocalAudioFiles(uris)
                _snackbarEvents.tryEmit(
                    if (count > 0) "$count pistas importadas"
                    else "No se pudieron leer los archivos"
                )
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Hide permanently from app & Delete from device storage
    fun hideTrackPermanently(track: Track) {
        viewModelScope.launch {
            repository.hideTrackPermanently(track)
            playbackEngine.onTrackRemovedOrHidden(track.id)
            _snackbarEvents.tryEmit("Ocultada: ${track.title}")
        }
    }

    fun unhideTrack(trackId: String) {
        viewModelScope.launch {
            repository.unhideTrack(trackId)
            _snackbarEvents.tryEmit("Pista restaurada en la biblioteca")
        }
    }

    fun deleteTrackFromDevice(track: Track) {
        viewModelScope.launch {
            when (val res = repository.requestDeleteFromDevice(track)) {
                is DeviceDeleteResult.Deleted -> {
                    playbackEngine.onTrackRemovedOrHidden(track.id)
                    _snackbarEvents.tryEmit("Eliminada del dispositivo: ${track.title}")
                }
                is DeviceDeleteResult.RequiresUserConfirmation -> {
                    _deleteConfirmationEvents.tryEmit(PendingDeviceDelete(track, res.intentSender))
                }
                is DeviceDeleteResult.Failed -> {
                    _snackbarEvents.tryEmit(res.reason)
                }
            }
        }
    }

    fun onDeviceDeleteConfirmed(track: Track) {
        viewModelScope.launch {
            repository.finalizeTrackDeleted(track.id)
            playbackEngine.onTrackRemovedOrHidden(track.id)
            _snackbarEvents.tryEmit("Eliminada del dispositivo: ${track.title}")
        }
    }

    fun enrichTrackForInspection(track: Track, onReady: (Track) -> Unit) {
        viewModelScope.launch {
            onReady(repository.enrichTrackIfNeeded(track))
        }
    }

    // Playback actions (instant, non-blocking)
    fun playTracks(tracks: List<Track>, startIndex: Int = 0, sessionLabel: String = "", shuffle: Boolean? = null) {
        playbackEngine.playTrackList(tracks, startIndex, sessionLabel, shuffle)
    }

    fun playTrack(track: Track) = playbackEngine.playTrackNow(track)
    fun togglePlayPause() = playbackEngine.togglePlayPause()
    fun seekTo(positionMs: Long) = playbackEngine.seekTo(positionMs)
    fun skipNext() = playbackEngine.skipToNext(isManualSkip = true)
    fun skipPrevious() = playbackEngine.skipToPrevious()
    fun toggleShuffle() = playbackEngine.toggleShuffle()
    fun cycleRepeatMode() = playbackEngine.cycleRepeatMode()
    fun setCrossfadeSeconds(seconds: Int) = playbackEngine.setCrossfadeSeconds(seconds)
    fun setSmartAutoplayEnabled(enabled: Boolean) = playbackEngine.setSmartAutoplayEnabled(enabled)

    fun playNextInQueue(track: Track) {
        playbackEngine.playNextInQueue(track)
        _snackbarEvents.tryEmit("Siguiente: ${track.title}")
    }

    fun addToQueue(track: Track) {
        playbackEngine.addToQueue(track)
        _snackbarEvents.tryEmit("En cola: ${track.title}")
    }

    fun removeFromManualQueue(trackId: String) = playbackEngine.removeFromManualQueue(trackId)
    fun removeFromSessionQueue(trackId: String) = playbackEngine.removeFromSessionQueue(trackId)
    fun clearManualQueue() = playbackEngine.clearManualQueue()

    // Favorites & Playlists
    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            val nowFav = repository.toggleFavorite(track.id)
            _snackbarEvents.tryEmit(if (nowFav) "En Favoritas" else "Quitada de Favoritas")
        }
    }

    fun createPlaylist(name: String, description: String = "", initialTrack: Track? = null) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val id = repository.createPlaylist(trimmed, description)
            if (initialTrack != null) {
                repository.addTrackToPlaylist(id, initialTrack.id)
                _snackbarEvents.tryEmit("Lista «$trimmed» creada")
            } else {
                _snackbarEvents.tryEmit("Lista «$trimmed» creada")
            }
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String, description: String = "") {
        viewModelScope.launch {
            repository.renamePlaylist(playlistId, newName, description)
            _snackbarEvents.tryEmit("Lista actualizada")
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            _snackbarEvents.tryEmit("Lista eliminada")
        }
    }

    fun addTrackToPlaylist(playlistId: Long, playlistName: String, track: Track) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, track.id)
            _snackbarEvents.tryEmit("Añadida a «$playlistName»")
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: String) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, trackId)
            _snackbarEvents.tryEmit("Quitada de la lista")
        }
    }

    // Equalizer & Audio FX
    fun setEqEnabled(enabled: Boolean) = audioFxController.setEnabled(enabled)
    fun selectEqPreset(presetId: String) = audioFxController.selectPreset(presetId)
    fun setEqBandLevel(bandIndex: Int, levelMb: Int) = audioFxController.setBandLevel(bandIndex, levelMb)
    fun setBassBoost(strength: Int) = audioFxController.setBassBoost(strength)
    fun setVirtualizer(strength: Int) = audioFxController.setVirtualizer(strength)
    fun resetEqualizer() = audioFxController.resetFlat()

    private data class DerivedCatalog(
        val smartSessions: List<SmartSession>,
        val hiFiTracks: List<Track>
    )

    private data class CatalogBundle(
        val tracks: List<Track>,
        val artists: List<ArtistGroup>,
        val albums: List<AlbumGroup>,
        val folders: List<FolderGroup>,
        val playlists: List<PlaylistSummary>
    )

    private data class UserBundle(
        val favorites: List<Track>,
        val recent: List<Track>,
        val most: List<Track>,
        val hiddenCount: Int,
        val derived: DerivedCatalog
    )

    companion object {
        fun factory(container: MozartContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MozartViewModel(
                        repository = container.musicRepository,
                        smartQueueEngine = container.smartQueueEngine,
                        playbackEngine = container.playbackEngine,
                        audioFxController = container.audioFxController
                    ) as T
                }
            }
    }
}
