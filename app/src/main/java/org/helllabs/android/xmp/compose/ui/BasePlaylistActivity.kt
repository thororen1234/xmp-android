package org.helllabs.android.xmp.compose.ui

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.compose.ui.player.PlayerActivity
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.service.PlayerBinder
import org.helllabs.android.xmp.service.PlayerService
import timber.log.Timber

/**
 * Base activity for [org.helllabs.android.xmp.compose.ui.filelist.FileListActivity]
 * and [org.helllabs.android.xmp.compose.ui.playlist.PlaylistActivity].
 *
 * Contains things needed for [ServiceConnection] and [PlayerService]
 *      to play modules, queue files, and to provide shuffle and loop commands.
 */
abstract class BasePlaylistActivity : ComponentActivity() {

    internal lateinit var snackBarHostState: SnackbarHostState

    private val playerContract = ActivityResultContracts.StartActivityForResult()
    private var playerResult = registerForActivityResult(playerContract) { result ->
        if (result.resultCode == 1) {
            result.data?.getStringExtra("error")?.let {
                Timber.w("Result with error: $it")
                showSnack(it)
            }
        }
        if (result.resultCode == 2) {
            update()
        }
    }

    private var mAddList: List<Uri> = listOf()
    private var mModPlayer: PlayerService? = null

    protected abstract val isShuffleMode: Boolean
    protected abstract val isLoopMode: Boolean

    @Deprecated("This is Blocking")
    protected abstract val allFiles: List<Uri>

    protected abstract fun update()

    protected abstract suspend fun getAllFiles(): List<Uri>

    internal fun showSnack(message: String, actionLabel: String? = null) = lifecycleScope.launch {
        snackBarHostState.showSnackbar(message = message, actionLabel = actionLabel)
    }

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        snackBarHostState = SnackbarHostState()
    }

    public override fun onResume() {
        super.onResume()
        update()
    }

    /**
     * Play all `playable` modules in the current path we're in.
     */
    @Deprecated("This is Blocking")
    open fun onPlayAll() {
        val files = allFiles
        if (files.isEmpty()) {
            showSnack(getString(R.string.error_no_files_to_play))
            return
        }

        playModule(modList = files)
    }

    open fun onPlayAll2() {
        lifecycleScope.launch {
            val files = getAllFiles()

            if (files.isEmpty()) {
                showSnack(getString(R.string.error_no_files_to_play))
                return@launch
            }

            playModule(modList = files)
        }
    }

    open fun onItemClick(
        items: List<Uri>,
        directoryCount: Int,
        position: Int
    ) {
        fun playAllStaringAtPosition() {
            val count = position - directoryCount
            if (count < 0) {
                throw RuntimeException("Play count is negative")
            }
            playModule(modList = items, start = count, keepFirst = true)
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
            playModule(filename)
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
            addToQueue(filename)
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

    protected fun playModule(uri: Uri?) {
        if (uri == null) {
            showSnack("Null uri when playing module")
            return
        }

        listOf(uri).also(::playModule)
    }

    // TODO: We're not keeping first with shuffle enabled when clicking an item
    protected fun playModule(modList: List<Uri>, start: Int = 0, keepFirst: Boolean = false) {
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
            playerResult.launch(intent)
        }
    }

    /**
     * Add an URI to the queue.
     *
     * If the service alive, connect to it and append the play queue. @see [connection].
     * Else start the service normally with @see [playModule].
     */
    protected fun addToQueue(uri: Uri?) {
        if (uri == null) {
            showSnack("Null uri when adding to Queue")
            return
        }

        listOf(uri).also(::addToQueue)
    }

    /**
     * Add a list of URI's to the queue.
     *
     * If the service alive, connect to it and append the play queue. @see [connection].
     * Else start the service normally with @see [playModule].
     */
    protected fun addToQueue(list: List<Uri>) {
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
            playModule(modList = list)
        }
    }
}
