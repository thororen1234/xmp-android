package org.helllabs.android.xmp.browser

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.saket.cascade.CascadeDropdownMenu
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.browser.playlist.PlaylistItem
import org.helllabs.android.xmp.browser.playlist.PlaylistUtils
import org.helllabs.android.xmp.compose.components.BottomBarButtons
import org.helllabs.android.xmp.compose.components.ErrorScreen
import org.helllabs.android.xmp.compose.components.ProgressbarIndicator
import org.helllabs.android.xmp.compose.components.XmpDropdownMenuHeader
import org.helllabs.android.xmp.compose.components.XmpTopBar
import org.helllabs.android.xmp.compose.components.pullrefresh.ExperimentalMaterialApi
import org.helllabs.android.xmp.compose.components.pullrefresh.PullRefreshIndicator
import org.helllabs.android.xmp.compose.components.pullrefresh.pullRefresh
import org.helllabs.android.xmp.compose.components.pullrefresh.rememberPullRefreshState
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.core.Assets
import org.helllabs.android.xmp.core.Files
import org.helllabs.android.xmp.model.DropDownItem
import org.helllabs.android.xmp.util.InfoCache.clearCache
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.text.DateFormat
import kotlin.time.Duration.Companion.seconds

// TODO: Seriously need to separate classes, and hoist composables!

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
        getFilenameList().forEach { clearCache(it) }
    }
}

class FileListActivity : BasePlaylistActivity() {

    private val viewModel by viewModels<FileListViewModel>()

    override val allFiles: List<String>
        get() = Files.recursiveList(viewModel.currentPath)

    override val isShuffleMode: Boolean
        get() = viewModel.uiState.value.isShuffle

    override val isLoopMode: Boolean
        get() = viewModel.uiState.value.isLoop

    /**
     * Recursively add current directory to playlist
     */
    private val addCurrentRecursiveChoice: PlaylistChoice = object : PlaylistChoice {
        override fun execute(fileSelection: Int, playlistSelection: Int) {
            PlaylistUtils.filesToPlaylist(
                this@FileListActivity,
                Files.recursiveList(viewModel.currentPath),
                PlaylistUtils.getPlaylistName(playlistSelection)
            )
        }
    }

    /**
     * Recursively add directory to playlist
     */
    private val addRecursiveToPlaylistChoice: PlaylistChoice = object : PlaylistChoice {
        override fun execute(fileSelection: Int, playlistSelection: Int) {
            PlaylistUtils.filesToPlaylist(
                this@FileListActivity,
                Files.recursiveList(viewModel.uiState.value.list[fileSelection].file),
                PlaylistUtils.getPlaylistName(playlistSelection)
            )
        }
    }

    /**
     * Add one file to playlist
     */
    private val addFileToPlaylistChoice: PlaylistChoice = object : PlaylistChoice {
        override fun execute(fileSelection: Int, playlistSelection: Int) {
            PlaylistUtils.filesToPlaylist(
                this@FileListActivity,
                viewModel.uiState.value.list[fileSelection].file?.path.orEmpty(),
                PlaylistUtils.getPlaylistName(playlistSelection)
            )
        }
    }

    /**
     * Add file list to playlist
     */
    private val addFileListToPlaylistChoice: PlaylistChoice = object : PlaylistChoice {
        override fun execute(fileSelection: Int, playlistSelection: Int) {
            PlaylistUtils.filesToPlaylist(
                this@FileListActivity,
                viewModel.uiState.value.list.filter { it.type == PlaylistItem.TYPE_FILE }
                    .mapNotNull { it.file?.path },
                PlaylistUtils.getPlaylistName(playlistSelection)
            )
        }
    }

    /**
     * For actions based on playlist selection made using choosePlaylist()
     */
    interface PlaylistChoice {
        fun execute(fileSelection: Int, playlistSelection: Int)
    }

    data class PlaylistChoiceData(
        val fileSelection: Int,
        val playlistChoice: PlaylistChoice
    )

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")

        WindowCompat.setDecorFitsSystemWindows(window, false)

        // On back pressed handler
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (PrefManager.backButtonNavigation) {
                    if (!viewModel.onBackPressed()) {
                        this.remove()
                        onBackPressedDispatcher.onBackPressed()
                    }
                } else {
                    this.remove()
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }

        // Init our file manager
        viewModel.init()

