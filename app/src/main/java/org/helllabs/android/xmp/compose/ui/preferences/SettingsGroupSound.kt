package org.helllabs.android.xmp.compose.ui.preferences

import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.*
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsMenuLink
import com.alorma.compose.settings.ui.SettingsSlider
import com.alorma.compose.settings.ui.SettingsSwitch
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.SingleChoiceListDialog
import org.helllabs.android.xmp.core.PrefManager
import timber.log.Timber

@Composable
fun SettingsGroupSound() {
    SettingsGroup(
        title = { Text(text = stringResource(id = R.string.pref_category_sound)) }
    ) {
        val samplingRateDialog = remember { mutableStateOf(false) }
        var samplingRate by remember { mutableIntStateOf(0) }
        val samplingRateValues = stringArrayResource(id = R.array.sampling_rate_values)
        LaunchedEffect(Unit) {
            samplingRateValues.forEachIndexed { index, s ->
                if (PrefManager.samplingRate == s.toInt()) {
                    samplingRate = index
                }
            }
        }
        SettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.pref_sampling_rate_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_sampling_rate_summary)) },
            onClick = {
                samplingRateDialog.value = true
            }
        )
        SingleChoiceListDialog(
            isShowing = samplingRateDialog.value,
            title = stringResource(id = R.string.pref_playlist_mode_title),
            icon = Icons.Filled.CheckCircle,
            list = stringArrayResource(id = R.array.sampling_rate_array).toList(),
            selectedIndex = samplingRate,
            onConfirm = {
                PrefManager.samplingRate = it
                samplingRateDialog.value = false
            },
            onDismiss = {
                samplingRateDialog.value = false
            },
            onEmpty = {
                samplingRateDialog.value = false
            }
        )

        var bufferSize by remember { mutableFloatStateOf(PrefManager.bufferMs.toFloat()) }
        SettingsSlider(
            title = { Text(text = stringResource(id = R.string.pref_buffer_ms_title)) },
            subtitle = {
                Text(
                    text = stringResource(
                        id = R.string.pref_buffer_ms_dialog,
                        "${bufferSize.toInt()}ms"
                    )
                )
            },
            valueRange = 1f..1000f,
            value = bufferSize,
            onValueChange = {
                bufferSize = it
            },
            onValueChangeFinished = {
                Timber.d("Setting value to: $bufferSize")
                PrefManager.bufferMs = bufferSize.toInt()
            }
        )

        val volBoostDialog = remember { mutableStateOf(false) }
        var volBoost by remember { mutableIntStateOf(0) }
        val volBoostValues = stringArrayResource(id = R.array.vol_boost_values)
        LaunchedEffect(Unit) {
            volBoostValues.forEachIndexed { index, s ->
                if (PrefManager.volumeBoost == s.toInt()) {
                    volBoost = index
                }
            }
        }
        SettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.pref_vol_boost_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_vol_boost_summary)) },
            onClick = {
                volBoostDialog.value = true
            }
        )
        SingleChoiceListDialog(
            isShowing = volBoostDialog.value,
            title = stringResource(id = R.string.pref_playlist_mode_title),
            icon = Icons.Filled.CheckCircle,
            list = stringArrayResource(id = R.array.vol_boost_array).toList(),
            selectedIndex = volBoost,
            onConfirm = {
                PrefManager.volumeBoost = it
                volBoostDialog.value = false
            },
            onDismiss = {
                volBoostDialog.value = false
            },
            onEmpty = {
                volBoostDialog.value = false
            }
        )

        val amigaMixer by remember { mutableStateOf(PrefManager.amigaMixer) }
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_amiga_mixer_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_amiga_mixer_summary)) },
            state = amigaMixer,
            onCheckedChange = {
                PrefManager.amigaMixer = it
            }
        )

        val interpolate by remember { mutableStateOf(PrefManager.interpolate) }
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_interpolate_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_interpolate_summary)) },
            state = interpolate,
            onCheckedChange = {
                PrefManager.interpolate = it
            }
        )

        val interpTypeDialog = remember { mutableStateOf(false) }
        var interpType by remember { mutableIntStateOf(0) }
        val interpTypeValues = stringArrayResource(id = R.array.interp_type_values)
        LaunchedEffect(Unit) {
            interpTypeValues.forEachIndexed { index, s ->
                if (PrefManager.interpType == s.toInt()) {
                    interpType = index
                }
            }
        }
        SettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.pref_interp_type_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_interp_type_summary)) },
            onClick = {
                interpTypeDialog.value = true
            }
        )
        SingleChoiceListDialog(
            isShowing = interpTypeDialog.value,
            icon = Icons.Filled.CheckCircle,
            title = stringResource(id = R.string.pref_playlist_mode_title),
            list = stringArrayResource(id = R.array.interp_type_array).toList(),
            selectedIndex = samplingRate,
            onConfirm = {
                PrefManager.interpType = it
                interpTypeDialog.value = false
            },
            onDismiss = {
                interpTypeDialog.value = false
            },
            onEmpty = {
                interpTypeDialog.value = false
            }
        )

        var stereoMix by remember { mutableFloatStateOf(PrefManager.stereoMix.toFloat()) }
        SettingsSlider(
            title = { Text(text = stringResource(id = R.string.pref_pan_separation_title)) },
            subtitle = {
                Text(
                    text = stringResource(
                        id = R.string.pref_pan_separation_dialog,
                        "${stereoMix.toInt()}%"
                    )
                )
            },
            valueRange = 1f..100f,
            value = stereoMix,
            onValueChange = {
                stereoMix = it
            },
            onValueChangeFinished = {
                Timber.d("Setting value to: $stereoMix")
                PrefManager.stereoMix = stereoMix.toInt()
            }
        )

        var defaultPan by remember { mutableFloatStateOf(PrefManager.defaultPan.toFloat()) }
        SettingsSlider(
            title = { Text(text = stringResource(id = R.string.pref_default_pan_title)) },
            subtitle = {
                Text(
                    text = stringResource(
                        id = R.string.pref_default_pan_dialog,
                        "${defaultPan.toInt()}%"
                    )
                )
            },
            valueRange = 1f..100f,
            value = defaultPan,
            onValueChange = {
                defaultPan = it
            },
            onValueChangeFinished = {
                Timber.d("Setting value to: $defaultPan")
                PrefManager.defaultPan = defaultPan.toInt()
            }
        )

        val allSequence by remember { mutableStateOf(PrefManager.allSequences) }
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_all_sequences_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_all_sequences_summary)) },
            state = allSequence,
            onCheckedChange = {
                PrefManager.allSequences = it
            }
        )
    }
}
