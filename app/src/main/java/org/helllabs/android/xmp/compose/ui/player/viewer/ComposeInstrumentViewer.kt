package org.helllabs.android.xmp.compose.ui.player.viewer

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.theme.seed
import org.helllabs.android.xmp.model.ChannelInfo
import org.helllabs.android.xmp.model.ModVars

private const val VOLUME_STEPS = 32
private val barShape = CornerRadius(8f, 8f)

@Composable
internal fun InstrumentViewer(
    onTap: () -> Unit,
    channelInfo: ChannelInfo,
    isMuted: BooleanArray,
    modVars: ModVars,
    insName: Array<String>
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()
    val view = LocalView.current

    val textColor by remember {
        val list = (0..VOLUME_STEPS).map {
            val fraction = it.coerceIn(0, VOLUME_STEPS) / VOLUME_STEPS.toFloat()
            lerp(Color.Gray, Color.White, fraction)
        }
        mutableStateOf(list)
    }
    val measuredText by remember(modVars.numInstruments, insName) {
        val list = (0 until modVars.numInstruments).map {
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
    val chn by remember(modVars.numChannels) {
        mutableIntStateOf(modVars.numChannels)
    }
    val ins by remember(modVars.numInstruments) {
        mutableIntStateOf(modVars.numInstruments)
    }
    val yOffset = remember {
        Animatable(0f)
    }
    val canvasSize = remember {
        mutableStateOf(Size.Zero)
    }
    val scrollState = rememberScrollableState { delta ->
        scope.launch {
            val totalContentHeight = with(density) { 24.dp.toPx() * ins }
            val maxOffset = (totalContentHeight - canvasSize.value.height).coerceAtLeast(0f)
            val newOffset = (yOffset.value + delta).coerceIn(-maxOffset, 0f)
            yOffset.snapTo(newOffset)
        }
        delta
    }

    LaunchedEffect(modVars.numInstruments, insName) {
        // Scroll to the top on song change
        scope.launch {
            yOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 300)
            )
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .scrollable(
                orientation = Orientation.Vertical,
                state = scrollState
            )
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTap() })
            }
    ) {
        if (canvasSize.value != size) {
            canvasSize.value = size
        }

        var maxVol: Int
        var yPos: Float
        var vol: Int

        var totalPadding: Float
        var availableWidth: Float
        var boxWidth: Float
        var start: Float

        for (i in 0 until ins) {
            maxVol = 0
            yPos = yOffset.value + (24.dp.toPx() * i)

            // Top Culling || Bottom Culling
            if (yPos < -measuredText[0].size.height || yPos > size.height) {
                // Timber.d(String.format("%s %02X", "Culling: ", i+1))
                continue
            }

            for (j in 0 until chn) {
                if (isMuted[j]) {
                    continue
                }

                if (i == channelInfo.instruments[j]) {
                    vol = (channelInfo.volumes[j] / 2).coerceAtMost(VOLUME_STEPS)

                    totalPadding = (chn - 1) * 2.dp.toPx()
                    availableWidth = size.width - totalPadding
                    boxWidth = availableWidth / modVars.numChannels
                    start = j * (boxWidth + 2.dp.toPx())

                    if (vol > maxVol) {
                        maxVol = vol
                    }

                    drawRoundRect(
                        color = seed,
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

@Preview(device = "id:Nexus One")
@Preview(device = "id:pixel_xl")
@Preview(device = "id:pixel_4a")
@Preview(device = "id:pixel_8_pro")
@Composable
private fun Preview_InstrumentViewer() {
    val modVars = composeSampleModVars()
    XmpTheme(useDarkTheme = true) {
        InstrumentViewer(
            onTap = {},
            channelInfo = composeSampleChannelInfo(),
            isMuted = BooleanArray(modVars.numChannels) { false },
            modVars = modVars,
            insName = Array(modVars.numInstruments) {
                String.format("%02X %s", it + 1, "Instrument Name")
            }
        )
    }
}
