package org.helllabs.android.xmp.core

import android.net.Uri
import com.lazygeniouz.dfc.file.DocumentFileCompat
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.model.Playlist
import org.helllabs.android.xmp.model.PlaylistItem
import timber.log.Timber

class PlaylistManager {

    private lateinit var moshi: Moshi

    private lateinit var adapter: JsonAdapter<Playlist>

    lateinit var playlist: Playlist
        internal set

    private var oldName: String? = null

    fun new(name: String, comment: String): Boolean {
        playlist = Playlist(
            name = name,
            comment = comment
        )

        return save()
    }

    fun load(uri: Uri): Boolean {
        moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .add(UriAdapter())
            .build()

        adapter = moshi.adapter(Playlist::class.java)

        val context = XmpApplication.instance!!.applicationContext
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            playlist = adapter.fromJson(jsonString) ?: return false
        }

        return true
    }

    fun save(): Boolean {
        if (!oldName.isNullOrEmpty()) {
            val file = StorageManager.getPlaylistDirectory()?.findFile(oldName + Playlist.SUFFIX)
            if (file == null || !file.delete()) {
                Timber.e("Failed to delete old file")
            }
        } else {
            val file =
                StorageManager.getPlaylistDirectory()?.findFile(playlist.name + Playlist.SUFFIX)
            file!!.delete()
        }

        // New playlist
        val file = StorageManager.getPlaylistDirectory()
        if (file == null) {
            Timber.e("Saving playlist failed, unable to get playlist directory")
            return false
        }

        val mimeType = "application/octet-stream"
        val newFile = file.createFile(mimeType, playlist.name + Playlist.SUFFIX)

        playlist.uri = newFile!!.uri

        val jsonString = adapter.toJson(playlist)
        val context = XmpApplication.instance!!.applicationContext
        context.contentResolver.openOutputStream(playlist.uri!!)?.use { outputStream ->
            outputStream.writer().use { it.write(jsonString) }
        }

        return true
    }

    fun rename(newName: String, newComment: String): Boolean {
        if (newName != playlist.name) {
            oldName = playlist.name
        }

        playlist.comment = newComment
        playlist.name = newName

        return save()
    }

    fun setLoop(value: Boolean): Boolean {
        playlist.isLoop = value
        return save()
    }

    fun setShuffle(value: Boolean): Boolean {
        playlist.isShuffle = value
        return save()
    }

    fun setList(list: List<PlaylistItem>): Boolean {
        playlist.list = list
        return save()
    }

    companion object {
        fun listPlaylistsDF(): List<DocumentFileCompat> {
            val playlistsDir = StorageManager.getPlaylistDirectory()
            if (playlistsDir == null || playlistsDir.isFile()) {
                Timber.e("Unable to query playlist dir")
                return emptyList()
            }

            return playlistsDir.listFiles()
        }

        fun listPlaylists(): List<Playlist> {
            val playlistsDir = StorageManager.getPlaylistDirectory()
            if (playlistsDir == null || playlistsDir.isFile()) {
                Timber.e("Unable to query playlist dir")
                return emptyList()
            }

            return playlistsDir.listFiles().map {
                PlaylistManager().run {
                    load(it.uri)
                    playlist
                }
            }
        }

        fun delete(name: String): Boolean {
            val file = StorageManager.getPlaylistDirectory()
            if (file == null) {
                Timber.e("Deleting playlist failed, unable to get playlist directory")
                return false
            }

            val playlist = file.findFile(name + Playlist.SUFFIX)
            if (playlist == null) {
                Timber.e("Deleting playlist failed, unable to find $name")
                return false
            }

            return playlist.delete()
        }
    }
}
