package org.helllabs.android.xmp.compose.ui.player.viewer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.theme.accent

private const val VOLUME_STEPS = 32
private val barShape = CornerRadius(8f, 8f)

@Composable
internal fun InstrumentViewer(
    viewInfo: Viewer.Info,
    isMuted: BooleanArray,
    modVars: IntArray,
    insName: Array<String>,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()
    val verticalScroll = rememberScrollState()
    val view = LocalView.current

    LaunchedEffect(modVars[4], insName) {
        // Scroll to the top on song change
        scope.launch { verticalScroll.scrollTo(0) }
    }

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
    val canvasHeight by remember(ins) {
        val singleRowHeightPx = with(density) { 24.dp.toPx() }
        val totalHeightPx = singleRowHeightPx * ins
        mutableStateOf(with(density) { totalHeightPx.toDp() })
    }

    Box(
        modifier = Modifier
            .verticalScroll(verticalScroll)
            .border(1.dp, Color.Magenta)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .height(canvasHeight)
        ) {
            for (i in 0 until ins) {
                var maxVol = 0
                val textHeight = measuredText[0].size.height

                if (!view.isInEditMode) {
                    // Top Culling
                    if (verticalScroll.value.div(textHeight) > i) {
                        // Timber.d("TOP: Culling Channel ${String.format("%02X", i+1)}")
                        continue
                    }

                    // TODO Bottom Culling
//                    if ((textHeight.times(i) + paddingValues.calculateTopPadding().value) > displayMetrics.heightPixels) {
//                        Timber.d("BOTTOM: Culling Channel ${String.format("%02X", i)}")
//                        continue
//                    }
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
                            topLeft = Offset(start, 24.dp.toPx() * i),
                            size = Size(boxWidth, 24.dp.toPx())
                        )
                    }
                }

                drawText(
                    color = textColor[maxVol],
                    textLayoutResult = measuredText[i],
                    topLeft = Offset(0f, 24.dp.toPx() * i),
                )
            }

            // PLACEHOLDER LINES
            if (view.isInEditMode) {
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
        }
    }
}

@Preview(device = "id:Nexus One")
@Composable
private fun Preview_InstrumentViewer() {
    val info = remember {
        Viewer.Info().apply {
            time = 109
            values = intArrayOf(16, 12, 8, 64, 0, 7, 134)
            volumes = intArrayOf(
                64, 17, 32, 48, 64, 19, 53, 15, 0, 7, 39, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
            )
            finalVols = intArrayOf(
                64, 16, 32, 48, 64, 19, 3, 15, 0, 26, 16, 22, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
            )
            pans = intArrayOf(
                128, 128, 135, 128, 112, 112, 128, 160, 208, 148, 200, 128, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
            )
            instruments = intArrayOf(
                1, 1, 3, 14, 10, 12, 17, 11, 18, 20, 20, 15, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
            )
            keys = intArrayOf(
                72, 69, 67, 72, 72, 72, 77, 77, -1, -1, 74, -1, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
            )
            periods = intArrayOf(
                3424, 4071, 4570, 1298, 1227, 3424, 1225, 2565, 6848, 762, 762,
                3424, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
            )
            type = "FastTracker v2.00 XM 1.04"
        }
    }

    val viewInfo by remember { mutableStateOf(info) }
    val modVars by remember { mutableStateOf(intArrayOf(190968, 30, 25, 12, 40, 18, 1, 0, 0, 0)) }

    XmpTheme(useDarkTheme = true) {
        XmpCanvas(
            onChangeViewer = {},
            currentViewer = 0,
            viewInfo = viewInfo,
            isMuted = BooleanArray(modVars[3]) { false },
            modVars = modVars,
            insName = Array(modVars[4]) { String.format("%02X %s", it + 1, "Instrument Name") },
        )
    }
}
