package org.helllabs.android.xmp.compose.ui.player.viewer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.theme.accent

@Composable
internal fun InstrumentViewer(
    viewInfo: Viewer.Info,
    isMuted: BooleanArray,
    modVars: IntArray,
    insName: Array<String>,
) {
    val density = LocalDensity.current
    val verticalScroll = rememberScrollState()
    val textMeasurer = rememberTextMeasurer()

    val paddingPx by remember {
        mutableFloatStateOf(with(density) { 2.dp.toPx() })
    }
    val chn by remember(modVars[3]) {
        mutableIntStateOf(modVars[3])
    }
    val ins by remember(modVars[4]) {
        mutableIntStateOf(modVars[4])
    }
    var vol by remember {
        mutableIntStateOf(0)
    }
    var textFraction by remember {
        mutableFloatStateOf(0f)
    }
    var textColor by remember {
        mutableStateOf(Color.Gray)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(verticalScroll)
    ) {
        for (i in 0 until ins) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .drawWithCache {
                        val measuredText = textMeasurer.measure(
                            text = AnnotatedString(insName[i]),
                            constraints = Constraints.fixedWidth(size.width.toInt()),
                            style = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Normal,
                                textAlign = TextAlign.Start,
                                fontFamily = FontFamily.Monospace
                            )
                        )

                        onDrawBehind {
                            vol = 0 // Reset volume on each draw

                            for (j in 0 until chn) {
                                if (isMuted[j]) {
                                    continue
                                }
                                if (viewInfo.instruments[j] == i) {
                                    val totalPadding = (chn - 1) * paddingPx
                                    val availableWidth = size.width - totalPadding
                                    val boxWidth = availableWidth / modVars[3]
                                    val start = j * (boxWidth + paddingPx)

                                    // Clamp volume
                                    vol = (viewInfo.volumes[j] / 2).coerceAtMost(32)

                                    drawRect(
                                        color = accent,
                                        alpha = vol / 32f,
                                        topLeft = Offset(start, 0f),
                                        size = Size(boxWidth, measuredText.size.height.toFloat())
                                    )
                                }
                            }

                            textFraction = vol.coerceIn(0, 32) / 32f
                            textColor = lerp(Color.Gray, Color.White, textFraction)
                            drawText(textLayoutResult = measuredText, color = textColor)
                        }
                    }
            )
        }
    }
}

@Preview
@Composable
private fun Preview_ComposeViewer() {
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

    val viewInfo by remember {
        mutableStateOf(info)
    }
    val currentViewer by remember {
        mutableIntStateOf(0)
    }
    val modVars by remember {
        mutableStateOf(intArrayOf(190968, 30, 25, 12, 24, 18, 1, 0, 0, 0))
    }

    XmpTheme(useDarkTheme = true) {
        XmpCanvas(
            onSizeChanged = { _, _ -> },
            onChangeViewer = {},
            currentViewer = currentViewer,
            viewInfo = viewInfo,
            isMuted = BooleanArray(modVars[3]) { false },
            modVars = modVars,
            insName = Array(modVars[4]) { String.format("%02X %s", it + 1, "Instument Name") },
        )
    }
}
