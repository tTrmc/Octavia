package com.octavia.player.domain.usecase

import com.octavia.player.data.model.PlaybackQueue
import com.octavia.player.data.model.Track
import com.octavia.player.domain.repository.MediaPlaybackRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Use case for queue management operations
 * Handles business logic for queue manipulation
 */
class QueueManagementUseCase @Inject constructor(
    private val mediaPlaybackRepository: MediaPlaybackRepository
) {

    /**
     * Expose the current playback queue
     */
    val playbackQueue: StateFlow<PlaybackQueue> = mediaPlaybackRepository.playbackQueue

    /**
     * Add a track to the end of the queue
     */
    suspend fun addToQueue(track: Track) {
        mediaPlaybackRepository.addToQueue(track)
    }

    /**
     * Add a track to play next (after current track)
     */
    suspend fun addToQueueNext(track: Track) {
        mediaPlaybackRepository.addToQueueNext(track)
    }

    /**
     * Add multiple tracks to the end of the queue
     */
    suspend fun addTracksToQueue(tracks: List<Track>) {
        tracks.forEach { track ->
            mediaPlaybackRepository.addToQueue(track)
        }
    }

    /**
     * Insert a track at a specific position in the queue
     */
    suspend fun insertInQueue(track: Track, position: Int) {
        mediaPlaybackRepository.insertInQueue(track, position)
    }

    /**
     * Remove a track from the queue by index
     * Validates that we're not removing the currently playing track
     */
    suspend fun removeFromQueue(index: Int) {
        val currentQueue = playbackQueue.value

        // Business rule: Don't allow removing the currently playing track
        if (index == currentQueue.currentIndex) {
            android.util.Log.w("QueueManagement", "Cannot remove currently playing track")
            return
        }

        mediaPlaybackRepository.removeFromQueue(index)
    }

    /**
     * Move a track from one position to another in the queue
     */
    suspend fun moveInQueue(fromIndex: Int, toIndex: Int) {
        // Validate indices
        val currentQueue = playbackQueue.value
        if (fromIndex < 0 || fromIndex >= currentQueue.tracks.size ||
            toIndex < 0 || toIndex >= currentQueue.tracks.size) {
            android.util.Log.w("QueueManagement", "Invalid move indices")
            return
        }

        mediaPlaybackRepository.moveInQueue(fromIndex, toIndex)
    }

    /**
     * Clear the entire queue and stop playback
     */
    suspend fun clearQueue() {
        mediaPlaybackRepository.clearQueue()
    }

    /**
     * Jump to a specific track in the queue and start playing
     */
    suspend fun jumpToQueueItem(index: Int) {
        val currentQueue = playbackQueue.value
        if (index < 0 || index >= currentQueue.tracks.size) {
            android.util.Log.w("QueueManagement", "Invalid queue index")
            return
        }

        mediaPlaybackRepository.jumpToQueueItem(index)
    }

    /**
     * Get the current queue size
     */
    fun getQueueSize(): Int {
        return playbackQueue.value.size
    }

    /**
     * Check if queue is empty
     */
    fun isQueueEmpty(): Boolean {
        return playbackQueue.value.isEmpty
    }

    /**
     * Get total duration of all tracks in queue
     */
    fun getTotalQueueDuration(): Long {
        return playbackQueue.value.tracks.sumOf { it.durationMs }
    }

    /**
     * Remove all tracks after the current track
     */
    suspend fun clearUpNext() {
        val currentQueue = playbackQueue.value
        val currentIndex = currentQueue.currentIndex

        if (currentIndex < 0 || currentIndex >= currentQueue.tracks.size - 1) {
            return // No tracks after current track
        }

        // Remove tracks from the end backwards to avoid index shifting issues
        for (i in currentQueue.tracks.size - 1 downTo currentIndex + 1) {
            mediaPlaybackRepository.removeFromQueue(i)
        }
    }
}