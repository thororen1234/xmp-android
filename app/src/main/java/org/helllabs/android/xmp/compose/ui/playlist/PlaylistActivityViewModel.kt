package org.helllabs.android.xmp.compose.ui.playlist

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.helllabs.android.xmp.core.PlaylistManager
import org.helllabs.android.xmp.model.PlaylistItem

class PlaylistActivityViewModel : ViewModel() {

    data class PlaylistState(
        val manager: PlaylistManager = PlaylistManager(),
        val useFileName: Boolean = false,
        val isLoop: Boolean = false,
        val isShuffle: Boolean = false
    )

    private val _uiState = MutableStateFlow(PlaylistState())
    val uiState = _uiState.asStateFlow()

    fun setShuffle(value: Boolean) {
        _uiState.value.manager.setShuffle(value)
        _uiState.update { it.copy(isShuffle = value) }
    }

    fun setLoop(value: Boolean) {
        _uiState.value.manager.setLoop(value)
        _uiState.update { it.copy(isLoop = value) }
    }

    fun onDragEnd(list: List<PlaylistItem>) {
        _uiState.value.manager.setList(list)
    }

    fun getUriItems(): List<Uri> = _uiState.value.manager.playlist.list.map { it.uri!! }

    fun onRefresh(name: String) {
        _uiState.value.manager.load(Uri.parse(name))

        _uiState.update {
            it.copy(
                isLoop = _uiState.value.manager.playlist.isLoop,
                isShuffle = _uiState.value.manager.playlist.isShuffle
            )
        }
    }

    fun removeItem(index: Int) {
        val list = _uiState.value.manager.playlist.list.toMutableList()
        list.removeAt(index)

        _uiState.value.manager.setList(list)
    }
}
