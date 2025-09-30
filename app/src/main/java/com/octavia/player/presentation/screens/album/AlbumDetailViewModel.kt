package com.octavia.player.presentation.screens.album

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.octavia.player.data.model.Album
import com.octavia.player.data.model.PlaybackState
import com.octavia.player.data.model.Track
import com.octavia.player.domain.repository.MediaPlaybackRepository
import com.octavia.player.domain.usecase.GetAlbumsUseCase
import com.octavia.player.domain.usecase.PlaybackControlUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Album Detail screen
 * Manages album state, track list, and playback integration
 */
@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val getAlbumsUseCase: GetAlbumsUseCase,
    private val playbackControlUseCase: PlaybackControlUseCase,
    private val mediaPlaybackRepository: MediaPlaybackRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val albumId: Long = savedStateHandle.get<String>("albumId")?.toLongOrNull() ?: 0L

    private val _errorMessage = MutableStateFlow<String?>(null)

    // Flow that emits album with tracks
    private val albumWithTracks = flow {
        val (album, tracks) = getAlbumsUseCase.getAlbumWithTracks(albumId)
        emit(Pair(album, tracks))
    }

    val uiState: StateFlow<AlbumDetailUiState> = combine(
        albumWithTracks,
        mediaPlaybackRepository.currentTrack,
        mediaPlaybackRepository.playerState,
        _errorMessage
    ) { (album, tracks), currentTrack, playerState, error ->
        AlbumDetailUiState(
            album = album,
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
        initialValue = AlbumDetailUiState(isLoading = true)
    )

    init {
        android.util.Log.d("AlbumDetailVM", "Initialized with album ID: $albumId")
    }

    // Playback methods

    /**
     * Play all tracks in the album
     */
    fun playAlbum(shuffled: Boolean = false) {
        viewModelScope.launch {
            val tracks = uiState.value.tracks
            if (tracks.isEmpty()) {
                _errorMessage.value = "Album has no tracks"
                return@launch
            }

            val tracksToPlay = if (shuffled) tracks.shuffled() else tracks
            android.util.Log.d("AlbumDetailVM", "Playing album with ${tracksToPlay.size} tracks (shuffled: $shuffled)")
            playbackControlUseCase.playTracks(tracksToPlay, 0)
        }
    }

    /**
     * Play a specific track from the album
     */
    fun playTrack(track: Track) {
        viewModelScope.launch {
            val tracks = uiState.value.tracks
            val trackIndex = tracks.indexOf(track)

            if (trackIndex >= 0) {
                android.util.Log.d("AlbumDetailVM", "Playing track '${track.displayTitle}' at index $trackIndex")
                playbackControlUseCase.playTracks(tracks, trackIndex)
            } else {
                android.util.Log.w("AlbumDetailVM", "Track not found in album, playing standalone")
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

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}

/**
 * UI state for the Album Detail screen
 */
data class AlbumDetailUiState(
    val album: Album? = null,
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = true,
    val isPlaying: Boolean = false,
    val currentlyPlayingTrack: Track? = null,
    val playbackState: PlaybackState = PlaybackState.IDLE,
    val error: String? = null
)
