package org.helllabs.android.xmp.compose.ui.player.viewer

@Suppress("ArrayInDataClass")
data class ViewerInfo(
    val finalVols: IntArray = IntArray(64),
    val instruments: IntArray = IntArray(64),
    val keys: IntArray = IntArray(64),
    val pans: IntArray = IntArray(64),
    val periods: IntArray = IntArray(64),
    var time: Int = 0,
    var type: String = "",
    val values: IntArray = IntArray(7), // order pattern row num_rows frame speed bpm
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
            "values=${values.contentToString()}, " +
            "volumes=${volumes.contentToString()}" +
            ")"
    }
}

@Suppress("ArrayInDataClass")
data class PatternInfo(
    var lineInPattern: Int = 0,
    var pat: Int = 0,
    val rowFxParm: IntArray = IntArray(64),
    val rowFxType: IntArray = IntArray(64),
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
