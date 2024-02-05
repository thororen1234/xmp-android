package org.helllabs.android.xmp.compose.ui.preferences

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.alorma.compose.settings.storage.base.rememberBooleanSettingState
import com.alorma.compose.settings.ui.SettingsGroup
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.ui.preferences.components.FixedSettingsSwitch

@Composable
fun SettingsGroupInterface() {
    SettingsGroup(
        title = { Text(text = stringResource(id = R.string.pref_category_interface)) }
    ) {
        val showInfoLine = rememberBooleanSettingState(PrefManager.showInfoLine)
        FixedSettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_show_info_line_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_show_info_line_summary)) },
            state = showInfoLine,
            onCheckedChange = {
                showInfoLine.value = it
                PrefManager.showInfoLine = it
            }
        )
        val showToast = rememberBooleanSettingState(PrefManager.showToast)
        FixedSettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_show_toast_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_show_toast_summary)) },
            state = showToast,
            onCheckedChange = {
                showToast.value = it
                PrefManager.showToast = it
            }
        )
        val keepScreenOn = rememberBooleanSettingState(PrefManager.keepScreenOn)
        FixedSettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_keep_screen_on_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_keep_screen_on_summary)) },
            state = keepScreenOn,
            onCheckedChange = {
                keepScreenOn.value = it
                PrefManager.keepScreenOn = it
            }
        )
        val betterWaveform = rememberBooleanSettingState(PrefManager.useBetterWaveform)
        FixedSettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_draw_lines_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_draw_lines_summary)) },
            state = betterWaveform,
            onCheckedChange = {
                betterWaveform.value = it
                PrefManager.useBetterWaveform = it
            }
        )
        val startOnPlayer = rememberBooleanSettingState(PrefManager.startOnPlayer)
        FixedSettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_start_on_player_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_start_on_player_summary)) },
            state = startOnPlayer,
            onCheckedChange = {
                startOnPlayer.value = it
                PrefManager.startOnPlayer = it
            }
        )
        val enableDelete = rememberBooleanSettingState(PrefManager.enableDelete)
        FixedSettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_enable_delete_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_enable_delete_summary)) },
            state = enableDelete,
            onCheckedChange = {
                enableDelete.value = it
                PrefManager.enableDelete = it
            }
        )

        val showHex = rememberBooleanSettingState(PrefManager.showHex)
        FixedSettingsSwitch(
            title = { Text(text = "Show hex values") },
            subtitle = { Text(text = "Show hex values for player info") },
            state = showHex,
            onCheckedChange = {
                showHex.value = it
                PrefManager.showHex = it
            }
        )

        val useMediaStyle = rememberBooleanSettingState(PrefManager.useMediaStyle)
        FixedSettingsSwitch(
            title = { Text(text = "Use media style notification") },
            subtitle = { Text(text = "Use a standard notification for media controls if disabled") },
            state = useMediaStyle,
            onCheckedChange = {
                useMediaStyle.value = it
                PrefManager.useMediaStyle = it
            }
        )
    }
}
