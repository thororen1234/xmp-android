package org.helllabs.android.xmp.model

/**
 * @see [org.helllabs.android.xmp.Xmp.getInfo]
 */
// order pattern row num_rows frame speed bpm
data class FrameInfo(
    val pos: Int = 0,
    val pattern: Int = 0,
    val row: Int = 0,
    val numRows: Int = 0,
    val frame: Int = 0,
    val speed: Int = 0,
    val bpm: Int = 0
)
