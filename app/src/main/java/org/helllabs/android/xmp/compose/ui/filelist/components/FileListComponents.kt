package org.helllabs.android.xmp.compose.ui.filelist.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.saket.cascade.CascadeDropdownMenu
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.XmpDropdownMenuHeader
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.filelist.FileListViewModel
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.model.DropDownItem
import org.helllabs.android.xmp.model.DropDownSelection
import org.helllabs.android.xmp.model.FileItem

private val crumbDropDownItems = listOf(
    DropDownItem("Add to playlist", DropDownSelection.DIR_ADD_TO_PLAYLIST),
    DropDownItem("Add to play queue", DropDownSelection.DIR_ADD_TO_QUEUE),
    DropDownItem("Play contents", DropDownSelection.DIR_PLAY_CONTENTS),
)

private val directoryDropDownItems = listOf(
    DropDownItem("Add to playlist", DropDownSelection.DIR_ADD_TO_PLAYLIST),
    DropDownItem("Add to play queue", DropDownSelection.DIR_ADD_TO_QUEUE),
    DropDownItem("Play contents", DropDownSelection.DIR_PLAY_CONTENTS),
    DropDownItem("Delete directory", DropDownSelection.DELETE)
)

private val fileDropDownItems: List<DropDownItem> = listOf(
    DropDownItem("Add to playlist", DropDownSelection.FILE_ADD_TO_PLAYLIST),
    DropDownItem("Delete file", DropDownSelection.DELETE),
    DropDownItem("Add to play queue", DropDownSelection.FILE_ADD_TO_QUEUE),
    DropDownItem("Play this file", DropDownSelection.FILE_PLAY_THIS_ONLY),
    DropDownItem("Play all starting here", DropDownSelection.FILE_PLAY_HERE),
)

@Composable
fun FileListCard(
    item: FileItem,
    onItemClick: () -> Unit,
    onItemLongClick: (DropDownSelection) -> Unit
) {
    var isContextMenuVisible by rememberSaveable { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        enabled = item.isValid,
        onClick = onItemClick
    ) {
        ListItem(
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth(),
            leadingContent = {
                val icon = if (item.isFile) {
                    Icons.AutoMirrored.Filled.InsertDriveFile
                } else {
                    Icons.Default.Folder
                }

                Icon(imageVector = icon, contentDescription = null)
            },
            headlineContent = {
                Text(
                    text = item.name,
                    textDecoration = if (item.isValid) TextDecoration.None else TextDecoration.LineThrough
                )
            },
            trailingContent = {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isContextMenuVisible = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = null
                    )
                }

                val list = if (item.isFile) fileDropDownItems else directoryDropDownItems
                CascadeDropdownMenu(
                    expanded = isContextMenuVisible,
                    onDismissRequest = { isContextMenuVisible = false }
                ) {
                    XmpDropdownMenuHeader(
                        text = if (item.isFile) "This File" else "This Directory"
                    )

                    list.forEach {
                        DropdownMenuItem(
                            text = { Text(text = it.text) },
                            enabled = if (it.selection == DropDownSelection.DELETE) {
                                true
                            } else {
                                item.isValid
                            },
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onItemLongClick(it.selection)
                                isContextMenuVisible = false
                            }
                        )
                    }
                }
            },
            supportingContent = {
                Text(
                    text = if (!item.isFile) {
                        stringResource(id = R.string.directory)
                    } else {
                        item.comment
                    },
                    fontStyle = FontStyle.Italic
                )
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BreadCrumbs(
    modifier: Modifier = Modifier,
    crumbScrollState: LazyListState,
    crumbs: List<FileListViewModel.BreadCrumb>,
    onCrumbMenu: (DropDownSelection) -> Unit,
    onCrumbClick: (FileListViewModel.BreadCrumb, Int) -> Unit
) {
    Row(modifier = modifier.fillMaxWidth()) {
        LazyRow(
            state = crumbScrollState,
            contentPadding = PaddingValues(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            stickyHeader {
                var isContextMenuVisible by rememberSaveable { mutableStateOf(false) }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(
                        topEnd = 16.dp,
                        bottomEnd = 16.dp
                    )
                ) {
                    val haptic = LocalHapticFeedback.current
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isContextMenuVisible = true
                        }
                    ) {
                        Icon(imageVector = Icons.Default.MoreHoriz, contentDescription = null)

                        CascadeDropdownMenu(
                            expanded = isContextMenuVisible,
                            onDismissRequest = { isContextMenuVisible = false }
                        ) {
                            XmpDropdownMenuHeader(text = "All Files")
                            crumbDropDownItems.forEach {
                                DropdownMenuItem(
                                    text = { Text(text = it.text) },
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onCrumbMenu(it.selection)
                                        isContextMenuVisible = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            itemsIndexed(crumbs) { index, item ->
                BreadCrumbChip(
                    enabled = item.enabled,
                    onClick = { onCrumbClick(item, index) },
                    label = item.name
                )
            }
        }
    }
}

@Composable
private fun BreadCrumbChip(
    enabled: Boolean,
    onClick: () -> Unit,
    label: String
) {
    AssistChip(
        modifier = Modifier.padding(horizontal = 3.dp),
        enabled = enabled,
        onClick = onClick,
        label = { Text(text = label) },
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null
            )
        }
    )
}

@Preview
@Composable
private fun Preview_FileListCard() {
    PrefManager.init(LocalContext.current)
    XmpTheme(useDarkTheme = true) {
        FileListCard(
            item = FileItem(
                name = "File List Card",
                comment = "File List Comment",
                docFile = null
            ),
            onItemClick = { },
            onItemLongClick = { }
        )
    }
}

@Preview
@Composable
private fun Preview_BreadCrumbs() {
    XmpTheme(useDarkTheme = true) {
        val state = rememberLazyListState()
        BreadCrumbs(
            crumbScrollState = state,
            crumbs = listOf(
                FileListViewModel.BreadCrumb("Bread Crumb 1", null),
                FileListViewModel.BreadCrumb("Bread Crumb 2", null),
                FileListViewModel.BreadCrumb("Bread Crumb 3", null)
            ),
            onCrumbMenu = { },
            onCrumbClick = { _, _ -> }
        )
    }
}

@Preview
@Composable
private fun Preview_BreadCrumbChip() {
    XmpTheme(useDarkTheme = true) {
        BreadCrumbChip(enabled = true, onClick = { }, label = "Bread Crumb Chip")
    }
}
