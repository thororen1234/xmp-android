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
import org.helllabs.android.xmp.compose.ui.player.Util
import org.helllabs.android.xmp.service.PlayerService

private const val MAX_NOTES = 120
private val NOTES = arrayOf("C ", "C#", "D ", "D#", "E ", "F ", "F#", "G ", "G#", "A ", "A#", "B ")
private val allNotes = (0 until MAX_NOTES).map { NOTES[it % 12] + it / 12 }

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
    val textMeasurer = rememberTextMeasurer()
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

    val effectsTable by remember(viewInfo.type) {
        val list = Effects.getEffectList(viewInfo.type)
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
        for (i in 0 until chn) {
            if (i == 0) {
                // Shadow for number row
                drawRect(
                    color = Color.Gray.copy(.05f),
                    topLeft = Offset(0f, 0f),
                    size = Size(xAxisMultiplier.plus(offsetX.value), canvasSize.height)
                )
                continue
            }
            val xPosition = (i * 3 + 1) * xAxisMultiplier + offsetX.value
            drawRect(
                color = if (i % 2 == 0) Color.Unspecified else Color.Gray.copy(.05f),
                topLeft = Offset(xPosition, 0f),
                size = Size(xAxisMultiplier.times(3), canvasSize.height)
            )
        }

        /***** Header Text Background *****/
        drawRect(
            color = accent,
            size = Size(canvasSize.width, yAxisMultiplier),
            topLeft = Offset(0f, 0f)
        )

        /***** Header Text Numbers *****/
        for (i in 0 until chn) {
            val text = headerText[i]
            val division = xAxisMultiplier + (i * 3 * xAxisMultiplier) + (xAxisMultiplier * 1.5f)
            val textCenterX = offsetX.value + (division - (text.size.width / 2))
            val textCenterY = (yAxisMultiplier / 2) - (text.size.height / 2)
            // TODO culling
            drawText(
                textLayoutResult = text,
                topLeft = Offset(textCenterX, textCenterY)
            )
        }

        /***** Line Bar *****/
        val middle = canvasSize.height.div(2)
        val barLineY = middle.div(yAxisMultiplier).toInt().times(yAxisMultiplier)
        drawRect(
            color = Color.DarkGray,
            topLeft = Offset(0f, barLineY),
            size = Size(canvasSize.width, yAxisMultiplier)
        )

        val currentRow = viewInfo.values[2].toFloat()
        val rowYOffset = barLineY - (currentRow * yAxisMultiplier)
        patternInfo.pat = viewInfo.values[1]

        val numRows = viewInfo.values[3]
        if (numRows == 0) {
            // If row numbers is 0, let's stop drawing for now. (Song change)
            return@Canvas
        }
        for (i in 0 until numRows) {
            patternInfo.lineInPattern = i

            /***** Row numbers *****/
            val rowText = textMeasurer.measure(
                text = AnnotatedString(i.toString()),
                density = density,
                style = TextStyle(
                    fontSize = 11.sp,
                    background = if (view.isInEditMode) Color.Green else Color.Unspecified,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    platformStyle = PlatformTextStyle(includeFontPadding = true)
                )
            )
            val textCenterX = xAxisMultiplier.div(2).minus(rowText.size.width.div(2))
            val textCenterY = rowYOffset
                .plus(i.times(yAxisMultiplier))
                .plus(yAxisMultiplier.div(2).minus(rowText.size.height.div(2)))
            if (textCenterY < yAxisMultiplier || textCenterY + rowText.size.height > size.height) {
                // Top culling || Bottom Culling
                continue
            }
            drawText(
                textLayoutResult = rowText,
                color = Color.White,
                topLeft = Offset(textCenterX.plus(offsetX.value), textCenterY)
            )

            for (j in 0 until chn) {
                // Be very careful here!
                // Our variables are latency-compensated but pattern data is current
                // so caution is needed to avoid retrieving data using old variables
                // from a module with pattern data from a newly loaded one.
                if (PlayerService.isAlive) {
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
                            val notes = when (val note = patternInfo.rowNotes[j]) {
                                in 1..MAX_NOTES -> allNotes[note - 1]
                                else -> if (note < 0) "===" else "---"
                            }
                            append(notes)
                        }
                        withStyle(
                            style = SpanStyle(
                                color = if (isMuted[j]) Color(80, 40, 40) else Color(160, 80, 80)
                            )
                        ) {
                            /***** Instruments *****/
                            val inst = if (patternInfo.rowInsts[j] > 0) {
                                instHexByte[patternInfo.rowInsts[j].toInt()]
                            } else {
                                "--"
                            }
                            append(inst)
                        }
                        withStyle(
                            style = SpanStyle(
                                color = if (isMuted[j]) Color(16, 75, 28) else Color(34, 158, 60)
                            )
                        ) {
                            /***** Effects *****/
                            val effectType = effectsTable[patternInfo.rowFxType[j]]
                            val effect: String = when {
                                patternInfo.rowFxType[j] > -1 -> effectType ?: "?"
                                else -> "-"
                            }
                            append(effect)
                        }
                        withStyle(
                            style = SpanStyle(
                                color = if (isMuted[j]) Color(16, 75, 28) else Color(34, 158, 60)
                            )
                        ) {
                            /***** Effects Params *****/
                            val effectParam: String = when {
                                patternInfo.rowFxParm[j] > -1 ->
                                    instHexByte[patternInfo.rowFxParm[j]]

                                else -> "--"
                            }
                            append(effectParam)
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
                // TODO culling
                val noteRowCenterX = xAxisMultiplier.times((j * 3 + 2))
                val noteCenterX = noteRowCenterX.minus(info.size.width.div(3))
                drawText(
                    textLayoutResult = info,
                    topLeft = Offset(noteCenterX.plus(offsetX.value), textCenterY)
                )
            }
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
