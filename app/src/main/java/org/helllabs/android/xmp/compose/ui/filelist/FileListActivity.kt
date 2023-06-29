package org.helllabs.android.xmp.compose.ui.filelist

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.saket.cascade.CascadeDropdownMenu
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.browser.playlist.PlaylistItem
import org.helllabs.android.xmp.browser.playlist.PlaylistUtils
import org.helllabs.android.xmp.compose.components.BottomBarButtons
import org.helllabs.android.xmp.compose.components.ErrorScreen
import org.helllabs.android.xmp.compose.components.ListDialog
import org.helllabs.android.xmp.compose.components.MessageDialog
import org.helllabs.android.xmp.compose.components.ProgressbarIndicator
import org.helllabs.android.xmp.compose.components.XmpDropdownMenuHeader
import org.helllabs.android.xmp.compose.components.XmpTopBar
import org.helllabs.android.xmp.compose.components.pullrefresh.ExperimentalMaterialApi
import org.helllabs.android.xmp.compose.components.pullrefresh.PullRefreshIndicator
import org.helllabs.android.xmp.compose.components.pullrefresh.pullRefresh
import org.helllabs.android.xmp.compose.components.pullrefresh.rememberPullRefreshState
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.BasePlaylistActivity
import org.helllabs.android.xmp.core.Assets
import org.helllabs.android.xmp.core.Files
import org.helllabs.android.xmp.model.DropDownItem
import org.helllabs.android.xmp.util.FileUtils
import org.helllabs.android.xmp.util.InfoCache
import timber.log.Timber
import java.io.File
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

