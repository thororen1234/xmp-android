package org.helllabs.android.xmp.compose.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun SegmentedButton(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    selectedIndex: Int,
    itemsList: List<String>,
    onClick: (index: Int) -> Unit
) {
    Row(modifier = modifier) {
        itemsList.forEachIndexed { index, item ->
            OutlinedButton(
                onClick = { onClick(index) },
                modifier = Modifier
                    .weight(1f)
                    .then(
                        when (index) {
                            0 ->
                                Modifier
                                    .offset(0.dp, 0.dp)
                                    .zIndex(if (selectedIndex == index) 1f else 0f)

                            else ->
                                Modifier
                                    .offset((-1 * index).dp, 0.dp)
                                    .zIndex(if (selectedIndex == index) 1f else 0f)
                        }
                    ),
                shape = when (index) {
                    0 -> RoundedCornerShape(
                        topStart = cornerRadius,
                        topEnd = 0.dp,
                        bottomStart = cornerRadius,
                        bottomEnd = 0.dp
                    )

                    itemsList.size - 1 -> RoundedCornerShape(
                        topStart = 0.dp,
                        topEnd = cornerRadius,
                        bottomStart = 0.dp,
                        bottomEnd = cornerRadius
                    )

                    else -> RoundedCornerShape(
                        topStart = 0.dp,
                        topEnd = 0.dp,
                        bottomStart = 0.dp,
                        bottomEnd = 0.dp
                    )
                },
                border = BorderStroke(
                    1.dp,
                    if (selectedIndex == index) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                    }
                ),
                colors = if (selectedIndex == index) {
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                } else {
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                Text(text = item)
            }
        }
    }
}
