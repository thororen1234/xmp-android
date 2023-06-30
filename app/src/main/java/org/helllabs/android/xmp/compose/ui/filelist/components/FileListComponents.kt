package org.helllabs.android.xmp.compose.ui.filelist.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import me.saket.cascade.CascadeDropdownMenu
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.browser.playlist.PlaylistItem
import org.helllabs.android.xmp.compose.components.XmpDropdownMenuHeader
import org.helllabs.android.xmp.compose.ui.filelist.FileListViewModel
import org.helllabs.android.xmp.model.DropDownItem

private val crumbDropDownItems = listOf(
    DropDownItem("Add to playlist", 0),
    DropDownItem("Recursive add to playlist", 1),
    DropDownItem("Add to play queue", 2),
    DropDownItem("Set as default path", 3),
    DropDownItem("Clear cache", 4)
)

private val directoryDropDownItems = listOf(
    DropDownItem("Add to playlist", 0),
    DropDownItem("Add to play queue", 1),
    DropDownItem("Play contents", 2),
    DropDownItem("Delete directory", 3)
)

@Composable
fun FileListCard(
    item: PlaylistItem,
    onItemClick: () -> Unit,
    onItemLongClick: (Int) -> Unit
) {
    var isContextMenuVisible by rememberSaveable { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Card(modifier = Modifier.padding(6.dp)) {
        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onItemClick() },
            leadingContent = {
                val icon = when {
                    item.isDirectory -> Icons.Default.Folder
                    item.isFile -> Icons.Default.InsertDriveFile
                    else -> Icons.Default.QuestionMark
                }

                Icon(imageVector = icon, contentDescription = null)
            },
            headlineContent = {
                Text(text = item.name)
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

                val contextList =
                    if (item.isDirectory) {
                        directoryDropDownItems
                    } else {
                        val mode = PrefManager.playlistMode
                        mutableListOf(
                            DropDownItem("Add to playlist", 0),
                            DropDownItem("Delete file", 4)
                        ).also {
                            if (mode != 3) {
                                it.add(1, DropDownItem("Add to play queue", 1))
                            }
                            if (mode != 2) {
                                it.add(2, DropDownItem("Play this file", 2))
                            }
                            if (mode != 1) {
                                it.add(
                                    3,
                                    DropDownItem("Play all starting here", 3)
                                )
                            }
                        }
                    }
                CascadeDropdownMenu(
                    expanded = isContextMenuVisible,
                    onDismissRequest = { isContextMenuVisible = false }
                ) {
                    XmpDropdownMenuHeader(
                        text = if (item.isDirectory) "This Directory" else "This File"
                    )

                    contextList.forEach {
                        DropdownMenuItem(
                            text = { Text(text = it.text) },
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onItemLongClick(it.index)
                                isContextMenuVisible = false
                            }
                        )
                    }
                }
            },
            supportingContent = {
                // Hacky
                val comment = if (item.isDirectory) {
                    stringResource(id = R.string.directory)
                } else {
                    item.comment
                }
                Text(text = comment, fontStyle = FontStyle.Italic)
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BreadCrumbs(
    crumbScrollState: LazyListState,
    crumbs: List<FileListViewModel.BreadCrumb>,
    onCrumbMenu: (Int) -> Unit,
    onCrumbClick: (FileListViewModel.BreadCrumb, Int) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
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
                                        onCrumbMenu(it.index)
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
