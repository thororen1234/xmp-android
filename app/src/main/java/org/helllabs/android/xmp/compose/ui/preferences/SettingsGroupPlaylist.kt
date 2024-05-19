package org.helllabs.android.xmp.compose.ui.preferences

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsMenuLink
import com.alorma.compose.settings.ui.SettingsSwitch
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.SingleChoiceAlertDialog
import org.helllabs.android.xmp.core.PrefManager

@Composable
fun SettingsGroupPlaylist(
    onChangeDir: () -> Unit
) {
    SettingsGroup(
        title = {
            Text(text = stringResource(id = R.string.pref_category_files))
        }
    ) {
        SettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.pref_media_path_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_media_path_summary)) },
            onClick = onChangeDir
        )

        var installModules by remember { mutableStateOf(PrefManager.examples) }
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_examples_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_examples_summary)) },
            state = installModules,
            onCheckedChange = {
                PrefManager.examples = it
                installModules = it
            }

        )

        var playlistMode by remember { mutableIntStateOf(0) }
        val playlistModeValues = stringArrayResource(id = R.array.playlist_mode_values)
        val playlistModeDialog = remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            playlistModeValues.forEachIndexed { index, s ->
                if (PrefManager.playlistMode == s.toInt()) {
                    playlistMode = index
                }
            }
        }
        SettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.pref_playlist_mode_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_playlist_mode_summary)) },
            onClick = {
                playlistModeDialog.value = true
            }
        )
        if (playlistModeDialog.value) {
            val items = stringArrayResource(id = R.array.playlist_mode_array).toList()
            SingleChoiceAlertDialog(
                title = stringResource(id = R.string.pref_playlist_mode_title),
                items = items,
                selectedItemIndex = playlistMode,
                onItemSelected = {
                    PrefManager.playlistMode = it
                    playlistModeDialog.value = false
                },
                onCancel = {
                    playlistModeDialog.value = false
                }
            )
        }

        var useFileName by remember { mutableStateOf(PrefManager.useFileName) }
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_use_filename_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_use_filename_summary)) },
            state = useFileName,
            onCheckedChange = {
                PrefManager.useFileName = it
                useFileName = it
            }
        )

        var backButton by remember { mutableStateOf(PrefManager.backButtonNavigation) }
        SettingsSwitch(
            title = {
                Text(text = stringResource(id = R.string.pref_back_button_navigation_title))
            },
            subtitle = {
                Text(text = stringResource(id = R.string.pref_back_button_navigation_summary))
            },
            state = backButton,
            onCheckedChange = {
                PrefManager.backButtonNavigation = it
                backButton = it
            }
        )
    }
}
