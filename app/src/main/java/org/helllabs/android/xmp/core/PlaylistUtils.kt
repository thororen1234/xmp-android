// package org.helllabs.android.xmp.core
//
// import android.content.Context
// import android.net.Uri
// import org.helllabs.android.xmp.Xmp
// import org.helllabs.android.xmp.model.ModInfo
// import org.helllabs.android.xmp.model.Playlist
// import org.helllabs.android.xmp.model.PlaylistItem
// import timber.log.Timber
// import java.io.File
// import java.io.IOException
//
// sealed class PlaylistMessages {
//    data object AddingFiles : PlaylistMessages()
//    data object CantWriteToPlaylist : PlaylistMessages()
//    data object UnrecognizedFormat : PlaylistMessages()
//    data object ValidFormatsAdded : PlaylistMessages()
// }
//
// object PlaylistUtils {
//
//    /*
// 	 * Send files to the specified playlist
// 	 */
//    private fun addFiles(
//        fileList: List<Uri>,
//        playlistName: String,
//        onMessage: (PlaylistMessages) -> Unit
//    ) {
//        val modInfo = ModInfo()
//
//        val list = fileList.mapNotNull { filename ->
//            return@mapNotNull if (Xmp.testFromFd(filename, modInfo)) {
//                val item = PlaylistItem(PlaylistItem.TYPE_FILE, modInfo.name, modInfo.type).apply {
//                    // file = File(filename)
//                }
//                item
//            } else {
//                null
//            }
//        }.also { renumberIds(it) }
//
//        if (list.isNotEmpty()) {
//            Playlist.addToList(playlistName, list, onMessage)
//            if (fileList.any { !Xmp.testFromFd(it, modInfo) }) {
//                if (list.size > 1) {
//                    onMessage(PlaylistMessages.ValidFormatsAdded)
//                } else {
//                    onMessage(PlaylistMessages.UnrecognizedFormat)
//                }
//            }
//        }
//    }
//
//    fun filesToPlaylist(
//        fileList: List<Uri>,
//        playlistName: String,
//        onMessage: (PlaylistMessages) -> Unit
//    ) {
//        onMessage(PlaylistMessages.AddingFiles)
//        Thread {
//            addFiles(fileList, playlistName, onMessage)
//        }.start()
//    }
//
//    fun filesToPlaylist(
//        filename: Uri,
//        playlistName: String,
//        onMessage: (PlaylistMessages) -> Unit
//    ) {
//        val fileList: MutableList<Uri> = ArrayList()
//        fileList.add(filename)
//        addFiles(fileList, playlistName, onMessage)
//    }
//
//    @Deprecated("")
//    fun list(): Array<String> = PrefManager.DATA_DIR.list { _, name ->
//        name.endsWith(Playlist.PLAYLIST_SUFFIX)
//    } ?: arrayOf()
//
//    @Deprecated("")
//    fun listNoSuffix(): Array<String> = list().map {
//        it.substringBeforeLast(Playlist.PLAYLIST_SUFFIX)
//    }.toTypedArray()
//
//    @Deprecated("")
//    fun getPlaylistName(index: Int): String =
//        list()[index].substringBeforeLast(Playlist.PLAYLIST_SUFFIX)
//
//    fun listNoSuffix2(context: Context): List<String> {
//        val playlistsDir = StorageManager.getPlaylistDirectory(context)
//        if (playlistsDir == null || playlistsDir.isFile()) {
//            Timber.e("listNoSuffix2: Playlist Directory returned null or is file!")
//            return emptyList()
//        }
//
//        return playlistsDir.listFiles()
//            .filter { it.name.endsWith(Playlist.PLAYLIST_SUFFIX) }
//            .map { it.name.substringBeforeLast(Playlist.PLAYLIST_SUFFIX) }
//            .toList()
//    }
//
//    @Deprecated("Use createEmptyPlaylist2")
//    fun createEmptyPlaylist(
//        newName: String,
//        newComment: String,
//        onSuccess: () -> Unit,
//        onError: () -> Unit
//    ) {
//        try {
//            Playlist(newName).apply {
//                comment = newComment
//            }.commit()
//            onSuccess()
//        } catch (e: IOException) {
//            onError()
//        }
//    }
//
//    fun createEmptyPlaylist2(
//        playlistsDir: Uri?,
//        newName: String,
//        newComment: String,
//        onSuccess: () -> Unit,
//        onError: (String) -> Unit
//    ) {
//        if (playlistsDir == null) {
//            onError("Creating Playlist failed, uri null")
//            return
//        }
//
//        Playlist(newName).apply {
//            this.comment = newComment
//            this.playlistsDir = playlistsDir
//        }.commit2(
//            onSuccess = onSuccess,
//            onError = onError
//        )
//    }
//
//    // Stable IDs for used by Advanced RecyclerView
//    fun renumberIds(list: List<PlaylistItem>) {
//        list.forEachIndexed { index, item ->
//            item.id = index
//        }
//    }
// }
