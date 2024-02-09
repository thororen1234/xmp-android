package org.helllabs.android.xmp.compose.ui.player.viewer

import android.os.RemoteException
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import org.helllabs.android.xmp.Xmp
import timber.log.Timber

// TODO: This is a WIP

@Suppress("ArrayInDataClass")
data class SampleData(
    var key: Int = 0,
    var ins: Int = 0,
    var holdKey: IntArray = intArrayOf(),
    var period: Int = 0,
    var scopeWidth: Int = 0,
    var buffer: Array<ByteArray> = arrayOf()
)

class CanvasViewModel : ViewModel() {
    private val seqVars = IntArray(Xmp.maxSeqFromHeader)
    private var serviceConnected by mutableStateOf(false)

    var currentViewer by mutableIntStateOf(0)
    var insName by mutableStateOf(arrayOf<String>())
    val modVars by mutableStateOf(IntArray(10))
    var isMuted by mutableStateOf(BooleanArray(0))

    var viewInfo by mutableStateOf(Viewer.Info())
    var sampleData by mutableStateOf(SampleData())

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

    fun update(info: Viewer.Info) {
        viewInfo = info
    }
}

@Composable
internal fun XmpCanvas(
    modifier: Modifier = Modifier,
    onChangeViewer: () -> Unit,
    currentViewer: Int,
    viewInfo: Viewer.Info,
    isMuted: BooleanArray,
    modVars: IntArray,
    insName: Array<String>
) {
    // TODO we can possibly get the 'ViewPort' size here to aid in the bottom culling

    Box(
        modifier = modifier
            .clickable { onChangeViewer() }
    ) {
        when (currentViewer) {
            0 -> InstrumentViewer(viewInfo, isMuted, modVars, insName)
            1 -> ComposePatternViewer(viewInfo, isMuted, modVars)
            2 -> ComposeChannelViewer(viewInfo, isMuted, modVars, insName)
        }
    }
}
