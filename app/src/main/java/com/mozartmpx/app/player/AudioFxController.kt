package com.mozartmpx.app.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import com.mozartmpx.app.data.local.PreferencesRepository
import com.mozartmpx.app.data.model.AudioFxState
import com.mozartmpx.app.data.model.EqBandInfo
import com.mozartmpx.app.data.model.EqPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Controls Android hardware Equalizer, BassBoost, and Virtualizer bound to the active
 * Media3 ExoPlayer audioSessionId, with curated genre presets and graceful degradation.
 */
class AudioFxController(
    private val preferencesRepository: PreferencesRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var attachedSessionId: Int = 0

    private val defaultFrequencies = listOf(60, 230, 910, 3600, 14000)

    val builtInPresets: List<EqPreset> = listOf(
        EqPreset("flat", "Plano (Referencia)", listOf(0, 0, 0, 0, 0), bassBoost = 0, virtualizer = 0),
        EqPreset("salsa", "Salsa & Tropical", listOf(350, 180, 100, 320, 400), bassBoost = 380, virtualizer = 260),
        EqPreset("classical", "Clásica (Mozart)", listOf(120, 80, -40, 220, 360), bassBoost = 120, virtualizer = 320),
        EqPreset("acoustic", "Acústica", listOf(220, 140, 160, 240, 180), bassBoost = 180, virtualizer = 200),
        EqPreset("rock", "Rock", listOf(420, 200, -100, 280, 420), bassBoost = 450, virtualizer = 180),
        EqPreset("electronic", "Electrónica", listOf(500, 260, -60, 200, 440), bassBoost = 620, virtualizer = 350),
        EqPreset("vocals", "Voces & Bolero", listOf(-120, 160, 420, 260, 100), bassBoost = 100, virtualizer = 180),
        EqPreset("custom", "Personalizado", listOf(0, 0, 0, 0, 0), bassBoost = 0, virtualizer = 0, isBuiltIn = false)
    )

    private val _state = MutableStateFlow(
        AudioFxState(
            enabled = false,
            hardwareSupported = true,
            bands = defaultFrequencies.mapIndexed { idx, hz ->
                EqBandInfo(index = idx, centerFreqHz = hz, levelMb = 0, minLevelMb = -1500, maxLevelMb = 1500)
            },
            presets = builtInPresets,
            activePresetId = "flat",
            bassBoostStrength = 0,
            virtualizerStrength = 0
        )
    )
    val state: StateFlow<AudioFxState> = _state.asStateFlow()

    init {
        scope.launch {
            val prefs = preferencesRepository.preferences.first()
            val savedCustomLevels = parseBandsCsv(prefs.customBandLevelsCsv)
            val basePreset = builtInPresets.firstOrNull { it.id == prefs.activeEqPresetId } ?: builtInPresets.first()
            val levels = if (prefs.activeEqPresetId == "custom" && savedCustomLevels.isNotEmpty()) {
                savedCustomLevels
            } else {
                basePreset.bandLevelsMb
            }
            val currentBands = _state.value.bands.mapIndexed { idx, band ->
                val lvl = levels.getOrElse(idx) { 0 }.coerceIn(band.minLevelMb, band.maxLevelMb)
                band.copy(levelMb = lvl)
            }
            _state.value = _state.value.copy(
                enabled = prefs.eqEnabled,
                activePresetId = prefs.activeEqPresetId,
                bassBoostStrength = prefs.bassBoostStrength,
                virtualizerStrength = prefs.virtualizerStrength,
                bands = currentBands
            )
        }
    }

    fun attachToAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == attachedSessionId) return
        releaseEffects()
        attachedSessionId = audioSessionId

        val currentState = _state.value
        var hwSupported = false

        runCatching {
            val eq = Equalizer(0, audioSessionId)
            val bandCount = eq.numberOfBands.toInt().coerceAtLeast(0)
            val range = eq.bandLevelRange
            val minMb = range.getOrElse(0) { -1500 }.toInt()
            val maxMb = range.getOrElse(1) { 1500 }.toInt()

            val hwBands = (0 until bandCount).map { idx ->
                val centerHz = (eq.getCenterFreq(idx.toShort()) / 1000).coerceAtLeast(30)
                val desiredLevel = currentState.bands.getOrNull(idx)?.levelMb ?: 0
                val clamped = desiredLevel.coerceIn(minMb, maxMb)
                eq.setBandLevel(idx.toShort(), clamped.toShort())
                EqBandInfo(
                    index = idx,
                    centerFreqHz = centerHz,
                    levelMb = clamped,
                    minLevelMb = minMb,
                    maxLevelMb = maxMb
                )
            }
            eq.enabled = currentState.enabled
            equalizer = eq
            hwSupported = bandCount > 0

            _state.value = _state.value.copy(
                hardwareSupported = hwSupported,
                bands = if (hwBands.isNotEmpty()) hwBands else _state.value.bands
            )
        }.onFailure {
            _state.value = _state.value.copy(hardwareSupported = false)
        }

        runCatching {
            val bb = BassBoost(0, audioSessionId)
            if (bb.strengthSupported) {
                bb.setStrength(currentState.bassBoostStrength.coerceIn(0, 1000).toShort())
            }
            bb.enabled = currentState.enabled && currentState.bassBoostStrength > 0
            bassBoost = bb
        }

        runCatching {
            val virt = Virtualizer(0, audioSessionId)
            if (virt.strengthSupported) {
                virt.setStrength(currentState.virtualizerStrength.coerceIn(0, 1000).toShort())
            }
            virt.enabled = currentState.enabled && currentState.virtualizerStrength > 0
            virtualizer = virt
        }
    }

    fun setEnabled(enabled: Boolean) {
        val current = _state.value.copy(enabled = enabled)
        _state.value = current
        applyStateToHardware(current)
        persistState(current)
    }

    fun selectPreset(presetId: String) {
        val preset = builtInPresets.firstOrNull { it.id == presetId } ?: return
        val updatedBands = _state.value.bands.mapIndexed { idx, band ->
            val target = preset.bandLevelsMb.getOrElse(idx) { 0 }.coerceIn(band.minLevelMb, band.maxLevelMb)
            band.copy(levelMb = target)
        }
        val nextState = _state.value.copy(
            enabled = true,
            activePresetId = preset.id,
            bands = updatedBands,
            bassBoostStrength = preset.bassBoost,
            virtualizerStrength = preset.virtualizer
        )
        _state.value = nextState
        applyStateToHardware(nextState)
        persistState(nextState)
    }

    fun setBandLevel(bandIndex: Int, levelMb: Int) {
        val updatedBands = _state.value.bands.map { band ->
            if (band.index == bandIndex) {
                band.copy(levelMb = levelMb.coerceIn(band.minLevelMb, band.maxLevelMb))
            } else band
        }
        val nextState = _state.value.copy(
            enabled = true,
            activePresetId = "custom",
            bands = updatedBands
        )
        _state.value = nextState
        runCatching {
            equalizer?.enabled = true
            val band = updatedBands.getOrNull(bandIndex)
            if (band != null) {
                equalizer?.setBandLevel(bandIndex.toShort(), band.levelMb.toShort())
            }
        }
        persistState(nextState, saveCustomBands = true)
    }

    fun setBassBoost(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        val nextState = _state.value.copy(
            enabled = true,
            bassBoostStrength = clamped
        )
        _state.value = nextState
        runCatching {
            bassBoost?.let { bb ->
                if (bb.strengthSupported) bb.setStrength(clamped.toShort())
                bb.enabled = clamped > 0
            }
        }
        persistState(nextState)
    }

    fun setVirtualizer(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        val nextState = _state.value.copy(
            enabled = true,
            virtualizerStrength = clamped
        )
        _state.value = nextState
        runCatching {
            virtualizer?.let { virt ->
                if (virt.strengthSupported) virt.setStrength(clamped.toShort())
                virt.enabled = clamped > 0
            }
        }
        persistState(nextState)
    }

    fun resetFlat() {
        selectPreset("flat")
    }

    private fun applyStateToHardware(s: AudioFxState) {
        runCatching {
            equalizer?.let { eq ->
                s.bands.forEach { band ->
                    if (band.index < eq.numberOfBands) {
                        eq.setBandLevel(band.index.toShort(), band.levelMb.toShort())
                    }
                }
                eq.enabled = s.enabled
            }
        }
        runCatching {
            bassBoost?.let { bb ->
                if (bb.strengthSupported) bb.setStrength(s.bassBoostStrength.toShort())
                bb.enabled = s.enabled && s.bassBoostStrength > 0
            }
        }
        runCatching {
            virtualizer?.let { virt ->
                if (virt.strengthSupported) virt.setStrength(s.virtualizerStrength.toShort())
                virt.enabled = s.enabled && s.virtualizerStrength > 0
            }
        }
    }

    private fun persistState(s: AudioFxState, saveCustomBands: Boolean = false) {
        val csv = if (saveCustomBands) s.bands.joinToString(",") { it.levelMb.toString() } else null
        scope.launch {
            preferencesRepository.saveEqState(
                enabled = s.enabled,
                presetId = s.activePresetId,
                bassBoost = s.bassBoostStrength,
                virtualizer = s.virtualizerStrength,
                customBandsCsv = csv
            )
        }
    }

    private fun parseBandsCsv(csv: String): List<Int> {
        if (csv.isBlank()) return emptyList()
        return csv.split(",").mapNotNull { it.trim().toIntOrNull() }
    }

    fun releaseEffects() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        attachedSessionId = 0
    }
}
