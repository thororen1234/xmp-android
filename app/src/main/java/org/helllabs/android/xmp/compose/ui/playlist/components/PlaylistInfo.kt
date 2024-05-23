package org.helllabs.android.xmp.compose.ui.playlist.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.theme.XmpTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistInfo(
    isScrolled: Boolean,
    playlistName: String,
    playlistComment: String
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = if (isScrolled) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f)
            } else {
                MaterialTheme.colorScheme.surface
            },
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        title = {
            Column {
                Text(text = playlistName)
                Text(
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    text = playlistComment.ifEmpty {
                        stringResource(
                            id = R.string.error_no_comment
                        )
                    }
                )
            }
        },
        windowInsets = WindowInsets(right = 16.dp)
    )
    HorizontalDivider(modifier = Modifier.fillMaxWidth())
}

@Preview
@Composable
private fun Preview_PlaylistInfo() {
    XmpTheme(useDarkTheme = true) {
        PlaylistInfo(
            isScrolled = false,
            playlistName = "Playlist Name",
            playlistComment = "Playlist Comment"
        )
    }
}
