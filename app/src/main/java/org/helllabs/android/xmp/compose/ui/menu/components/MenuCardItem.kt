package org.helllabs.android.xmp.compose.ui.menu.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.model.FileItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MenuCardItem(
    item: FileItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        ListItem(
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            leadingContent = {
                Icon(
                    imageVector = if (item.isSpecial) {
                        Icons.Default.Folder
                    } else {
                        Icons.AutoMirrored.Filled.List
                    },
                    contentDescription = null
                )
            },
            headlineContent = { Text(text = item.name) },
            supportingContent = { Text(text = item.comment) }
        )
    }
}

@Preview
@Composable
private fun Preview_MenuCardItem() {
    XmpTheme(useDarkTheme = true) {
        MenuCardItem(
            item = FileItem(
                name = "Menu Card Item",
                comment = "Menu Card Comment",
                docFile = null
            ),
            onClick = { },
            onLongClick = { }
        )
    }
}
