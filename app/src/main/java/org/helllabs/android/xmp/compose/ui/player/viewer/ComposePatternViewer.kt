package org.helllabs.android.xmp.compose.ui.player.viewer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.theme.accent
import org.helllabs.android.xmp.compose.ui.player.Util
import org.helllabs.android.xmp.service.PlayerService

private const val MAX_NOTES = 120
private val NOTES = arrayOf("C ", "C#", "D ", "D#", "E ", "F ", "F#", "G ", "G#", "A ", "A#", "B ")
private val allNotes = (0 until MAX_NOTES).map { NOTES[it % 12] + it / 12 }

// TODO: I don't like the text centering. Row numbers >99 clash into the next row
//  and columns are too close to one another

// TODO: Maybe keep the row numbers in view at all times, and move the channel columns instead?

@Composable
internal fun ComposePatternViewer(
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
        mutableFloatStateOf(with(density) { 22.dp.toPx() })
    }

    val yAxisMultiplier by remember {
        mutableFloatStateOf(with(density) { 24.dp.toPx() })
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
                    background = if (view.isInEditMode) Color.Green else Color.Unspecified,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
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
        // Scroll to the top on song change
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
    ) {
        if (canvasSize != size) {
            canvasSize = size
        }

        // Column Shadows, even numbers
        for (i in 0 until chn) {
            if (i == 0) {
                // Shadow for number row
                drawRect(
                    color = Color.Gray.copy(.05f),
                    topLeft = Offset(0f, 0f),
                    size = Size(xAxisMultiplier + offsetX.value, canvasSize.height)
                )
                continue
            }
            val xPosition = (i * 3 + 1) * xAxisMultiplier + offsetX.value
            drawRect(
                color = if (i % 2 == 0) Color.Unspecified else Color.Gray.copy(.05f),
                topLeft = Offset(xPosition, 0f),
                size = Size(66.dp.toPx(), canvasSize.height)
            )
        }

        // Header Text Background
        drawRect(
            color = accent,
            size = Size(canvasSize.width, yAxisMultiplier),
            topLeft = Offset(0f, 0f)
        )

        // Header Text Numbers
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

        // Line Bar
        val barLineY = yAxisMultiplier * 17
        drawRect(
            color = Color.DarkGray,
            topLeft = Offset(0f, barLineY),
            size = Size(canvasSize.width, yAxisMultiplier)
        )

        val currentRow = viewInfo.values[2].toFloat()
        val rowHeight = yAxisMultiplier
        val rowYOffset = barLineY - (currentRow * rowHeight)
        patternInfo.pat = viewInfo.values[1]

        // Row numbers
        val numRows = viewInfo.values[3]
        if (numRows == 0) {
            // If row numbers is 0, let's stop drawing for now. (Song change)
            return@Canvas
        }
        for (i in 0 until numRows) {
            patternInfo.lineInPattern = i

            val rowText = textMeasurer.measure(
                text = AnnotatedString(i.toString()),
                density = density,
                style = TextStyle(
                    fontSize = 14.sp,
                    background = if (view.isInEditMode) Color.Green else Color.Unspecified,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            )
            val textCenterX = 11.dp.toPx() - (rowText.size.width / 2)
            val textCenterY = rowYOffset + (i * rowHeight) +
                (rowHeight / 2) - (rowText.size.height / 2)
            if (textCenterY < yAxisMultiplier || textCenterY + rowText.size.height > size.height) {
                // Top culling || Bottom Culling
                continue
            }
            drawText(
                textLayoutResult = rowText,
                color = Color.White,
                topLeft = Offset(offsetX.value + textCenterX, textCenterY)
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

                /****** Notes *****/
                val notes = when (val note = patternInfo.rowNotes[j]) {
                    in 1..MAX_NOTES -> allNotes[note - 1]
                    else -> if (note < 0) "===" else "---"
                }
                val noteText = textMeasurer.measure(
                    text = AnnotatedString(notes),
                    density = density,
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = if (isMuted[j]) Color.LightGray else Color(140, 140, 160),
                        background = if (view.isInEditMode) Color.Green else Color.Unspecified,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                )
                // TODO culling
                val noteDivision = (j * 3 + 1) * xAxisMultiplier
                val noteCenterX = offsetX.value + noteDivision
                drawText(
                    textLayoutResult = noteText,
                    topLeft = Offset(noteCenterX, textCenterY)
                )

                /***** Instruments *****/
                val inst =
                    if (patternInfo.rowInsts[j] > 0) instHexByte[patternInfo.rowInsts[j].toInt()] else "--"
                val instText = textMeasurer.measure(
                    text = AnnotatedString(inst),
                    density = density,
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = if (isMuted[j]) Color(80, 40, 40) else Color(160, 80, 80),
                        background = if (view.isInEditMode) Color.Green else Color.Unspecified,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                )
                // TODO culling
                val instOffsetX = noteCenterX + noteText.size.width.toDp().toPx()
                drawText(
                    textLayoutResult = instText,
                    topLeft = Offset(instOffsetX, textCenterY)
                )

                /***** Effects *****/
                val effectType = effectsTable[patternInfo.rowFxType[j]]
                val effect: String = when {
                    patternInfo.rowFxType[j] > -1 -> effectType ?: "?"
                    else -> "-"
                }
                val fxText = textMeasurer.measure(
                    text = AnnotatedString(effect),
                    density = density,
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = if (isMuted[j]) Color(16, 75, 28) else Color(34, 158, 60),
                        background = if (view.isInEditMode) Color.Green else Color.Unspecified,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                )
                // TODO culling
                val fxOffsetX = instOffsetX + instText.size.width.toDp().toPx()
                drawText(
                    textLayoutResult = fxText,
                    topLeft = Offset(fxOffsetX, textCenterY)
                )

                // Effects Params
                val effectParam: String = when {
                    patternInfo.rowFxParm[j] > -1 -> instHexByte[patternInfo.rowFxParm[j]]
                    else -> "--"
                }
                val fxParmText = textMeasurer.measure(
                    text = AnnotatedString(effectParam),
                    density = density,
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = if (isMuted[j]) Color(16, 75, 28) else Color(34, 158, 60),
                        background = if (view.isInEditMode) Color.Green else Color.Unspecified,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                )
                // TODO culling
                val fxParmOffsetX = fxOffsetX + fxText.size.width.toDp().toPx()
                drawText(
                    textLayoutResult = fxParmText,
                    topLeft = Offset(fxParmOffsetX, textCenterY)
                )
            }
        }

        if (BuildConfig.DEBUG) {
            if (view.isInEditMode) {
                debugScreen(
                    textMeasurer = textMeasurer,
                    xValue = xAxisMultiplier
                )
                // debugPatternViewColumns()
            }

            drawText(
                textLayoutResult = textMeasurer.measure(
                    text = AnnotatedString(
                        "Row: ${currentRow.toInt()} / Rows: $numRows"
                    )
                )
            )
            drawText(
                textMeasurer.measure(text = AnnotatedString("Chn: $chn")),
                topLeft = Offset(0f, 28f)
            )
        }
    }
}

@Preview
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
            viewInfo = viewInfo,
            patternInfo = patternInfo,
            isMuted = BooleanArray(modVars[3]) { false },
            modVars = modVars
        )
    }
}
