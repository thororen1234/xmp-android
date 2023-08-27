package org.helllabs.android.xmp.compose.ui.search.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import okio.ForwardingSource
import okio.buffer
import okio.sink
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.api.Repository
import org.helllabs.android.xmp.core.Constants.isSupported
import org.helllabs.android.xmp.core.Files
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.model.ModuleResult
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val repository: Repository
) : ViewModel() {
    sealed class DownloadStatus {
        data class Error(val error: Exception) : DownloadStatus()
        data class Progress(val percent: Int) : DownloadStatus()
        object None : DownloadStatus()
        object Loading : DownloadStatus()
        object Success : DownloadStatus()
    }

    data class ModuleResultState(
        val isRandom: Boolean = false,
        val isLoading: Boolean = false,
        val softError: String? = null,
        val hardError: Throwable? = null,
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

                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    response.body.let { body ->
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
                update()
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
            } catch (e: Exception) {
                Timber.e(e)
                _uiState.update { it.copy(hardError = e) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
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
            } catch (e: Exception) {
                Timber.e(e)
                _uiState.update { it.copy(hardError = e) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun deleteModule() {
        val result = Files.deleteModuleFile(_uiState.value.module?.module!!)
        Timber.d("Module deleted was: $result")
        update()
    }

    fun update() {
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

    private fun saveModuleToHistory(module: Module) {
        val history = mutableListOf<Module>()

        // We only care about a few things to store.
        try {
            Json.decodeFromString<MutableList<Module>>(PrefManager.searchHistory).map {
                Module(
                    id = it.id,
                    format = it.format,
                    songtitle = it.songtitle,
                    artistInfo = it.artistInfo,
                    bytes = it.bytes
                )
            }.also { history.addAll(it) }
        } catch (e: Exception) {
            // Something happened or empty, make it an empty list
            Timber.w("Failed to deserialize history!")
            PrefManager.searchHistory = "[]"
        }

        if (history.any { it.id == module.id }) {
            Timber.i("Module ${module.id} already exists in history. Skipping")
            return
        }

        val moduleToAdd = Module(
            id = module.id,
            format = module.format,
            songtitle = module.songtitle,
            artistInfo = module.artistInfo,
            bytes = module.bytes
        )
        history.add(moduleToAdd)

        if (history.size >= 50) {
            history.removeFirst()
        }

        PrefManager.searchHistory = Json.encodeToString(history)
    }
}
