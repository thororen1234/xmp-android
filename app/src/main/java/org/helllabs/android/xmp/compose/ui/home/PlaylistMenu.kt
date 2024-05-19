package org.helllabs.android.xmp.compose.ui.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.EditPlaylistDialog
import org.helllabs.android.xmp.compose.components.ErrorScreen
import org.helllabs.android.xmp.compose.components.MessageDialog
import org.helllabs.android.xmp.compose.components.NewPlaylistDialog
import org.helllabs.android.xmp.compose.components.ProgressbarIndicator
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.theme.michromaFontFamily
import org.helllabs.android.xmp.compose.theme.themedText
import org.helllabs.android.xmp.core.PlaylistManager
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.core.StorageManager
import org.helllabs.android.xmp.model.FileItem
import timber.log.Timber

sealed class HomeScreenAction {
    data class OnPlaylist(val name: String) : HomeScreenAction()
    data object OnDocumentTree : HomeScreenAction()
    data object OnFatalError : HomeScreenAction()
    data object OnFileList : HomeScreenAction()
    data object OnPlayerScreen : HomeScreenAction()
    data object OnPreferenceScreen : HomeScreenAction()
    data object OnRequestSettings : HomeScreenAction()
    data object OnSearchScreen : HomeScreenAction()
}

@Serializable
object NavigationHome

