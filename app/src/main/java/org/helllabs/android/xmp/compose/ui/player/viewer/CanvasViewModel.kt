package org.helllabs.android.xmp.compose.ui.player.viewer

import android.os.RemoteException
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import org.helllabs.android.xmp.Xmp
import timber.log.Timber

class CanvasViewModel : ViewModel() {
    private var serviceConnected by mutableStateOf(false)
    var currentViewer by mutableIntStateOf(0)

    var insName by mutableStateOf(arrayOf<String>())
    val modVars by mutableStateOf(IntArray(10))
    var isMuted by mutableStateOf(BooleanArray(0))

    val seqVars by mutableStateOf(IntArray(Xmp.maxSeqFromHeader))
    val viewInfo by mutableStateOf(ViewerInfo())
    val patternInfo by mutableStateOf(PatternInfo())

    fun changeViewer() {
        currentViewer = (currentViewer + 1) % 3
    }

    fun setup(connected: Boolean) {
        Timber.d("Setup: $connected")
        serviceConnected = connected

        if (serviceConnected) {
            insName = Xmp.getInstruments() ?: arrayOf()
            Xmp.getModVars(modVars)
            Xmp.getSeqVars(seqVars)

            val chn = modVars[3]
            isMuted = BooleanArray(chn)
            for (i in 0 until chn) {
                try {
                    isMuted[i] = Xmp.mute(i, -1) == 1
                } catch (e: RemoteException) {
                    Timber.w("Can't read channel mute status: ${e.message}")
                }
            }
        }
    }
}
