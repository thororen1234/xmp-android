package org.helllabs.android.xmp.compose.ui.home

import android.content.Intent
import android.net.Uri
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.EditPlaylistDialog
import org.helllabs.android.xmp.compose.components.ErrorScreen
import org.helllabs.android.xmp.compose.components.MessageDialog
import org.helllabs.android.xmp.compose.components.NewPlaylistDialog
import org.helllabs.android.xmp.compose.components.ProgressbarIndicator
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.theme.michromaFontFamily
import org.helllabs.android.xmp.compose.theme.themedText
import org.helllabs.android.xmp.compose.ui.player.PlayerActivity
import org.helllabs.android.xmp.core.PlaylistManager
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.core.StorageManager
import org.helllabs.android.xmp.model.FileItem
import org.helllabs.android.xmp.service.PlayerService
import timber.log.Timber

@Serializable
object NavigationHome

@Composable
fun HomeScreenImpl(
    viewModel: PlaylistMenuViewModel,
    snackBarHostState: SnackbarHostState,
    onNavFileList: () -> Unit,
    onNavPlaylist: (String) -> Unit,
    onNavPreferences: () -> Unit,
    onNavSearch: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var hasStorage by remember { mutableStateOf(false) }

    val appSettings = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.updateList()
    }

    val documentTreeResult = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        StorageManager.setPlaylistDirectory(uri).onSuccess {
            viewModel.setDefaultPath()
        }.onFailure {
            viewModel.showError(message = it.message ?: context.getString(R.string.error))
        }
    }

    val playerResult = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == 1) {
            result.data?.getStringExtra("error")?.let {
                Timber.w("Result with error: $it")
                scope.launch {
                    snackBarHostState.showSnackbar(message = it)
                }
            }
        }
        if (result.resultCode == 2) {
            viewModel.updateList()
        }
    }

    // Ask for Permissions
    LaunchedEffect(Unit) {
        val savedUri = PrefManager.safStoragePath.let {
            try {
                Uri.parse(it)
            } catch (e: NullPointerException) {
                null
            }
        }
        val persistedUris = context.contentResolver.persistedUriPermissions
        val hasAccess = persistedUris.any {
            it.uri == savedUri && it.isWritePermission
        }

        if (savedUri == null || !hasAccess) {
            documentTreeResult.launch(null)
        } else {
            viewModel.setDefaultPath()
        }
    }

    LaunchedEffect(state.mediaPath) {
        hasStorage = StorageManager.checkPermissions()
    }

    /**
     * Error message Dialog
     */
    MenuErrorDialog(
        state = state,
        onConfirm = { viewModel.showError("") }
    )

    /**
     * Edit playlist dialog
     */
    MenuEditDialog(
        state = state,
        onConfirm = { res ->
            if (!res) {
                scope.launch {
                    val msg = "Failed to edit playlist"
                    snackBarHostState.showSnackbar(msg, "OK")
                }
            }

            viewModel.editPlaylist(null)
        },
        onDelete = { item ->
            PlaylistManager.delete(item.name)
            viewModel.editPlaylist(null)
        },
        onDismiss = { viewModel.editPlaylist(null) },
    )

    /**
     * New playlist dialog
     */
    MenuNewPlaylist(
        state = state,
        onConfirm = { res ->
            if (res) {
                viewModel.updateList()
            } else {
                viewModel.showError(message = context.getString(R.string.error_create_playlist))
            }

            viewModel.newPlaylist(false)
        },
        onDismiss = { viewModel.newPlaylist(false) }
    )

    LaunchedEffect(state.mediaPath) {
        if (state.mediaPath.isNotEmpty()) {
            viewModel.setupDataDir(
                name = context.getString(R.string.empty_playlist),
                comment = context.getString(R.string.empty_comment),
            ).onSuccess {
                viewModel.updateList()
            }.onFailure {
                viewModel.showError(it.message ?: context.getString(R.string.error))
            }
        }
    }

    LifecycleStartEffect(Lifecycle.Event.ON_START) {
        Timber.d("Lifecycle onStart")
        if (PrefManager.startOnPlayer && PlayerService.isAlive) {
            Intent(context, PlayerActivity::class.java).also(playerResult::launch)
        }
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

    HomeScreen(
        state = state,
        snackBarHostState = snackBarHostState,
        permissionState = hasStorage,
        onItemClick = { item ->
            if (item.isSpecial) {
                onNavFileList()
            } else {
                onNavPlaylist(item.docFile!!.uri.toString())
            }
        },
        onItemLongClick = { item ->
            if (item.isSpecial) {
                documentTreeResult.launch(null)
            } else {
                viewModel.editPlaylist(item)
            }
        },
        onRefresh = viewModel::updateList,
        onNewPlaylist = { viewModel.newPlaylist(true) },
        onTitleClicked = {
            if (PrefManager.startOnPlayer && PlayerService.isAlive) {
                Intent(context, PlayerActivity::class.java).also(playerResult::launch)
            }
        },
        onDownload = onNavSearch,
        onSettings = onNavPreferences,
        onRequestSettings = {
            Intent().apply {
                action = ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.fromParts(
                    "package",
                    BuildConfig.APPLICATION_ID,
                    null
                )
                addCategory(Intent.CATEGORY_DEFAULT)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            }.also(appSettings::launch)
        },
        onRequestStorage = { documentTreeResult.launch(null) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    state: PlaylistMenuViewModel.PlaylistMenuState,
    snackBarHostState: SnackbarHostState,
    permissionState: Boolean,
    onDownload: () -> Unit,
    onItemClick: (item: FileItem) -> Unit,
    onItemLongClick: (item: FileItem) -> Unit,
    onNewPlaylist: () -> Unit,
    onRefresh: () -> Unit,
    onRequestSettings: () -> Unit,
    onRequestStorage: () -> Unit,
    onSettings: () -> Unit,
    onTitleClicked: () -> Unit
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

@Composable
private fun MenuErrorDialog(
    state: PlaylistMenuViewModel.PlaylistMenuState,
    onConfirm: () -> Unit
) {
    MessageDialog(
        isShowing = state.errorText != null,
        title = stringResource(id = R.string.error),
        text = state.errorText.orEmpty(),
        confirmText = stringResource(id = R.string.ok),
        onConfirm = onConfirm
    )
}

@Composable
private fun MenuEditDialog(
    state: PlaylistMenuViewModel.PlaylistMenuState,
    onConfirm: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (FileItem) -> Unit
) {
    EditPlaylistDialog(
        isShowing = state.editPlaylist != null,
        fileItem = state.editPlaylist,
        onConfirm = { item, newName, newComment ->
            val res = PlaylistManager().run {
                load(item.docFile!!.uri)
                rename(newName, newComment)
            }

            onConfirm(res.isSuccess)
        },
        onDismiss = onDismiss,
        onDelete = onDelete
    )
}

@Composable
private fun MenuNewPlaylist(
    state: PlaylistMenuViewModel.PlaylistMenuState,
    onConfirm: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    NewPlaylistDialog(
        isShowing = state.newPlaylist,
        onConfirm = { name, comment ->
            val res = PlaylistManager().run {
                new(name, comment)
            }
            onConfirm(res.isSuccess)
        },
        onDismiss = onDismiss
    )
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
        HomeScreen(
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
