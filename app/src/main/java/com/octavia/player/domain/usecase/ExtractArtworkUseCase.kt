package com.octavia.player.domain.usecase

import android.content.Context
import com.octavia.player.data.model.Track
import com.octavia.player.data.scanner.ArtworkExtractor
import com.octavia.player.data.scanner.ArtworkExtractionProgress
import com.octavia.player.data.scanner.ArtworkCacheStats
import com.octavia.player.domain.repository.TrackRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/**
 * Use case for extracting artwork from music files
 * Now directly uses ArtworkExtractor for better performance
 */
class ExtractArtworkUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackRepository: TrackRepository
) {

    /**
     * Extract artwork for a single track
     * @param track The track to extract artwork for
     * @param maxRetries Maximum number of retry attempts
     * @return The artwork path if successful, null otherwise
     */
    suspend fun extractForTrack(track: Track, maxRetries: Int = 2): Result<String?> {
        val result = ArtworkExtractor.extractArtworkForTrack(context, track, maxRetries)

        // Update database if successful
        result.getOrNull()?.let { artworkPath ->
            val updatedTrack = track.copy(artworkPath = artworkPath)
            trackRepository.updateTrack(updatedTrack)
        }

        return result
    }

    /**
     * Extract artwork for multiple tracks with progress reporting
     * @param tracks List of tracks to extract artwork for
     * @return Flow of progress updates
     */
    fun extractForTracks(tracks: List<Track>): Flow<ArtworkExtractionProgress> = flow {
        var completed = 0
        val total = tracks.size

        emit(ArtworkExtractionProgress(completed = 0, total = total))

        for (track in tracks) {
            emit(ArtworkExtractionProgress(
                completed = completed,
                total = total,
                currentTrack = track
            ))

            // Extract artwork and update database
            val result = extractForTrack(track, maxRetries = 2)

            if (result.isFailure) {
                emit(ArtworkExtractionProgress(
                    completed = completed,
                    total = total,
                    currentTrack = track,
                    error = "Failed to extract artwork: ${result.exceptionOrNull()?.message}"
                ))
            }

            completed++
            emit(ArtworkExtractionProgress(
                completed = completed,
                total = total,
                currentTrack = track
            ))
        }

        emit(ArtworkExtractionProgress(
            completed = completed,
            total = total,
            isCompleted = true
        ))
    }.flowOn(kotlinx.coroutines.Dispatchers.IO)

    /**
     * Extract artwork for tracks that don't have any
     * @param limit Maximum number of tracks to process
     * @return Flow of progress updates
     */
    suspend fun extractForTracksWithoutArtwork(limit: Int = 100): Flow<ArtworkExtractionProgress> {
        val tracksWithoutArtwork = trackRepository.getTracksWithoutArtwork(limit)
        return ArtworkExtractor.extractArtworkForTracks(context, tracksWithoutArtwork)
    }

    /**
     * Get artwork cache statistics
     */
    suspend fun getCacheStats(): ArtworkCacheStats {
        val totalTracks = trackRepository.getTrackCount()
        val tracksWithoutArtwork = trackRepository.getTracksWithoutArtwork(1000).size
        val tracksWithArtwork = totalTracks - tracksWithoutArtwork

        return ArtworkExtractor.getArtworkCacheStats(
            context = context,
            tracksWithArtwork = tracksWithArtwork,
            tracksWithoutArtwork = tracksWithoutArtwork
        )
    }

    /**
     * Clear artwork cache
     */
    suspend fun clearCache() {
        ArtworkExtractor.clearArtworkCache(context)
        // Also clear from database
        trackRepository.clearAllArtworkPaths()
    }

    /**
     * Validate and cleanup broken artwork paths
     * @return Number of invalid paths cleaned up
     */
    suspend fun validateAndCleanup(): Int {
        return ArtworkExtractor.validateAndCleanupArtwork(context)
    }

    /**
     * Sync cached artwork to database for tracks without artwork paths
     * This fixes the issue where artwork was extracted to cache but database wasn't updated
     * @return Number of tracks updated
     */
    suspend fun syncCachedArtworkToDatabase(): Int {
        var updatedCount = 0
        val tracksWithoutArtwork = trackRepository.getTracksWithoutArtwork(1000)

        for (track in tracksWithoutArtwork) {
            try {
                // Check if artwork exists in cache for this track
                val artworkPath = ArtworkExtractor.extractArtwork(context, track.filePath, track.albumId)
                if (artworkPath != null) {
                    // Update database with the cached artwork path
                    val updatedTrack = track.copy(artworkPath = artworkPath)
                    trackRepository.updateTrack(updatedTrack)
                    updatedCount++
                }
            } catch (e: Exception) {
                // Continue with next track on error
                android.util.Log.w("ExtractArtworkUseCase", "Failed to sync cached artwork for track ${track.id}", e)
            }
        }

        return updatedCount
    }
}