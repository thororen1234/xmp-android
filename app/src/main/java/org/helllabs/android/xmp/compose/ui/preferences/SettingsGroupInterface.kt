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
    }
}
