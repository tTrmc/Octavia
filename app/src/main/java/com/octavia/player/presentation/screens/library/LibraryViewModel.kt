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
        mediaPlaybackRepository.playerState
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
            val stats = mediaLibraryScanUseCase.getLibraryStats()
            if (stats.totalTracks == 0) {
                scanLibrary()
            }
        }
    }

    fun scanLibrary() {
        viewModelScope.launch {
            mediaLibraryScanUseCase.scanLibrary(application)
                .onSuccess { trackCount ->
                    // Successfully scanned $trackCount tracks
                }
                .onFailure { exception ->
                    // TODO: Handle error state in UI
                }
        }
    }

    fun playTrack(track: Track) {
        viewModelScope.launch {
            playbackControlUseCase.playTrack(track)
        }
    }

    fun playTracks(tracks: List<Track>, startIndex: Int = 0) {
        viewModelScope.launch {
            playbackControlUseCase.playTracks(tracks, startIndex)
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
    val isLoading: Boolean = true,
    val error: String? = null
)
