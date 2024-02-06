package org.helllabs.android.xmp.compose.ui.search

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.XmpTopBar
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.theme.topazFontFamily
import timber.log.Timber
import java.util.Locale

class SearchError : ComponentActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.auto(
                Color.Transparent.toArgb(),
                Color.Transparent.toArgb()
            )
        )

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Intent(this@SearchError, Search::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }.also(::startActivity)
            }
        }

        val error = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(Search.ERROR, Throwable::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(Search.ERROR) as Throwable?
        }

        // Print the error into Logcat.
        error?.let { Timber.e(it) }

        val cleanMessage = error?.message?.substringAfter("Exception: ")?.trim()
        val message = when {
            cleanMessage.isNullOrEmpty() -> getString(R.string.search_unknown_error)
            else -> getString(
                R.string.search_known_error,
                cleanMessage.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
            )
        }

        Timber.d("onCreate")
        setContent {
            DisposableEffect(onBackPressedDispatcher) {
                onBackPressedDispatcher.addCallback(callback)
                onDispose {
                    callback.remove()
                }
            }

            XmpTheme {
                ErrorScreen(
                    onBack = { onBackPressedDispatcher.onBackPressed() },
                    message = message
                )
            }
        }
    }
}

@Composable
private fun ErrorScreen(
    message: String,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            XmpTopBar(
                onBack = onBack,
                title = stringResource(id = R.string.search_title_error)
            )
        }
    ) { paddingValues ->
        GuruFrame(
            modifier = Modifier.padding(paddingValues),
            message = message
        )
    }
}

@Composable
private fun GuruFrame(
    modifier: Modifier,
    message: String
) {
    val scope = rememberCoroutineScope()
    var frameState by remember { mutableStateOf(true) }

    LaunchedEffect(frameState) {
        scope.launch {
            delay(1337L)
            frameState = !frameState
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .border(5.dp, if (frameState) Color.Red else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = Modifier.padding(12.dp),
            text = message,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center,
            fontFamily = topazFontFamily,
            fontSize = 16.sp,
            color = Color.Red
        )
    }
}

@Preview
@Composable
private fun Preview_ErrorScreen() {
    XmpTheme(useDarkTheme = true) {
        ErrorScreen(
            message = stringResource(id = R.string.search_unknown_error),
            onBack = {}
        )
    }
}
