package org.helllabs.android.xmp.model

import android.net.Uri
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Playlist(
    var name: String = "",
    var comment: String = "",
    var isLoop: Boolean = false,
    var isShuffle: Boolean = false,
    var uri: Uri = Uri.EMPTY,
    var list: List<PlaylistItem> = listOf()
) {
    companion object {
        const val SUFFIX = ".json"
    }
}

@JsonClass(generateAdapter = true)
data class PlaylistItem(
    val name: String,
    val type: String,
    val uri: Uri
) {
    @field:Json(ignore = true)
    var id = 0
}