        setContent {
            val haptic = LocalHapticFeedback.current
            val scope = rememberCoroutineScope()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            // Set up and override on back pressed.
            DisposableEffect(onBackPressedDispatcher) {
                onBackPressedDispatcher.addCallback(callback)
                onDispose {
                    callback.remove()
                }
            }

            XmpTheme {
                if (state.pathNotFound) {
                    AlertDialog(
                        onDismissRequest = { viewModel.showPathNotFound(false) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null
                            )
                        },
                        title = { Text(text = stringResource(id = R.string.file_no_path_title)) },
                        text = {
                            Text(
                                text = stringResource(
                                    id = R.string.file_no_path_text,
                                    viewModel.currentPath
                                )
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    try {
                                        Assets.install(
                                            this@FileListActivity,
                                            viewModel.currentPath,
                                            PrefManager.examples
                                        )
                                        viewModel.onRefresh()
                                    } catch (e: IOException) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "Error creating directory ${viewModel.currentPath}.",
                                                actionLabel = getString(R.string.ok)
                                            )

                                            finish()
                                        }
                                    }
                                    viewModel.showPathNotFound(false)
                                }
                            ) {
                                Text(text = stringResource(id = R.string.create))
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    viewModel.showPathNotFound(false)
                                    finish()
                                }
                            ) {
                                Text(text = stringResource(id = R.string.cancel))
                            }
                        }
                    )
                }

                // TODO meh
                var playlistChoiceState: PlaylistChoiceData? by remember { mutableStateOf(null) }
                if (playlistChoiceState != null) {
                    // Return if no playlists exist
                    if (PlaylistUtils.list().isEmpty()) {
                        LaunchedEffect(playlistChoiceState) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = getString(R.string.msg_no_playlists)
                                )
                            }
                        }
                        playlistChoiceState = null
                    } else {
                        var selection by remember { mutableIntStateOf(0) }
                        AlertDialog(
                            onDismissRequest = { playlistChoiceState = null },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.PlaylistAdd,
                                    contentDescription = null
                                )
                            },
                            title = {
                                Text(text = stringResource(id = R.string.msg_select_playlist))
                            },
                            text = {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    PlaylistUtils.listNoSuffix().forEachIndexed { index, text ->
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .selectable(
                                                    selected = (index == selection),
                                                    onClick = { selection = index }
                                                )
                                                .padding(vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = (index == selection),
                                                onClick = null
                                            )
                                            Text(text = text)
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        with(playlistChoiceState!!) {
                                            playlistChoice.execute(
                                                fileSelection,
                                                selection
                                            )
                                        }
                                        playlistChoiceState = null
                                    }
                                ) {
                                    Text(text = stringResource(id = R.string.ok))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { playlistChoiceState = null }) {
                                    Text(text = stringResource(id = R.string.cancel))
                                }
                            }
                        )
                    }
                }

                FileListScreen(
                    state = state,
                    snackbarHostState = snackbarHostState,
                    onBack = {
                        callback.remove()
                        onBackPressedDispatcher.onBackPressed()
                    },
                    onRefresh = viewModel::onRefresh,
                    onRestore = viewModel::onRestore,
                    onShuffle = viewModel::onShuffle,
                    onLoop = viewModel::onLoop,
                    onPlayAll = ::onPlayAll,
                    onCrumbMenu = { index ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        when (index) {
                            0 ->
                                playlistChoiceState =
                                    PlaylistChoiceData(0, addFileListToPlaylistChoice)
                            1 ->
                                playlistChoiceState =
                                    PlaylistChoiceData(0, addCurrentRecursiveChoice)
                            2 -> addToQueue(viewModel.getFilenameList())
                            3 -> {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Set as default module path"
                                    )
                                }
                                PrefManager.mediaPath = viewModel.currentPath
                            }
                            4 -> viewModel.clearCachedEntries()
                        }
                    },
                    onCrumbClick = { crumb, _ ->
                        val file = File(crumb.path)
                        viewModel.onNavigate(file)
                    },
                    onItemClick = { item, index ->
                        if (item.file?.isDirectory == true) {
                            viewModel.onNavigate(item.file!!)
                        } else {
                            onItemClick(
                                viewModel.getItems(),
                                viewModel.getFilenameList(),
                                viewModel.getDirectoryCount(),
                                index
                            )
                        }
                    },
                    onItemLongClick = { item, index ->
                        // TODO
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                )
            }
        }
    }

    override fun update() {
        viewModel.onRefresh()
    }

