package org.helllabs.android.xmp.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun XmpDropdownMenuHeader(
    text: String,
    color: Color = MaterialTheme.colorScheme.primary
) {
    XmpDropdownMenuHeader {
        Text(text = text, color = color)
    }
}

/**
 * Pretty much just a title header for drop down menu's
 *
 * Sampled from:
 * https://github.com/saket/cascade/blob/f3840d7ec5d4ce5fa3edf0c0d930cfd08107b456/cascade-compose/src/main/java/me/saket/cascade/Cascade.kt#L353
 */
@Composable
private fun XmpDropdownMenuHeader(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(vertical = 4.dp),
    text: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val headerColor = LocalContentColor.current.copy(alpha = 0.6f)
        val headerStyle =
            MaterialTheme.typography.labelLarge.run { // labelLarge is also used by DropdownMenuItem().
                copy(
                    fontSize = fontSize * 0.9f,
                    letterSpacing = letterSpacing * 0.9f
                )
            }
        CompositionLocalProvider(
            LocalContentColor provides headerColor,
            LocalTextStyle provides headerStyle
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp, top = 16.dp)
            ) {
                text()
            }
        }
    }
}
