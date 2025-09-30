package com.octavia.player.presentation.screens.artist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.octavia.player.data.model.Artist
import com.octavia.player.data.model.PlaybackState
import com.octavia.player.data.model.Track
import com.octavia.player.domain.repository.MediaPlaybackRepository
import com.octavia.player.domain.usecase.GetArtistsUseCase
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
 * ViewModel for the Artist Detail screen
 * Manages artist state, track list, and playback integration
 */
@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    private val getArtistsUseCase: GetArtistsUseCase,
    private val playbackControlUseCase: PlaybackControlUseCase,
    private val mediaPlaybackRepository: MediaPlaybackRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val artistId: Long = savedStateHandle.get<String>("artistId")?.toLongOrNull() ?: 0L

    private val _errorMessage = MutableStateFlow<String?>(null)

    // Flow that emits artist with tracks
    private val artistWithTracks = flow {
        val (artist, tracks) = getArtistsUseCase.getArtistWithTracks(artistId)
        emit(Pair(artist, tracks))
    }

    val uiState: StateFlow<ArtistDetailUiState> = combine(
        artistWithTracks,
        mediaPlaybackRepository.currentTrack,
        mediaPlaybackRepository.playerState,
        _errorMessage
    ) { (artist, tracks), currentTrack, playerState, error ->
        ArtistDetailUiState(
            artist = artist,
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
        initialValue = ArtistDetailUiState(isLoading = true)
    )

    init {
        android.util.Log.d("ArtistDetailVM", "Initialized with artist ID: $artistId")
    }

    // Playback methods

    /**
     * Play all tracks by the artist
     */
    fun playAllTracks(shuffled: Boolean = false) {
        viewModelScope.launch {
            val tracks = uiState.value.tracks
            if (tracks.isEmpty()) {
                _errorMessage.value = "Artist has no tracks"
                return@launch
            }

            val tracksToPlay = if (shuffled) tracks.shuffled() else tracks
            android.util.Log.d("ArtistDetailVM", "Playing artist with ${tracksToPlay.size} tracks (shuffled: $shuffled)")
            playbackControlUseCase.playTracks(tracksToPlay, 0)
        }
    }

    /**
     * Play a specific track from the artist
     */
    fun playTrack(track: Track) {
        viewModelScope.launch {
            val tracks = uiState.value.tracks
            val trackIndex = tracks.indexOf(track)

            if (trackIndex >= 0) {
                android.util.Log.d("ArtistDetailVM", "Playing track '${track.displayTitle}' at index $trackIndex")
                playbackControlUseCase.playTracks(tracks, trackIndex)
            } else {
                android.util.Log.w("ArtistDetailVM", "Track not found for artist, playing standalone")
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
 * UI state for the Artist Detail screen
 */
data class ArtistDetailUiState(
    val artist: Artist? = null,
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = true,
    val isPlaying: Boolean = false,
    val currentlyPlayingTrack: Track? = null,
    val playbackState: PlaybackState = PlaybackState.IDLE,
    val error: String? = null
)
