package org.helllabs.android.xmp.compose.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.core.PlaylistManager
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.core.StorageManager
import org.helllabs.android.xmp.core.XmpException
import org.helllabs.android.xmp.model.FileItem

class PlaylistMenuViewModel : ViewModel() {
    data class PlaylistMenuState(
        val errorText: String? = null,
        val isLoading: Boolean = true,
        val mediaPath: String = "",
        val playlistItems: List<FileItem> = listOf(),
        val editPlaylist: FileItem? = null,
        val newPlaylist: Boolean = false
    )

    private val _uiState = MutableStateFlow(PlaylistMenuState())
    val uiState = _uiState.asStateFlow()

    fun showError(message: String) {
        _uiState.update { it.copy(errorText = message) }
    }

    // Create application directory and populate with empty playlist
    fun setupDataDir(name: String, comment: String): Result<Unit> {
        return StorageManager.getPlaylistDirectory().mapCatching { dir ->
            if (dir.isFile()) {
                throw XmpException("Playlist Directory returned null or is file!")
            }

            if (PrefManager.installedExamplePlaylist) {
                return@mapCatching
            }

            val isPlaylistEmpty = dir.listFiles().isEmpty()
            if (isPlaylistEmpty) {
                val res = PlaylistManager().run {
                    new(name, comment)
                }

                if (res.isSuccess) {
                    PrefManager.installedExamplePlaylist = true
                } else {
                    throw XmpException("Unable to create Example playlist")
                }
            }
        }
    }

    fun setDefaultPath() {
        StorageManager.getDefaultPathName().onSuccess { name ->
            _uiState.update { it.copy(mediaPath = name) }
            updateList()
        }.onFailure { err ->
            showError(err.message ?: "Error setting default path")
            _uiState.update { it.copy(mediaPath = "") }
        }
    }

    fun updateList() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            if (uiState.value.mediaPath.isEmpty()) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            val items = mutableListOf<FileItem>()
            FileItem(
                name = "File browser",
                comment = "Files in ${uiState.value.mediaPath}",
                isSpecial = true,
                docFile = null
            ).also(items::add)

            PlaylistManager.listPlaylistsDF().forEach {
                val playlist = PlaylistManager()
                if (!playlist.load(it.uri)) return@forEach

                FileItem(
                    name = playlist.playlist.name,
                    comment = playlist.playlist.comment,
                    docFile = it
                ).also(items::add)
            }

            _uiState.update { it.copy(playlistItems = items.sorted(), isLoading = false) }
        }
    }

    fun editPlaylist(item: FileItem?) {
        _uiState.update { it.copy(editPlaylist = item) }

        if (item == null) {
            updateList()
        }
    }

    fun newPlaylist(show: Boolean) {
        _uiState.update { it.copy(newPlaylist = show) }
    }
}
