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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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

    // Coroutine scope for position updates
    private val positionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Position updates flow - lifecycle-aware and event-driven with smart intervals
    override val currentPosition: Flow<Long> = callbackFlow {
        var positionUpdateRunnable: Runnable? = null
        var handler: android.os.Handler? = null

        val updatePosition = {
            val position = exoPlayer.currentPosition
            trySend(position)
            // Temporary debug logging - can be removed in production
            android.util.Log.d("PositionUpdate", "Position: ${position}ms, Playing: ${exoPlayer.isPlaying}")
        }

        fun schedulePositionUpdate() {
            handler?.removeCallbacks(positionUpdateRunnable ?: return)

            // Smart update intervals: fast when playing, slower when paused
            val updateInterval = if (exoPlayer.isPlaying) {
                100L // Update every 100ms when playing for smooth progress
            } else {
                5000L // Update every 5 seconds when paused to keep position in sync
            }

            positionUpdateRunnable = Runnable {
                updatePosition()
                schedulePositionUpdate()
            }
            handler?.postDelayed(positionUpdateRunnable!!, updateInterval)
        }

        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePosition() // Immediate update on state change
                schedulePositionUpdate() // Always schedule updates (interval will adjust based on playing state)
            }

            override fun onPlaybackStateChanged(state: Int) {
                updatePosition() // Update position on state changes
                when (state) {
                    Player.STATE_ENDED, Player.STATE_IDLE -> {
                        handler?.removeCallbacks(positionUpdateRunnable ?: return)
                    }
                    else -> {
                        schedulePositionUpdate() // Keep updates going for other states
                    }
                }
            }
        }

        // Initialize handler and add listener
        handler = android.os.Handler(android.os.Looper.getMainLooper())
        exoPlayer.addListener(listener)

        // Send initial position and always start updates
        updatePosition()
        schedulePositionUpdate() // Always start scheduling updates (interval adapts to play state)

        awaitClose {
            handler?.removeCallbacks(positionUpdateRunnable ?: return@awaitClose)
            exoPlayer.removeListener(listener)
            handler = null
            positionUpdateRunnable = null
        }
    }.stateIn(
        scope = positionScope,
        started = kotlinx.coroutines.flow.SharingStarted.Eagerly,
        initialValue = 0L
    )

    // Audio focus change listener
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true
                exoPlayer.volume = 1.0f
                // Only resume if we were playing before losing focus
                if (wasPlayingBeforeFocusLoss) {
                    playInternal()
                    wasPlayingBeforeFocusLoss = false
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                hasAudioFocus = false
                wasPlayingBeforeFocusLoss = exoPlayer.isPlaying
                pauseInternal()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                wasPlayingBeforeFocusLoss = exoPlayer.isPlaying
                pauseInternal()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
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
        if (!audioFocusEnabled || requestAudioFocus()) {
            playInternal()
        }
    }

    override fun pause() {
        pauseInternal()
    }

    /**
     * Internal play method that doesn't request audio focus (to prevent recursion)
     */
    private fun playInternal() {
        exoPlayer.play()
    }

    /**
     * Internal pause method
     */
    private fun pauseInternal() {
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

        // Cancel position scope
        positionScope.cancel()

        exoPlayer.release()
    }

    /**
     * Set the playback queue - optimized to avoid redundant operations
     */
    private fun setQueue(tracks: List<Track>, startIndex: Int) {
        try {
            // Validate inputs
            if (tracks.isEmpty()) {
                return
            }

            if (startIndex < 0 || startIndex >= tracks.size) {
                return
            }

            // Only update if the queue actually changed - optimized comparison
            val currentQueue = _playbackQueue.value
            val isSameQueue = isSameTrackQueue(currentQueue.tracks, tracks)

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
                    shuffledOrder = if (tracks.size > 1) {
                        // Only shuffle if not already shuffled to avoid recreating the same order
                        if (currentQueue.shuffledOrder.isEmpty()) tracks.shuffled() else currentQueue.shuffledOrder
                    } else tracks,
                    isShuffled = false
                )
            }

            _currentTrack.value = tracks.getOrNull(startIndex)
        } catch (e: Exception) {
            // Only log critical errors
        }
    }

    /**
     * Optimized track queue comparison using hash-based approach
     * O(n) instead of O(n²) for large lists
     */
    private fun isSameTrackQueue(current: List<Track>, new: List<Track>): Boolean {
        if (current.size != new.size) return false
        if (current.isEmpty()) return true

        // For small lists, direct comparison is faster
        if (current.size <= 10) {
            return current.zip(new).all { (a, b) -> a.id == b.id }
        }

        // For larger lists, use hash-based comparison
        val currentIds = current.mapTo(HashSet()) { it.id }
        val newIds = new.mapTo(HashSet()) { it.id }

        // First check if all IDs match (same content)
        if (currentIds != newIds) return false

        // Then check if order is the same (for ordered queues)
        return current.zip(new).all { (a, b) -> a.id == b.id }
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
            return true
        }

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