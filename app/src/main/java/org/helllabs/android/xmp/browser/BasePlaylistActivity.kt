package org.helllabs.android.xmp.browser

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.browser.playlist.PlaylistAdapter
import org.helllabs.android.xmp.compose.ui.menu.PlaylistMenu
import org.helllabs.android.xmp.player.PlayerActivity
import org.helllabs.android.xmp.service.ModInterface
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.util.InfoCache.testModule
import org.helllabs.android.xmp.util.InfoCache.testModuleForceIfInvalid
import org.helllabs.android.xmp.util.Log.i
import org.helllabs.android.xmp.util.Message.toast
import timber.log.Timber

abstract class BasePlaylistActivity : AppCompatActivity() {

    private var mAddList: MutableList<String>? = null
    private var mModPlayer: ModInterface? = null
    private var mShowToasts = false
    private var refresh = false
    protected lateinit var mPlaylistAdapter: PlaylistAdapter

    private val playAllButtonListener: View.OnClickListener = View.OnClickListener {
        if (allFiles.isEmpty()) {
            toast(this@BasePlaylistActivity, R.string.error_no_files_to_play)
        } else {
            playModule(allFiles)
        }
    }

    private val toggleLoopButtonListener: View.OnClickListener =
        View.OnClickListener { view ->
            var loopMode: Boolean = isLoopMode
            loopMode = loopMode xor true
            (view as ImageButton).setImageResource(if (loopMode) R.drawable.list_loop_on else R.drawable.list_loop_off)
            if (mShowToasts) {
                toast(
                    view.getContext(),
                    if (loopMode) R.string.msg_loop_on else R.string.msg_loop_off
                )
            }
            isLoopMode = loopMode
        }

    private val toggleShuffleButtonListener: View.OnClickListener =
        View.OnClickListener { view ->
            var shuffleMode: Boolean = isShuffleMode
            shuffleMode = shuffleMode xor true
            (view as ImageButton).setImageResource(if (shuffleMode) R.drawable.list_shuffle_on else R.drawable.list_shuffle_off)
            if (mShowToasts) {
                toast(
                    view.getContext(),
                    if (shuffleMode) R.string.msg_shuffle_on else R.string.msg_shuffle_off
                )
            }
            isShuffleMode = shuffleMode
        }

    // Connection
    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            mModPlayer = ModInterface.Stub.asInterface(service)
            try {
                mModPlayer!!.add(mAddList)
            } catch (e: RemoteException) {
                toast(this@BasePlaylistActivity, R.string.error_adding_mod)
            }
            unbindService(this)
        }

        override fun onServiceDisconnected(className: ComponentName) {
            mModPlayer = null
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mShowToasts = PrefManager.showToast

        // Action bar icon navigation
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    public override fun onResume() {
        super.onResume()
        if (refresh) {
            update()
        }
    }

    protected abstract var isShuffleMode: Boolean
    protected abstract var isLoopMode: Boolean
    protected abstract val allFiles: List<String>
    protected abstract fun update()

    protected fun setSwipeRefresh(recyclerView: RecyclerView) {
        val swipeRefresh = findViewById<View>(R.id.swipeContainer) as SwipeRefreshLayout
        swipeRefresh.setOnRefreshListener {
            update()
            swipeRefresh.isRefreshing = false
        }
        swipeRefresh.setColorSchemeResources(R.color.refresh_color)
        recyclerView.addOnItemTouchListener(object : OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                if (e.action == MotionEvent.ACTION_DOWN) {
                    var enable = false
                    if (recyclerView.childCount > 0) {
                        enable = !recyclerView.canScrollVertically(-1)
                    }
                    swipeRefresh.isEnabled = enable
                }
                return false
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                // do nothing
            }

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                // do nothing
            }
        })
    }

    protected fun setupButtons() {
        val playAllButton = findViewById<View>(R.id.play_all) as ImageButton
        val toggleLoopButton = findViewById<View>(R.id.toggle_loop) as ImageButton
        val toggleShuffleButton = findViewById<View>(R.id.toggle_shuffle) as ImageButton
        playAllButton.setImageResource(R.drawable.list_play)
        playAllButton.setOnClickListener(playAllButtonListener)
        toggleLoopButton.setImageResource(if (isLoopMode) R.drawable.list_loop_on else R.drawable.list_loop_off)
        toggleLoopButton.setOnClickListener(toggleLoopButtonListener)
        toggleShuffleButton.setImageResource(if (isShuffleMode) R.drawable.list_shuffle_on else R.drawable.list_shuffle_off)
        toggleShuffleButton.setOnClickListener(toggleShuffleButtonListener)
    }

    open fun onItemClick(adapter: PlaylistAdapter, view: View?, position: Int) {
        val filename = adapter.getItem(position).file?.path.orEmpty()
        val mode = PrefManager.playlistMode

        /* Test module again if invalid, in case a new file format is added to the
		 * player library and the file was previously unrecognized and cached as invalid.
		 */
        if (testModuleForceIfInvalid(filename)) {
            when (mode) {
                1 -> {
                    val count = position - adapter.directoryCount
                    if (count >= 0) {
                        playModule(adapter.filenameList, count, isShuffleMode)
                    }
                }
                2 -> playModule(filename)
                3 -> {
                    addToQueue(filename)
                    toast(this, "Added to queue")
                }
            }
        } else {
            toast(this, "Unrecognized file format")
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        i(TAG, "Activity result $requestCode,$resultCode")
        when (requestCode) {
            SETTINGS_REQUEST -> {
                update()
                mShowToasts = PrefManager.showToast
            }
            PLAY_MOD_REQUEST -> if (resultCode != RESULT_OK) {
                update()
            }
            SEARCH_REQUEST -> refresh = true
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
            toast(this, R.string.msg_only_valid_files_sent)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                val intent = Intent(this, PlaylistMenu::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val TAG = "PlaylistActivity"
        private const val SETTINGS_REQUEST = 45
        private const val PLAY_MOD_REQUEST = 669
        private const val SEARCH_REQUEST = 47
    }
}