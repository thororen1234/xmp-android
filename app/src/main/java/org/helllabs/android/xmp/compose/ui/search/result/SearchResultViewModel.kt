package org.helllabs.android.xmp.compose.ui.search.result

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.api.Repository
import timber.log.Timber

@Stable
data class SearchResultState(
    val hardError: String? = null,
    val isLoading: Boolean = false,
    val softError: String? = null,
    val title: String = "",
    val result: Any? = null
)

@Stable
class SearchResultViewModelFactory : ViewModelProvider.Factory {
    private val repository = Repository(XmpApplication.modArchiveModule.apiHelper)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SearchResultViewModel(repository) as T
    }
}

@Stable
class SearchResultViewModel(
    private val repository: Repository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchResultState())
    val uiState = _uiState.asStateFlow()

    fun getFileOrTitle(string: String, query: String) = viewModelScope.launch {
        _uiState.update { it.copy(title = string, isLoading = true) }

        try {
            val result = repository.getFileNameOrTitle(query)
            if (!result.error.isNullOrBlank()) {
                _uiState.update { it.copy(softError = result.error) }
            } else {
                _uiState.update { it.copy(result = result, softError = "") }
            }
        } catch (e: Exception) {
            Timber.e(e)
            _uiState.update { it.copy(hardError = e.message) }
        } finally {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun getArtistById(id: Int) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }

        try {
            val result = repository.getArtistById(id)
            if (!result.error.isNullOrBlank()) {
                _uiState.update { it.copy(softError = result.error) }
            } else {
                _uiState.update { it.copy(result = result, softError = "") }
            }
        } catch (e: Exception) {
            Timber.e(e)
            _uiState.update { it.copy(hardError = e.message) }
        } finally {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun getArtists(string: String, query: String) = viewModelScope.launch {
        _uiState.update { it.copy(title = string, isLoading = true) }

        try {
            val result = repository.getArtistSearch(query)
            if (!result.error.isNullOrBlank()) {
                _uiState.update { it.copy(softError = result.error) }
            } else {
                _uiState.update { it.copy(result = result, softError = "") }
            }
        } catch (e: Exception) {
            Timber.e(e)
            _uiState.update { it.copy(hardError = e.message) }
        } finally {
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
