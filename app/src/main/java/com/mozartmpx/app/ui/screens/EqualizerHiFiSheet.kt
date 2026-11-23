package com.mozartmpx.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.mozartmpx.app.data.model.AudioFxState
import com.mozartmpx.app.data.model.EqBandInfo
import com.mozartmpx.app.data.model.Track
import com.mozartmpx.app.data.model.formatDuration
import com.mozartmpx.app.data.model.formatFileSize
import com.mozartmpx.app.ui.components.HiFiBadge
import com.mozartmpx.app.ui.theme.MonoSpecStyle
import com.mozartmpx.app.ui.theme.MonoTimerStyle
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerHiFiSheet(
    audioFxState: AudioFxState,
    crossfadeSeconds: Int,
    inspectedTrack: Track?,
    onDismiss: () -> Unit,
    onSetEqEnabled: (Boolean) -> Unit,
    onSelectPreset: (String) -> Unit,
    onSetBandLevel: (Int, Int) -> Unit,
    onSetBassBoost: (Int) -> Unit,
    onSetVirtualizer: (Int) -> Unit,
    onResetEq: () -> Unit,
    onSetCrossfadeSeconds: (Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var localCrossfade by remember { mutableFloatStateOf(crossfadeSeconds.toFloat()) }
    var crossfadeScrubbing by remember { mutableStateOf(false) }
    LaunchedEffect(crossfadeSeconds) {
        if (!crossfadeScrubbing) localCrossfade = crossfadeSeconds.toFloat()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 32.dp)
                .testTag("equalizer_hifi_sheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Ecualizador & Hi-Fi",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = if (audioFxState.hardwareSupported) "Motor DSP activo del dispositivo"
                            else "Modo de perfil guardado (DSP no expuesto)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onResetEq,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("eq_reset_button")
                    ) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = "Restablecer a plano")
                    }
                    Switch(
                        checked = audioFxState.enabled,
                        onCheckedChange = onSetEqEnabled,
                        modifier = Modifier.testTag("eq_master_switch")
                    )
                }
            }

            // Genre Presets
            LazyRow(
                contentPadding = PaddingValues(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(audioFxState.presets, key = { it.id }) { preset ->
                    val selected = audioFxState.activePresetId == preset.id
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectPreset(preset.id) },
                        label = { Text(preset.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("eq_preset_${preset.id}")
                    )
                }
            }

            // Vertical Band Sliders Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        RoundedCornerShape(20.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    audioFxState.bands.forEach { band ->
                        VerticalBandControl(
                            band = band,
                            enabled = audioFxState.enabled,
                            onValueChange = { mb -> onSetBandLevel(band.index, mb) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Bass Boost & Virtualizer
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LabeledAudioSlider(
                        title = "Refuerzo de bajos (Bass Boost)",
                        value = audioFxState.bassBoostStrength.toFloat(),
                        valueRange = 0f..1000f,
                        valueFormatter = { "${(it.roundToInt() / 10)}%" },
                        onValueChange = { onSetBassBoost(it.roundToInt()) },
                        testTag = "eq_bass_slider"
                    )
                    LabeledAudioSlider(
                        title = "Virtualizador espacial",
                        value = audioFxState.virtualizerStrength.toFloat(),
                        valueRange = 0f..1000f,
                        valueFormatter = { "${(it.roundToInt() / 10)}%" },
                        onValueChange = { onSetVirtualizer(it.roundToInt()) },
                        testTag = "eq_virtualizer_slider"
                    )
                }
            }

            // Gapless & Adjustable Crossfade Control
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Waves,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text("Transiciones sin pausas", style = MaterialTheme.typography.titleMedium)
                        }
                        HiFiBadge(
                            label = if (crossfadeSeconds == 0) "GAPLESS PURO" else "CROSSFADE ${crossfadeSeconds}s",
                            emphasized = true
                        )
                    }
                    Text(
                        text = if (crossfadeSeconds == 0) {
                            "Reproducción continua sin pausas ni fundido (fiel al orden del álbum)."
                        } else {
                            "Fundido cruzado suave de $crossfadeSeconds segundos entre pistas sin silencios."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = localCrossfade,
                        onValueChange = {
                            localCrossfade = it
                            crossfadeScrubbing = true
                        },
                        onValueChangeFinished = {
                            onSetCrossfadeSeconds(localCrossfade.roundToInt())
                            crossfadeScrubbing = false
                        },
                        valueRange = 0f..10f,
                        steps = 9,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("crossfade_slider")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0s (Gapless)", style = MonoTimerStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("5s", style = MonoTimerStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("10s", style = MonoTimerStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Technical Hi-Fi Spec Sheet
            inspectedTrack?.let { track ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            RoundedCornerShape(20.dp)
                        )
                        .testTag("hifi_technical_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.HighQuality,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text("Ficha técnica Hi-Fi", style = MaterialTheme.typography.titleMedium)
                            }
                            HiFiBadge(label = track.hiFiShortBadge, emphasized = true)
                        }

                        Text(
                            text = "${track.title} — ${track.artist}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        SpecRow("Formato / Códec", "${track.formatLabel} (${track.mimeType})")
                        SpecRow(
                            "Tasa de muestreo",
                            if (track.sampleRateHz > 0) {
                                String.format(Locale.getDefault(), "%.1f kHz (%d Hz)", track.sampleRateHz / 1000.0, track.sampleRateHz)
                            } else "—"
                        )
                        SpecRow(
                            "Tasa de bits (Bitrate)",
                            if (track.bitrateKbps > 0) "${track.bitrateKbps} kbps" else "—"
                        )
                        SpecRow("Canales", if (track.channels == 1) "Mono (1.0)" else "Estéreo (${track.channels}.0)")
                        SpecRow("Duración · Tamaño", "${formatDuration(track.durationMs)} · ${formatFileSize(track.fileSizeBytes)}")
                        SpecRow("Género · Año", "${track.genre.ifBlank { "Auto-detectado" }} · ${if (track.year > 0) track.year else "—"}")
                        SpecRow("Álbum", track.album.ifBlank { "—" })
                        SpecRow("Artista del álbum", track.albumArtist.ifBlank { track.artist })
                        SpecRow("N.º de pista", if (track.trackNumber > 0) track.trackNumber.toString() else "—")
                        SpecRow("Favorita", if (track.isFavorite) "Sí" else "No")
                        SpecRow("Carpeta local", track.folderPath)
                        SpecRow(
                            "Ruta / URI del archivo",
                            track.uri.removePrefix("file://").ifBlank { "—" }
                        )
                        SpecRow(
                            "Carátula",
                            track.artworkUri?.takeIf { it.isNotBlank() } ?: "Incrustada o no disponible"
                        )
                        SpecRow(
                            "Añadida",
                            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault())
                                .format(Date(
                                    if (track.dateAdded in 1L..99_999_999_999L) track.dateAdded * 1000L else track.dateAdded
                                ))
                        )
                        SpecRow("ID interno", track.id)
                    }
                }
            }
        }
    }
}

