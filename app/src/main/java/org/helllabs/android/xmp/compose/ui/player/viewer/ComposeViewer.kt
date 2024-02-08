package org.helllabs.android.xmp.compose.ui.player.viewer

import android.os.RemoteException
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    var buffer: Array<ByteArray> = arrayOf(),
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
fun ComposeCanvas(
    modifier: Modifier = Modifier,
    viewModel: CanvasViewModel,
    isPlaying: Boolean
) {
    XmpCanvas(
        modifier = modifier,
        isPlaying = isPlaying,
        onChangeViewer = viewModel::changeViewer,
        currentViewer = viewModel.currentViewer,
        viewInfo = viewModel.viewInfo,
        isMuted = viewModel.isMuted,
        modVars = viewModel.modVars,
        insName = viewModel.insName
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun XmpCanvas(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    onChangeViewer: () -> Unit,
    currentViewer: Int,
    viewInfo: Viewer.Info,
    isMuted: BooleanArray,
    modVars: IntArray,
    insName: Array<String>,
) {
    Box(modifier = modifier.combinedClickable(
        onClick = onChangeViewer,
        onLongClick = {
        }
    )
    ) {
        Surface {
            when (currentViewer) {
                0 -> InstrumentViewer(isPlaying, viewInfo, isMuted, modVars, insName)
                1 -> ComposePatternViewer(viewInfo, isMuted, modVars)
                2 -> ComposeChannelViewer(viewInfo, isMuted, modVars, insName)
            }
        }
    }
}



