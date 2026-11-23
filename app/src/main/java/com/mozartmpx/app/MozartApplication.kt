package com.mozartmpx.app

import android.app.Application
import com.mozartmpx.app.data.local.MozartDatabase
import com.mozartmpx.app.data.local.PreferencesRepository
import com.mozartmpx.app.data.repository.MusicRepository
import com.mozartmpx.app.data.scanner.LocalAudioScanner
import com.mozartmpx.app.domain.SmartQueueEngine
import com.mozartmpx.app.player.AudioFxController
import com.mozartmpx.app.player.MozartPlaybackEngine
import com.mozartmpx.app.ui.theme.ArtworkPaletteExtractor

class MozartContainer(application: Application) {
    val database: MozartDatabase by lazy { MozartDatabase.getInstance(application) }
    val preferencesRepository: PreferencesRepository by lazy { PreferencesRepository(application) }
    val localAudioScanner: LocalAudioScanner by lazy { LocalAudioScanner(application) }
    val musicRepository: MusicRepository by lazy {
        MusicRepository(
            database = database,
            scanner = localAudioScanner,
            preferencesRepository = preferencesRepository
        )
    }
    val smartQueueEngine: SmartQueueEngine by lazy { SmartQueueEngine() }
    val audioFxController: AudioFxController by lazy { AudioFxController(preferencesRepository) }
    val paletteExtractor: ArtworkPaletteExtractor by lazy { ArtworkPaletteExtractor(application) }
    val playbackEngine: MozartPlaybackEngine by lazy {
        MozartPlaybackEngine(
            context = application,
            repository = musicRepository,
            smartQueueEngine = smartQueueEngine,
            audioFxController = audioFxController,
            paletteExtractor = paletteExtractor
        )
    }
}

class MozartApplication : Application() {
    lateinit var container: MozartContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = MozartContainer(this)
    }
}
