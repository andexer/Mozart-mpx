package com.mozartmpx.app.data.scanner

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.mozartmpx.app.data.local.TrackEntity
import com.mozartmpx.app.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.abs

sealed interface DeviceDeleteResult {
    data object Deleted : DeviceDeleteResult
    data class RequiresUserConfirmation(val intentSender: IntentSender) : DeviceDeleteResult
    data class Failed(val reason: String) : DeviceDeleteResult
}

data class EnrichedAudioTech(
    val sampleRateHz: Int,
    val bitrateKbps: Int,
    val channels: Int
)

class LocalAudioScanner(private val context: Context) {

    private val supportedExtensions = setOf(
        "flac", "wav", "alac", "aiff", "aif", "mp3", "m4a", "aac", "ogg", "opus", "wma", "dsf", "dff", "mka"
    )

    /**
     * Ultra-fast single-query MediaStore scan (<200ms even for 3,000+ tracks).
     * Never opens MediaMetadataRetriever in a loop; deep technical metadata is fetched
     * lazily only for the active or inspected track.
     */
    suspend fun scanMediaStore(favoriteIds: Set<String>): List<TrackEntity> = withContext(Dispatchers.IO) {
        val tracks = ArrayList<TrackEntity>(512)
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.ALBUM_ID)
            add(MediaStore.Audio.Media.TRACK)
            add(MediaStore.Audio.Media.YEAR)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.MIME_TYPE)
            add(MediaStore.Audio.Media.SIZE)
            add(MediaStore.Audio.Media.DATE_ADDED)
            add(MediaStore.Audio.Media.DISPLAY_NAME)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                add(MediaStore.Audio.Media.DATA)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Audio.Media.RELATIVE_PATH)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(MediaStore.Audio.Media.GENRE)
                add(MediaStore.Audio.Media.ALBUM_ARTIST)
                add(MediaStore.Audio.Media.BITRATE)
            }
        }.toTypedArray()

        val selection = "(${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.DURATION} >= 15000)"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        val albumArtBaseUri = Uri.parse("content://media/external/audio/albumart")

        context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val displayCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val dataCol = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    @Suppress("DEPRECATION")
                    cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                } else -1
                val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                } else -1
                val genreCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
                } else -1
                val albumArtistCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ARTIST)
                } else -1
                val bitrateCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.BITRATE)
                } else -1

                while (cursor.moveToNext()) {
                    val mediaId = cursor.getLong(idCol)
                    val durationMs = cursor.getLong(durationCol)
                    if (durationMs < 5_000L) continue

                    val contentUri = ContentUris.withAppendedId(collection, mediaId)
                    val displayName = cursor.getString(displayCol).orEmpty()
                    val mimeType = cursor.getString(mimeCol).orEmpty()
                    val formatLabel = detectFormatLabel(mimeType, displayName)

                    val rawTitle = cursor.getString(titleCol).orEmpty().trim()
                    val title = if (rawTitle.isBlank() || rawTitle.equals("<unknown>", true)) {
                        displayName.substringBeforeLast('.').ifBlank { "Pista sin título" }
                    } else rawTitle

                    val rawArtist = cursor.getString(artistCol).orEmpty().trim()
                    val artist = if (rawArtist.isBlank() || rawArtist.equals("<unknown>", true)) {
                        "Artista desconocido"
                    } else rawArtist

                    val rawAlbum = cursor.getString(albumCol).orEmpty().trim()
                    val album = if (rawAlbum.isBlank() || rawAlbum.equals("<unknown>", true)) {
                        "Sencillos locales"
                    } else rawAlbum

                    val albumId = cursor.getLong(albumIdCol)
                    val albumArtUri = if (albumId > 0) {
                        ContentUris.withAppendedId(albumArtBaseUri, albumId).toString()
                    } else null

                    val dataPath = if (dataCol >= 0) cursor.getString(dataCol).orEmpty() else ""
                    val relPath = if (relPathCol >= 0) cursor.getString(relPathCol).orEmpty() else ""
                    val (folderPath, folderName) = extractFolderInfo(relPath, dataPath)

                    val rawGenre = if (genreCol >= 0) cursor.getString(genreCol).orEmpty().trim() else ""
                    val albumArtist = if (albumArtistCol >= 0) {
                        cursor.getString(albumArtistCol).orEmpty().trim().ifBlank { artist }
                    } else artist

                    val rawBitrateBps = if (bitrateCol >= 0) cursor.getInt(bitrateCol) else 0
                    val sizeBytes = cursor.getLong(sizeCol)
                    val estimatedKbps = when {
                        rawBitrateBps > 0 -> rawBitrateBps / 1000
                        durationMs > 0 && sizeBytes > 0 -> ((sizeBytes * 8L) / durationMs).toInt()
                        else -> 0
                    }

                    val trackId = "ms_$mediaId"
                    tracks += TrackEntity(
                        id = trackId,
                        uri = contentUri.toString(),
                        title = title,
                        artist = artist,
                        albumArtist = albumArtist,
                        album = album,
                        albumId = albumId,
                        genre = rawGenre.ifBlank { inferGenreFromFolder(folderName) },
                        year = cursor.getInt(yearCol),
                        trackNumber = cursor.getInt(trackCol) % 1000,
                        durationMs = durationMs,
                        folderPath = folderPath,
                        folderName = folderName,
                        mimeType = mimeType.ifBlank { "audio/*" },
                        formatLabel = formatLabel,
                        bitrateKbps = estimatedKbps,
                        sampleRateHz = 44100,
                        channels = 2,
                        fileSizeBytes = sizeBytes,
                        artworkUri = albumArtUri,
                        isFavorite = favoriteIds.contains(trackId),
                        dateAdded = cursor.getLong(dateCol) * 1000L,
                        sourceType = "mediastore"
                    )
                }
        }
        tracks
    }

    /**
     * Lazy on-demand technical metadata extraction for ONE track only (active or inspected).
     */
    suspend fun enrichSingleTrack(track: Track): EnrichedAudioTech? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.parse(track.uri))
            val bitrateBps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                ?.toIntOrNull() ?: 0
            val sampleRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull() ?: track.sampleRateHz
            } else track.sampleRateHz
            val bitrateKbps = if (bitrateBps > 0) bitrateBps / 1000 else track.bitrateKbps
            EnrichedAudioTech(
                sampleRateHz = sampleRate.coerceAtLeast(8000),
                bitrateKbps = bitrateKbps,
                channels = 2
            )
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    /**
     * Deletes an audio file from the device storage complying with Android 10/11+ Scoped Storage.
     */
    suspend fun deleteTrackFromDevice(track: Track): DeviceDeleteResult = withContext(Dispatchers.IO) {
        val uri = runCatching { Uri.parse(track.uri) }.getOrNull()
            ?: return@withContext DeviceDeleteResult.Failed("URI inválida")

        // SAF DocumentFile deletion
        if (track.id.startsWith("saf_")) {
            val deleted = runCatching { DocumentFile.fromSingleUri(context, uri)?.delete() == true }
                .getOrDefault(false)
            return@withContext if (deleted) DeviceDeleteResult.Deleted
            else DeviceDeleteResult.Failed("No se pudo eliminar el archivo de la carpeta externa")
        }

        // Android 11+ (API 30+): Official MediaStore.createDeleteRequest confirmation dialog
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return@withContext try {
                val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
                DeviceDeleteResult.RequiresUserConfirmation(pendingIntent.intentSender)
            } catch (e: Exception) {
                DeviceDeleteResult.Failed(e.localizedMessage ?: "No se pudo solicitar la eliminación")
            }
        }

        // Android 10 & below
        try {
            val rows = context.contentResolver.delete(uri, null, null)
            if (rows > 0) DeviceDeleteResult.Deleted
            else DeviceDeleteResult.Failed("El archivo no se encontró o ya fue eliminado")
        } catch (securityException: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && securityException is RecoverableSecurityException) {
                DeviceDeleteResult.RequiresUserConfirmation(securityException.userAction.actionIntent.intentSender)
            } else {
                DeviceDeleteResult.Failed("Permiso denegado por el sistema")
            }
        } catch (e: Exception) {
            DeviceDeleteResult.Failed(e.localizedMessage ?: "Error al eliminar el archivo")
        }
    }

    suspend fun scanSafTree(treeUri: Uri, favoriteIds: Set<String>): List<TrackEntity> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        val result = mutableListOf<TrackEntity>()
        val rootLabel = root.name ?: "Carpeta local"
        traverseDocumentTree(root, rootLabel, favoriteIds, result, depth = 0)
        result
    }

    suspend fun importDocumentUris(uris: List<Uri>, favoriteIds: Set<String>): List<TrackEntity> = withContext(Dispatchers.IO) {
        uris.mapNotNull { uri ->
            val doc = DocumentFile.fromSingleUri(context, uri)
            val fileName = doc?.name ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Audio local"
            buildTrackFromUri(
                uri = uri,
                fileName = fileName,
                mimeType = doc?.type ?: context.contentResolver.getType(uri).orEmpty(),
                fileSize = doc?.length() ?: 0L,
                folderPath = "Importados",
                folderName = "Importados",
                favoriteIds = favoriteIds
            )
        }
    }

    private fun traverseDocumentTree(
        dir: DocumentFile,
        currentPath: String,
        favoriteIds: Set<String>,
        out: MutableList<TrackEntity>,
        depth: Int
    ) {
        if (depth > 5) return
        val children = runCatching { dir.listFiles() }.getOrDefault(emptyArray())
        for (child in children) {
            if (child.isDirectory) {
                val subName = child.name ?: "Subcarpeta"
                traverseDocumentTree(child, "$currentPath/$subName", favoriteIds, out, depth + 1)
            } else if (child.isFile) {
                val name = child.name ?: continue
                val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
                val mime = child.type.orEmpty()
                if (mime.startsWith("audio/") || ext in supportedExtensions) {
                    val folderName = currentPath.substringAfterLast('/')
                    buildTrackFromUri(
                        uri = child.uri,
                        fileName = name,
                        mimeType = mime,
                        fileSize = child.length(),
                        folderPath = currentPath,
                        folderName = folderName,
                        favoriteIds = favoriteIds
                    )?.let { out += it }
                }
            }
        }
    }

    private fun buildTrackFromUri(
        uri: Uri,
        fileName: String,
        mimeType: String,
        fileSize: Long,
        folderPath: String,
        folderName: String,
        favoriteIds: Set<String>
    ): TrackEntity? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            if (durationMs < 3_000L) return null

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.trim()?.takeIf { it.isNotBlank() }
                ?: fileName.substringBeforeLast('.').ifBlank { "Pista local" }
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.trim()?.takeIf { it.isNotBlank() }
                ?: "Artista desconocido"
            val albumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                ?.trim()?.takeIf { it.isNotBlank() }
                ?: artist
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.trim()?.takeIf { it.isNotBlank() }
                ?: folderName
            val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                ?.trim().orEmpty().ifBlank { inferGenreFromFolder(folderName) }
            val year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                ?.take(4)?.toIntOrNull() ?: 0
            val trackNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                ?.substringBefore('/')?.toIntOrNull() ?: 0
            val bitrateBps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                ?.toIntOrNull() ?: 0
            val sampleRateHz = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull() ?: 44100
            } else 44100

            val bitrateKbps = if (bitrateBps > 0) bitrateBps / 1000
            else if (durationMs > 0 && fileSize > 0) ((fileSize * 8L) / durationMs).toInt() else 0

            val id = "saf_${abs(uri.toString().hashCode())}_${fileName.hashCode()}"
            val albumId = abs("${album.lowercase(Locale.ROOT)}|${albumArtist.lowercase(Locale.ROOT)}".hashCode()).toLong()

            TrackEntity(
                id = id,
                uri = uri.toString(),
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
                mimeType = mimeType.ifBlank { "audio/*" },
                formatLabel = detectFormatLabel(mimeType, fileName),
                bitrateKbps = bitrateKbps,
                sampleRateHz = sampleRateHz,
                channels = 2,
                fileSizeBytes = fileSize,
                artworkUri = null,
                isFavorite = favoriteIds.contains(id),
                dateAdded = System.currentTimeMillis(),
                sourceType = "saf"
            )
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun extractFolderInfo(relativePath: String, dataPath: String): Pair<String, String> {
        val cleanedRel = relativePath.trim().trimEnd('/')
        if (cleanedRel.isNotBlank()) {
            val name = cleanedRel.substringAfterLast('/').ifBlank { cleanedRel }
            return cleanedRel to name
        }
        if (dataPath.isNotBlank()) {
            val parent = File(dataPath).parent.orEmpty().trimEnd('/')
            val name = parent.substringAfterLast('/').ifBlank { "Música local" }
            val shortPath = parent.substringAfter("/0/", parent).ifBlank { name }
            return shortPath to name
        }
        return "Música local" to "Música local"
    }

    private fun detectFormatLabel(mimeType: String, fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").uppercase(Locale.ROOT)
        if (ext == "DSF" || ext == "DFF") return "DSD"
        if (ext in setOf("FLAC", "WAV", "ALAC", "AIFF", "AIF", "MP3", "M4A", "AAC", "OGG", "OPUS", "WMA")) {
            return if (ext == "AIF") "AIFF" else ext
        }
        val lowerMime = mimeType.lowercase(Locale.ROOT)
        return when {
            "flac" in lowerMime -> "FLAC"
            "wav" in lowerMime || "wave" in lowerMime -> "WAV"
            "alac" in lowerMime -> "ALAC"
            "opus" in lowerMime -> "OPUS"
            "ogg" in lowerMime || "vorbis" in lowerMime -> "OGG"
            "mp4" in lowerMime || "aac" in lowerMime || "m4a" in lowerMime -> "AAC"
            "mpeg" in lowerMime || "mp3" in lowerMime -> "MP3"
            else -> "AUDIO"
        }
    }

    private fun inferGenreFromFolder(folderName: String): String {
        val lower = folderName.lowercase(Locale.ROOT)
        return when {
            "salsa" in lower -> "Salsa"
            "tropical" in lower || "merengue" in lower || "bachata" in lower -> "Tropical"
            "clasica" in lower || "clásica" in lower || "classical" in lower || "mozart" in lower -> "Clásica"
            "jazz" in lower -> "Jazz"
            "rock" in lower -> "Rock"
            "electron" in lower || "house" in lower || "techno" in lower -> "Electrónica"
            "bolero" in lower -> "Bolero"
            "cumbia" in lower -> "Cumbia"
            "reggaeton" in lower || "urbano" in lower -> "Urbano"
            else -> ""
        }
    }
}
