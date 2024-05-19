package org.helllabs.android.xmp.compose.ui.preferences

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.*
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsSwitch
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.core.PrefManager

@Composable
fun SettingsGroupDownload() {
    SettingsGroup(
        title = { Text(text = stringResource(id = R.string.pref_category_modarchive)) }
    ) {
        var modArchive by remember { mutableStateOf(PrefManager.modArchiveFolder) }
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_modarchive_folder_title)) },
            subtitle = {
                Text(text = stringResource(id = R.string.pref_modarchive_folder_summary))
            },
            state = modArchive,
            onCheckedChange = {
                modArchive = it
                PrefManager.modArchiveFolder = it
            }
        )
        var artist by remember { mutableStateOf(PrefManager.artistFolder) }
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_artist_folder_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_artist_folder_summary)) },
            state = artist,
            onCheckedChange = {
                artist = it
                PrefManager.artistFolder = it
            }
        )
    }
}
