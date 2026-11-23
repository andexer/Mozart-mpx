package com.mozartmpx.app.domain

import com.mozartmpx.app.data.local.TasteAffinityEntity
import com.mozartmpx.app.data.model.RecommendedTrack
import com.mozartmpx.app.data.model.SmartSession
import com.mozartmpx.app.data.model.Track
import java.text.Normalizer
import java.util.Locale

/**
 * 100% offline musical coherence and recommendation engine.
 *
 * Keeps listening sessions stylistically coherent without abrupt rhythm changes:
 * e.g., when playing Maelo Ruiz (salsa romántica/clásica), prioritizes classic &
 * contemporary salsa artists and smoothly related tropical rhythms, weighted by local
 * Room listening affinity (completed listens vs. skips).
 */
class SmartQueueEngine {

    enum class MusicFamily(
        val key: String,
        val displayName: String,
        val tagline: String,
        val relatedFamilies: Set<String>
    ) {
        SALSA(
            key = "salsa",
            displayName = "Salsa Clásica y Contemporánea",
            tagline = "Clave, metales y cadencia salsera continua",
            relatedFamilies = setOf("tropical", "bolero")
        ),
        TROPICAL(
            key = "tropical",
            displayName = "Ritmos Tropicales",
            tagline = "Merengue, bachata, son, timba y cumbia",
            relatedFamilies = setOf("salsa", "bolero")
        ),
        BOLERO(
            key = "bolero",
            displayName = "Bolero & Trova",
            tagline = "Armonías cálidas y sentimiento latino",
            relatedFamilies = setOf("salsa", "tropical", "acoustic")
        ),
        CLASSICAL(
            key = "classical",
            displayName = "Clásica & Orquestal",
            tagline = "Sinfonías, cámara y maestros clásicos",
            relatedFamilies = setOf("jazz", "acoustic")
        ),
        JAZZ(
            key = "jazz",
            displayName = "Jazz & Soul",
            tagline = "Improvisación, calidez analógica y groove",
            relatedFamilies = setOf("classical", "bolero", "acoustic")
        ),
        ROCK(
            key = "rock",
            displayName = "Rock & Alternativo",
            tagline = "Guitarras, energía y rock en español/inglés",
            relatedFamilies = setOf("pop", "acoustic")
        ),
        ELECTRONIC(
            key = "electronic",
            displayName = "Electrónica & Atmosférica",
            tagline = "House, ambient, synth y pulsos continuos",
            relatedFamilies = setOf("pop", "urban")
        ),
        URBAN(
            key = "urban",
            displayName = "Urbano & Hip-Hop",
            tagline = "Ritmo urbano, R&B y beats modernos",
            relatedFamilies = setOf("tropical", "electronic", "pop")
        ),
        ACOUSTIC(
            key = "acoustic",
            displayName = "Acústica & Cantautor",
            tagline = "Cuerdas íntimas, folk y balada orgánica",
            relatedFamilies = setOf("bolero", "jazz", "rock")
        ),
        POP(
            key = "pop",
            displayName = "Pop & Contemporáneo",
            tagline = "Melodías accesibles y producción moderna",
            relatedFamilies = setOf("rock", "urban", "electronic")
        ),
        GENERAL(
            key = "general",
            displayName = "Sesión Local",
            tagline = "Selección equilibrada de tu biblioteca",
            relatedFamilies = emptySet()
        )
    }

    // Curated offline artist-to-style knowledge so even poorly tagged local files maintain
    // accurate genre/subgenre coherence (especially classic & contemporary salsa/tropical).
    private val salsaArtists = setOf(
        "maelo ruiz", "frankie ruiz", "eddie santiago", "gilberto santa rosa",
        "willie gonzalez", "jerry rivera", "marc anthony", "hector lavoe",
        "ruben blades", "willie colon", "grupo niche", "oscar d leon", "oscar d'leon",
        "el gran combo", "gran combo", "joe arroyo", "victor manuelle",
        "luis enrique", "tito nieves", "rey ruiz", "tony vega", "tito rojas",
        "la india", "india", "celia cruz", "fania all stars", "fania",
        "sonora poncena", "richie ray", "bobby cruz", "ismael rivera",
        "cheo feliciano", "guayacan", "grupo gale", "adolescentes orquesta",
        "adolescentes", "nino segarra", "lalo rodriguez", "danny daniel",
        "david pabon", "pedro arroyo", "hildemaro", "tito puente",
        "roberto roena", "ray barretto", "dimensión latina", "dimension latina",
        "andy montanez", "puerto rican power", "fruko", "piper pimienta",
        "orquesta guayacan", "roberto blades", "hansel y raul", "mickey tavera",
        "jose alberto el canario", "el canario", "la sonora carruseles", "gale",
        "servando y florentino", "salserin", "dlq", "dark latin groove"
    )

