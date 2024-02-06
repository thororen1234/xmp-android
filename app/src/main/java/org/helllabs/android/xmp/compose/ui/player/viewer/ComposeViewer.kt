package org.helllabs.android.xmp.compose.ui.player.viewer

import android.os.RemoteException
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.delay
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.theme.accent
import timber.log.Timber

// TODO: This is a WIP

enum class ViewerState {
    PLAYING, STOPPED
}

class ComposeInstrumentViewer : ViewerObject()
class ComposePatternViewer : ViewerObject()
class ComposeChannelViewer : ViewerObject()

class ViewerData {
    private var currentScreen by mutableIntStateOf(0)
    var viewerObject by mutableStateOf<ViewerObject?>(null)

    var drawState by mutableStateOf(ViewerState.STOPPED)

    var width by mutableStateOf(0.dp)
    var height by mutableStateOf(0.dp)

    init {
        init()
    }

    fun init() {
        drawState = ViewerState.PLAYING
    }

    fun update() {
        if (drawState == ViewerState.STOPPED) return // Screen isn't active

        viewerObject?.let {
            it.insName = Xmp.getInstruments()?.toList() ?: listOf() // TODO: Don't call so often

            with(it.viewerInfo) {
                time = Xmp.time() / 1000
                type = Xmp.getModType() // TODO: Don't call so often
                Xmp.getInfo(values)
                Xmp.getChannelData(volumes, finalVols, pans, instruments, keys, periods)
            }
        }
    }

    fun changeState(state: ViewerState) {
        drawState = state
    }

    fun changeCanvas() {
        currentScreen = (currentScreen + 1) % 3
        setCanvasScreen()
    }

    private fun setCanvasScreen() {
        when (currentScreen) {
            0 -> viewerObject = ComposeInstrumentViewer()
            1 -> viewerObject = ComposeChannelViewer()
            2 -> viewerObject = ComposePatternViewer()
        }

        viewerObject?.setup()
    }
}

@Composable
fun ComposeViewer() {
    val viewerData = remember { ViewerData() }

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
    LaunchedEffect(lifecycleState) {
        when (lifecycleState) {
            Lifecycle.State.RESUMED -> viewerData.changeState(ViewerState.PLAYING)
            Lifecycle.State.DESTROYED -> viewerData.changeState(ViewerState.STOPPED)
            else -> Unit
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(100)
            withFrameNanos {
                viewerData::update
            }
        }
    }

    XmpCanvas(data = viewerData)
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
private fun XmpCanvas(data: ViewerData) {
    val density = LocalDensity.current
    Surface {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds() // Needed?
                .pointerInteropFilter {
                    with(density) {

                    }
                    false
                }
                .combinedClickable(
                    onClick = {
                        data.changeCanvas()
                    },
                    onLongClick = {

                    }
                )
                .onSizeChanged {
                    with(density) {
                        data.width = it.width.toDp()
                        data.height = it.height.toDp()
                    }
                }
        ) {
            // Canvas shit
            data.viewerObject.let {
                when (it) {
                    is ComposeInstrumentViewer -> InstrumentView(it)
                    is ComposeChannelViewer -> Unit
                    is ComposePatternViewer -> Unit
                }
            }
        }
    }
}

