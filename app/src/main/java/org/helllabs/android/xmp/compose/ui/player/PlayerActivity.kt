package org.helllabs.android.xmp.compose.ui.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.session.MediaControllerCompat
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import java.nio.charset.StandardCharsets
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.compose.MainActivity
import org.helllabs.android.xmp.compose.components.MessageDialog
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.player.components.PlayerBottomAppBar
import org.helllabs.android.xmp.compose.ui.player.components.PlayerControls
import org.helllabs.android.xmp.compose.ui.player.components.PlayerControlsEvent
import org.helllabs.android.xmp.compose.ui.player.components.PlayerInfo
import org.helllabs.android.xmp.compose.ui.player.components.PlayerSeekBar
import org.helllabs.android.xmp.compose.ui.player.components.PlayerSheet
import org.helllabs.android.xmp.compose.ui.player.components.PlayerSheetEvent
import org.helllabs.android.xmp.compose.ui.player.components.SeekEvent
import org.helllabs.android.xmp.compose.ui.player.components.ViewFlipper
import org.helllabs.android.xmp.compose.ui.player.viewer.ComposeChannelViewer
import org.helllabs.android.xmp.compose.ui.player.viewer.ComposePatternViewer
import org.helllabs.android.xmp.compose.ui.player.viewer.InstrumentViewer
import org.helllabs.android.xmp.compose.ui.player.viewer.composeSampleChannelInfo
import org.helllabs.android.xmp.compose.ui.player.viewer.composeSampleFrameInfo
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.model.ChannelInfo
import org.helllabs.android.xmp.model.FrameInfo
import org.helllabs.android.xmp.model.ModVars
import org.helllabs.android.xmp.service.EndPlayback
import org.helllabs.android.xmp.service.PlayerBinder
import org.helllabs.android.xmp.service.PlayerEvent
import org.helllabs.android.xmp.service.PlayerService
import timber.log.Timber

class PlayerActivity : ComponentActivity() {

    companion object {
        const val PARM_KEEPFIRST = "keepFirst"
        const val PARM_LOOP = "loop"
        const val PARM_SHUFFLE = "shuffle"
        const val PARM_START = "start"
    }

    private val viewModel by viewModels<PlayerViewModel>()

    private val snackBarHostState = SnackbarHostState()

    /* Detect if Screen is on or off */
    private lateinit var screenReceiver: ScreenReceiver

    /* Actual mod player (the Service) */
    private var modPlayer: PlayerService? = null
    private var controls: MediaControllerCompat? = null

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Timber.i("Service connected")
            modPlayer = (service as PlayerBinder).getService()
            controls = modPlayer!!.mediaController

            viewModel.onConnected(true)
            viewModel.isPlaying(PlayerService.isPlaying.value)

            lifecycleScope.launch {
                modPlayer!!.playerEvent.collect { event ->
                    handlePlayerEvent(event)
                }
            }

