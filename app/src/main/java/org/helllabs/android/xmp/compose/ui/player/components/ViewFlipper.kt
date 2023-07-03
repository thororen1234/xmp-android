package org.helllabs.android.xmp.compose.ui.player.components

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.theme.michromaFontFamily

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ViewFlipper(
    actions: (@Composable BoxScope.() -> Unit)? = null,
    pagerState: PagerState,
    title: List<String>,
    format: List<String>
) {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        ),
        title = {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                beyondBoundsPageCount = 0
            ) { page ->
                ViewFlipperItem(
                    actions = actions,
                    modTitle = title[page],
                    format = format[page]
                )
            }
        }
    )
}

@Composable
private fun ViewFlipperItem(
    actions: (@Composable BoxScope.() -> Unit)? = null,
    modTitle: String,
    format: String
) {
    Box {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ProvideTextStyle(
                LocalTextStyle.current.merge(
                    TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                )
            ) {
                Text(
                    fontFamily = michromaFontFamily,
                    fontSize = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    text = modTitle
                )
                Text(
                    fontFamily = michromaFontFamily,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    text = format
                )
            }
        }

        if (actions != null) {
            actions()
        }
    }
}

@Preview
@Composable
private fun ViewFlipperItemPreview() {
    XmpTheme {
        ViewFlipperItem(actions = {}, "Some Text", "Some Format")
    }
}

// To be ran on a device or emulator to test
@OptIn(ExperimentalFoundationApi::class)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun ViewFlipperPreview() {
    XmpTheme {
        var infoName by remember { mutableStateOf(listOf("Name: 0")) }
        var infoType by remember { mutableStateOf(listOf("Format: 0")) }
        val scope = rememberCoroutineScope()
        val pagerState = rememberPagerState(
            initialPage = 0,
            pageCount = { infoName.size }
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ViewFlipper(
                actions = {
                    IconButton(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        onClick = { }
                    ) {
                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null)
                    }
                },
                pagerState = pagerState,
                title = infoName,
                format = infoType
            )
            Spacer(Modifier.size(20.dp))
            Row(horizontalArrangement = Arrangement.SpaceAround) {
                Button(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                ) { Text("Previous") }
                Spacer(Modifier.size(60.dp))
                Button(
                    onClick = {
                        infoName = infoName.toMutableList().apply { add("Name: ${infoName.size}") }
                        infoType = infoType.toMutableList().apply { add("Format: ${infoType.size}") }
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                ) { Text("Forward") }
            }
        }
    }
}
