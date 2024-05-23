package org.helllabs.android.xmp.compose.theme

import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

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
