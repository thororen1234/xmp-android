package org.helllabs.android.xmp.compose.ui.search.result

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import okio.ForwardingSource
import okio.buffer
import okio.sink
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.api.Repository
import org.helllabs.android.xmp.compose.components.ErrorScreen
import org.helllabs.android.xmp.compose.components.ModuleLayout
import org.helllabs.android.xmp.compose.components.ProgressbarIndicator
import org.helllabs.android.xmp.compose.components.XmpTopBar
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.search.Search
import org.helllabs.android.xmp.core.Constants.isSupported
import org.helllabs.android.xmp.core.Files
import org.helllabs.android.xmp.model.Artist
import org.helllabs.android.xmp.model.ArtistInfo
import org.helllabs.android.xmp.model.License
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.model.ModuleResult
import org.helllabs.android.xmp.model.Sponsor
import org.helllabs.android.xmp.model.SponsorDetails
import org.helllabs.android.xmp.player.PlayerActivity
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject

sealed class DownloadStatus {
    data class Error(val error: Exception) : DownloadStatus()
    data class Progress(val percent: Int) : DownloadStatus()
    object None : DownloadStatus()
    object Loading : DownloadStatus()
    object Success : DownloadStatus()
}

@HiltViewModel
class ModuleResultViewModel @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val repository: Repository
) : ViewModel() {
    data class ModuleResultState(
        val isRandom: Boolean = false,
        val isLoading: Boolean = false,
        val softError: String? = null,
        val hardError: String? = null,
        val module: ModuleResult? = null,
        val moduleExists: Boolean = false,
        val moduleSupported: Boolean = true,
        val downloadStatus: DownloadStatus = DownloadStatus.None
    )

    private val _uiState = MutableStateFlow(ModuleResultState())
    val uiState = _uiState.asStateFlow()

    private var currentDownloadJob: Job? = null

    override fun onCleared() {
        super.onCleared()
        currentDownloadJob?.cancel()
    }

    fun downloadModule(mod: String, url: String, file: String) {
        currentDownloadJob?.cancel()

        val pathFile = File(file, mod)
        if (pathFile.parentFile?.exists() == false) {
            pathFile.parentFile?.mkdirs()
        }

        currentDownloadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .build()

                _uiState.update {
                    it.copy(
                        isLoading = true,
                        downloadStatus = DownloadStatus.Loading
                    )
                }

                Timber.d("Mod file is: $mod")
                Timber.d("Trying to download a file to $pathFile")
                Timber.d("Download URL is: $url")

                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    response.body?.let { body ->
                        val contentLength = body.contentLength()
                        val source = body.source()

                        val progressSource = object : ForwardingSource(source) {
                            var totalBytesRead = 0L

                            override fun read(sink: Buffer, byteCount: Long): Long {
                                val bytesRead = super.read(sink, byteCount)
                                totalBytesRead += if (bytesRead != -1L) bytesRead else 0
                                _uiState.update {
                                    val value = (totalBytesRead * 100 / contentLength).toInt()
                                    it.copy(downloadStatus = DownloadStatus.Progress(value))
                                }
                                return bytesRead
                            }
                        }

                        progressSource.use { input ->
                            pathFile.sink().buffer().use { output ->
                                output.writeAll(input)
                            }
                        }
                    }
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        downloadStatus = DownloadStatus.Success
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        downloadStatus = DownloadStatus.Error(e)
                    )
                }
            } finally {
                _uiState.update {
                    it.copy(
                        moduleExists = doesModuleExist(_uiState.value.module),
                        moduleSupported = isModuleSupported(_uiState.value.module)
                    )
                }
            }
        }
    }

    fun getModuleById(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRandom = false, isLoading = true) }

            try {
                val result = repository.getModuleById(id)
                if (result.error != null) {
                    _uiState.update { it.copy(softError = result.error) }
                } else {
                    saveModuleToHistory(result.module)
                    _uiState.update {
                        it.copy(
                            module = result,
                            moduleExists = doesModuleExist(result),
                            moduleSupported = isModuleSupported(result)
                        )
                    }
                }

                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Timber.e(e)
                _uiState.update { it.copy(hardError = e.localizedMessage) }
            }
        }
    }

    fun getRandomModule() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRandom = true, isLoading = true) }

            try {
                val result = repository.getRandomModule()
                if (!result.error.isNullOrBlank()) {
                    _uiState.update { it.copy(softError = result.error) }
                } else {
                    saveModuleToHistory(result.module)
                    _uiState.update {
                        it.copy(
                            module = result,
                            moduleExists = doesModuleExist(result),
                            moduleSupported = isModuleSupported(result)
                        )
                    }
                }

                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Timber.e(e)
                _uiState.update { it.copy(hardError = e.localizedMessage) }
            }
        }
    }

    fun deleteModule() {
        val result = Files.deleteModuleFile(_uiState.value.module?.module!!)
        Timber.d("Module deleted was: $result")
        _uiState.update {
            it.copy(
                moduleExists = doesModuleExist(_uiState.value.module),
                moduleSupported = isModuleSupported(_uiState.value.module)
            )
        }
    }

    private fun doesModuleExist(result: ModuleResult?): Boolean {
        val exist = Files.localFile(result?.module)?.exists()
        Timber.d("Does module exist? -> $exist")
        return exist ?: false
    }

    private fun isModuleSupported(result: ModuleResult?): Boolean {
        val supported = result?.module?.isSupported() ?: true
        Timber.d("Is module supported? -> $supported")
        return supported
    }

    private fun saveModuleToHistory(module: Module?) {
        // TODO save to history
    }
}

