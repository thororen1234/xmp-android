package org.helllabs.android.xmp.compose.ui.menu

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.StorageManager
import org.helllabs.android.xmp.compose.components.ChangeLogDialog
import org.helllabs.android.xmp.compose.components.EditPlaylistDialog
import org.helllabs.android.xmp.compose.components.MessageDialog
import org.helllabs.android.xmp.compose.components.NewPlaylistDialog
import org.helllabs.android.xmp.compose.components.TextInputDialog
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.theme.michromaFontFamily
import org.helllabs.android.xmp.compose.theme.themedText
import org.helllabs.android.xmp.compose.ui.filelist.FileListActivity
import org.helllabs.android.xmp.compose.ui.menu.components.MenuCardItem
import org.helllabs.android.xmp.compose.ui.player.PlayerActivity
import org.helllabs.android.xmp.compose.ui.playlist.PlaylistActivity
import org.helllabs.android.xmp.compose.ui.preferences.Preferences
import org.helllabs.android.xmp.compose.ui.search.Search
import org.helllabs.android.xmp.core.PlaylistUtils
import org.helllabs.android.xmp.model.Playlist
import org.helllabs.android.xmp.model.PlaylistItem
import org.helllabs.android.xmp.service.PlayerService
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

class PlaylistMenu : ComponentActivity() {

    private val viewModel by viewModels<PlaylistMenuViewModel>()

    private var snackBarHostState = SnackbarHostState()

    private val settingsContract = ActivityResultContracts.StartActivityForResult()
    private val settingsResult = registerForActivityResult(settingsContract) {
        viewModel.updateList(this)
    }

    private val playlistContract = ActivityResultContracts.StartActivityForResult()
    private val playlistResult = registerForActivityResult(playlistContract) {
        viewModel.updateList(this)
    }

    private val playerContract = ActivityResultContracts.StartActivityForResult()
    private var playerResult = registerForActivityResult(playerContract) { result ->
        if (result.resultCode == 1) {
            result.data?.getStringExtra("error")?.let {
                Timber.w("Result with error: $it")
                lifecycleScope.launch {
                    snackBarHostState.showSnackbar(message = it)
                }
            }
        }
        if (result.resultCode == 2) {
            viewModel.updateList(this)
        }
    }

    private val documentTreeContract = ActivityResultContracts.OpenDocumentTree()
    private val documentTreeResult = registerForActivityResult(documentTreeContract) { uri ->
        StorageManager.setPlaylistDirectory(
            context = this,
            uri = uri,
            onSuccess = {
                // Refresh the list
                viewModel.setDefaultPath(this)
            },
            onError = {
                viewModel.showError(message = it, isFatal = true)
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.auto(
                Color.Transparent.toArgb(),
                Color.Transparent.toArgb()
            )
        )

        // TODO does this even work?
        if (PlayerService.isAlive && PrefManager.startOnPlayer) {
            if (intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0) {
                Intent(this, PlayerActivity::class.java).also(::startActivity)
            }
        }

        if (!viewModel.checkStorage()) {
            val message = getString(R.string.error_storage)
            viewModel.showError(message, true)
        }

        Timber.d("onCreate")
        setContent {
            val context = LocalContext.current
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            // User theme setting? Check this out
            // https://github.com/android/nowinandroid/commit/a3ee09ec3e53412e65c1f01d2e8588fecd2b7157

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
                        finish()
                    }

                    viewModel.showError("", false)
                }
            )

            /**
             * Changelog dialog
             */
            var changeLogDialog by remember { mutableStateOf(false) }
            ChangeLogDialog(
                isShowing = changeLogDialog,
                onDismiss = { PrefManager.changeLogVersion = BuildConfig.VERSION_CODE }
            )

            /**
             * Change default directory dialog
             */
            // TODO use SAF file picker
            var changeMediaPath by remember { mutableStateOf(false) }
            TextInputDialog(
                isShowing = changeMediaPath,
                icon = Icons.Default.Folder,
                title = "Change directory",
                text = "Enter the mod directory:",
                defaultText = state.mediaPath,
                onConfirm = { value ->
                    PrefManager.mediaPath = value
                    viewModel.updateList(this)
                    changeMediaPath = false
                },
                onDismiss = {
                    changeMediaPath = false
                }
            )

