package org.helllabs.android.xmp.compose.ui.playlist

import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.helllabs.android.xmp.core.PlaylistManager
import org.helllabs.android.xmp.model.Playlist
import timber.log.Timber

@Stable
class PlaylistActivityViewModel : ViewModel() {

    private val manager: MutableStateFlow<PlaylistManager> = MutableStateFlow(PlaylistManager())

    private val _uiState = MutableStateFlow(Playlist())
    val uiState = _uiState.asStateFlow()

    fun save() {
        with(manager.value) {
            setLoop(_uiState.value.isLoop)
            setShuffle(_uiState.value.isShuffle)
            setList(_uiState.value.list)
            save()
        }
    }

    fun setShuffle(value: Boolean) {
        _uiState.update { it.copy(isShuffle = value) }
    }

    fun setLoop(value: Boolean) {
        _uiState.update { it.copy(isLoop = value) }
    }

    fun onMove(from: Int, to: Int) {
        val list = _uiState.value.list.toMutableList().apply {
            add(to, removeAt(from))
        }

        _uiState.update { it.copy(list = list) }
    }

    fun onDragStopped() {
        Timber.d("Drag stopped")
        save()
    }

    fun getUriItems(): List<Uri> = _uiState.value.list.map { it.uri }

    fun onRefresh(name: String) {
        manager.value = PlaylistManager()
        with(manager.value) {
            load(Uri.parse(name))

            playlist.list.forEachIndexed { index, playlistItem ->
                playlistItem.id = index
            }

            _uiState.update {
                it.copy(
                    comment = playlist.comment,
                    isLoop = playlist.isLoop,
                    isShuffle = playlist.isShuffle,
                    list = playlist.list,
                    name = playlist.name,
                    uri = playlist.uri
                )
            }
        }
    }

    fun removeItem(index: Int) {
        val list = _uiState.value.list.toMutableList()
        list.removeAt(index)

        _uiState.update { it.copy(list = list) }
        save() // Save just in-case
    }
}