//    private fun deleteDirectory(position: Int) {
//        val deleteName = mPlaylistAdapter.getFilename(position)
//        val mediaPath = PrefManager.mediaPath
//        if (deleteName.startsWith(mediaPath) && deleteName != mediaPath) {
//            yesNoDialog(
//                this,
//                "Delete directory",
//                "Are you sure you want to delete directory \"" +
//                    basename(deleteName) + "\" and all its contents?"
//            ) {
//                if (deleteRecursive(deleteName)) {
//                    updateModlist()
//                    toast(this@FileListActivity, getString(R.string.msg_dir_deleted))
//                } else {
//                    toast(this@FileListActivity, getString(R.string.msg_cant_delete_dir))
//                }
//            }
//        } else {
//            toast(this, R.string.error_dir_not_under_moddir)
//        }
//    }
//
//    // Playlist context menu
//    override fun onCreateContextMenu(menu: ContextMenu?, view: View?, menuInfo: ContextMenuInfo?) {
//        super.onCreateContextMenu(menu, view, menuInfo)
//        val position = mPlaylistAdapter.position
//        if (mPlaylistAdapter.getFile(position)!!.isDirectory) { // For directory
//            menu.setHeaderTitle("This directory")
//            menu.add(Menu.NONE, 0, 0, "Add to playlist")
//            menu.add(Menu.NONE, 1, 1, "Add to play queue")
//            menu.add(Menu.NONE, 2, 2, "Play contents")
//            menu.add(Menu.NONE, 3, 3, "Delete directory")
//        } else { // For files
//            val mode = PrefManager.playlistMode
//            menu.setHeaderTitle("This file")
//            menu.add(Menu.NONE, 0, 0, "Add to playlist")
//            if (mode != 3) {
//                menu.add(Menu.NONE, 1, 1, "Add to play queue")
//            }
//            if (mode != 2) {
//                menu.add(Menu.NONE, 2, 2, "Play this file")
//            }
//            if (mode != 1) {
//                menu.add(Menu.NONE, 3, 3, "Play all starting here")
//            }
//            menu.add(Menu.NONE, 4, 4, "Delete file")
//        }
//    }
//
//    override fun onContextItemSelected(item: MenuItem): Boolean {
//        val id = item.itemId
//        val position = mPlaylistAdapter.position
//        if (mPlaylistAdapter.getFile(position)!!.isDirectory) { // Directories
//            when (id) {
//                0 -> choosePlaylist(position, addRecursiveToPlaylistChoice)
//                1 -> addToQueue(recursiveList(mPlaylistAdapter.getFile(position)))
//                2 -> playModule(recursiveList(mPlaylistAdapter.getFile(position)))
//                3 -> deleteDirectory(position)
//            }
//        } else { // Files
//            when (id) {
//                0 -> choosePlaylist(position, addFileToPlaylistChoice)
//                1 -> addToQueue(mPlaylistAdapter.getFilename(position))
//                2 -> playModule(mPlaylistAdapter.getFilename(position))
//                3 -> playModule(mPlaylistAdapter.filenameList, position)
//                4 -> {
//                    val deleteName = mPlaylistAdapter.getFilename(position)
//                    yesNoDialog(
//                        this,
//                        "Delete",
//                        "Are you sure you want to delete " + basename(deleteName) + "?"
//                    ) {
//                        if (delete(deleteName)) {
//                            updateModlist()
//                            toast(this@FileListActivity, getString(R.string.msg_file_deleted))
//                        } else {
//                            toast(this@FileListActivity, getString(R.string.msg_cant_delete))
//                        }
//                    }
//                }
//            }
//        }
//        return true
//    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
private fun FileListScreen(
    state: FileListViewModel.FileListState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onRestore: () -> Unit,
    onShuffle: (value: Boolean) -> Unit,
    onLoop: (value: Boolean) -> Unit,
    onPlayAll: () -> Unit,
    onCrumbMenu: (index: Int) -> Unit,
    onCrumbClick: (crumb: FileListViewModel.BreadCrumb, index: Int) -> Unit,
    onItemClick: (item: PlaylistItem, index: Int) -> Unit,
    onItemLongClick: (item: PlaylistItem, index: Int) -> Unit
) {
    val scrollState = rememberLazyListState()
    val crumbScrollState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex > 0
        }
    }
    Scaffold(
        topBar = {
            XmpTopBar(
                title = stringResource(id = R.string.browser_filelist_title),
                isScrolled = isScrolled,
                onBack = onBack
            )
        },
        bottomBar = {
            BottomBarButtons(
                isShuffle = state.isShuffle,
                isLoop = state.isLoop,
                onShuffle = onShuffle,
                onLoop = onLoop,
                onPlayAll = onPlayAll
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        val scope = rememberCoroutineScope()
        var refreshing by remember { mutableStateOf(false) }
        fun refresh() = scope.launch {
            refreshing = true
            delay(1.seconds)
            onRefresh()
            refreshing = false
        }

        val pullState = rememberPullRefreshState(refreshing, ::refresh)
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                LazyRow(
                    state = crumbScrollState,
                    contentPadding = PaddingValues(end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    stickyHeader {
                        var isContextMenuVisible by rememberSaveable { mutableStateOf(false) }

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(
                                topEnd = 16.dp,
                                bottomEnd = 16.dp
                            )
                        ) {
                            IconButton(
                                onClick = { isContextMenuVisible = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreHoriz,
                                    contentDescription = null
                                )

                                val dropdownItems = listOf(
                                    DropDownItem("Add to playlist", 0),
                                    DropDownItem("Recursive add to playlist", 1),
                                    DropDownItem("Add to play queue", 2),
                                    DropDownItem("Set as default path", 3),
                                    DropDownItem("Clear cache", 4)
                                )
                                CascadeDropdownMenu(
                                    expanded = isContextMenuVisible,
                                    onDismissRequest = { isContextMenuVisible = false }
                                ) {
                                    XmpDropdownMenuHeader {
                                        Text(
                                            text = "All Files",
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    dropdownItems.forEach {
                                        DropdownMenuItem(
                                            onClick = {
                                                onCrumbMenu(it.index)
                                                isContextMenuVisible = false
                                            },
                                            text = { Text(text = it.text) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    itemsIndexed(state.crumbs) { index, item ->
                        AssistChip(
                            modifier = Modifier.padding(horizontal = 3.dp),
                            enabled = item.enabled,
                            onClick = { onCrumbClick(item, index) },
                            label = { Text(text = item.name) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pullRefresh(pullState),
                contentAlignment = Alignment.Center
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = scrollState
                ) {
                    itemsIndexed(state.list) { index, item ->
                        ListItem(
                            modifier = Modifier.combinedClickable(
                                onClick = { onItemClick(item, index) },
                                onLongClick = { onItemLongClick(item, index) }
                            ),
                            leadingContent = {
                                val icon = when (item.type) {
                                    PlaylistItem.TYPE_DIRECTORY -> Icons.Default.Folder
                                    PlaylistItem.TYPE_FILE -> Icons.Default.InsertDriveFile
                                    else -> Icons.Default.QuestionMark
                                }

                                Icon(imageVector = icon, contentDescription = null)
                            },
                            headlineContent = {
                                Text(text = item.name)
                            },
                            supportingContent = {
                                // Hacky
                                val comment = if (item.type == PlaylistItem.TYPE_DIRECTORY) {
                                    stringResource(id = R.string.directory)
                                } else {
                                    item.comment
                                }
                                Text(text = comment, fontStyle = FontStyle.Italic)
                            }
                        )
                    }
                }

                if (state.list.isEmpty() && !state.isLoading) {
                    ErrorScreen(
                        text = stringResource(id = R.string.empty_directory),
                        content = {
                            if (!state.pathNotFound) {
                                OutlinedButton(onClick = onRestore) {
                                    Text(text = stringResource(id = R.string.go_back))
                                }
                            }
                        }
                    )
                }

                ProgressbarIndicator(isLoading = state.isLoading)

                PullRefreshIndicator(refreshing, pullState, Modifier.align(Alignment.TopCenter))
            }
        }

        // Scroll to the necessary ends for our lists.
        LaunchedEffect(key1 = state.list, key2 = state.crumbs) {
            if (state.list.isNotEmpty() || state.crumbs.isNotEmpty()) {
                crumbScrollState.animateScrollToItem(state.crumbs.lastIndex)
                scrollState.animateScrollToItem(0)
            }
        }
    }
}

@Preview
@Composable
private fun Preview_FileListScreen() {
    XmpTheme {
        FileListScreen(
            state = FileListViewModel.FileListState(
                isLoading = true,
                error = "An error has occurred.",
                list = List(15) {
                    PlaylistItem(
                        type = if (it < 5) PlaylistItem.TYPE_DIRECTORY else PlaylistItem.TYPE_FILE,
                        name = "Name $it",
                        comment = "Comment $it"
                    ).apply {
                        file = File("file/$it")
                    }
                },
                crumbs = List(10) {
                    FileListViewModel.BreadCrumb(
                        name = "Crumb $it",
                        path = "\\Some\\Current\\File\\$it"
                    )
                },
                isLoop = true,
                isShuffle = false
            ),
            snackbarHostState = SnackbarHostState(),
            onBack = {},
            onRefresh = {},
            onRestore = {},
            onLoop = {},
            onShuffle = {},
            onPlayAll = {},
            onCrumbClick = { _, _ -> },
            onCrumbMenu = {},
            onItemClick = { _, _ -> },
            onItemLongClick = { _, _ -> }
        )
    }
}
