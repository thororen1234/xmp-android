package org.helllabs.android.xmp.compose.ui.preferences

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.alorma.compose.settings.storage.base.rememberBooleanSettingState
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsSwitch
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.R

@Composable
fun SettingsGroupDownload() {
    SettingsGroup(
        title = { Text(text = stringResource(id = R.string.pref_category_modarchive)) }
    ) {
        val modArchive = rememberBooleanSettingState(PrefManager.modArchiveFolder)
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_modarchive_folder_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_modarchive_folder_summary)) },
            state = modArchive,
            onCheckedChange = {
                modArchive.value = it
                PrefManager.modArchiveFolder = it
            }
        )
        val artist = rememberBooleanSettingState(PrefManager.artistFolder)
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_artist_folder_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_artist_folder_summary)) },
            state = artist,
            onCheckedChange = {
                artist.value = it
                PrefManager.artistFolder = it
            }
        )
    }
}
