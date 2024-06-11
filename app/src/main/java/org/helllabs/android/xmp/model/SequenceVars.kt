package org.helllabs.android.xmp.model

import androidx.compose.runtime.*

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
