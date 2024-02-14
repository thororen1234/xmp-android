package org.helllabs.android.xmp.compose.ui.player.viewer

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import org.helllabs.android.xmp.compose.theme.accent
import org.helllabs.android.xmp.compose.ui.player.Util
import org.helllabs.android.xmp.service.PlayerService
import timber.log.Timber

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
    val yAxisMultiplier by remember {
        // https://m3.material.io/components/lists/specs
        mutableFloatStateOf(with(density) { 56.dp.toPx() })
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
    val numChannels by remember(modVars[3]) {
        mutableIntStateOf(modVars[3])
    }
    val buffer by remember {
        val array = Array(Xmp.MAX_CHANNELS) {
            ByteArray(scopeWidth.toInt())
        }
        mutableStateOf(array)
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

    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis {
                if (PlayerService.isAlive) {
                    Xmp.getChannelData(
                        viewInfo.volumes,
                        viewInfo.finalVols,
                        viewInfo.pans,
                        viewInfo.instruments,
                        viewInfo.keys,
                        viewInfo.periods
                    )
                }
            }
        }
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


        val numInstruments = modVars[4]
        // row = viewInfo.values[2]

        for (chn in 0 until numChannels) {
            val ins = if (isMuted[chn]) -1 else viewInfo.instruments[chn]
            val pan = viewInfo.pans[chn]
            val period = viewInfo.periods[chn]
            val row = viewInfo.values[2]
            var key = viewInfo.keys[chn]

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
            val vol = if (isMuted[chn]) 0 else viewInfo.volumes[chn]
            val volSize = xAxisMultiplier.times(5) * (vol.toFloat() / 64)
            drawRect(
                color = accent.copy(alpha = .35f),
                topLeft = Offset(
                    x = xAxisMultiplier.times(4),
                    y = volY
                ),
                size = Size(volSize, 16f)
            )
            val fvol = if (isMuted[chn]) 0 else viewInfo.finalVols[chn]
            val fVolSize = xAxisMultiplier.times(5) * (fvol.toFloat() / 64)
            drawRect(
                color = accent,
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
                color = accent,
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
                if (PlayerService.isAlive) {
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
                        scopeWidth.toInt(),
                        buffer[chn]
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

                // Sinewave to draw over the above rectangle.
                // TODO kinda terrible
                val centerY = scopeYOffset + (yAxisMultiplier - yAxisMultiplier.div(3)) / 2
                buffer[chn].forEachIndexed { index, byteValue ->
                    val x = scopeXOffset + (scopeWidth / buffer[chn].size) * index
                    val halfHeight = (yAxisMultiplier - yAxisMultiplier.div(3)) / 2
                    val scaledByteValue = byteValue * (halfHeight / 256f)
                    val y = centerY - scaledByteValue

                    if (index == 0) waveformPath.moveTo(x, y)
                    else waveformPath.lineTo(x, y)
                }

                drawPath(
                    path = waveformPath,
                    color = Color.Green
                )
                waveformPath.reset()
            }

            if (view.isInEditMode) {
                // DEBUG: Make sure the bars are equal
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
            serviceConnected = false,
            currentViewer = 2,
            viewInfo = viewInfo,
            isMuted = BooleanArray(modVars[3]) { false },
            modVars = modVars,
            insName = Array(modVars[4]) { String.format("%02X %s", it + 1, "Instrument Name") }
        )
    }
}