@Composable
private fun VerticalBandControl(
    band: EqBandInfo,
    enabled: Boolean,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var localLevel by remember { mutableFloatStateOf(band.levelMb.toFloat()) }
    var scrubbing by remember { mutableStateOf(false) }
    LaunchedEffect(band.levelMb) { if (!scrubbing) localLevel = band.levelMb.toFloat() }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "${localLevel.roundToInt()} dB",
            style = MonoSpecStyle,
            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Box(
            modifier = Modifier
                .height(140.dp)
                .width(44.dp),
            contentAlignment = Alignment.Center
        ) {
            Slider(
                value = localLevel,
                onValueChange = {
                    localLevel = it
                    scrubbing = true
                },
                onValueChangeFinished = {
                    onValueChange(localLevel.roundToInt())
                    scrubbing = false
                },
                valueRange = band.minLevelMb.toFloat()..band.maxLevelMb.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    activeTrackColor = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
                ),
                modifier = Modifier
                    .graphicsLayer {
                        rotationZ = 270f
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                    }
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(
                            Constraints(
                                minWidth = constraints.minHeight,
                                maxWidth = constraints.maxHeight,
                                minHeight = constraints.minWidth,
                                maxHeight = constraints.maxWidth
                            )
                        )
                        layout(placeable.height, placeable.width) {
                            placeable.place(
                                x = -(placeable.width - placeable.height) / 2,
                                y = (placeable.width - placeable.height) / 2
                            )
                        }
                    }
                    .width(136.dp)
                    .testTag("eq_band_${band.index}")
            )
        }
        Text(
            text = band.freqLabel,
            style = MonoSpecStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun LabeledAudioSlider(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueFormatter: (Float) -> String,
    onValueChange: (Float) -> Unit,
    testTag: String
) {
    var localValue by remember { mutableFloatStateOf(value) }
    var scrubbing by remember { mutableStateOf(false) }
    LaunchedEffect(value) { if (!scrubbing) localValue = value }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(text = valueFormatter(localValue), style = MonoTimerStyle, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = localValue,
            onValueChange = {
                localValue = it
                scrubbing = true
            },
            onValueChangeFinished = {
                onValueChange(localValue)
                scrubbing = false
            },
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = if (value.length > 40) MaterialTheme.typography.bodySmall else MonoTimerStyle,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            maxLines = if (value.length > 40) 4 else 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
