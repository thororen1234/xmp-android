package org.helllabs.android.xmp.compose.ui.preferences

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsSwitch
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.core.PrefManager

@Composable
fun SettingsGroupInterface() {
    SettingsGroup(
        title = { Text(text = stringResource(id = R.string.pref_category_interface)) }
    ) {
        var showInfoLine by remember { mutableStateOf(PrefManager.showInfoLine) }
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_show_info_line_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_show_info_line_summary)) },
            state = showInfoLine,
            onCheckedChange = {
                showInfoLine = it
                PrefManager.showInfoLine = it
            }
        )
        var showToast by remember { mutableStateOf(PrefManager.showToast) }
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_show_toast_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_show_toast_summary)) },
            state = showToast,
            onCheckedChange = {
                showToast = it
                PrefManager.showToast = it
            }
        )
        var keepScreenOn by remember { mutableStateOf(PrefManager.keepScreenOn) }
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_keep_screen_on_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_keep_screen_on_summary)) },
            state = keepScreenOn,
            onCheckedChange = {
                keepScreenOn = it
                PrefManager.keepScreenOn = it
            }
        )
        var betterWaveform by remember { mutableStateOf(PrefManager.useBetterWaveform) }
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_draw_lines_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_draw_lines_summary)) },
            state = betterWaveform,
            onCheckedChange = {
                betterWaveform = it
                PrefManager.useBetterWaveform = it
            }
        )
        var startOnPlayer by remember { mutableStateOf(PrefManager.startOnPlayer) }
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_start_on_player_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_start_on_player_summary)) },
            state = startOnPlayer,
            onCheckedChange = {
                startOnPlayer = it
                PrefManager.startOnPlayer = it
            }
        )
        var enableDelete by remember { mutableStateOf(PrefManager.enableDelete) }
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_enable_delete_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_enable_delete_summary)) },
            state = enableDelete,
            onCheckedChange = {
                enableDelete = it
                PrefManager.enableDelete = it
            }
        )

        var showHex by remember { mutableStateOf(PrefManager.showHex) }
        SettingsSwitch(
            title = { Text(text = "Show hex values") },
            subtitle = { Text(text = "Show hex values for player info") },
            state = showHex,
            onCheckedChange = {
                showHex = it
                PrefManager.showHex = it
            }
        )

        var useMediaStyle by remember { mutableStateOf(PrefManager.useMediaStyle) }
        SettingsSwitch(
            title = { Text(text = "Use media style notification") },
            subtitle = {
                Text(text = "Use a standard notification for media controls if disabled")
            },
            state = useMediaStyle,
            onCheckedChange = {
                useMediaStyle = it
                PrefManager.useMediaStyle = it
            }
        )
    }
}
