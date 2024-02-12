package org.helllabs.android.xmp.compose.ui.player.viewer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.helllabs.android.xmp.BuildConfig

internal fun DrawScope.debugPatternViewColumns() {
    if (!BuildConfig.DEBUG) {
        throw Exception("This class shouldn't be used in non debug builds.")
    }
    for (i in 0 until (size.width / 24.dp.toPx()).toInt()) {
        val xPosition = (i * 3 + 1) * 22.dp.toPx()
        drawRect(
            color = (if (i % 2 == 0) Color.Magenta else Color.Cyan).copy(alpha = .15f),
            topLeft = Offset(xPosition, 0f),
            size = Size(66.dp.toPx(), size.height)
        )
    }
}

internal fun DrawScope.debugScreen(
    textMeasurer: TextMeasurer,
    xValue: Float = 24.dp.toPx(),
    yValue: Float = 24.dp.toPx()
) {
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

internal fun composePatternSampleData(): PatternInfo {
    if (!BuildConfig.DEBUG) {
        throw Exception("This class shouldn't be used in non debug builds.")
    }

    return PatternInfo(
        pat = 0,
        lineInPattern = 0,
        rowNotes = byteArrayOf(
            73, 0, 77, 80, 84, 73, 0, 0, 73, 73, 73, 0, 73, 0, 73, 73, 0, 77, 0, 77, 0, 77,
            0, 0, 77, 80, 84, 77, 80, 84, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        ),
        rowInsts = byteArrayOf(
            25, 0, 1, 1, 1, 25, 0, 0, 3, 3, 3, 0, 5, 0, 7, 7, 0, 9, 0, 9, 0, 10, 0, 0, 11,
            11, 11, 12, 12, 12, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        ),
        rowFxType = intArrayOf(
            -1, 15, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        ),
        rowFxParm = intArrayOf(
            -1, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        )
    )
}

internal fun composeViewerSampleData(): ViewerInfo {
    if (!BuildConfig.DEBUG) {
        throw Exception("This class shouldn't be used in non debug builds.")
    }
    return ViewerInfo(
        time = 109,
        values = intArrayOf(16, 12, 8, 64, 0, 7, 134),
        volumes = intArrayOf(
            64, 17, 32, 48, 64, 19, 53, 15, 0, 7, 39, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        ),
        finalVols = intArrayOf(
            64, 16, 32, 48, 64, 19, 3, 15, 0, 26, 16, 22, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        ),
        pans = intArrayOf(
            128, 128, 135, 128, 112, 112, 128, 160, 208, 148, 200, 128, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        ),
        instruments = intArrayOf(
            1, 1, 3, 14, 10, 12, 17, 11, 18, 20, 20, 15, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        ),
        keys = intArrayOf(
            72, 69, 67, 72, 72, 72, 77, 77, -1, -1, 74, -1, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        ),
        periods = intArrayOf(
            3424, 4071, 4570, 1298, 1227, 3424, 1225, 2565, 6848, 762, 762,
            3424, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        ),
        type = "FastTracker v2.00 XM 1.04"
    )
}
