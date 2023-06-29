package org.helllabs.android.xmp.compose.ui

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.browser.playlist.PlaylistItem
import org.helllabs.android.xmp.player.PlayerActivity
import org.helllabs.android.xmp.service.ModInterface
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.util.InfoCache.testModule
import org.helllabs.android.xmp.util.InfoCache.testModuleForceIfInvalid
import timber.log.Timber

/**
 * Base activity for [FileListActivity] and [PlaylistActivity].
 * Contains things needed for [ServiceConnection] and [PlayerService]
 *      to play modules, queue files, and to provide shuffle and loop commands.
 */
abstract class BasePlaylistActivity : ComponentActivity() {

    internal lateinit var snackbarHostState: SnackbarHostState

    private var mAddList: MutableList<String>? = null
    private var mModPlayer: ModInterface? = null

    protected abstract val isShuffleMode: Boolean
    protected abstract val isLoopMode: Boolean
    protected abstract val allFiles: List<String>
    protected abstract fun update()

    private fun showSnack(message: String) {
        lifecycleScope.launch {
            snackbarHostState.showSnackbar(
                message = message
            )
        }
    }

    // Connection
    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            mModPlayer = ModInterface.Stub.asInterface(service)
            try {
                mModPlayer!!.add(mAddList)
            } catch (e: RemoteException) {
                lifecycleScope.launch {
                    snackbarHostState.showSnackbar(
                        message = getString(R.string.error_adding_mod)
                    )
                }
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
        } else {
            playModule(allFiles)
        }
    }

    open fun onItemClick(
        items: List<PlaylistItem>,
        filenameList: List<String>,
        directoryCount: Int,
        position: Int
    ) {
        val filename = items[position].file?.path.orEmpty()
        val mode = PrefManager.playlistMode

        /**
         * Test module again if invalid, in case a new file format is added to the
         * player library and the file was previously unrecognized and cached as invalid.
         */
        if (testModuleForceIfInvalid(filename)) {
            /**
             * mode:
             * 1. Start playing at selection
             * 2. Play selected file
             * 3. Enqueue selected file
             */
            when (mode) {
                1 -> {
                    val count = position - directoryCount
                    if (count >= 0) {
                        playModule(filenameList, count, isShuffleMode)
                    }
                }
                2 -> playModule(filename)
                3 -> {
                    addToQueue(filename)
                    showSnack("Added to queue")
                }
            }
        } else {
            showSnack("Unrecognized file format")
        }
    }

    // Play this module
    protected fun playModule(mod: String) {
        val modList = listOf(mod)
        playModule(modList, 0, false)
    }

    // Play all modules in list and honor default shuffle mode
    protected fun playModule(modList: List<String>) {
        playModule(modList, 0, false)
    }

    protected fun playModule(modList: List<String>, start: Int) {
        playModule(modList, start, false)
    }

    private fun playModule(modList: List<String>, start: Int, keepFirst: Boolean) {
        (application as XmpApplication).fileList = modList.toMutableList()
        Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.PARM_SHUFFLE, isShuffleMode)
            putExtra(PlayerActivity.PARM_LOOP, isLoopMode)
            putExtra(PlayerActivity.PARM_START, start)
            putExtra(PlayerActivity.PARM_KEEPFIRST, keepFirst)
        }.also { intent ->
            Timber.i("Start Player activity")
            startActivityForResult(intent, PLAY_MOD_REQUEST)
        }
    }

    protected fun addToQueue(filename: String?) {
        if (testModule(filename!!)) {
            if (PlayerService.isAlive) {
                val service = Intent(this, PlayerService::class.java)
                mAddList = ArrayList()
                mAddList!!.add(filename)
                bindService(service, connection, 0)
            } else {
                playModule(filename)
            }
        }
    }

    protected fun addToQueue(list: List<String>) {
        val realList: MutableList<String> = ArrayList()
        var realSize = 0
        var invalid = false
        for (filename in list) {
            if (testModule(filename)) {
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
                playModule(realList)
            }
        }
    }

    companion object {
        private const val PLAY_MOD_REQUEST = 669
    }
}
