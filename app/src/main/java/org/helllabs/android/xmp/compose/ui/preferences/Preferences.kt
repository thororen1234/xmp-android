package org.helllabs.android.xmp.compose.ui.preferences

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsMenuLink
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.XmpTopBar
import org.helllabs.android.xmp.compose.theme.XmpTheme
import timber.log.Timber
import java.io.File

class Preferences : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.auto(
                Color.Transparent.toArgb(),
                Color.Transparent.toArgb()
            )
        )

        Timber.d("onCreate")
        setContent {
            XmpTheme {
                PreferencesScreen(
                    onBack = {
                        onBackPressedDispatcher.onBackPressed()
                    },
                    onFormats = {
                        Intent(this, PreferencesFormats::class.java).also(::startActivity)
                    },
                    onAbout = {
                        Intent(this, PreferencesAbout::class.java).also(::startActivity)
                    },
                    onClearCache = {
                        val result = if (deleteCache(PrefManager.CACHE_DIR)) {
                            getString(R.string.cache_clear)
                        } else {
                            getString(R.string.cache_clear_error)
                        }
                        Toast.makeText(this, result, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    companion object {
        // TODO Remove caching, we can do it on the fly.
        @Deprecated("Remove caching, we can do it on the fly.")
        private fun deleteCache(file: File): Boolean {
            if (!file.exists()) {
                return true
            }

            if (file.isDirectory) {
                file.listFiles().orEmpty().forEach { cacheFile ->
                    if (!deleteCache(cacheFile)) {
                        return false
                    }
                }
            }
            return file.delete()
        }
    }
}

@Composable
private fun PreferencesScreen(
    onBack: () -> Unit,
    onFormats: () -> Unit,
    onAbout: () -> Unit,
    onClearCache: () -> Unit
) {
    val scrollState = rememberScrollState()
    val isScrolled = remember {
        derivedStateOf {
            scrollState.value > 0
        }
    }

    Scaffold(
        topBar = {
            XmpTopBar(
                title = stringResource(id = R.string.settings),
                isScrolled = isScrolled.value,
                onBack = onBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            val context = LocalContext.current
            SettingsGroupPlaylist(onClearCache = onClearCache)
            SettingsGroupSound()
            SettingsGroupInterface()
            SettingsGroupDownload()
            SettingsGroupInformation(onFormats = onFormats, onAbout = onAbout)

            if (BuildConfig.DEBUG) {
                SettingsGroup(
                    title = { Text(text = "Debug") }
                ) {
                    SettingsMenuLink(
                        title = { Text(text = "Revoke all Uri Permissions") },
                        onClick = {
                            val uriPermissions = context.contentResolver.persistedUriPermissions
                            for (permission in uriPermissions) {
                                try {
                                    context.contentResolver.releasePersistableUriPermission(
                                        permission.uri,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                    )
                                } catch (e: SecurityException) {
                                    Timber.d("Failed to revoke permissions for URI: ${permission.uri}")
                                }
                            }
                            (context as ComponentActivity).finishAffinity()
                        }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview_PreferencesScreen() {
    val context = LocalContext.current
    PrefManager.init(context, File("sdcard"))
    XmpTheme(useDarkTheme = true) {
        PreferencesScreen(
            onBack = {},
            onFormats = {},
            onAbout = {},
            onClearCache = {}
        )
    }
}
