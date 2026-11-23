package com.mozartmpx.app.data.repository

import android.net.Uri
import androidx.room.withTransaction
import com.mozartmpx.app.data.local.HiddenTrackEntity
import com.mozartmpx.app.data.local.MozartDatabase
import com.mozartmpx.app.data.local.PlayHistoryEntity
import com.mozartmpx.app.data.local.PlaylistEntity
import com.mozartmpx.app.data.local.PlaylistTrackCrossRef
import com.mozartmpx.app.data.local.PreferencesRepository
import com.mozartmpx.app.data.local.TasteAffinityEntity
import com.mozartmpx.app.data.local.TrackTechInfo
import com.mozartmpx.app.data.local.UserAudioPreferences
import com.mozartmpx.app.data.local.toDomain
import com.mozartmpx.app.data.model.AlbumGroup
import com.mozartmpx.app.data.model.ArtistGroup
import com.mozartmpx.app.data.model.FolderGroup
import com.mozartmpx.app.data.model.PlaylistSummary
import com.mozartmpx.app.data.model.RepeatMode
import com.mozartmpx.app.data.model.Track
import com.mozartmpx.app.data.scanner.DeviceDeleteResult
import com.mozartmpx.app.data.scanner.LocalAudioScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.text.Collator
import java.util.Locale