            /**
             * Edit playlist dialog
             */
            var changePlaylist by remember { mutableStateOf(false) }
            var changePlaylistInfo: PlaylistItem? by remember { mutableStateOf(null) }
            EditPlaylistDialog(
                isShowing = changePlaylist,
                playlistItem = changePlaylistInfo,
                onConfirm = { oldName, newName, oldComment, newComment ->
                    if (oldComment != newComment) {
                        with(viewModel) {
                            editComment(oldName, newComment) {
                                showError(
                                    message = getString(R.string.error_edit_comment),
                                    isFatal = false
                                )
                            }
                        }
                    }

                    if (oldName != newName) {
                        with(viewModel) {
                            editPlaylist(oldName, newName) {
                                showError(
                                    message = getString(R.string.error_rename_playlist),
                                    isFatal = false
                                )
                            }
                        }
                    }

                    changePlaylist = false
                    viewModel.updateList(context)
                },
                onDismiss = {
                    changePlaylist = false
                    viewModel.updateList(context)
                },
                onDelete = { name ->
                    Playlist.delete(name)
                    changePlaylist = false
                    viewModel.updateList(context)
                }
            )

            /**
             * New playlist dialog
             */
            var newPlaylist by remember { mutableStateOf(false) }
            NewPlaylistDialog(
                isShowing = newPlaylist,
                onConfirm = { name, comment ->
                    val playlistsDir = StorageManager.getPlaylistDirectory(context)
                    PlaylistUtils.createEmptyPlaylist2(
                        context,
                        playlistsDir?.uri,
                        name,
                        comment,
                        onSuccess = {
                            viewModel.updateList(this)
                        },
                        onError = {
                            viewModel.showError(
                                message = getString(R.string.error_create_playlist),
                                isFatal = false
                            )
                        }
                    )

                    newPlaylist = false
                },
                onDismiss = {
                    newPlaylist = false
                }
            )

            // Ask for Permissions
            LaunchedEffect(Unit) {
                val savedUri = PrefManager.safStoragePath.let {
                    try {
                        Uri.parse(it)
                    } catch (e: NullPointerException) {
                        null
                    }
                }
                val persistedUris = contentResolver.persistedUriPermissions
                val hasAccess = persistedUris.any { it.uri == savedUri && it.isWritePermission }

                if (savedUri == null || !hasAccess) {
                    documentTreeResult.launch(null)
                } else {
                    viewModel.setDefaultPath(context)
                }
            }

            LaunchedEffect(state.mediaPath) {
                if (state.mediaPath.isNotEmpty()) {
                    if (BuildConfig.VERSION_CODE < PrefManager.changeLogVersion) {
                        changeLogDialog = true
                    }

                    viewModel.setupDataDir(
                        context = context,
                        name = getString(R.string.empty_playlist),
                        comment = getString(R.string.empty_comment),
                        onSuccess = {
                            viewModel.updateList(context)
                        },
                        onError = {
                            viewModel.showError(it, true)
                        }
                    )
                }
            }

