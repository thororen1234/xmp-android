package org.helllabs.android.xmp.compose

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.compose.components.ListDialog
import org.helllabs.android.xmp.compose.components.MessageDialog
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.filelist.FileListScreen
import org.helllabs.android.xmp.compose.ui.filelist.FileListViewModel
import org.helllabs.android.xmp.compose.ui.filelist.NavFileList
import org.helllabs.android.xmp.compose.ui.home.HomeScreen
import org.helllabs.android.xmp.compose.ui.home.HomeScreenAction
import org.helllabs.android.xmp.compose.ui.home.NavigationHome
import org.helllabs.android.xmp.compose.ui.home.PlaylistMenuViewModel
import org.helllabs.android.xmp.compose.ui.player.PlayerActivity
import org.helllabs.android.xmp.compose.ui.playlist.NavPlaylist
import org.helllabs.android.xmp.compose.ui.playlist.PlaylistActivityViewModel
import org.helllabs.android.xmp.compose.ui.playlist.PlaylistScreen
import org.helllabs.android.xmp.compose.ui.preferences.AboutScreen
import org.helllabs.android.xmp.compose.ui.preferences.FormatsScreen
import org.helllabs.android.xmp.compose.ui.preferences.NavPreferenceAbout
import org.helllabs.android.xmp.compose.ui.preferences.NavPreferenceFormats
import org.helllabs.android.xmp.compose.ui.preferences.NavPreferences
import org.helllabs.android.xmp.compose.ui.preferences.PreferencesScreen
import org.helllabs.android.xmp.compose.ui.search.ErrorScreen
import org.helllabs.android.xmp.compose.ui.search.NavSearch
import org.helllabs.android.xmp.compose.ui.search.NavSearchError
import org.helllabs.android.xmp.compose.ui.search.NavSearchHistory
import org.helllabs.android.xmp.compose.ui.search.SearchHistoryScreen
import org.helllabs.android.xmp.compose.ui.search.SearchScreen
import org.helllabs.android.xmp.compose.ui.search.result.ModuleResultScreen
import org.helllabs.android.xmp.compose.ui.search.result.NavSearchResult
import org.helllabs.android.xmp.compose.ui.search.result.NavSearchTitleResult
import org.helllabs.android.xmp.compose.ui.search.result.ResultViewModel
import org.helllabs.android.xmp.compose.ui.search.result.SearchResultViewModel
import org.helllabs.android.xmp.compose.ui.search.result.TitleResultScreen
import org.helllabs.android.xmp.core.PlaylistManager
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.core.StorageManager
import org.helllabs.android.xmp.model.DropDownSelection
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.service.PlayerBinder
import org.helllabs.android.xmp.service.PlayerService
import timber.log.Timber

class MainActivity : ComponentActivity() {

    private val snackBarHostState = SnackbarHostState()

    private var mAddList: List<Uri> = listOf()
    private var mModPlayer: PlayerService? = null

