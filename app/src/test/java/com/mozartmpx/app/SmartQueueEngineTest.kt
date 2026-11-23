package com.mozartmpx.app

import com.mozartmpx.app.data.model.Track
import com.mozartmpx.app.domain.SmartQueueEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartQueueEngineTest {

    private val engine = SmartQueueEngine()

    private fun track(
        id: String,
        title: String,
        artist: String,
        genre: String = "",
        album: String = "Album",
        folder: String = "Music"
    ) = Track(
        id = id,
        uri = "content://audio/$id",
        title = title,
        artist = artist,
        albumArtist = artist,
        album = album,
        albumId = id.hashCode().toLong(),
        genre = genre,
        year = 2004,
        trackNumber = 1,
        durationMs = 240_000L,
        folderPath = folder,
        folderName = folder,
        mimeType = "audio/flac",
        formatLabel = "FLAC",
        bitrateKbps = 980,
        sampleRateHz = 44100,
        channels = 2,
        fileSizeBytes = 28_000_000L,
        artworkUri = null,
        isFavorite = false,
        dateAdded = 1000L
    )

    @Test
    fun maeloRuizPrioritizesClassicAndContemporarySalsaOverRockAndClassical() {
        val seed = track("1", "Te Va a Doler", "Maelo Ruiz", genre = "")
        val frankie = track("2", "Deseándote", "Frankie Ruiz", genre = "Salsa")
        val gilberto = track("3", "Conteo Regresivo", "Gilberto Santa Rosa", genre = "")
        val juanLuis = track("4", "Burbujas de Amor", "Juan Luis Guerra", genre = "Tropical")
        val rock = track("5", "De Música Ligera", "Soda Stereo", genre = "Rock")
        val mozart = track("6", "Symphony No. 40", "Wolfgang Amadeus Mozart", genre = "Clásica")

        val recs = engine.recommendNextTracks(
            seedTrack = seed,
            library = listOf(seed, rock, mozart, frankie, juanLuis, gilberto),
            affinities = emptyMap(),
            excludeTrackIds = setOf(seed.id),
            recentArtistNames = listOf(seed.artist),
            limit = 5
        )

        assertEquals(5, recs.size)
        val topTwoArtists = recs.take(2).map { it.track.artist }.toSet()
        assertTrue(topTwoArtists.contains("Frankie Ruiz"))
        assertTrue(topTwoArtists.contains("Gilberto Santa Rosa"))
        assertEquals("Juan Luis Guerra", recs[2].track.artist)
    }
}
