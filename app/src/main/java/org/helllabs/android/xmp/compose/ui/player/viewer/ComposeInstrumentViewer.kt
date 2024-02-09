package org.helllabs.android.xmp.compose.ui.player.viewer

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.theme.accent
import org.helllabs.android.xmp.compose.theme.toPx

private const val VOLUME_STEPS = 32
private val barShape = CornerRadius(8f, 8f)

@Composable
internal fun InstrumentViewer(
    viewInfo: Viewer.Info,
    isMuted: BooleanArray,
    modVars: IntArray,
    insName: Array<String>
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val view = LocalView.current

    val textColor by remember {
        val list = (0..VOLUME_STEPS).map {
            val fraction = it.coerceIn(0, VOLUME_STEPS) / VOLUME_STEPS.toFloat()
            lerp(Color.Gray, Color.White, fraction)
        }
        mutableStateOf(list)
    }
    val measuredText by remember(modVars[4], insName) {
        val list = (0 until modVars[4]).map {
            textMeasurer.measure(
                text = AnnotatedString(insName[it]),
                density = density,
                style = TextStyle(
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    platformStyle = PlatformTextStyle(
                        includeFontPadding = true
                    )
                )
            )
        }
        mutableStateOf(list)
    }
    val chn by remember(modVars[3]) {
        mutableIntStateOf(modVars[3])
    }
    val ins by remember(modVars[4]) {
        mutableIntStateOf(modVars[4])
    }
    var yOffset by remember {
        mutableFloatStateOf(0f)
    }
    var canvasSize by remember {
        mutableStateOf(Size.Zero)
    }
    val scrollState = rememberScrollableState { delta ->
        val totalContentHeight = with(density) { 24.dp.toPx() * ins }
        val maxOffset = (totalContentHeight - canvasSize.height).coerceAtLeast(0f)
        val newOffset = (yOffset + delta).coerceIn(-maxOffset, 0f)
        yOffset = newOffset
        delta
    }

    LaunchedEffect(modVars[4], insName) {
        // Scroll to the top on song change
        // TODO
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .scrollable(
                orientation = Orientation.Vertical,
                state = scrollState,
                enabled = true
            )
    ) {
        if (canvasSize != size) {
            canvasSize = size
        }

        for (i in 0 until ins) {
            var maxVol = 0
            val yPos = yOffset + (24.dp.toPx() * i)

            // Timber.d(String.format("%s %02X", "Processing: ", i + 1))

            // TODO culling methods aren't perfect.
            // Top Culling || Bottom Culling
            if (yPos < -measuredText[0].size.height || yPos > size.height) {
                // Timber.d(String.format("%s %02X", "Culling: ", i+1))
                continue
            }

            for (j in 0 until chn) {
                var vol: Int

                if (isMuted[j]) {
                    continue
                }

                if (i == viewInfo.instruments[j]) {
                    vol = (viewInfo.volumes[j] / 2).coerceAtMost(VOLUME_STEPS)

                    val totalPadding = (chn - 1) * 2.dp.toPx()
                    val availableWidth = size.width - totalPadding
                    val boxWidth = availableWidth / modVars[3]
                    val start = j * (boxWidth + 2.dp.toPx())

                    if (vol > maxVol) {
                        maxVol = vol
                    }

                    drawRoundRect(
                        color = accent,
                        cornerRadius = barShape,
                        alpha = vol / VOLUME_STEPS.toFloat(),
                        topLeft = Offset(start, yPos),
                        size = Size(boxWidth, 24.dp.toPx())
                    )
                }
            }

            drawText(
                color = textColor[maxVol],
                textLayoutResult = measuredText[i],
                topLeft = Offset(0f, yPos)
            )
        }

        if (view.isInEditMode) {
            debugScreen(textMeasurer = textMeasurer)
        }
    }
}

private fun DrawScope.debugScreen(textMeasurer: TextMeasurer) {
    val xValue = 24.dp.toPx()
    val yValue = 24.dp.toPx()
    for (i in 0 until (size.width / xValue).toInt()) {
        val xPosition = i * xValue
        drawRect(
            color = Color.Green.copy(alpha = .12f),
            topLeft = Offset(xPosition, 0f),
            size = Size(1f, size.height)
        )
        val text = textMeasurer.measure(
            text = AnnotatedString(i.toString()),
            style = TextStyle(
                color = Color.White,
                fontSize = 5.sp,
                fontFamily = FontFamily.Monospace
            )

        )
        drawText(textLayoutResult = text, topLeft = Offset(xPosition, 0f))
    }
    for (i in 0 until (size.height / yValue).toInt()) {
        val yPosition = i * yValue
        val text = textMeasurer.measure(
            text = AnnotatedString(i.toString()),
            style = TextStyle(
                color = Color.White,
                fontSize = 5.sp,
                fontFamily = FontFamily.Monospace
            )

        )
        drawText(textLayoutResult = text, topLeft = Offset(0f, yPosition))
        drawRect(
            color = Color.Yellow.copy(alpha = .12f),
            topLeft = Offset(0f, yPosition),
            size = Size(size.width, 1f)
        )
    }
}

@Preview(device = "id:Nexus One")
@Composable
private fun Preview_InstrumentViewer() {
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
            currentViewer = 0,
            viewInfo = viewInfo,
            isMuted = BooleanArray(modVars[3]) { false },
            modVars = modVars,
            insName = Array(modVars[4]) { String.format("%02X %s", it + 1, "Instrument Name") }
        )
    }
}