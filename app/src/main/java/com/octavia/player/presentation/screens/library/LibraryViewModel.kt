package com.octavia.player.presentation.screens.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.octavia.player.data.model.Album
import com.octavia.player.data.model.Artist
import com.octavia.player.data.model.Playlist
import com.octavia.player.data.model.Track
import com.octavia.player.domain.repository.MediaPlaybackRepository
import com.octavia.player.domain.usecase.GetTracksUseCase
import com.octavia.player.domain.usecase.MediaLibraryScanUseCase
import com.octavia.player.domain.usecase.PlaybackControlUseCase
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
    private val getTracksUseCase: GetTracksUseCase,
    private val playbackControlUseCase: PlaybackControlUseCase,
    private val mediaLibraryScanUseCase: MediaLibraryScanUseCase,
    private val mediaPlaybackRepository: MediaPlaybackRepository
) : AndroidViewModel(application) {

    val uiState: StateFlow<LibraryUiState> = combine(
        getTracksUseCase.getAllTracks(),
        mediaPlaybackRepository.currentTrack,
        mediaPlaybackRepository.playerState,
        mediaPlaybackRepository.currentPosition
    ) { tracks, currentTrack, playerState, currentPosition ->
        LibraryUiState(
            tracks = tracks,
            currentlyPlayingTrack = currentTrack,
            isPlaying = playerState.isPlaying,
            currentPosition = currentPosition,
            duration = playerState.duration,
            progress = if (playerState.duration > 0) currentPosition.toFloat() / playerState.duration else 0f,
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
            val stats = mediaLibraryScanUseCase.getLibraryStats()
            if (stats.totalTracks == 0) {
                scanLibrary()
            }
        }
    }

    fun scanLibrary() {
        viewModelScope.launch {
            try {
                mediaLibraryScanUseCase.scanLibrary(application)
                    .onSuccess { trackCount ->
                        // Successfully scanned $trackCount tracks
                        android.util.Log.i("LibraryViewModel", "Successfully scanned $trackCount tracks")
                    }
                    .onFailure { exception ->
                        android.util.Log.e("LibraryViewModel", "Failed to scan library", exception)
                        // TODO: Update UI state with error
                    }
            } catch (e: Exception) {
                android.util.Log.e("LibraryViewModel", "Unexpected error during library scan", e)
            }
        }
    }

    fun playTrack(track: Track) {
        viewModelScope.launch {
            try {
                playbackControlUseCase.playTrack(track)
            } catch (e: Exception) {
                android.util.Log.e("LibraryViewModel", "Failed to play track: ${track.displayTitle}", e)
            }
        }
    }

    fun playTracks(tracks: List<Track>, startIndex: Int = 0) {
        viewModelScope.launch {
            try {
                playbackControlUseCase.playTracks(tracks, startIndex)
            } catch (e: Exception) {
                android.util.Log.e("LibraryViewModel", "Failed to play tracks", e)
            }
        }
    }

    fun togglePlayPause() {
        playbackControlUseCase.togglePlayPause()
    }

    fun skipToNext() {
        viewModelScope.launch {
            playbackControlUseCase.skipToNext()
        }
    }

    fun skipToPrevious() {
        viewModelScope.launch {
            playbackControlUseCase.skipToPrevious()
        }
    }
    
    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            playbackControlUseCase.toggleFavorite(track)
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
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val progress: Float = 0f,
    val isLoading: Boolean = true,
    val error: String? = null
)
