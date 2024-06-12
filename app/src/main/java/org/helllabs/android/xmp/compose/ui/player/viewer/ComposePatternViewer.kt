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
import org.helllabs.android.xmp.compose.ui.player.PlayerActivity
import org.helllabs.android.xmp.compose.ui.player.Util
import org.helllabs.android.xmp.model.FrameInfo
import org.helllabs.android.xmp.model.ModVars
import org.helllabs.android.xmp.service.PlayerService
import timber.log.Timber

// Maybe keep the row numbers in view at all times, and move the channel columns instead?

// TODO: Lag when there are many channels, is it culling right?

@Composable
internal fun ComposePatternViewer(
    onTap: () -> Unit,
    modType: String,
    fi: FrameInfo,
    isMuted: BooleanArray,
    modVars: ModVars
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer(0)
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

    val rowTextSize = remember { 14.sp }

    var currentType by remember { mutableStateOf("") }
    val effectsTable = remember(modType) {
        val type = Effects.getEffectList(modType)
        currentType = type.name
        type.table
    }

    val numRows = remember(fi.numRows) {
        fi.numRows
    }

    val rowText = remember(numRows) {
        (0..numRows).map {
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
    }

    val chn = remember(modVars.numChannels) {
        modVars.numChannels
    }

    val offsetX = remember {
        Animatable(0f)
    }

    val headerText = remember(chn) {
        (0 until chn).map {
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

    val rowFxParm = remember { ByteArray(64) }
    val rowFxType = remember { ByteArray(64) }
    val rowInsts = remember { ByteArray(64) }
    val rowNotes = remember { ByteArray(64) }

    LaunchedEffect(chn) {
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
            color = seed,
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

        currentRow = fi.row.toFloat()
        rowYOffset = barLineY - (currentRow * yAxisMultiplier)

        for (i in 0 until numRows) {
            for (j in 0 until chn) {
                // Be very careful here!
                // Our variables are latency-compensated but pattern data is current
                // so caution is needed to avoid retrieving data using old variables
                // from a module with pattern data from a newly loaded one.
                if (PlayerService.isAlive.value && PlayerActivity.canChangeViewer) {
                    Xmp.getPatternRow(fi.pattern, i, rowNotes, rowInsts, rowFxType, rowFxParm)
                }

                val info = textMeasurer.measure(
                    text = buildAnnotatedString {
                        /****** Notes *****/
                        withStyle(
                            style = SpanStyle(
                                color = if (isMuted[j]) Color.LightGray else Color(140, 140, 160)
                            ),
                            block = {
                                append(Util.note(rowNotes[j].toInt()))
                            }
                        )
                        /***** Instruments *****/
                        withStyle(
                            style = SpanStyle(
                                color = if (isMuted[j]) Color(80, 40, 40) else Color(160, 80, 80)
                            ),
                            block = {
                                append(Util.num(rowInsts[j].toInt()))
                            }
                        )
                        /***** Effects *****/
                        withStyle(
                            style = SpanStyle(
                                color = if (isMuted[j]) Color(16, 75, 28) else Color(34, 158, 60)
                            )
                        ) {
                            val fxt = rowFxType[j]
                            val fx = if (fxt < 0) {
                                "-"
                            } else {
                                effectsTable.getOrElse(fxt) {
                                    Timber.w(
                                        "Unknown FX: $fxt in chn ${j + 1}, row $i, " +
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
                                color = if (isMuted[j]) Color(16, 75, 28) else Color(34, 158, 60)
                            ),
                            block = {
                                append(Util.num(rowFxParm[j].toInt()))
                            }
                        )
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
    val modVars = remember {
        ModVars(190968, 30, 25, 12, 40, 18, 1, 0)
    }
    XmpTheme(useDarkTheme = true) {
        ComposePatternViewer(
            onTap = { },
            modType = "FastTracker v2.00 XM 1.04",
            fi = composeFrameInfoSampleData(),
            isMuted = BooleanArray(modVars.numChannels) { false },
            modVars = modVars
        )
    }
}
