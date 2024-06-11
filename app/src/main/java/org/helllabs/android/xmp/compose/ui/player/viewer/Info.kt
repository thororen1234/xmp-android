package org.helllabs.android.xmp.compose.ui.player.viewer

import androidx.compose.runtime.*

@Deprecated("")
@Stable
data class PatternInfo(
    val rowFxParm: ByteArray = ByteArray(64),
    val rowFxType: ByteArray = ByteArray(64),
    val rowInsts: ByteArray = ByteArray(64),
    val rowNotes: ByteArray = ByteArray(64)
)
