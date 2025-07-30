package com.octavia.player.domain.usecase

import com.octavia.player.data.model.RepeatMode
import com.octavia.player.data.model.ShuffleMode
import com.octavia.player.data.model.Track
import com.octavia.player.domain.repository.MediaPlaybackRepository
import com.octavia.player.domain.repository.TrackRepository
import javax.inject.Inject

/**
 * Use case for media playback control operations
 * Combines playback and track statistics
 */
class PlaybackControlUseCase @Inject constructor(
    private val mediaPlaybackRepository: MediaPlaybackRepository,
    private val trackRepository: TrackRepository
) {
    
    suspend fun playTrack(track: Track) {
        mediaPlaybackRepository.playTrack(track)
        trackRepository.incrementPlayCount(track.id)
    }
    
    suspend fun playTracks(tracks: List<Track>, startIndex: Int = 0) {
        mediaPlaybackRepository.playTracks(tracks, startIndex)
        tracks.getOrNull(startIndex)?.let { track ->
            trackRepository.incrementPlayCount(track.id)
        }
    }
    
    fun togglePlayPause() = mediaPlaybackRepository.togglePlayPause()
    
    suspend fun skipToNext() = mediaPlaybackRepository.skipToNext()
    
    suspend fun skipToPrevious() = mediaPlaybackRepository.skipToPrevious()
    
    fun seekTo(position: Long) = mediaPlaybackRepository.seekTo(position)
    
    fun setRepeatMode(repeatMode: RepeatMode) = 
        mediaPlaybackRepository.setRepeatMode(repeatMode)
    
    fun setShuffleMode(shuffleMode: ShuffleMode) = 
        mediaPlaybackRepository.setShuffleMode(shuffleMode)
    
    fun setPlaybackSpeed(speed: Float) = 
        mediaPlaybackRepository.setPlaybackSpeed(speed)
    
    fun setVolume(volume: Float) = 
        mediaPlaybackRepository.setVolume(volume)
    
    suspend fun toggleFavorite(track: Track) {
        trackRepository.updateFavoriteStatus(track.id, !track.isFavorite)
    }
}