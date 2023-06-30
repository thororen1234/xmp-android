package org.helllabs.android.xmp.browser.playlist

import java.io.File

class PlaylistItem(
    val type: Int,
    val name: String,
    val comment: String
) : Comparable<PlaylistItem> {

    // Accessors
    var id = 0
    var file: File? = null

    val filename: String
        get() = file!!.name

    val isDirectory: Boolean
        get() = type == TYPE_DIRECTORY

    val isFile: Boolean
        get() = type == TYPE_FILE

    val isPlaylist: Boolean
        get() = type == TYPE_PLAYLIST

    val isSpecial: Boolean
        get() = type == TYPE_SPECIAL

    override fun toString(): String =
        String.format("%s:%s:%s\n", file?.path ?: "", comment, name)

    // Comparable
    override fun compareTo(other: PlaylistItem): Int {
        val d1 = file!!.isDirectory
        val d2 = other.file!!.isDirectory
        return if (d1 xor d2) {
            if (d1) -1 else 1
        } else {
            name.compareTo(other.name, true)
        }
    }

    companion object {
        const val TYPE_DIRECTORY = 1
        const val TYPE_FILE = 2
        const val TYPE_PLAYLIST = 3
        const val TYPE_SPECIAL = 4
    }
}
