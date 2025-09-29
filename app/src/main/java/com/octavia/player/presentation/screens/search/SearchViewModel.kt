package com.octavia.player.presentation.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.octavia.player.data.model.Track
import com.octavia.player.domain.usecase.GetTracksUseCase
import com.octavia.player.domain.usecase.PlaybackControlUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Search screen
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val getTracksUseCase: GetTracksUseCase,
    private val playbackControlUseCase: PlaybackControlUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun search(query: String) {
        if (query.trim().isEmpty()) {
            _uiState.value = SearchUiState()
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            getTracksUseCase.searchTracks(query.trim()).collect { results ->
                _uiState.value = SearchUiState(
                    searchResults = results,
                    isLoading = false,
                    query = query
                )
            }
        }
    }

    fun clearSearch() {
        _uiState.value = SearchUiState()
    }

    fun playTrack(track: Track) {
        viewModelScope.launch {
            try {
                // Get current search results to use as queue context
                val searchResults = uiState.value.searchResults
                val trackIndex = searchResults.indexOf(track)

                if (trackIndex >= 0 && searchResults.size > 1) {
                    // Play track within search results context for skip functionality
                    android.util.Log.d("SearchViewModel", "Playing track '${track.displayTitle}' at index $trackIndex of ${searchResults.size} search results")
                    playbackControlUseCase.playTracks(searchResults, trackIndex)
                } else {
                    // Fallback to single track if not found in search results
                    android.util.Log.w("SearchViewModel", "Track not found in search results or single result, playing single track: ${track.displayTitle}")
                    playbackControlUseCase.playTrack(track)
                }
            } catch (e: Exception) {
                android.util.Log.e("SearchViewModel", "Failed to play track: ${track.displayTitle}", e)
                // Final fallback
                playbackControlUseCase.playTrack(track)
            }
        }
    }
}

/**
 * UI state for the Search screen
 */
data class SearchUiState(
    val searchResults: List<Track> = emptyList(),
    val isLoading: Boolean = false,
    val query: String = "",
    val error: String? = null
)
