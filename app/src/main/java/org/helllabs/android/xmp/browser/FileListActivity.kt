package org.helllabs.android.xmp.browser

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.browser.playlist.PlaylistItem
import org.helllabs.android.xmp.browser.playlist.PlaylistUtils
import org.helllabs.android.xmp.compose.components.BottomBarButtons
import org.helllabs.android.xmp.compose.components.ErrorScreen
import org.helllabs.android.xmp.compose.components.ProgressbarIndicator
import org.helllabs.android.xmp.compose.components.XmpTopBar
import org.helllabs.android.xmp.compose.components.pullrefresh.ExperimentalMaterialApi
import org.helllabs.android.xmp.compose.components.pullrefresh.PullRefreshIndicator
import org.helllabs.android.xmp.compose.components.pullrefresh.pullRefresh
import org.helllabs.android.xmp.compose.components.pullrefresh.rememberPullRefreshState
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.util.Message
import timber.log.Timber
import java.io.File
import kotlin.time.Duration.Companion.seconds

data class BreadCrumb(
    val name: String,
    val path: String
)

class FileListViewModel : ViewModel() {
    data class FileListState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val list: List<PlaylistItem> = listOf(),
        val crumbs: List<BreadCrumb> = listOf(),
        val isLoop: Boolean = false,
        val isShuffle: Boolean = false
    )

    private val _uiState = MutableStateFlow(FileListState())
    val uiState = _uiState.asStateFlow()

    fun init() {
        val initialPath = PrefManager.mediaPath
        _uiState.update {
            it.copy(
                isShuffle = PrefManager.shuffleMode,
                isLoop = PrefManager.loopMode
            )
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
        // TODO
    }

    fun getCurrentPath(): String {
        return _uiState.value.crumbs.last().path
    }

    fun startNavigation() {
        val file = File(getCurrentPath())
    }

    fun changeDirectory(file: File): Boolean {
        val crumbs = _uiState.value.crumbs.toMutableList()

        if (file.isDirectory) {
            crumbs.add(
                BreadCrumb(
                    name = File(file.parent!!).name,
                    path = file.path
                )
            )
        }
    }
}

class FileListActivity : ComponentActivity() {

    private val viewModel by viewModels<FileListViewModel>()

    val allFiles: List<String>
        get() = recursiveList(viewModel.getCurrentPath())

    /**
     * Recursively add current directory to playlist
     */
    private val addCurrentRecursiveChoice: PlaylistChoice = object : PlaylistChoice {
        override fun execute(fileSelection: Int, playlistSelection: Int) {
            PlaylistUtils.filesToPlaylist(
                this@FileListActivity,
                recursiveList(viewModel.getCurrentPath()),
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
                recursiveList(viewModel.uiState.value.list[fileSelection].file),
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
    private interface PlaylistChoice {
        fun execute(fileSelection: Int, playlistSelection: Int)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")

        WindowCompat.setDecorFitsSystemWindows(window, false)

        // TODO handle nav bar back.
        // mBackButtonParentdir = PrefManager.backButtonNavigation

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                // Init some state variables
                viewModel.init()
            }

            XmpTheme {
                var pathNotFound by remember { mutableStateOf(false) }
                if (pathNotFound) {
                    AlertDialog(
                        onDismissRequest = { /*TODO*/ },
                        icon = {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null)
                        },
                        title = {
                            Text(text = stringResource(id = R.string.file_no_path_title))
                        },
                        text = {
                            Text(
                                text = stringResource(
                                    id = R.string.file_no_path_text,
                                    viewModel.getCurrentPath()
                                )
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val ret = Examples.install(
                                        this@FileListActivity,
                                        viewModel.getCurrentPath(),
                                        PrefManager.examples
                                    )
                                    if (ret < 0) {
                                        Message.error(
                                            this@FileListActivity,
                                            "Error creating directory ${viewModel.getCurrentPath()}."
                                        )
                                    }
                                    viewModel.startNavigation()
                                    pathNotFound = false
                                }
                            ) {
                                Text(text = stringResource(id = R.string.create))
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    finish()
                                    pathNotFound = false
                                }
                            ) {
                                Text(text = stringResource(id = R.string.cancel))
                            }
                        }
                    )
                }

                FileListScreen(
                    state = state,
                    onBack = { onBackPressedDispatcher.onBackPressed() },
                    onRefresh = viewModel::onRefresh,
                    onShuffle = viewModel::onShuffle,
                    onLoop = viewModel::onLoop,
                    onPlayAll = {
                    },
                    onCrumbMenu = {
                    },
                    onCrumbClick = { crumb, index ->
                    },
                    onItemClick = { item, index ->
                        if (viewModel.changeDirectory(item.file)) {
                            updateModlist()
                        } else {
                            // TODO handle module click
                        }
                    },
                    onItemLongClick = { item, index ->
                    }
                )
            }
        }

//        // Check if directory exists
//        val modDir = File(mediaPath)
//        if (modDir.isDirectory) {
//            mNavigation!!.startNavigation(modDir)
//            updateModlist()
//        } else {
//            pathNotFound(mediaPath.orEmpty())
//        }
    }

