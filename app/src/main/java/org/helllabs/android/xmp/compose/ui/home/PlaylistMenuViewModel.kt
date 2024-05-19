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
import org.helllabs.android.xmp.model.FileItem
import timber.log.Timber

class PlaylistMenuViewModel : ViewModel() {
    data class PlaylistMenuState(
        val errorText: String = "",
        val isFatalError: Boolean = false,
        val isLoading: Boolean = true,
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
}