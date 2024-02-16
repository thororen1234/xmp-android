package org.helllabs.android.xmp.model

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.StorageManager
import org.helllabs.android.xmp.core.InfoCache.fileExists
import org.helllabs.android.xmp.core.PlaylistMessages
import org.helllabs.android.xmp.core.PlaylistUtils
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader

class Playlist(val name: String) {

    private var mCommentChanged = false
    private var mListChanged = false

    var playlistsDir: Uri? = null
    var comment: String = ""
    var list = mutableListOf<PlaylistItem>()

    var isLoopMode = false
    var isShuffleMode = false

    private class ListFile : File {
        constructor(name: String) : super(PrefManager.DATA_DIR, name + PLAYLIST_SUFFIX)
        constructor(name: String, suffix: String) : super(
            PrefManager.DATA_DIR,
            name + PLAYLIST_SUFFIX + suffix
        )
    }

    private class CommentFile : File {
        constructor(name: String) : super(PrefManager.DATA_DIR, name + COMMENT_SUFFIX)
        constructor(name: String, suffix: String) : super(
            PrefManager.DATA_DIR,
            name + COMMENT_SUFFIX + suffix
        )
    }

    init {
        val file = ListFile(name)
        if (file.exists()) {
            Timber.i("Read playlist $name")

            // TODO
            val comment = "" //  Files.readFromFile(CommentFile(name))

            // read list contents
            if (readList(name)) {
                this.comment = comment
                isShuffleMode = readShuffleModePref(name)
                isLoopMode = readLoopModePref(name)
            }
        } else {
            Timber.i("New playlist $name")
            isShuffleMode = DEFAULT_SHUFFLE_MODE
            isLoopMode = DEFAULT_LOOP_MODE
            mListChanged = true
            mCommentChanged = true
        }
    }

    /**
     * Save the current playlist.
     */
    @Deprecated("Use commit2")
    fun commit() {
        Timber.i("Commit playlist $name")

        if (mListChanged) {
            writeList(name)
            mListChanged = false
        }

        if (mCommentChanged) {
            writeComment(name)
            mCommentChanged = false
        }

        var saveModes = false
        if (isShuffleMode != readShuffleModePref(name) ||
            isLoopMode != readLoopModePref(name)
        ) {
            saveModes = true
        }

        if (saveModes) {
            PrefManager.putBoolean(optionName(name, SHUFFLE_MODE), isShuffleMode)
            PrefManager.putBoolean(optionName(name, LOOP_MODE), isLoopMode)
        }
    }

