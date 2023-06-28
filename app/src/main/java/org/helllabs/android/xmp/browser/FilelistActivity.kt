package org.helllabs.android.xmp.browser

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h6ah4i.android.widget.advrecyclerview.decoration.SimpleListDividerDecorator
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.browser.playlist.PlaylistAdapter
import org.helllabs.android.xmp.browser.playlist.PlaylistItem
import org.helllabs.android.xmp.browser.playlist.PlaylistUtils
import org.helllabs.android.xmp.pluscubed.recyclerfastscroll.RecyclerFastScroller
import org.helllabs.android.xmp.util.Crossfader
import org.helllabs.android.xmp.util.FileUtils.basename
import org.helllabs.android.xmp.util.InfoCache.clearCache
import org.helllabs.android.xmp.util.InfoCache.delete
import org.helllabs.android.xmp.util.InfoCache.deleteRecursive
import org.helllabs.android.xmp.util.Log.i
import org.helllabs.android.xmp.util.Message.error
import org.helllabs.android.xmp.util.Message.toast
import org.helllabs.android.xmp.util.Message.yesNoDialog
import java.io.File
import java.text.DateFormat

class FilelistActivity : BasePlaylistActivity(), PlaylistAdapter.OnItemClickListener {

    override var isLoopMode = false
    override var isShuffleMode = false
    private var curPath: TextView? = null
    private var isPathMenu = false
    private var mBackButtonParentdir = false
    private var mCrossfade: Crossfader? = null
    private var mNavigation: FilelistNavigation? = null
    private var recyclerView: RecyclerView? = null

    /**
     * Recursively add current directory to playlist
     */
    private val addCurrentRecursiveChoice: PlaylistChoice = object : PlaylistChoice {
        override fun execute(fileSelection: Int, playlistSelection: Int) {
            PlaylistUtils.filesToPlaylist(
                this@FilelistActivity,
                recursiveList(mNavigation?.currentDir),
                PlaylistUtils.getPlaylistName(playlistSelection)
            )
        }
    }

    /**
     * Recursively add directory to playlist
     */
    private val addRecursiveToPlaylistChoice: PlaylistChoice = object : PlaylistChoice {
        override fun execute(fileSelection: Int, playlistSelection: Int) {
            PlaylistUtils.filesToPlaylist(
                this@FilelistActivity,
                recursiveList(
                    mPlaylistAdapter.getFile(fileSelection)
                ),
                PlaylistUtils.getPlaylistName(playlistSelection)
            )
        }
    }

    /**
     * Add one file to playlist
     */
    private val addFileToPlaylistChoice: PlaylistChoice = object : PlaylistChoice {
        override fun execute(fileSelection: Int, playlistSelection: Int) {
            PlaylistUtils.filesToPlaylist(
                this@FilelistActivity,
                mPlaylistAdapter.getFilename(fileSelection),
                PlaylistUtils.getPlaylistName(playlistSelection)
            )
        }
    }

    /**
     * Add file list to playlist
     */
    private val addFileListToPlaylistChoice: PlaylistChoice = object : PlaylistChoice {
        override fun execute(fileSelection: Int, playlistSelection: Int) {
            PlaylistUtils.filesToPlaylist(
                this@FilelistActivity,
                mPlaylistAdapter.filenameList,
                PlaylistUtils.getPlaylistName(playlistSelection)
            )
        }
    }

    /**
     * For actions based on playlist selection made using choosePlaylist()
     */
    private interface PlaylistChoice {
        fun execute(fileSelection: Int, playlistSelection: Int)
    }

    override val allFiles: List<String>
        get() = recursiveList(mNavigation?.currentDir)

    override fun onItemClick(adapter: PlaylistAdapter, view: View?, position: Int) {
        val file = mPlaylistAdapter.getFile(position)
        if (mNavigation!!.changeDirectory(file)) {
            mNavigation!!.saveListPosition(recyclerView)
            updateModlist()
        } else {
            super.onItemClick(adapter, view, position)
        }
    }

    private fun pathNotFound(mediaPath: String) {
        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.setTitle("Path not found")
        alertDialog.setMessage("$mediaPath not found. Create this directory or change the module path.")
        alertDialog.setButton(
            AlertDialog.BUTTON_POSITIVE,
            getString(R.string.create)
        ) { _, _ ->
            val ret = Examples.install(
                this@FilelistActivity,
                mediaPath,
                PrefManager.examples
            )
            if (ret < 0) {
                error(this@FilelistActivity, "Error creating directory $mediaPath.")
            }
            mNavigation!!.startNavigation(File(mediaPath))
            updateModlist()
        }
        alertDialog.setButton(
            AlertDialog.BUTTON_NEGATIVE,
            getString(R.string.cancel)
        ) { _, _ ->
            finish()
        }
        alertDialog.show()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.modlist)
        recyclerView = findViewById<View>(R.id.modlist_listview) as RecyclerView
        setSwipeRefresh(recyclerView!!)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        recyclerView!!.layoutManager = layoutManager
        mPlaylistAdapter = PlaylistAdapter(
            this,
            ArrayList(),
            false,
            PlaylistAdapter.LAYOUT_LIST
        )
        mPlaylistAdapter.setOnItemClickListener(this)
        recyclerView!!.adapter = mPlaylistAdapter
        recyclerView!!.addItemDecoration(
            SimpleListDividerDecorator(resources.getDrawable(R.drawable.list_divider), true)
        )

