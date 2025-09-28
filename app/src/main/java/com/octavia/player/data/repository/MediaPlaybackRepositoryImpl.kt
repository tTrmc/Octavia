package com.octavia.player.data.repository

import android.content.Context
import android.media.AudioManager
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of MediaPlaybackRepository interface
 * Manages ExoPlayer and playback state
 */
@Singleton
class MediaPlaybackRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exoPlayer: ExoPlayer
) : MediaPlaybackRepository {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _playerState = MutableStateFlow(PlayerState())
    override val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _playbackQueue = MutableStateFlow(PlaybackQueue())
    override val playbackQueue: StateFlow<PlaybackQueue> = _playbackQueue.asStateFlow()

    private val _currentTrack = MutableStateFlow<Track?>(null)
    override val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    // Mutex for thread-safe state updates
    private val stateMutex = Mutex()

    // Player listener to be properly removed
    private var playerListener: Player.Listener? = null

    // Audio focus management
    private var audioFocusRequest: Any? = null
    private var hasAudioFocus = false
    private var wasPlayingBeforeFocusLoss = false
    // Safety switch to disable audio focus temporarily if issues persist
    private val audioFocusEnabled = false

    // Position updates flow - optimized to only emit when playing and needed
    override val currentPosition: Flow<Long> = flow {
        while (true) {
            if (exoPlayer.isPlaying) {
                emit(exoPlayer.currentPosition)
                delay(1000) // Update once per second when playing (reduced frequency)
            } else {
                emit(exoPlayer.currentPosition) // Emit current position even when paused
                delay(2000) // Check less frequently when paused
            }
        }
    }.flowOn(kotlinx.coroutines.Dispatchers.Main)

    // Audio focus change listener
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        android.util.Log.d("MediaPlaybackRepository", "Audio focus changed: $focusChange")
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                android.util.Log.d("MediaPlaybackRepository", "Audio focus gained")
                hasAudioFocus = true
                exoPlayer.volume = 1.0f
                // Only resume if we were playing before losing focus
                if (wasPlayingBeforeFocusLoss) {
                    android.util.Log.d("MediaPlaybackRepository", "Resuming playback after focus gain")
                    playInternal()
                    wasPlayingBeforeFocusLoss = false
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                android.util.Log.d("MediaPlaybackRepository", "Audio focus lost permanently")
                hasAudioFocus = false
                wasPlayingBeforeFocusLoss = exoPlayer.isPlaying
                pauseInternal()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                android.util.Log.d("MediaPlaybackRepository", "Audio focus lost temporarily")
                wasPlayingBeforeFocusLoss = exoPlayer.isPlaying
                pauseInternal()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                android.util.Log.d("MediaPlaybackRepository", "Audio focus lost - ducking")
                // Duck audio instead of pausing for short interruptions
                exoPlayer.volume = 0.3f
            }
        }
    }

    init {
        setupPlayerListener()
        setupAudioFocus()
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
        android.util.Log.d("MediaPlaybackRepository", "play() called")
        if (!audioFocusEnabled || requestAudioFocus()) {
            playInternal()
        } else {
            android.util.Log.w("MediaPlaybackRepository", "Failed to gain audio focus")
        }
    }

    override fun pause() {
        android.util.Log.d("MediaPlaybackRepository", "pause() called")
        pauseInternal()
    }

    /**
     * Internal play method that doesn't request audio focus (to prevent recursion)
     */
    private fun playInternal() {
        android.util.Log.d("MediaPlaybackRepository", "playInternal() - starting playback")
        exoPlayer.play()
    }

    /**
     * Internal pause method
     */
    private fun pauseInternal() {
        android.util.Log.d("MediaPlaybackRepository", "pauseInternal() - pausing playback")
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
        updatePlayerState { it.copy(repeatMode = repeatMode) }
    }

    override fun setShuffleMode(shuffleMode: ShuffleMode) {
        val isShuffled = shuffleMode == ShuffleMode.ON
        exoPlayer.shuffleModeEnabled = isShuffled
        updatePlayerState { it.copy(shuffleMode = shuffleMode) }
        updatePlaybackQueue { it.copy(isShuffled = isShuffled) }
    }

    override fun setPlaybackSpeed(speed: Float) {
        exoPlayer.setPlaybackSpeed(speed)
        updatePlayerState { it.copy(playbackSpeed = speed) }
    }

    override fun setVolume(volume: Float) {
        exoPlayer.volume = volume.coerceIn(0f, 1f)
        updatePlayerState { it.copy(volume = volume) }
    }

    override fun stop() {
        exoPlayer.stop()
        _currentTrack.value = null
        _playbackQueue.value = PlaybackQueue()
    }

    override fun release() {
        // Release audio focus
        abandonAudioFocus()

        // Remove listener before releasing
        playerListener?.let { exoPlayer.removeListener(it) }
        playerListener = null
        exoPlayer.release()
    }

    /**
     * Set the playback queue - optimized to avoid redundant operations
     */
    private fun setQueue(tracks: List<Track>, startIndex: Int) {
        try {
            // Validate inputs
            if (tracks.isEmpty()) {
                android.util.Log.w("MediaPlaybackRepository", "Attempted to set empty queue")
                return
            }

            if (startIndex < 0 || startIndex >= tracks.size) {
                android.util.Log.w("MediaPlaybackRepository", "Invalid start index: $startIndex for ${tracks.size} tracks")
                return
            }

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

            updatePlaybackQueue {
                PlaybackQueue(
                    tracks = tracks,
                    currentIndex = startIndex,
                    originalOrder = tracks,
                    shuffledOrder = if (tracks.size > 1) tracks.shuffled() else tracks,
                    isShuffled = false
                )
            }

            _currentTrack.value = tracks.getOrNull(startIndex)
        } catch (e: Exception) {
            android.util.Log.e("MediaPlaybackRepository", "Failed to set playback queue", e)
        }
    }

    /**
     * Thread-safe state update helper methods
     */
    private fun updatePlayerState(update: (PlayerState) -> PlayerState) {
        _playerState.value = update(_playerState.value)
    }

    private fun updatePlaybackQueue(update: (PlaybackQueue) -> PlaybackQueue) {
        _playbackQueue.value = update(_playbackQueue.value)
    }

    /**
     * Setup player event listener
     */
    private fun setupPlayerListener() {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlayerState { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlaybackStateChanged(state: Int) {
                val playbackState = when (state) {
                    Player.STATE_IDLE -> PlaybackState.IDLE
                    Player.STATE_BUFFERING -> PlaybackState.BUFFERING
                    Player.STATE_READY -> PlaybackState.READY
                    Player.STATE_ENDED -> PlaybackState.ENDED
                    else -> PlaybackState.IDLE
                }

                updatePlayerState {
                    it.copy(
                        playbackState = playbackState,
                        duration = if (exoPlayer.duration > 0) exoPlayer.duration else 0L
                    )
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateCurrentTrackFromPlayer()
            }
        }

        playerListener = listener
        exoPlayer.addListener(listener)
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

    /**
     * Audio focus management methods
     */
    private fun setupAudioFocus() {
        // Audio focus is set up with the listener
        // Actual focus request happens when play() is called
    }

    private fun requestAudioFocus(): Boolean {
        if (hasAudioFocus) {
            android.util.Log.d("MediaPlaybackRepository", "Already have audio focus")
            return true
        }

        android.util.Log.d("MediaPlaybackRepository", "Requesting audio focus...")
        val result = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val focusRequest = android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()

            audioFocusRequest = focusRequest
            audioManager.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }

        hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        android.util.Log.d("MediaPlaybackRepository", "Audio focus request result: $result, hasAudioFocus: $hasAudioFocus")
        return hasAudioFocus
    }

    private fun abandonAudioFocus() {
        if (!hasAudioFocus) return

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            audioFocusRequest?.let { request ->
                audioManager.abandonAudioFocusRequest(request as android.media.AudioFocusRequest)
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }

        hasAudioFocus = false
        audioFocusRequest = null
    }
}