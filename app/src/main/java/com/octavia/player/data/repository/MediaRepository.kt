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
 * Repository for managing media playback state and operations
 * Acts as a bridge between the UI and Media3 ExoPlayer
 */
@Singleton
class MediaRepository @Inject constructor(
    private val exoPlayer: ExoPlayer,
    private val trackRepository: TrackRepository
) {

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _playbackQueue = MutableStateFlow(PlaybackQueue())
    val playbackQueue: StateFlow<PlaybackQueue> = _playbackQueue.asStateFlow()

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    // Position updates flow
    val currentPosition: Flow<Long> = flow {
        while (true) {
            emit(exoPlayer.currentPosition)
            delay(1000) // Update every second
        }
    }.flowOn(kotlinx.coroutines.Dispatchers.Main)

    init {
        setupPlayerListener()
    }

    /**
     * Play a specific track
     */
    suspend fun playTrack(track: Track) {
        setQueue(listOf(track), 0)
        play()
        trackRepository.incrementPlayCount(track.id)
    }

    /**
     * Play a list of tracks starting from a specific index
     */
    suspend fun playTracks(tracks: List<Track>, startIndex: Int = 0) {
        setQueue(tracks, startIndex)
        play()
        tracks.getOrNull(startIndex)?.let { track ->
            trackRepository.incrementPlayCount(track.id)
        }
    }

    /**
     * Set the playback queue
     */
    private fun setQueue(tracks: List<Track>, startIndex: Int) {
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
            shuffledOrder = tracks.shuffled(),
            isShuffled = false
        )

        _currentTrack.value = tracks.getOrNull(startIndex)
    }

    /**
     * Play/resume playback
     */
    fun play() {
        exoPlayer.play()
    }

    /**
     * Pause playback
     */
    fun pause() {
        exoPlayer.pause()
    }

    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    /**
     * Skip to next track
     */
    suspend fun skipToNext() {
        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNext()
            updateCurrentTrackFromPlayer()
            currentTrack.value?.let { track ->
                trackRepository.incrementPlayCount(track.id)
            }
        }
    }

    /**
     * Skip to previous track
     */
    suspend fun skipToPrevious() {
        if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPrevious()
            updateCurrentTrackFromPlayer()
            currentTrack.value?.let { track ->
                trackRepository.incrementPlayCount(track.id)
            }
        }
    }

    /**
     * Seek to a specific position
     */
    fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
    }

    /**
     * Set repeat mode
     */
    fun setRepeatMode(repeatMode: RepeatMode) {
        val exoRepeatMode = when (repeatMode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
        exoPlayer.repeatMode = exoRepeatMode

        _playerState.value = _playerState.value.copy(repeatMode = repeatMode)
    }

    /**
     * Set shuffle mode
     */
    fun setShuffleMode(shuffleMode: ShuffleMode) {
        val isShuffled = shuffleMode == ShuffleMode.ON
        exoPlayer.shuffleModeEnabled = isShuffled

        _playerState.value = _playerState.value.copy(shuffleMode = shuffleMode)
        _playbackQueue.value = _playbackQueue.value.copy(isShuffled = isShuffled)
    }

    /**
     * Set playback speed
     */
    fun setPlaybackSpeed(speed: Float) {
        exoPlayer.setPlaybackSpeed(speed)
        _playerState.value = _playerState.value.copy(playbackSpeed = speed)
    }

    /**
     * Set volume
     */
    fun setVolume(volume: Float) {
        exoPlayer.volume = volume.coerceIn(0f, 1f)
        _playerState.value = _playerState.value.copy(volume = volume)
    }

    /**
     * Stop playback
     */
    fun stop() {
        exoPlayer.stop()
        _currentTrack.value = null
        _playbackQueue.value = PlaybackQueue()
    }

    /**
     * Release the player
     */
    fun release() {
        exoPlayer.release()
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