@Composable
private fun InstrumentView(view: ComposeInstrumentViewer) {
    fun DrawScope.drawInstrumentBar(
        x: Float,
        y: Float,
        width: Int,
        fontSize: Float,
        volume: Int,
        accentColor: Color
    ) {
        // Constants
        val maxVolume = 32 // Assuming a maximum volume value for scaling
        val barHeight = fontSize // Use font size as a proxy for bar height for simplicity

        // Calculate volume-based color intensity
        val colorIntensity = volume.toFloat() / maxVolume
        val volumeColor = lerp(Color.Black, accentColor, colorIntensity)

        // Draw the volume bar
        drawRect(
            color = volumeColor,
            topLeft = Offset(x, y - barHeight),
            size = androidx.compose.ui.geometry.Size(width.toFloat(), barHeight)
        )
    }

    fun computeTextColor(volume: Int, maxVolume: Int = 32): Color {
        // Define the start (low volume) and end (high volume) colors
        val startColor = Color.Gray // Less intense color for low volume
        val endColor = Color.White // More intense color for high volume

        // Calculate the interpolation factor based on the volume
        val volumeFactor = volume.toFloat() / maxVolume.toFloat()

        // Compute the interpolated color based on the volume factor
        return lerp(startColor, endColor, volumeFactor)
    }

    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Constants and initial setup similar to your custom view
        val shadeSteps = 32
        val fontSize = 18.sp // Example, adjust based on your theme
        val fontHeight = (fontSize.value * 1.6).toInt() // Adjust as necessary

        val chn = view.modVars[3]
        val ins = view.modVars[4]
        val drawWidth = (size.width - 3 * fontHeight) / chn

        for (i in 0 until ins) {
            val drawY = (i + 1) * fontHeight - view.modVars[5] // posY in your code, adjust as needed
            var maxVol = 0

            // Loop to draw each instrument's bars and names
            for (j in 0 until chn) {
                if (view.isMuted[j]) {
                    continue
                }

                // Example logic, adjust according to your actual data and logic
                val vol = view.viewerInfo.volumes[j] / 2
                val adjustedVol = vol.coerceIn(0, shadeSteps)
                if (adjustedVol > maxVol) maxVol = adjustedVol

                val drawX = 3 * fontHeight + drawWidth * j
                drawInstrumentBar(
                    drawX,
                    drawY.toFloat(),
                    drawWidth.toInt(),
                    fontSize.toPx(),
                    adjustedVol,
                    accent
                )
            }

            val measuredText = textMeasurer.measure(
                text = view.insName[i],
                constraints = Constraints.fixedWidth((size.width * 2f / 3f).toInt()),
                style = TextStyle(fontSize = fontSize)
            )
            drawText(
                textLayoutResult = measuredText,
                color = computeTextColor(maxVol, shadeSteps),
                topLeft = Offset(0f, drawY.toFloat())
            )

//            drawText(
//                textMeasurer = textMeasurer,
//                text = view.insName[i],
//                x = 0f,
//                y = drawY.toFloat(),
//                color = computeTextColor(maxVol, shadeSteps),
//                fontSize = fontSize,
//                fontFamily = FontFamily.Monospace,
//                fontWeight = FontWeight.Bold
//            )
        }
    }
}

abstract class ViewerObject() {
    var isMuted: BooleanArray = BooleanArray(0)
    var modVars: IntArray = IntArray(10)
    var insName by mutableStateOf(listOf<String>())
    var viewerInfo by mutableStateOf(Viewer.Info())

    // fun onClick()

    // fun onClick(x: Int, y: Int)

    open fun setup() {
        Timber.d("Viewer Setup: ${this::class.java.simpleName}")
        val chn = modVars[3]
        isMuted = BooleanArray(chn)

        for (i in 0 until chn) {
            try {
                isMuted[i] = false // TODO Xmp.mute(i, -1) == 1
            } catch (e: RemoteException) {
                Timber.w("Can't read channel mute status: ${e.message}")
            }
        }
    }

    fun update() {

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

    val viewerData = remember {
        ViewerData().apply {
            this.viewerObject = ComposeInstrumentViewer().apply {
                viewerInfo = info
                modVars = intArrayOf(190968, 30, 25, 12, 24, 18, 1, 0, 0, 0)
                insName = List(modVars[4]) { "Instrument Name: $it" }
                setup() // TODO: eeeh, this should be set up in the composables.
            }
        }
    }

    XmpTheme(useDarkTheme = true) {
        XmpCanvas(data = viewerData)
    }
}
