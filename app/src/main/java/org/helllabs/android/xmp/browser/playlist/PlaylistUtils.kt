package org.helllabs.android.xmp.browser.playlist

import android.app.Activity
import android.app.ProgressDialog
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp.testModule
import org.helllabs.android.xmp.util.Message.error
import org.helllabs.android.xmp.util.Message.toast
import org.helllabs.android.xmp.util.ModInfo
import java.io.File
import java.io.IOException

object PlaylistUtils {

    /*
	 * Send files to the specified playlist
	 */
    private fun addFiles(activity: Activity, fileList: List<String>, playlistName: String) {
        val list: MutableList<PlaylistItem> = ArrayList()
        val modInfo = ModInfo()
        var hasInvalid = false
        for (filename in fileList) {
            if (testModule(filename, modInfo)) {
                val item = PlaylistItem(PlaylistItem.TYPE_FILE, modInfo.name, modInfo.type)
                item.file = File(filename)
                list.add(item)
            } else {
                hasInvalid = true
            }
        }
        if (list.isNotEmpty()) {
            Playlist.addToList(activity, playlistName, list)
            if (hasInvalid) {
                activity.runOnUiThread {
                    if (list.size > 1) {
                        toast(activity, R.string.msg_only_valid_files_added)
                    } else {
                        error(activity, R.string.unrecognized_format)
                    }
                }
            }
        }
        renumberIds(list.toList())
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

    fun list(): Array<String> {
        return PrefManager.DATA_DIR.list(PlaylistFilter()) ?: arrayOf()
    }

    fun listNoSuffix(): Array<String> {
        val pList = list()
        for (i in pList.indices) {
            pList[i] = pList[i].substring(0, pList[i].lastIndexOf(Playlist.PLAYLIST_SUFFIX))
        }
        return pList
    }

    fun getPlaylistName(index: Int): String {
        val pList = list()
        return pList[index].substring(
            0,
            pList[index].lastIndexOf(Playlist.PLAYLIST_SUFFIX)
        )
    }

    fun createEmptyPlaylist(
        name: String,
        comment: String,
        onSuccess: () -> Unit,
        onError: () -> Unit,
    ) {
        try {
            Playlist(name).apply {
                this.comment = comment
            }.commit()
            onSuccess()
        } catch (e: IOException) {
            onError()
        }
    }

    // Stable IDs for used by Advanced RecyclerView
    fun renumberIds(list: List<PlaylistItem>) {
        var id = 0
        for (item in list) {
            item.id = id++
        }
    }
}
