package org.helllabs.android.xmp.compose.ui.menu

import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.browser.playlist.Playlist
import org.helllabs.android.xmp.browser.playlist.PlaylistItem
import org.helllabs.android.xmp.browser.playlist.PlaylistUtils
import org.helllabs.android.xmp.util.FileUtils
import timber.log.Timber
import java.io.File
import java.io.IOException

class PlaylistMenuViewModel : ViewModel() {
    data class PlaylistMenuState(
        val isRefreshing: Boolean = false,
        val errorText: String = "",
        val isFatalError: Boolean = false,
        val mediaPath: String = "",
        val playlistItems: List<PlaylistItem> = listOf()
    )

    private val _uiState = MutableStateFlow(PlaylistMenuState())
    val uiState = _uiState.asStateFlow()

    fun showError(message: String, isFatal: Boolean) {
        _uiState.update { it.copy(errorText = message, isFatalError = isFatal) }
    }

    fun checkStorage(): Boolean {
        val state = Environment.getExternalStorageState()
        val result = if (Environment.MEDIA_MOUNTED == state ||
            Environment.MEDIA_MOUNTED_READ_ONLY == state
        ) {
            true
        } else {
            Timber.e("External storage state error: $state")
            false
        }

        return result
    }

    // Create application directory and populate with empty playlist
    fun setupDataDir(
        name: String,
        comment: String,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        if (!PrefManager.DATA_DIR.isDirectory) {
            if (PrefManager.DATA_DIR.mkdirs()) {
                PlaylistUtils.createEmptyPlaylist(
                    name = name,
                    comment = comment,
                    onSuccess = onSuccess,
                    onError = onError
                )
            } else {
                onError()
            }
        }
    }

    fun updateList(context: Context) {
        _uiState.update { it.copy(mediaPath = PrefManager.mediaPath) }

        val items = mutableListOf<PlaylistItem>()
        val browserItem = PlaylistItem(
            type = PlaylistItem.TYPE_SPECIAL,
            name = "File browser",
            comment = "Files in ${uiState.value.mediaPath}"
        )
        items.add(browserItem)

        PlaylistUtils.listNoSuffix().forEachIndexed { index, name ->
            val item = PlaylistItem(
                PlaylistItem.TYPE_PLAYLIST,
                name,
                Playlist.readComment(context, name)
            )
            item.id = index + 1

            items.add(item)
        }

        _uiState.update { it.copy(playlistItems = items) }
    }

    fun editComment(oldName: String, newComment: String, onError: () -> Unit) {
        val value = newComment.replace("\n", " ")
        val file = File(PrefManager.DATA_DIR, oldName + Playlist.COMMENT_SUFFIX)
        try {
            file.delete()
            file.createNewFile()
            FileUtils.writeToFile(file, value)
        } catch (e: IOException) {
            onError()
        }
    }

    fun editPlaylist(oldName: String, newName: String, onError: () -> Unit) {
        if (!Playlist.rename(oldName, newName)) {
            onError()
        }
    }
}
