package org.helllabs.android.xmp.compose.ui.filelist

import android.annotation.SuppressLint
import android.net.Uri
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
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
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
import org.helllabs.android.xmp.core.PlaylistManager
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.core.StorageManager
import org.helllabs.android.xmp.model.DropDownSelection
import org.helllabs.android.xmp.model.FileItem
import timber.log.Timber

class FileListActivity : BasePlaylistActivity() {

    private val viewModel by viewModels<FileListViewModel>()

    // TODO tombstones when loading lots of items -> use getAllFiles2()
    override val allFiles: List<Uri>
        get() = viewModel.onAllFiles()

    override val isShuffleMode: Boolean
        get() = viewModel.uiState.value.isShuffle

    override val isLoopMode: Boolean
        get() = viewModel.uiState.value.isLoop

    override fun update() {
        viewModel.onRefresh()
    }

    override suspend fun getAllFiles(): List<Uri> = viewModel.onAllFiles2()

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
             * Playlist choice dialog
             */
            ListDialog(
                isShowing = viewModel.playlistChoice != null,
                icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                title = stringResource(id = R.string.msg_select_playlist),
                list = viewModel.playlistList,
                onConfirm = { choice ->
                    viewModel.addToPlaylist(choice)
                    viewModel.playlistChoice = null
                },
                onDismiss = { viewModel.playlistChoice = null },
                onEmpty = {
                    showSnack(message = getString(R.string.msg_no_playlists))
                    viewModel.playlistChoice = null
                }
            )

            /**
             * Delete directory dialog
             */
            var deleteDirectory: Uri? by remember { mutableStateOf(null) }
            if (deleteDirectory != null) {
                MessageDialog(
                    isShowing = true,
                    icon = Icons.Default.QuestionMark,
                    title = "Delete directory",
                    text = "Are you sure you want to delete directory " +
                        "${StorageManager.getFileName(deleteDirectory)} and all its contents?",
                    confirmText = stringResource(id = R.string.menu_delete),
                    onConfirm = {
                        val res = StorageManager.deleteFileOrDirectory(deleteDirectory)
                        if (!res) {
                            showSnack("Unable to delete directory")
                        }
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
            var deleteFile: Uri? by remember { mutableStateOf(null) }
            MessageDialog(
                isShowing = deleteFile != null,
                icon = Icons.Default.QuestionMark,
                title = "Delete File",
                text = "Are you sure you want to delete ${StorageManager.getFileName(deleteFile)}",
                confirmText = stringResource(id = R.string.menu_delete),
                onConfirm = {
                    val res = StorageManager.deleteFileOrDirectory(deleteFile)
                    if (!res) {
                        showSnack("Unable to delete item")
                    }
                    update()
                    deleteFile = null
                },
                onDismiss = {
                    deleteFile = null
                }
            )

            XmpTheme {
                FileListScreen(
                    state = state,
                    snackBarHostState = snackBarHostState,
                    onBack = {
                        callback.remove()
                        onBackPressedDispatcher.onBackPressed()
                    },
                    onScrollPosition = viewModel::setScrollPosition,
                    onRefresh = viewModel::onRefresh,
                    onRestore = viewModel::onRestore,
                    onShuffle = viewModel::onShuffle,
                    onLoop = viewModel::onLoop,
                    onPlayAll = ::onPlayAll2,
                    onCrumbMenu = { selection ->
                        when (selection) {
                            DropDownSelection.DIR_ADD_TO_PLAYLIST -> {
                                viewModel.playlistList = PlaylistManager.listPlaylists()
                                viewModel.playlistChoice = viewModel.currentPath
                            }

                            DropDownSelection.DIR_ADD_TO_QUEUE ->
                                addToQueue(viewModel.getItems())

                            DropDownSelection.DIR_PLAY_CONTENTS ->
                                playModule(viewModel.getItems())

                            else -> Unit
                        }
                    },
                    onCrumbClick = { crumb, _ ->
                        viewModel.onNavigate(crumb.path)
                    },
                    onItemClick = { item, index ->
                        if (item.docFile!!.isFile()) {
                            onItemClick(
                                viewModel.getItems(),
                                viewModel.getDirectoryCount(),
                                index
                            )
                        } else {
                            viewModel.onNavigate(item.docFile)
                        }
                    },
                    onItemLongClick = { item, index, sel ->
                        when (sel) {
                            DropDownSelection.DELETE ->
                                deleteFile = item.docFile?.uri

                            DropDownSelection.DIR_ADD_TO_PLAYLIST -> {
                                viewModel.playlistList = PlaylistManager.listPlaylists()
                                viewModel.playlistChoice = item.docFile
                            }

                            DropDownSelection.DIR_ADD_TO_QUEUE ->
                                StorageManager.walkDownDirectory(
                                    uri = item.docFile?.uri,
                                    includeDirectories = false
                                ).also(::playModule)

                            DropDownSelection.DIR_PLAY_CONTENTS ->
                                StorageManager.walkDownDirectory(
                                    uri = item.docFile?.uri,
                                    includeDirectories = false
                                ).also(::playModule)

                            DropDownSelection.FILE_ADD_TO_PLAYLIST -> {
                                viewModel.playlistList = PlaylistManager.listPlaylists()
                                viewModel.playlistChoice = item.docFile
                            }

                            DropDownSelection.FILE_ADD_TO_QUEUE ->
                                addToQueue(item.docFile?.uri)

                            DropDownSelection.FILE_PLAY_HERE ->
                                playModule(viewModel.getItems(), index, true)

                            DropDownSelection.FILE_PLAY_THIS_ONLY ->
                                playModule(item.docFile?.uri?.let { listOf(it) }.orEmpty())
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
    snackBarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onScrollPosition: (Int) -> Unit,
    onRefresh: () -> Unit,
    onRestore: () -> Unit,
    onShuffle: (value: Boolean) -> Unit,
    onLoop: (value: Boolean) -> Unit,
    onPlayAll: () -> Unit,
    onCrumbMenu: (DropDownSelection) -> Unit,
    onCrumbClick: (crumb: FileListViewModel.BreadCrumb, index: Int) -> Unit,
    onItemClick: (item: FileItem, index: Int) -> Unit,
    onItemLongClick: (item: FileItem, index: Int, sel: DropDownSelection) -> Unit
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
        if (state.crumbs.isEmpty()) return@LaunchedEffect

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
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) }
    ) { paddingValues ->
        // Outer Box() to properly hide Pull-Refresh
        Box(modifier = Modifier.padding(paddingValues)) {
            val pullRefreshState = rememberPullToRefreshState()
            if (pullRefreshState.isRefreshing) {
                LaunchedEffect(true) {
                    onRefresh()
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
                            OutlinedButton(onClick = onRestore) {
                                Text(text = stringResource(id = R.string.go_back))
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
    PrefManager.init(context)

    XmpTheme(useDarkTheme = true) {
        FileListScreen(
            state = FileListViewModel.FileListState(
                isLoading = true,
                error = "An error has occurred.",
                list = List(10) {
                    FileItem(
                        name = "Name $it",
                        comment = "Comment $it",
                        docFile = null
                    )
                },
                crumbs = List(4) {
                    FileListViewModel.BreadCrumb(
                        name = "Crumb $it",
                        path = null
                    )
                },
                isLoop = true,
                isShuffle = false
            ),
            snackBarHostState = SnackbarHostState(),
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
