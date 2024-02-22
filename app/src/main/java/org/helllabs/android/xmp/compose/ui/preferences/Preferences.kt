package org.helllabs.android.xmp.compose.ui.preferences

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.lifecycle.lifecycleScope
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsMenuLink
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.XmpTopBar
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.core.StorageManager
import timber.log.Timber

class Preferences : ComponentActivity() {

    private var snackBarHostState = SnackbarHostState()

    private val documentTreeContract = ActivityResultContracts.OpenDocumentTree()
    private val documentTreeResult = registerForActivityResult(documentTreeContract) { uri ->
        StorageManager.setPlaylistDirectory(
            uri = uri,
            onSuccess = {
                lifecycleScope.launch {
                    snackBarHostState.showSnackbar("Default directory changed")
                }
            },
            onError = {
                lifecycleScope.launch {
                    snackBarHostState.showSnackbar("Failed to change default directory")
                }
            }
        )
    }

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
                    snackBarHostState = snackBarHostState,
                    onBack = onBackPressedDispatcher::onBackPressed,
                    onChangeDir = {
                        val dir = PrefManager.safStoragePath.run { Uri.parse(this) }
                        documentTreeResult.launch(dir)
                    },
                    onFormats = {
                        Intent(this, PreferencesFormats::class.java).also(::startActivity)
                    },
                    onAbout = {
                        Intent(this, PreferencesAbout::class.java).also(::startActivity)
                    }
                )
            }
        }
    }
}

@Composable
private fun PreferencesScreen(
    snackBarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onChangeDir: () -> Unit,
    onFormats: () -> Unit,
    onAbout: () -> Unit
) {
    val scrollState = rememberScrollState()
    val isScrolled = remember {
        derivedStateOf {
            scrollState.value > 0
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
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
            SettingsGroupPlaylist(onChangeDir = onChangeDir)
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
                                    val uri = permission.uri
                                    Timber.d("Failed to revoke perms for URI: $uri")
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
    PrefManager.init(context)
    XmpTheme(useDarkTheme = true) {
        PreferencesScreen(
            snackBarHostState = SnackbarHostState(),
            onBack = {},
            onChangeDir = {},
            onFormats = {},
            onAbout = {}
        )
    }
}
