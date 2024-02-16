package org.helllabs.android.xmp.compose.ui.filelist

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.BottomBarButtons
import org.helllabs.android.xmp.compose.components.ErrorScreen
import org.helllabs.android.xmp.compose.components.ListDialog
import org.helllabs.android.xmp.compose.components.MessageDialog
import org.helllabs.android.xmp.compose.components.ProgressbarIndicator
import org.helllabs.android.xmp.compose.components.XmpTopBar
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.BasePlaylistActivity
import org.helllabs.android.xmp.compose.ui.filelist.components.BreadCrumbs
import org.helllabs.android.xmp.compose.ui.filelist.components.FileListCard
import org.helllabs.android.xmp.core.Assets
import org.helllabs.android.xmp.core.PlaylistMessages
import org.helllabs.android.xmp.core.PlaylistUtils
import org.helllabs.android.xmp.model.PlaylistItem
import timber.log.Timber
import java.io.File
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

class FileListActivity : BasePlaylistActivity() {

    private val viewModel by viewModels<FileListViewModel>()

    override val allFiles: List<String>
        get() = listOf() // TODO // Files.recursiveList(viewModel.currentPath)

    override val isShuffleMode: Boolean
        get() = viewModel.uiState.value.isShuffle

    override val isLoopMode: Boolean
        get() = viewModel.uiState.value.isLoop

    override fun update() {
        viewModel.onRefresh()
    }

    fun playlistMessage(playlistMessages: PlaylistMessages) {
        val message = when (playlistMessages) {
            PlaylistMessages.AddingFiles -> "Scanning module files..."
            PlaylistMessages.CantWriteToPlaylist -> getString(R.string.error_write_to_playlist)
            PlaylistMessages.UnrecognizedFormat -> getString(R.string.unrecognized_format)
            PlaylistMessages.ValidFormatsAdded -> getString(R.string.msg_only_valid_files_added)
        }

        showSnack(message)
    }

    /**
     * Recursively add current directory to playlist
     */
    private val addCurrentRecursiveChoice: PlaylistChoice = object : PlaylistChoice {
        override fun execute(fileSelection: Int, playlistSelection: Int) {
            PlaylistUtils.filesToPlaylist(
                fileList = listOf(), // TODO // Files.recursiveList(viewModel.currentPath),
                playlistName = PlaylistUtils.getPlaylistName(playlistSelection),
                onMessage = ::playlistMessage
            )
        }
    }

    /**
     * Recursively add directory to playlist
     */
    private val addRecursiveToPlaylistChoice: PlaylistChoice = object : PlaylistChoice {
        override fun execute(fileSelection: Int, playlistSelection: Int) {
            PlaylistUtils.filesToPlaylist(
                fileList = listOf(), // TODO // Files.recursiveList(viewModel.getItems()[fileSelection].file),
                playlistName = PlaylistUtils.getPlaylistName(playlistSelection),
                onMessage = ::playlistMessage
            )
        }
    }

    /**
     * Add one file to playlist
     */
    private val addFileToPlaylistChoice: PlaylistChoice = object : PlaylistChoice {
        override fun execute(fileSelection: Int, playlistSelection: Int) {
            PlaylistUtils.filesToPlaylist(
                filename = viewModel.getItems()[fileSelection].file?.path.orEmpty(),
                playlistName = PlaylistUtils.getPlaylistName(playlistSelection),
                onMessage = ::playlistMessage
            )
        }
    }

    /**
     * Add file list to playlist
     */
    private val addFileListToPlaylistChoice: PlaylistChoice = object : PlaylistChoice {
        override fun execute(fileSelection: Int, playlistSelection: Int) {
            PlaylistUtils.filesToPlaylist(
                fileList = viewModel.getItems().filter { it.isFile }.mapNotNull { it.file?.path },
                playlistName = PlaylistUtils.getPlaylistName(playlistSelection),
                onMessage = ::playlistMessage
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

        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.auto(
                Color.Transparent.toArgb(),
                Color.Transparent.toArgb()
            )
        )

        // On back pressed handler
        val callback = object : OnBackPressedCallback(true) {
            private fun goBack() {
                this.remove()
                onBackPressedDispatcher.onBackPressed()
            }

            override fun handleOnBackPressed() {
                if (PrefManager.backButtonNavigation) {
                    if (!viewModel.onBackPressed()) {
                        goBack()
                    }
                } else {
                    goBack()
                }
            }
        }

        Timber.d("onCreate")
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            // Set up and override on back pressed.
            DisposableEffect(onBackPressedDispatcher) {
                onBackPressedDispatcher.addCallback(callback)
                onDispose {
                    callback.remove()
                }
            }

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
                        showSnack(
                            message = "Error creating directory ${viewModel.currentPath}.",
                            actionLabel = getString(R.string.ok)
                        )

                        finish()
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
                icon = Icons.AutoMirrored.Filled.PlaylistAdd,
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
                    showSnack(message = getString(R.string.msg_no_playlists))
                    playlistChoiceState = null
                }
            )

            /**
             * Delete directory dialog
             */
            var deleteDirectory: String? by remember { mutableStateOf(null) }
            if (deleteDirectory != null) {
                MessageDialog(
                    isShowing = true,
                    precondition = deleteDirectory!!.startsWith(PrefManager.mediaPath) &&
                        deleteDirectory != PrefManager.mediaPath,
                    onPrecondition = {
                        // Prevent deletion of files outside preferred mod dir.
                        showSnack(message = getString(R.string.error_dir_not_under_moddir))

                        deleteDirectory = null
                    },
                    icon = Icons.Default.QuestionMark,
                    title = "Delete directory",
                    text = "Are you sure you want to delete directory /*Files.basename(deleteDirectory)*/and all its contents?", // TODO
                    confirmText = stringResource(id = R.string.menu_delete),
                    onConfirm = {
                        // TODO delete recursively
//                        val text = if (InfoCache.deleteRecursive(deleteDirectory!!)) {
//                            viewModel.onRefresh()
//                            getString(R.string.msg_dir_deleted)
//                        } else {
//                            getString(R.string.msg_cant_delete_dir)
//                        }
//                        showSnack(message = text)
                        deleteDirectory = null
                    },
                    onDismiss = {
                        deleteDirectory = null
                    }
                )
            }

