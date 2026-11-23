package com.mozartmpx.app.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.OptIn
import androidx.compose.runtime.Immutable
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.mozartmpx.app.data.local.TasteAffinityEntity
import com.mozartmpx.app.data.model.RecommendedTrack
import com.mozartmpx.app.data.model.RepeatMode
import com.mozartmpx.app.data.model.Track
import com.mozartmpx.app.data.repository.MusicRepository
import com.mozartmpx.app.domain.SmartQueueEngine
import com.mozartmpx.app.ui.theme.AlbumPalette
import com.mozartmpx.app.ui.theme.ArtworkPaletteExtractor
import com.mozartmpx.app.ui.theme.DefaultAlbumPalette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Intentionally excludes rapidly changing positionMs so UI screens never recompose
 * every frame while music plays. Only scrubbers/progress bars observe positionMsFlow.
 */
@Immutable
data class PlaybackUiState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val crossfadeSeconds: Int = 3,
    val smartAutoplayEnabled: Boolean = true,
    val manualQueue: List<Track> = emptyList(),
    val upcomingSessionQueue: List<Track> = emptyList(),
    val smartQueue: List<RecommendedTrack> = emptyList(),
    val activeSessionLabel: String = "",
    val isCrossfading: Boolean = false
)

class MozartPlaybackEngine(
    private val context: Context,
    private val repository: MusicRepository,
    private val smartQueueEngine: SmartQueueEngine,
    private val audioFxController: AudioFxController,
    private val paletteExtractor: ArtworkPaletteExtractor
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

    private var primaryPlayer: ExoPlayer = createPlayer()
    private var secondaryPlayer: ExoPlayer? = null

    val mediaSessionPlayer: Player
        get() = primaryPlayer

    private val _playbackState = MutableStateFlow(PlaybackUiState())
    val playbackState: StateFlow<PlaybackUiState> = _playbackState.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _albumPalette = MutableStateFlow(DefaultAlbumPalette)
    val albumPalette: StateFlow<AlbumPalette> = _albumPalette.asStateFlow()

    @Volatile private var allLibraryTracks: List<Track> = emptyList()
    @Volatile private var tasteAffinities: Map<String, TasteAffinityEntity> = emptyMap()
    private val playHistoryStack = mutableListOf<Track>()
    private val recentSessionTrackIds = LinkedHashSet<String>()
    private val recentArtists = mutableListOf<String>()

    private var progressJob: Job? = null
    private var crossfadeJob: Job? = null
    private var paletteJob: Job? = null
    private var smartQueueJob: Job? = null
    private var enrichJob: Job? = null
    var onPrimaryPlayerChanged: ((Player) -> Unit)? = null
    var onPlaybackServiceStopped: (() -> Unit)? = null

    private var isCrossfadeInFlight = false
    private var gaplessPreloadedTrackId: String? = null
    private var foregroundServiceStarted = false

    private val primaryListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) {
                ensureForegroundServiceStarted()
                startProgressLoop()
            } else {
                progressJob?.cancel()
                progressJob = null
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                val dur = primaryPlayer.duration.takeIf { it > 0 } ?: _playbackState.value.currentTrack?.durationMs ?: 0L
                if (dur != _playbackState.value.durationMs) {
                    _playbackState.update { it.copy(durationMs = dur) }
                }
                attachAudioFxToPrimary()
                prepareGaplessNextIfNeeded()
            } else if (playbackState == Player.STATE_ENDED && !isCrossfadeInFlight) {
                onTrackFinishedNaturally()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO && !isCrossfadeInFlight) {
                val newId = mediaItem?.mediaId ?: return
                val currentId = _playbackState.value.currentTrack?.id
                if (newId != currentId) {
                    val previous = _playbackState.value.currentTrack
                    if (previous != null) {
                        recordCompletionAsync(previous, completed = true)
                    }
                    consumeNextMatchingTrack(newId)
                    if (primaryPlayer.currentMediaItemIndex > 0) {
                        primaryPlayer.removeMediaItems(0, primaryPlayer.currentMediaItemIndex)
                    }
                    gaplessPreloadedTrackId = null
                    prepareGaplessNextIfNeeded()
                }
            }
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            attachAudioFxToPrimary()
        }
    }

    init {
        primaryPlayer.addListener(primaryListener)

        scope.launch {
            repository.allTracks.collect { tracks ->
                allLibraryTracks = tracks
                val current = _playbackState.value.currentTrack
                if (current != null) {
                    val refreshed = tracks.firstOrNull { it.id == current.id }
                    if (refreshed != null && refreshed.isFavorite != current.isFavorite) {
                        _playbackState.update { it.copy(currentTrack = refreshed) }
                    }
                }
            }
        }

        scope.launch {
            repository.affinities.collect { map ->
                tasteAffinities = map
            }
        }

        scope.launch {
            repository.preferences.collect { prefs ->
                _playbackState.update {
                    it.copy(
                        crossfadeSeconds = prefs.crossfadeSeconds,
                        smartAutoplayEnabled = prefs.smartAutoplayEnabled,
                        shuffleEnabled = prefs.shuffleEnabled,
                        repeatMode = prefs.repeatMode
                    )
                }
                if (prefs.crossfadeSeconds == 0) {
                    prepareGaplessNextIfNeeded()
                } else {
                    clearGaplessLookahead()
                }
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun createPlayer(handleAudioFocus: Boolean = true): ExoPlayer {
        return ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, handleAudioFocus)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
    }

    private fun getOrCreateSecondaryPlayer(): ExoPlayer {
        return secondaryPlayer ?: createPlayer(handleAudioFocus = false).also { secondaryPlayer = it }
    }

    fun playTrackList(
        tracks: List<Track>,
        startIndex: Int = 0,
        sessionLabel: String = "",
        shuffle: Boolean? = null
    ) {
        if (tracks.isEmpty()) return
        cancelCrossfade()

        val effectiveShuffle = shuffle ?: _playbackState.value.shuffleEnabled
        val safeStart = startIndex.coerceIn(0, tracks.lastIndex)
        val firstTrack = tracks[safeStart]

        val remaining = if (effectiveShuffle) {
            tracks.filterIndexed { idx, _ -> idx != safeStart }.shuffled().take(120)
        } else {
            tracks.drop(safeStart + 1).take(120)
        }

        _playbackState.value.currentTrack?.let { prev ->
            if (prev.id != firstTrack.id) {
                playHistoryStack.add(prev)
                if (playHistoryStack.size > 40) playHistoryStack.removeAt(0)
                val progressRatio = if (prev.durationMs > 0) _positionMs.value.toFloat() / prev.durationMs else 0f
                recordCompletionAsync(prev, completed = progressRatio >= 0.65f)
            }
        }

        if (shuffle != null && shuffle != _playbackState.value.shuffleEnabled) {
            scope.launch(Dispatchers.IO) { repository.setShuffleEnabled(shuffle) }
        }

        _playbackState.update {
            it.copy(
                upcomingSessionQueue = remaining,
                activeSessionLabel = sessionLabel.ifBlank { firstTrack.artist },
                shuffleEnabled = effectiveShuffle
            )
        }
        startTrackOnPrimary(firstTrack)
    }

    fun playTrackNow(track: Track) {
        val currentQueue = _playbackState.value.upcomingSessionQueue.filterNot { it.id == track.id }
        playTrackList(
            tracks = listOf(track) + currentQueue,
            startIndex = 0,
            sessionLabel = track.artist
        )
    }

    fun togglePlayPause() {
        val current = _playbackState.value.currentTrack
        if (current == null) {
            if (allLibraryTracks.isNotEmpty()) {
                playTrackList(allLibraryTracks, 0, "Biblioteca local")
            }
            return
        }
        if (primaryPlayer.isPlaying) {
            primaryPlayer.pause()
        } else {
            if (primaryPlayer.playbackState == Player.STATE_IDLE) {
                startTrackOnPrimary(current)
            } else {
                primaryPlayer.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val dur = _playbackState.value.durationMs.coerceAtLeast(1L)
        val clamped = positionMs.coerceIn(0L, dur)
        primaryPlayer.seekTo(clamped)
        _positionMs.value = clamped
    }

    fun skipToNext(isManualSkip: Boolean = true) {
        cancelCrossfade()
        val current = _playbackState.value.currentTrack
        if (current != null && isManualSkip) {
            val ratio = if (current.durationMs > 0) _positionMs.value.toFloat() / current.durationMs else 0f
            recordCompletionAsync(current, completed = ratio >= 0.75f)
        }

        val nextTrack = popNextTrack(ignoreRepeatOne = isManualSkip)
        if (nextTrack != null) {
            if (current != null && current.id != nextTrack.id) {
                playHistoryStack.add(current)
                if (playHistoryStack.size > 40) playHistoryStack.removeAt(0)
            }
            startTrackOnPrimary(nextTrack)
        } else {
            primaryPlayer.pause()
            primaryPlayer.seekTo(0)
            _positionMs.value = 0L
            _playbackState.update { it.copy(isPlaying = false) }
        }
    }

    fun skipToPrevious() {
        cancelCrossfade()
        if (_positionMs.value > 3500L) {
            seekTo(0L)
            return
        }
        val prev = playHistoryStack.removeLastOrNull()
        if (prev != null) {
            _playbackState.value.currentTrack?.let { curr ->
                _playbackState.update {
                    it.copy(upcomingSessionQueue = listOf(curr) + it.upcomingSessionQueue)
                }
            }
            startTrackOnPrimary(prev)
        } else {
            seekTo(0L)
        }
    }

    fun onTrackRemovedOrHidden(trackId: String) {
        playHistoryStack.removeAll { it.id == trackId }
        _playbackState.update { state ->
            state.copy(
                manualQueue = state.manualQueue.filterNot { it.id == trackId },
                upcomingSessionQueue = state.upcomingSessionQueue.filterNot { it.id == trackId },
                smartQueue = state.smartQueue.filterNot { it.track.id == trackId }
            )
        }
        if (_playbackState.value.currentTrack?.id == trackId) {
            val next = popNextTrack(ignoreRepeatOne = true)
            if (next != null) {
                startTrackOnPrimary(next)
            } else {
                primaryPlayer.stop()
                primaryPlayer.clearMediaItems()
                _positionMs.value = 0L
                _playbackState.update { it.copy(currentTrack = null, isPlaying = false, durationMs = 0L) }
                _albumPalette.value = DefaultAlbumPalette
            }
        }
    }

    fun playNextInQueue(track: Track) {
        _playbackState.update { state ->
            val updated = listOf(track) + state.manualQueue.filterNot { it.id == track.id }
            state.copy(manualQueue = updated)
        }
        prepareGaplessNextIfNeeded(forceRefresh = true)
    }

    fun addToQueue(track: Track) {
        _playbackState.update { state ->
            if (state.manualQueue.any { it.id == track.id }) state
            else state.copy(manualQueue = state.manualQueue + track)
        }
        prepareGaplessNextIfNeeded(forceRefresh = true)
    }

    fun removeFromManualQueue(trackId: String) {
        _playbackState.update { state ->
            state.copy(manualQueue = state.manualQueue.filterNot { it.id == trackId })
        }
        prepareGaplessNextIfNeeded(forceRefresh = true)
    }

    fun removeFromSessionQueue(trackId: String) {
        _playbackState.update { state ->
            state.copy(upcomingSessionQueue = state.upcomingSessionQueue.filterNot { it.id == trackId })
        }
        prepareGaplessNextIfNeeded(forceRefresh = true)
    }

    fun clearManualQueue() {
        _playbackState.update { it.copy(manualQueue = emptyList()) }
        prepareGaplessNextIfNeeded(forceRefresh = true)
    }

    fun toggleShuffle() {
        val nextShuffle = !_playbackState.value.shuffleEnabled
        _playbackState.update { state ->
            val reordered = if (nextShuffle) state.upcomingSessionQueue.shuffled() else state.upcomingSessionQueue
            state.copy(shuffleEnabled = nextShuffle, upcomingSessionQueue = reordered)
        }
        scope.launch(Dispatchers.IO) { repository.setShuffleEnabled(nextShuffle) }
        prepareGaplessNextIfNeeded(forceRefresh = true)
    }

    fun cycleRepeatMode() {
        val next = _playbackState.value.repeatMode.next()
        _playbackState.update { it.copy(repeatMode = next) }
        scope.launch(Dispatchers.IO) { repository.setRepeatMode(next) }
        prepareGaplessNextIfNeeded(forceRefresh = true)
    }

    fun setCrossfadeSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(0, 10)
        _playbackState.update { it.copy(crossfadeSeconds = clamped) }
        scope.launch(Dispatchers.IO) { repository.setCrossfadeSeconds(clamped) }
    }

    fun setSmartAutoplayEnabled(enabled: Boolean) {
        _playbackState.update { it.copy(smartAutoplayEnabled = enabled) }
        scope.launch(Dispatchers.IO) { repository.setSmartAutoplayEnabled(enabled) }
        val current = _playbackState.value.currentTrack
        if (enabled && current != null) {
            refreshSmartQueueAsync(current)
        }
    }

    private fun startTrackOnPrimary(track: Track) {
        trackHistoryVisited(track)
        gaplessPreloadedTrackId = null
        primaryPlayer.volume = 1f
        primaryPlayer.setMediaItem(buildMediaItem(track))
        primaryPlayer.prepare()
        primaryPlayer.play()

        _positionMs.value = 0L
        _playbackState.update {
            it.copy(
                currentTrack = track,
                durationMs = track.durationMs,
                isPlaying = true,
                isCrossfading = false
            )
        }

        // All heavy operations run asynchronously off the UI thread!
        refreshSmartQueueAsync(track)
        extractPaletteAsync(track)
        enrichActiveTrackAsync(track)
        ensureForegroundServiceStarted()
    }

    private fun onTrackFinishedNaturally() {
        val current = _playbackState.value.currentTrack
        if (current != null) {
            recordCompletionAsync(current, completed = true)
        }
        val nextTrack = popNextTrack(ignoreRepeatOne = false)
        if (nextTrack != null) {
            if (current != null && current.id != nextTrack.id) {
                playHistoryStack.add(current)
            }
            startTrackOnPrimary(nextTrack)
        } else {
            _positionMs.value = 0L
            _playbackState.update { it.copy(isPlaying = false) }
        }
    }

    private fun peekNextTrack(ignoreRepeatOne: Boolean = false): Track? {
        val state = _playbackState.value
        val current = state.currentTrack ?: return null
        if (!ignoreRepeatOne && state.repeatMode == RepeatMode.ONE) return current
        if (state.manualQueue.isNotEmpty()) return state.manualQueue.first()
        if (state.upcomingSessionQueue.isNotEmpty()) return state.upcomingSessionQueue.first()
        if (state.smartAutoplayEnabled && state.smartQueue.isNotEmpty()) return state.smartQueue.first().track
        if (state.repeatMode == RepeatMode.ALL && allLibraryTracks.isNotEmpty()) {
            return allLibraryTracks.firstOrNull { it.id != current.id } ?: current
        }
        return null
    }

    private fun popNextTrack(ignoreRepeatOne: Boolean = false): Track? {
        val state = _playbackState.value
        val current = state.currentTrack ?: return null
        if (!ignoreRepeatOne && state.repeatMode == RepeatMode.ONE) return current

        if (state.manualQueue.isNotEmpty()) {
            val next = state.manualQueue.first()
            _playbackState.update { it.copy(manualQueue = it.manualQueue.drop(1)) }
            return next
        }
        if (state.upcomingSessionQueue.isNotEmpty()) {
            val next = state.upcomingSessionQueue.first()
            _playbackState.update { it.copy(upcomingSessionQueue = it.upcomingSessionQueue.drop(1)) }
            return next
        }
        if (state.smartAutoplayEnabled && state.smartQueue.isNotEmpty()) {
            val nextRec = state.smartQueue.first()
            _playbackState.update {
                it.copy(
                    smartQueue = it.smartQueue.drop(1),
                    activeSessionLabel = nextRec.reason
                )
            }
            return nextRec.track
        }
        if (state.repeatMode == RepeatMode.ALL && allLibraryTracks.isNotEmpty()) {
            val pool = if (state.shuffleEnabled) allLibraryTracks.shuffled().take(120) else allLibraryTracks.take(120)
            val next = pool.firstOrNull { it.id != current.id } ?: current
            _playbackState.update { it.copy(upcomingSessionQueue = pool.filterNot { t -> t.id == next.id }) }
            return next
        }
        return null
    }

    private fun consumeNextMatchingTrack(trackId: String) {
        val state = _playbackState.value
        val matchedTrack: Track? = when {
            state.manualQueue.firstOrNull()?.id == trackId -> {
                val t = state.manualQueue.first()
                _playbackState.update { it.copy(manualQueue = it.manualQueue.drop(1)) }
                t
            }
            state.upcomingSessionQueue.firstOrNull()?.id == trackId -> {
                val t = state.upcomingSessionQueue.first()
                _playbackState.update { it.copy(upcomingSessionQueue = it.upcomingSessionQueue.drop(1)) }
                t
            }
            state.smartQueue.firstOrNull()?.track?.id == trackId -> {
                val rec = state.smartQueue.first()
                _playbackState.update {
                    it.copy(
                        smartQueue = it.smartQueue.drop(1),
                        activeSessionLabel = rec.reason
                    )
                }
                rec.track
            }
            else -> allLibraryTracks.firstOrNull { it.id == trackId }
        }

        if (matchedTrack != null) {
            state.currentTrack?.let { prev ->
                if (prev.id != matchedTrack.id) playHistoryStack.add(prev)
            }
            trackHistoryVisited(matchedTrack)
            _positionMs.value = 0L
            _playbackState.update {
                it.copy(
                    currentTrack = matchedTrack,
                    durationMs = matchedTrack.durationMs
                )
            }
            refreshSmartQueueAsync(matchedTrack)
            extractPaletteAsync(matchedTrack)
            enrichActiveTrackAsync(matchedTrack)
        }
    }

    private fun prepareGaplessNextIfNeeded(forceRefresh: Boolean = false) {
        val state = _playbackState.value
        if (state.crossfadeSeconds > 0) {
            clearGaplessLookahead()
            return
        }
        val nextTrack = peekNextTrack(ignoreRepeatOne = false) ?: return
        if (!forceRefresh && gaplessPreloadedTrackId == nextTrack.id && primaryPlayer.mediaItemCount > 1) return

        clearGaplessLookahead()
        primaryPlayer.addMediaItem(buildMediaItem(nextTrack))
        gaplessPreloadedTrackId = nextTrack.id
    }

    private fun clearGaplessLookahead() {
        val currentIdx = primaryPlayer.currentMediaItemIndex
        if (currentIdx >= 0 && primaryPlayer.mediaItemCount > currentIdx + 1) {
            primaryPlayer.removeMediaItems(currentIdx + 1, primaryPlayer.mediaItemCount)
        }
        gaplessPreloadedTrackId = null
    }

    private fun startProgressLoop() {
        if (progressJob?.isActive == true) return
        progressJob = scope.launch {
            while (primaryPlayer.isPlaying) {
                val pos = primaryPlayer.currentPosition.coerceAtLeast(0L)
                val dur = primaryPlayer.duration.takeIf { it > 0 } ?: _playbackState.value.durationMs
                _positionMs.value = pos
                if (dur > 0 && dur != _playbackState.value.durationMs) {
                    _playbackState.update { it.copy(durationMs = dur) }
                }

                val fadeSec = _playbackState.value.crossfadeSeconds
                if (fadeSec > 0 && !isCrossfadeInFlight && dur > (fadeSec * 1000L + 4000L)) {
                    val remainingMs = dur - pos
                    if (remainingMs in 400L..(fadeSec * 1000L)) {
                        val nextTrack = peekNextTrack(ignoreRepeatOne = false)
                        if (nextTrack != null) {
                            launchCrossfadeTo(nextTrack, remainingMs.coerceAtMost(fadeSec * 1000L))
                        }
                    }
                }
                delay(350L)
            }
        }
    }

    private fun launchCrossfadeTo(nextTrack: Track, fadeDurationMs: Long) {
        isCrossfadeInFlight = true
        val outgoingTrack = _playbackState.value.currentTrack
        _playbackState.update { it.copy(isCrossfading = true) }

        val outgoing = primaryPlayer
        val incoming = getOrCreateSecondaryPlayer()

        incoming.stop()
        incoming.clearMediaItems()
        incoming.volume = 0f
        incoming.setMediaItem(buildMediaItem(nextTrack))
        incoming.prepare()
        incoming.play()

        crossfadeJob?.cancel()
        crossfadeJob = scope.launch {
            val steps = 24
            val stepDelay = (fadeDurationMs / steps).coerceAtLeast(45L)
            for (step in 1..steps) {
                val t = step.toFloat() / steps.toFloat()
                val outVol = cos(t * (PI / 2.0)).toFloat().coerceIn(0f, 1f)
                val inVol = sin(t * (PI / 2.0)).toFloat().coerceIn(0f, 1f)
                outgoing.volume = outVol
                incoming.volume = inVol
                delay(stepDelay)
            }

            outgoing.removeListener(primaryListener)
            outgoing.pause()
            outgoing.stop()
            outgoing.clearMediaItems()
            outgoing.volume = 1f

            primaryPlayer = incoming
            secondaryPlayer = outgoing
            primaryPlayer.volume = 1f
            primaryPlayer.addListener(primaryListener)
            onPrimaryPlayerChanged?.invoke(primaryPlayer)

            if (outgoingTrack != null) {
                recordCompletionAsync(outgoingTrack, completed = true)
                if (outgoingTrack.id != nextTrack.id) {
                    playHistoryStack.add(outgoingTrack)
                }
            }
            popNextTrack(ignoreRepeatOne = false)
            trackHistoryVisited(nextTrack)

            _positionMs.value = primaryPlayer.currentPosition.coerceAtLeast(0L)
            _playbackState.update {
                it.copy(
                    currentTrack = nextTrack,
                    durationMs = nextTrack.durationMs,
                    isPlaying = primaryPlayer.isPlaying,
                    isCrossfading = false
                )
            }
            refreshSmartQueueAsync(nextTrack)
            extractPaletteAsync(nextTrack)
            enrichActiveTrackAsync(nextTrack)
            attachAudioFxToPrimary()
            isCrossfadeInFlight = false
            startProgressLoop()
        }
    }

    private fun cancelCrossfade() {
        if (!isCrossfadeInFlight) return
        crossfadeJob?.cancel()
        crossfadeJob = null
        secondaryPlayer?.let { sec ->
            sec.stop()
            sec.clearMediaItems()
            sec.volume = 1f
        }
        primaryPlayer.volume = 1f
        isCrossfadeInFlight = false
        _playbackState.update { it.copy(isCrossfading = false) }
    }

    /**
     * Computes Smart Queue recommendations strictly on Dispatchers.Default so
     * even libraries with 3,000+ tracks never touch or block the UI thread.
     */
    private fun refreshSmartQueueAsync(seedTrack: Track) {
        smartQueueJob?.cancel()
        val snapshotLibrary = allLibraryTracks
        val snapshotAffinities = tasteAffinities
        val state = _playbackState.value
        val excluded = buildSet {
            add(seedTrack.id)
            addAll(state.manualQueue.map { it.id })
            addAll(state.upcomingSessionQueue.take(30).map { it.id })
            addAll(recentSessionTrackIds.toList().takeLast(8))
        }
        val recentArtistSnapshot = recentArtists.toList()

        smartQueueJob = scope.launch {
            val recs = withContext(Dispatchers.Default) {
                var r = smartQueueEngine.recommendNextTracks(
                    seedTrack = seedTrack,
                    library = snapshotLibrary,
                    affinities = snapshotAffinities,
                    excludeTrackIds = excluded,
                    recentArtistNames = recentArtistSnapshot,
                    limit = 15
                )
                if (r.isEmpty() && snapshotLibrary.size > 1) {
                    r = smartQueueEngine.recommendNextTracks(
                        seedTrack = seedTrack,
                        library = snapshotLibrary,
                        affinities = snapshotAffinities,
                        excludeTrackIds = setOf(seedTrack.id),
                        recentArtistNames = emptyList(),
                        limit = 12
                    )
                }
                r
            }
            if (_playbackState.value.currentTrack?.id == seedTrack.id) {
                _playbackState.update { it.copy(smartQueue = recs) }
                if (_playbackState.value.crossfadeSeconds == 0) {
                    prepareGaplessNextIfNeeded()
                }
            }
        }
    }

    private fun extractPaletteAsync(track: Track) {
        paletteJob?.cancel()
        paletteJob = scope.launch {
            val extracted = paletteExtractor.extractForTrack(track)
            if (_playbackState.value.currentTrack?.id == track.id) {
                _albumPalette.value = extracted
            }
        }
    }

    private fun enrichActiveTrackAsync(track: Track) {
        enrichJob?.cancel()
        enrichJob = scope.launch {
            val updated = repository.enrichTrackIfNeeded(track)
            if (updated != track && _playbackState.value.currentTrack?.id == track.id) {
                _playbackState.update { it.copy(currentTrack = updated) }
            }
        }
    }

    private fun trackHistoryVisited(track: Track) {
        recentSessionTrackIds.remove(track.id)
        recentSessionTrackIds.add(track.id)
        if (recentSessionTrackIds.size > 25) {
            val first = recentSessionTrackIds.firstOrNull()
            if (first != null) recentSessionTrackIds.remove(first)
        }
        recentArtists.add(track.artist)
        if (recentArtists.size > 8) recentArtists.removeAt(0)
    }

    private fun recordCompletionAsync(track: Track, completed: Boolean) {
        scope.launch(Dispatchers.IO) {
            val familyKey = smartQueueEngine.detectFamily(track).key
            repository.recordPlaybackInteraction(track, familyKey, completed)
        }
    }

    @OptIn(UnstableApi::class)
    private fun attachAudioFxToPrimary() {
        val sessionId = primaryPlayer.audioSessionId
        if (sessionId != C.AUDIO_SESSION_ID_UNSET && sessionId > 0) {
            audioFxController.attachToAudioSession(sessionId)
        }
    }

    private fun buildMediaItem(track: Track): MediaItem {
        val metaBuilder = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setGenre(track.genre)
        track.artworkUri?.let { art ->
            runCatching { metaBuilder.setArtworkUri(Uri.parse(art)) }
        }
        return MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(Uri.parse(track.uri))
            .setMediaMetadata(metaBuilder.build())
            .build()
    }

    private fun ensureForegroundServiceStarted() {
        if (foregroundServiceStarted) return
        runCatching {
            val intent = Intent(context, MozartPlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            foregroundServiceStarted = true
        }
    }

    fun notifyPlaybackServiceStopped() {
        foregroundServiceStarted = false
    }

    fun release() {
        progressJob?.cancel()
        crossfadeJob?.cancel()
        paletteJob?.cancel()
        smartQueueJob?.cancel()
        enrichJob?.cancel()
        audioFxController.releaseEffects()
        primaryPlayer.release()
        secondaryPlayer?.release()
        secondaryPlayer = null
    }
}
