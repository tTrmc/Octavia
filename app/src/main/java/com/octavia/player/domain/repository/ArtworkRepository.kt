package com.octavia.player.domain.repository

import com.octavia.player.data.model.Track
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for artwork management
 */
interface ArtworkRepository {

    /**
     * Extract artwork for a single track
     * @param track The track to extract artwork for
     * @return The artwork path if successful, null otherwise
     */
    suspend fun extractArtworkForTrack(track: Track): String?

    /**
     * Extract artwork for multiple tracks with progress reporting
     * @param tracks List of tracks to extract artwork for
     * @return Flow of progress (completed/total)
     */
    fun extractArtworkForTracks(tracks: List<Track>): Flow<ArtworkExtractionProgress>

    /**
     * Update the artwork path for a track in the database
     * @param trackId The track ID
     * @param artworkPath The new artwork path (null to clear)
     */
    suspend fun updateTrackArtwork(trackId: Long, artworkPath: String?)

    /**
     * Get tracks that are missing artwork
     * @param limit Maximum number of tracks to return
     * @return List of tracks without artwork
     */
    suspend fun getTracksWithoutArtwork(limit: Int = 100): List<Track>

    /**
     * Clear all cached artwork files and reset database paths
     */
    suspend fun clearArtworkCache()

    /**
     * Get artwork cache statistics
     * @return Cache statistics
     */
    suspend fun getArtworkCacheStats(): ArtworkCacheStats

    /**
     * Validate and clean up broken artwork paths
     * @return Number of invalid paths cleaned up
     */
    suspend fun validateAndCleanupArtwork(): Int
}

/**
 * Progress data for artwork extraction operations
 */
data class ArtworkExtractionProgress(
    val completed: Int,
    val total: Int,
    val currentTrack: Track? = null,
    val isCompleted: Boolean = false,
    val error: String? = null
) {
    val progressPercentage: Float
        get() = if (total > 0) (completed.toFloat() / total.toFloat()) * 100f else 0f
}

/**
 * Artwork cache statistics
 */
data class ArtworkCacheStats(
    val totalCachedFiles: Int,
    val cacheSize: Long,
    val tracksWithArtwork: Int,
    val tracksWithoutArtwork: Int
)