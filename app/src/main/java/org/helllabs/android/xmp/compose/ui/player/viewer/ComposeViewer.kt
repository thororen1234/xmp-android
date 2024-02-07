package org.helllabs.android.xmp.compose.ui.player.viewer

import android.os.RemoteException
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.ViewModel
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.theme.accent
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
private fun XmpCanvas(
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


@Composable
private fun InstrumentViewer(
    viewInfo: Viewer.Info,
    isMuted: BooleanArray,
    modVars: IntArray,
    insName: Array<String>,
) {
    val verticalScroll = rememberScrollState()

    val barPaint by remember {
        val shades = (32 downTo 0).map {
            val value = it / 32.toFloat()
            val blendedColor =
                ColorUtils.blendARGB(accent.toArgb(), Color.Transparent.toArgb(), value)
            Color(blendedColor)
        }

        mutableStateOf(shades)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(verticalScroll)
    ) {
        val chn = modVars[3]
        val ins = modVars[4]
        var vol: Int
        val paddingPx = with(LocalDensity.current) { 2.dp.toPx() }

        // TODO text volume shading
        for (i in 0 until ins) {
            Text(
                text = insName[i],
                style = TextStyle(fontSize = 18.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .drawBehind {
                        for (j in 0 until chn) {
                            if (isMuted[j]) {
                                continue
                            }

                            if (viewInfo.instruments[j] == i) {
                                val totalPadding = (chn - 1) * paddingPx
                                val availableWidth = size.width - totalPadding
                                val boxWidth = availableWidth / modVars[3]
                                val start = j * (boxWidth + paddingPx)

                                vol = (viewInfo.volumes[j] / 2).coerceAtMost(32)

                                drawRect(
                                    color = barPaint[vol],
                                    topLeft = Offset(start, 0f),
                                    size = Size(boxWidth, size.height)
                                )
                            }
                        }
                    }
            )
        }
    }
}

@Preview
@Composable
private fun Preview_ComposeViewer() {
    val info = remember {
        Viewer.Info().apply {
            time = 109
            values = intArrayOf(16, 12, 8, 64, 0, 7, 134)
            volumes = intArrayOf(
                64, 17, 32, 48, 64, 19, 53, 15, 0, 7, 39, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
            )
            finalVols = intArrayOf(
                64, 16, 32, 48, 64, 19, 3, 15, 0, 26, 16, 22, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
            )
            pans = intArrayOf(
                128, 128, 135, 128, 112, 112, 128, 160, 208, 148, 200, 128, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
            )
            instruments = intArrayOf(
                1, 1, 3, 14, 10, 12, 17, 11, 18, 20, 20, 15, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
            )
            keys = intArrayOf(
                72, 69, 67, 72, 72, 72, 77, 77, -1, -1, 74, -1, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
            )
            periods = intArrayOf(
                3424, 4071, 4570, 1298, 1227, 3424, 1225, 2565, 6848, 762, 762,
                3424, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
            )
            type = "FastTracker v2.00 XM 1.04"
        }
    }

    val viewInfo by remember {
        mutableStateOf(info)
    }
    val currentViewer by remember {
        mutableIntStateOf(0)
    }
    val modVars by remember {
        mutableStateOf(intArrayOf(190968, 30, 25, 12, 24, 18, 1, 0, 0, 0))
    }

    XmpTheme(useDarkTheme = true) {
        XmpCanvas(
            onSizeChanged = { _, _ -> },
            onChangeViewer = {},
            currentViewer = currentViewer,
            viewInfo = viewInfo,
            isMuted = BooleanArray(modVars[3]) { false },
            modVars = modVars,
            insName = Array(modVars[4]) { String.format("%02X %s", it + 1, "Instument Name") },
        )
    }
}
