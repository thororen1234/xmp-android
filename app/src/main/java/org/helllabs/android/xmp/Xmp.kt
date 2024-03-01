@file:Suppress("MemberVisibilityCanBePrivate")

package org.helllabs.android.xmp

import android.net.Uri
import org.helllabs.android.xmp.model.ModInfo
import timber.log.Timber

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

    init {
        System.loadLibrary("xmp-jni")
    }

    external fun deInitPlayer()
    external fun endPlayer()
    external fun getMaxSequences(): Int
    external fun getComment(): String?
    external fun getInfo(values: IntArray?)
    external fun getInstruments(): Array<String>?
    external fun getModVars(vars: IntArray?)
    external fun getModuleName(): String?
    external fun getModuleType(): String?
    external fun getSupportedFormats(): Array<String>?
    external fun getTime(): Int
    external fun getVersion(): String
    external fun initPlayer(sampleRate: Int): Boolean
    external fun pause(isPaused: Boolean): Boolean
    external fun releaseModule()
    external fun restartModule()
    external fun setSequence(seq: Int): Boolean
    external fun startModule(): Boolean
    external fun stopModule()
    external fun tick(shouldLoop: Boolean): Int
    external fun mute(channel: Int, status: Int): Int
    external fun seek(value: Int): Int

    private external fun loadModule(fd: Int): Boolean
    private external fun testModule(fd: Int, modInfo: ModInfo): Boolean

    /**
     * Test module from File Descriptor
     */
    fun testFromFd(uri: Uri, modInfo: ModInfo = ModInfo()): Boolean {
        val context = XmpApplication.instance!!.applicationContext
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
        val res = if (pfd != null) {
            val fd = pfd.detachFd()
            pfd.close()

            testModule(fd, modInfo)
        } else {
            false
        }

        // Timber.d("Testing $uri returned $res")
        return res
    }

    /**
     * Load module from File Descriptor
     */
    fun loadFromFd(uri: Uri): Boolean {
        if (!testFromFd(uri)) {
            Timber.d("Load Module: $uri, Result failed")
            return false
        }

        val context = XmpApplication.instance!!.applicationContext
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
        val res = if (pfd != null) {
            val fd = pfd.detachFd()
            pfd.close()

            loadModule(fd)
        } else {
            false
        }

        Timber.d("Load Module: Result $res")
        return res
    }
}
