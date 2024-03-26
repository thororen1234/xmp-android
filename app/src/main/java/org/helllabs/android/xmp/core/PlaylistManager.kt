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

    private var oldName: String? = null

    fun init() {
        moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .add(UriAdapter())
            .build()

        adapter = moshi.adapter(Playlist::class.java)
    }

    fun new(name: String, comment: String): Boolean {
        init()

        playlist = Playlist(
            name = name.trim(),
            comment = comment.trim()
        )

        return save()
    }

    fun load(uri: Uri): Boolean {
        if (!uri.pathSegments.last().contains(".json")) {
            Timber.w("Uri: $uri is not a .json file")
            return false
        }

        init()

        val context = XmpApplication.instance!!.applicationContext
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            playlist = adapter.fromJson(jsonString) ?: return false
        }

        return true
    }

    fun save(): Boolean {
        val playlistDir = StorageManager.getPlaylistDirectory()

        if (playlistDir == null) {
            Timber.e("Saving playlist failed, unable to get playlist directory")
            return false
        }

        if (!oldName.isNullOrEmpty()) {
            playlistDir.findFile(oldName + Playlist.SUFFIX)

            if (!playlistDir.delete()) {
                Timber.e("Failed to delete old file")
            }
        } else {
            playlistDir
                .findFile(playlist.name + Playlist.SUFFIX)
                ?.delete()
        }

        val mimeType = "application/octet-stream"
        val newFile = playlistDir.createFile(mimeType, playlist.name + Playlist.SUFFIX)

        playlist.uri = newFile!!.uri

        val jsonString = adapter.toJson(playlist)
        val context = XmpApplication.instance!!.applicationContext
        context.contentResolver.openOutputStream(playlist.uri)?.use { outputStream ->
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

    fun add(list: List<PlaylistItem>): Boolean {
        val newList = playlist.list.toMutableList()
        var res = newList.addAll(list)

        playlist.list = newList

        if (res) {
            res = save()
        }

        return res
    }

    fun setLoop(value: Boolean) {
        playlist.isLoop = value
    }

    fun setShuffle(value: Boolean) {
        playlist.isShuffle = value
    }

    fun setList(list: List<PlaylistItem>) {
        playlist.list = list
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

            val list = mutableListOf<Playlist>()
            for (dfc in playlistsDir.listFiles()) {
                if (dfc.extension != "json") continue

                PlaylistManager().run {
                    load(dfc.uri)
                    playlist
                }.also(list::add)
            }

            return list
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
