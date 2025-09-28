package com.octavia.player.domain.usecase

import com.octavia.player.data.model.Track
import com.octavia.player.domain.repository.ArtworkRepository
import com.octavia.player.domain.repository.ArtworkExtractionProgress
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for extracting artwork from music files
 */
class ExtractArtworkUseCase @Inject constructor(
    private val artworkRepository: ArtworkRepository
) {

    /**
     * Extract artwork for a single track
     * @param track The track to extract artwork for
     * @param maxRetries Maximum number of retry attempts
     * @return The artwork path if successful, null otherwise
     */
    suspend fun extractForTrack(track: Track, maxRetries: Int = 2): Result<String?> {
        var lastException: Exception? = null

        repeat(maxRetries + 1) { attempt ->
            try {
                val artworkPath = artworkRepository.extractArtworkForTrack(track)
                return Result.success(artworkPath)
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries) {
                    // Wait a bit before retrying
                    kotlinx.coroutines.delay(500L * (attempt + 1))
                }
            }
        }

        return Result.failure(lastException ?: Exception("Unknown error"))
    }

    /**
     * Extract artwork for multiple tracks with progress reporting
     * @param tracks List of tracks to extract artwork for
     * @return Flow of progress updates
     */
    fun extractForTracks(tracks: List<Track>): Flow<ArtworkExtractionProgress> {
        return artworkRepository.extractArtworkForTracks(tracks)
    }

    /**
     * Extract artwork for tracks that don't have any
     * @param limit Maximum number of tracks to process
     * @return Flow of progress updates
     */
    suspend fun extractForTracksWithoutArtwork(limit: Int = 100): Flow<ArtworkExtractionProgress> {
        val tracksWithoutArtwork = artworkRepository.getTracksWithoutArtwork(limit)
        return artworkRepository.extractArtworkForTracks(tracksWithoutArtwork)
    }

    /**
     * Get artwork cache statistics
     */
    suspend fun getCacheStats() = artworkRepository.getArtworkCacheStats()

    /**
     * Clear artwork cache
     */
    suspend fun clearCache() = artworkRepository.clearArtworkCache()

    /**
     * Validate and cleanup broken artwork paths
     * @return Number of invalid paths cleaned up
     */
    suspend fun validateAndCleanup(): Int {
        return artworkRepository.validateAndCleanupArtwork()
    }
}