package org.helllabs.android.xmp.compose.ui.menu

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.helllabs.android.xmp.core.PlaylistManager
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.core.StorageManager
import org.helllabs.android.xmp.model.FileItem
import timber.log.Timber

class PlaylistMenuViewModel : ViewModel() {
    data class PlaylistMenuState(
        val isRefreshing: Boolean = false,
        val errorText: String = "",
        val isFatalError: Boolean = false,
        val mediaPath: String = "",
        val playlistItems: List<FileItem> = listOf()
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
        name: String,
        comment: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val playlistsDir = StorageManager.getPlaylistDirectory()
        if (playlistsDir == null || playlistsDir.isFile()) {
            onError("setupDataDir: Playlist Directory returned null or is file!")
            return
        }

        if (PrefManager.installedExamplePlaylist) {
            onSuccess()
            return
        }

        val isPlaylistEmpty = playlistsDir.listFiles().isEmpty()
        if (isPlaylistEmpty) {
            val res = PlaylistManager().run {
                new(name, comment)
            }

            if (res) {
                PrefManager.installedExamplePlaylist = true
                onSuccess()
            } else {
                onError("Unable to create Example playlist")
            }
        }
    }

    fun setDefaultPath() {
        StorageManager.getDefaultPathName(
            onSuccess = { name ->
                _uiState.update { it.copy(mediaPath = name) }
                updateList()
            },
            onError = { error ->
                showError(error, false)
                _uiState.update { it.copy(mediaPath = "") }
            }
        )
    }

    fun updateList() {
        if (uiState.value.mediaPath.isEmpty()) {
            return
        }

        val items = mutableListOf<FileItem>()
        val browserItem = FileItem(
            name = "File browser",
            comment = "Files in ${uiState.value.mediaPath}",
            isSpecial = true,
            docFile = null
        )
        items.add(browserItem)

        PlaylistManager.listPlaylistsDF().forEach {
            val playlist = PlaylistManager()
            playlist.load(it.uri)

            FileItem(
                name = playlist.playlist.name,
                comment = playlist.playlist.comment,
                docFile = it
            ).also(items::add)
        }

        _uiState.update { it.copy(playlistItems = items.sorted()) }
    }
}
