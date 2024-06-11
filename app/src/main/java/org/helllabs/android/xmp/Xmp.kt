@file:Suppress("MemberVisibilityCanBePrivate")

package org.helllabs.android.xmp

import android.net.Uri
import org.helllabs.android.xmp.model.ModInfo
import org.helllabs.android.xmp.model.ModVars
import org.helllabs.android.xmp.model.SequenceVars
import timber.log.Timber

object Xmp {

    const val MIN_BUFFER_MS = 80

    const val MAX_BUFFER_MS = 1000

    const val DUCK_VOLUME = 0x500

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

    const val MAX_BUFFERS = 256

    // MAX_SEQUENCES from common.h
    val maxSeqFromHeader: Int
        get() = getMaxSequences()

    init {
        System.loadLibrary("xmp-jni")
    }

    // external fun loadModule(name: String?): Int

    // external fun testModule(name: String?, info: ModInfo?): Boolean

    external fun loadModuleFd(fd: Int): Int

    external fun deinit(): Int

    external fun dropAudio()

    external fun endPlayer(): Int

    external fun fillBuffer(loop: Boolean): Int

    external fun getInfo(values: IntArray?) // TODO data class

    external fun getPlayer(parm: Int): Int

    external fun hasFreeBuffer(): Boolean

    external fun init(rate: Int, ms: Int): Boolean

    external fun mute(chn: Int, status: Int): Int

    external fun playAudio(): Int

    external fun releaseModule(): Int

    external fun restartAudio(): Boolean

    external fun seek(time: Int): Int

    external fun setPlayer(parm: Int, `val`: Int)

    external fun startPlayer(rate: Int): Int

    external fun stopAudio(): Boolean

    external fun stopModule(): Int

    private external fun testModuleFd(fd: Int, info: ModInfo?): Boolean

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

    external fun getModVars(vars: ModVars)

    external fun getPatternRow(
        pat: Int,
        row: Int,
        rowNotes: ByteArray,
        rowInstruments: ByteArray,
        rowFxType: ByteArray,
        rowFxParm: ByteArray
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

    external fun getSeqVars(vars: SequenceVars)

    external fun getVersion(): String

    external fun getVolume(): Int

    external fun nextPosition(): Int

    external fun prevPosition(): Int

    external fun restartModule(): Int

    external fun setPosition(num: Int): Int

    external fun setSequence(seq: Int): Boolean

    external fun setVolume(vol: Int): Int

    /**
     * Helper to get formats
     */
    val formats: List<String>
        get() = getFormats().orEmpty().toList()

    /**
     * Test module from File Descriptor
     */
    fun testFromFd(uri: Uri, modInfo: ModInfo = ModInfo()): Boolean {
        val context = XmpApplication.instance!!.applicationContext
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
        val res = if (pfd != null) {
            val fd = pfd.detachFd()
            pfd.close()

            testModuleFd(fd, modInfo)
        } else {
            false
        }

        // Timber.d("Testing $uri returned $res")
        return res
    }

    /**
     * Load module from File Descriptor
     */
    fun loadFromFd(uri: Uri): Int {
        if (!testFromFd(uri)) {
            Timber.d("Load Module: $uri, Result failed")
            return -1
        }

        val context = XmpApplication.instance!!.applicationContext
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
        val res = if (pfd != null) {
            val fd = pfd.detachFd()
            pfd.close()

            loadModuleFd(fd)
        } else {
            -1
        }

        Timber.d("Load Module: Result $res")
        return res
    }
}