            /**
             * Delete file dialog
             */
            var deleteFile: String? by remember { mutableStateOf(null) }
            MessageDialog(
                isShowing = deleteFile != null,
                icon = Icons.Default.QuestionMark,
                title = "Delete File",
                text = "Are you sure you want to delete Files.basename(deleteFile)", // TODO
                confirmText = stringResource(id = R.string.menu_delete),
                onConfirm = {
                    // TODO delete file
//                    if (InfoCache.delete(deleteFile!!)) {
//                        viewModel.onRefresh()
//                        showSnack(message = getString(R.string.msg_file_deleted))
//                    } else {
//                        showSnack(message = getString(R.string.msg_cant_delete))
//                    }
                    deleteFile = null
                },
                onDismiss = {
                    deleteFile = null
                }
            )

            XmpTheme {
                FileListScreen(
                    state = state,
                    snackbarHostState = snackbarHostState,
                    onBack = {
                        callback.remove()
                        onBackPressedDispatcher.onBackPressed()
                    },
                    onScrollPosition = viewModel::setScrollPosition,
                    onRefresh = viewModel::onRefresh,
                    onRestore = viewModel::onRestore,
                    onShuffle = viewModel::onShuffle,
                    onLoop = viewModel::onLoop,
                    onPlayAll = ::onPlayAll,
                    onCrumbMenu = { index ->
                        when (index) {
                            0 ->
                                playlistChoiceState =
                                    PlaylistChoiceData(0, addFileListToPlaylistChoice)

                            1 ->
                                playlistChoiceState =
                                    PlaylistChoiceData(0, addCurrentRecursiveChoice)

                            2 -> addToQueue(viewModel.getFilenameList())
                            3 -> {
                                showSnack(message = "Set as default module path")
                                PrefManager.mediaPath = viewModel.currentPath
                            }
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
                        if (item.isDirectory) {
                            // Directories
                            when (menuIndex) {
                                0 ->
                                    playlistChoiceState =
                                        PlaylistChoiceData(index, addRecursiveToPlaylistChoice)

                                1 -> addToQueue(listOf()) // TODO // Files.recursiveList(item.file))
                                2 -> playModule(listOf()) // TODO // Files.recursiveList(item.file))
                                3 -> deleteDirectory = item.file!!.path
                            }
                        } else {
                            // Files
                            when (menuIndex) {
                                0 ->
                                    playlistChoiceState =
                                        PlaylistChoiceData(index, addFileToPlaylistChoice)

                                1 -> addToQueue(item.file!!.path)
                                2 -> playModule(listOf(item.file!!.path))
                                3 -> playModule(viewModel.getFilenameList(), index)
                                4 -> deleteFile = item.file!!.path
                            }
                        }
                    }
                )
            }
        }
    }
}

@OptIn(FlowPreview::class, ExperimentalMaterial3Api::class)
@Composable
private fun FileListScreen(
    state: FileListViewModel.FileListState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onScrollPosition: (Int) -> Unit,
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
    val scrollState = rememberLazyListState(initialFirstVisibleItemIndex = state.lastScrollPosition)
    val crumbScrollState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex > 0
        }
    }

    // Save our scroll position
    LaunchedEffect(scrollState) {
        snapshotFlow {
            scrollState.firstVisibleItemIndex
        }.debounce(1.seconds).collectLatest {
            onScrollPosition(it)
        }
    }

    // Refresh our list's scroll position if our crumbs changes.
    LaunchedEffect(state.crumbs) {
        scrollState.animateScrollToItem(state.lastScrollPosition)
        crumbScrollState.animateScrollToItem(state.crumbs.lastIndex)
    }

    Scaffold(
        topBar = {
            Column {
                XmpTopBar(
                    title = stringResource(id = R.string.browser_filelist_title),
                    isScrolled = isScrolled,
                    onBack = onBack
                )
                BreadCrumbs(
                    modifier = Modifier.background(MaterialTheme.colorScheme.background),
                    crumbScrollState = crumbScrollState,
                    crumbs = state.crumbs,
                    onCrumbMenu = onCrumbMenu,
                    onCrumbClick = onCrumbClick
                )
            }
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
        Box(modifier = Modifier.padding(paddingValues)) {
            var refreshing by remember { mutableStateOf(false) }

            val pullRefreshState = rememberPullToRefreshState()
            if (pullRefreshState.isRefreshing) {
                LaunchedEffect(true) {
                    refreshing = true
                    delay(1.seconds)
                    onRefresh()
                    refreshing = false
                    pullRefreshState.endRefresh()
                }
            }

            Box(
                modifier = Modifier.nestedScroll(pullRefreshState.nestedScrollConnection),
                contentAlignment = Alignment.Center
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = scrollState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(state.list) { index, item ->
                        FileListCard(
                            item = item,
                            onItemClick = { onItemClick(item, index) },
                            onItemLongClick = { onItemLongClick(item, index, it) }
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

                PullToRefreshContainer(
                    modifier = Modifier.align(Alignment.TopCenter),
                    state = pullRefreshState
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview_FileListScreen() {
    val context = LocalContext.current
    PrefManager.init(context, File(""))

    XmpTheme(useDarkTheme = true) {
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
            onScrollPosition = {},
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
