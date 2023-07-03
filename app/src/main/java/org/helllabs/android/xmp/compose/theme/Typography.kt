package org.helllabs.android.xmp.compose.theme

import android.content.Context
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import org.helllabs.android.xmp.R

val michromaFontFamily = FontFamily(Font(R.font.font_michroma))
val topazFontFamily = FontFamily(Font(R.font.font_topaz_plus_a500))

val channelViewFontSize = 12.sp
val channelViewChannelFontSize = 14.sp
val patternViewFontSize = 14.sp
val instrumentViewFontSize = 14.sp

fun TextUnit.toPx(context: Context): Float {
    val unitValue = this.value
    val scale = context.resources.displayMetrics.scaledDensity
    return unitValue * scale
}
