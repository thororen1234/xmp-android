package org.helllabs.android.xmp.model

import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.*

/**
 * Data class wrapper for [DropdownMenu]
 *  that provides a text string and it's index
 */
@Stable
data class DropDownItem(val text: String, val selection: DropDownSelection)

enum class DropDownSelection {
    DELETE,
    ADD_TO_PLAYLIST,
    ADD_TO_QUEUE,
    DIR_PLAY_CONTENTS,
    FILE_PLAY_HERE,
    FILE_PLAY_THIS_ONLY
}
