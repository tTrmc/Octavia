package com.octavia.player.presentation.screens.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.octavia.player.data.model.Album
import com.octavia.player.data.model.Artist
import com.octavia.player.data.model.Playlist
import com.octavia.player.data.model.Track
import com.octavia.player.data.repository.MediaRepository
import com.octavia.player.data.repository.TrackRepository
import com.octavia.player.data.scanner.ArtworkExtractor
import com.octavia.player.data.scanner.MediaScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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

    val uiState: StateFlow<LibraryUiState> = combine(
        trackRepository.getAllTracks(),
        mediaRepository.currentTrack,
        mediaRepository.playerState
    ) { tracks, currentTrack, playerState ->
        LibraryUiState(
            tracks = tracks,
            currentlyPlayingTrack = currentTrack,
            isPlaying = playerState.isPlaying,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState()
    )

    init {
        // Trigger initial scan if library is empty
        viewModelScope.launch {
            val trackCount = trackRepository.getTrackCount()
            if (trackCount == 0) {
                scanLibrary()
            }
        }
    }

    fun scanLibrary() {
        viewModelScope.launch {
            try {
                // Fast scan without artwork extraction for immediate UI update
                val scannedTracks = MediaScanner.scanMusicLibrary(application, extractArtworkInBackground = false)
                if (scannedTracks.isNotEmpty()) {
                    trackRepository.insertTracks(scannedTracks)
                    
                    // Extract artwork in background after tracks are inserted
                    launch {
                        ArtworkExtractor.preloadArtwork(application, scannedTracks)
                    }
                }
            } catch (e: Exception) {
                // TODO: Handle error state in a better way
                // For now, the error will be logged, but we could emit it through a separate flow
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

    fun togglePlayPause() {
        mediaRepository.togglePlayPause()
    }

    fun skipToNext() {
        viewModelScope.launch {
            mediaRepository.skipToNext()
        }
    }

    fun skipToPrevious() {
        viewModelScope.launch {
            mediaRepository.skipToPrevious()
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
    val currentlyPlayingTrack: Track? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)
