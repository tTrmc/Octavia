package com.octavia.player.presentation.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.octavia.player.data.model.PlaybackQueue
import com.octavia.player.data.model.PlaybackState
import com.octavia.player.data.model.RepeatMode
import com.octavia.player.data.model.ShuffleMode
import com.octavia.player.data.model.Track
import com.octavia.player.domain.repository.MediaPlaybackRepository
import com.octavia.player.domain.usecase.PlaybackControlUseCase
import com.octavia.player.domain.usecase.QueueManagementUseCase
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
    private val playbackControlUseCase: PlaybackControlUseCase,
    private val queueManagementUseCase: QueueManagementUseCase
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
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlayerUiState()
    )

    /**
     * Expose the playback queue
     */
    val playbackQueue: StateFlow<PlaybackQueue> = queueManagementUseCase.playbackQueue

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

    // Queue management methods

    fun addToQueue(track: Track) {
        viewModelScope.launch {
            queueManagementUseCase.addToQueue(track)
        }
    }

    fun addToQueueNext(track: Track) {
        viewModelScope.launch {
            queueManagementUseCase.addToQueueNext(track)
        }
    }

    fun removeFromQueue(index: Int) {
        viewModelScope.launch {
            queueManagementUseCase.removeFromQueue(index)
        }
    }

    fun moveInQueue(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            queueManagementUseCase.moveInQueue(fromIndex, toIndex)
        }
    }

    fun clearQueue() {
        viewModelScope.launch {
            queueManagementUseCase.clearQueue()
        }
    }

    fun jumpToQueueItem(index: Int) {
        viewModelScope.launch {
            queueManagementUseCase.jumpToQueueItem(index)
        }
    }

    fun clearUpNext() {
        viewModelScope.launch {
            queueManagementUseCase.clearUpNext()
        }
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