    private val tropicalArtists = setOf(
        "juan luis guerra", "romeo santos", "aventura", "prince royce",
        "elvis crespo", "eddy herrera", "sergio vargas", "wilfrido vargas",
        "carlos vives", "fonseca", "silvestre dangond", "diomedes diaz",
        "binomiode oro", "binomio de oro", "los angeles azules", "celso pina",
        "buena vista social club", "compay segundo", "ibrahim ferrer",
        "los van van", "alexander abreu", "havana d primera", "manolito simonet",
        "antony santos", "frank reyes", "hector acosta", "el torito",
        "olga tanon", "merenglass", "proyecto uno", "ilegal", "ilegales",
        "raulin rodriguez", "zacarias ferreira", "kinito mendez", "milly quezada"
    )

    private val classicalArtists = setOf(
        "mozart", "wolfgang amadeus mozart", "beethoven", "ludwig van beethoven",
        "bach", "johann sebastian bach", "chopin", "vivaldi", "debussy",
        "tchaikovsky", "brahms", "schubert", "handel", "haydn", "rachmaninoff",
        "mahler", "liszt", "verdi", "puccini", "paganini", "satie", "ravel",
        "ludovico einaudi", "max richter", "hans zimmer", "ennio morricone"
    )

    private val jazzArtists = setOf(
        "miles davis", "john coltrane", "bill evans", "chet baker", "nina simone",
        "ella fitzgerald", "louis armstrong", "dave brubeck", "herbie hancock",
        "thelonious monk", "stan getz", "norah jones", "amy winehouse",
        "robert glasper", "kamasi washington", "frank sinatra", "nat king cole"
    )

    private val rockArtists = setOf(
        "soda stereo", "gustavo cerati", "heroes del silencio", "caifanes",
        "mana", "enanitos verdes", "cafe tacvba", "zoé", "zoe", "fito paez",
        "charly garcia", "pink floyd", "queen", "led zeppelin", "the beatles",
        "beatles", "nirvana", "radiohead", "coldplay", "arctic monkeys",
        "metallica", "ac dc", "guns n roses", "u2", "red hot chili peppers",
        "the rolling stones", "david bowie", "the strokes"
    )

    private val electronicArtists = setOf(
        "daft punk", "disclosure", "calvin harris", "avicii", "deadmau5",
        "tiesto", "armin van buuren", "flume", "odesza", "fred again",
        "rufus du sol", "bonobo", "tycho", "four tet", "aphex twin", "bicep"
    )

    private val urbanArtists = setOf(
        "bad bunny", "feid", "karol g", "daddy yankee", "don omar", "j balvin",
        "rauw alejandro", "ozuna", "wisin y yandel", "tego calderon", "residente",
        "calle 13", "kendrick lamar", "drake", "the weeknd", "frank ocean",
        "sza", "travis scott", "kanye west", "tyler the creator", "j cole"
    )

