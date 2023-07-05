package org.helllabs.android.xmp.core

import android.app.Activity
import android.app.ProgressDialog
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp.testModule
import org.helllabs.android.xmp.model.ModInfo
import org.helllabs.android.xmp.model.Playlist
import org.helllabs.android.xmp.model.PlaylistItem
import org.helllabs.android.xmp.util.Message.error
import org.helllabs.android.xmp.util.Message.toast
import java.io.File
import java.io.IOException

object PlaylistUtils {

    /*
	 * Send files to the specified playlist
	 */
    private fun addFiles(activity: Activity, fileList: List<String>, playlistName: String) {
        val modInfo = ModInfo()

        val list = fileList.mapNotNull { filename ->
            return@mapNotNull if (testModule(filename, modInfo)) {
                val item = PlaylistItem(PlaylistItem.TYPE_FILE, modInfo.name, modInfo.type).apply {
                    file = File(filename)
                }
                item
            } else {
                null
            }
        }.also { renumberIds(it) }

        if (list.isNotEmpty()) {
            Playlist.addToList(activity, playlistName, list)
            if (fileList.any { !testModule(it, modInfo) }) {
                activity.runOnUiThread {
                    if (list.size > 1) {
                        toast(activity, R.string.msg_only_valid_files_added)
                    } else {
                        error(activity, R.string.unrecognized_format)
                    }
                }
            }
        }
    }

    fun filesToPlaylist(activity: Activity, fileList: List<String>, playlistName: String) {
        val progressDialog =
            ProgressDialog.show(activity, "Please wait", "Scanning module files...", true)
        object : Thread() {
            override fun run() {
                addFiles(activity, fileList, playlistName)
                progressDialog.dismiss()
            }
        }.start()
    }

    fun filesToPlaylist(activity: Activity, filename: String, playlistName: String) {
        val fileList: MutableList<String> = ArrayList()
        fileList.add(filename)
        addFiles(activity, fileList, playlistName)
    }

    fun list(): Array<String> = PrefManager.DATA_DIR.list { _, name ->
        name.endsWith(Playlist.PLAYLIST_SUFFIX)
    } ?: arrayOf()

    fun listNoSuffix(): Array<String> = list().map {
        it.substringBeforeLast(Playlist.PLAYLIST_SUFFIX)
    }.toTypedArray()

    fun getPlaylistName(index: Int): String =
        list()[index].substringBeforeLast(Playlist.PLAYLIST_SUFFIX)

    fun createEmptyPlaylist(
        newName: String,
        newComment: String,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        try {
            Playlist(newName).apply {
                comment = newComment
            }.commit()
            onSuccess()
        } catch (e: IOException) {
            onError()
        }
    }

    // Stable IDs for used by Advanced RecyclerView
    fun renumberIds(list: List<PlaylistItem>) {
        list.forEachIndexed { index, item ->
            item.id = index
        }
    }
}
