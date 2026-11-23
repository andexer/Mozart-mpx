package com.mozartmpx.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mozartmpx.app.data.model.Track

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val uri: String,
    val title: String,
    val artist: String,
    val albumArtist: String,
    val album: String,
    val albumId: Long,
    val genre: String,
    val year: Int,
    val trackNumber: Int,
    val durationMs: Long,
    val folderPath: String,
    val folderName: String,
    val mimeType: String,
    val formatLabel: String,
    val bitrateKbps: Int,
    val sampleRateHz: Int,
    val channels: Int,
    val fileSizeBytes: Long,
    val artworkUri: String?,
    val isFavorite: Boolean = false,
    val dateAdded: Long = 0L,
    val sourceType: String = "mediastore"
)

fun TrackEntity.toDomain(): Track = Track(
    id = id,
    uri = uri,
    title = title,
    artist = artist,
    albumArtist = albumArtist,
    album = album,
    albumId = albumId,
    genre = genre,
    year = year,
    trackNumber = trackNumber,
    durationMs = durationMs,
    folderPath = folderPath,
    folderName = folderName,
    mimeType = mimeType,
    formatLabel = formatLabel,
    bitrateKbps = bitrateKbps,
    sampleRateHz = sampleRateHz,
    channels = channels,
    fileSizeBytes = fileSizeBytes,
    artworkUri = artworkUri,
    isFavorite = isFavorite,
    dateAdded = dateAdded
)

@Entity(tableName = "hidden_tracks")
data class HiddenTrackEntity(
    @PrimaryKey val trackId: String,
    val uri: String,
    val title: String,
    val artist: String,
    val hiddenAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId"), Index("trackId")]
)
data class PlaylistTrackCrossRef(
    val playlistId: Long,
    val trackId: String,
    val position: Int,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "play_history")
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: String,
    val playedAt: Long = System.currentTimeMillis(),
    val completed: Boolean = true
)

@Entity(tableName = "taste_affinity")
data class TasteAffinityEntity(
    @PrimaryKey val key: String,
    val completedCount: Int = 0,
    val skipCount: Int = 0,
    val lastInteractedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_eq_presets")
data class CustomEqPresetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val bandLevelsCsv: String,
    val bassBoost: Int,
    val virtualizer: Int
)
