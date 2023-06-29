package org.helllabs.android.xmp.compose.ui.filelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.browser.playlist.PlaylistItem
import org.helllabs.android.xmp.browser.playlist.PlaylistUtils
import org.helllabs.android.xmp.util.InfoCache
import timber.log.Timber
import java.io.File
import java.text.DateFormat

class FileListViewModel : ViewModel() {

    // Bread crumbs are the back bone of the file explorer. :)
    data class BreadCrumb(
        val name: String,
        val path: String,
        val enabled: Boolean = false
    )

    // State class for UI related stuff
    data class FileListState(
        val crumbs: List<BreadCrumb> = listOf(),
        val error: String? = null,
        val isLoading: Boolean = false,
        val isLoop: Boolean = false,
        val isShuffle: Boolean = false,
        val lastPath: String? = null,
        val list: List<PlaylistItem> = listOf(),
        val pathNotFound: Boolean = false
    )

    private val _uiState = MutableStateFlow(FileListState())
    val uiState = _uiState.asStateFlow()

    val currentPath: String
        get() {
            val crumbs = uiState.value.crumbs
            return if (crumbs.isEmpty()) "" else crumbs.last().path
        }

    fun init() {
        _uiState.update {
            it.copy(
                isShuffle = PrefManager.shuffleMode,
                isLoop = PrefManager.loopMode
            )
        }

        val initialPath = File(PrefManager.mediaPath)
        onNavigate(initialPath)
    }

    /**
     * Handle back presses
     * @return *true* if successful, otherwise false
     */
    fun onBackPressed(): Boolean {
        val popCrumb = _uiState.value.crumbs.dropLast(1).lastOrNull()

        popCrumb?.let {
            if (!popCrumb.enabled) {
                return false
            }

            val file = File(it.path)
            onNavigate(file)
        }

        return popCrumb != null
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
        if (currentPath.isNotEmpty()) {
            val file = File(currentPath)
            onNavigate(file)
        }
    }

    fun onRestore() {
        val file = _uiState.value.lastPath?.let { File(it) } ?: return
        onNavigate(file)
    }

    fun onNavigate(modDir: File) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Snapshot our last known path.
            if (currentPath.isNotEmpty()) {
                val checkPath = File(currentPath).list()?.isNotEmpty() ?: false
                if (checkPath) {
                    _uiState.update { it.copy(lastPath = currentPath) }
                }
            }

            Timber.d("File: ${modDir.path}")
            if (!modDir.exists()) {
                _uiState.update { it.copy(pathNotFound = true, isLoading = false) }
            }

            // Rebuild our bread crumbs
            val crumbParts = modDir.path.split("/")
            var currentCrumbPath = ""
            val crumbs = crumbParts.filter { it.isNotEmpty() }.map { crumb ->
                currentCrumbPath += "/$crumb"
                BreadCrumb(
                    name = crumb,
                    path = currentCrumbPath,
                    enabled = File(currentCrumbPath).canRead()
                )
            }
            _uiState.update { it.copy(crumbs = crumbs) }

            val list = modDir.listFiles()?.map { file ->
                val item = if (file.isDirectory) {
                    PlaylistItem(
                        type = PlaylistItem.TYPE_DIRECTORY,
                        name = file.name,
                        comment = ""
                    )
                } else {
                    val date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM)
                        .format(file.lastModified())

                    PlaylistItem(
                        type = PlaylistItem.TYPE_FILE,
                        name = file.name,
                        comment = "$date (${file.length() / 1024} kB)"
                    )
                }
                item.file = file
                item
            }?.sorted() ?: mutableListOf()

            PlaylistUtils.renumberIds(list)

            _uiState.update { it.copy(list = list, isLoading = false) }
        }
    }

    fun showPathNotFound(value: Boolean) {
        _uiState.update { it.copy(pathNotFound = value) }
    }

    fun getFilenameList(): List<String> =
        _uiState.value.list.filter { it.type == PlaylistItem.TYPE_FILE }.map { it.file!!.path }

    fun getDirectoryCount(): Int =
        _uiState.value.list.takeWhile { it.type == PlaylistItem.TYPE_DIRECTORY }.count()

    fun getItems(): List<PlaylistItem> = _uiState.value.list

    fun clearCachedEntries() {
        InfoCache.clearCache(getFilenameList())
    }
}