    // Connection
    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            mModPlayer = (service as PlayerBinder).getService()
            try {
                mModPlayer!!.add(mAddList)
                mAddList = listOf()
            } catch (e: RemoteException) {
                showSnack(getString(R.string.error_adding_mod))
            }
            unbindService(this)
        }

        override fun onServiceDisconnected(className: ComponentName) {
            mModPlayer = null
        }
    }

    internal fun showSnack(message: String, actionLabel: String? = null) = lifecycleScope.launch {
        snackBarHostState.showSnackbar(message = message, actionLabel = actionLabel)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.d("onCreate")

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb())
        )

        setContent {
            val scope = rememberCoroutineScope()

            XmpTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = NavigationHome
                ) {
                    composable<NavigationHome> {
                        val viewModel by viewModels<PlaylistMenuViewModel>()

                        val appSettings = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.StartActivityForResult()
                        ) {
                            viewModel.updateList()
                        }

                        val documentTreeResult = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.OpenDocumentTree()
                        ) { uri ->
                            StorageManager.setPlaylistDirectory(
                                uri = uri,
                                onSuccess = viewModel::setDefaultPath,
                                onError = {
                                    viewModel.showError(message = it, isFatal = true)
                                }
                            )
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
                            val persistedUris = contentResolver.persistedUriPermissions
                            val hasAccess = persistedUris.any {
                                it.uri == savedUri && it.isWritePermission
                            }

                            if (savedUri == null || !hasAccess) {
                                documentTreeResult.launch(null)
                            } else {
                                viewModel.setDefaultPath()
                            }
                        }

                        HomeScreen(
                            viewModel = viewModel,
                            snackBarHostState = snackBarHostState,
                            onAction = {
                                when (it) {
                                    HomeScreenAction.OnFatalError ->
                                        finishAffinity()

                                    HomeScreenAction.OnPlayerScreen ->
                                        if (PrefManager.startOnPlayer && PlayerService.isAlive) {
                                            Intent(
                                                this@MainActivity,
                                                PlayerActivity::class.java
                                            ).also(playerResult::launch)
                                        }

                                    HomeScreenAction.OnPreferenceScreen ->
                                        NavPreferences.also(navController::navigate)

                                    HomeScreenAction.OnSearchScreen ->
                                        NavSearch.also(navController::navigate)

                                    HomeScreenAction.OnRequestSettings -> Intent().apply {
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

                                    HomeScreenAction.OnDocumentTree ->
                                        documentTreeResult.launch(null)

                                    HomeScreenAction.OnFileList ->
                                        NavFileList.also(navController::navigate)

                                    is HomeScreenAction.OnPlaylist ->
                                        NavPlaylist(it.name).also(navController::navigate)
                                }
                            }
                        )
                    }
                    composable<NavFileList> {
                        val viewModel by viewModels<FileListViewModel>()
                        val state by viewModel.uiState.collectAsStateWithLifecycle()

                        val playerResult = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.StartActivityForResult()
                        ) { result ->
                            if (result.resultCode == 1) {
                                result.data?.getStringExtra("error")?.let {
                                    Timber.w("Result with error: $it")
                                    showSnack(it)
                                }
                            }
                            if (result.resultCode == 2) {
                                viewModel.onRefresh()
                            }
                        }

                        LifecycleResumeEffect(Lifecycle.Event.ON_RESUME) {
                            Timber.d("Lifecycle onResume")
                            viewModel.onRefresh()

                            onPauseOrDispose {
                                Timber.d("Lifecycle onPause")
                            }
                        }

                        LaunchedEffect(Unit) {
                            viewModel.softError.collectLatest {
                                showSnack(it)
                            }
                        }

                        // On back pressed handler
                        val callback = remember {
                            object : OnBackPressedCallback(true) {
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
                        }

                        // Set up and override on back pressed.
                        DisposableEffect(onBackPressedDispatcher) {
                            onBackPressedDispatcher.addCallback(callback)
                            onDispose {
                                callback.remove()
                            }
                        }

                        //todo: Ugly
                        /**
                         * Playlist choice dialog
                         */
                        ListDialog(
                            isShowing = viewModel.playlistChoice != null,
                            icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                            title = stringResource(id = R.string.msg_select_playlist),
                            list = viewModel.playlistList,
                            onConfirm = viewModel::addToPlaylist,
                            onDismiss = { viewModel.playlistChoice = null },
                            onEmpty = {
                                showSnack(message = getString(R.string.msg_no_playlists))
                                viewModel.playlistChoice = null
                            }
                        )

                        /**
                         * Delete directory dialog
                         */
                        MessageDialog(
                            isShowing = viewModel.deleteDirChoice != null,
                            icon = Icons.Default.QuestionMark,
                            title = "Delete directory",
                            text = "Are you sure you want to delete " +
                                "${StorageManager.getFileName(
                                    viewModel.deleteDirChoice
                                )} and all its contents?",
                            confirmText = stringResource(id = R.string.menu_delete),
                            onConfirm = {
                                val res =
                                    StorageManager.deleteFileOrDirectory(viewModel.deleteDirChoice)
                                if (!res) {
                                    showSnack("Unable to delete directory")
                                }
                                viewModel.deleteDirChoice = null
                            },
                            onDismiss = {
                                viewModel.deleteDirChoice = null
                            }
                        )

                        /**
                         * Delete file dialog
                         */
                        MessageDialog(
                            isShowing = viewModel.deleteFileChoice != null,
                            icon = Icons.Default.QuestionMark,
                            title = "Delete File",
                            text = "Are you sure you want to delete " +
                                "${StorageManager.getFileName(viewModel.deleteFileChoice)}?",
                            confirmText = stringResource(id = R.string.menu_delete),
                            onConfirm = {
                                val res =
                                    StorageManager.deleteFileOrDirectory(viewModel.deleteFileChoice)
                                if (!res) {
                                    showSnack("Unable to delete item")
                                }
                                viewModel.onRefresh()
                                viewModel.deleteFileChoice = null
                            },
                            onDismiss = {
                                viewModel.deleteFileChoice = null
                            }
                        )

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
                            onPlayAll = {
                                lifecycleScope.launch {
                                    onPlayAll(
                                        modList = viewModel.onAllFiles(),
                                        isShuffleMode = viewModel.uiState.value.isShuffle,
                                        isLoopMode = viewModel.uiState.value.isLoop,
                                        result = playerResult
                                    )
                                }
                            },
                            onCrumbMenu = { selection ->
                                when (selection) {
                                    DropDownSelection.ADD_TO_PLAYLIST -> {
                                        viewModel.playlistList = PlaylistManager.listPlaylists()
                                        viewModel.playlistChoice = viewModel.currentPath
                                    }

                                    DropDownSelection.ADD_TO_QUEUE -> addToQueue(
                                        list = viewModel.getItems(),
                                        isShuffleMode = viewModel.uiState.value.isShuffle,
                                        isLoopMode = viewModel.uiState.value.isLoop,
                                        result = playerResult
                                    )

                                    DropDownSelection.DIR_PLAY_CONTENTS -> playModule(
                                        modList = viewModel.getItems(),
                                        isShuffleMode = viewModel.uiState.value.isShuffle,
                                        isLoopMode = viewModel.uiState.value.isLoop,
                                        result = playerResult
                                    )

                                    else -> Unit
                                }
                            },
                            onCrumbClick = { crumb, _ ->
                                viewModel.onNavigate(crumb.path)
                            },
                            onItemClick = { item, index ->
                                if (item.docFile!!.isFile()) {
                                    onItemClick(
                                        items = viewModel.getItems(),
                                        position = index,
                                        isShuffleMode = viewModel.uiState.value.isShuffle,
                                        isLoopMode = viewModel.uiState.value.isLoop,
                                        result = playerResult
                                    )
                                } else {
                                    viewModel.onNavigate(item.docFile)
                                }
                            },
                            onItemLongClick = { item, index, sel ->
                                when (sel) {
                                    DropDownSelection.DELETE -> {
                                        if (item.isFile) {
                                            viewModel.deleteFileChoice = item.docFile
                                        } else {
                                            viewModel.deleteDirChoice = item.docFile
                                        }
                                    }

                                    DropDownSelection.ADD_TO_PLAYLIST -> {
                                        viewModel.playlistList = PlaylistManager.listPlaylists()
                                        viewModel.playlistChoice = item.docFile
                                    }

                                    DropDownSelection.ADD_TO_QUEUE -> {
                                        if (item.isFile) {
                                            addToQueue(
                                                uri = item.docFile?.uri,
                                                isShuffleMode = viewModel.uiState.value.isShuffle,
                                                isLoopMode = viewModel.uiState.value.isLoop,
                                                result = playerResult
                                            )
                                        } else {
                                            playModule(
                                                modList = StorageManager.walkDownDirectory(
                                                    uri = item.docFile?.uri,
                                                    includeDirectories = false
                                                ),
                                                isShuffleMode = viewModel.uiState.value.isShuffle,
                                                isLoopMode = viewModel.uiState.value.isLoop,
                                                result = playerResult
                                            )
                                        }
                                    }

                                    DropDownSelection.DIR_PLAY_CONTENTS -> playModule(
                                        modList = StorageManager.walkDownDirectory(
                                            uri = item.docFile?.uri,
                                            includeDirectories = false
                                        ),
                                        isShuffleMode = viewModel.uiState.value.isShuffle,
                                        isLoopMode = viewModel.uiState.value.isLoop,
                                        result = playerResult
                                    )

                                    DropDownSelection.FILE_PLAY_HERE ->
                                        playModule(
                                            modList = viewModel.getItems(),
                                            start = index,
                                            keepFirst = true,
                                            isShuffleMode = viewModel.uiState.value.isShuffle,
                                            isLoopMode = viewModel.uiState.value.isLoop,
                                            result = playerResult
                                        )

                                    DropDownSelection.FILE_PLAY_THIS_ONLY ->
                                        playModule(
                                            modList = item.docFile?.uri?.let { listOf(it) }
                                                .orEmpty(),
                                            isShuffleMode = viewModel.uiState.value.isShuffle,
                                            isLoopMode = viewModel.uiState.value.isLoop,
                                            result = playerResult
                                        )
                                }
                            }
                        )
                    }
                    composable<NavPlaylist> {
                        val args = it.toRoute<NavPlaylist>()
                        val viewModel by viewModels<PlaylistActivityViewModel>()
                        val state by viewModel.uiState.collectAsStateWithLifecycle()

                        val playerResult = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.StartActivityForResult()
                        ) { result ->
                            if (result.resultCode == 1) {
                                result.data?.getStringExtra("error")?.let { str ->
                                    Timber.w("Result with error: $str")
                                    showSnack(str)
                                }
                            }
                            if (result.resultCode == 2) {
                                viewModel.onRefresh(args.playlist)
                            }
                        }

                        LifecycleResumeEffect(Lifecycle.Event.ON_RESUME) {
                            Timber.d("Lifecycle onResume")
                            viewModel.onRefresh(args.playlist)

                            onPauseOrDispose {
                                Timber.d("Lifecycle onPause")
                                viewModel.save()
                            }
                        }

                        PlaylistScreen(
                            state = state,
                            snackBarHostState = snackBarHostState,
                            onBack = navController::popBackStack,
                            onItemClick = { index ->
                                onItemClick(
                                    items = viewModel.getUriItems(),
                                    position = index,
                                    isShuffleMode = viewModel.uiState.value.isShuffle,
                                    isLoopMode = viewModel.uiState.value.isLoop,
                                    result = playerResult
                                )
                            },
                            onMenuClick = { item, index, selection ->
                                when (selection) {
                                    DropDownSelection.DELETE -> {
                                        viewModel.removeItem(index)
                                        viewModel.onRefresh(args.playlist)
                                    }

                                    DropDownSelection.ADD_TO_QUEUE ->
                                        addToQueue(
                                            uri = item.uri,
                                            isShuffleMode = viewModel.uiState.value.isShuffle,
                                            isLoopMode = viewModel.uiState.value.isLoop,
                                            result = playerResult
                                        )

                                    DropDownSelection.FILE_PLAY_HERE ->
                                        playModule(
                                            modList = viewModel.getUriItems(),
                                            start = index,
                                            isShuffleMode = viewModel.uiState.value.isShuffle,
                                            isLoopMode = viewModel.uiState.value.isLoop,
                                            result = playerResult
                                        )

                                    DropDownSelection.FILE_PLAY_THIS_ONLY ->
                                        playModule(
                                            uri = item.uri,
                                            isShuffleMode = viewModel.uiState.value.isShuffle,
                                            isLoopMode = viewModel.uiState.value.isLoop,
                                            result = playerResult
                                        )

                                    else -> Unit
                                }
                            },
                            onPlayAll = {
                                onPlayAll(
                                    modList = viewModel.getUriItems(),
                                    isShuffleMode = viewModel.uiState.value.isShuffle,
                                    isLoopMode = viewModel.uiState.value.isLoop,
                                    result = playerResult
                                )
                            },
                            onShuffle = viewModel::setShuffle,
                            onLoop = viewModel::setLoop,
                            onMove = viewModel::onMove,
                            onDragStopped = viewModel::onDragStopped
                        )
                    }
                    /** Search and Download **/
                    composable<NavSearch> {
                        SearchScreen(
                            onBack = navController::popBackStack,
                            onSearch = { query, type ->
                                NavSearchTitleResult(query, type).also(navController::navigate)
                            },
                            onRandom = {
                                NavSearchResult(-1).also(navController::navigate)
                            },
                            onHistory = {
                                NavSearchHistory.also(navController::navigate)
                            }
                        )
                    }
                    composable<NavSearchHistory> {
                        var historyList = remember {
                            var list: MutableList<Module> = mutableListOf()

                            try {
                                list = Json.decodeFromString(PrefManager.searchHistory)
                            } catch (e: Exception) {
                                // Something happened or empty, make it an empty list
                                Timber.w("Failed to deserialize history!")
                                PrefManager.searchHistory = "[]"
                            }

                            list.toList()
                        }
                        SearchHistoryScreen(
                            historyList = historyList,
                            onBack = navController::popBackStack,
                            onClear = {
                                PrefManager.searchHistory = "[]"
                                historyList = listOf()
                            },
                            onClicked = {
                                NavSearchResult(it).also(navController::navigate)
                            }
                        )
                    }
                    composable<NavSearchError> {
                        val args = it.toRoute<NavSearchError>()
                        ErrorScreen(
                            message = args.error,
                            onBack = { navController.popBackStack(NavSearch, true) }
                        )
                    }
                    composable<NavSearchTitleResult> {
                        val args = it.toRoute<NavSearchTitleResult>()
                        val viewModel by viewModels<SearchResultViewModel> {
                            SearchResultViewModel.Factory
                        }
                        val state by viewModel.uiState.collectAsStateWithLifecycle()

                        LaunchedEffect(Unit) {
                            if (args.isArtistSearch == 0) {
                                val title = getString(R.string.search_artist_title)
                                viewModel.getArtists(title, args.searchQuery)
                            } else {
                                val title = getString(R.string.search_title_title)
                                viewModel.getFileOrTitle(title, args.searchQuery)
                            }
                        }
                        LaunchedEffect(state.hardError) {
                            if (state.hardError != null) {
                                Timber.w("Hard error has occurred")
                                NavSearchError(state.hardError).also(navController::navigate)
                            }
                        }

                        TitleResultScreen(
                            state = state,
                            onBack = navController::popBackStack,
                            onItemId = { id ->
                                NavSearchResult(id).also(navController::navigate)
                            },
                            onArtistId = viewModel::getArtistById,
                        )
                    }
                    composable<NavSearchResult> {
                        val args = it.toRoute<NavSearchResult>()
                        val viewModel by viewModels<ResultViewModel> { ResultViewModel.Factory }
                        val state by viewModel.uiState.collectAsStateWithLifecycle()

                        val playerResult = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.StartActivityForResult()
                        ) { result ->
                            if (result.resultCode == 1) {
                                result.data?.getStringExtra("error")?.let { str ->
                                    Timber.w("Result with error: $str")
                                    lifecycleScope.launch {
                                        snackBarHostState.showSnackbar(message = str)
                                    }
                                }
                            }
                            if (result.resultCode == 2) {
                                viewModel.update()
                            }
                        }

                        LaunchedEffect(state.hardError) {
                            if (state.hardError != null) {
                                Timber.w("Hard error has occurred")
                                NavSearchError(state.hardError).also(navController::navigate)
                            }
                        }

                        LaunchedEffect(Unit) {
                            if (args.moduleID < 0) {
                                viewModel.getRandomModule()
                            } else {
                                viewModel.getModuleById(args.moduleID)
                            }
                        }

                        ModuleResultScreen(
                            state = state,
                            snackBarHostState = snackBarHostState,
                            onBack = navController::popBackStack,
                            onRandom = viewModel::getRandomModule,
                            onShare = ::shareLink,
                            onDeleteModule = viewModel::deleteModule,
                            onDownloadModule = { module ->
                                StorageManager.getDownloadPath(
                                    module = module,
                                    onSuccess = { dfc ->
                                        viewModel.downloadModule(module, dfc)
                                    },
                                    onError = viewModel::showSoftError
                                )
                            },
                            onPlay = { module ->
                                StorageManager.doesModuleExist(
                                    module = module,
                                    onFound = { uri ->
                                        val modListUri = arrayListOf(uri)

                                        XmpApplication.instance?.fileListUri = modListUri

                                        Timber.i("Play ${uri.path}")
                                        Intent(
                                            this@MainActivity,
                                            PlayerActivity::class.java
                                        ).apply {
                                            putExtra(PlayerActivity.PARM_START, 0)
                                        }.also(playerResult::launch)
                                    },
                                    onNotFound = { dfc ->
                                        viewModel.downloadModule(module, dfc)
                                    },
                                    onError = viewModel::showSoftError
                                )
                            }
                        )
                    }
                    /** Preferences **/
                    composable<NavPreferences> {
                        PreferencesScreen(
                            snackBarHostState = snackBarHostState,
                            onBack = navController::popBackStack,
                            onFormats = { NavPreferenceFormats.also(navController::navigate) },
                            onAbout = { NavPreferenceAbout.also(navController::navigate) }
                        )
                    }
                    composable<NavPreferenceAbout> {
                        val buildVersion = remember { BuildConfig.VERSION_NAME }
                        val xmpVersion = remember { Xmp.getVersion() }
                        AboutScreen(
                            buildVersion = buildVersion,
                            libVersion = xmpVersion,
                            onBack = navController::popBackStack
                        )
                    }
                    composable<NavPreferenceFormats> {
                        val formats = remember { Xmp.formats }
                        FormatsScreen(
                            snackbarHostState = snackBarHostState,
                            formatsList = formats,
                            onBack = navController::popBackStack
                        )
                    }
                }
            }
        }
    }

    /**
     * Play all `playable` modules in the current path we're in.
     */
    private fun onPlayAll(
        modList: List<Uri>,
        isShuffleMode: Boolean,
        isLoopMode: Boolean,
        result: ManagedActivityResultLauncher<Intent, ActivityResult>
    ) {
        lifecycleScope.launch {
            if (modList.isEmpty()) {
                showSnack(getString(R.string.error_no_files_to_play))
                return@launch
            }

            playModule(
                modList = modList,
                isShuffleMode = isShuffleMode,
                isLoopMode = isLoopMode,
                result = result
            )
        }
    }

    private fun onItemClick(
        items: List<Uri>,
        position: Int,
        isShuffleMode: Boolean,
        isLoopMode: Boolean,
        result: ManagedActivityResultLauncher<Intent, ActivityResult>
    ) {
        fun playAllStaringAtPosition() {
            if (position < 0) {
                throw RuntimeException("Play count is negative")
            }
            playModule(
                modList = items,
                start = position,
                keepFirst = true,
                isShuffleMode = isShuffleMode,
                isLoopMode = isLoopMode,
                result = result
            )
        }

        fun playThisFile() {
            val filename = items[position]
            if (filename.path.isNullOrEmpty()) {
                showSnack("Invalid file path")
                return
            }
            if (!Xmp.testFromFd(filename)) {
                showSnack("Unrecognized file format")
                return
            }
            playModule(
                uri = filename,
                isShuffleMode = isShuffleMode,
                isLoopMode = isLoopMode,
                result = result
            )
        }

        fun addToQueue() {
            val filename = items[position]
            if (filename.path.isNullOrEmpty()) {
                showSnack("Invalid file path")
                return
            }
            if (!Xmp.testFromFd(filename)) {
                showSnack("Unrecognized file format")
                return
            }
            addToQueue(
                uri = filename,
                isShuffleMode = isShuffleMode,
                isLoopMode = isLoopMode,
                result = result
            )
            showSnack("Added to queue")
        }

        /**
         * mode:
         * 1. Start playing at selection
         * 2. Play selected file
         * 3. Enqueue selected file
         */
        Timber.d("Item Clicked: ${PrefManager.playlistMode}")
        when (PrefManager.playlistMode) {
            1 -> playAllStaringAtPosition()
            2 -> playThisFile()
            3 -> addToQueue()
        }
    }

    private fun playModule(
        uri: Uri?,
        isShuffleMode: Boolean,
        isLoopMode: Boolean,
        result: ManagedActivityResultLauncher<Intent, ActivityResult>
    ) {
        if (uri == null) {
            showSnack("Null uri when playing module")
            return
        }

        playModule(
            modList = listOf(uri),
            isShuffleMode = isShuffleMode,
            isLoopMode = isLoopMode,
            result = result
        )
    }

    private fun playModule(
        modList: List<Uri>,
        start: Int = 0,
        keepFirst: Boolean = false,
        isShuffleMode: Boolean,
        isLoopMode: Boolean,
        result: ManagedActivityResultLauncher<Intent, ActivityResult>
    ) {
        if (modList.isEmpty()) {
            showSnack("List is empty to play module(s)")
            return
        }

        XmpApplication.instance!!.fileListUri = modList
        Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.PARM_SHUFFLE, isShuffleMode)
            putExtra(PlayerActivity.PARM_LOOP, isLoopMode)
            putExtra(PlayerActivity.PARM_START, start)
            putExtra(PlayerActivity.PARM_KEEPFIRST, keepFirst)
        }.also { intent ->
            Timber.i("Start Player activity")
            result.launch(intent)
        }
    }

    /**
     * Add an URI to the queue.
     *
     * If the service alive, connect to it and append the play queue. @see [connection].
     * Else start the service normally with @see [playModule].
     */
    private fun addToQueue(
        uri: Uri?,
        isShuffleMode: Boolean,
        isLoopMode: Boolean,
        result: ManagedActivityResultLauncher<Intent, ActivityResult>
    ) {
        if (uri == null) {
            showSnack("Null uri when adding to Queue")
            return
        }

        addToQueue(
            list = listOf(uri),
            isShuffleMode = isShuffleMode,
            isLoopMode = isLoopMode,
            result = result
        )
    }

    /**
     * Add a list of URI's to the queue.
     *
     * If the service alive, connect to it and append the play queue. @see [connection].
     * Else start the service normally with @see [playModule].
     */
    private fun addToQueue(
        list: List<Uri>,
        isShuffleMode: Boolean,
        isLoopMode: Boolean,
        result: ManagedActivityResultLauncher<Intent, ActivityResult>
    ) {
        if (list.isEmpty()) {
            showSnack("Empty uri list when adding to Queue")
            return
        }

        if (PlayerService.isAlive) {
            mAddList = list
            Intent(this, PlayerService::class.java).also {
                bindService(it, connection, 0)
            }
        } else {
            playModule(
                modList = list,
                isShuffleMode = isShuffleMode,
                isLoopMode = isLoopMode,
                result = result
            )
        }
    }

    private fun shareLink(url: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, url)
            type = "text/html"
        }

        Intent.createChooser(sendIntent, null).also(::startActivity)
    }
}
