package org.helllabs.android.xmp.compose.ui.playlist

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.core.PlaylistUtils
import org.helllabs.android.xmp.model.Playlist
import org.helllabs.android.xmp.model.PlaylistItem
import timber.log.Timber
import java.io.IOException

class PlaylistActivityViewModel : ViewModel() {
    data class PlaylistState(
        val playlist: Playlist? = null,
        val useFileName: Boolean = false,
        val isLoop: Boolean = false,
        val isShuffle: Boolean = false
    )

    private val _uiState = MutableStateFlow(PlaylistState())
    val uiState = _uiState.asStateFlow()

    fun setShuffle(value: Boolean) {
        _uiState.value.playlist?.isShuffleMode = value
        _uiState.update { it.copy(isShuffle = value) }
        savePlaylist()
    }

    fun setLoop(value: Boolean) {
        _uiState.value.playlist?.isLoopMode = value
        _uiState.update { it.copy(isLoop = value) }
        savePlaylist()
    }

    fun savePlaylist() {
        uiState.value.playlist?.commit()
    }

    fun saveList(newList: List<PlaylistItem>) {
        _uiState.value.playlist?.apply {
            setNewList(newList)
            setListChanged(true)
            commit()
        }
    }

    fun getItems(): List<PlaylistItem> = _uiState.value.playlist?.list.orEmpty()

    fun getDirectoryCount(): Int =
        _uiState.value.playlist?.list?.takeWhile { it.isDirectory }?.count() ?: 0

    fun getFilenameList(): List<String> =
        _uiState.value.playlist?.list?.filter { it.isFile }?.map { it.file!!.path }.orEmpty()

    fun onRefresh(name: String) {
        try {
            val playlist = Playlist(name)
            PlaylistUtils.renumberIds(playlist.list)
            _uiState.update {
                it.copy(
                    playlist = playlist,
                    useFileName = PrefManager.useFileName,
                    isLoop = playlist.isLoopMode,
                    isShuffle = playlist.isShuffleMode
                )
            }
        } catch (e: IOException) {
            Timber.e("Can't read playlist $name")
        }
    }

    fun removeItem(index: Int) {
        _uiState.value.playlist?.apply {
            remove(index)
            commit()
        }
    }
}