            XmpTheme {
                PlaylistMenuScreen(
                    state = state,
                    snackbarHostState = snackBarHostState,
                    permissionState = state.mediaPath.isNotEmpty(), // TODO not really a state
                    permissionRationale = false, // TODO: permission.status.shouldShowRationale
                    onItemClick = { item ->
                        if (item.isSpecial) {
                            playlistResult.launch(
                                Intent(context, FileListActivity::class.java)
                            )
                        } else {
                            playlistResult.launch(
                                Intent(context, PlaylistActivity::class.java).apply {
                                    putExtra("name", item.name)
                                }
                            )
                        }
                    },
                    onItemLongClick = { item ->
                        if (item.isSpecial) {
                            changeMediaPath = true
                        } else {
                            changePlaylistInfo = item
                            changePlaylist = true
                        }
                    },
                    onRefresh = {
                        viewModel.updateList(this)
                    },
                    onNewPlaylist = {
                        newPlaylist = true
                    },
                    onTitleClicked = {
                        if (PrefManager.startOnPlayer && PlayerService.isAlive) {
                            Intent(this, PlayerActivity::class.java).also {
                                playerResult.launch(it)
                            }
                        }
                    },
                    onDownload = {
                        Intent(this, Search::class.java).also(::startActivity)
                    },
                    onSettings = {
                        settingsResult.launch(Intent(this, Preferences::class.java))
                    },
                    onRequestPermission = {
                        TODO("onRequestPermission")
//                        if (permission.status.shouldShowRationale) {
//                            permission.launchPermissionRequest()
//                        } else {
//                            val intent = Intent().apply {
//                                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
//                                data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
//                                addCategory(Intent.CATEGORY_DEFAULT)
//                                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
//                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
//                            }
//                            playlistResult.launch(intent)
//                        }
                    },
                    onRequestStorage = {
                        documentTreeResult.launch(null)
                    }
                )
            }
        }
    }

    public override fun onResume() {
        super.onResume()
        if (!PrefManager.safStoragePath.isNullOrEmpty()) {
            viewModel.updateList(this)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistMenuScreen(
    state: PlaylistMenuViewModel.PlaylistMenuState,
    snackbarHostState: SnackbarHostState,
    permissionState: Boolean,
    permissionRationale: Boolean,
    onItemClick: (item: PlaylistItem) -> Unit,
    onItemLongClick: (item: PlaylistItem) -> Unit,
    onRefresh: () -> Unit,
    onNewPlaylist: () -> Unit,
    onTitleClicked: () -> Unit,
    onDownload: () -> Unit,
    onSettings: () -> Unit,
    onRequestPermission: () -> Unit,
    onRequestStorage: () -> Unit
) {
    val scrollState = rememberLazyListState()
    val isScrolled = remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex > 0
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                        onClick = onDownload
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null)
                    }
                    IconButton(
                        enabled = permissionState,
                        onClick = onSettings
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                    }
                },
                title = {
                    TextButton(
                        enabled = permissionState,
                        onClick = onTitleClicked
                    ) {
                        ProvideTextStyle(
                            LocalTextStyle.current.merge(
                                TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
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

        if (permissionState) {
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .nestedScroll(pullRefreshState.nestedScrollConnection)
            ) {
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
                            onLongClick = {
                                onItemLongClick(item)
                            }
                        )
                    }
                }

                PullToRefreshContainer(
                    modifier = Modifier.align(Alignment.TopCenter),
                    state = pullRefreshState
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Card {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Permissions Not Granted")
                        OutlinedButton(
                            modifier = Modifier.padding(top = 16.dp),
                            onClick = onRequestPermission
                        ) {
                            val text = if (permissionRationale) {
                                "Grant Write Permission"
                            } else {
                                "Goto Settings"
                            }
                            Text(text = text)
                        }
                        OutlinedButton(
                            modifier = Modifier.padding(top = 16.dp),
                            onClick = onRequestStorage
                        ) {
                            Text(text = "Set Directory")
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview_PlaylistMenuScreen() {
    XmpTheme(useDarkTheme = true) {
        PlaylistMenuScreen(
            state = PlaylistMenuViewModel.PlaylistMenuState(
                isRefreshing = false,
                mediaPath = "sdcard\\some\\path",
                playlistItems = List(15) {
                    PlaylistItem(
                        type = if (it < 1) PlaylistItem.TYPE_DIRECTORY else PlaylistItem.TYPE_SPECIAL,
                        name = "Name $it",
                        comment = "Comment $it"
                    )
                }
            ),
            snackbarHostState = SnackbarHostState(),
            permissionState = true,
            permissionRationale = true,
            onItemClick = {},
            onItemLongClick = {},
            onRefresh = {},
            onNewPlaylist = {},
            onTitleClicked = {},
            onDownload = {},
            onSettings = {},
            onRequestPermission = {},
            onRequestStorage = {}
        )
    }
}
