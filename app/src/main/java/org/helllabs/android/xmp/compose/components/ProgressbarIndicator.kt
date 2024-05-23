package org.helllabs.android.xmp.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.helllabs.android.xmp.compose.theme.XmpTheme

@Composable
fun ProgressbarIndicator(isLoading: Boolean = true) {
    if (isLoading) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 8.dp
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(64.dp)
                    .scale(2f)
            )
        }
    }
}

@Preview
@Composable
private fun ProgressbarIndicatorPreview() {
    XmpTheme(useDarkTheme = true) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            content = {
                ProgressbarIndicator(isLoading = true)
            }
        )
    }
}
