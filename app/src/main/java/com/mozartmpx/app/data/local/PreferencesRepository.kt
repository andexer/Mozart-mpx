package com.mozartmpx.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mozartmpx.app.data.model.RepeatMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.mozartDataStore: DataStore<Preferences> by preferencesDataStore(name = "mozart_mpx_prefs")

data class UserAudioPreferences(
    val crossfadeSeconds: Int = 3,
    val smartAutoplayEnabled: Boolean = true,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val eqEnabled: Boolean = false,
    val activeEqPresetId: String = "flat",
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val customBandLevelsCsv: String = "",
    val safTreeUris: Set<String> = emptySet()
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val CROSSFADE_SECONDS = intPreferencesKey("crossfade_seconds")
        val SMART_AUTOPLAY = booleanPreferencesKey("smart_autoplay")
        val SHUFFLE = booleanPreferencesKey("shuffle")
        val REPEAT_MODE = stringPreferencesKey("repeat_mode")
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val ACTIVE_EQ_PRESET = stringPreferencesKey("active_eq_preset")
        val BASS_BOOST = intPreferencesKey("bass_boost")
        val VIRTUALIZER = intPreferencesKey("virtualizer")
        val CUSTOM_BANDS_CSV = stringPreferencesKey("custom_bands_csv")
        val SAF_TREE_URIS = stringSetPreferencesKey("saf_tree_uris")
    }

    val preferences: Flow<UserAudioPreferences> = context.mozartDataStore.data.map { prefs ->
        UserAudioPreferences(
            crossfadeSeconds = (prefs[Keys.CROSSFADE_SECONDS] ?: 3).coerceIn(0, 10),
            smartAutoplayEnabled = prefs[Keys.SMART_AUTOPLAY] ?: true,
            shuffleEnabled = prefs[Keys.SHUFFLE] ?: false,
            repeatMode = runCatching {
                RepeatMode.valueOf(prefs[Keys.REPEAT_MODE] ?: RepeatMode.OFF.name)
            }.getOrDefault(RepeatMode.OFF),
            eqEnabled = prefs[Keys.EQ_ENABLED] ?: false,
            activeEqPresetId = prefs[Keys.ACTIVE_EQ_PRESET] ?: "flat",
            bassBoostStrength = (prefs[Keys.BASS_BOOST] ?: 0).coerceIn(0, 1000),
            virtualizerStrength = (prefs[Keys.VIRTUALIZER] ?: 0).coerceIn(0, 1000),
            customBandLevelsCsv = prefs[Keys.CUSTOM_BANDS_CSV] ?: "",
            safTreeUris = prefs[Keys.SAF_TREE_URIS] ?: emptySet()
        )
    }.catch { emit(UserAudioPreferences()) }

    suspend fun setCrossfadeSeconds(seconds: Int) {
        context.mozartDataStore.edit { it[Keys.CROSSFADE_SECONDS] = seconds.coerceIn(0, 10) }
    }

    suspend fun setSmartAutoplayEnabled(enabled: Boolean) {
        context.mozartDataStore.edit { it[Keys.SMART_AUTOPLAY] = enabled }
    }

    suspend fun setShuffleEnabled(enabled: Boolean) {
        context.mozartDataStore.edit { it[Keys.SHUFFLE] = enabled }
    }

    suspend fun setRepeatMode(mode: RepeatMode) {
        context.mozartDataStore.edit { it[Keys.REPEAT_MODE] = mode.name }
    }

    suspend fun saveEqState(
        enabled: Boolean,
        presetId: String,
        bassBoost: Int,
        virtualizer: Int,
        customBandsCsv: String? = null
    ) {
        context.mozartDataStore.edit { prefs ->
            prefs[Keys.EQ_ENABLED] = enabled
            prefs[Keys.ACTIVE_EQ_PRESET] = presetId
            prefs[Keys.BASS_BOOST] = bassBoost.coerceIn(0, 1000)
            prefs[Keys.VIRTUALIZER] = virtualizer.coerceIn(0, 1000)
            if (customBandsCsv != null) {
                prefs[Keys.CUSTOM_BANDS_CSV] = customBandsCsv
            }
        }
    }

    suspend fun addSafTreeUri(uriString: String) {
        context.mozartDataStore.edit { prefs ->
            val current = prefs[Keys.SAF_TREE_URIS] ?: emptySet()
            prefs[Keys.SAF_TREE_URIS] = current + uriString
        }
    }
}
