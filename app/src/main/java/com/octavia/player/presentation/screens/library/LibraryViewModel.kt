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
import com.octavia.player.domain.usecase.PlaylistManagementUseCase
import com.octavia.player.domain.usecase.PlaylistTrackManagementUseCase
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
    private val mediaPlaybackRepository: MediaPlaybackRepository,
    private val playlistManagementUseCase: PlaylistManagementUseCase,
    private val playlistTrackManagementUseCase: PlaylistTrackManagementUseCase
) : AndroidViewModel(application) {

    val uiState: StateFlow<LibraryUiState> = combine(
        getTracksUseCase.getAllTracks(),
        playlistManagementUseCase.getAllPlaylists(),
        mediaPlaybackRepository.currentTrack,
        mediaPlaybackRepository.playerState,
        mediaPlaybackRepository.currentPosition
    ) { tracks, playlists, currentTrack, playerState, currentPosition ->
        LibraryUiState(
            tracks = tracks,
            playlists = playlists,
            currentlyPlayingTrack = currentTrack,
            isPlaying = playerState.isPlaying,
            currentPosition = currentPosition,
            duration = playerState.duration,
            progress = if (playerState.duration > 0) currentPosition.toFloat() / playerState.duration else 0f,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = LibraryUiState()
    )

    init {
        // Trigger initial scan if library is empty
        viewModelScope.launch {
            val stats = mediaLibraryScanUseCase.getLibraryStats()
            if (stats.totalTracks == 0) {
                scanLibrary() // Now uses scanLibraryWithArtwork
            }
        }
    }

    fun scanLibrary() {
        viewModelScope.launch {
            try {
                mediaLibraryScanUseCase.scanLibraryWithArtwork(application)
                    .onSuccess { trackCount ->
                        // Successfully scanned $trackCount tracks
                        android.util.Log.i("LibraryViewModel", "Successfully scanned $trackCount tracks with artwork")
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
                // Get current track list from UI state
                val currentTracks = uiState.value.tracks
                val trackIndex = currentTracks.indexOf(track)

                if (trackIndex >= 0) {
                    // Play track in context of current track list for proper skip functionality
                    android.util.Log.d("LibraryViewModel", "Playing track '${track.displayTitle}' at index $trackIndex of ${currentTracks.size} tracks")
                    playbackControlUseCase.playTracks(currentTracks, trackIndex)
                } else {
                    // Fallback to single track if not found in current list
                    android.util.Log.w("LibraryViewModel", "Track not found in current list, playing single track: ${track.displayTitle}")
                    playbackControlUseCase.playTrack(track)
                }
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

    // Playlist management methods

    fun createPlaylist(name: String, description: String?) {
        viewModelScope.launch {
            try {
                playlistManagementUseCase.createPlaylist(name, description)
                    .onSuccess { playlistId ->
                        android.util.Log.d("LibraryViewModel", "Created playlist: $name with ID: $playlistId")
                    }
                    .onFailure { error ->
                        android.util.Log.e("LibraryViewModel", "Failed to create playlist: $name", error)
                    }
            } catch (e: Exception) {
                android.util.Log.e("LibraryViewModel", "Unexpected error creating playlist", e)
            }
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            try {
                playlistManagementUseCase.deletePlaylist(playlistId)
                    .onSuccess {
                        android.util.Log.d("LibraryViewModel", "Deleted playlist ID: $playlistId")
                    }
                    .onFailure { error ->
                        android.util.Log.e("LibraryViewModel", "Failed to delete playlist", error)
                    }
            } catch (e: Exception) {
                android.util.Log.e("LibraryViewModel", "Unexpected error deleting playlist", e)
            }
        }
    }

    fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            try {
                playlistTrackManagementUseCase.addTrackToPlaylist(playlistId, trackId)
                    .onSuccess {
                        android.util.Log.d("LibraryViewModel", "Added track $trackId to playlist $playlistId")
                    }
                    .onFailure { error ->
                        android.util.Log.e("LibraryViewModel", "Failed to add track to playlist", error)
                    }
            } catch (e: Exception) {
                android.util.Log.e("LibraryViewModel", "Unexpected error adding track to playlist", e)
            }
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
