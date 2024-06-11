package org.helllabs.android.xmp.model

import androidx.compose.runtime.*

/**
 * @see [org.helllabs.android.xmp.Xmp.testFromFd]
 * @see [org.helllabs.android.xmp.Xmp.testModuleFd]
 */
@Stable
data class ModInfo(
    val name: String = "",
    val type: String = ""
)

/**
 * @see [org.helllabs.android.xmp.Xmp.getModVars]
 */
data class ModVars(
    val seqDuration: Int = 0,
    val lengthInPatterns: Int = 0,
    val numPatterns: Int = 0,
    val numChannels: Int = 0,
    val numInstruments: Int = 0,
    val numSamples: Int = 0,
    val numSequence: Int = 0,
    val currentSequence: Int = 0
)

/**
 * @see [org.helllabs.android.xmp.Xmp.getInfo]
 */
@Stable
data class FrameInfo(
    val pos: Int = 0,
    val pattern: Int = 0,
    val row: Int = 0,
    val numRows: Int = 0,
    val frame: Int = 0,
    val speed: Int = 0,
    val bpm: Int = 0
)

/**
 * @see [org.helllabs.android.xmp.Xmp.getSeqVars]
 */
@Stable
data class SequenceVars(val sequence: IntArray = intArrayOf()) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SequenceVars

        return sequence.contentEquals(other.sequence)
    }

    override fun hashCode(): Int {
        return sequence.contentHashCode()
    }
}