@Composable
fun HomeScreen(
    viewModel: PlaylistMenuViewModel,
    snackBarHostState: SnackbarHostState,
    onAction: (HomeScreenAction) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    /**
     * Error message Dialog
     */
    MessageDialog(
        isShowing = state.errorText.isNotEmpty(),
        title = stringResource(id = R.string.error),
        text = state.errorText,
        confirmText = if (state.isFatalError) {
            stringResource(id = R.string.exit)
        } else {
            stringResource(id = R.string.ok)
        },
        onConfirm = {
            if (state.isFatalError) {
                onAction(HomeScreenAction.OnFatalError)
            }

            viewModel.showError("", false)
        }
    )

    /**
     * Edit playlist dialog
     */
    var changePlaylist by remember { mutableStateOf(false) }
    var changePlaylistInfo: FileItem? by remember { mutableStateOf(null) }
    EditPlaylistDialog(
        isShowing = changePlaylist,
        fileItem = changePlaylistInfo,
        onConfirm = { item, newName, newComment ->
            val res = PlaylistManager().run {
                load(item.docFile!!.uri)
                rename(newName, newComment)
            }

            if (!res) {
                scope.launch {
                    val msg = "Failed to edit playlist"
                    snackBarHostState.showSnackbar(msg, "OK")
                }
            }

            changePlaylist = false
            changePlaylistInfo = null
            viewModel.updateList()
        },
        onDismiss = {
            changePlaylist = false
            changePlaylistInfo = null
            viewModel.updateList()
        },
        onDelete = { item ->
            PlaylistManager.delete(item.name)
            changePlaylist = false
            changePlaylistInfo = null
            viewModel.updateList()
        }
    )

    /**
     * New playlist dialog
     */
    var newPlaylist by remember { mutableStateOf(false) }
    NewPlaylistDialog(
        isShowing = newPlaylist,
        onConfirm = { name, comment ->
            val res = PlaylistManager().run {
                new(name, comment)
            }

            if (res) {
                viewModel.updateList()
            } else {
                viewModel.showError(
                    message = context.getString(R.string.error_create_playlist),
                    isFatal = false
                )
            }

            newPlaylist = false
        },
        onDismiss = {
            newPlaylist = false
        }
    )

    LaunchedEffect(state.mediaPath) {
        if (state.mediaPath.isNotEmpty()) {
            viewModel.setupDataDir(
                name = context.getString(R.string.empty_playlist),
                comment = context.getString(R.string.empty_comment),
                onSuccess = { viewModel.updateList() },
                onError = { viewModel.showError(it, true) }
            )
        }
    }

    LifecycleStartEffect(Lifecycle.Event.ON_START) {
        Timber.d("Lifecycle onStart")
        onAction(HomeScreenAction.OnPlayerScreen)

        onStopOrDispose {
            Timber.d("Lifecycle onStop")
        }
    }

    LifecycleResumeEffect(Lifecycle.Event.ON_RESUME) {
        Timber.d("Lifecycle onResume")
        if (!PrefManager.safStoragePath.isNullOrEmpty()) {
            viewModel.updateList()
        }

        onPauseOrDispose {
            Timber.d("Lifecycle onPause")
        }
    }

    PlaylistMenuScreen(
        state = state,
        snackBarHostState = snackBarHostState,
        permissionState = StorageManager.checkPermissions(),
        onItemClick = { item ->
            if (item.isSpecial) {
                onAction(HomeScreenAction.OnFileList)
            } else {
                onAction(HomeScreenAction.OnPlaylist(item.docFile!!.uri.toString()))
            }
        },
        onItemLongClick = { item ->
            if (item.isSpecial) {
                onAction(HomeScreenAction.OnDocumentTree)
            } else {
                changePlaylistInfo = item
                changePlaylist = true
            }
        },
        onRefresh = viewModel::updateList,
        onNewPlaylist = { newPlaylist = true },
        onTitleClicked = { onAction(HomeScreenAction.OnPlayerScreen) },
        onDownload = { onAction(HomeScreenAction.OnSearchScreen) },
        onSettings = { onAction(HomeScreenAction.OnPreferenceScreen) },
        onRequestSettings = { onAction(HomeScreenAction.OnRequestSettings) },
        onRequestStorage = { onAction(HomeScreenAction.OnDocumentTree) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistMenuScreen(
    state: PlaylistMenuViewModel.PlaylistMenuState,
    snackBarHostState: SnackbarHostState,
    permissionState: Boolean,
    onItemClick: (item: FileItem) -> Unit,
    onItemLongClick: (item: FileItem) -> Unit,
    onRefresh: () -> Unit,
    onNewPlaylist: () -> Unit,
    onTitleClicked: () -> Unit,
    onDownload: () -> Unit,
    onSettings: () -> Unit,
    onRequestStorage: () -> Unit,
    onRequestSettings: () -> Unit
) {
    val scrollState = rememberLazyListState()
    val isScrolled = remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex > 0
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        topBar = {
            val topBarContainerColor = if (isScrolled.value) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f)
            } else {
                MaterialTheme.colorScheme.surface
            }

            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarContainerColor,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(
                        enabled = permissionState,
                        onClick = onDownload,
                        content = {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null)
                        }
                    )
                    IconButton(
                        enabled = permissionState,
                        onClick = onSettings,
                        content = {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                        }
                    )
                },
                title = {
                    TextButton(
                        enabled = permissionState,
                        onClick = onTitleClicked
                    ) {
                        ProvideTextStyle(
                            LocalTextStyle.current.merge(
                                TextStyle(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                )
                            )
                        ) {
                            Text(
                                text = themedText(text = stringResource(id = R.string.app_name)),
                                fontFamily = michromaFontFamily,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (permissionState) {
                ExtendedFloatingActionButton(
                    text = { Text(text = stringResource(id = R.string.menu_new_playlist)) },
                    icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                    expanded = !isScrolled.value,
                    onClick = onNewPlaylist
                )
            }
        }
    ) { paddingValues ->
        val pullRefreshState = rememberPullToRefreshState()
        if (pullRefreshState.isRefreshing) {
            LaunchedEffect(true) {
                onRefresh()
                pullRefreshState.endRefresh()
            }
        }

        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .nestedScroll(pullRefreshState.nestedScrollConnection),
            contentAlignment = Alignment.Center
        ) {
            if (state.playlistItems.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    state = scrollState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.playlistItems) { item ->
                        MenuCardItem(
                            item = item,
                            onClick = { onItemClick(item) },
                            onLongClick = { onItemLongClick(item) }
                        )
                    }
                }
            }

            if (!state.isLoading && !permissionState) {
                ErrorScreen(text = "Unable to access storage for Xmp to use") {
                    Button(
                        onClick = onRequestStorage,
                        content = { Text(text = "Set Directory") }
                    )
                    OutlinedButton(
                        onClick = onRequestSettings,
                        content = { Text(text = "Goto Settings") }
                    )
                }
            }

            PullToRefreshContainer(
                modifier = Modifier.align(Alignment.TopCenter),
                state = pullRefreshState
            )

            ProgressbarIndicator(isLoading = state.isLoading)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MenuCardItem(
    item: FileItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        ListItem(
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            leadingContent = {
                Icon(
                    imageVector = if (item.isSpecial) {
                        Icons.Default.Folder
                    } else {
                        Icons.AutoMirrored.Filled.List
                    },
                    contentDescription = null
                )
            },
            headlineContent = { Text(text = item.name) },
            supportingContent = {
                Text(
                    text = item.comment.ifEmpty {
                        stringResource(id = R.string.no_comment)
                    }
                )
            }
        )
    }
}

@Preview
@Composable
private fun Preview_MenuCardItem() {
    XmpTheme(useDarkTheme = true) {
        MenuCardItem(
            item = FileItem(
                name = "Menu Card Item",
                comment = "Menu Card Comment",
                docFile = null
            ),
            onClick = { },
            onLongClick = { }
        )
    }
}

@Preview
@Composable
private fun Preview_PlaylistMenuScreen() {
    XmpTheme(useDarkTheme = true) {
        PlaylistMenuScreen(
            state = PlaylistMenuViewModel.PlaylistMenuState(
                mediaPath = "sdcard\\some\\path",
                isLoading = true,
                playlistItems = List(15) {
                    FileItem(
                        isSpecial = it >= 1,
                        name = "Name $it",
                        comment = "Comment $it",
                        docFile = null
                    )
                }
            ),
            snackBarHostState = SnackbarHostState(),
            permissionState = false,
            onItemClick = {},
            onItemLongClick = {},
            onRefresh = {},
            onNewPlaylist = {},
            onTitleClicked = {},
            onDownload = {},
            onSettings = {},
            onRequestStorage = {},
            onRequestSettings = {}
        )
    }
}
