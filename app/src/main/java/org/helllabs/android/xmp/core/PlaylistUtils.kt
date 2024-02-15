package org.helllabs.android.xmp.core

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.Xmp.testModule
import org.helllabs.android.xmp.model.ModInfo
import org.helllabs.android.xmp.model.Playlist
import org.helllabs.android.xmp.model.PlaylistItem
import timber.log.Timber
import java.io.File
import java.io.IOException

sealed class PlaylistMessages {
    data object AddingFiles : PlaylistMessages()
    data object CantWriteToPlaylist : PlaylistMessages()
    data object UnrecognizedFormat : PlaylistMessages()
    data object ValidFormatsAdded : PlaylistMessages()
}

object PlaylistUtils {

    /*
	 * Send files to the specified playlist
	 */
    private fun addFiles(
        fileList: List<String>,
        playlistName: String,
        onMessage: (PlaylistMessages) -> Unit
    ) {
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
            Playlist.addToList(playlistName, list, onMessage)
            if (fileList.any { !testModule(it, modInfo) }) {
                if (list.size > 1) {
                    onMessage(PlaylistMessages.ValidFormatsAdded)
                } else {
                    onMessage(PlaylistMessages.UnrecognizedFormat)
                }
            }
        }
    }

    fun filesToPlaylist(
        fileList: List<String>,
        playlistName: String,
        onMessage: (PlaylistMessages) -> Unit
    ) {
        onMessage(PlaylistMessages.AddingFiles)
        Thread {
            addFiles(fileList, playlistName, onMessage)
        }.start()
    }

    fun filesToPlaylist(
        filename: String,
        playlistName: String,
        onMessage: (PlaylistMessages) -> Unit
    ) {
        val fileList: MutableList<String> = ArrayList()
        fileList.add(filename)
        addFiles(fileList, playlistName, onMessage)
    }

    @Deprecated("")
    fun list(): Array<String> = PrefManager.DATA_DIR.list { _, name ->
        name.endsWith(Playlist.PLAYLIST_SUFFIX)
    } ?: arrayOf()

    @Deprecated("")
    fun listNoSuffix(): Array<String> = list().map {
        it.substringBeforeLast(Playlist.PLAYLIST_SUFFIX)
    }.toTypedArray()

    @Deprecated("")
    fun getPlaylistName(index: Int): String =
        list()[index].substringBeforeLast(Playlist.PLAYLIST_SUFFIX)

    fun listNoSuffix2(context: Context): List<String> {
        val parentUri = Uri.parse(PrefManager.safStoragePath)

        val playlistsDir = DocumentFile.fromTreeUri(context, parentUri)?.findFile("playlists")
        if (playlistsDir != null && playlistsDir.isFile) {
            Timber.e("Playlist Directory returned null or is file!")
            return emptyList()
        }

        val list = mutableListOf<String>()
        playlistsDir!!.listFiles().forEach {
            if (it.name!!.endsWith(Playlist.PLAYLIST_SUFFIX)) {
                list.add(it.name!!.substringBeforeLast(Playlist.PLAYLIST_SUFFIX))
            }
        }

        return list.toList()
    }

    @Deprecated("Use createEmptyPlaylist2")
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

    fun createEmptyPlaylist2(
        context: Context,
        playlistsDir: Uri,
        newName: String,
        newComment: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        Playlist(newName).apply {
            this.comment = newComment
            this.playlistsDir = playlistsDir
        }.commit2(
            context = context,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    // Stable IDs for used by Advanced RecyclerView
    fun renumberIds(list: List<PlaylistItem>) {
        list.forEachIndexed { index, item ->
            item.id = index
        }
    }
}
