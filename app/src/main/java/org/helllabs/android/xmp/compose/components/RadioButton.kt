package org.helllabs.android.xmp.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.theapache64.rebugger.Rebugger
import org.helllabs.android.xmp.compose.theme.XmpTheme

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

    Rebugger(
        composableName = "RadioButtonItem",
        trackMap = mapOf(
            "index" to index,
            "selection" to selection,
            "text" to text,
            "radioButtonColors" to radioButtonColors,
            "onClick" to onClick,
            "Modifier" to Modifier.fillMaxWidth()
                .height(56.dp)
                .selectable(
                    selected = (index == selection),
                    onClick = onClick,
                    role = Role.RadioButton
                )
                .padding(horizontal = 16.dp),
            "Alignment.CenterVertically" to Alignment.CenterVertically,
        ),
    )
}

@Preview
@Composable
private fun Preview_RadioButtonItem() {
    XmpTheme(useDarkTheme = true) {
        Surface {
            Column {
                Array(3) {
                    RadioButtonItem(
                        index = 0,
                        selection = 1,
                        text = "Radio Button $it",
                        onClick = { }
                    )
                }
            }
        }
    }
}
