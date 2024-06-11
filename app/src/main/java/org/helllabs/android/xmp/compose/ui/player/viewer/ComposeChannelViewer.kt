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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
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
import org.helllabs.android.xmp.compose.theme.seed
import org.helllabs.android.xmp.compose.ui.player.Util
import org.helllabs.android.xmp.model.ChannelInfo
import org.helllabs.android.xmp.model.FrameInfo
import org.helllabs.android.xmp.model.ModVars
import org.helllabs.android.xmp.service.PlayerService

// TODO: 2 Column support on wider screens or in landscape.

val c = CharArray(2)

@Composable
fun ComposeChannelViewer(
    onTap: () -> Unit,
    channelInfo: ChannelInfo,
    frameInfo: FrameInfo,
    isMuted: BooleanArray,
    modVars: ModVars,
    insName: Array<String>
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()
    val view = LocalView.current

    val xAxisMultiplier by remember {
        mutableFloatStateOf(
            with(density) {
                24.dp.toPx()
            }
        )
    }
    val yAxisMultiplier by remember {
        // https://m3.material.io/components/lists/specs
        mutableFloatStateOf(
            with(density) {
                56.dp.toPx()
            }
        )
    }
    var canvasSize by remember {
        mutableStateOf(Size.Zero)
    }
    val yOffset = remember {
        Animatable(0f)
    }
    val scopeWidth by remember {
        val width = xAxisMultiplier.times(3) - xAxisMultiplier.div(2)
        mutableFloatStateOf(width)
    }
    val numChannels by remember(modVars.numChannels) {
        mutableIntStateOf(modVars.numChannels)
    }
    val buffer = remember {
        ByteArray(Xmp.MAX_BUFFERS)
    }
    val holdKey by remember(modVars.numChannels) {
        mutableStateOf(IntArray(modVars.numChannels))
    }
    val keyRow by remember {
        mutableStateOf(IntArray(Xmp.MAX_CHANNELS))
    }
    val channelNumber by remember(modVars.numChannels) {
        val list = arrayOfNulls<String?>(modVars.numChannels)

        (0 until modVars.numChannels).map {
            Util.to2d(c, it + 1)
            list[it] = String(c)
        }

        mutableStateOf(list)
    }
    val scrollState = rememberScrollableState { delta ->
        scope.launch {
            val totalContentHeight = yAxisMultiplier * numChannels
            val maxOffset = (totalContentHeight - canvasSize.height).coerceAtLeast(0f)
            val newOffset = (yOffset.value + delta).coerceIn(-maxOffset, 0f)
            yOffset.snapTo(newOffset)
        }
        delta
    }
    val waveformPath = remember { Path() }

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
                        // TODO If a channel is solo, unmute all channels, otherwise solo this channel
                    }
                )
            }
    ) {
        if (canvasSize != size) {
            canvasSize = size
        }

        val numInstruments = modVars.numInstruments
        // row = viewInfo.values[2]

        for (chn in 0 until numChannels) {
            val ins = if (isMuted[chn]) -1 else channelInfo.instruments[chn]
            val pan = channelInfo.pans[chn]
            val period = channelInfo.periods[chn]
            val row = frameInfo.row
            var key = channelInfo.keys[chn]

            // IDK what this does, but it was in the legacy viewer
            if (key >= 0) {
                holdKey[chn] = key
                if (keyRow[chn] == row) {
                    key = -1
                } else {
                    keyRow[chn] = row
                }
            }

            /***** Channel Number *****/
            val chnText = textMeasurer.measure(
                text = AnnotatedString(channelNumber[chn].toString()),
                style = TextStyle(
                    color = Color(200, 200, 200, 255),
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

            /***** Instrument Name *****/
            if (ins in 0..<numInstruments) {
                val chnNameText = textMeasurer.measure(
                    text = AnnotatedString(insName[ins]),
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
            }

            /***** Volume Bar Background *****/
            val volY = yAxisMultiplier.times(chn + 1) - yAxisMultiplier.div(3) + yOffset.value
            drawRect(
                color = Color(40, 40, 40, 255),
                topLeft = Offset(
                    x = xAxisMultiplier.times(4),
                    y = volY
                ),
                size = Size(xAxisMultiplier.times(5), 16f)
            )

            /***** Volume Bars *****/
            val vol = if (isMuted[chn]) 0 else channelInfo.volumes[chn]
            val volSize = xAxisMultiplier.times(5) * (vol.toFloat() / 64)
            drawRect(
                color = seed.copy(alpha = .35f),
                topLeft = Offset(
                    x = xAxisMultiplier.times(4),
                    y = volY
                ),
                size = Size(volSize, 16f)
            )
            val fVol = if (isMuted[chn]) 0 else channelInfo.finalVols[chn]
            val fVolSize = xAxisMultiplier.times(5) * (fVol.toFloat() / 64)
            drawRect(
                color = seed,
                topLeft = Offset(
                    x = xAxisMultiplier.times(4),
                    y = volY
                ),
                size = Size(fVolSize, 16f)
            )

            /***** Pan Bar Background *****/
            val panY = yAxisMultiplier.times(chn + 1) - yAxisMultiplier.div(3) + yOffset.value
            drawRect(
                color = Color(40, 40, 40, 255),
                topLeft = Offset(
                    x = xAxisMultiplier.times(10),
                    y = panY
                ),
                size = Size(xAxisMultiplier.times(5), 16f)
            )

            /***** Pan Bar *****/
            val panRectWidth = 8.dp.toPx()
            val panMaxOffset = xAxisMultiplier.times(5) - panRectWidth
            val panOffset = panMaxOffset * (pan.toFloat() / 255)
            val panX = xAxisMultiplier.times(10) + panOffset
            drawRect(
                color = seed,
                topLeft = Offset(
                    x = panX,
                    y = panY
                ),
                size = Size(panRectWidth, 16f)
            )

            /***** Waveform *****/
            if (isMuted[chn]) {
                // TODO mute rect
            } else {
                if (PlayerService.isAlive.value) {
                    // Be very careful here!
                    // Our variables are latency-compensated but sample data is current
                    // so caution is needed to avoid retrieving data using old variables
                    // from a module with sample data from a newly loaded one.
                    Xmp.getSampleData(
                        key >= 0,
                        ins,
                        holdKey[chn],
                        period,
                        chn,
                        Xmp.MAX_BUFFERS,
                        buffer
                    )
                }

                /***** Channel Scope Background *****/
                val scopeXOffset = xAxisMultiplier + xAxisMultiplier.div(4)
                val scopeYOffset =
                    yAxisMultiplier.times(chn) + yAxisMultiplier.div(6) + yOffset.value
                drawRect(
                    color = Color(40, 40, 40, 255),
                    size = Size(
                        width = scopeWidth,
                        height = yAxisMultiplier - yAxisMultiplier.div(3)
                    ),
                    topLeft = Offset(
                        x = scopeXOffset,
                        y = scopeYOffset
                    )
                )

//                Timber.d(
//                    "Channel: $chn, Buffer: \n ${buffer.joinToString(" ") { it.toString() }}"
//                    "Channel $chn, Final Vol: ${viewInfo.finalVols[chn]}"
//                )

                // Sine wave to draw over the above rectangle.
//                val centerY = scopeYOffset + (yAxisMultiplier - yAxisMultiplier.div(3)) / 2
//                val halfHeight = (yAxisMultiplier - yAxisMultiplier.div(3)) / 2
//                val maxVal = buffer.maxOrNull() ?: 127
//                val minVal = buffer.minOrNull() ?: -128
//                val volumeScale = viewInfo.finalVols[chn].coerceIn(0, 64) / 64f
//                val yScale = halfHeight / (maxVal - minVal)
//                buffer.forEachIndexed { index, byteValue ->
//                    val x = scopeXOffset + (scopeWidth / buffer.size) * index
//                    val y = if (byteValue == 0.toByte()) {
//                        centerY
//                    } else {
//                        val scaledByteValue = byteValue * (halfHeight / 256f) * volumeScale
//                        (centerY - scaledByteValue) - (byteValue * yScale * volumeScale)
//                    }
//
//                    if (index == 0) {
//                        waveformPath.moveTo(x, y)
//                    } else {
//                        waveformPath.lineTo(x, y)
//                    }
//                }

                // TODO some wave forms are out of grid on some modules
                // TODO some wave forms peek outside the scope background
                val centerY = scopeYOffset + (yAxisMultiplier - yAxisMultiplier.div(3)) / 2
                val halfHeight = (yAxisMultiplier - yAxisMultiplier.div(3)) / 2
                val maxVal = buffer.maxOrNull() ?: 127
                val minVal = buffer.minOrNull() ?: -128
                val volumeScale = channelInfo.finalVols[chn].coerceIn(0, 64) / 64f
                val yScale = halfHeight / (maxVal - minVal)
                val widthScale = scopeWidth / buffer.size
                val xValues = FloatArray(buffer.size) { index -> scopeXOffset + widthScale * index }
                buffer.forEachIndexed { index, byteValue ->
                    val x = xValues[index]
                    val y = if (byteValue == 0.toByte()) {
                        centerY
                    } else {
                        val scaledByteValue = byteValue * (halfHeight / 256f) * volumeScale
                        (centerY - scaledByteValue) - (byteValue * yScale * volumeScale)
                    }

                    if (index == 0) {
                        waveformPath.moveTo(x, y)
                    } else {
                        waveformPath.lineTo(x, y)
                    }
                }

                drawPath(
                    path = waveformPath,
                    color = Color.Green,
                    style = Stroke(
                        width = 0.75f,
                        cap = StrokeCap.Butt,
                        join = StrokeJoin.Bevel,
                    )
                )
                waveformPath.reset()
            }

            if (view.isInEditMode) {
                // DEBUG: Make sure the bars are equal in size
                if (volY != panY) {
                    throw Exception("Bar backgrounds not equal")
                }
            }
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
    val modVars = remember {
        ModVars(190968, 30, 25, 12, 40, 18, 1, 0)
    }

    XmpTheme(useDarkTheme = true) {
        ComposeChannelViewer(
            onTap = {},
            channelInfo = composeChannelInfoSampleData(),
            frameInfo = composeFrameInfoSampleData(),
            isMuted = BooleanArray(modVars.numChannels) { false },
            modVars = modVars,
            insName = Array(
                modVars.numInstruments
            ) { String.format("%02X %s", it + 1, "Instrument Name") }
        )
    }
}
