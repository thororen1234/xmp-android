package org.helllabs.android.xmp.compose.theme

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.*
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

// Accent the "Xmp" part of the text, if we're on the main screen.
@Composable
fun themedText(text: String): AnnotatedString {
    return buildAnnotatedString {
        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
            append(text.substring(0, 3))
        }

        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onBackground)) {
            append(text.substring(3, text.length))
        }
    }
}

@Composable
fun XmpTheme(
    useDarkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = rememberDynamicColorScheme(
        seedColor = seed,
        isDark = useDarkTheme,
        style = PaletteStyle.TonalSpot,
        isExtendedFidelity = true
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
