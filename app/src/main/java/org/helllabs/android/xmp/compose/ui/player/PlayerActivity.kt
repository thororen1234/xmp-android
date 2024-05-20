package org.helllabs.android.xmp.compose.ui.player

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.RemoteException
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.compose.components.MessageDialog
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.player.components.PlayerBottomAppBar
import org.helllabs.android.xmp.compose.ui.player.components.PlayerControls
import org.helllabs.android.xmp.compose.ui.player.components.PlayerDrawer
import org.helllabs.android.xmp.compose.ui.player.components.PlayerInfo
import org.helllabs.android.xmp.compose.ui.player.components.PlayerSeekBar
import org.helllabs.android.xmp.compose.ui.player.components.ViewFlipper
import org.helllabs.android.xmp.compose.ui.player.viewer.PatternInfo
import org.helllabs.android.xmp.compose.ui.player.viewer.ViewerInfo
import org.helllabs.android.xmp.compose.ui.player.viewer.XmpCanvas
import org.helllabs.android.xmp.compose.ui.player.viewer.composePatternSampleData
import org.helllabs.android.xmp.compose.ui.player.viewer.composeViewerSampleData
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.service.PlayerBinder
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.service.PlayerServiceCallback
import timber.log.Timber

class PlayerActivity : ComponentActivity(), PlayerServiceCallback {

    private val viewModel by viewModels<PlayerViewModel>()

    /* Actual mod player (the Service) */
    private var modPlayer: PlayerService? = null

    private val handler = Handler(Looper.myLooper() ?: Looper.getMainLooper())
    private val screenReceiver = ScreenReceiver()
    private val snackbarHostState = SnackbarHostState()

    private var job: Job? = null
    private val playerLock = Any() // for sync

    private var fileList: List<Uri>? = null

    private var keepFirst = false
    private var loopListMode = false

    private var playTime = 0F
    private var shuffleMode = false
    private var skipToPrevious = false
    private var start = 0
    private var totalTime = 0

