package org.helllabs.android.xmp.compose.ui.player.viewer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.theme.seed
import org.helllabs.android.xmp.compose.ui.player.ChannelMuteState
import org.helllabs.android.xmp.compose.ui.player.Util
import org.helllabs.android.xmp.model.FrameInfo
import org.helllabs.android.xmp.model.ModVars
import org.helllabs.android.xmp.service.PlayerService
import timber.log.Timber

// Maybe keep the row numbers in view at all times, and move the channel columns instead?

// TODO: Lag when there are many channels, is it culling right?
//  many calls to textMeasure seems to tank performance.

@Composable
internal fun ComposePatternViewer(
    onTap: () -> Unit,
    fi: FrameInfo,
    isMuted: ChannelMuteState,
    modType: String,
    modVars: ModVars
) {
    val density = LocalDensity.current
    val infoTextMeasurer = rememberTextMeasurer(16384)
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    var canvasSize by remember {
        mutableStateOf(Size.Zero)
    }
    val xAxisMultiplier = remember {
        with(density) { 24.dp.toPx() }
    }
    val yAxisMultiplier = remember {
        with(density) { 24.dp.toPx() }
    }
    val offsetX = remember {
        Animatable(0f)
    }

    var currentType by remember { mutableStateOf("") }
    val effectsTable = remember(modType) {
        val type = Effects.getEffectList(modType)
        currentType = type.name
        type.table
    }

    val rowTextMeasurer = rememberTextMeasurer(256)
    val rowText = remember(fi.numRows) {
        (0..fi.numRows).map {
            rowTextMeasurer.measure(
                text = AnnotatedString(it.toString()),
                density = density,
                style = TextStyle(
                    fontSize = 11.sp,
                    background = if (view.isInEditMode) Color.Green else Color.Unspecified,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    platformStyle = PlatformTextStyle(includeFontPadding = true)
                )
            )
        }
    }

    val headerTextMeasurer = rememberTextMeasurer(64)
    val headerText = remember(modVars.numChannels) {
        (0 until modVars.numChannels).map {
            headerTextMeasurer.measure(
                text = AnnotatedString("${it + 1}"),
                density = density,
                style = TextStyle(
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }

    val rowFxParm = remember { ByteArray(64) }
    val rowFxType = remember { ByteArray(64) }
    val rowInsts = remember { ByteArray(64) }
    val rowNotes = remember { ByteArray(64) }

    var hdrDivision by remember { mutableFloatStateOf(0f) }
    var hdrTxtCenterX by remember { mutableFloatStateOf(0f) }
    var hdrTxtCenterY by remember { mutableFloatStateOf(0f) }

    var barLineY by remember { mutableFloatStateOf(0f) }

    var currentRow by remember { mutableFloatStateOf(0f) }
    var rowYOffset by remember { mutableFloatStateOf(0f) }

    var noteRowCenterX by remember { mutableFloatStateOf(0f) }
    var noteCenterX by remember { mutableFloatStateOf(0f) }
    var noteCenterY by remember { mutableFloatStateOf(0f) }

    var effectiveNoteCenterX by remember { mutableFloatStateOf(0f) }

    var textCenterX by remember { mutableFloatStateOf(0f) }
    var textCenterY by remember { mutableFloatStateOf(0f) }

    val scrollState = rememberScrollableState { delta ->
        scope.launch {
            val totalContentWidth = (modVars.numChannels * 3 + 1) * xAxisMultiplier
            val minOffsetX = (canvasSize.width - totalContentWidth).coerceAtMost(0f)
            val newValue = (offsetX.value + delta).coerceIn(minOffsetX, 0f)
            offsetX.snapTo(newValue)
        }
        delta
    }

    LaunchedEffect(modVars.numChannels) {
        // Scroll to the beginning if the channel number changes
        scope.launch {
            offsetX.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 300))
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .scrollable(
                orientation = Orientation.Horizontal,
                state = scrollState
            )
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTap() })
            }
    ) {
        if (canvasSize != size) {
            canvasSize = size
        }

        /***** Column Shadows (Even) *****/
        for (i in 1 until modVars.numChannels) {
            drawRect(
                color = if (i % 2 == 0) Color(0x00000000) else Color(0x0D888888),
                topLeft = Offset(
                    x = (i * 3 + 1) * xAxisMultiplier + offsetX.value,
                    y = 0f
                ),
                size = Size(width = xAxisMultiplier * 3, height = canvasSize.height)
            )
        }

        /***** Header Text Background *****/
        drawRect(
            color = seed,
            size = Size(canvasSize.width, yAxisMultiplier),
            topLeft = Offset(0f, 0f)
        )

        /***** Line Bar *****/
        barLineY = canvasSize.height.div(2).div(yAxisMultiplier).toInt().times(yAxisMultiplier)
        drawRect(
            color = Color.DarkGray,
            topLeft = Offset(0f, barLineY),
            size = Size(canvasSize.width, yAxisMultiplier)
        )

        /***** Row Numbers Background *****/
        drawRect(
            color = Color.DarkGray,
            alpha = 1f,
            topLeft = Offset(0f, yAxisMultiplier),
            size = Size(xAxisMultiplier, canvasSize.height)
        )

        if (fi.numRows == 0) {
            // If row numbers is 0, let's stop drawing for now. (Song change)
            return@Canvas
        }

        /***** Header Text Numbers *****/
        for (i in 0 until modVars.numChannels) {
            hdrDivision = xAxisMultiplier + (i * 3 * xAxisMultiplier) + (xAxisMultiplier * 1.5f)
            hdrTxtCenterX = offsetX.value + (hdrDivision - (headerText[i].size.width / 2))
            hdrTxtCenterY = (yAxisMultiplier / 2) - (headerText[i].size.height / 2)

            // Left Culling || Right Culling
            if (hdrTxtCenterX + headerText[i].size.width < 0 || hdrTxtCenterX > canvasSize.width) {
                continue
            }

            drawText(
                textLayoutResult = headerText[i],
                topLeft = Offset(hdrTxtCenterX, hdrTxtCenterY)
            )
        }

        currentRow = fi.row.toFloat()
        rowYOffset = barLineY - (currentRow * yAxisMultiplier)

        for (row in 0 until fi.numRows) {
            for (chn in 0 until modVars.numChannels) {
                // Be very careful here!
                // Our variables are latency-compensated but pattern data is current
                // so caution is needed to avoid retrieving data using old variables
                // from a module with pattern data from a newly loaded one.
                if (PlayerService.isAlive.value) {
                    Xmp.getPatternRow(
                        pat = fi.pattern,
                        row = row,
                        rowNotes = rowNotes,
                        rowInstruments = rowInsts,
                        rowFxType = rowFxType,
                        rowFxParm = rowFxParm
                    )
                }

                val info = infoTextMeasurer.measure(
                    text = buildAnnotatedString {
                        /****** Notes *****/
                        withStyle(
                            style = SpanStyle(
                                color = if (isMuted.isMuted[chn]) {
                                    Color(60, 60, 60)
                                } else {
                                    Color(140, 140, 160)
                                }
                            ),
                            block = {
                                append(Util.note(rowNotes[chn].toInt()))
                            }
                        )
                        /***** Instruments *****/
                        withStyle(
                            style = SpanStyle(
                                color = if (isMuted.isMuted[chn]) {
                                    Color(80, 40, 40)
                                } else {
                                    Color(160, 80, 80)
                                }
                            ),
                            block = {
                                append(Util.num(rowInsts[chn].toInt()))
                            }
                        )
                        /***** Effects *****/
                        withStyle(
                            style = SpanStyle(
                                color = if (isMuted.isMuted[chn]) {
                                    Color(16, 75, 28)
                                } else {
                                    Color(34, 158, 60)
                                }
                            )
                        ) {
                            val fxt = rowFxType[chn]
                            val fx = if (fxt < 0) {
                                "-"
                            } else {
                                effectsTable.getOrElse(fxt) {
                                    Timber.w(
                                        "Unknown FX: $fxt in chn ${chn + 1}, row $row, " +
                                            "using $currentType. Type:$modType"
                                    )
                                    "?"
                                }
                            }
                            append(fx)
                        }
                        /***** Effects Params *****/
                        withStyle(
                            style = SpanStyle(
                                color = if (isMuted.isMuted[chn]) {
                                    Color(16, 75, 28)
                                } else {
                                    Color(34, 158, 60)
                                }
                            ),
                            block = {
                                append(Util.num(rowFxParm[chn].toInt()))
                            }
                        )
                    },
                    density = density,
                    style = TextStyle(
                        fontSize = 14.sp,
                        background = if (view.isInEditMode) Color.Green else Color.Unspecified,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                )
                noteRowCenterX = xAxisMultiplier.times((chn * 3 + 2))
                noteCenterX = noteRowCenterX.minus(info.size.width.div(3))
                noteCenterY = rowYOffset
                    .plus(row.times(yAxisMultiplier))
                    .plus(yAxisMultiplier.div(2).minus(info.size.height.div(2)))

                // Top culling || Bottom Culling
                if (noteCenterY < yAxisMultiplier ||
                    (noteCenterY - yAxisMultiplier) + info.size.height > size.height
                ) {
                    continue
                }

                // Left Culling || Right Culling
                effectiveNoteCenterX = noteCenterX.plus(offsetX.value)
                if (effectiveNoteCenterX + info.size.width < 0 ||
                    effectiveNoteCenterX > canvasSize.width
                ) {
                    continue
                }

                drawText(
                    textLayoutResult = info,
                    topLeft = Offset(noteCenterX.plus(offsetX.value), noteCenterY)
                )
            }
        }

        for (i in 0 until fi.numRows) {
            /***** Row numbers *****/
            textCenterX = xAxisMultiplier.div(2).minus(rowText[i].size.width.div(2))
            textCenterY = rowYOffset
                .plus(i.times(yAxisMultiplier))
                .plus(yAxisMultiplier.div(2).minus(rowText[i].size.height.div(2)))

            // Top culling || Bottom Culling
            if (textCenterY < yAxisMultiplier ||
                (textCenterY - yAxisMultiplier) + rowText[i].size.height > size.height
            ) {
                continue
            }
            drawText(
                textLayoutResult = rowText[i],
                color = Color.White,
                topLeft = Offset(textCenterX, textCenterY)
            )
        }

        if (view.isInEditMode) {
            // debugPatternViewColumns()
            debugScreen(rowTextMeasurer, xAxisMultiplier, yAxisMultiplier)
        }
    }
}

@Preview
@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun Preview_PatternViewer() {
    val modVars = composeSampleModVars()
    XmpTheme(useDarkTheme = true) {
        ComposePatternViewer(
            onTap = { },
            modType = "FastTracker v2.00 XM 1.04",
            fi = composeSampleFrameInfo(),
            isMuted = ChannelMuteState(isMuted = BooleanArray(modVars.numChannels) { false }),
            modVars = modVars,
        )
    }
}