    fun detectFamily(track: Track): MusicFamily {
        val genreNorm = normalize(track.genre)
        val artistNorm = normalize(track.artist)
        val albumNorm = normalize(track.album)
        val folderNorm = normalize(track.folderName)
        val combined = "$genreNorm $folderNorm $albumNorm"

        // 1. Direct artist stylistic graph (handles missing/messy tags!)
        if (salsaArtists.any { artistNorm.contains(it) }) return MusicFamily.SALSA
        if (tropicalArtists.any { artistNorm.contains(it) }) return MusicFamily.TROPICAL
        if (classicalArtists.any { artistNorm.contains(it) }) return MusicFamily.CLASSICAL
        if (jazzArtists.any { artistNorm.contains(it) }) return MusicFamily.JAZZ
        if (rockArtists.any { artistNorm.contains(it) }) return MusicFamily.ROCK
        if (electronicArtists.any { artistNorm.contains(it) }) return MusicFamily.ELECTRONIC
        if (urbanArtists.any { artistNorm.contains(it) }) return MusicFamily.URBAN

        // 2. Genre / folder / album keywords
        return when {
            containsAny(combined, "salsa", "guaguanco", "descarga", "fania", "salsera", "salsero", "boogaloo") ->
                MusicFamily.SALSA
            containsAny(combined, "tropical", "merengue", "bachata", "cumbia", "vallenato", "timba", "son cubano", "chacha", "mambo", "caribe") ->
                MusicFamily.TROPICAL
            containsAny(combined, "bolero", "trova", "balada", "romantica", "trio") ->
                MusicFamily.BOLERO
            containsAny(combined, "clasica", "classical", "sinfon", "symphon", "orquest", "orchestr", "baroque", "opera", "piano", "sonata", "concerto", "mozart") ->
                MusicFamily.CLASSICAL
            containsAny(combined, "jazz", "blues", "soul", "bossa", "swing", "funk", "lounge") ->
                MusicFamily.JAZZ
            containsAny(combined, "rock", "metal", "punk", "grunge", "indie", "alternat") ->
                MusicFamily.ROCK
            containsAny(combined, "electron", "house", "techno", "trance", "edm", "ambient", "synth", "chill", "lofi", "lo-fi", "dubstep") ->
                MusicFamily.ELECTRONIC
            containsAny(combined, "reggaeton", "urbano", "hip hop", "hip-hop", "rap", "trap", "r&b", "rnb", "dembow") ->
                MusicFamily.URBAN
            containsAny(combined, "acust", "acoust", "folk", "unplugged", "guitarra", "cantautor", "country") ->
                MusicFamily.ACOUSTIC
            containsAny(combined, "pop", "dance", "latino", "latin") ->
                MusicFamily.POP
            else -> MusicFamily.GENERAL
        }
    }

    fun recommendNextTracks(
        seedTrack: Track,
        library: List<Track>,
        affinities: Map<String, TasteAffinityEntity>,
        excludeTrackIds: Set<String>,
        recentArtistNames: List<String>,
        limit: Int = 15
    ): List<RecommendedTrack> {
        if (library.isEmpty()) return emptyList()

        val seedFamily = detectFamily(seedTrack)
        val seedArtistNorm = normalize(seedTrack.artist)
        val seedGenreNorm = normalize(seedTrack.genre)
        val recentArtistsNorm = recentArtistNames.takeLast(3).map { normalize(it) }

        val candidates = library.filter { it.id != seedTrack.id && it.id !in excludeTrackIds }
        if (candidates.isEmpty()) return emptyList()

        val scored = candidates.map { candidate ->
            val candFamily = detectFamily(candidate)
            val candArtistNorm = normalize(candidate.artist)
            val candGenreNorm = normalize(candidate.genre)

            var score = 0.0
            var reason = "Continuidad inteligente"

            // 1. Musical family & genre coherence (primary driver)
            when {
                seedFamily != MusicFamily.GENERAL && candFamily == seedFamily -> {
                    score += 55.0
                    reason = when (seedFamily) {
                        MusicFamily.SALSA -> "Salsa clásica y contemporánea · Afín a ${seedTrack.artist}"
                        MusicFamily.TROPICAL -> "Ritmos tropicales · Mismo ambiente"
                        MusicFamily.CLASSICAL -> "Clásica · Flujo sinfónico continuo"
                        else -> "${candFamily.displayName} · Ambiente coherente"
                    }
                }
                seedFamily != MusicFamily.GENERAL && candFamily.key in seedFamily.relatedFamilies -> {
                    score += 34.0
                    reason = when {
                        seedFamily == MusicFamily.SALSA && candFamily == MusicFamily.TROPICAL ->
                            "Ritmos tropicales afines a tu sesión de salsa"
                        else -> "Transición armónica hacia ${candFamily.displayName}"
                    }
                }
                seedGenreNorm.isNotBlank() && candGenreNorm == seedGenreNorm -> {
                    score += 48.0
                    reason = "Mismo género · ${candidate.genre}"
                }
                seedFamily != MusicFamily.GENERAL && candFamily != MusicFamily.GENERAL -> {
                    // Penalize jarring style jumps (e.g. salsa -> heavy rock or EDM)
                    score -= 22.0
                }
            }

            // 2. Artist affinity with smart repetition cooldown so sessions stay varied yet coherent
            if (candArtistNorm == seedArtistNorm && seedArtistNorm != "artista desconocido") {
                if (recentArtistsNorm.count { it == candArtistNorm } >= 2) {
                    score += 6.0
                } else {
                    score += 24.0
                    reason = "Más de ${candidate.artist}"
                }
            } else if (candArtistNorm in recentArtistsNorm) {
                score -= 10.0
            }

            // 3. Same local folder or album nuance (subtle continuity cue)
            if (candidate.folderPath == seedTrack.folderPath) {
                score += 10.0
                if (reason == "Continuidad inteligente") {
                    reason = "De tu carpeta ${candidate.folderName}"
                }
            }

            // 4. Era proximity (keeps classic eras flowing naturally into contemporary ones)
            if (seedTrack.year > 1950 && candidate.year > 1950) {
                val gap = kotlin.math.abs(seedTrack.year - candidate.year)
                if (gap <= 12) score += 7.0 else if (gap <= 25) score += 3.0
            }

            // 5. Local user taste affinity from Room (completed listens vs early skips)
            val artistAff = affinities["artist:$candArtistNorm"]
            val trackAff = affinities["track:${candidate.id}"]
            val familyAff = affinities["family:${candFamily.key}"]

            if (artistAff != null) {
                score += (artistAff.completedCount * 3.5) - (artistAff.skipCount * 4.5)
            }
            if (trackAff != null) {
                score += (trackAff.completedCount * 2.0) - (trackAff.skipCount * 6.0)
            }
            if (familyAff != null) {
                score += (familyAff.completedCount * 1.5) - (familyAff.skipCount * 2.0)
            }
            if (candidate.isFavorite) {
                score += 9.0
            }
            if (candidate.isHiFi && seedTrack.isHiFi) {
                score += 4.0
            }

            // Subtle deterministic jitter per session seed so repeated sessions stay fresh
            val freshTieBreaker = ((candidate.id.hashCode() xor seedTrack.id.hashCode()) and 0xFF) / 255.0 * 3.5
            RecommendedTrack(
                track = candidate,
                reason = reason,
                score = score + freshTieBreaker
            )
        }

        // Greedy diversity ordering: avoid putting the same artist twice in a row when alternatives exist
        val pool = scored.sortedByDescending { it.score }.toMutableList()
        val ordered = mutableListOf<RecommendedTrack>()
        var lastArtist = seedArtistNorm

        while (ordered.size < limit && pool.isNotEmpty()) {
            val idx = pool.indexOfFirst { normalize(it.track.artist) != lastArtist }
                .takeIf { it >= 0 && pool[it].score >= pool[0].score - 14.0 }
                ?: 0
            val chosen = pool.removeAt(idx)
            ordered += chosen
            lastArtist = normalize(chosen.track.artist)
        }
        return ordered
    }

