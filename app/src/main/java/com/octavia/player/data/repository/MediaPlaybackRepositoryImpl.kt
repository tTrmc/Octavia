package com.octavia.player.data.repository

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.octavia.player.data.model.PlaybackQueue
import com.octavia.player.data.model.PlaybackState
import com.octavia.player.data.model.PlayerState
import com.octavia.player.data.model.RepeatMode
import com.octavia.player.data.model.ShuffleMode
import com.octavia.player.data.model.Track
import com.octavia.player.domain.repository.MediaPlaybackRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of MediaPlaybackRepository interface
 * Manages ExoPlayer and playback state
 */
@Singleton
class MediaPlaybackRepositoryImpl @Inject constructor(
    private val exoPlayer: ExoPlayer
) : MediaPlaybackRepository {

    private val _playerState = MutableStateFlow(PlayerState())
    override val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _playbackQueue = MutableStateFlow(PlaybackQueue())
    override val playbackQueue: StateFlow<PlaybackQueue> = _playbackQueue.asStateFlow()

    private val _currentTrack = MutableStateFlow<Track?>(null)
    override val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    // Position updates flow - optimized to only emit when playing
    override val currentPosition: Flow<Long> = flow {
        while (true) {
            if (exoPlayer.isPlaying) {
                emit(exoPlayer.currentPosition)
                delay(500) // Update twice per second when playing
            } else {
                delay(1000) // Check less frequently when paused
            }
        }
    }.flowOn(kotlinx.coroutines.Dispatchers.Main)

    init {
        setupPlayerListener()
    }

    override suspend fun playTrack(track: Track) {
        setQueue(listOf(track), 0)
        play()
    }

    override suspend fun playTracks(tracks: List<Track>, startIndex: Int) {
        setQueue(tracks, startIndex)
        play()
    }

    override fun play() {
        exoPlayer.play()
    }

    override fun pause() {
        exoPlayer.pause()
    }

    override fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    override suspend fun skipToNext() {
        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNext()
            updateCurrentTrackFromPlayer()
        }
    }

    override suspend fun skipToPrevious() {
        if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPrevious()
            updateCurrentTrackFromPlayer()
        }
    }

    override fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
    }

    override fun setRepeatMode(repeatMode: RepeatMode) {
        val exoRepeatMode = when (repeatMode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
        exoPlayer.repeatMode = exoRepeatMode
        _playerState.value = _playerState.value.copy(repeatMode = repeatMode)
    }

    override fun setShuffleMode(shuffleMode: ShuffleMode) {
        val isShuffled = shuffleMode == ShuffleMode.ON
        exoPlayer.shuffleModeEnabled = isShuffled
        _playerState.value = _playerState.value.copy(shuffleMode = shuffleMode)
        _playbackQueue.value = _playbackQueue.value.copy(isShuffled = isShuffled)
    }

    override fun setPlaybackSpeed(speed: Float) {
        exoPlayer.setPlaybackSpeed(speed)
        _playerState.value = _playerState.value.copy(playbackSpeed = speed)
    }

    override fun setVolume(volume: Float) {
        exoPlayer.volume = volume.coerceIn(0f, 1f)
        _playerState.value = _playerState.value.copy(volume = volume)
    }

    override fun stop() {
        exoPlayer.stop()
        _currentTrack.value = null
        _playbackQueue.value = PlaybackQueue()
    }

    override fun release() {
        exoPlayer.release()
    }

    /**
     * Set the playback queue - optimized to avoid redundant operations
     */
    private fun setQueue(tracks: List<Track>, startIndex: Int) {
        // Only update if the queue actually changed
        val currentQueue = _playbackQueue.value
        val isSameQueue = currentQueue.tracks.size == tracks.size && 
                         currentQueue.tracks.zip(tracks).all { (a, b) -> a.id == b.id }
        
        if (isSameQueue && currentQueue.currentIndex == startIndex) {
            // Just update the current track without rebuilding queue
            _currentTrack.value = tracks.getOrNull(startIndex)
            return
        }
        
        val mediaItems = tracks.map { track ->
            MediaItem.Builder()
                .setUri(track.filePath)
                .setMediaId(track.id.toString())
                .build()
        }

        exoPlayer.setMediaItems(mediaItems, startIndex, 0L)
        exoPlayer.prepare()

        _playbackQueue.value = PlaybackQueue(
            tracks = tracks,
            currentIndex = startIndex,
            originalOrder = tracks,
            shuffledOrder = if (tracks.size > 1) tracks.shuffled() else tracks,
            isShuffled = false
        )

        _currentTrack.value = tracks.getOrNull(startIndex)
    }

    /**
     * Setup player event listener
     */
    private fun setupPlayerListener() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playerState.value = _playerState.value.copy(isPlaying = isPlaying)
            }

            override fun onPlaybackStateChanged(state: Int) {
                val playbackState = when (state) {
                    Player.STATE_IDLE -> PlaybackState.IDLE
                    Player.STATE_BUFFERING -> PlaybackState.BUFFERING
                    Player.STATE_READY -> PlaybackState.READY
                    Player.STATE_ENDED -> PlaybackState.ENDED
                    else -> PlaybackState.IDLE
                }

                _playerState.value = _playerState.value.copy(
                    playbackState = playbackState,
                    duration = if (exoPlayer.duration > 0) exoPlayer.duration else 0L
                )
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateCurrentTrackFromPlayer()
            }
        })
    }

    /**
     * Update current track based on player state
     */
    private fun updateCurrentTrackFromPlayer() {
        val currentIndex = exoPlayer.currentMediaItemIndex
        val queue = _playbackQueue.value

        if (currentIndex >= 0 && currentIndex < queue.tracks.size) {
            _currentTrack.value = queue.tracks[currentIndex]
            _playbackQueue.value = queue.copy(currentIndex = currentIndex)
        }
    }
}