class MusicRepository(
    private val database: MozartDatabase,
    private val scanner: LocalAudioScanner,
    val preferencesRepository: PreferencesRepository
) {
    private val trackDao = database.trackDao()

    private fun spanishCollator() = Collator.getInstance(Locale.forLanguageTag("es")).apply {
        strength = Collator.SECONDARY
    }
    private val playlistDao = database.playlistDao()
    private val historyDao = database.historyAffinityDao()

    val preferences: Flow<UserAudioPreferences> = preferencesRepository.preferences

    val allTracks: Flow<List<Track>> = trackDao.observeAllTracks()
        .map { list -> list.map { it.toDomain() } }
        .flowOn(Dispatchers.Default)

    val hiddenTracks: Flow<List<HiddenTrackEntity>> = trackDao.observeHiddenTracks()

    val favoriteTracks: Flow<List<Track>> = trackDao.observeFavoriteTracks()
        .map { list -> list.map { it.toDomain() } }
        .flowOn(Dispatchers.Default)

    val recentlyPlayed: Flow<List<Track>> = historyDao.observeRecentlyPlayed()
        .map { list -> list.map { it.toDomain() } }
        .flowOn(Dispatchers.Default)

    val mostPlayed: Flow<List<Track>> = historyDao.observeMostPlayed()
        .map { list -> list.map { it.toDomain() } }
        .flowOn(Dispatchers.Default)

    val affinities: Flow<Map<String, TasteAffinityEntity>> = historyDao.observeAllAffinities()
        .map { list -> list.associateBy { it.key } }
        .flowOn(Dispatchers.Default)

    val albums: Flow<List<AlbumGroup>> = allTracks.map { tracks ->
        buildAlbumGroups(tracks)
    }.flowOn(Dispatchers.Default)

    val artists: Flow<List<ArtistGroup>> = allTracks.map { tracks ->
        val nameCollator = Collator.getInstance(Locale.forLanguageTag("es")).apply {
            strength = Collator.SECONDARY
        }
        fun key(value: String) = nameCollator.getCollationKey(value.trim())
            .toByteArray().joinToString("") { "%02x".format(it) }
        tracks.groupBy { key(it.artist) }
            .values
            .map { artistTracks ->
                val displayName = artistTracks.first().artist
                val artistAlbums = buildAlbumGroups(artistTracks)
                val coverTrack = artistTracks.firstOrNull { !it.artworkUri.isNullOrBlank() } ?: artistTracks.first()
                ArtistGroup(
                    id = key(displayName),
                    name = displayName,
                    artworkUri = coverTrack.artworkUri,
                    sampleTrackUri = coverTrack.uri,
                    albumCount = artistAlbums.size,
                    trackCount = artistTracks.size,
                    albums = artistAlbums,
                    tracks = artistTracks.sortedWith(compareBy(nameCollator) { it.title })
                )
            }
            .sortedWith(
                compareBy<ArtistGroup> { it.name.equals("Artista desconocido", true) }
                    .then(compareBy(nameCollator) { it.name })
            )
    }.flowOn(Dispatchers.Default)

    val folders: Flow<List<FolderGroup>> = allTracks.map { tracks ->
        val collator = spanishCollator()
        tracks.groupBy { it.folderPath }
            .values
            .map { group ->
                val sorted = group.sortedWith(compareBy<Track>({ it.trackNumber == 0 }, { it.trackNumber }, { it.title }))
                val first = sorted.first()
                FolderGroup(
                    folderPath = first.folderPath,
                    folderName = first.folderName,
                    trackCount = sorted.size,
                    totalDurationMs = sorted.sumOf { it.durationMs },
                    hasHiFi = sorted.any { it.isHiFi },
                    sampleTrack = first,
                    tracks = sorted
                )
            }
            .sortedWith(compareBy(collator) { it.folderName })
    }.flowOn(Dispatchers.Default)

    val playlists: Flow<List<PlaylistSummary>> = combine(
        playlistDao.observePlaylists(),
        playlistDao.observeAllPlaylistRefs(),
        allTracks
    ) { playlistEntities, refs, tracks ->
        val tracksById = tracks.associateBy { it.id }
        val refsByPlaylist = refs.groupBy { it.playlistId }
        playlistEntities.map { pl ->
            val plTracks = refsByPlaylist[pl.id].orEmpty().mapNotNull { tracksById[it.trackId] }
            PlaylistSummary(
                id = pl.id,
                name = pl.name,
                description = pl.description,
                trackCount = plTracks.size,
                coverTrack = plTracks.firstOrNull(),
                createdAt = pl.createdAt
            )
        }
    }.flowOn(Dispatchers.Default)

    private fun buildAlbumGroups(tracks: List<Track>): List<AlbumGroup> {
        val collator = spanishCollator()
        return tracks.groupBy { it.albumId }
            .values
            .map { group ->
                val sorted = group.sortedWith(compareBy<Track>({ it.trackNumber == 0 }, { it.trackNumber }, { it.title }))
                val first = sorted.first()
                AlbumGroup(
                    albumId = first.albumId,
                    name = first.album,
                    artist = first.albumArtist.ifBlank { first.artist },
                    year = sorted.maxOfOrNull { it.year } ?: 0,
                    artworkUri = sorted.firstNotNullOfOrNull { it.artworkUri },
                    sampleTrackUri = first.uri,
                    trackCount = sorted.size,
                    totalDurationMs = sorted.sumOf { it.durationMs },
                    isHiFi = sorted.any { it.isHiFi },
                    primaryFormat = sorted.firstOrNull { it.isHiFi }?.formatLabel ?: sorted.first().formatLabel,
                    tracks = sorted
                )
            }
            .sortedWith(compareBy(collator) { it.name })
    }

    fun observePlaylistTracks(playlistId: Long): Flow<List<Track>> {
        return playlistDao.observeTracksForPlaylist(playlistId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.Default)
    }

    suspend fun hasCachedTracks(): Boolean = trackDao.getTrackCount() > 0

    suspend fun scanDeviceLibrary(includeSafFolders: Boolean = true): Int {
        val favIds = trackDao.getFavoriteTrackIds().toSet()
        val scanned = runCatching { scanner.scanMediaStore(favIds) }.getOrElse { return 0 }
        // Nunca vaciar la caché sana si el escaneo falla o devuelve cero pistas
        // mientras existen favoritos: Suele indicar un error de permisos/consulta.
        if (scanned.isEmpty() && favIds.isNotEmpty()) return 0

        val previousTech = trackDao.getMediaStoreTech().associateBy { it.id }
        val scannedMediaStore = scanned.map { entry ->
            val old = previousTech[entry.id]
            if (old != null && entry.sampleRateHz <= 44100 && old.sampleRateHz > 44100) {
                entry.copy(
                    sampleRateHz = old.sampleRateHz,
                    bitrateKbps = old.bitrateKbps,
                    channels = old.channels
                )
            } else entry
        }

        database.withTransaction {
            trackDao.clearMediaStoreTracks()
            scannedMediaStore.chunked(400).forEach { chunk ->
                trackDao.upsertTracks(chunk)
            }
        }

        var safCount = 0
        if (includeSafFolders) {
            val safUris = preferencesRepository.preferences.first().safTreeUris
            for (uriStr in safUris) {
                val safTracks = scanner.scanSafTree(Uri.parse(uriStr), favIds)
                if (safTracks.isNotEmpty()) {
                    database.withTransaction {
                        safTracks.chunked(400).forEach { chunk ->
                            trackDao.upsertTracks(chunk)
                        }
                    }
                    safCount += safTracks.size
                }
            }
        }
        return scannedMediaStore.size + safCount
    }

    suspend fun enrichTrackIfNeeded(track: Track): Track {
        val enriched = scanner.enrichSingleTrack(track) ?: return track
        if (enriched.sampleRateHz != track.sampleRateHz || enriched.bitrateKbps != track.bitrateKbps) {
            trackDao.updateTechMetadata(track.id, enriched.sampleRateHz, enriched.bitrateKbps, enriched.channels)
            return track.copy(
                sampleRateHz = enriched.sampleRateHz,
                bitrateKbps = enriched.bitrateKbps,
                channels = enriched.channels
            )
        }
        return track
    }

    suspend fun hideTrackPermanently(track: Track) {
        trackDao.hideTrack(
            HiddenTrackEntity(
                trackId = track.id,
                uri = track.uri,
                title = track.title,
                artist = track.artist
            )
        )
    }

    suspend fun unhideTrack(trackId: String) {
        trackDao.unhideTrack(trackId)
    }

    suspend fun requestDeleteFromDevice(track: Track): DeviceDeleteResult {
        val result = scanner.deleteTrackFromDevice(track)
        if (result is DeviceDeleteResult.Deleted) {
            finalizeTrackDeleted(track.id)
        }
        return result
    }

    suspend fun finalizeTrackDeleted(trackId: String) {
        database.withTransaction {
            playlistDao.removeTrackFromAllPlaylists(trackId)
            historyDao.deleteHistoryForTrack(trackId)
            trackDao.deleteTrackById(trackId)
        }
    }

    suspend fun addSafFolder(treeUri: Uri): Int {
        preferencesRepository.addSafTreeUri(treeUri.toString())
        val favIds = trackDao.getFavoriteTrackIds().toSet()
        val tracks = scanner.scanSafTree(treeUri, favIds)
        if (tracks.isNotEmpty()) {
            database.withTransaction {
                tracks.chunked(400).forEach { trackDao.upsertTracks(it) }
            }
        }
        return tracks.size
    }

    suspend fun importLocalAudioFiles(uris: List<Uri>): Int {
        val favIds = trackDao.getFavoriteTrackIds().toSet()
        val tracks = scanner.importDocumentUris(uris, favIds)
        if (tracks.isNotEmpty()) {
            database.withTransaction {
                tracks.chunked(400).forEach { trackDao.upsertTracks(it) }
            }
        }
        return tracks.size
    }

    suspend fun toggleFavorite(trackId: String): Boolean {
        val current = trackDao.getTrackById(trackId) ?: return false
        val newFav = !current.isFavorite
        trackDao.setFavorite(trackId, newFav)
        if (newFav) {
            recordTasteSignal("artist:${normalizeKey(current.artist)}", completed = true)
            if (current.genre.isNotBlank()) {
                recordTasteSignal("genre:${normalizeKey(current.genre)}", completed = true)
            }
        }
        return newFav
    }

    suspend fun createPlaylist(name: String, description: String = ""): Long {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "El nombre de la lista no puede estar vacío." }
        return playlistDao.insertPlaylist(PlaylistEntity(name = cleanName, description = description.trim()))
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String, description: String = "") {
        val cleanName = newName.trim()
        if (cleanName.isNotEmpty()) {
            playlistDao.updatePlaylist(playlistId, cleanName, description.trim())
        }
    }

    suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: String) {
        val nextPos = playlistDao.getMaxPosition(playlistId) + 1
        playlistDao.insertPlaylistTrack(
            PlaylistTrackCrossRef(playlistId = playlistId, trackId = trackId, position = nextPos)
        )
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String) {
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)
    }

    suspend fun recordPlaybackInteraction(track: Track, familyKey: String, completed: Boolean) {
        historyDao.insertHistory(PlayHistoryEntity(trackId = track.id, completed = completed))
        recordTasteSignal("track:${track.id}", completed)
        recordTasteSignal("artist:${normalizeKey(track.artist)}", completed)
        recordTasteSignal("family:$familyKey", completed)
        if (track.genre.isNotBlank()) {
            recordTasteSignal("genre:${normalizeKey(track.genre)}", completed)
        }
    }

    private suspend fun recordTasteSignal(key: String, completed: Boolean) {
        val existing = historyDao.getAffinity(key)
        val updated = if (existing == null) {
            TasteAffinityEntity(
                key = key,
                completedCount = if (completed) 1 else 0,
                skipCount = if (completed) 0 else 1,
                lastInteractedAt = System.currentTimeMillis()
            )
        } else {
            existing.copy(
                completedCount = existing.completedCount + if (completed) 1 else 0,
                skipCount = existing.skipCount + if (completed) 0 else 1,
                lastInteractedAt = System.currentTimeMillis()
            )
        }
        historyDao.upsertAffinity(updated)
    }

    suspend fun setCrossfadeSeconds(seconds: Int) = preferencesRepository.setCrossfadeSeconds(seconds)
    suspend fun setSmartAutoplayEnabled(enabled: Boolean) = preferencesRepository.setSmartAutoplayEnabled(enabled)
    suspend fun setShuffleEnabled(enabled: Boolean) = preferencesRepository.setShuffleEnabled(enabled)
    suspend fun setRepeatMode(mode: RepeatMode) = preferencesRepository.setRepeatMode(mode)

    private fun normalizeKey(raw: String): String = raw.trim().lowercase(Locale.ROOT)
}
