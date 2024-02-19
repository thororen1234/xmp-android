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
