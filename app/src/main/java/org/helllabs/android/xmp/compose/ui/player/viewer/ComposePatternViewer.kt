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
import androidx.compose.runtime.mutableIntStateOf
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
import org.helllabs.android.xmp.compose.theme.accent
import org.helllabs.android.xmp.compose.ui.player.PlayerActivity
import org.helllabs.android.xmp.compose.ui.player.Util
import org.helllabs.android.xmp.service.PlayerService
import timber.log.Timber

// Maybe keep the row numbers in view at all times, and move the channel columns instead?

@Composable
internal fun ComposePatternViewer(
    onTap: () -> Unit,
    viewInfo: ViewerInfo,
    patternInfo: PatternInfo,
    isMuted: BooleanArray,
    modVars: IntArray
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer(0)
    val view = LocalView.current

    var canvasSize by remember {
        mutableStateOf(Size.Zero)
    }

    val xAxisMultiplier by remember {
        mutableFloatStateOf(with(density) { 24.dp.toPx() })
    }

    val yAxisMultiplier by remember {
        mutableFloatStateOf(with(density) { 24.dp.toPx() })
    }

    val rowTextSize by remember {
        mutableStateOf(14.sp)
    }

    val instHexByte by remember {
        val c = CharArray(3)
        val inst = (0..255).map { i ->
            Util.to02X(c, i)
            String(c)
        }
        mutableStateOf(inst)
    }

    var currentType by remember {
        mutableStateOf("")
    }
    val effectsTable by remember(viewInfo.type) {
        with(viewInfo.type) {
            Timber.d("New FX table: $this")
            currentType = this
            val list = Effects.getEffectList(this)
            mutableStateOf(list)
        }
    }

    val numRows by remember(viewInfo.values[3]) {
        mutableIntStateOf(viewInfo.values[3])
    }

    val rowText by remember(numRows) {
        val list = (0..numRows).map {
            textMeasurer.measure(
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
        mutableStateOf(list)
    }

    val chn by remember(modVars[3]) {
        mutableIntStateOf(modVars[3])
    }

    val offsetX = remember {
        Animatable(0f)
    }

    val headerText by remember(chn) {
        val text = (0 until chn).map {
            textMeasurer.measure(
                text = AnnotatedString((it + 1).toString()),
                density = density,
                style = TextStyle(
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        mutableStateOf(text)
    }

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
            val totalContentWidth = (chn * 3 + 1) * xAxisMultiplier
            val minOffsetX = (canvasSize.width - totalContentWidth).coerceAtMost(0f)
            val newValue = (offsetX.value + delta).coerceIn(minOffsetX, 0f)
            offsetX.snapTo(newValue)
        }
        delta
    }

    LaunchedEffect(chn) {
        // Scroll to the beginning if the channel number changes
        scope.launch {
            offsetX.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 300)
            )
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
        for (i in 1 until chn) {
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
            color = accent,
            size = Size(canvasSize.width, yAxisMultiplier),
            topLeft = Offset(0f, 0f)
        )

        /***** Row Numbers Background *****/
        drawRect(
            color = Color.DarkGray,
            alpha = 1f,
            topLeft = Offset(0f, yAxisMultiplier),
            size = Size(xAxisMultiplier, canvasSize.height)
        )

        if (numRows == 0) {
            // If row numbers is 0, let's stop drawing for now. (Song change)
            return@Canvas
        }

        /***** Header Text Numbers *****/
        for (i in 0 until chn) {
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

        /***** Line Bar *****/
        barLineY = canvasSize.height.div(2).div(yAxisMultiplier).toInt().times(yAxisMultiplier)
        drawRect(
            color = Color.DarkGray,
            topLeft = Offset(0f, barLineY),
            size = Size(canvasSize.width, yAxisMultiplier)
        )

        currentRow = viewInfo.values[2].toFloat()
        rowYOffset = barLineY - (currentRow * yAxisMultiplier)
        patternInfo.pat = viewInfo.values[1]

        for (i in 0 until numRows) {
            patternInfo.lineInPattern = i

            for (j in 0 until chn) {
                // Be very careful here!
                // Our variables are latency-compensated but pattern data is current
                // so caution is needed to avoid retrieving data using old variables
                // from a module with pattern data from a newly loaded one.
                if (PlayerService.isAlive && PlayerActivity.canChangeViewer) {
                    Xmp.getPatternRow(
                        patternInfo.pat,
                        patternInfo.lineInPattern,
                        patternInfo.rowNotes,
                        patternInfo.rowInsts,
                        patternInfo.rowFxType,
                        patternInfo.rowFxParm
                    )
                }

                val info = textMeasurer.measure(
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                color = if (isMuted[j]) Color.LightGray else Color(140, 140, 160)
                            )
                        ) {
                            /****** Notes *****/
                            patternInfo.rowNotes[j].run {
                                if (this > 80) {
                                    "==="
                                } else if (this > 0) {
                                    "${Util.noteName[(this - 1) % 12]}${(this - 1) / 12}"
                                } else {
                                    "---"
                                }
                            }.also(::append)
                        }
                        withStyle(
                            style = SpanStyle(
                                color = if (isMuted[j]) Color(80, 40, 40) else Color(160, 80, 80)
                            )
                        ) {
                            /***** Instruments *****/
                            patternInfo.rowInsts[j].run {
                                if (this > 0) {
                                    instHexByte[this.toInt()]
                                } else {
                                    "--"
                                }
                            }.also(::append)
                        }
                        withStyle(
                            style = SpanStyle(
                                color = if (isMuted[j]) Color(16, 75, 28) else Color(34, 158, 60)
                            )
                        ) {
                            /***** Effects *****/
                            patternInfo.rowFxType[j].run {
                                if (this < 0) {
                                    "-"
                                } else {
                                    effectsTable[this] ?: run {
                                        Timber.w("Unknown FX: $this in chn ${j + 1}, row $i")
                                        "?"
                                    }
                                }
                            }.also(::append)
                        }
                        withStyle(
                            style = SpanStyle(
                                color = if (isMuted[j]) Color(16, 75, 28) else Color(34, 158, 60)
                            )
                        ) {
                            /***** Effects Params *****/
                            patternInfo.rowFxParm[j].run {
                                if (this > -1) {
                                    instHexByte[this.toInt()]
                                } else {
                                    "--"
                                }
                            }.also(::append)
                        }
                    },
                    density = density,
                    style = TextStyle(
                        fontSize = rowTextSize,
                        background = if (view.isInEditMode) Color.Green else Color.Unspecified,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                )
                noteRowCenterX = xAxisMultiplier.times((j * 3 + 2))
                noteCenterX = noteRowCenterX.minus(info.size.width.div(3))
                noteCenterY = rowYOffset
                    .plus(i.times(yAxisMultiplier))
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

        for (i in 0 until numRows) {
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
            debugScreen(textMeasurer, xAxisMultiplier, yAxisMultiplier)
        }
    }
}

@Preview(device = "id:Nexus One")
@Preview(device = "spec:parent=Nexus One,orientation=landscape")
@Preview(device = "id:pixel_xl")
@Preview(device = "spec:parent=pixel_xl,orientation=landscape")
@Preview(device = "id:pixel_4a")
@Preview(device = "spec:parent=pixel_4a,orientation=landscape")
@Preview(device = "id:pixel_8_pro")
@Preview(device = "spec:parent=pixel_8_pro,orientation=landscape")
@Composable
private fun Preview_PatternViewer() {
    val viewInfo = remember {
        composeViewerSampleData()
    }
    val patternInfo = remember {
        composePatternSampleData()
    }
    val modVars by remember {
        val array = intArrayOf(190968, 30, 25, 12, 40, 18, 1, 0, 0, 0)
        mutableStateOf(array)
    }

    XmpTheme(useDarkTheme = true) {
        ComposePatternViewer(
            onTap = { },
            viewInfo = viewInfo,
            patternInfo = patternInfo,
            isMuted = BooleanArray(modVars[3]) { false },
            modVars = modVars
        )
    }
}
