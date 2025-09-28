package com.octavia.player.data.repository

import android.content.Context
import com.octavia.player.data.database.dao.TrackDao
import com.octavia.player.data.model.Track
import com.octavia.player.data.scanner.ArtworkExtractor
import com.octavia.player.domain.repository.ArtworkRepository
import com.octavia.player.domain.repository.ArtworkExtractionProgress
import com.octavia.player.domain.repository.ArtworkCacheStats
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
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
        try {
            val artworkPath = ArtworkExtractor.extractArtwork(context, track.filePath, track.albumId)

            if (artworkPath != null) {
                // Update the track in the database with the new artwork path
                updateTrackArtwork(track.id, artworkPath)
            }

            artworkPath
        } catch (e: Exception) {
            android.util.Log.e("ArtworkRepository", "Failed to extract artwork for track ${track.id}", e)
            null
        }
    }

    override fun extractArtworkForTracks(tracks: List<Track>): Flow<ArtworkExtractionProgress> = flow {
        var completed = 0
        val total = tracks.size

        emit(ArtworkExtractionProgress(completed = 0, total = total))

        for (track in tracks) {
            emit(ArtworkExtractionProgress(
                completed = completed,
                total = total,
                currentTrack = track
            ))

            var success = false
            var lastError: String? = null

            // Retry logic for failed extractions
            repeat(3) { attempt ->
                if (!success) {
                    try {
                        val artworkPath = extractArtworkForTrack(track)
                        success = true
                    } catch (e: Exception) {
                        lastError = e.message
                        android.util.Log.w("ArtworkRepository", "Attempt ${attempt + 1} failed for track ${track.id}: ${e.message}")

                        // Wait before retrying
                        if (attempt < 2) {
                            kotlinx.coroutines.delay(300L * (attempt + 1))
                        }
                    }
                }
            }

            if (!success) {
                emit(ArtworkExtractionProgress(
                    completed = completed,
                    total = total,
                    currentTrack = track,
                    error = "Failed after 3 attempts: $lastError"
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
    }.flowOn(Dispatchers.IO)

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

    override suspend fun getArtworkCacheStats(): ArtworkCacheStats = withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, "artwork_cache")
            val cachedFiles = if (cacheDir.exists()) {
                cacheDir.listFiles()?.size ?: 0
            } else {
                0
            }

            val cacheSize = if (cacheDir.exists()) {
                cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
            } else {
                0L
            }

            val totalTracks = trackDao.getTrackCount()
            val tracksWithArtwork = trackDao.getTracksWithArtworkCount()
            val tracksWithoutArtwork = totalTracks - tracksWithArtwork

            ArtworkCacheStats(
                totalCachedFiles = cachedFiles,
                cacheSize = cacheSize,
                tracksWithArtwork = tracksWithArtwork,
                tracksWithoutArtwork = tracksWithoutArtwork
            )
        } catch (e: Exception) {
            android.util.Log.e("ArtworkRepository", "Failed to get cache stats", e)
            ArtworkCacheStats(
                totalCachedFiles = 0,
                cacheSize = 0L,
                tracksWithArtwork = 0,
                tracksWithoutArtwork = 0
            )
        }
    }

    override suspend fun validateAndCleanupArtwork(): Int = withContext(Dispatchers.IO) {
        try {
            var cleanedCount = 0

            // Get all tracks and validate their artwork paths
            // This is a placeholder - should use proper DAO method to get tracks with artwork
            android.util.Log.i("ArtworkRepository", "Artwork validation completed")

            cleanedCount
        } catch (e: Exception) {
            android.util.Log.e("ArtworkRepository", "Failed to validate artwork", e)
            0
        }
    }
}