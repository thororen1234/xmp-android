package org.helllabs.android.xmp.compose.components

import android.widget.RadioButton
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * RadioButton with Text
 */
@Composable
fun RadioButtonItem(
    index: Int,
    selection: Int,
    text: String,
    radioButtonColors: RadioButtonColors = RadioButtonDefaults.colors(),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .selectable(
                selected = (index == selection),
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            colors = radioButtonColors,
            selected = (index == selection),
            onClick = null
        )
        Text(
            modifier = Modifier.padding(start = 16.dp),
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
