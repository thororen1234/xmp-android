package org.helllabs.android.xmp.browser

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.browser.playlist.Playlist
import org.helllabs.android.xmp.browser.playlist.PlaylistItem
import org.helllabs.android.xmp.browser.playlist.PlaylistUtils
import org.helllabs.android.xmp.compose.components.ChangeLogDialog
import org.helllabs.android.xmp.compose.components.EditPlaylistDialog
import org.helllabs.android.xmp.compose.components.TextInputDialog
import org.helllabs.android.xmp.compose.components.pullrefresh.ExperimentalMaterialApi
import org.helllabs.android.xmp.compose.components.pullrefresh.PullRefreshIndicator
import org.helllabs.android.xmp.compose.components.pullrefresh.pullRefresh
import org.helllabs.android.xmp.compose.components.pullrefresh.rememberPullRefreshState
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.theme.michromaFontFamily
import org.helllabs.android.xmp.compose.theme.themedText
import org.helllabs.android.xmp.modarchive.Search
import org.helllabs.android.xmp.player.PlayerActivity
import org.helllabs.android.xmp.preferences.Preferences
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.util.FileUtils.writeToFile
import org.helllabs.android.xmp.util.Message.error
import org.helllabs.android.xmp.util.Message.fatalError
import timber.log.Timber
import java.io.File
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

class PlaylistMenuViewModel : ViewModel() {
    data class PlaylistMenuState(
        val isRefreshing: Boolean = false,
        val mediaPath: String = "",
        val playlistItems: List<PlaylistItem> = listOf()
    )

    private val _uiState = MutableStateFlow(PlaylistMenuState())
    val uiState = _uiState.asStateFlow()

    fun checkStorage(): Boolean {
        val state = Environment.getExternalStorageState()
        val result = if (Environment.MEDIA_MOUNTED == state ||
            Environment.MEDIA_MOUNTED_READ_ONLY == state
        ) {
            true
        } else {
            Timber.e("External storage state error: $state")
            false
        }

        return result
    }

    // Create application directory and populate with empty playlist
    fun setupDataDir(context: Context) {
        if (!PrefManager.DATA_DIR.isDirectory) {
            if (PrefManager.DATA_DIR.mkdirs()) {
                PlaylistUtils.createEmptyPlaylist(
                    context,
                    context.getString(R.string.empty_playlist),
                    context.getString(R.string.empty_comment)
                )
            } else {
                fatalError(context as Activity, context.getString(R.string.error_datadir))
            }
        }
    }

    fun updateList(context: Context) {
        _uiState.update { it.copy(mediaPath = PrefManager.mediaPath) }

        val items = mutableListOf<PlaylistItem>()
        val browserItem = PlaylistItem(
            type = PlaylistItem.TYPE_SPECIAL,
            name = "File browser",
            comment = "Files in ${uiState.value.mediaPath}"
        )
        items.add(browserItem)

        PlaylistUtils.listNoSuffix().forEachIndexed { index, name ->
            val item = PlaylistItem(
                PlaylistItem.TYPE_PLAYLIST,
                name,
                Playlist.readComment(context, name)
            )
            item.id = index + 1

            items.add(item)
        }

        _uiState.update { it.copy(playlistItems = items) }
    }
}

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

        // TODO go to player if we're playing something
//        if (intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0) {
//            Intent(this, PlayerActivity::class.java).also(::startActivity)
//            return
//        }

        if (!viewModel.checkStorage()) {
            fatalError(this, getString(R.string.error_storage))
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
            var changePlaylistInfo: PlaylistItem? by remember { mutableStateOf(null) }

            LaunchedEffect(storagePermission) {
                if (storagePermission.status.isGranted) {
                    if (BuildConfig.VERSION_CODE < PrefManager.changeLogVersion) {
                        changeLogDialog = true
                    }

                    viewModel.setupDataDir(context)
                    viewModel.updateList(context)
                }
            }

            XmpTheme {
                ChangeLogDialog(
                    isShowing = changeLogDialog,
                    onDismiss = { PrefManager.changeLogVersion = BuildConfig.VERSION_CODE }
                )

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

                EditPlaylistDialog(
                    isShowing = changePlaylist,
                    playlistItem = changePlaylistInfo,
                    onConfirm = { oldName, newName, oldComment, newComment ->
                        if (oldComment != newComment) {
                            val value = newComment.replace("\n", " ")
                            val file = File(PrefManager.DATA_DIR, oldName + Playlist.COMMENT_SUFFIX)
                            try {
                                file.delete()
                                file.createNewFile()
                                writeToFile(file, value)
                            } catch (e: IOException) {
                                error(context, getString(R.string.error_edit_comment))
                            }
                        }

                        if (oldName != newName) {
                            if (!Playlist.rename(context, oldName, newName)) {
                                error(context, getString(R.string.error_rename_playlist))
                                return@EditPlaylistDialog
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
                        Playlist.delete(context, name)
                        changePlaylist = false
                        viewModel.updateList(context)
                    }
                )

                PlaylistMenuScreen(
                    state = state,
                    permissionState = storagePermission.status.isGranted,
                    permissionRationale = storagePermission.status.shouldShowRationale,
                    onItemClick = { item ->
                        if (item.type == PlaylistItem.TYPE_SPECIAL) {
                            playlistResult.launch(
                                Intent(context, FilelistActivity::class.java)
                            )
                            return@PlaylistMenuScreen
                        }

                        playlistResult.launch(
                            Intent(context, PlaylistActivity::class.java).apply {
                                putExtra("name", item.name)
                            }
                        )
                    },
                    onItemLongClick = { item ->
                        if (item.type == PlaylistItem.TYPE_SPECIAL) {
                            changeMediaPath = true
                            return@PlaylistMenuScreen
                        }

                        changePlaylistInfo = item
                        changePlaylist = true
                    },
                    onRefresh = {
                        viewModel.updateList(this)
                    },
                    onNewPlaylist = {
                        PlaylistUtils.newPlaylistDialog(this) {
                            viewModel.updateList(this)
                        }
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
                    state = scrollState
                ) {
                    items(state.playlistItems) { item ->
                        Card(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        onItemClick(item)
                                    },
                                    onLongClick = {
                                        onItemLongClick(item)
                                    }
                                )
                        ) {
                            ListItem(
                                colors = ListItemDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                leadingContent = {
                                    val icon = if (item.type == PlaylistItem.TYPE_SPECIAL) {
                                        Icons.Default.Folder
                                    } else {
                                        Icons.Default.List
                                    }

                                    Icon(
                                        imageVector = icon,
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
    val list = List(15) {
        PlaylistItem(
            type = if (it < 1) PlaylistItem.TYPE_DIRECTORY else PlaylistItem.TYPE_SPECIAL,
            name = "Name $it",
            comment = "Comment $it"
        )
    }
    val state = PlaylistMenuViewModel.PlaylistMenuState(
        isRefreshing = false,
        mediaPath = "sdcard\\some\\path",
        playlistItems = list
    )
    XmpTheme {
        PlaylistMenuScreen(
            state = state,
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
