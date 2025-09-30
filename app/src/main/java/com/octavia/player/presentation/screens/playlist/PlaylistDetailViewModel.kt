package com.octavia.player.presentation.screens.playlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.octavia.player.data.model.Playlist
import com.octavia.player.data.model.PlaybackState
import com.octavia.player.data.model.Track
import com.octavia.player.domain.repository.MediaPlaybackRepository
import com.octavia.player.domain.usecase.PlaybackControlUseCase
import com.octavia.player.domain.usecase.PlaylistManagementUseCase
import com.octavia.player.domain.usecase.PlaylistTrackManagementUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Playlist Detail screen
 * Manages playlist state, track list, and playback integration
 */
@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val playlistManagementUseCase: PlaylistManagementUseCase,
    private val playlistTrackManagementUseCase: PlaylistTrackManagementUseCase,
    private val playbackControlUseCase: PlaybackControlUseCase,
    private val mediaPlaybackRepository: MediaPlaybackRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val playlistId: Long = savedStateHandle.get<String>("playlistId")?.toLongOrNull() ?: 0L

    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<PlaylistDetailUiState> = combine(
        playlistManagementUseCase.getPlaylistWithTracks(playlistId),
        mediaPlaybackRepository.currentTrack,
        mediaPlaybackRepository.playerState,
        _errorMessage
    ) { (playlist, tracks), currentTrack, playerState, error ->
        PlaylistDetailUiState(
            playlist = playlist,
            tracks = tracks,
            isLoading = false,
            isPlaying = playerState.isPlaying,
            currentlyPlayingTrack = currentTrack,
            playbackState = playerState.playbackState,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlaylistDetailUiState(isLoading = true)
    )

    init {
        android.util.Log.d("PlaylistDetailVM", "Initialized with playlist ID: $playlistId")
    }

    // Playback methods

    /**
     * Play all tracks in the playlist
     */
    fun playPlaylist(shuffled: Boolean = false) {
        viewModelScope.launch {
            val tracks = uiState.value.tracks
            if (tracks.isEmpty()) {
                _errorMessage.value = "Playlist is empty"
                return@launch
            }

            val tracksToPlay = if (shuffled) tracks.shuffled() else tracks
            android.util.Log.d("PlaylistDetailVM", "Playing playlist with ${tracksToPlay.size} tracks (shuffled: $shuffled)")
            playbackControlUseCase.playTracks(tracksToPlay, 0)
        }
    }

    /**
     * Play a specific track from the playlist
     */
    fun playTrack(track: Track) {
        viewModelScope.launch {
            val tracks = uiState.value.tracks
            val trackIndex = tracks.indexOf(track)

            if (trackIndex >= 0) {
                android.util.Log.d("PlaylistDetailVM", "Playing track '${track.displayTitle}' at index $trackIndex")
                playbackControlUseCase.playTracks(tracks, trackIndex)
            } else {
                android.util.Log.w("PlaylistDetailVM", "Track not found in playlist, playing standalone")
                playbackControlUseCase.playTrack(track)
            }
        }
    }

    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        playbackControlUseCase.togglePlayPause()
    }

    // Track management methods

    /**
     * Remove a track from the playlist
     */
    fun removeTrack(trackId: Long) {
        viewModelScope.launch {
            playlistTrackManagementUseCase.removeTrackFromPlaylist(playlistId, trackId)
                .onSuccess {
                    android.util.Log.d("PlaylistDetailVM", "Removed track $trackId from playlist")
                }
                .onFailure { error ->
                    android.util.Log.e("PlaylistDetailVM", "Failed to remove track", error)
                    _errorMessage.value = "Failed to remove track"
                }
        }
    }

    /**
     * Move a track from one position to another
     */
    fun moveTrack(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            playlistTrackManagementUseCase.moveTrack(playlistId, fromIndex, toIndex)
                .onSuccess {
                    android.util.Log.d("PlaylistDetailVM", "Moved track from $fromIndex to $toIndex")
                }
                .onFailure { error ->
                    android.util.Log.e("PlaylistDetailVM", "Failed to move track", error)
                    _errorMessage.value = "Failed to reorder track"
                }
        }
    }

    /**
     * Clear all tracks from the playlist
     */
    fun clearPlaylist() {
        viewModelScope.launch {
            playlistTrackManagementUseCase.clearPlaylist(playlistId)
                .onSuccess {
                    android.util.Log.d("PlaylistDetailVM", "Cleared all tracks from playlist")
                }
                .onFailure { error ->
                    android.util.Log.e("PlaylistDetailVM", "Failed to clear playlist", error)
                    _errorMessage.value = "Failed to clear playlist"
                }
        }
    }

    // Playlist management methods

    /**
     * Update playlist metadata (name and description)
     */
    fun updatePlaylist(name: String, description: String?) {
        viewModelScope.launch {
            playlistManagementUseCase.updatePlaylistMetadata(playlistId, name, description)
                .onSuccess {
                    android.util.Log.d("PlaylistDetailVM", "Updated playlist: $name")
                }
                .onFailure { error ->
                    android.util.Log.e("PlaylistDetailVM", "Failed to update playlist", error)
                    _errorMessage.value = error.message ?: "Failed to update playlist"
                }
        }
    }

    /**
     * Delete the playlist
     */
    fun deletePlaylist(onSuccess: () -> Unit) {
        viewModelScope.launch {
            playlistManagementUseCase.deletePlaylist(playlistId)
                .onSuccess {
                    android.util.Log.d("PlaylistDetailVM", "Deleted playlist")
                    onSuccess()
                }
                .onFailure { error ->
                    android.util.Log.e("PlaylistDetailVM", "Failed to delete playlist", error)
                    _errorMessage.value = "Failed to delete playlist"
                }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}

/**
 * UI state for the Playlist Detail screen
 */
data class PlaylistDetailUiState(
    val playlist: Playlist? = null,
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = true,
    val isPlaying: Boolean = false,
    val currentlyPlayingTrack: Track? = null,
    val playbackState: PlaybackState = PlaybackState.IDLE,
    val error: String? = null
)