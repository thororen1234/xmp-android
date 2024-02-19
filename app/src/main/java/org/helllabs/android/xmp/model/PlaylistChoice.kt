package org.helllabs.android.xmp.model

/**
 * For actions based on playlist selection made using choosePlaylist()
 */
interface PlaylistChoice {
    fun execute(fileSelection: Int, playlistSelection: Int)
}
