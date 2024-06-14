package org.helllabs.android.xmp.compose.ui.player.viewer

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.theme.michromaFontFamily
import org.helllabs.android.xmp.compose.theme.seed
import org.helllabs.android.xmp.compose.ui.player.ChannelMuteState
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
    isMuted: ChannelMuteState,
    modVars: ModVars,
    insName: Array<String>
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()
    val view = LocalView.current

    val xMultiplier by remember {
        mutableFloatStateOf(
            with(density) {
                24.dp.toPx()
            }
        )
    }
    val yMultiplier by remember {
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
        val width = xMultiplier.times(3) - xMultiplier.div(2)
        mutableFloatStateOf(width)
    }
    val buffer = remember {
        ByteArray(Xmp.MAX_BUFFERS)
    }
    val isChnMuted by remember(isMuted) {
        // Need this to keep pointerInput updated for any changes.
        mutableStateOf(isMuted.isMuted)
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
            val totalContentHeight = yMultiplier * modVars.numChannels
            val maxOffset = (totalContentHeight - canvasSize.height).coerceAtLeast(0f)
            val newOffset = (yOffset.value + delta).coerceIn(-maxOffset, 0f)
            yOffset.snapTo(newOffset)
        }
        delta
    }
    LaunchedEffect(modVars.numInstruments, modVars.numChannels) {
        // Scroll to the top on song change
        scope.launch {
            yOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 300)
            )
        }
    }
    val waveformPath = remember { Path() }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .scrollable(
                orientation = Orientation.Vertical,
                state = scrollState
            )
            .pointerInput(isChnMuted) {
                detectTapGestures(
                    onTap = { offset ->
                        val adjustedOffset = offset.copy(y = offset.y - yOffset.value)
                        for (chn in 0 until modVars.numChannels) {
                            val scopeXOffset = xMultiplier + xMultiplier.div(4)
                            val scopeYOffset = yMultiplier.times(chn) + yMultiplier.div(6)
                            val scopeHeight = yMultiplier - yMultiplier.div(3)
                            val scopeRect = Rect(
                                left = scopeXOffset,
                                top = scopeYOffset,
                                right = scopeXOffset + scopeWidth,
                                bottom = scopeYOffset + scopeHeight
                            )

                            if (scopeRect.contains(adjustedOffset)) {
                                Xmp.mute(chn, 2)
                                return@detectTapGestures
                            }
                        }

                        onTap()
                    },
                    onLongPress = { offset ->
                        val adjustedOffset = offset.copy(y = offset.y - yOffset.value)
                        for (chn in 0 until modVars.numChannels) {
                            val scopeXOffset = xMultiplier + xMultiplier.div(4)
                            val scopeYOffset = yMultiplier.times(chn) + yMultiplier.div(6)
                            val scopeHeight = yMultiplier - yMultiplier.div(3)
                            val scopeRect = Rect(
                                left = scopeXOffset,
                                top = scopeYOffset,
                                right = scopeXOffset + scopeWidth,
                                bottom = scopeYOffset + scopeHeight
                            )

                            if (scopeRect.contains(adjustedOffset)) {
                                val unMuteCount = isChnMuted.count { !it }
                                if (unMuteCount == 1) {
                                    // Un-mute all
                                    for (i in 0 until modVars.numChannels) {
                                        Xmp.mute(i, 0)
                                    }
                                } else {
                                    // Mute all except chn
                                    for (i in 0 until modVars.numChannels) {
                                        Xmp.mute(i, if (i == chn) 0 else 1)
                                    }
                                }
                            }
                        }
                    }
                )
            }
    ) {
        if (canvasSize != size) {
            canvasSize = size
        }

        for (chn in 0 until modVars.numChannels) {
            val ins = channelInfo.instruments[chn]
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
            val textCenterX = xMultiplier.div(2) - chnText.size.width.div(2)
            val textCenterY =
                yMultiplier.times(chn) + yMultiplier.div(2) - chnText.size.height.div(2)
            drawText(
                textLayoutResult = chnText,
                color = Color.White,
                topLeft = Offset(textCenterX, textCenterY + yOffset.value)
            )

            /***** Instrument Name *****/
            if (ins in 0..<modVars.numInstruments) {
                val chnNameText = textMeasurer.measure(
                    text = AnnotatedString(if (isChnMuted[chn]) "---" else insName[ins]),
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
                        x = xMultiplier.times(4),
                        y = yMultiplier.times(chn) + yMultiplier.div(4) + yOffset.value
                    )
                )
            }

            /***** Volume Bar Background *****/
            val volY = yMultiplier.times(chn + 1) - yMultiplier.div(3) + yOffset.value
            drawRect(
                color = Color(40, 40, 40, 255),
                topLeft = Offset(
                    x = xMultiplier.times(4),
                    y = volY
                ),
                size = Size(xMultiplier.times(5), 16f)
            )

            /***** Volume Bars *****/
            val vol = if (isChnMuted[chn]) 0 else channelInfo.volumes[chn]
            val volSize = xMultiplier.times(5) * (vol.toFloat() / 64)
            drawRect(
                color = seed.copy(alpha = .35f),
                topLeft = Offset(
                    x = xMultiplier.times(4),
                    y = volY
                ),
                size = Size(volSize, 16f)
            )
            val fVol = if (isChnMuted[chn]) 0 else channelInfo.finalVols[chn]
            val fVolSize = xMultiplier.times(5) * (fVol.toFloat() / 64)
            drawRect(
                color = seed,
                topLeft = Offset(
                    x = xMultiplier.times(4),
                    y = volY
                ),
                size = Size(fVolSize, 16f)
            )

            /***** Pan Bar Background *****/
            val panY = yMultiplier.times(chn + 1) - yMultiplier.div(3) + yOffset.value
            drawRect(
                color = Color(40, 40, 40, 255),
                topLeft = Offset(
                    x = xMultiplier.times(10),
                    y = panY
                ),
                size = Size(xMultiplier.times(5), 16f)
            )

            /***** Pan Bar *****/
            val panRectWidth = 8.dp.toPx()
            val panMaxOffset = xMultiplier.times(5) - panRectWidth
            val panOffset = if (isChnMuted[chn]) 0f else panMaxOffset * (pan.toFloat() / 255)
            val panX = xMultiplier.times(10) + panOffset
            drawRect(
                color = seed,
                topLeft = Offset(
                    x = panX,
                    y = panY
                ),
                size = Size(panRectWidth, 16f)
            )

            /***** Scope or Mute Background X/Y *****/
            val scopeXOffset = xMultiplier + xMultiplier.div(4)
            val scopeYOffset = yMultiplier.times(chn) + yMultiplier.div(6) + yOffset.value
            val scopeHeight = yMultiplier - yMultiplier.div(3)

            /***** Waveform *****/
            if (isChnMuted[chn]) {
                /***** Muted Channel Scope Background *****/
                drawRect(
                    color = Color(60, 0, 0, 255),
                    size = Size(
                        width = scopeWidth,
                        height = scopeHeight
                    ),
                    topLeft = Offset(
                        x = scopeXOffset,
                        y = scopeYOffset
                    )
                )
                val muteText = textMeasurer.measure(
                    text = AnnotatedString("MUTE"),
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = michromaFontFamily,
                    )
                )
                drawText(
                    textLayoutResult = muteText,
                    topLeft = Offset(
                        x = scopeXOffset + (scopeWidth - muteText.size.width) / 2,
                        y = scopeYOffset + (scopeHeight - muteText.size.height) / 2
                    )
                )
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
                drawRect(
                    color = Color(40, 40, 40, 255),
                    size = Size(
                        width = scopeWidth,
                        height = scopeHeight
                    ),
                    topLeft = Offset(
                        x = scopeXOffset,
                        y = scopeYOffset
                    )
                )

                // Waveform testing
//                val buffer = ByteArray(Xmp.MAX_BUFFERS) { i ->
//                    val minValue = -128
//                    val maxValue = 127
//
//                    // PI
//                    // val amplitude = (maxValue - minValue) / 2
//                    // val offset = (maxValue + minValue) / 2
//                    // val angle = 2 * PI * i / Xmp.MAX_BUFFERS
//                    // val value = sin(angle) * amplitude + offset
//                    // value.toInt().toByte()
//
//                    // Random
//                    // Random.nextInt(minValue, maxValue + 1).toByte()
//
//                    // Sawtooth
//                    val range = maxValue - minValue + 1
//                    val stepsPerRepeat = Xmp.MAX_BUFFERS / 24 // Tooth Steps
//                    val value = ((i % stepsPerRepeat) * range / stepsPerRepeat + minValue).toByte()
//                    value
//
//                    // Square
//                    // val stepsPerRepeat = Xmp.MAX_BUFFERS / 8
//                    // val halfPeriod = stepsPerRepeat / 2
//                    // if (i % stepsPerRepeat < halfPeriod) maxValue.toByte() else minValue.toByte()
//                }
//                channelInfo.finalVols[chn] = 96

                val centerY = scopeYOffset + (scopeHeight / 2)
                val halfHeight = scopeHeight.div(2)
                val maxVal = buffer.maxOrNull()?.toFloat() ?: 127f
                val minVal = buffer.minOrNull()?.toFloat() ?: -128f
                val range = maxVal - minVal
                val volumeScale = channelInfo.finalVols[chn].coerceIn(0, 64) / 64f
                val widthScale = scopeWidth / buffer.size
                val xValues = FloatArray(buffer.size) { index -> scopeXOffset + widthScale * index }
                buffer.forEachIndexed { index, byteValue ->
                    val x = xValues[index]
                    val normalizedValue = if (byteValue == 0.toByte()) {
                        0f
                    } else {
                        ((byteValue - minVal) / range - 0.5f) * 2f
                    }
                    val y = centerY - (normalizedValue * halfHeight * volumeScale)

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
                xValue = xMultiplier,
                yValue = yMultiplier
            )
        }
    }
}

@Preview
@Composable
private fun Preview_ChannelViewer() {
    val modVars = composeSampleModVars()
    XmpTheme(useDarkTheme = true) {
        ComposeChannelViewer(
            onTap = {},
            channelInfo = composeSampleChannelInfo(),
            frameInfo = composeSampleFrameInfo(),
            isMuted = ChannelMuteState(BooleanArray(modVars.numChannels) { it % 2 == 0 }),
            modVars = modVars,
            insName = Array(
                modVars.numInstruments
            ) { String.format("%02X %s", it + 1, "Instrument Name") }
        )
    }
}
