package org.helllabs.android.xmp.model

// class PlaylistItem(
//    val type: Int,
//    val name: String,
//    val comment: String
// ) : Comparable<PlaylistItem> {
//
//    // Accessors
//    var id = 0
//    var docFile: DocumentFileCompat? = null
//
//    val uri: Uri
//        get() = docFile!!.uri
//
//    val docName: String
//        get() = docFile!!.name
//
//    val isDocDirectory: Boolean
//        get() = type == TYPE_DIRECTORY || docFile?.isDirectory() ?: false
//
//    val isDocFile: Boolean
//        get() = type == TYPE_FILE || docFile?.isFile() ?: false
//
//    val isPlaylist: Boolean
//        get() = type == TYPE_PLAYLIST
//
//    val isSpecial: Boolean
//        get() = type == TYPE_SPECIAL
//
//    override fun toString(): String =
//        String.format("%s:%s:%s\n", docFile!!.uri, comment, name)
//
//    // Comparable
//    override fun compareTo(other: PlaylistItem): Int {
//        val d1 = docFile!!.isDirectory()
//        val d2 = other.docFile!!.isDirectory()
//        return if (d1 xor d2) {
//            if (d1) -1 else 1
//        } else {
//            name.compareTo(other.name, true)
//        }
//    }
//
//    companion object {
//        const val TYPE_DIRECTORY = 1
//        const val TYPE_FILE = 2
//        const val TYPE_PLAYLIST = 3
//        const val TYPE_SPECIAL = 4
//    }
// }
