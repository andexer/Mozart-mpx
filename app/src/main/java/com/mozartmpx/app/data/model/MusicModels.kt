package com.mozartmpx.app.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class Track(
    val id: String,
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
    val isFavorite: Boolean,
    val dateAdded: Long
) {
    val isLossless: Boolean
        get() = formatLabel in setOf("FLAC", "WAV", "ALAC", "AIFF", "DSD")

    val isHiFi: Boolean
        get() = isLossless || sampleRateHz >= 88200

    val hiFiShortBadge: String
        get() = when {
            isLossless && sampleRateHz >= 88200 -> "HI-RES $formatLabel"
            isLossless -> formatLabel
            bitrateKbps >= 300 -> "$formatLabel 320k"
            else -> formatLabel
        }
}

@Immutable
data class ArtistGroup(
    val id: String,
    val name: String,
    val artworkUri: String?,
    val sampleTrackUri: String,
    val albumCount: Int,
    val trackCount: Int,
    val albums: List<AlbumGroup>,
    val tracks: List<Track>
)

@Immutable
data class AlbumGroup(
    val albumId: Long,
    val name: String,
    val artist: String,
    val year: Int,
    val artworkUri: String?,
    val sampleTrackUri: String,
    val trackCount: Int,
    val totalDurationMs: Long,
    val isHiFi: Boolean,
    val primaryFormat: String,
    val tracks: List<Track>
)

@Immutable
data class FolderGroup(
    val folderPath: String,
    val folderName: String,
    val trackCount: Int,
    val totalDurationMs: Long,
    val hasHiFi: Boolean,
    val sampleTrack: Track,
    val tracks: List<Track>
)

@Immutable
data class PlaylistSummary(
    val id: Long,
    val name: String,
    val description: String,
    val trackCount: Int,
    val coverTrack: Track?,
    val createdAt: Long
)

@Immutable
data class SmartSession(
    val id: String,
    val title: String,
    val subtitle: String,
    val familyKey: String,
    val tracks: List<Track>,
    val coverTrack: Track?
)

@Immutable
data class RecommendedTrack(
    val track: Track,
    val reason: String,
    val score: Double
)

enum class RepeatMode {
    OFF,
    ALL,
    ONE;

    fun next(): RepeatMode = when (this) {
        OFF -> ALL
        ALL -> ONE
        ONE -> OFF
    }
}

@Immutable
data class EqBandInfo(
    val index: Int,
    val centerFreqHz: Int,
    val levelMb: Int,
    val minLevelMb: Int,
    val maxLevelMb: Int
) {
    val freqLabel: String
        get() = if (centerFreqHz >= 1000) {
            val khz = centerFreqHz / 1000.0
            if (centerFreqHz % 1000 == 0) "${centerFreqHz / 1000}k" else String.format("%.1fk", khz)
        } else {
            "${centerFreqHz}Hz"
        }

    val dbLabel: String
        get() {
            val db = levelMb / 100f
            return if (db >= 0f) String.format("+%.1f dB", db) else String.format("%.1f dB", db)
        }
}

@Immutable
data class EqPreset(
    val id: String,
    val name: String,
    val bandLevelsMb: List<Int>,
    val bassBoost: Int,
    val virtualizer: Int,
    val isBuiltIn: Boolean = true
)

@Immutable
data class AudioFxState(
    val enabled: Boolean = false,
    val hardwareSupported: Boolean = true,
    val bands: List<EqBandInfo> = emptyList(),
    val presets: List<EqPreset> = emptyList(),
    val activePresetId: String = "flat",
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0
)

fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSeconds = ms / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "—"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1.0) String.format("%.1f MB", mb) else String.format("%d KB", bytes / 1024L)
}
