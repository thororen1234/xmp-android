package org.helllabs.android.xmp.compose.ui.player.viewer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.helllabs.android.xmp.compose.theme.XmpTheme

// TODO wider/landscape support

@Composable
fun ComposeChannelViewer(
    viewInfo: ViewerInfo,
    isMuted: BooleanArray,
    modVars: IntArray,
    insName: Array<String>
) {
    val verticalScroll = rememberScrollState()

    val chn by remember(modVars[3]) {
        mutableIntStateOf(modVars[3])
    }
    val ins by remember(modVars[4]) {
        mutableIntStateOf(modVars[4])
    }
    val instrumentNames by remember(insName) {
        val list = insName.toMutableList().apply {
            if (size < ins) {
                addAll(List(ins - size) { "" })
            }
        }.ifEmpty { List(ins) { "" } }

        mutableStateOf(list)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(verticalScroll)
    ) {
    }
}

@Composable
private fun ChannelItem(
    channelNumber: String,
    channelName: String

) {
    val textMeasurer = rememberTextMeasurer()
    val view = LocalView.current

    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .drawWithCache {
                // Scope background
                val rectHeight = 66.dp
                val rectWidthPx = (rectHeight * (16f / 9f)).toPx()

                val endPadding = size.width / 8.dp.toPx()
                fun xOffset(value: Int): Float = value * 8.dp.toPx()
                fun yOffset(value: Int): Float = (size.height / 6) * value - 1 / 2

                val chnNumberText = textMeasurer.measure(
                    text = AnnotatedString(channelNumber),
                    style = TextStyle(
                        color = Color(200, 200, 200, 255),
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )
                val chnNameText = textMeasurer.measure(
                    text = AnnotatedString(channelName),
                    constraints = Constraints.fixedWidth((xOffset(48).toInt() - xOffset(12)).toInt()),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        color = Color(200, 200, 200, 255),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )

                onDrawBehind {
                    // Channel Number
                    drawText(
                        textLayoutResult = chnNumberText,
                        topLeft = Offset(
                            xOffset(2) - (chnNumberText.size.width / 2),
                            yOffset(3) - (chnNumberText.size.height / 2)
                        )
                    )

                    // Channel Scope Background
                    // TODO not wide enough?
                    drawRect(
                        color = Color(40, 40, 40, 255),
                        topLeft = Offset(
                            x = xOffset(4),
                            y = yOffset(1)
                        ),
                        size = Size(xOffset(4) * (16f / 9f), yOffset(4))
                    )

                    // Instrument Name
                    drawText(
                        textLayoutResult = chnNameText,
                        topLeft = Offset(
                            x = xOffset(12),
                            y = (size.height / 3) - (chnNameText.size.height / 2)
                        )
                    )

                    // Volume Bar
                    val volumeBarWidth = xOffset(29) - xOffset(12)
                    drawRect(
                        color = Color(40, 40, 40, 255),
                        topLeft = Offset(
                            x = xOffset(12),
                            y = (size.height / 6) * 4 - 8 / 2
                        ),
                        size = Size(volumeBarWidth, 12f)
                    )

                    // Pan Bar
                    val panBarWidth = xOffset(48) - xOffset(31)
                    drawRect(
                        color = Color(40, 40, 40, 255),
                        topLeft = Offset(
                            x = xOffset(31),
                            y = (size.height / 6) * 4 - 8 / 2
                        ),
                        size = Size(panBarWidth, 12f)
                    )

                    // PLACEHOLDER LINES
                    if (view.isInEditMode) {
                        val lineSpacing = 8.dp.toPx()
                        for (i in 0 until (size.width / lineSpacing).toInt()) {
                            val xPosition = i * lineSpacing
                            drawRect(
                                color = Color.Green.copy(alpha = .12f),
                                topLeft = Offset(xPosition, 0f),
                                size = Size(1f, size.height)
                            )
                            val text = textMeasurer.measure(
                                text = AnnotatedString(i.toString()),
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = 3.sp,
                                    fontFamily = FontFamily.Monospace
                                )

                            )
                            drawText(textLayoutResult = text, topLeft = Offset(xPosition, 0f))
                        }

                        val segmentHeight = size.height / 6
                        for (i in 1..5) {
                            val yPosition = segmentHeight * i - 1 / 2
                            drawRect(
                                color = Color.Yellow.copy(alpha = .12f),
                                topLeft = Offset(0f, yPosition),
                                size = Size(size.width, 1f)
                            )
                        }

                        // Should be even
                        if (volumeBarWidth != panBarWidth) {
                            throw IllegalAccessError("Bar widths not the same")
                        }
                    }
                }
            }
    )
}

@Preview
@Composable
private fun Preview_ChannelItem() {
    XmpTheme(useDarkTheme = true) {
        val name = "A Super Very Long Instrument Name Item That Should Eclipse"
        Surface {
            Column {
                ChannelItem("1", name)
                Spacer(modifier = Modifier.height(4.dp))
                ChannelItem("10", name)
                Spacer(modifier = Modifier.height(4.dp))
                ChannelItem("28", name)
                Spacer(modifier = Modifier.height(4.dp))
                ChannelItem("100", name)
            }
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
            currentViewer = 2,
            viewInfo = viewInfo,
            isMuted = BooleanArray(modVars[3]) { false },
            modVars = modVars,
            insName = Array(modVars[4]) { String.format("%02X %s", it + 1, "Instrument Name") }
        )
    }
}
