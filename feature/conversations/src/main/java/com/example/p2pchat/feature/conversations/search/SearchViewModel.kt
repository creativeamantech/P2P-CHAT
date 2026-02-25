package com.example.p2pchat.feature.conversations.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.p2pchat.feature.conversations.domain.SearchResults
import com.example.p2pchat.feature.conversations.domain.SearchUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SearchUiState {
    object Idle : SearchUiState
    object Loading : SearchUiState
    data class Success(val results: SearchResults) : SearchUiState
    object Empty : SearchUiState
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchUseCase: SearchUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .filter { it.isNotBlank() }
                .collectLatest { query ->
                    _uiState.value = SearchUiState.Loading
                    searchUseCase(query)
                        .collect { results ->
                            val isEmpty = results.messages.isEmpty() && results.topics.isEmpty() && results.peers.isEmpty()
                            _uiState.value = if (isEmpty) SearchUiState.Empty else SearchUiState.Success(results)
                        }
                }
        }
    }

    fun onQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _uiState.value = SearchUiState.Idle
        }
    }
}
