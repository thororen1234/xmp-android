package org.helllabs.android.xmp.model

/**
 * @see [org.helllabs.android.xmp.Xmp.getModVars]
 */
// seq_duration, length, pat, chn, ins, smp, num_sequences, seq
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
