package org.helllabs.android.xmp

import android.content.Context
import android.net.Uri
import org.helllabs.android.xmp.model.ModInfo

object Xmp {
    // Return codes
    const val XMP_END = 1 // End of module reached

    // Sample format flags
    const val FORMAT_MONO = 1 shl 2

    // player parameters
    const val PLAYER_AMP = 0 // Amplification factor
    const val PLAYER_MIX = 1 // Stereo mixing
    const val PLAYER_INTERP = 2 // Interpolation type
    const val PLAYER_DSP = 3 // DSP effect flags
    const val PLAYER_CFLAGS = 5 // Current module flags
    const val PLAYER_VOLUME = 7 // Player volume (for audio focus duck)
    const val PLAYER_DEFPAN = 10 // Default pan separation

    // Interpolation types
    const val INTERP_NEAREST = 0 // Nearest neighbor
    const val INTERP_LINEAR = 1 // Linear (default)
    const val INTERP_SPLINE = 2 // Cubic spline

    // Player flags
    const val FLAGS_A500 = 1 shl 3

    // DSP effect types
    const val DSP_LOWPASS = 1 shl 0 // Lowpass filter effect

    // Limits
    const val MAX_CHANNELS = 64 // Max number of channels in module

    // MAX_SEQUENCES from common.h
    val maxSeqFromHeader: Int
        get() = getMaxSequences()

    external fun deinit(): Int
    external fun dropAudio()
    external fun endPlayer(): Int
    external fun fillBuffer(loop: Boolean): Int
    external fun getInfo(values: IntArray?)
    external fun getPlayer(parm: Int): Int
    external fun hasFreeBuffer(): Boolean
    external fun init(rate: Int, ms: Int): Boolean
    external fun loadModule(name: String?): Int
    external fun loadModuleFd(fd: Int): Int
    external fun mute(chn: Int, status: Int): Int
    external fun playAudio(): Int
    external fun releaseModule(): Int
    external fun restartAudio(): Boolean
    external fun seek(time: Int): Int
    external fun setPlayer(parm: Int, `val`: Int)
    external fun startPlayer(rate: Int): Int
    external fun stopAudio(): Boolean
    external fun stopModule(): Int
    external fun testModule(name: String?, info: ModInfo?): Boolean
    external fun time(): Int

    external fun getChannelData(
        volumes: IntArray?,
        finalvols: IntArray?,
        pans: IntArray?,
        instruments: IntArray?,
        keys: IntArray?,
        periods: IntArray?
    )

    external fun getComment(): String?
    external fun getFormats(): Array<String>?
    external fun getInstruments(): Array<String>?
    external fun getLoopCount(): Int
    private external fun getMaxSequences(): Int
    external fun getModName(): String
    external fun getModType(): String
    external fun getModVars(vars: IntArray?)

    external fun getPatternRow(
        pat: Int,
        row: Int,
        rowNotes: ByteArray,
        rowInstruments: ByteArray,
        rowFxType: IntArray,
        rowFxParm: IntArray
    )

    external fun getSampleData(
        trigger: Boolean,
        ins: Int,
        key: Int,
        period: Int,
        chn: Int,
        width: Int,
        buffer: ByteArray?
    )

    external fun getSeqVars(vars: IntArray?)
    external fun getVersion(): String
    external fun getVolume(): Int
    external fun nextPosition(): Int
    external fun prevPosition(): Int
    external fun restartModule(): Int
    external fun setPosition(num: Int): Int
    external fun setSequence(seq: Int): Boolean
    external fun setVolume(vol: Int): Int

    // TODO add testModule helper

    /**
     * Load module from File Descriptor
     */
    fun loadFromFd(context: Context, uri: Uri): Int {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
        val res = if (pfd != null) {
            val fd = pfd.detachFd()
            pfd.close()

            loadModuleFd(fd)
        } else -1

        return res
    }

    init {
        System.loadLibrary("xmp-jni")
    }
}
