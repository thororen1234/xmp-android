package me.saket.extendedspans.internal

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.toArgb

fun Color?.serialize(): String = if (this == null || isUnspecified) "null" else "${toArgb()}"

fun String.deserializeToColor(): Color? = if (this == "null") null else Color(this.toInt())