@AndroidEntryPoint
open class Result : ComponentActivity() {

    private val viewModel by viewModels<ModuleResultViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val id = intent.getIntExtra(Search.MODULE_ID, -1)
        if (id < 0) {
            viewModel.getRandomModule()
        } else {
            viewModel.getModuleById(id)
        }

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            XmpTheme {
                var deleteModule by remember { mutableStateOf(false) }
                if (deleteModule) {
                    AlertDialog(
                        onDismissRequest = { deleteModule = false },
                        icon = {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                        },
                        title = {
                            Text(text = stringResource(id = R.string.delete_file))
                        },
                        text = {
                            Text(
                                text = stringResource(
                                    id = R.string.delete_file_message,
                                    state.module?.module?.filename ?: ""
                                )
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.deleteModule()
                                    deleteModule = false
                                }
                            ) {
                                Text(text = stringResource(id = R.string.menu_delete))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { deleteModule = false }) {
                                Text(text = stringResource(id = R.string.cancel))
                            }
                        }
                    )
                }

                var moduleExists by remember { mutableStateOf(false) }
                if (moduleExists) {
                    AlertDialog(
                        onDismissRequest = { moduleExists = false },
                        icon = {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                        },
                        title = {
                            Text(text = stringResource(id = R.string.msg_file_exists))
                        },
                        text = {
                            Text(text = stringResource(id = R.string.msg_file_exists_message))
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    moduleExists = false
                                }
                            ) {
                                Text(text = stringResource(id = R.string.ok))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { moduleExists = false }) {
                                Text(text = stringResource(id = R.string.cancel))
                            }
                        }
                    )
                }

                ModuleResultScreen(
                    state = state,
                    onBack = { onBackPressedDispatcher.onBackPressed() },
                    onRandom = viewModel::getRandomModule,
                    onDelete = { deleteModule = true },
                    onPlay = { module ->
                        if (Files.localFile(module)!!.exists()) {
                            val path = Files.localFile(module)!!.path
                            val modList = ArrayList<String>()

                            modList.add(path)
                            XmpApplication.instance?.fileList = modList

                            Timber.i("Play $path")
                            Intent(this@Result, PlayerActivity::class.java).apply {
                                putExtra(PlayerActivity.PARM_START, 0)
                            }.also(::startActivity)
                        } else {
                            // Does not exist, download module
                            val modDir = Files.getDownloadPath(module)
                            val url = module.url

                            Timber.i("Downloaded $url to $modDir")
                            if (Files.localFile(state.module?.module)?.exists() == true) {
                                moduleExists = true
                            } else {
                                val mod = module.filename
                                viewModel.downloadModule(mod, url, modDir)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ModuleResultScreen(
    state: ModuleResultViewModel.ModuleResultState,
    onBack: () -> Unit,
    onRandom: () -> Unit,
    onDelete: () -> Unit,
    onPlay: (module: Module) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val isScrolled = remember {
        derivedStateOf {
            scrollState.value > 0
        }
    }

    Scaffold(
        topBar = {
            XmpTopBar(
                isScrolled = isScrolled.value,
                title = if (state.isRandom) {
                    stringResource(id = R.string.search_random_title)
                } else {
                    stringResource(id = R.string.search_result_title)
                },
                onBack = onBack,
                actions = {
                    if (state.moduleExists) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            val buttonText = when {
                state.isLoading -> stringResource(id = R.string.button_loading)
                state.moduleExists -> stringResource(id = R.string.button_play)
                !state.moduleSupported ->
                    stringResource(id = R.string.button_download_unsupported)
                else -> stringResource(id = R.string.download)
            }

            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        modifier = Modifier.width(112.dp),
                        enabled = !state.isLoading && state.moduleSupported,
                        onClick = { onPlay(state.module!!.module) }
                    ) {
                        Text(text = buttonText)
                    }
                    Button(
                        modifier = Modifier.width(112.dp),
                        enabled = !state.isLoading,
                        onClick = onRandom
                    ) {
                        Text(text = stringResource(id = R.string.search_random_pick))
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(.5f),
                contentAlignment = Alignment.Center
            ) {
                ModuleLayout(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    moduleResult = state.module
                )

                state.softError?.let {
                    ErrorScreen(text = it)
                }

                ProgressbarIndicator(isLoading = state.isLoading)
            }
        }
    }
}

@Preview
@Composable
private fun Preview_ModuleResult() {
    XmpTheme {
        ModuleResultScreen(
            state = ModuleResultViewModel.ModuleResultState(
                module = ModuleResult(
                    sponsor = Sponsor(
                        details = SponsorDetails(
                            link = "",
                            text = "Some Sponsor Text"
                        )
                    ),
                    module = Module(
                        filename = "",
                        bytes = 669669,
                        format = "XM",
                        artistInfo = ArtistInfo(artist = listOf(Artist(alias = "Some Artist"))),
                        infopage = "",
                        license = License(
                            title = "Some License Title",
                            legalurl = "",
                            description = "Some License Description"
                        ),
                        comment = "Some Comment",
                        instruments = buildAnnotatedString {
                            repeat(20) {
                                append("Some Instrument $it\n")
                            }
                        }.toString()
                    )
                )
            ),
            onBack = {},
            onDelete = {},
            onPlay = {},
            onRandom = {}
        )
    }
}