        // fast scroll
        val fastScroller = findViewById<View>(R.id.fast_scroller) as RecyclerFastScroller
        fastScroller.attachRecyclerView(recyclerView!!)
        registerForContextMenu(recyclerView)
        val mediaPath = PrefManager.mediaPath
        setTitle(R.string.browser_filelist_title)
        mNavigation = FilelistNavigation()
        mCrossfade = Crossfader(this)
        mCrossfade!!.setup(R.id.modlist_content, R.id.modlist_spinner)
        curPath = findViewById<View>(R.id.current_path) as TextView
        registerForContextMenu(curPath)
        mBackButtonParentdir = PrefManager.backButtonNavigation
        val textColor = curPath!!.currentTextColor
        curPath!!.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                curPath!!.setTextColor(textColor)
            } else {
                curPath!!.setTextColor(resources.getColor(R.color.pressed_color))
            }
            view.performClick()
            false
        }

        // Check if directory exists
        val modDir = File(mediaPath)
        if (modDir.isDirectory) {
            mNavigation!!.startNavigation(modDir)
            updateModlist()
        } else {
            pathNotFound(mediaPath.orEmpty())
        }
        isShuffleMode = PrefManager.shuffleMode
        isLoopMode = PrefManager.loopMode
        setupButtons()
    }

    private fun parentDir() {
        if (mNavigation!!.parentDir()) {
            updateModlist()
            mNavigation!!.restoreListPosition(recyclerView)
        }
    }

    fun upButtonClick(view: View?) {
        parentDir()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // if (mBackButtonParentdir) {
            // Return to parent dir up to the starting level, then act as regular back
            if (!mNavigation!!.isAtTopDir) {
                parentDir()
                return true
            }
            // }
        }
        return super.onKeyDown(keyCode, event)
    }

    public override fun onDestroy() {
        super.onDestroy()
        var saveModes = false
        if (isShuffleMode != PrefManager.shuffleMode) {
            saveModes = true
        }
        if (isLoopMode != PrefManager.loopMode) {
            saveModes = true
        }
        if (saveModes) {
            i(TAG, "Save new file list preferences")
            PrefManager.loopMode = isLoopMode
            PrefManager.shuffleMode = isShuffleMode
        }
    }

    public override fun update() {
        updateModlist()
    }

    private fun updateModlist() {
        val modDir = mNavigation?.currentDir ?: return
        mPlaylistAdapter.clear()
        curPath!!.text = modDir.path
        val list: MutableList<PlaylistItem> = ArrayList()
        val dirFiles = modDir.listFiles()
        if (dirFiles != null) {
            for (file in dirFiles) {
                val item: PlaylistItem = if (file.isDirectory) {
                    PlaylistItem(
                        PlaylistItem.TYPE_DIRECTORY,
                        file.name,
                        getString(R.string.directory)
                    )
                } else {
                    val date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM)
                        .format(file.lastModified())
                    val comment = date + String.format(" (%d kB)", file.length() / 1024)
                    PlaylistItem(PlaylistItem.Companion.TYPE_FILE, file.name, comment)
                }
                item.file = file
                list.add(item)
            }
        }
        list.sort()
        PlaylistUtils.renumberIds(list)
        mPlaylistAdapter.addList(list)
        mPlaylistAdapter.notifyDataSetChanged()
        mCrossfade!!.crossfade()
    }

    private fun deleteDirectory(position: Int) {
        val deleteName = mPlaylistAdapter.getFilename(position)
        val mediaPath = PrefManager.mediaPath
        if (deleteName.startsWith(mediaPath) && deleteName != mediaPath) {
            yesNoDialog(
                this,
                "Delete directory",
                "Are you sure you want to delete directory \"" +
                    basename(deleteName) + "\" and all its contents?"
            ) {
                if (deleteRecursive(deleteName)) {
                    updateModlist()
                    toast(this@FilelistActivity, getString(R.string.msg_dir_deleted))
                } else {
                    toast(this@FilelistActivity, getString(R.string.msg_cant_delete_dir))
                }
            }
        } else {
            toast(this, R.string.error_dir_not_under_moddir)
        }
    }

    private fun choosePlaylist(fileSelection: Int, choice: PlaylistChoice) {
        // Return if no playlists exist
        if (PlaylistUtils.list().isEmpty()) {
            toast(this, getString(R.string.msg_no_playlists))
            return
        }
        val playlistSelection = IntArray(1)
        val listener = DialogInterface.OnClickListener { _, which ->
            if (which == DialogInterface.BUTTON_POSITIVE && playlistSelection[0] >= 0) {
                choice.execute(fileSelection, playlistSelection[0])
            }
        }
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.msg_select_playlist)
            .setPositiveButton(R.string.ok, listener)
            .setNegativeButton(R.string.cancel, listener)
            .setSingleChoiceItems(
                PlaylistUtils.listNoSuffix(),
                0
            ) { _, which ->
                playlistSelection[0] = which
            }
            .show()
    }

    private fun clearCachedEntries(fileList: List<String>) {
        for (filename in fileList) {
            clearCache(filename)
        }
    }

    // Playlist context menu
    override fun onCreateContextMenu(menu: ContextMenu?, view: View?, menuInfo: ContextMenuInfo?) {
        super.onCreateContextMenu(menu, view, menuInfo)

        if (menu == null) {
            return
        }

        if (view == curPath) {
            isPathMenu = true
            menu.setHeaderTitle("All files")
            menu.add(Menu.NONE, 0, 0, "Add to playlist")
            menu.add(Menu.NONE, 1, 1, "Recursive add to playlist")
            menu.add(Menu.NONE, 2, 2, "Add to play queue")
            menu.add(Menu.NONE, 3, 3, "Set as default path")
            menu.add(Menu.NONE, 4, 4, "Clear cache")
            return
        }
        isPathMenu = false
        val position = mPlaylistAdapter.position
        if (mPlaylistAdapter.getFile(position)!!.isDirectory) { // For directory
            menu.setHeaderTitle("This directory")
            menu.add(Menu.NONE, 0, 0, "Add to playlist")
            menu.add(Menu.NONE, 1, 1, "Add to play queue")
            menu.add(Menu.NONE, 2, 2, "Play contents")
            menu.add(Menu.NONE, 3, 3, "Delete directory")
        } else { // For files
            val mode = PrefManager.playlistMode
            menu.setHeaderTitle("This file")
            menu.add(Menu.NONE, 0, 0, "Add to playlist")
            if (mode != 3) {
                menu.add(Menu.NONE, 1, 1, "Add to play queue")
            }
            if (mode != 2) {
                menu.add(Menu.NONE, 2, 2, "Play this file")
            }
            if (mode != 1) {
                menu.add(Menu.NONE, 3, 3, "Play all starting here")
            }
            menu.add(Menu.NONE, 4, 4, "Delete file")
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (isPathMenu) {
            when (id) {
                0 -> choosePlaylist(0, addFileListToPlaylistChoice)
                1 -> choosePlaylist(0, addCurrentRecursiveChoice)
                2 -> addToQueue(mPlaylistAdapter.filenameList)
                3 -> {
                    PrefManager.mediaPath = mNavigation?.currentDir?.path.toString()
                    toast(this, "Set as default module path")
                }
                4 -> clearCachedEntries(mPlaylistAdapter.filenameList)
            }
            return true
        }
        val position = mPlaylistAdapter.position
        if (mPlaylistAdapter.getFile(position)!!.isDirectory) { // Directories
            when (id) {
                0 -> choosePlaylist(position, addRecursiveToPlaylistChoice)
                1 -> addToQueue(recursiveList(mPlaylistAdapter.getFile(position)))
                2 -> playModule(recursiveList(mPlaylistAdapter.getFile(position)))
                3 -> deleteDirectory(position)
            }
        } else { // Files
            when (id) {
                0 -> choosePlaylist(position, addFileToPlaylistChoice)
                1 -> addToQueue(mPlaylistAdapter.getFilename(position))
                2 -> playModule(mPlaylistAdapter.getFilename(position))
                3 -> playModule(mPlaylistAdapter.filenameList, position)
                4 -> {
                    val deleteName = mPlaylistAdapter.getFilename(position)
                    yesNoDialog(
                        this,
                        "Delete",
                        "Are you sure you want to delete " + basename(deleteName) + "?"
                    ) {
                        if (delete(deleteName)) {
                            updateModlist()
                            toast(this@FilelistActivity, getString(R.string.msg_file_deleted))
                        } else {
                            toast(this@FilelistActivity, getString(R.string.msg_cant_delete))
                        }
                    }
                }
            }
        }
        return true
    }

    companion object {
        private const val TAG = "BasePlaylistActivity"

        private fun recursiveList(file: File?): List<String> {
            val list: MutableList<String> = ArrayList()
            if (file == null) {
                return list
            }
            if (file.isDirectory) {
                val fileArray = file.listFiles()
                if (fileArray != null) { // prevent crash reported in dev console
                    for (f in fileArray) {
                        if (f.isDirectory) {
                            list.addAll(recursiveList(f))
                        } else {
                            list.add(f.path)
                        }
                    }
                }
            } else {
                list.add(file.path)
            }
            return list
        }
    }
}