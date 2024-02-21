package org.helllabs.android.xmp.compose.ui.playlist

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.helllabs.android.xmp.core.PlaylistManager
import org.helllabs.android.xmp.model.Playlist
import org.helllabs.android.xmp.model.PlaylistItem

class PlaylistActivityViewModel : ViewModel() {

    private var manager: PlaylistManager = PlaylistManager()

    private val _uiState = MutableStateFlow(Playlist())
    val uiState = _uiState.asStateFlow()

    fun save() {
        with(manager) {
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

    fun onDragEnd(list: List<PlaylistItem>) {
        _uiState.update { it.copy(list = list) }
        save() // Save just in-case
    }

    fun getUriItems(): List<Uri> = _uiState.value.list.map { it.uri }

    fun onRefresh(name: String) {
        manager = PlaylistManager()
        manager.load(Uri.parse(name))

        _uiState.update {
            it.copy(
                comment = manager.playlist.comment,
                isLoop = manager.playlist.isLoop,
                isShuffle = manager.playlist.isShuffle,
                list = manager.playlist.list,
                name = manager.playlist.name,
                uri = manager.playlist.uri
            )
        }
    }

    fun removeItem(index: Int) {
        val list = _uiState.value.list.toMutableList()
        list.removeAt(index)

        _uiState.update { it.copy(list = list) }
        save() // Save just in-case
    }
}