    fun buildSmartSessions(tracks: List<Track>): List<SmartSession> {
        if (tracks.isEmpty()) return emptyList()
        val sessions = mutableListOf<SmartSession>()

        // Group real library tracks by detected musical family
        val grouped = tracks.groupBy { detectFamily(it) }
            .filterKeys { it != MusicFamily.GENERAL }
            .entries
            .sortedByDescending { it.value.size }

        for ((family, familyTracks) in grouped.take(5)) {
            if (familyTracks.isEmpty()) continue
            sessions += SmartSession(
                id = "family_${family.key}",
                title = family.displayName,
                subtitle = "${familyTracks.size} pistas · ${family.tagline}",
                familyKey = family.key,
                tracks = familyTracks,
                coverTrack = familyTracks.firstOrNull { it.artworkUri != null } ?: familyTracks.first()
            )
        }

        // Hi-Fi lossless session if any lossless/high-bitrate tracks exist
        val hiFiTracks = tracks.filter { it.isHiFi }
        if (hiFiTracks.isNotEmpty()) {
            sessions += SmartSession(
                id = "session_hifi",
                title = "Sesión Audiófila Hi-Fi",
                subtitle = "${hiFiTracks.size} pistas en alta fidelidad local",
                familyKey = "hifi",
                tracks = hiFiTracks,
                coverTrack = hiFiTracks.firstOrNull { it.artworkUri != null } ?: hiFiTracks.first()
            )
        }

        // Fallback full local flow session if library has tracks without identifiable genres
        if (sessions.isEmpty()) {
            sessions += SmartSession(
                id = "session_all",
                title = "Flujo Continuo Local",
                subtitle = "${tracks.size} pistas listas para sonar sin pausas",
                familyKey = "general",
                tracks = tracks,
                coverTrack = tracks.firstOrNull { it.artworkUri != null } ?: tracks.first()
            )
        }

        return sessions
    }

    private fun normalize(text: String): String {
        val decomposed = Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        return decomposed.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "").trim()
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean =
        keywords.any { text.contains(it) }
}
