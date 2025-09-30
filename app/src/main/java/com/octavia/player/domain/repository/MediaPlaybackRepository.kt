package com.octavia.player.domain.repository

import com.octavia.player.data.model.PlaybackQueue
import com.octavia.player.data.model.PlayerState
import com.octavia.player.data.model.RepeatMode
import com.octavia.player.data.model.ShuffleMode
import com.octavia.player.data.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for media playback operations
 * Separates playback logic from data persistence
 */
interface MediaPlaybackRepository {
    
    // State observables
    val playerState: StateFlow<PlayerState>
    val playbackQueue: StateFlow<PlaybackQueue>
    val currentTrack: StateFlow<Track?>
    val currentPosition: Flow<Long>
    
    // Playback controls
    suspend fun playTrack(track: Track)
    suspend fun playTracks(tracks: List<Track>, startIndex: Int = 0)
    fun play()
    fun pause()
    fun togglePlayPause()
    fun stop()
    
    // Navigation
    suspend fun skipToNext()
    suspend fun skipToPrevious()
    fun seekTo(position: Long)
    
    // Settings
    fun setRepeatMode(repeatMode: RepeatMode)
    fun setShuffleMode(shuffleMode: ShuffleMode)
    fun setPlaybackSpeed(speed: Float)
    fun setVolume(volume: Float)

    // Queue management
    suspend fun addToQueue(track: Track)
    suspend fun addToQueueNext(track: Track)
    suspend fun insertInQueue(track: Track, position: Int)
    suspend fun removeFromQueue(index: Int)
    suspend fun moveInQueue(fromIndex: Int, toIndex: Int)
    suspend fun clearQueue()
    suspend fun jumpToQueueItem(index: Int)

    // Lifecycle
    fun release()
}