//    private fun parentDir() {
//        if (mNavigation!!.parentDir()) {
//            updateModlist()
//            mNavigation!!.restoreListPosition(recyclerView)
//        }
//    }
//
//    private fun updateModlist() {
//        val modDir = mNavigation?.currentDir ?: return
//        mPlaylistAdapter.clear()
//        curPath!!.text = modDir.path
//        val list: MutableList<PlaylistItem> = ArrayList()
//        val dirFiles = modDir.listFiles()
//        if (dirFiles != null) {
//            for (file in dirFiles) {
//                val item: PlaylistItem = if (file.isDirectory) {
//                    PlaylistItem(
//                        PlaylistItem.TYPE_DIRECTORY,
//                        file.name,
//                        getString(R.string.directory)
//                    )
//                } else {
//                    val date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM)
//                        .format(file.lastModified())
//                    val comment = date + String.format(" (%d kB)", file.length() / 1024)
//                    PlaylistItem(PlaylistItem.TYPE_FILE, file.name, comment)
//                }
//                item.file = file
//                list.add(item)
//            }
//        }
//        list.sort()
//        PlaylistUtils.renumberIds(list)
//        mPlaylistAdapter.addList(list)
//        mPlaylistAdapter.notifyDataSetChanged()
//        mCrossfade!!.crossfade()
//    }
//
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
//    private fun choosePlaylist(fileSelection: Int, choice: PlaylistChoice) {
//        // Return if no playlists exist
//        if (PlaylistUtils.list().isEmpty()) {
//            toast(this, getString(R.string.msg_no_playlists))
//            return
//        }
//        val playlistSelection = IntArray(1)
//        val listener = DialogInterface.OnClickListener { _, which ->
//            if (which == DialogInterface.BUTTON_POSITIVE && playlistSelection[0] >= 0) {
//                choice.execute(fileSelection, playlistSelection[0])
//            }
//        }
//        val builder = AlertDialog.Builder(this)
//        builder.setTitle(R.string.msg_select_playlist)
//            .setPositiveButton(R.string.ok, listener)
//            .setNegativeButton(R.string.cancel, listener)
//            .setSingleChoiceItems(
//                PlaylistUtils.listNoSuffix(),
//                0
//            ) { _, which ->
//                playlistSelection[0] = which
//            }
//            .show()
//    }
//
//    private fun clearCachedEntries(fileList: List<String>) {
//        for (filename in fileList) {
//            clearCache(filename)
//        }
//    }
//
//    // Playlist context menu
//    override fun onCreateContextMenu(menu: ContextMenu?, view: View?, menuInfo: ContextMenuInfo?) {
//        super.onCreateContextMenu(menu, view, menuInfo)
//
//        if (menu == null) {
//            return
//        }
//
//        if (view == curPath) {
//            isPathMenu = true
//            menu.setHeaderTitle("All files")
//            menu.add(Menu.NONE, 0, 0, "Add to playlist")
//            menu.add(Menu.NONE, 1, 1, "Recursive add to playlist")
//            menu.add(Menu.NONE, 2, 2, "Add to play queue")
//            menu.add(Menu.NONE, 3, 3, "Set as default path")
//            menu.add(Menu.NONE, 4, 4, "Clear cache")
//            return
//        }
//        isPathMenu = false
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
//        if (isPathMenu) {
//            when (id) {
//                0 -> choosePlaylist(0, addFileListToPlaylistChoice)
//                1 -> choosePlaylist(0, addCurrentRecursiveChoice)
//                2 -> addToQueue(mPlaylistAdapter.filenameList)
//                3 -> {
//                    PrefManager.mediaPath = mNavigation?.currentDir?.path.toString()
//                    toast(this, "Set as default module path")
//                }
//
//                4 -> clearCachedEntries(mPlaylistAdapter.filenameList)
//            }
//            return true
//        }
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

    companion object {
        private fun recursiveList(path: String): List<String> =
            recursiveList(File(path))

        private fun recursiveList(file: File?): List<String> =
            file?.walk()?.filter { it.isFile }?.map { it.path }?.toList() ?: emptyList()
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
private fun FileListScreen(
    state: FileListViewModel.FileListState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onShuffle: (value: Boolean) -> Unit,
    onLoop: (value: Boolean) -> Unit,
    onPlayAll: () -> Unit,
    onCrumbMenu: () -> Unit,
    onCrumbClick: (crumb: BreadCrumb, index: Int) -> Unit,
    onItemClick: (item: PlaylistItem, index: Int) -> Unit,
    onItemLongClick: (item: PlaylistItem, index: Int) -> Unit
) {
    val snackBarHostState = remember { SnackbarHostState() }
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
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) }
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
                    contentPadding = PaddingValues(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    stickyHeader {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(
                                topEnd = 16.dp,
                                bottomEnd = 16.dp
                            )
                        ) {
                            IconButton(
                                modifier = Modifier.padding(horizontal = 3.dp),
                                onClick = onCrumbMenu
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreHoriz,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                    itemsIndexed(state.crumbs) { index, item ->
                        AssistChip(
                            modifier = Modifier.padding(horizontal = 3.dp),
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
                                Text(text = item.comment)
                            }
                        )
                    }
                }

                state.error?.let {
                    ErrorScreen(text = it)
                }

                ProgressbarIndicator(isLoading = state.isLoading)

                PullRefreshIndicator(refreshing, pullState, Modifier.align(Alignment.TopCenter))
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
                    BreadCrumb(
                        name = "Crumb $it",
                        path = "\\Some\\Current\\File\\$it"
                    )
                },
                isLoop = true,
                isShuffle = false
            ),
            onBack = {},
            onRefresh = {},
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
