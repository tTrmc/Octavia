package com.octavia.player.presentation.screens.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.octavia.player.data.model.*
import com.octavia.player.data.repository.MediaRepository
import com.octavia.player.data.repository.TrackRepository
import com.octavia.player.data.scanner.MediaScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Library screen
 */
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val application: Application,
    private val trackRepository: TrackRepository,
    private val mediaRepository: MediaRepository
) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()
    
    init {
        loadLibraryData()
        // Trigger initial scan if library is empty
        viewModelScope.launch {
            val trackCount = trackRepository.getTrackCount()
            if (trackCount == 0) {
                scanLibrary()
            }
        }
    }
    
    private fun loadLibraryData() {
        viewModelScope.launch {
            trackRepository.getAllTracks().collect { tracks ->
                _uiState.value = _uiState.value.copy(
                    tracks = tracks,
                    isLoading = false
                )
            }
        }
    }
    
    fun scanLibrary() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val scannedTracks = MediaScanner.scanMusicLibrary(application)
                if (scannedTracks.isNotEmpty()) {
                    trackRepository.insertTracks(scannedTracks)
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to scan library: ${e.message}"
                )
            }
        }
    }
    
    fun playTrack(track: Track) {
        viewModelScope.launch {
            mediaRepository.playTrack(track)
        }
    }
    
    fun playTracks(tracks: List<Track>, startIndex: Int = 0) {
        viewModelScope.launch {
            mediaRepository.playTracks(tracks, startIndex)
        }
    }
}

/**
 * UI state for the Library screen
 */
data class LibraryUiState(
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
