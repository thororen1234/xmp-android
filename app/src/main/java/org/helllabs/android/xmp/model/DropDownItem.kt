package org.helllabs.android.xmp.model

import androidx.compose.material3.DropdownMenu

/**
 * Data class wrapper for [DropdownMenu]
 *  that provides a text string and it's index
 */
data class DropDownItem(
    val text: String,
    val selection: DropDownSelection
)

enum class DropDownSelection {
    DELETE,
    DIR_ADD_TO_PLAYLIST,
    DIR_ADD_TO_QUEUE,
    DIR_PLAY_CONTENTS,
    FILE_ADD_TO_PLAYLIST,
    FILE_ADD_TO_QUEUE,
    FILE_PLAY_HERE,
    FILE_PLAY_THIS_ONLY
}
