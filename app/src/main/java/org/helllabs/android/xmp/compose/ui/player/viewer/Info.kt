package org.helllabs.android.xmp.compose.ui.player.viewer

import androidx.compose.runtime.*
import org.helllabs.android.xmp.model.FrameInfo

@Immutable // I think
data class ViewerInfo(
    val finalVols: IntArray = IntArray(64),
    val instruments: IntArray = IntArray(64),
    val keys: IntArray = IntArray(64),
    val pans: IntArray = IntArray(64),
    val periods: IntArray = IntArray(64),
    val time: Int = 0,
    val type: String = "",
    val frameInfo: FrameInfo = FrameInfo(),
    val volumes: IntArray = IntArray(64)
) {
    override fun toString(): String {
        return "ViewerInfo(" +
            "finalVols=${finalVols.contentToString()}, " +
            "instruments=${instruments.contentToString()}, " +
            "keys=${keys.contentToString()}, " +
            "pans=${pans.contentToString()}, " +
            "periods=${periods.contentToString()}, " +
            "time=$time, " +
            "type='$type', " +
            "frameInfo=$frameInfo" +
            "volumes=${volumes.contentToString()}" +
            ")"
    }
}

@Stable
data class PatternInfo(
    var lineInPattern: Int = 0,
    var pat: Int = 0,
    val rowFxParm: ByteArray = ByteArray(64),
    val rowFxType: ByteArray = ByteArray(64),
    val rowInsts: ByteArray = ByteArray(64),
    val rowNotes: ByteArray = ByteArray(64)
) {
    override fun toString(): String {
        return "PatternInfo(" +
            "lineInPattern=$lineInPattern, " +
            "pat=$pat, " +
            "rowFxParm=${rowFxParm.contentToString()}, " +
            "rowFxType=${rowFxType.contentToString()}, " +
            "rowInsts=${rowInsts.contentToString()}, " +
            "rowNotes=${rowNotes.contentToString()}" +
            ")"
    }
}