    private val s = StringBuilder()
    private val c = CharArray(2)

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Timber.i("Service connected")
            synchronized(playerLock) {
                modPlayer = (service as PlayerBinder).getService()
                modPlayer!!.setCallback(this@PlayerActivity)

                if (fileList != null && fileList!!.isNotEmpty()) {
                    // Start new queue
                    playNewMod(fileList!!, start)
                } else {
                    // Reconnect to existing service
                    try {
                        handler.post(showNewModRunnable)
                    } catch (e: RemoteException) {
                        Timber.e("Can't get module file name")
                    }
                }

                viewModel.onConnected(true)
                viewModel.isPlaying(!modPlayer!!.isPaused())
            }
        }

        override fun onServiceDisconnected(className: ComponentName) {
            saveAllSeqPreference()
            synchronized(playerLock) {
                stopUpdate = true
                modPlayer = null
                Timber.i("Service disconnected")
                setResult(RESULT_OK)
                finish()
            }
            viewModel.onConnected(false)
        }
    }

    private val updateInfoRunnable: Runnable = object : Runnable {
        private var oldBpm = -1
        private var oldPat = -1
        private var oldPos = -1
        private var oldSpd = -1
        private var oldTime = -1
        private var oldTotalTime = -1

        override fun run() {
            if (viewModel.isPlaying) {
                // update seekbar
                if (!viewModel.isSeeking && playTime >= 0) {
                    viewModel.seekPos(playTime)
                }

                // get current frame info
                synchronized(playerLock) {
                    if (modPlayer == null) {
                        return@synchronized
                    }
                    try {
                        modPlayer!!.getInfo(viewModel.viewInfo.values)
                        viewModel.viewInfo.time = modPlayer!!.time() / 1000
                    } catch (e: RemoteException) {
                        // fail silently
                    }
                }

                // Frame Info - Speed
                oldSpd = updateFrameInfo(
                    viewModel.viewInfo.values[5],
                    oldSpd,
                    viewModel::setInfoSpeed
                )
                // Frame Info - BPM
                oldBpm = updateFrameInfo(
                    viewModel.viewInfo.values[6],
                    oldBpm,
                    viewModel::setInfoBpm
                )
                // Frame Info - Position
                oldPos = updateFrameInfo(
                    viewModel.viewInfo.values[0],
                    oldPos,
                    viewModel::setInfoPos
                )
                // Frame Info - Pattern
                oldPat = updateFrameInfo(
                    viewModel.viewInfo.values[1],
                    oldPat,
                    viewModel::setInfoPat
                )

                // display playback time
                if (viewModel.viewInfo.time != oldTime) {
                    var t = viewModel.viewInfo.time
                    if (t < 0) {
                        t = 0
                    }
                    s.delete(0, s.length)
                    Util.to2d(c, t / 60)
                    s.append(c)
                    s.append(":")
                    Util.to02d(c, t % 60)
                    s.append(c)

                    viewModel.setTimeNow(s.toString())
                    oldTime = viewModel.viewInfo.time
                }

                // display total playback time
                if (totalTime != oldTotalTime) {
                    s.delete(0, s.length)
                    Util.to2d(c, totalTime / 60)
                    s.append(c)
                    s.append(":")
                    Util.to02d(c, totalTime % 60)
                    s.append(c)

                    viewModel.setTimeTotal(s.toString())
                    oldTotalTime = totalTime
                }
            }
        }
    }

    private val showNewSequenceRunnable = Runnable {
        val time = viewModel.modVars[0]
        totalTime = time / 1000
        viewModel.setSeekBar(0F, time / 100F)

        val timeString =
            String.format(Locale.getDefault(), "%d:%02d", time / 60000, time / 1000 % 60)
        showSnack("New sequence duration: $timeString")
        viewModel.currentSequence(viewModel.modVars[7])
    }

    private val showNewModRunnable = Runnable {
        Timber.i("Show new module")
        if (modPlayer == null) {
            return@Runnable
        }

        synchronized(playerLock) {
            playTime = try {
                modPlayer!!.getModVars(viewModel.modVars)
                modPlayer!!.getSeqVars(viewModel.seqVars)
                modPlayer!!.time() / 100F
            } catch (e: RemoteException) {
                Timber.e("Can't get module data")
                return@Runnable
            }

            var name: String
            var type: String
            var allSeq: Boolean
            var loop: Boolean
            try {
                name = modPlayer!!.getModName()
                type = modPlayer!!.getModType()
                allSeq = modPlayer!!.getAllSequences()
                loop = modPlayer!!.getLoop()
                if (name.trim().isEmpty()) {
                    name = modPlayer!!.getFileName()
                }
            } catch (e: RemoteException) {
                name = ""
                type = ""
                allSeq = false
                loop = false
                Timber.e("Can't get module name and type")
            }
            val time = viewModel.modVars[0]
            val len = viewModel.modVars[1]
            val pat = viewModel.modVars[2]
            val chn = viewModel.modVars[3]
            val ins = viewModel.modVars[4]
            val smp = viewModel.modVars[5]
            val numSeq = viewModel.modVars[6]
            val sequences = viewModel.seqVars.take(numSeq)

            viewModel.setDetails(pat, ins, smp, chn, len, allSeq, 0, sequences)
            totalTime = time / 1000

            viewModel.setSeekBar(playTime, time / 100F)
            viewModel.toggleLoop(loop)

            viewModel.setFlipperInfo(name, type, skipToPrevious)
            skipToPrevious = false

            viewModel.viewInfo.type = Xmp.getModType()

            viewModel.setup()

            stopUpdate = false

            if (job == null || !job!!.isActive) {
                startProgressCoroutine()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        if (PrefManager.keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        val filter = IntentFilter(Intent.ACTION_SCREEN_ON).apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, filter)

        viewModel.screenOn(true)
        viewModel.showInfoLine(PrefManager.showInfoLine)

        if (PlayerService.isLoaded) {
            canChangeViewer = true
        }

        onNewIntent(intent)

        Timber.d("onCreate")
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val infoState by viewModel.infoState.collectAsStateWithLifecycle()
            val buttonState by viewModel.buttonState.collectAsStateWithLifecycle()
            val timeState by viewModel.timeState.collectAsStateWithLifecycle()
            val drawerState by viewModel.drawerState.collectAsStateWithLifecycle()

            val scope = rememberCoroutineScope()
            fun onCloseDrawer() = scope.launch {
                drawerState.drawerState.close()
            }

            fun onOpenDrawer() = scope.launch {
                drawerState.drawerState.open()
            }

            var showComments by remember { mutableStateOf(false) }
            MessageDialog(
                isShowing = showComments,
                icon = Icons.Default.Info,
                title = "Comments",
                text = Xmp.getComment().orEmpty(),
                confirmText = stringResource(id = R.string.ok),
                onConfirm = { showComments = false }
            )

            var deleteFile by remember { mutableStateOf(false) }
            MessageDialog(
                isShowing = deleteFile,
                icon = Icons.Default.DeleteForever,
                title = "Delete",
                text = "Are you sure to delete this file?",
                confirmText = stringResource(id = R.string.menu_delete),
                onConfirm = {
                    try {
                        if (modPlayer!!.deleteFile()) {
                            showSnack("File deleted")
                            setResult(2) // Why this?
                            modPlayer!!.nextSong()
                        } else {
                            showSnack("Can\'t delete file")
                        }
                    } catch (e: RemoteException) {
                        showSnack("Can\'t connect service")
                    }
                    deleteFile = false
                }
            )

            XmpTheme {
                PlayerScreen(
                    snackbarHostState = snackbarHostState,
                    uiState = uiState,
                    infoState = infoState,
                    buttonState = buttonState,
                    timeState = timeState,
                    drawerState = drawerState,
                    onMenu = ::onOpenDrawer,
                    onMenuClose = ::onCloseDrawer,
                    currentViewer = viewModel.currentViewer,
                    viewInfo = viewModel.viewInfo,
                    patternInfo = viewModel.patternInfo,
                    isMuted = viewModel.isMuted,
                    modVars = viewModel.modVars,
                    insName = viewModel.insName,
                    onDelete = {
                        deleteFile = true
                    },
                    onMessage = {
                        if (Xmp.getComment().isNullOrEmpty()) {
                            showSnack("No comment to display")
                        } else {
                            showComments = true
                        }
                        onCloseDrawer()
                    },
                    onAllSeq = {
                        synchronized(playerLock) {
                            if (modPlayer == null) {
                                return@synchronized
                            }
                            try {
                                modPlayer!!.toggleAllSequences()
                            } catch (e: RemoteException) {
                                Timber.e("Can't toggle all sequences status")
                            }
                        }
                        viewModel.onAllSequence(modPlayer!!.getAllSequences())
                    },
                    onSequence = {
                        synchronized(playerLock) {
                            if (modPlayer == null) {
                                return@synchronized
                            }
                            try {
                                Timber.i("Set sequence $it")
                                modPlayer!!.setSequence(it)
                            } catch (e: RemoteException) {
                                Timber.e("Can't set sequence $it")
                            }
                        }
                    },
                    onSeek = { s ->
                        synchronized(playerLock) {
                            if (modPlayer == null) {
                                return@synchronized
                            }
                            try {
                                modPlayer!!.seek(s.toInt() * 100)
                                playTime = modPlayer!!.time() / 100F
                            } catch (e: RemoteException) {
                                Timber.e("Can't seek to time")
                            }
                        }
                    },
                    onIsSeeking = viewModel::isSeeking,
                    onStop = {
                        synchronized(playerLock) {
                            Timber.d("Stop button pressed")
                            if (modPlayer == null) {
                                return@synchronized
                            }
                            try {
                                modPlayer!!.stop()
                            } catch (e1: RemoteException) {
                                Timber.e("Can't stop module")
                            }
                        }
                    },
                    onPrev = {
                        synchronized(playerLock) {
                            Timber.d("Back button pressed")
                            if (modPlayer == null) {
                                return@synchronized
                            }
                            try {
                                if (modPlayer!!.time() > 3000) {
                                    modPlayer!!.seek(0)
                                    if (!viewModel.isPlaying) {
                                        modPlayer!!.pause()
                                    }
                                } else {
                                    modPlayer!!.prevSong()
                                    skipToPrevious = true
                                }
                                viewModel.isPlaying(true)
                            } catch (e: RemoteException) {
                                Timber.e("Can't go to previous module")
                            }
                        }
                    },
                    onPlay = {
                        synchronized(playerLock) {
                            Timber.d("Play/pause button pressed (playing=${viewModel.isPlaying})")
                            if (modPlayer == null) {
                                return@synchronized
                            }
                            try {
                                modPlayer!!.pause()
                                viewModel.isPlaying(!modPlayer!!.isPaused())
                            } catch (e: RemoteException) {
                                Timber.e("Can't pause/unpause module")
                            }
                        }
                    },
                    onNext = {
                        synchronized(playerLock) {
                            Timber.d("Next button pressed")
                            if (modPlayer == null) {
                                return@synchronized
                            }
                            try {
                                modPlayer!!.nextSong()
                                viewModel.isPlaying(true)
                            } catch (e: RemoteException) {
                                Timber.e("Can't go to next module")
                            }
                        }
                    },
                    onRepeat = {
                        synchronized(playerLock) {
                            if (modPlayer == null) {
                                return@synchronized
                            }
                            try {
                                viewModel.toggleLoop(modPlayer!!.toggleLoop())
                            } catch (e: RemoteException) {
                                Timber.e("Can't get loop status")
                            }
                        }
                    },
                    onChangeViewer = viewModel::changeViewer
                )
            }
        }
    }

    public override fun onDestroy() {
        Timber.d("onDestroy")

        saveAllSeqPreference()

        synchronized(playerLock) {
            if (modPlayer == null) {
                return@synchronized
            }
        }

        unregisterReceiver(screenReceiver)

        try {
            modPlayer?.setCallback(null)
            modPlayer = null
            unbindService(connection)
            Timber.i("Unbind service")
        } catch (e: IllegalArgumentException) {
            Timber.i("Can't unbind unregistered service")
        }

        job?.cancel()
        job = null

        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        Timber.d("onPause")

        if (ScreenReceiver.wasScreenOn) {
            viewModel.screenOn(false)
        }
    }

    override fun onPlayerPause() {
        Timber.d("pauseCallback")
        if (modPlayer == null) {
            return
        }

        synchronized(playerLock) {
            try {
                // Set pause status according to external state
                viewModel.isPlaying(!modPlayer!!.isPaused())
            } catch (e: RemoteException) {
                Timber.e("Can't get pause status")
            }
        }
    }

    override fun onNewSequence() {
        if (modPlayer == null) {
            return
        }

        synchronized(playerLock) {
            Timber.d("newSequenceCallback: show new sequence")
            try {
                modPlayer!!.getModVars(viewModel.modVars)
            } catch (e: RemoteException) {
                Timber.e("Can't get new sequence data")
            }
            handler.post(showNewSequenceRunnable)
        }
    }

    override fun onNewMod() {
        synchronized(playerLock) {
            Timber.d("newModCallback: show module data")
            handler.post(showNewModRunnable)
            canChangeViewer = true
        }
    }

    override fun onEndMod() {
        synchronized(playerLock) {
            Timber.d("endModCallback: end of module")
            stopUpdate = true
            canChangeViewer = false
        }
    }

    override fun onEndPlayCallback(result: Int) {
        synchronized(playerLock) {
            Timber.d("endPlayCallback: End progress thread")
            stopUpdate = true
            if (result != PlayerService.RESULT_OK) {
                val resultIntent = Intent().apply {
                    if (result == PlayerService.RESULT_CANT_OPEN_AUDIO) {
                        putExtra("error", getString(R.string.error_opensl))
                    } else if (result == PlayerService.RESULT_NO_AUDIO_FOCUS) {
                        putExtra("error", getString(R.string.error_audiofocus))
                    }
                }

                setResult(1, resultIntent)
            } else {
                setResult(RESULT_OK)
            }

            job?.cancel()
            finish()
        }
    }

    override fun onErrorMessage(msg: String) {
        showSnack(msg)
    }

    override fun onResume() {
        super.onResume()
        Timber.d("onResume")
        viewModel.screenOn(true)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Timber.d("onConfigurationChanged")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Timber.i("onNewIntent")

        var reconnect = false
        var fromHistory = false

        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) {
            Timber.i("Player started from history")
            fromHistory = true
        }

        val path: Uri? = intent.data

        if (path != null) {
            // from intent filter
            Timber.i("Player started from intent filter")
            fileList = listOf(path)
            shuffleMode = false
            loopListMode = false
            keepFirst = false
            start = 0
        } else if (fromHistory) {
            // Oops. We don't want to start service if launched from history and service is not running
            // so run the browser instead.
            Timber.i("Start file browser")
//            Intent(this, PlaylistMenu::class.java).apply {
//                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
//            }.also(::startActivity)
            TODO("Nav back to Home Screen")

            setResult(RESULT_OK)
            finish()

            return
        } else {
            val extras = intent.extras
            if (extras != null) {
                val app = XmpApplication.instance
                fileList = app!!.fileListUri
                shuffleMode = extras.getBoolean(PARM_SHUFFLE)
                loopListMode = extras.getBoolean(PARM_LOOP)
                keepFirst = extras.getBoolean(PARM_KEEPFIRST)
                start = extras.getInt(PARM_START)
                app.clearFileList()
            } else {
                reconnect = true
            }
        }

        val service = Intent(this, PlayerService::class.java)
        if (!reconnect) {
            Timber.i("Start service")
            startService(service)
        }
        if (!bindService(service, connection, 0)) {
            Timber.e("Can't bind to service")
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun startProgressCoroutine() {
        job = CoroutineScope(Dispatchers.IO).launch {
            Timber.i("Start progress coroutine")

            val frameStartTime = System.nanoTime()
            var frameTime: Long
            playTime = 0F

            while (isActive) {
                if (stopUpdate) {
                    Timber.i("Stop update")
                    break
                }

                synchronized(playerLock) {
                    if (modPlayer == null) {
                        return@synchronized
                    }

                    playTime = try {
                        modPlayer!!.time() / 100F
                    } catch (e: RemoteException) {
                        Timber.w("Failed to get Mod-Player time")
                        0F
                    }
                }

                if (viewModel.screenOn) {
                    withContext(Dispatchers.Main) {
                        // Need to be in Main Thread to update info
                        handler.post(updateInfoRunnable)
                    }
                }

                frameTime = (System.nanoTime() - frameStartTime) / 1000000
                if (frameTime < FRAME_RATE && !stopUpdate) {
                    delay(FRAME_RATE - frameTime)
                }

                if (playTime < 0) {
                    break
                }
            }

            synchronized(playerLock) {
                if (modPlayer == null) {
                    return@synchronized
                }

                try {
                    Timber.i("Flush interface update")
                    modPlayer!!.allowRelease() // finished playing, we can release the module
                } catch (e: RemoteException) {
                    Timber.e("Can't allow module release")
                }
            }
        }
    }

    private fun saveAllSeqPreference() {
        synchronized(playerLock) {
            if (modPlayer == null) {
                return@synchronized
            }
            try {
                // Write our all sequences button status to shared prefs
                val allSeq = modPlayer!!.getAllSequences()
                if (allSeq != PrefManager.allSequences) {
                    Timber.d("Write all sequences preference")
                    PrefManager.allSequences = allSeq
                }
            } catch (e: RemoteException) {
                Timber.e("Can't save all sequences preference")
            }
        }
    }

    private fun playNewMod(fileList: List<Uri>, start: Int) {
        synchronized(playerLock) {
            try {
                modPlayer?.play(fileList, start, shuffleMode, loopListMode, keepFirst)
            } catch (e: RemoteException) {
                Timber.e("Can't play module")
            }
        }
    }

    /**
     * Updates the Player Info text either by Hex or Numerical Value
     */
    private fun updateFrameInfo(
        value: Int,
        old: Int,
        update: (String) -> Unit
    ): Int {
        if (value != old) {
            s.delete(0, s.length)
            if (PrefManager.showHex) {
                Util.to02X(c, value)
                s.append(c)
            } else {
                value.let {
                    if (it < 10) s.append(0)
                    s.append(it)
                }
            }
            update(s.toString())
        }
        return value
    }

    private fun showSnack(message: String) {
        lifecycleScope.launch {
            snackbarHostState.showSnackbar(
                message = message
            )
        }
    }

    companion object {
        const val PARM_KEEPFIRST = "keepFirst"
        const val PARM_LOOP = "loop"
        const val PARM_SHUFFLE = "shuffle"
        const val PARM_START = "start"

        private var stopUpdate = false // this MUST be static (volatile doesn't work!)
        var canChangeViewer = false

        // Phone CPU's are more than capable enough to do more work with drawing.
        // With android O+, we can use hardware rendering on the canvas, if supported.
        private val newWaveform: Boolean by lazy { PrefManager.useBetterWaveform }
        private val FRAME_RATE: Int = 1000 / if (newWaveform) 50 else 30
    }
}

@Composable
private fun PlayerScreen(
    buttonState: PlayerViewModel.PlayerButtonsState,
    currentViewer: Int,
    drawerState: PlayerViewModel.PlayerDrawerState,
    infoState: PlayerViewModel.PlayerInfoState,
    insName: Array<String>,
    isMuted: BooleanArray,
    modVars: IntArray,
    patternInfo: PatternInfo = PatternInfo(),
    snackbarHostState: SnackbarHostState,
    timeState: PlayerViewModel.PlayerTimeState,
    uiState: PlayerViewModel.PlayerState,
    viewInfo: ViewerInfo,
    onAllSeq: (Boolean) -> Unit,
    onChangeViewer: () -> Unit,
    onDelete: () -> Unit,
    onIsSeeking: (Boolean) -> Unit,
    onMenu: () -> Unit,
    onMenuClose: () -> Unit,
    onMessage: () -> Unit,
    onNext: () -> Unit,
    onPlay: () -> Unit,
    onPrev: () -> Unit,
    onRepeat: (Boolean) -> Unit,
    onSeek: (Float) -> Unit,
    onSequence: (Int) -> Unit,
    onStop: () -> Unit
) {
    ModalNavigationDrawer(
        drawerState = drawerState.drawerState,
        gesturesEnabled = false,
        drawerContent = {
            PlayerDrawer(
                modifier = Modifier.systemBarsPadding(),
                onMessage = onMessage,
                onMenuClose = onMenuClose,
                moduleInfo = drawerState.moduleInfo,
                playAllSeq = drawerState.isPlayAllSequences,
                onAllSeq = onAllSeq,
                sequences = drawerState.numOfSequences,
                currentSequence = drawerState.currentSequence,
                onSequence = onSequence
            )
        }
    ) {
        val viewFlipperText by remember(uiState.infoTitle, uiState.infoType) {
            mutableStateOf(Pair(uiState.infoTitle, uiState.infoType))
        }
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                ViewFlipper(
                    actions = if (PrefManager.enableDelete) {
                        {
                            IconButton(onClick = onDelete) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null
                                )
                            }
                        }
                    } else {
                        null
                    },
                    navigation = {
                        IconButton(onClick = onMenu) {
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
                    PlayerInfo(
                        speed = infoState.infoSpeed,
                        bpm = infoState.infoBpm,
                        pos = infoState.infoPos,
                        pat = infoState.infoPat
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    PlayerSeekBar(
                        currentTime = timeState.timeNow,
                        totalTime = timeState.timeTotal,
                        position = timeState.seekPos,
                        range = timeState.seekMax,
                        isSeeking = timeState.isSeeking,
                        onSeek = onSeek,
                        onIsSeeking = onIsSeeking
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    PlayerControls(
                        onStop = onStop,
                        onPrev = onPrev,
                        onPlay = onPlay,
                        onNext = onNext,
                        onRepeat = onRepeat,
                        isPlaying = buttonState.isPlaying,
                        isRepeating = buttonState.isRepeating
                    )
                }
            }
        ) { paddingValues ->
            XmpCanvas(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                serviceConnected = uiState.serviceConnected,
                onChangeViewer = onChangeViewer,
                currentViewer = currentViewer,
                viewInfo = viewInfo,
                patternInfo = patternInfo,
                isMuted = isMuted,
                modVars = modVars,
                insName = insName
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_PlayerScreen() {
    val context = LocalContext.current
    PrefManager.init(context)

    val modVars by remember {
        val array = intArrayOf(190968, 30, 25, 12, 40, 18, 1, 0, 0, 0)
        mutableStateOf(array)
    }

    XmpTheme {
        PlayerScreen(
            snackbarHostState = SnackbarHostState(),
            uiState = PlayerViewModel.PlayerState(
                infoTitle = "Title 1",
                infoType = "Fast Tracker"
            ),
            infoState = PlayerViewModel.PlayerInfoState(
                infoSpeed = "11",
                infoBpm = "22",
                infoPos = "33",
                infoPat = "44"
            ),
            buttonState = PlayerViewModel.PlayerButtonsState(
                isPlaying = true,
                isRepeating = false
            ),
            timeState = PlayerViewModel.PlayerTimeState(
                timeNow = "00:00",
                timeTotal = "00:00",
                seekPos = 25f,
                seekMax = 100f
            ),
            drawerState = PlayerViewModel.PlayerDrawerState(
                drawerState = DrawerState(DrawerValue.Closed),
                moduleInfo = listOf(111, 222, 333, 444, 555),
                isPlayAllSequences = true,
                numOfSequences = List(8) { it },
                currentSequence = 2
            ),
            currentViewer = 1,
            viewInfo = composeViewerSampleData(),
            patternInfo = composePatternSampleData(),
            isMuted = BooleanArray(modVars[3]) { false },
            modVars = modVars,
            insName = Array(modVars[4]) { String.format("%02X %s", it + 1, "Instrument Name") },
            onMessage = { },
            onMenu = { },
            onMenuClose = { },
            onDelete = { },
            onAllSeq = { },
            onSequence = { },
            onSeek = { },
            onIsSeeking = { },
            onStop = { },
            onPrev = { },
            onPlay = { },
            onNext = { },
            onRepeat = { },
            onChangeViewer = { }
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_PlayerScreenDrawerOpen() {
    val context = LocalContext.current
    PrefManager.init(context)

    val modVars by remember {
        val array = intArrayOf(190968, 30, 25, 12, 40, 18, 1, 0, 0, 0)
        mutableStateOf(array)
    }

    XmpTheme {
        PlayerScreen(
            snackbarHostState = SnackbarHostState(),
            uiState = PlayerViewModel.PlayerState(
                infoTitle = "Title 1",
                infoType = "Fast Tracker"
            ),
            infoState = PlayerViewModel.PlayerInfoState(
                infoSpeed = "11",
                infoBpm = "22",
                infoPos = "33",
                infoPat = "44"
            ),
            buttonState = PlayerViewModel.PlayerButtonsState(
                isPlaying = true,
                isRepeating = false
            ),
            timeState = PlayerViewModel.PlayerTimeState(
                timeNow = "00:00",
                timeTotal = "00:00",
                seekPos = 25f,
                seekMax = 100f
            ),
            drawerState = PlayerViewModel.PlayerDrawerState(
                drawerState = DrawerState(DrawerValue.Open),
                moduleInfo = listOf(111, 222, 333, 444, 555),
                isPlayAllSequences = true,
                numOfSequences = List(8) { it },
                currentSequence = 2
            ),
            currentViewer = 0,
            viewInfo = composeViewerSampleData(),
            patternInfo = composePatternSampleData(),
            isMuted = BooleanArray(modVars[3]) { false },
            modVars = modVars,
            insName = Array(modVars[4]) { String.format("%02X %s", it + 1, "Instrument Name") },
            onMenu = { },
            onMenuClose = { },
            onDelete = { },
            onMessage = { },
            onAllSeq = { },
            onSequence = { },
            onSeek = { },
            onIsSeeking = { },
            onStop = { },
            onPrev = { },
            onPlay = { },
            onNext = { },
            onRepeat = { },
            onChangeViewer = { }
        )
    }
}
