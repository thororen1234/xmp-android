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
import androidx.compose.runtime.remember
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.compose.theme.XmpTheme
import timber.log.Timber

// TODO: This is a WIP

class CanvasViewModel : ViewModel() {
    private val seqVars = IntArray(Xmp.maxSeqFromHeader)
    private var serviceConnected by mutableStateOf(false)

    var width by mutableStateOf(0.dp)
    var height by mutableStateOf(0.dp)
    var currentViewer by mutableIntStateOf(0)
    var insName by mutableStateOf(arrayOf<String>())
    val modVars by mutableStateOf(IntArray(10))
    var isMuted by mutableStateOf(BooleanArray(0))

    var viewInfo by mutableStateOf(Viewer.Info())

    fun changeViewer() {
        currentViewer = (currentViewer + 1) % 3

    }

    fun setup(connected: Boolean) {
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

    fun onSizeChanged(w: Dp, h: Dp) {
        width = w
        height = h
    }
}

@Composable
fun ComposeCanvas(
    modifier: Modifier = Modifier,
    viewModel: CanvasViewModel
) {
    XmpCanvas(
        modifier = modifier,
        onSizeChanged = viewModel::onSizeChanged,
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
    onSizeChanged: (width: Dp, height: Dp) -> Unit,
    onChangeViewer: () -> Unit,
    currentViewer: Int,
    viewInfo: Viewer.Info,
    isMuted: BooleanArray,
    modVars: IntArray,
    insName: Array<String>,
) {
    val density = LocalDensity.current

    Box(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    do {
                        //This PointerEvent contains details including
                        // event, id, position and more
                        val event: PointerEvent = awaitPointerEvent()
                        // ACTION_MOVE loop

                        // Consuming event prevents other gestures or scroll to intercept
                        event.changes.forEach { pointerInputChange: PointerInputChange ->
                            if (pointerInputChange.positionChange() != Offset.Zero)
                                pointerInputChange.consume()
                        }
                    } while (event.changes.any { it.pressed })

                    // ACTION_UP is here
                }
            }
            .combinedClickable(
                onClick = onChangeViewer,
                onLongClick = {

                }
            )
            .onSizeChanged {
                with(density) {
                    onSizeChanged(it.width.toDp(), it.height.toDp())
                }
            }
        ) {
            Surface {
                when (currentViewer) {
                    0 -> InstrumentViewer(viewInfo, isMuted, modVars, insName)
                    1 -> Unit
                    2 -> Unit
                }
            }
        }
    }
}