    /**
     * Save the current playlist using SAF
     */
    fun commit2(
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        if (playlistsDir == null) {
            onError("playlistsDir URI is null")
            return
        }

        val dir = DocumentFile.fromTreeUri(context, playlistsDir!!)
        if (dir != null && dir.isDirectory && dir.canWrite()) {
            if (mListChanged) {
                val plist = dir.createFile("application/octet-stream", "$name.playlist")
                if (plist != null && list.isNotEmpty()) {
                    try {
                        context.contentResolver.openOutputStream(plist.uri)?.use { out ->
                            list.forEach { item ->
                                out.write(item.toString().toByteArray())
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error writing empty playlist")
                        onError("Failed to write empty playlist")
                        return
                    }
                }

                mListChanged = false
            }

            if (mCommentChanged) {
                val pComment = dir.createFile("application/octet-stream", "$name.comment")
                if (pComment != null) {
                    try {
                        context.contentResolver.openOutputStream(pComment.uri)?.use { out ->
                            out.write(comment.toByteArray())
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error writing empty playlist comment")
                        onError("Failed to write empty playlist comment")
                        return
                    }
                }

                mCommentChanged = false
            }

            onSuccess()
            return
        }
    }

    /**
     * Remove an item from the playlist.
     *
     * @param index The index of the item to be removed
     */
    fun remove(index: Int) {
        Timber.i("Remove item #$index: ${list[index].name}")
        list.removeAt(index)
        mListChanged = true
    }

    // Helper methods
    @Deprecated("")
    private fun readList(name: String): Boolean {
        Timber.d("Reading playlist: $name")
        list.clear()
        val file: File = ListFile(name)
        var lineNum: Int
        val invalidList: MutableList<Int> = ArrayList()
        try {
            lineNum = 0
            file.bufferedReader().forEachLine { line ->
                val fields = line.split(":".toRegex(), limit = 3).toTypedArray()
                val filename = fields[0]
                val comment = if (fields.size > 1) fields[1] else ""
                val title = if (fields.size > 2) fields[2] else ""
                if (fileExists(filename)) {
                    val item = PlaylistItem(PlaylistItem.TYPE_FILE, title, comment)
                    item.file = File(filename)
                    // item.imageRes = R.drawable.grabber
                    list.add(item)
                } else {
                    invalidList.add(lineNum)
                }
                lineNum++
            }
            PlaylistUtils.renumberIds(list)
        } catch (e: IOException) {
            Timber.e("Error reading playlist ${file.path}")
            return false
        }
        if (invalidList.isNotEmpty()) {
            val array = IntArray(invalidList.size)
            val iterator: Iterator<Int> = invalidList.iterator()
            for (i in array.indices) {
                array[i] = iterator.next()
            }
            try {
                // Files.removeLineFromFile(file, array) // TODO
            } catch (e: FileNotFoundException) {
                Timber.e("Playlist file ${file.path} not found")
            } catch (e: IOException) {
                Timber.e("I/O error removing invalid lines from ${file.path}")
            }
        }
        return true
    }

    @Deprecated("")
    private fun writeList(name: String) {
        Timber.i("Write list")

        val file = ListFile(name, ".new").apply { delete() }
        try {
            file.bufferedWriter().use { out ->
                list.forEach { item ->
                    out.write(item.toString())
                }
            }

            val oldFile = ListFile(name).apply { delete() }
            file.renameTo(oldFile)
        } catch (e: IOException) {
            Timber.e("Error writing playlist file ${file.path}")
        }
    }

    @Deprecated("")
    private fun writeComment(name: String) {
        Timber.i("Write comment")

        val file = CommentFile(name, ".new").apply { delete() }
        try {
            file.writeText(comment)

            val oldFile = CommentFile(name).apply { delete() }
            file.renameTo(oldFile)
        } catch (e: IOException) {
            Timber.e("Error writing comment file ${file.path}")
        }
    }

    private fun readShuffleModePref(name: String): Boolean =
        PrefManager.getBoolean(optionName(name, SHUFFLE_MODE), DEFAULT_SHUFFLE_MODE)

    private fun readLoopModePref(name: String): Boolean =
        PrefManager.getBoolean(optionName(name, LOOP_MODE), DEFAULT_LOOP_MODE)

    fun setListChanged(listChanged: Boolean) {
        mListChanged = listChanged
    }

    fun setNewList(newList: List<PlaylistItem>) {
        list.clear()
        list.addAll(newList)
    }

    companion object {
        const val COMMENT_SUFFIX = ".comment"
        const val PLAYLIST_SUFFIX = ".playlist"
        private const val OPTIONS_PREFIX = "options_"
        private const val SHUFFLE_MODE = "_shuffleMode"
        private const val LOOP_MODE = "_loopMode"
        private const val DEFAULT_SHUFFLE_MODE = true
        private const val DEFAULT_LOOP_MODE = false
        // Static utilities
        /**
         * Rename a playlist.
         *
         * @param oldName The current name of the playlist
         * @param newName The new name of the playlist
         *
         * @return Whether the rename was successful
         */
        // TODO Replace with SAF alternative
        @Deprecated("Replace with SAF alternative")
        fun rename(oldName: String, newName: String): Boolean {
            val old1 = ListFile(oldName)
            val old2 = CommentFile(oldName)
            val new1 = ListFile(newName)
            val new2 = CommentFile(newName)

            val error = when {
                !old1.renameTo(new1) -> false
                !old2.renameTo(new2) -> {
                    new1.renameTo(old1)
                    false
                }

                else -> true
            }

            if (error) {
                return false
            }

            with(PrefManager) {
                putBoolean(
                    key = optionName(newName, SHUFFLE_MODE),
                    value = getBoolean(optionName(oldName, SHUFFLE_MODE), DEFAULT_SHUFFLE_MODE)
                )
                putBoolean(
                    key = optionName(newName, LOOP_MODE),
                    value = getBoolean(optionName(oldName, LOOP_MODE), DEFAULT_LOOP_MODE)
                )
                remove(optionName(oldName, SHUFFLE_MODE))
                remove(optionName(oldName, LOOP_MODE))
            }

            return true
        }

        /**
         * Delete the specified playlist.
         *
         * @param name The playlist name
         */
        @Deprecated("")
        fun delete(name: String) {
            ListFile(name).delete()
            CommentFile(name).delete()
            PrefManager.remove(optionName(name, SHUFFLE_MODE))
            PrefManager.remove(optionName(name, LOOP_MODE))
        }

        /**
         * Add a list of items to the specified playlist file.
         *
         * @param name The playlist name
         * @param items The list of playlist items to add
         * @param onMessage TODO
         */
        @Deprecated("")
        fun addToList(
            name: String,
            items: List<PlaylistItem>,
            onMessage: (PlaylistMessages) -> Unit
        ) {
            val lines = items.map { it.toString() }

            try {
                val file = File(PrefManager.DATA_DIR, "$name$PLAYLIST_SUFFIX")
                file.writeText(lines.joinToString(separator = "\n"))
            } catch (e: IOException) {
                onMessage(PlaylistMessages.CantWriteToPlaylist)
            }
        }

        /**
         * Read comment from a playlist file.
         *
         * @param context The activity we're running
         * @param name The playlist name
         *
         * @return The playlist comment
         */
        @Deprecated("")
        fun readComment(context: Context, name: String, onError: () -> Unit): String {
            var comment: String? = null
            try {
                comment = "" // Files.readFromFile(CommentFile(name)) // TODO
            } catch (e: IOException) {
                onError()
            }
            if (comment == null || comment.trim().isEmpty()) {
                comment = context.getString(R.string.no_comment)
            }
            return comment
        }

        fun readComment2(
            context: Context,
            comment: String,
            onSuccess: (String) -> Unit,
            onError: (String) -> Unit
        ) {
            val playlistsDir = StorageManager.getPlaylistDirectory(context)
            if (playlistsDir == null || playlistsDir.isFile) {
                onError("Comment file playlist Directory returned null or is file!")
                return
            }

            val commentFile = playlistsDir.findFile(comment + COMMENT_SUFFIX)
            if (commentFile == null || commentFile.isDirectory) {
                onError("Comment file returned null or is directory")
                return
            }
            context.contentResolver.openInputStream(commentFile.uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val text = reader.readText()
                    onSuccess(text)
                }
            }
        }

        private fun optionName(name: String, option: String): String =
            OPTIONS_PREFIX + name + option
    }
}
