package org.helllabs.android.xmp.compose.ui.preferences.about

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.core.view.WindowCompat
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.compose.components.XmpTopBar
import org.helllabs.android.xmp.compose.theme.XmpTheme
import timber.log.Timber

class ListFormats : ComponentActivity() {

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            val scrollState = rememberLazyListState()
            val isScrolled = remember {
                derivedStateOf {
                    scrollState.firstVisibleItemIndex > 0
                }
            }

            XmpTheme {
                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    topBar = {
                        XmpTopBar(
                            title = stringResource(id = R.string.pref_about_formats),
                            isScrolled = isScrolled.value,
                            onBack = {
                                onBackPressedDispatcher.onBackPressed()
                            }
                        )
                    }
                ) { paddingValues ->
                    val formats by remember { mutableStateOf(Xmp.getFormats().orEmpty()) }
                    val clip = LocalClipboardManager.current
                    val context = LocalContext.current
                    val haptic = LocalHapticFeedback.current
                    val scope = rememberCoroutineScope()

                    LazyColumn(
                        modifier = Modifier
                            .padding(paddingValues)
                            .fillMaxSize(),
                        state = scrollState
                    ) {
                        items(formats) { item ->
                            ListItem(
                                modifier = Modifier.combinedClickable(
                                    onClick = { /* Nothing */ },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = context.getString(R.string.clipboard_copied)
                                            )
                                        }
                                        clip.setText(buildAnnotatedString { append(item) })
                                    }
                                ),
                                headlineContent = {
                                    Text(text = item)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
