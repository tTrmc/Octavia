package com.octavia.player.data.repository

import android.content.Context
import com.octavia.player.data.database.dao.TrackDao
import com.octavia.player.data.model.Track
import com.octavia.player.data.scanner.ArtworkExtractor
import com.octavia.player.data.scanner.ArtworkExtractionProgress
import com.octavia.player.data.scanner.ArtworkCacheStats
import com.octavia.player.domain.repository.ArtworkRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ArtworkRepository
 */
@Singleton
class ArtworkRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackDao: TrackDao
) : ArtworkRepository {

    override suspend fun extractArtworkForTrack(track: Track): String? = withContext(Dispatchers.IO) {
        val result = ArtworkExtractor.extractArtworkForTrack(context, track)
        val artworkPath = result.getOrNull()

        if (artworkPath != null) {
            // Update the track in the database with the new artwork path
            updateTrackArtwork(track.id, artworkPath)
        }

        artworkPath
    }

    override fun extractArtworkForTracks(tracks: List<Track>): Flow<ArtworkExtractionProgress> {
        return ArtworkExtractor.extractArtworkForTracks(context, tracks)
    }

    override suspend fun updateTrackArtwork(trackId: Long, artworkPath: String?): Unit = withContext(Dispatchers.IO) {
        try {
            val track = trackDao.getTrackById(trackId)
            if (track != null) {
                val updatedTrack = track.copy(artworkPath = artworkPath)
                trackDao.updateTrack(updatedTrack)
            }
        } catch (e: Exception) {
            android.util.Log.e("ArtworkRepository", "Failed to update artwork for track $trackId", e)
        }
    }

    override suspend fun getTracksWithoutArtwork(limit: Int): List<Track> = withContext(Dispatchers.IO) {
        try {
            trackDao.getTracksWithoutArtwork(limit)
        } catch (e: Exception) {
            android.util.Log.e("ArtworkRepository", "Failed to get tracks without artwork", e)
            emptyList()
        }
    }

    override suspend fun clearArtworkCache(): Unit = withContext(Dispatchers.IO) {
        try {
            // Clear the artwork cache files
            ArtworkExtractor.clearArtworkCache(context)

            // Clear artwork paths from all tracks in database
            trackDao.clearAllArtworkPaths()

            android.util.Log.i("ArtworkRepository", "Artwork cache cleared")
        } catch (e: Exception) {
            android.util.Log.e("ArtworkRepository", "Failed to clear artwork cache", e)
        }
    }

    override suspend fun getArtworkCacheStats(): ArtworkCacheStats {
        val totalTracks = trackDao.getTrackCount()
        val tracksWithArtwork = trackDao.getTracksWithArtworkCount()
        val tracksWithoutArtwork = totalTracks - tracksWithArtwork

        return ArtworkExtractor.getArtworkCacheStats(
            context = context,
            tracksWithArtwork = tracksWithArtwork,
            tracksWithoutArtwork = tracksWithoutArtwork
        )
    }

    override suspend fun validateAndCleanupArtwork(): Int {
        return ArtworkExtractor.validateAndCleanupArtwork(context)
    }
}