package org.helllabs.android.xmp.compose.ui.menu

import android.content.Context
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.helllabs.android.xmp.StorageManager
import org.helllabs.android.xmp.core.PlaylistUtils
import org.helllabs.android.xmp.model.Playlist
import org.helllabs.android.xmp.model.Playlist.Companion.PLAYLIST_SUFFIX
import org.helllabs.android.xmp.model.PlaylistItem
import timber.log.Timber

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
        if (isFatal) {
            Timber.e(message)
        } else {
            Timber.w(message)
        }
        _uiState.update { it.copy(errorText = message, isFatalError = isFatal) }
    }

    // Create application directory and populate with empty playlist
    fun setupDataDir(
        context: Context,
        name: String,
        comment: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val playlistsDir = StorageManager.getPlaylistDirectory(context)
        if (playlistsDir == null || playlistsDir.isFile) {
            onError("setupDataDir: Playlist Directory returned null or is file!")
            return
        }

        val isPlaylistEmpty = playlistsDir.listFiles().isEmpty()
        if (isPlaylistEmpty) {
            val playlist = playlistsDir.findFile(name + PLAYLIST_SUFFIX)
            if (playlist != null && playlist.exists()) {
                Timber.w("Attempted to create another empty playlist")
                onSuccess()
                return
            }

            PlaylistUtils.createEmptyPlaylist2(
                context = context,
                playlistsDir = playlistsDir.uri,
                newName = name,
                newComment = comment,
                onSuccess = onSuccess,
                onError = onError
            )
        }
    }

    fun setDefaultPath(context: Context) {
        StorageManager.getDefaultPathName(
            context = context,
            onSuccess = { name ->
                _uiState.update { it.copy(mediaPath = name) }
                updateList(context)
            },
            onError = { error ->
                showError(error, false)
                _uiState.update { it.copy(mediaPath = "") }
            }
        )
    }

    fun updateList(context: Context) {
        val items = mutableListOf<PlaylistItem>()
        val browserItem = PlaylistItem(
            type = PlaylistItem.TYPE_SPECIAL,
            name = "File browser",
            comment = "Files in ${uiState.value.mediaPath}"
        )
        items.add(browserItem)

        PlaylistUtils.listNoSuffix2(context).forEachIndexed { index, name ->
            Playlist.readComment2(
                context = context,
                comment = name,
                onSuccess = {
                    val item = PlaylistItem(PlaylistItem.TYPE_PLAYLIST, name, it)
                    item.id = index + 1

                    items.add(item)
                },
                onError = { error ->
                    _uiState.update {
                        it.copy(errorText = error)
                    }
                }
            )
        }

        _uiState.update { it.copy(playlistItems = items) }
    }
}
