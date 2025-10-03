package com.octavia.player.data.repository

import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Implementation of MediaPlaybackRepository interface
 * Manages ExoPlayer and playback state
 */
@Singleton
class MediaPlaybackRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exoPlayer: ExoPlayer,
    @Named("ApplicationScope") private val applicationScope: CoroutineScope,
    private val playbackStateDataStore: com.octavia.player.data.datastore.PlaybackStateDataStore
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
    private val audioFocusEnabled = true

    // Use injected application scope for position updates to ensure proper lifecycle management

    // Position updates flow - lifecycle-aware and throttled to avoid hammering the main thread
    override val currentPosition: Flow<Long> = callbackFlow {
        android.util.Log.d("MediaPlayback", "Position flow started")
        var lastEmissionTime = 0L

        val listener = object : Player.Listener {
            private fun emitPosition(force: Boolean = false) {
                val now = SystemClock.elapsedRealtime()
                val minimumInterval = if (exoPlayer.isPlaying) 250L else 1000L

                if (force || now - lastEmissionTime >= minimumInterval) {
                    lastEmissionTime = now
                    trySend(exoPlayer.currentPosition)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                android.util.Log.d("MediaPlayback", "Playing state changed: $isPlaying")
                emitPosition(force = true)
            }

            override fun onPlaybackStateChanged(state: Int) {
                android.util.Log.d("MediaPlayback", "Playback state changed: $state")
                emitPosition(force = true)
            }

            override fun onEvents(player: Player, events: Player.Events) {
                if (events.contains(Player.EVENT_POSITION_DISCONTINUITY) ||
                    events.contains(Player.EVENT_TIMELINE_CHANGED) ||
                    events.contains(Player.EVENT_PLAYBACK_PARAMETERS_CHANGED)
                ) {
                    emitPosition(force = true)
                } else {
                    emitPosition()
                }
            }
        }

        exoPlayer.addListener(listener)
        trySend(exoPlayer.currentPosition)

        awaitClose {
            android.util.Log.d("MediaPlayback", "Position flow closed")
            exoPlayer.removeListener(listener)
        }
    }.stateIn(
        scope = applicationScope,
        started = kotlinx.coroutines.flow.SharingStarted.Lazily,
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
        android.util.Log.d("MediaPlayback", "skipToNext() called")

        val currentMediaItemCount = exoPlayer.mediaItemCount
        val currentMediaItemIndex = exoPlayer.currentMediaItemIndex

        android.util.Log.d("MediaPlayback", "Current queue: $currentMediaItemIndex of $currentMediaItemCount items")

        if (exoPlayer.hasNextMediaItem()) {
            android.util.Log.d("MediaPlayback", "Skipping to next track")
            exoPlayer.seekToNext()
            updateCurrentTrackFromPlayer()
            android.util.Log.d("MediaPlayback", "Skipped to track ${exoPlayer.currentMediaItemIndex}")
        } else {
            android.util.Log.w("MediaPlayback", "No next track available - already at end of queue")
        }
    }

    override suspend fun skipToPrevious() {
        android.util.Log.d("MediaPlayback", "skipToPrevious() called")

        val currentMediaItemCount = exoPlayer.mediaItemCount
        val currentMediaItemIndex = exoPlayer.currentMediaItemIndex

        android.util.Log.d("MediaPlayback", "Current queue: $currentMediaItemIndex of $currentMediaItemCount items")

        if (exoPlayer.hasPreviousMediaItem()) {
            android.util.Log.d("MediaPlayback", "Skipping to previous track")
            exoPlayer.seekToPrevious()
            updateCurrentTrackFromPlayer()
            android.util.Log.d("MediaPlayback", "Skipped to track ${exoPlayer.currentMediaItemIndex}")
        } else {
            android.util.Log.w("MediaPlayback", "No previous track available - already at beginning of queue")
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
        if (audioFocusEnabled) {
            abandonAudioFocus()
        }
        _currentTrack.value = null
        _playbackQueue.value = PlaybackQueue()
    }

    override suspend fun addToQueue(track: Track) = stateMutex.withLock {
        val currentQueue = _playbackQueue.value
        val updatedTracks = currentQueue.tracks + track

        android.util.Log.d("MediaPlayback", "addToQueue() - Adding ${track.displayTitle} to queue")

        // Create new media item for ExoPlayer
        val mediaItem = MediaItem.Builder()
            .setUri(track.filePath)
            .setMediaId(track.id.toString())
            .build()

        exoPlayer.addMediaItem(mediaItem)

        updatePlaybackQueue {
            it.copy(tracks = updatedTracks)
        }
    }

    override suspend fun addToQueueNext(track: Track) = stateMutex.withLock {
        val currentQueue = _playbackQueue.value
        val currentIndex = currentQueue.currentIndex
        val insertPosition = if (currentIndex >= 0) currentIndex + 1 else 0

        android.util.Log.d("MediaPlayback", "addToQueueNext() - Adding ${track.displayTitle} at position $insertPosition")

        insertInQueueInternal(track, insertPosition)
    }

    override suspend fun insertInQueue(track: Track, position: Int) = stateMutex.withLock {
        val currentQueue = _playbackQueue.value
        val validPosition = position.coerceIn(0, currentQueue.tracks.size)

        android.util.Log.d("MediaPlayback", "insertInQueue() - Inserting ${track.displayTitle} at position $validPosition")

        insertInQueueInternal(track, validPosition)
    }

    private fun insertInQueueInternal(track: Track, position: Int) {
        val currentQueue = _playbackQueue.value
        val updatedTracks = currentQueue.tracks.toMutableList().apply {
            add(position, track)
        }

        // Create new media item for ExoPlayer
        val mediaItem = MediaItem.Builder()
            .setUri(track.filePath)
            .setMediaId(track.id.toString())
            .build()

        exoPlayer.addMediaItem(position, mediaItem)

        // Update current index if insertion affects it
        val newCurrentIndex = if (position <= currentQueue.currentIndex) {
            currentQueue.currentIndex + 1
        } else {
            currentQueue.currentIndex
        }

        updatePlaybackQueue {
            it.copy(tracks = updatedTracks, currentIndex = newCurrentIndex)
        }
    }

    override suspend fun removeFromQueue(index: Int) = stateMutex.withLock {
        val currentQueue = _playbackQueue.value

        if (index < 0 || index >= currentQueue.tracks.size) {
            android.util.Log.w("MediaPlayback", "removeFromQueue() - Invalid index: $index")
            return@withLock
        }

        // Don't allow removing the currently playing track
        if (index == currentQueue.currentIndex) {
            android.util.Log.w("MediaPlayback", "removeFromQueue() - Cannot remove currently playing track")
            return@withLock
        }

        android.util.Log.d("MediaPlayback", "removeFromQueue() - Removing track at index $index")

        val updatedTracks = currentQueue.tracks.toMutableList().apply {
            removeAt(index)
        }

        exoPlayer.removeMediaItem(index)

        // Update current index if removal affects it
        val newCurrentIndex = when {
            index < currentQueue.currentIndex -> currentQueue.currentIndex - 1
            else -> currentQueue.currentIndex
        }

        updatePlaybackQueue {
            it.copy(tracks = updatedTracks, currentIndex = newCurrentIndex)
        }
    }

    override suspend fun moveInQueue(fromIndex: Int, toIndex: Int) = stateMutex.withLock {
        val currentQueue = _playbackQueue.value

        if (fromIndex < 0 || fromIndex >= currentQueue.tracks.size ||
            toIndex < 0 || toIndex >= currentQueue.tracks.size) {
            android.util.Log.w("MediaPlayback", "moveInQueue() - Invalid indices: from=$fromIndex, to=$toIndex")
            return@withLock
        }

        if (fromIndex == toIndex) {
            android.util.Log.d("MediaPlayback", "moveInQueue() - Same position, no move needed")
            return@withLock
        }

        android.util.Log.d("MediaPlayback", "moveInQueue() - Moving track from $fromIndex to $toIndex")

        val updatedTracks = currentQueue.tracks.toMutableList()
        val trackToMove = updatedTracks.removeAt(fromIndex)
        updatedTracks.add(toIndex, trackToMove)

        exoPlayer.moveMediaItem(fromIndex, toIndex)

        // Calculate new current index after move
        val currentIndex = currentQueue.currentIndex
        val newCurrentIndex = when {
            currentIndex == fromIndex -> toIndex
            fromIndex < currentIndex && toIndex >= currentIndex -> currentIndex - 1
            fromIndex > currentIndex && toIndex <= currentIndex -> currentIndex + 1
            else -> currentIndex
        }

        updatePlaybackQueue {
            it.copy(tracks = updatedTracks, currentIndex = newCurrentIndex)
        }
    }

    override suspend fun clearQueue() = stateMutex.withLock {
        android.util.Log.d("MediaPlayback", "clearQueue() - Clearing all tracks from queue")

        stop()
    }

    override suspend fun jumpToQueueItem(index: Int) = stateMutex.withLock {
        val currentQueue = _playbackQueue.value

        if (index < 0 || index >= currentQueue.tracks.size) {
            android.util.Log.w("MediaPlayback", "jumpToQueueItem() - Invalid index: $index")
            return@withLock
        }

        android.util.Log.d("MediaPlayback", "jumpToQueueItem() - Jumping to track at index $index")

        exoPlayer.seekTo(index, 0L)
        exoPlayer.prepare()

        updatePlaybackQueue {
            it.copy(currentIndex = index)
        }

        _currentTrack.value = currentQueue.tracks.getOrNull(index)

        // Start playing if not already playing
        if (!exoPlayer.isPlaying) {
            play()
        }
    }

    override suspend fun savePlaybackState() {
        try {
            val currentTrack = _currentTrack.value
            val queue = _playbackQueue.value

            if (currentTrack != null && queue.tracks.isNotEmpty()) {
                playbackStateDataStore.savePlaybackState(
                    trackId = currentTrack.id,
                    position = exoPlayer.currentPosition,
                    queueTrackIds = queue.tracks.map { it.id },
                    queueIndex = queue.currentIndex
                )
                android.util.Log.d("MediaPlayback", "Saved playback state: track=${currentTrack.displayTitle}, position=${exoPlayer.currentPosition}")
            }
        } catch (e: Exception) {
            android.util.Log.e("MediaPlayback", "Failed to save playback state", e)
        }
    }

    override suspend fun restorePlaybackState() {
        try {
            val savedState = playbackStateDataStore.getPlaybackState().firstOrNull()
            if (savedState != null) {
                android.util.Log.d(
                    "MediaPlayback",
                    "Restoring playback state: trackId=${savedState.trackId}, position=${savedState.position}"
                )
                // Actual restoration happens in higher-level ViewModels once track data is available
            } else {
                android.util.Log.d("MediaPlayback", "No saved playback state found")
            }
        } catch (e: Exception) {
            android.util.Log.e("MediaPlayback", "Failed to restore playback state", e)
        }
    }

    override fun release() {
        // Release audio focus
        abandonAudioFocus()

        // Remove listener before releasing
        playerListener?.let { exoPlayer.removeListener(it) }
        playerListener = null

        // Position flow is managed by applicationScope, no manual cleanup needed

        exoPlayer.release()
    }

    /**
     * Set the playback queue - optimized to avoid redundant operations
     */
    private fun setQueue(tracks: List<Track>, startIndex: Int) {
        try {
            android.util.Log.d("MediaPlayback", "setQueue() called with ${tracks.size} tracks, startIndex: $startIndex")

            // Validate inputs
            if (tracks.isEmpty()) {
                android.util.Log.w("MediaPlayback", "setQueue() - tracks list is empty, returning")
                return
            }

            if (startIndex < 0 || startIndex >= tracks.size) {
                android.util.Log.w("MediaPlayback", "setQueue() - invalid startIndex: $startIndex for ${tracks.size} tracks, returning")
                return
            }

            // Only update if the queue actually changed - optimized comparison
            val currentQueue = _playbackQueue.value
            val isSameQueue = isSameTrackQueue(currentQueue.tracks, tracks)

            if (isSameQueue && currentQueue.currentIndex == startIndex) {
                // Just update the current track without rebuilding queue
                android.util.Log.d("MediaPlayback", "setQueue() - same queue and index, just updating current track")
                _currentTrack.value = tracks.getOrNull(startIndex)
                return
            }

            android.util.Log.d("MediaPlayback", "setQueue() - creating new queue with ${tracks.size} tracks")
            val mediaItems = tracks.map { track ->
                MediaItem.Builder()
                    .setUri(track.filePath)
                    .setMediaId(track.id.toString())
                    .build()
            }

            android.util.Log.d("MediaPlayback", "setQueue() - setting ExoPlayer media items and preparing")
            exoPlayer.setMediaItems(mediaItems, startIndex, 0L)
            exoPlayer.prepare()

            val isShuffleEnabled = exoPlayer.shuffleModeEnabled
            val shuffledOrder = if (isShuffleEnabled && tracks.size > 1) {
                tracks.shuffled()
            } else {
                tracks
            }

            updatePlaybackQueue {
                PlaybackQueue(
                    tracks = tracks,
                    currentIndex = startIndex,
                    originalOrder = tracks,
                    shuffledOrder = shuffledOrder,
                    isShuffled = isShuffleEnabled
                )
            }

            _currentTrack.value = tracks.getOrNull(startIndex)
            android.util.Log.d("MediaPlayback", "setQueue() - completed successfully, current track: ${tracks.getOrNull(startIndex)?.displayTitle}")
        } catch (e: Exception) {
            android.util.Log.e("MediaPlayback", "setQueue() - error occurred", e)
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
