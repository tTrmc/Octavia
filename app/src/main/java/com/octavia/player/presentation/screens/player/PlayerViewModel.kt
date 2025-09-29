package com.octavia.player.presentation.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.octavia.player.data.model.PlaybackState
import com.octavia.player.data.model.RepeatMode
import com.octavia.player.data.model.ShuffleMode
import com.octavia.player.data.model.Track
import com.octavia.player.domain.repository.MediaPlaybackRepository
import com.octavia.player.domain.usecase.PlaybackControlUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Player screen
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val mediaPlaybackRepository: MediaPlaybackRepository,
    private val playbackControlUseCase: PlaybackControlUseCase
) : ViewModel() {

    val uiState: StateFlow<PlayerUiState> = combine(
        mediaPlaybackRepository.playerState,
        mediaPlaybackRepository.currentTrack,
        mediaPlaybackRepository.currentPosition
    ) { playerState, currentTrack, currentPosition ->
        PlayerUiState(
            currentTrack = currentTrack,
            isPlaying = playerState.isPlaying,
            playbackState = playerState.playbackState,
            currentPosition = currentPosition,
            duration = playerState.duration,
            progress = if (playerState.duration > 0) currentPosition.toFloat() / playerState.duration else 0f,
            repeatMode = playerState.repeatMode,
            shuffleMode = playerState.shuffleMode,
            playbackSpeed = playerState.playbackSpeed,
            volume = playerState.volume
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PlayerUiState()
    )

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

    fun seekTo(position: Long) {
        playbackControlUseCase.seekTo(position)
    }

    fun toggleShuffle() {
        val currentShuffle = uiState.value.shuffleMode
        val newShuffle = if (currentShuffle == ShuffleMode.ON) ShuffleMode.OFF else ShuffleMode.ON
        playbackControlUseCase.setShuffleMode(newShuffle)
    }

    fun toggleRepeat() {
        val currentRepeat = uiState.value.repeatMode
        val newRepeat = when (currentRepeat) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        playbackControlUseCase.setRepeatMode(newRepeat)
    }

    fun setPlaybackSpeed(speed: Float) {
        playbackControlUseCase.setPlaybackSpeed(speed)
    }

    fun setVolume(volume: Float) {
        playbackControlUseCase.setVolume(volume)
    }
}

/**
 * UI state for the Player screen
 */
data class PlayerUiState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val playbackState: PlaybackState = PlaybackState.IDLE,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val progress: Float = 0f,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleMode: ShuffleMode = ShuffleMode.OFF,
    val playbackSpeed: Float = 1.0f,
    val volume: Float = 1.0f
)