// TODO: Seriously need to separate classes, and hoist composables!

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
                /**
                 * Path not found dialog
                 */
                MessageDialog(
                    isShowing = state.pathNotFound,
                    icon = Icons.Default.Warning,
                    title = stringResource(id = R.string.file_no_path_title),
                    text = stringResource(
                        id = R.string.file_no_path_text,
                        viewModel.currentPath
                    ),
                    confirmText = stringResource(id = R.string.create),
                    onConfirm = {
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
                    },
                    onDismiss = {
                        viewModel.showPathNotFound(false)
                        finish()
                    }
                )

                /**
                 * Playlist choice dialog
                 */
                var playlistChoiceState: PlaylistChoiceData? by remember { mutableStateOf(null) }
                ListDialog(
                    isShowing = playlistChoiceState != null,
                    icon = Icons.Default.PlaylistAdd,
                    title = stringResource(id = R.string.msg_select_playlist),
                    list = PlaylistUtils.listNoSuffix().toList(),
                    onConfirm = { choice ->
                        with(playlistChoiceState!!) {
                            playlistChoice.execute(fileSelection, choice)
                        }
                        playlistChoiceState = null
                    },
                    onDismiss = { playlistChoiceState = null },
                    onEmpty = {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = getString(R.string.msg_no_playlists)
                            )
                        }
                        playlistChoiceState = null
                    }
                )

                /**
                 * Delete directory dialog
                 */
                var deleteDirectory by remember { mutableIntStateOf(-1) }
                val deleteName by remember(deleteDirectory) {
                    val deleteName = viewModel.getItems()[deleteDirectory].file!!.path
                    mutableStateOf(deleteName)
                }
                MessageDialog(
                    isShowing = deleteDirectory >= 0,
                    precondition = deleteName.startsWith(PrefManager.mediaPath) &&
                        deleteName != PrefManager.mediaPath,
                    onPrecondition = {
                        // Prevent deletion of files outside preferred mod dir.
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = getString(R.string.error_dir_not_under_moddir)
                            )
                        }
                        deleteDirectory = -1
                    },
                    icon = Icons.Default.QuestionMark,
                    title = "Delete directory",
                    text = "Are you sure you want to delete directory \"${
                    FileUtils.basename(
                        deleteName
                    )
                    }\" and all its contents?",
                    confirmText = stringResource(id = R.string.menu_delete),
                    onConfirm = {
                        if (InfoCache.deleteRecursive(deleteName)) {
                            viewModel.onRefresh()
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = getString(R.string.msg_dir_deleted)
                                )
                            }
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = getString(R.string.msg_cant_delete_dir)
                                )
                            }
                        }
                        deleteDirectory = -1
                    },
                    onDismiss = {
                        deleteDirectory = -1
                    }
                )

                /**
                 * Delete file dialog
                 */
                var deleteFile: String? by remember { mutableStateOf(null) }
                MessageDialog(
                    isShowing = deleteFile != null,
                    icon = Icons.Default.QuestionMark,
                    title = "Delete File",
                    text = "Are you sure you want to delete ${FileUtils.basename(deleteFile)}?",
                    confirmText = stringResource(id = R.string.menu_delete),
                    onConfirm = {
                        if (InfoCache.delete(deleteFile!!)) {
                            viewModel.onRefresh()
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = getString(R.string.msg_file_deleted)
                                )
                            }
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = getString(R.string.msg_cant_delete)
                                )
                            }
                        }
                        deleteFile = null
                    },
                    onDismiss = {
                        deleteFile = null
                    }
                )

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
                    onItemLongClick = { item, index, menuIndex ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (item.type == PlaylistItem.TYPE_DIRECTORY) {
                            // Directories
                            when (menuIndex) {
                                0 ->
                                    playlistChoiceState =
                                        PlaylistChoiceData(index, addRecursiveToPlaylistChoice)

                                1 -> addToQueue(Files.recursiveList(item.file))
                                2 -> playModule(Files.recursiveList(item.file))
                                3 -> deleteDirectory = index
                            }
                        } else {
                            // Files
                            when (menuIndex) {
                                0 ->
                                    playlistChoiceState =
                                        PlaylistChoiceData(index, addFileToPlaylistChoice)

                                1 -> addToQueue(item.file!!.path)
                                2 -> playModule(item.file!!.path)
                                3 -> playModule(viewModel.getFilenameList(), index)
                                4 -> deleteFile = item.file!!.path
                            }
                        }
                    }
                )
            }
        }
    }

    override fun update() {
        viewModel.onRefresh()
    }
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
    onItemLongClick: (item: PlaylistItem, index: Int, menuIndex: Int) -> Unit
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
                            val haptic = LocalHapticFeedback.current

                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isContextMenuVisible = true
                                }
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
                        Card(modifier = Modifier.padding(6.dp)) {
                            var isContextMenuVisible by rememberSaveable { mutableStateOf(false) }
                            val haptic = LocalHapticFeedback.current
                            ListItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onItemClick(item, index) },
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
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            isContextMenuVisible = true
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = null
                                        )
                                    }

                                    val contextList =
                                        if (item.type == PlaylistItem.TYPE_DIRECTORY) {
                                            listOf(
                                                DropDownItem("Add to playlist", 0),
                                                DropDownItem("Add to play queue", 1),
                                                DropDownItem("Play contents", 2),
                                                DropDownItem("Delete directory", 3)
                                            )
                                        } else {
                                            val mode = PrefManager.playlistMode
                                            mutableListOf(
                                                DropDownItem("Add to playlist", 0),
                                                DropDownItem("Delete file", 4)
                                            ).also {
                                                if (mode != 3) {
                                                    it.add(1, DropDownItem("Add to play queue", 1))
                                                }
                                                if (mode != 2) {
                                                    it.add(2, DropDownItem("Play this file", 2))
                                                }
                                                if (mode != 1) {
                                                    it.add(
                                                        3,
                                                        DropDownItem("Play all starting here", 3)
                                                    )
                                                }
                                            }
                                        }
                                    CascadeDropdownMenu(
                                        expanded = isContextMenuVisible,
                                        onDismissRequest = { isContextMenuVisible = false }
                                    ) {
                                        XmpDropdownMenuHeader {
                                            val text =
                                                if (item.type == PlaylistItem.TYPE_DIRECTORY) {
                                                    "This Directory"
                                                } else {
                                                    "This File"
                                                }
                                            Text(text = text)
                                        }
                                        contextList.forEach {
                                            DropdownMenuItem(
                                                text = { Text(text = it.text) },
                                                onClick = {
                                                    onItemLongClick(item, index, it.index)
                                                    isContextMenuVisible = false
                                                }
                                            )
                                        }
                                    }
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
            onItemLongClick = { _, _, _ -> }
        )
    }
}
