package org.helllabs.android.xmp.compose.ui.filelist

import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazygeniouz.dfc.file.DocumentFileCompat
import java.text.DateFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.core.PlaylistManager
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.core.StorageManager
import org.helllabs.android.xmp.model.FileItem
import org.helllabs.android.xmp.model.ModInfo
import org.helllabs.android.xmp.model.Playlist
import org.helllabs.android.xmp.model.PlaylistItem
import timber.log.Timber

class FileListViewModel : ViewModel() {

    // Bread crumbs are the back bone of the file explorer. :)
    data class BreadCrumb(
        val name: String,
        val path: DocumentFileCompat?,
        val enabled: Boolean = false
    )

    // State class for UI related stuff
    data class FileListState(
        val crumbs: List<BreadCrumb> = listOf(),
        val isLoading: Boolean = false,
        val isLoop: Boolean = false,
        val isShuffle: Boolean = false,
        val lastScrollPosition: Int = 0,
        val list: List<FileItem> = listOf()
    )

    private val _uiState = MutableStateFlow(FileListState())
    val uiState = _uiState.asStateFlow()

    private val _softError = MutableSharedFlow<String>()
    val softError = _softError.asSharedFlow()

    val currentPath: DocumentFileCompat?
        get() {
            val crumbs = uiState.value.crumbs
            return if (crumbs.isEmpty()) null else crumbs.last().path
        }

    var playlistList: List<Playlist> by mutableStateOf(listOf())
    var playlistChoice: DocumentFileCompat? by mutableStateOf(null)
    var deleteDirChoice: DocumentFileCompat? by mutableStateOf(null)
    var deleteFileChoice: DocumentFileCompat? by mutableStateOf(null)

    init {
        _uiState.update {
            it.copy(
                isShuffle = PrefManager.shuffleMode,
                isLoop = PrefManager.loopMode
            )
        }

        val init = StorageManager.getModDirectory()
        Timber.d("Initial Path: ${init!!.uri}")
        onNavigate(init)
    }

    fun setScrollPosition(value: Int) {
        _uiState.update { it.copy(lastScrollPosition = value) }
    }

    /**
     * Handle back presses
     * @return *true* if successful, otherwise false
     */
    fun onBackPressed(): Boolean {
        val currentCrumb = _uiState.value.crumbs.last().path

        currentCrumb?.parentFile?.let {
            onNavigate(it)
            return true
        }

        return false
    }

    /**
     * Play all valid files
     */
    suspend fun onAllFiles(): List<Uri> {
        return withContext(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }

            val list = StorageManager
                .walkDownDirectory(currentPath!!.uri, false)
            // .filter(Xmp::testFromFd)

            _uiState.update { it.copy(isLoading = false) }

            list
        }
    }

    fun onLoop(value: Boolean) {
        PrefManager.loopMode = value
        _uiState.update { it.copy(isLoop = value) }
    }

    fun onShuffle(value: Boolean) {
        PrefManager.shuffleMode = value
        _uiState.update { it.copy(isShuffle = value) }
    }

    fun onRefresh() {
        onNavigate(currentPath)
    }

    fun onRestore() {
        onNavigate(uiState.value.crumbs.last().path!!.parentFile)
    }

    fun onNavigate(modDir: DocumentFileCompat?) {
        if (modDir == null) {
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            Timber.d("Path: ${modDir.uri}")

            // Rebuild our bread crumbs
            val crumbs = mutableListOf<BreadCrumb>()
            var docFile: DocumentFileCompat? = modDir
            while (docFile != null) {
                val crumb = BreadCrumb(
                    name = docFile.name,
                    path = docFile,
                    enabled = docFile.canRead()
                )

                crumbs.add(crumb)
                docFile = docFile.parentFile
            }
            _uiState.update { it.copy(crumbs = crumbs.reversed()) }

            val list = modDir.listFiles().map { file ->
                val item = if (file.isDirectory()) {
                    FileItem(
                        name = file.name,
                        comment = "",
                        docFile = file
                    )
                } else {
                    val date = DateFormat
                        .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM)
                        .format(file.lastModified)

                    FileItem(
                        name = file.name,
                        comment = "$date (${file.length / 1024} kB)",
                        docFile = file
                    )
                }

                item
            }.sorted()

            _uiState.update { it.copy(list = list, isLoading = false, lastScrollPosition = 0) }
        }
    }

    fun addToPlaylist(choice: Playlist) {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch(Dispatchers.IO) {
            if (playlistChoice == null) {
                _softError.emit("Playlist choice is null")
                _uiState.update { it.copy(isLoading = false) }
                playlistChoice = null
                return@launch
            }

            val manager = PlaylistManager()
            if (!manager.load(choice.uri)) {
                _softError.emit("Playlist manager failed to load playlist")
                _uiState.update { it.copy(isLoading = false) }
                playlistChoice = null
                return@launch
            }

            val modInfo = ModInfo()
            if (playlistChoice!!.isFile()) {
                if (!Xmp.testFromFd(playlistChoice!!.uri, modInfo)) {
                    _softError.emit("Failed to validate file")
                    _uiState.update { it.copy(isLoading = false) }
                    playlistChoice = null
                    return@launch
                }

                val playlist = PlaylistItem(
                    name = modInfo.name,
                    type = modInfo.type,
                    uri = playlistChoice!!.uri
                )
                val list = listOf(playlist)
                val res = manager.add(list)

                if (!res) {
                    _softError.emit("Couldn't add module to playlist")
                }
            } else if (playlistChoice!!.isDirectory()) {
                val list = mutableListOf<PlaylistItem>()
                StorageManager.walkDownDirectory(playlistChoice!!.uri, false).forEach { uri ->
                    if (!Xmp.testFromFd(uri, modInfo)) {
                        Timber.w("Invalid playlist item $uri")
                        return@forEach
                    }

                    val playlist = PlaylistItem(
                        name = modInfo.name.ifEmpty {
                            StorageManager.getFileName(uri)
                        } ?: "",
                        type = modInfo.type,
                        uri = uri
                    )

                    list.add(playlist)
                }

                if (list.isEmpty()) {
                    _softError.emit("Empty directory")
                    _uiState.update { it.copy(isLoading = false) }
                    playlistChoice = null
                    return@launch
                }

                val res = manager.add(list)
                if (!res) {
                    _softError.emit("Couldn't add modules to playlist")
                }
            }

            _uiState.update { it.copy(isLoading = false) }
            playlistChoice = null
        }
    }

    fun getItems(): List<Uri> = _uiState.value.list.map { it.docFile!!.uri }
}
