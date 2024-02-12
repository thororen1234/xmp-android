package org.helllabs.android.xmp.compose.ui

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.compose.ui.player.PlayerActivity
import org.helllabs.android.xmp.core.InfoCache
import org.helllabs.android.xmp.model.PlaylistItem
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

    internal lateinit var snackbarHostState: SnackbarHostState

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

    private var mAddList: MutableList<String>? = null
    private var mModPlayer: PlayerService? = null

    protected abstract val isShuffleMode: Boolean
    protected abstract val isLoopMode: Boolean
    protected abstract val allFiles: List<String>
    protected abstract fun update()

    internal fun showSnack(message: String, actionLabel: String? = null) = lifecycleScope.launch {
        snackbarHostState.showSnackbar(message = message, actionLabel = actionLabel)
    }

    // Connection
    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            mModPlayer = (service as PlayerService.LocalBinder).getService()
            try {
                // TODO ehh? Should care about null or empty
                mModPlayer!!.add(mAddList?.toList().orEmpty())
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
        snackbarHostState = SnackbarHostState()
    }

    public override fun onResume() {
        super.onResume()
        update()
    }

    /**
     * Play all `playable` modules in the current path we're in.
     */
    open fun onPlayAll() {
        if (allFiles.isEmpty()) {
            showSnack(getString(R.string.error_no_files_to_play))
            return
        }

        playModule(modList = allFiles)
    }

    open fun onItemClick(
        items: List<PlaylistItem>,
        filenameList: List<String>,
        directoryCount: Int,
        position: Int
    ) {
        fun playAllStaringAtPosition() {
            val count = position - directoryCount
            if (count < 0) {
                throw RuntimeException("Play count is negative")
            }
            playModule(modList = filenameList, start = count, keepFirst = isShuffleMode)
        }

        fun playThisFile() {
            val filename = items[position].file?.path
            if (filename.isNullOrEmpty()) {
                showSnack("Invalid file path")
                return
            }
            if (!InfoCache.testModuleForceIfInvalid(filename)) {
                showSnack("Unrecognized file format")
                return
            }
            val item = listOf(filename)
            playModule(modList = item)
        }

        fun addToQueue() {
            val filename = items[position].file?.path
            if (filename.isNullOrEmpty()) {
                showSnack("Invalid file path")
                return
            }
            if (!InfoCache.testModuleForceIfInvalid(filename)) {
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
        when (PrefManager.playlistMode) {
            1 -> playAllStaringAtPosition()
            2 -> playThisFile()
            3 -> addToQueue()
        }
    }

    // TODO: We're not keeping first with shuffle enabled when clicking an item
    protected fun playModule(modList: List<String>, start: Int = 0, keepFirst: Boolean = false) {
        (application as XmpApplication).fileList = modList.toMutableList()
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

    protected fun addToQueue(filename: String) {
        if (InfoCache.testModule(filename)) {
            if (PlayerService.isAlive) {
                val service = Intent(this, PlayerService::class.java)
                mAddList = listOf(filename).toMutableList()
                bindService(service, connection, 0)
            } else {
                val item = listOf(filename)
                playModule(modList = item)
            }
        }
    }

    protected fun addToQueue(list: List<String>) {
        val realList: MutableList<String> = ArrayList()
        var realSize = 0
        var invalid = false
        for (filename in list) {
            if (InfoCache.testModule(filename)) {
                realList.add(filename)
                realSize++
            } else {
                invalid = true
            }
        }
        if (invalid) {
            showSnack(getString(R.string.msg_only_valid_files_sent))
        }
        if (realSize > 0) {
            if (PlayerService.isAlive) {
                val service = Intent(this, PlayerService::class.java)
                mAddList = realList
                bindService(service, connection, 0)
            } else {
                playModule(modList = realList)
            }
        }
    }
}
