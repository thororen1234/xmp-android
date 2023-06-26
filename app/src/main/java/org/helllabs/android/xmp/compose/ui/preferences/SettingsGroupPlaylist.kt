package org.helllabs.android.xmp.compose.ui.preferences

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.alorma.compose.settings.storage.base.rememberBooleanSettingState
import com.alorma.compose.settings.storage.base.rememberIntSettingState
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsList
import com.alorma.compose.settings.ui.SettingsMenuLink
import com.alorma.compose.settings.ui.SettingsSwitch
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.TextInputDialog
import timber.log.Timber

@Composable
fun SettingsGroupPlaylist(
    onClearCache: () -> Unit
) {
    SettingsGroup(
        title = {
            Text(text = stringResource(id = R.string.pref_category_files))
        }
    ) {
        var mediaPathDialog by remember { mutableStateOf(false) }
        var mediaPath by remember { mutableStateOf(PrefManager.mediaPath) }
        TextInputDialog(
            isShowing = mediaPathDialog,
            title = stringResource(id = R.string.pref_media_path_title),
            defaultText = mediaPath,
            onConfirm = {
                mediaPath = it
                PrefManager.mediaPath = it
                mediaPathDialog = false
            },
            onDismiss = {
                mediaPathDialog = false
            }
        )

        SettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.pref_media_path_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_media_path_summary)) },
            onClick = {
                mediaPathDialog = true
            }
        )

        val installModules = rememberBooleanSettingState(PrefManager.examples)
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_examples_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_examples_summary)) },
            state = installModules,
            onCheckedChange = {
                PrefManager.examples = it
            }
        )

        val playlistMode = rememberIntSettingState()
        val playlistModeValues = stringArrayResource(id = R.array.playlist_mode_values)
        LaunchedEffect(Unit) {
            playlistModeValues.forEachIndexed { index, s ->
                if (PrefManager.playlistMode == s.toInt()) {
                    playlistMode.value = index
                }
            }
        }
        SettingsList(
            title = { Text(text = stringResource(id = R.string.pref_playlist_mode_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_playlist_mode_summary)) },
            useSelectedValueAsSubtitle = false,
            state = playlistMode,
            items = stringArrayResource(id = R.array.playlist_mode_array).toList(),
            onItemSelected = { i, s ->
                Timber.d("Setting value to: $s")
                PrefManager.playlistMode = playlistModeValues[i].toInt()
            }
        )

        val useFileName = rememberBooleanSettingState(PrefManager.useFileName)
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_use_filename_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_use_filename_summary)) },
            state = useFileName,
            onCheckedChange = {
                PrefManager.useFileName = it
            }
        )

        val backButton = rememberBooleanSettingState(PrefManager.backButtonNavigation)
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_back_button_navigation_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_back_button_navigation_summary)) },
            state = backButton,
            onCheckedChange = {
                PrefManager.backButtonNavigation = it
            }
        )

        SettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.pref_clear_cache_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_clear_cache_summary)) },
            onClick = onClearCache
        )
    }
}