            with(viewModel.activityState.value) {
                if (fileList.isNotEmpty()) {
                    // Start new queue
                    playNewMod(fileList, start)
                } else {
                    // Reconnect to existing service
                    viewModel.showNewMod(modPlayer!!, false)
                }
            }
        }

        override fun onServiceDisconnected(className: ComponentName) {
            Timber.i("Service disconnected")

            saveAllSeqPreference()
            viewModel.onConnected(false)
            viewModel.allowUpdate(false)

            modPlayer = null

            setResult(RESULT_OK)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.d("onCreate")

        handleIntent(intent)

        // Enable Edge-to-Edge coloring
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        // Initialize our ScreenReceiver
        screenReceiver = ScreenReceiver(
            onScreenEvent = viewModel::screenOn
        )

        // Register ScreenReceiver on/off events
        screenReceiver.register(this)

        // Keep screen on if preference is set.
        if (PrefManager.keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        setContent {
            // Collect different states
            val buttonState by viewModel.buttonState.collectAsStateWithLifecycle()
            val drawerState by viewModel.drawerState.collectAsStateWithLifecycle()
            val infoState by viewModel.infoState.collectAsStateWithLifecycle()
            val instrumentNames by viewModel.insName.collectAsStateWithLifecycle()
            val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
            val modVars by viewModel.modVars.collectAsStateWithLifecycle()
            val timeState by viewModel.timeState.collectAsStateWithLifecycle()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val channelInfo by viewModel.channelInfo.collectAsStateWithLifecycle()
            val frameInfo by viewModel.frameInfo.collectAsStateWithLifecycle()

            // Stabilize lambdas, this helps reduce useless recompositions.
            val onChangeViewer: () -> Unit = remember {
                {
                    viewModel.changeViewer()
                }
            }
            val showSheet: (Boolean) -> Unit = remember {
                {
                    viewModel.showSheet(it)
                }
            }
            val closeMessage: (Boolean) -> Unit = remember {
                {
                    viewModel.showMessage(it, "")
                }
            }
            val onSheetEvent: (PlayerSheetEvent) -> Unit = remember {
                {
                    when (it) {
                        PlayerSheetEvent.OnAllSeq -> {
                            val res = modPlayer!!.toggleAllSequences()
                            viewModel.onAllSequence(res)
                        }

                        PlayerSheetEvent.OnMessage -> {
                            val comment = String(Xmp.getComment(), StandardCharsets.UTF_8)
                            if (comment.isEmpty()) {
                                lifecycleScope.launch {
                                    val msg = "No comment to display"
                                    snackBarHostState.showSnackbar(msg)
                                }
                            } else {
                                viewModel.showMessage(true, comment.trim())
                            }
                            viewModel.showSheet(false)
                        }

                        is PlayerSheetEvent.OnSequence -> {
                            Timber.i("Set sequence $it")
                            val res = modPlayer!!.setSequence(it.seq)
                            viewModel.onSequence(res)
                        }
                    }
                }
            }
            val onControlsEvent: (PlayerControlsEvent) -> Unit = remember {
                {
                    Timber.d("onControlsEvent $it")
                    when (it) {
                        PlayerControlsEvent.OnNext -> {
                            controls!!.transportControls.skipToNext()
                            viewModel.isPlaying(true)
                        }

                        PlayerControlsEvent.OnPlay -> {
                            if (PlayerService.isPlaying.value) {
                                controls!!.transportControls.pause()
                            } else {
                                controls!!.transportControls.play()
                            }
                            viewModel.isPlaying(PlayerService.isPlaying.value)
                        }

                        PlayerControlsEvent.OnPrev -> {
                            controls!!.transportControls.skipToPrevious()
                            viewModel.isPlaying(PlayerService.isPlaying.value)
                        }

                        PlayerControlsEvent.OnRepeat -> {
                            val res = modPlayer!!.toggleLoop()
                            viewModel.toggleLoop(res)
                        }

                        PlayerControlsEvent.OnStop -> {
                            controls!!.transportControls.stop()
                        }
                    }
                }
            }
            val onSeekEvent: (SeekEvent) -> Unit = remember {
                {
                    when (it) {
                        is SeekEvent.OnSeek -> {
                            if (it.isSeeking) {
                                viewModel.isSeeking(true)
                            } else {
                                controls!!.transportControls.seekTo(it.value.toLong() * 100)
                                viewModel.isSeeking(false)
                                viewModel.setPlayTime(Xmp.time().div(100F))
                            }
                        }
                    }
                }
            }

            // Restart the loop on info change
            LaunchedEffect(uiState.infoTitle, uiState.infoType) {
                launch(Dispatchers.Default) {
                    Timber.d("Start LaunchedEffect Loop")

                    viewModel.resetPlayTime()

                    while (true) {
                        if (!viewModel.uiState.value.allowUpdate &&
                            viewModel.activityState.value.playTime < 0
                        ) {
                            Timber.i("Stop update")
                            break
                        }

                        if ((!viewModel.uiState.value.screenOn || !viewModel.isPlaying) ||
                            modPlayer == null
                        ) {
                            Timber.d(
                                "Waiting - " +
                                    "Screen On: ${viewModel.uiState.value.screenOn}, " +
                                    "isPlaying: ${viewModel.isPlaying}, " +
                                    "modPlayer null: ${(modPlayer == null)}"
                            )
                            delay(500.milliseconds)
                            continue
                        }

                        // Update ViewerInfo()
                        viewModel.updateViewInfo()

                        // Get the current playback time
                        viewModel.setPlayTime(Xmp.time().div(100F))

                        // Update the seekbar for the current time
                        viewModel.updateSeekBar()

                        // Update playback and total-playback time
                        viewModel.updateInfoTime()

                        // Update Speed, Bpm, Pos, Pat
                        viewModel.updateInfoState()

                        delay(33.milliseconds)
                    }
                }
            }

            XmpTheme {
                // Hoisted here because of native jni call.
                MessageDialog(
                    isShowing = uiState.showMessageDialog,
                    icon = Icons.Default.Info,
                    title = "Comments",
                    text = uiState.currentMessage,
                    confirmText = stringResource(id = android.R.string.ok),
                    onConfirm = { closeMessage(false) }
                )

                PlayerScreen(
                    snackBarHostState = snackBarHostState,
                    uiState = uiState,
                    timeState = timeState,
                    drawerState = drawerState,
                    instrumentNames = instrumentNames,
                    modVars = modVars,
                    buttonState = buttonState,
                    frameInfo = frameInfo,
                    channelInfo = channelInfo,
                    isMuted = isMuted,
                    infoState = infoState,
                    onControlsEvent = onControlsEvent,
                    onSheetEvent = onSheetEvent,
                    onSeekEvent = onSeekEvent,
                    onChangeViewer = onChangeViewer,
                    onSheetVisibleDialog = showSheet,
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Timber.d("onPause")
    }

    override fun onResume() {
        super.onResume()
        Timber.d("onResume")

        viewModel.showInfoLine(PrefManager.showInfoLine)
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy")

        saveAllSeqPreference()

        modPlayer = null
        if (viewModel.uiState.value.serviceConnected) {
            unbindService(connection)
        }

        screenReceiver.unregister(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Timber.i("onNewIntent")
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        Timber.d("handleIntent: $intent")

        if (intent == null) {
            Timber.w("Intent was null")
            setResult(RESULT_OK)
            finish()
            return
        }

        // Oops. We don't want to start service if launched from history and service is not running
        // so run the browser instead.
        if ((intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
            Timber.i("Player started from history")
            Intent(this, MainActivity::class.java).apply {
                setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }.also(::startActivity)
            finish()
            return
        }

        intent.data?.let { uri ->
            Timber.i("Player started from intent filter")
            viewModel.setActivityState(
                fileList = listOf(uri),
                shuffleMode = false,
                loopListMode = false,
                keepFirst = false,
                start = 0
            )
        }

        intent.extras?.let { extras ->
            // Sanity, because i can't brain
            if (intent.data != null) {
                return@let
            }

            Timber.i("Player started from intent extras")
            val app = XmpApplication.instance!!
            viewModel.setActivityState(
                fileList = app.fileListUri.orEmpty(),
                shuffleMode = extras.getBoolean(PARM_SHUFFLE),
                loopListMode = extras.getBoolean(PARM_LOOP),
                keepFirst = extras.getBoolean(PARM_KEEPFIRST),
                start = extras.getInt(PARM_START)
            )
            app.clearFileList()
        }

        val service = Intent(this, PlayerService::class.java)

        if (!viewModel.uiState.value.serviceConnected) {
            Timber.i("Start service")
            startService(service)
        }

        if (!bindService(service, connection, Context.BIND_AUTO_CREATE)) {
            Timber.e("Can't bind to service")
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun handlePlayerEvent(event: PlayerEvent) {
        when (event) {
            PlayerEvent.EndMod -> {
                Timber.d("endModCallback: end of module")
                viewModel.allowUpdate(false)
            }

            is PlayerEvent.NewMod -> {
                Timber.d("newModCallback: show module data")
                viewModel.showNewMod(modPlayer!!, event.isPrevious)
                viewModel.allowUpdate(true)
            }

            PlayerEvent.NewSequence -> {
                if (modPlayer == null) {
                    return
                }

                val modVars = ModVars()
                Xmp.getModVars(modVars)
                viewModel.modVars.update { modVars }

                viewModel.allowUpdate(true)

                viewModel.showNewSequence { time ->
                    val minutes = time / 60000
                    val seconds = time / 1000 % 60
                    val string = "$minutes:${seconds.toString().padStart(2, '0')}"
                    lifecycleScope.launch {
                        snackBarHostState.showSnackbar("New sequence duration: $string")
                    }
                }
            }

            PlayerEvent.Paused -> {
                modPlayer?.let { viewModel.isPlaying(false) }
                viewModel.allowUpdate(false)
            }

            PlayerEvent.Play -> {
                modPlayer?.let { viewModel.isPlaying(true) }
                viewModel.allowUpdate(true)
            }

            is PlayerEvent.EndPlay -> {
                Timber.d("endPlayCallback: End progress thread")
                viewModel.allowUpdate(false)
                val resultIntent = Intent().apply {
                    val message = when (event.result) {
                        EndPlayback.ERROR_FOCUS -> "Unable to get Audio Focus"
                        EndPlayback.ERROR_WATCHDOG -> "Stopped by watchdog"
                        EndPlayback.ERROR_INIT -> "Unable to initialize native XMP library"
                        else -> ""
                    }

                    putExtra("message", message)
                }
                setResult(RESULT_OK, resultIntent)

                finish()
            }

            is PlayerEvent.ErrorMessage -> {
                lifecycleScope.launch {
                    snackBarHostState.showSnackbar(event.msg)
                }
            }
        }
    }

    private fun saveAllSeqPreference() {
        Timber.d("Write all sequences preference")
        // Write our all sequences button status to shared prefs
        PrefManager.allSequences = modPlayer?.playAllSequences ?: false
    }

    private fun playNewMod(fileList: List<Uri>, start: Int) {
        modPlayer?.play(
            fileList = fileList,
            start = start,
            shuffle = viewModel.activityState.value.shuffleMode,
            loopList = viewModel.activityState.value.loopListMode,
            keepFirst = viewModel.activityState.value.keepFirst
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerScreen(
    buttonState: PlayerButtonsState,
    drawerState: PlayerSheetState,
    infoState: PlayerInfoState,
    instrumentNames: Array<String>,
    isMuted: ChannelMuteState,
    modVars: ModVars,
    channelInfo: ChannelInfo,
    frameInfo: FrameInfo,
    snackBarHostState: SnackbarHostState,
    timeState: PlayerTimeState,
    uiState: PlayerState,
    onChangeViewer: () -> Unit,
    onControlsEvent: (PlayerControlsEvent) -> Unit,
    onSeekEvent: (SeekEvent) -> Unit,
    onSheetEvent: (PlayerSheetEvent) -> Unit,
    onSheetVisibleDialog: (Boolean) -> Unit
) {
    val viewFlipperText by remember(uiState.infoTitle, uiState.infoType) {
        mutableStateOf(Pair(uiState.infoTitle, uiState.infoType))
    }

    if (uiState.showInfoDialog) {
        Dialog(
            onDismissRequest = { onSheetVisibleDialog(false) },
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text(text = "Module Info") },
                        navigationIcon = {
                            IconButton(onClick = { onSheetVisibleDialog(false) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                }
            ) { paddingValues ->
                PlayerSheet(
                    modifier = Modifier.padding(paddingValues),
                    state = drawerState,
                    onEvent = onSheetEvent
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        topBar = {
            ViewFlipper(
                navigationIcon = {
                    IconButton(onClick = { onSheetVisibleDialog(true) }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = null
                        )
                    }
                },
                skipToPrevious = uiState.skipToPrevious,
                info = viewFlipperText
            )
        },
        bottomBar = {
            PlayerBottomAppBar {
                PlayerInfo(state = infoState)
                Spacer(modifier = Modifier.height(12.dp))
                PlayerSeekBar(
                    state = timeState,
                    onSeek = onSeekEvent,
                )
                Spacer(modifier = Modifier.height(12.dp))
                PlayerControls(
                    state = buttonState,
                    onEvent = onControlsEvent,
                )
            }
        }
    ) { paddingValues ->
        val configuration = LocalConfiguration.current
        val modifier = remember(configuration.orientation) {
            if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                Modifier
            } else {
                Modifier.displayCutoutPadding()
            }
        }

        Box(
            modifier = modifier.padding(paddingValues)
        ) {
            when (uiState.currentViewer) {
                0 -> InstrumentViewer(
                    onTap = onChangeViewer,
                    channelInfo = channelInfo,
                    insName = instrumentNames,
                    isMuted = isMuted,
                    modVars = modVars,
                )

                1 -> ComposePatternViewer(
                    onTap = onChangeViewer,
                    allowUpdate = uiState.allowUpdate,
                    fi = frameInfo,
                    isMuted = isMuted,
                    modType = uiState.infoType,
                    modVars = modVars,
                )

                2 -> ComposeChannelViewer(
                    onTap = onChangeViewer,
                    channelInfo = channelInfo,
                    frameInfo = frameInfo,
                    insName = instrumentNames,
                    isMuted = isMuted,
                    modVars = modVars,
                )
            }
        }
    }
}

// region [Region] Compose Previews
class PlayerPreviewProvider : PreviewParameterProvider<Boolean> {
    override val values = sequenceOf(false, true)
}

@Preview
@Composable
private fun Preview_PlayerScreen(
    @PreviewParameter(PlayerPreviewProvider::class) sheetValue: Boolean
) {
    val modVars = remember {
        ModVars(190968, 30, 25, 12, 40, 18, 1, 0)
    }

    val sheetVisible = remember(sheetValue) {
        mutableStateOf(sheetValue)
    }

    XmpTheme {
        PlayerScreen(
            snackBarHostState = SnackbarHostState(),
            uiState = PlayerState(
                infoTitle = "Title 1",
                infoType = "Fast Tracker",
                currentViewer = 0,
                showInfoDialog = sheetVisible.value,
            ),
            infoState = PlayerInfoState(
                infoSpeed = "11",
                infoBpm = "22",
                infoPos = "33",
                infoPat = "44"
            ),
            buttonState = PlayerButtonsState(
                isPlaying = true,
                isRepeating = false
            ),
            timeState = PlayerTimeState(
                timeNow = "00:00",
                timeTotal = "00:00",
                seekPos = 25f,
                seekMax = 100f
            ),
            drawerState = PlayerSheetState(
                moduleInfo = listOf(111, 222, 333, 444, 555),
                isPlayAllSequences = true,
                numOfSequences = List(12) { it },
                currentSequence = 2
            ),
            instrumentNames = Array(modVars.numInstruments) {
                String.format("%02X %s", it + 1, "Instrument Name")
            },
            modVars = modVars,
            channelInfo = composeSampleChannelInfo(),
            frameInfo = composeSampleFrameInfo(),
            isMuted = ChannelMuteState(BooleanArray(modVars.numChannels) { false }),
            onControlsEvent = { },
            onSeekEvent = { },
            onSheetEvent = { },
            onChangeViewer = { },
            onSheetVisibleDialog = {
                sheetVisible.value = it
            },
        )
    }
}
// endregion
