package org.helllabs.android.xmp.compose.ui.player.components

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
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
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.theme.michromaFontFamily

private const val ANIMATION_DURATION = 500

// I guess this acts like a ViewFlipper now
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewFlipper(
    actions: (@Composable RowScope.() -> Unit)? = null,
    navigation: (@Composable RowScope.() -> Unit)? = null,
    skipToPrevious: Boolean,
    info: Pair<String, String>
) {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        ),
        title = {
            AnimatedContent(
                targetState = info,
                transitionSpec = {
                    if (skipToPrevious) {
                        (slideInHorizontally(animationSpec = tween(ANIMATION_DURATION)) { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally(animationSpec = tween(ANIMATION_DURATION)) { width -> width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally(animationSpec = tween(ANIMATION_DURATION)) { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally(animationSpec = tween(ANIMATION_DURATION)) { width -> -width } + fadeOut()
                        )
                    }.using(
                        SizeTransform(clip = false)
                    )
                },
                label = "XMP ViewFlipper"
            ) {
                ViewFlipperItem(
                    actions = actions,
                    navigation = navigation,
                    modTitle = it.first,
                    format = it.second
                )
            }
        }
    )
}

@Composable
private fun ViewFlipperItem(
    actions: (@Composable RowScope.() -> Unit)? = null,
    navigation: (@Composable RowScope.() -> Unit)? = null,
    modTitle: String,
    format: String
) {
    Row {
        if (navigation != null) {
            navigation()
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
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
        } else {
            Spacer(modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(40.dp)
            )
        }
    }
}

@Preview
@Composable
private fun Preview_ViewFlipperItem() {
    XmpTheme(useDarkTheme = true) {
        Surface {
            ViewFlipperItem(actions = {}, navigation = {}, "Some Text", "Some Format")
        }
    }
}

@Preview
@Composable
private fun Preview_ViewVlipperItem_2() {
    XmpTheme(useDarkTheme = true) {
        Surface {
            ViewFlipper(
                navigation = {
                    IconButton(onClick = { }) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = null)
                    }
                },
                skipToPrevious = false,
                info = Pair(
                    "Some Super Very Long Name",
                    "Some Super Duper Very Long Type"
                )
            )
        }
    }
}

// To be ran on a device or emulator to test
@Preview
@Composable
private fun Preview_Demo_ViewFlipper() {
    XmpTheme(useDarkTheme = true) {
        var infoName by remember { mutableIntStateOf(0) }
        var infoType by remember { mutableIntStateOf(0) }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ViewFlipper(
                actions = {
                    IconButton(onClick = { }) {
                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null)
                    }
                },
                navigation = {
                    IconButton(onClick = { }) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = null)
                    }
                },
                skipToPrevious = false,
                info = Pair(
                    "Some Super Very Long Name: $infoName",
                    "Some Super Duper Very Long Type: $infoType"
                )
            )
            Spacer(Modifier.size(20.dp))
            Row(horizontalArrangement = Arrangement.SpaceAround) {
                Button(
                    onClick = {
                        infoName -= 1
                        infoType -= 1
                    },
                    content = {
                        Text("Previous")
                    }
                )
                Spacer(Modifier.size(60.dp))
                Button(
                    onClick = {
                        infoName += 1
                        infoType += 1
                    },
                    content = {
                        Text("Forward")
                    }
                )
            }
        }
    }
}
