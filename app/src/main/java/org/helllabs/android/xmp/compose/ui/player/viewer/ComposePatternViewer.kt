package org.helllabs.android.xmp.compose.ui.player.viewer

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import org.helllabs.android.xmp.compose.theme.toPx
import org.helllabs.android.xmp.compose.ui.player.Util
import org.helllabs.android.xmp.service.PlayerService
import timber.log.Timber

private const val MAX_NOTES = 120
private val NOTES = arrayOf("C ", "C#", "D ", "D#", "E ", "F ", "F#", "G ", "G#", "A ", "A#", "B ")

val allNotes = (0 until MAX_NOTES).map {
    NOTES[it % 12] + it / 12
}

private val headerRectCorner = CornerRadius(16f, 16f)

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

    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis {
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
            }
        }
    }

    var canvasSize by remember {
        mutableStateOf(Size.Zero)
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
    var effectsTable = remember {
        mutableMapOf<Int, String>()
    }

    val chn by remember(modVars[3]) {
        mutableIntStateOf(modVars[3])
    }

    val offsetX = remember {
        Animatable(0f)
    }

    // TODO X-Axis scrolling
    val scrollState = rememberScrollableState { delta ->
        scope.launch {
            val totalContentWidth = with(density) { 22.dp.toPx() * chn }
            val maxOffset = (totalContentWidth - canvasSize.width).coerceAtLeast(0f)
            val newOffset = (offsetX.value + delta).coerceIn(-maxOffset, 0f)
            offsetX.snapTo(newOffset)
        }
        delta
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

        // Header Text Background
        val headerRect = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(
                        offset = Offset(0f, 0f),
                        size = Size(size.width, 24.dp.toPx())
                    ),
                    bottomLeft = headerRectCorner,
                    bottomRight = headerRectCorner
                )
            )
        }
        drawPath(headerRect, accent)

        // Header Text Numbers
        for (i in 0 until chn) {
            val channelNumber = i + 1
            val headerText = textMeasurer.measure(
                text = AnnotatedString(channelNumber.toString()),
                density = density,
                style = TextStyle(
                    background = if (view.isInEditMode) Color.Green else Color.Unspecified,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            )

            val textDivision = 22.dp.toPx() + (i * 3 * 22.dp.toPx()) + (22.dp.toPx() * 1.5f)
            val textCenterX = offsetX.value + (textDivision - (headerText.size.width / 2))
            val textCenterY = (24.dp.toPx() / 2) - (headerText.size.height / 2)

            // TODO culling
            drawText(
                textLayoutResult = headerText,
                topLeft = Offset(textCenterX, textCenterY)
            )
        }

        // Line Bar
        val barLineY = 24.dp.toPx() * 17
        drawRect(
            color = Color.DarkGray,
            topLeft = Offset(0f, barLineY),
            size = Size(size.width, 24.dp.toPx())
        )

        val currentRow = viewInfo.values[2].toFloat()
        val numRows = viewInfo.values[3]
        val rowHeight = 24.dp.toPx()
        val currentRowYOffset = barLineY - (currentRow * rowHeight)

        // Row numbers
        for (i in 0 until numRows) {
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
            val textCenterY =
                currentRowYOffset + (i * rowHeight) + (rowHeight / 2) - (rowText.size.height / 2)

            // Top culling || Bottom Culling
            if (textCenterY < 24.dp.toPx() || textCenterY + rowText.size.height > size.height) {
                continue
            }

            drawText(
                textLayoutResult = rowText,
                color = Color.White,
                topLeft = Offset(offsetX.value + textCenterX, textCenterY)
            )

            // TODO it seems `patternInfo` is not getting current data?
            for (j in 0 until chn) {
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
                val noteDivision = (j * 3 + 1) * 22.dp.toPx()
                val noteCenterX = offsetX.value + noteDivision
                drawText(
                    textLayoutResult = noteText,
                    topLeft = Offset(noteCenterX, textCenterY)
                )

                // Instruments
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
                val instDivision = (j * 3 + 1) * 47.dp.toPx()
                val instCenterX = offsetX.value + instDivision
                drawText(
                    textLayoutResult = instText,
                    topLeft = Offset(instCenterX, textCenterY)
                )

                // Effects
                if (currentType != viewInfo.type) {
                    currentType = viewInfo.type
                    effectsTable = Effects.getEffectList(viewInfo.type)
                    Timber.d("Refreshing effects list $currentType")
                }
                val effectType = effectsTable[patternInfo.rowFxType[j]]
                val effect: String = when {
                    patternInfo.rowFxType[j] > -1 ->
                        if (effectType != null) {
                            effectType
                        } else {
                            Timber.w("Unknown Effect: $currentType | ${patternInfo.rowFxType[j]}")
                            "?"
                        }

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
                val fxDivision = (j * 3 + 1) * 63.dp.toPx()
                val fxCenterX = offsetX.value + fxDivision
                drawText(
                    textLayoutResult = fxText,
                    topLeft = Offset(fxCenterX, textCenterY)
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
                val fxParmDivision = (j * 3 + 1) * 71.dp.toPx()
                val fxParmCenterX = offsetX.value + fxParmDivision
                drawText(
                    textLayoutResult = fxParmText,
                    topLeft = Offset(fxParmCenterX, textCenterY)
                )
            }
        }

        if (view.isInEditMode) {
            debugScreen(
                textMeasurer = textMeasurer,
                xValue = 22.dp.toPx()
            )
            debugPatternViewColumns()
        }

        if (BuildConfig.DEBUG) {
            drawText(
                textLayoutResult = textMeasurer.measure(
                    text = AnnotatedString(
                        "Row: ${viewInfo.values[2]} / Rows: ${viewInfo.values[3]}"
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
