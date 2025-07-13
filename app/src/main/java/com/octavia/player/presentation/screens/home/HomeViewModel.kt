package com.octavia.player.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.octavia.player.data.model.Album
import com.octavia.player.data.model.Track
import com.octavia.player.data.repository.MediaRepository
import com.octavia.player.data.repository.TrackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Home screen
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val trackRepository: TrackRepository,
    private val mediaRepository: MediaRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        loadHomeData()
    }
    
    private fun loadHomeData() {
        viewModelScope.launch {
            // Combine multiple data sources
            combine(
                trackRepository.getRecentlyPlayedTracks(10),
                trackRepository.getRecentlyAddedTracks(),
                trackRepository.getFavoriteTracks(),
                trackRepository.getHiResTracks()
            ) { recentlyPlayed, recentlyAdded, favorites, hiRes ->
                
                // Get additional stats (suspend calls)
                val trackCount = trackRepository.getTrackCount()
                val totalDuration = trackRepository.getTotalDuration()
                
                HomeUiState(
                    recentlyPlayed = recentlyPlayed,
                    recentlyAdded = recentlyAdded.take(10),
                    favoriteTracksPreview = favorites.take(5),
                    hiResTracksPreview = hiRes.take(5),
                    trackCount = trackCount,
                    albumCount = 0, // TODO: Get from album repository
                    artistCount = 0, // TODO: Get from artist repository
                    totalDuration = formatDuration(totalDuration),
                    recentlyAddedAlbums = emptyList() // TODO: Get from album repository
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }
    
    fun playTrack(track: Track) {
        viewModelScope.launch {
            mediaRepository.playTrack(track)
        }
    }
    
    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val days = totalSeconds / (24 * 3600)
        val hours = (totalSeconds % (24 * 3600)) / 3600
        val minutes = (totalSeconds % 3600) / 60
        
        return when {
            days > 0 -> "${days}d ${hours}h"
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }
}

/**
 * UI state for the Home screen
 */
data class HomeUiState(
    val recentlyPlayed: List<Track> = emptyList(),
    val recentlyAdded: List<Track> = emptyList(),
    val favoriteTracksPreview: List<Track> = emptyList(),
    val hiResTracksPreview: List<Track> = emptyList(),
    val recentlyAddedAlbums: List<Album> = emptyList(),
    val trackCount: Int = 0,
    val albumCount: Int = 0,
    val artistCount: Int = 0,
    val totalDuration: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)
