package org.helllabs.android.xmp.compose.ui.player.viewer

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.player.Util

// TODO: 2 Column support on wider screens or in landscape.

val c = CharArray(2)

@Composable
fun ComposeChannelViewer(
    onTap: () -> Unit,
    viewInfo: ViewerInfo,
    isMuted: BooleanArray,
    modVars: IntArray,
    insName: Array<String>
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()
    val view = LocalView.current

    val xAxisMultiplier by remember {
        mutableFloatStateOf(with(density) { 24.dp.toPx() })
    }

    // https://m3.material.io/components/lists/specs
    val yAxisMultiplier by remember {
        mutableFloatStateOf(with(density) { 56.dp.toPx() })
    }

    val holdKey by remember(modVars[3]) {
        mutableStateOf(IntArray(modVars[3]))
    }
    val keyRow by remember {
        mutableStateOf(IntArray(Xmp.MAX_CHANNELS))
    }
    val channelNumber by remember(modVars[3]) {
        val list = arrayOfNulls<String?>(modVars[3])

        (0 until modVars[3]).map {
            Util.to2d(c, it + 1)
            list[it] = String(c)
        }

        mutableStateOf(list)
    }

    var canvasSize by remember {
        mutableStateOf(Size.Zero)
    }
    val yOffset = remember {
        Animatable(0f)
    }

    val scrollState = rememberScrollableState { delta ->
        scope.launch {
            // TODO bounds
            // val totalContentHeight = with(density) { 24.dp.toPx() * ins }
            // val maxOffset = (totalContentHeight - canvasSize.height).coerceAtLeast(0f)
            // val newOffset = (yOffset.value + delta).coerceIn(-maxOffset, 0f)
            yOffset.snapTo(delta)
        }
        delta
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .scrollable(
                orientation = Orientation.Vertical,
                state = scrollState
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        // TODO: Find what channel was clicked. and XMP mute that channel
                        onTap()
                    },
                    onLongPress = {
                        // TODO If a channel is solo, unmute all channels
                        //  otherwise solo this channel
                    }
                )
            }
    ) {
        if (canvasSize != size) {
            canvasSize = size
        }

        // TODO doDraw
        // Xmp.getChannelData()
        val numChannels = modVars[3]
        // numInstruments = modVars[4]
        // row = viewInfo.values[2]

        for (chn in 0 until numChannels) {
            /***** Channel Number *****/
            val chnText = textMeasurer.measure(
                text = AnnotatedString(channelNumber[chn].toString()),
                style = TextStyle(
                    color = Color(200, 200, 200, 255),
                    background = if (view.isInEditMode) Color.Magenta else Color.Unspecified,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
            )
            val textCenterX = xAxisMultiplier.div(2) - chnText.size.width.div(2)
            val textCenterY =
                yAxisMultiplier.times(chn) + yAxisMultiplier.div(2) - chnText.size.height.div(2)
            drawText(
                textLayoutResult = chnText,
                color = Color.White,
                topLeft = Offset(textCenterX, textCenterY + yOffset.value)
            )

            /***** Channel Scope Background *****/
            drawRect(
                color = Color(40, 40, 40, 255),
                size = Size(
                    width = xAxisMultiplier.times(3) - xAxisMultiplier.div(2),
                    height = yAxisMultiplier - yAxisMultiplier.div(3)
                ),
                topLeft = Offset(
                    x = xAxisMultiplier + xAxisMultiplier.div(4),
                    y = yAxisMultiplier.times(chn) + yAxisMultiplier.div(6) + yOffset.value
                )
            )

            /***** Instrument Name *****/
            val chnNameText = textMeasurer.measure(
                text = AnnotatedString("TODO"),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = Color(200, 200, 200, 255),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            )
            drawText(
                textLayoutResult = chnNameText,
                topLeft = Offset(
                    x = xAxisMultiplier.times(4),
                    y = yAxisMultiplier.times(chn) + yAxisMultiplier.div(4) + yOffset.value
                )
            )

            /***** Volume Bar Background *****/
            drawRect(
                color = Color(40, 40, 40, 255),
                topLeft = Offset(
                    x = xAxisMultiplier.times(4),
                    y = yAxisMultiplier.times(chn) - yAxisMultiplier.div(3) + yOffset.value
                ),
                size = Size(xAxisMultiplier.times(5), 16f)
            )

            /***** Pan Bar Background *****/
            drawRect(
                color = Color(40, 40, 40, 255),
                topLeft = Offset(
                    x = xAxisMultiplier.times(10),
                    y = yAxisMultiplier.times(chn) - yAxisMultiplier.div(3) + yOffset.value
                ),
                size = Size(xAxisMultiplier.times(5), 16f)
            )
        }

        if (view.isInEditMode) {
            debugScreen(
                textMeasurer = textMeasurer,
                xValue = xAxisMultiplier,
                yValue = yAxisMultiplier
            )
        }
    }
}

@Preview
@Composable
private fun Preview_ChannelViewer() {
    val viewInfo = remember {
        composeViewerSampleData()
    }
    val modVars by remember {
        val array = intArrayOf(190968, 30, 25, 12, 40, 18, 1, 0, 0, 0)
        mutableStateOf(array)
    }

    XmpTheme(useDarkTheme = true) {
        XmpCanvas(
            onChangeViewer = {},
            currentViewer = 2,
            viewInfo = viewInfo,
            isMuted = BooleanArray(modVars[3]) { false },
            modVars = modVars,
            insName = Array(modVars[4]) { String.format("%02X %s", it + 1, "Instrument Name") }
        )
    }
}
