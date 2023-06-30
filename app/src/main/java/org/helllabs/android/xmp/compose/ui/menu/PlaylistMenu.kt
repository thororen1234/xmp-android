package org.helllabs.android.xmp.compose.ui.menu

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.browser.playlist.Playlist
import org.helllabs.android.xmp.browser.playlist.PlaylistItem
import org.helllabs.android.xmp.browser.playlist.PlaylistUtils
import org.helllabs.android.xmp.compose.components.ChangeLogDialog
import org.helllabs.android.xmp.compose.components.EditPlaylistDialog
import org.helllabs.android.xmp.compose.components.MessageDialog
import org.helllabs.android.xmp.compose.components.NewPlaylistDialog
import org.helllabs.android.xmp.compose.components.TextInputDialog
import org.helllabs.android.xmp.compose.components.pullrefresh.ExperimentalMaterialApi
import org.helllabs.android.xmp.compose.components.pullrefresh.PullRefreshIndicator
import org.helllabs.android.xmp.compose.components.pullrefresh.pullRefresh
import org.helllabs.android.xmp.compose.components.pullrefresh.rememberPullRefreshState
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.theme.michromaFontFamily
import org.helllabs.android.xmp.compose.theme.themedText
import org.helllabs.android.xmp.compose.ui.filelist.FileListActivity
import org.helllabs.android.xmp.compose.ui.playlist.PlaylistActivity
import org.helllabs.android.xmp.compose.ui.preferences.Preferences
import org.helllabs.android.xmp.compose.ui.search.Search
import org.helllabs.android.xmp.player.PlayerActivity
import org.helllabs.android.xmp.service.PlayerService
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

class PlaylistMenu : ComponentActivity() {

    private val viewModel by viewModels<PlaylistMenuViewModel>()

    private val settingsContract = ActivityResultContracts.StartActivityForResult()
    private val settingsResult = registerForActivityResult(settingsContract) {
        viewModel.updateList(this)
    }

    private val playlistContract = ActivityResultContracts.StartActivityForResult()
    private val playlistResult = registerForActivityResult(playlistContract) {
        viewModel.updateList(this)
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")

        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (PlayerService.isAlive && PrefManager.startOnPlayer) {
            if (PrefManager.startOnPlayer && PlayerService.isAlive) {
                Intent(this, PlayerActivity::class.java).also(::startActivity)
            }
        }

        if (!viewModel.checkStorage()) {
            val message = getString(R.string.error_storage)
            viewModel.showError(message, true)
        }

        setContent {
            val context = LocalContext.current
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val storagePermission =
                rememberPermissionState(Manifest.permission.WRITE_EXTERNAL_STORAGE)

            // Dialog visible states
            var changeLogDialog by remember { mutableStateOf(false) }
            var changeMediaPath by remember { mutableStateOf(false) }
            var changePlaylist by remember { mutableStateOf(false) }
            var newPlaylist by remember { mutableStateOf(false) }
            var changePlaylistInfo: PlaylistItem? by remember { mutableStateOf(null) }

            // Ask for Permissions
            LaunchedEffect(Unit) {
                if (!storagePermission.status.isGranted) {
                    storagePermission.launchPermissionRequest()
                }
            }

            LaunchedEffect(storagePermission) {
                if (storagePermission.status.isGranted) {
                    if (BuildConfig.VERSION_CODE < PrefManager.changeLogVersion) {
                        changeLogDialog = true
                    }

                    viewModel.setupDataDir(
                        name = getString(R.string.empty_playlist),
                        comment = getString(R.string.empty_comment),
                        onSuccess = {
                            viewModel.updateList(context)
                        },
                        onError = {
                            val message = getString(R.string.error_datadir)
                            viewModel.showError(message, true)
                        }
                    )
                }
            }

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
            ChangeLogDialog(
                isShowing = changeLogDialog,
                onDismiss = { PrefManager.changeLogVersion = BuildConfig.VERSION_CODE }
            )

            /**
             * Change default directory dialog
             */
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
            NewPlaylistDialog(
                isShowing = newPlaylist,
                onConfirm = { name, comment ->
                    PlaylistUtils.createEmptyPlaylist(
                        name = name,
                        comment = comment,
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

            XmpTheme {
                PlaylistMenuScreen(
                    state = state,
                    permissionState = storagePermission.status.isGranted,
                    permissionRationale = storagePermission.status.shouldShowRationale,
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
                            Intent(this, PlayerActivity::class.java).also(::startActivity)
                        }
                    },
                    onDownload = {
                        Intent(this, Search::class.java).also(::startActivity)
                    },
                    onSettings = {
                        settingsResult.launch(Intent(this, Preferences::class.java))
                    },
                    onRequestPermission = {
                        if (storagePermission.status.shouldShowRationale) {
                            storagePermission.launchPermissionRequest()
                        } else {
                            val intent = Intent().apply {
                                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                                addCategory(Intent.CATEGORY_DEFAULT)
                                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                            }
                            playlistResult.launch(intent)
                        }
                    }
                )
            }
        }
    }

    public override fun onResume() {
        super.onResume()
        viewModel.updateList(this)
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class,
    ExperimentalFoundationApi::class
)
@Composable
private fun PlaylistMenuScreen(
    state: PlaylistMenuViewModel.PlaylistMenuState,
    permissionState: Boolean,
    permissionRationale: Boolean,
    onItemClick: (item: PlaylistItem) -> Unit,
    onItemLongClick: (item: PlaylistItem) -> Unit,
    onRefresh: () -> Unit,
    onNewPlaylist: () -> Unit,
    onTitleClicked: () -> Unit,
    onDownload: () -> Unit,
    onSettings: () -> Unit,
    onRequestPermission: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberLazyListState()
    val isScrolled = remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex > 0
        }
    }

    Scaffold(
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
                        Text(
                            text = themedText(text = stringResource(id = R.string.app_name)),
                            fontFamily = michromaFontFamily,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            // IDK how I found this "Clip to Padding" hack...
                            style = TextStyle(baselineShift = BaselineShift(.3f))
                        )
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
        if (permissionState) {
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .pullRefresh(pullState)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    state = scrollState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.playlistItems) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { onItemClick(item) },
                                    onLongClick = { onItemLongClick(item) }
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
                                            Icons.Default.List
                                        },
                                        contentDescription = null
                                    )
                                },
                                headlineContent = { Text(text = item.name) },
                                supportingContent = { Text(text = item.comment) }
                            )
                        }
                    }
                }

                PullRefreshIndicator(refreshing, pullState, Modifier.align(Alignment.TopCenter))
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
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview_PlaylistMenuScreen() {
    XmpTheme {
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
            permissionState = true,
            permissionRationale = true,
            onItemClick = {},
            onItemLongClick = {},
            onRefresh = {},
            onNewPlaylist = {},
            onTitleClicked = {},
            onDownload = {},
            onSettings = {},
            onRequestPermission = {}
        )
    }
}
