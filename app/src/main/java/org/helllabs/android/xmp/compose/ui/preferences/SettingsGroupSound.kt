package org.helllabs.android.xmp.compose.ui.preferences

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.alorma.compose.settings.storage.base.rememberBooleanSettingState
import com.alorma.compose.settings.storage.base.rememberFloatSettingState
import com.alorma.compose.settings.storage.base.rememberIntSettingState
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsList
import com.alorma.compose.settings.ui.SettingsSwitch
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.ui.preferences.components.XmpSettingsSlider
import timber.log.Timber

@Composable
fun SettingsGroupSound() {
    SettingsGroup(
        title = { Text(text = stringResource(id = R.string.pref_category_sound)) }
    ) {
        val samplingRate = rememberIntSettingState()
        val samplingRateValues = stringArrayResource(id = R.array.sampling_rate_values)
        LaunchedEffect(Unit) {
            samplingRateValues.forEachIndexed { index, s ->
                if (PrefManager.samplingRate == s.toInt()) {
                    samplingRate.value = index
                }
            }
        }
        SettingsList(
            title = { Text(text = stringResource(id = R.string.pref_sampling_rate_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_sampling_rate_summary)) },
            useSelectedValueAsSubtitle = false,
            state = samplingRate,
            items = stringArrayResource(id = R.array.sampling_rate_array).toList(),
            onItemSelected = { i, s ->
                Timber.d("Setting value to: $s")
                PrefManager.samplingRate = samplingRateValues[i].toInt()
            }
        )

        val bufferSize = rememberFloatSettingState(PrefManager.bufferMs.toFloat())
        XmpSettingsSlider(
            title = { Text(text = stringResource(id = R.string.pref_buffer_ms_title)) },
            subtitle = {
                Text(
                    text = stringResource(
                        id = R.string.pref_buffer_ms_dialog,
                        "${bufferSize.value.toInt()}ms"
                    )
                )
            },
            valueRange = 1f..1000f,
            state = bufferSize,
            onValueChangeFinished = {
                Timber.d("Setting value to: ${bufferSize.value}")
                PrefManager.bufferMs = bufferSize.value.toInt()
            }
        )

        val volBoost = rememberIntSettingState()
        val volBoostValues = stringArrayResource(id = R.array.vol_boost_values)
        LaunchedEffect(Unit) {
            volBoostValues.forEachIndexed { index, s ->
                if (PrefManager.volumeBoost == s.toInt()) {
                    volBoost.value = index
                }
            }
        }
        SettingsList(
            title = { Text(text = stringResource(id = R.string.pref_vol_boost_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_vol_boost_summary)) },
            useSelectedValueAsSubtitle = false,
            state = volBoost,
            items = stringArrayResource(id = R.array.vol_boost_array).toList(),
            onItemSelected = { i, s ->
                Timber.d("Setting value to: $s")
                PrefManager.volumeBoost = volBoostValues[i].toInt()
            }
        )

        val amigaMixer = rememberBooleanSettingState(PrefManager.amigaMixer)
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_amiga_mixer_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_amiga_mixer_summary)) },
            state = amigaMixer,
            onCheckedChange = {
                PrefManager.amigaMixer = it
            }
        )

        val interpolate = rememberBooleanSettingState(PrefManager.interpolate)
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_interpolate_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_interpolate_summary)) },
            state = interpolate,
            onCheckedChange = {
                PrefManager.interpolate = it
            }
        )

        val interpType = rememberIntSettingState()
        val interpTypeValues = stringArrayResource(id = R.array.interp_type_values)
        LaunchedEffect(Unit) {
            interpTypeValues.forEachIndexed { index, s ->
                if (PrefManager.interpType == s.toInt()) {
                    interpType.value = index
                }
            }
        }
        SettingsList(
            title = { Text(text = stringResource(id = R.string.pref_interp_type_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_interp_type_summary)) },
            useSelectedValueAsSubtitle = false,
            state = interpType,
            items = stringArrayResource(id = R.array.interp_type_array).toList(),
            onItemSelected = { i, s ->
                Timber.d("Setting value to: $s")
                PrefManager.interpType = interpTypeValues[i].toInt()
            }
        )

        val stereoMix = rememberFloatSettingState(PrefManager.stereoMix.toFloat())
        XmpSettingsSlider(
            title = { Text(text = stringResource(id = R.string.pref_pan_separation_title)) },
            subtitle = {
                Text(
                    text = stringResource(
                        id = R.string.preferences_pan_separation_dialog,
                        "${stereoMix.value.toInt()}%"
                    )
                )
            },
            valueRange = 1f..100f,
            state = stereoMix,
            onValueChangeFinished = {
                Timber.d("Setting value to: ${stereoMix.value}")
                PrefManager.stereoMix = stereoMix.value.toInt()
            }
        )

        val defaultPan = rememberFloatSettingState(PrefManager.defaultPan.toFloat())
        XmpSettingsSlider(
            title = { Text(text = stringResource(id = R.string.pref_default_pan_title)) },
            subtitle = {
                Text(
                    text = stringResource(
                        id = R.string.preferences_default_pan_dialog,
                        "${defaultPan.value.toInt()}%"
                    )
                )
            },
            valueRange = 1f..100f,
            state = defaultPan,
            onValueChangeFinished = {
                Timber.d("Setting value to: ${defaultPan.value}")
                PrefManager.defaultPan = defaultPan.value.toInt()
            }
        )

        val headset = rememberBooleanSettingState(PrefManager.headsetPause)
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_headset_pause_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_headset_pause_summary)) },
            state = headset,
            onCheckedChange = {
                PrefManager.headsetPause = it
            }
        )

        val bluetooth = rememberBooleanSettingState(PrefManager.bluetoothPause)
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_bluetooth_pause_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_bluetooth_pause_summary)) },
            state = bluetooth,
            onCheckedChange = {
                PrefManager.bluetoothPause = it
            }
        )

        val allSequence = rememberBooleanSettingState(PrefManager.allSequences)
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
