package com.octavia.player.data.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Log
import com.octavia.player.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import androidx.core.graphics.scale

/**
 * Centralized artwork extraction and management service
 * Handles extraction, caching, database updates, and progress reporting
 * Optimized for performance with intelligent caching strategy
 */
object ArtworkExtractor {

    private const val TAG = "ArtworkExtractor"
    private const val ARTWORK_CACHE_DIR = "artwork_cache"
    private const val MAX_ARTWORK_SIZE = 1024 // Max width/height in pixels
    private const val MAX_CACHE_SIZE = 500 // Maximum number of cached items
    private const val MAX_FAILED_CACHE_SIZE = 100 // Maximum failed extractions to remember

    // Use proper LruCache instead of LinkedHashMap for better memory management
    private val artworkCache = object : android.util.LruCache<String, String?>(MAX_CACHE_SIZE) {
        override fun sizeOf(key: String, value: String?): Int {
            return 1 // Each cache entry counts as 1 unit
        }
    }

    // LRU cache for failed extractions with size limit
    private val failedExtractions = object : LinkedHashSet<String>() {
        override fun add(element: String): Boolean {
            if (size >= MAX_FAILED_CACHE_SIZE) {
                remove(iterator().next()) // Remove oldest
            }
            return super.add(element)
        }
    }

    /**
     * Extracts artwork for a track, trying both embedded and external sources
     * Returns the path to the artwork file if found, null otherwise
     * Optimized with in-memory caching and failure tracking
     */
    @Synchronized
    fun extractArtwork(context: Context, filePath: String, albumId: Long?): String? {
        val cacheKey = getCacheKey(albumId, filePath)

        // Check in-memory cache first
        artworkCache.get(cacheKey)?.let { return it }

        // Check if we've already failed to extract this artwork
        if (failedExtractions.contains(cacheKey)) {
            return null
        }
        
        try {
            // First try to find cached artwork on disk
            val cachedPath = getCachedArtworkPath(context, albumId, filePath)
            if (cachedPath != null && File(cachedPath).exists()) {
                artworkCache.put(cacheKey, cachedPath)
                return cachedPath
            }

            // Try external artwork files first (faster than embedded extraction)
            val externalArtwork = findExternalArtwork(filePath)
            if (externalArtwork != null) {
                // For external files, just cache the path directly if it's a reasonable size
                val externalFile = File(externalArtwork)
                if (externalFile.length() < 10 * 1024 * 1024) { // Skip very large files (>10MB)
                    artworkCache.put(cacheKey, externalArtwork)
                    return externalArtwork
                }
            }
            
            // Try embedded artwork (more expensive operation)
            val embeddedArtwork = extractEmbeddedArtwork(filePath)
            if (embeddedArtwork != null) {
                val savedPath = saveArtworkToCache(context, embeddedArtwork, albumId, filePath)
                if (savedPath != null) {
                    artworkCache.put(cacheKey, savedPath)
                    return savedPath
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error extracting artwork for $filePath", e)
        }
        
        // Cache the failure to avoid repeated attempts
        failedExtractions.add(cacheKey)
        artworkCache.put(cacheKey, null)
        return null
    }
    
    /**
     * Generate cache key for artwork
     */
    private fun getCacheKey(albumId: Long?, filePath: String): String {
        return if (albumId != null) {
            "album_$albumId"
        } else {
            "track_${filePath.hashCode()}"
        }
    }

    /**
     * Extracts embedded artwork from a music file using MediaMetadataRetriever
     */
    private fun extractEmbeddedArtwork(filePath: String): Bitmap? {
        var retriever: MediaMetadataRetriever? = null
        try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val artworkBytes = retriever.embeddedPicture

            if (artworkBytes != null) {
                val bitmap = BitmapFactory.decodeByteArray(artworkBytes, 0, artworkBytes.size)
                return resizeBitmap(bitmap)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not extract embedded artwork from $filePath", e)
        } finally {
            try {
                retriever?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing MediaMetadataRetriever", e)
            }
        }
        return null
    }

    /**
     * Looks for external artwork files in the same directory as the music file
     */
    private fun findExternalArtwork(filePath: String): String? {
        val trackFile = File(filePath)
        val parentDir = trackFile.parentFile ?: return null

        val artworkExtensions = listOf(".jpg", ".jpeg", ".png", ".webp")
        val commonNames = listOf("cover", "album", "front", "folder", "albumart")

        // Try common names first
        for (name in commonNames) {
            for (ext in artworkExtensions) {
                val artworkFile = File(parentDir, "$name$ext")
                if (artworkFile.exists() && artworkFile.isFile) {
                    return artworkFile.absolutePath
                }
            }
        }

        // Try any image file in the directory
        parentDir.listFiles { file ->
            file.isFile && artworkExtensions.any { ext ->
                file.name.lowercase().endsWith(ext)
            }
        }?.firstOrNull()?.let { return it.absolutePath }

        return null
    }

    /**
     * Saves artwork bitmap to cache directory
     */
    private fun saveArtworkToCache(
        context: Context,
        bitmap: Bitmap,
        albumId: Long?,
        filePath: String
    ): String? {
        try {
            val cacheDir = File(context.cacheDir, ARTWORK_CACHE_DIR)
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            // Generate filename based on album ID or file path hash
            val fileName = if (albumId != null) {
                "album_$albumId.jpg"
            } else {
                "track_${filePath.hashCode().toString().replace("-", "n")}.jpg"
            }

            val cacheFile = File(cacheDir, fileName)

            FileOutputStream(cacheFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }

            return cacheFile.absolutePath

        } catch (e: IOException) {
            Log.e(TAG, "Error saving artwork to cache", e)
        }

        return null
    }

    /**
     * Gets cached artwork path if it exists
     */
    private fun getCachedArtworkPath(context: Context, albumId: Long?, filePath: String): String? {
        val cacheDir = File(context.cacheDir, ARTWORK_CACHE_DIR)
        if (!cacheDir.exists()) return null

        val fileName = if (albumId != null) {
            "album_$albumId.jpg"
        } else {
            "track_${filePath.hashCode().toString().replace("-", "n")}.jpg"
        }

        val cacheFile = File(cacheDir, fileName)
        return if (cacheFile.exists()) cacheFile.absolutePath else null
    }

    /**
     * Resizes bitmap to maximum dimensions while maintaining aspect ratio
     */
    private fun resizeBitmap(bitmap: Bitmap?): Bitmap? {
        if (bitmap == null) return null

        val width = bitmap.width
        val height = bitmap.height

        if (width <= MAX_ARTWORK_SIZE && height <= MAX_ARTWORK_SIZE) {
            return bitmap
        }

        val scale = (MAX_ARTWORK_SIZE.toFloat() / maxOf(width, height))
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        val resized = bitmap.scale(newWidth, newHeight)
        if (resized != bitmap) {
            bitmap.recycle()
        }

        return resized
    }

    /**
     * Clears all cached artwork
     */
    @Synchronized
    fun clearArtworkCache(context: Context) {
        try {
            val cacheDir = File(context.cacheDir, ARTWORK_CACHE_DIR)
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
            }
            // Clear in-memory caches
            artworkCache.evictAll()
            failedExtractions.clear()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing artwork cache", e)
        }
    }

    /**
     * Clean up memory by trimming caches if they're getting too large
     */
    @Synchronized
    fun trimMemoryCache() {
        // LruCache automatically handles memory trimming, but we can force eviction if needed
        if (artworkCache.size() > MAX_CACHE_SIZE * 0.8) {
            artworkCache.trimToSize(MAX_CACHE_SIZE / 2)
        }

        if (failedExtractions.size > MAX_FAILED_CACHE_SIZE * 0.8) {
            val iterator = failedExtractions.iterator()
            var removed = 0
            while (iterator.hasNext() && removed < MAX_FAILED_CACHE_SIZE / 4) {
                iterator.next()
                iterator.remove()
                removed++
            }
        }
    }
    
    /**
     * Preload artwork for a batch of tracks (for background processing)
     */
    suspend fun preloadArtwork(context: Context, tracks: List<Track>) = withContext(Dispatchers.IO) {
        // Group tracks by album to avoid duplicate work
        val albumGroups = tracks.groupBy { it.albumId }

        albumGroups.forEach { (albumId, albumTracks) ->
            if (albumTracks.isNotEmpty()) {
                val firstTrack = albumTracks.first()
                extractArtwork(context, firstTrack.filePath, albumId)
            }
        }
    }

    /**
     * Extract artwork for a single track with retry logic and error handling
     * This replaces the repository layer functionality
     */
    suspend fun extractArtworkForTrack(context: Context, track: Track, maxRetries: Int = 2): Result<String?> = withContext(Dispatchers.IO) {
        var lastException: Exception? = null

        repeat(maxRetries + 1) { attempt ->
            try {
                val artworkPath = extractArtwork(context, track.filePath, track.albumId)
                return@withContext Result.success(artworkPath)
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Attempt ${attempt + 1} failed for track ${track.id}: ${e.message}")
                if (attempt < maxRetries) {
                    // Wait before retrying
                    kotlinx.coroutines.delay(500L * (attempt + 1))
                }
            }
        }

        return@withContext Result.failure(lastException ?: Exception("Unknown error"))
    }

    /**
     * Extract artwork for multiple tracks with progress reporting
     * This replaces the repository and use case layer functionality
     */
    fun extractArtworkForTracks(context: Context, tracks: List<Track>): Flow<ArtworkExtractionProgress> = flow {
        var completed = 0
        val total = tracks.size

        emit(ArtworkExtractionProgress(completed = 0, total = total))

        for (track in tracks) {
            emit(ArtworkExtractionProgress(
                completed = completed,
                total = total,
                currentTrack = track
            ))

            val result = extractArtworkForTrack(context, track, maxRetries = 2)

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
    }.flowOn(Dispatchers.IO)

    /**
     * Get artwork cache statistics
     */
    suspend fun getArtworkCacheStats(
        context: Context,
        tracksWithArtwork: Int = 0,
        tracksWithoutArtwork: Int = 0
    ): ArtworkCacheStats = withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, ARTWORK_CACHE_DIR)
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

            ArtworkCacheStats(
                totalCachedFiles = cachedFiles,
                cacheSize = cacheSize,
                memoryCache = artworkCache.size(),
                failedExtractions = failedExtractions.size,
                tracksWithArtwork = tracksWithArtwork,
                tracksWithoutArtwork = tracksWithoutArtwork
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get cache stats", e)
            ArtworkCacheStats(
                totalCachedFiles = 0,
                cacheSize = 0L,
                memoryCache = 0,
                failedExtractions = 0,
                tracksWithArtwork = 0,
                tracksWithoutArtwork = 0
            )
        }
    }

    /**
     * Validate and cleanup broken artwork paths
     */
    suspend fun validateAndCleanupArtwork(context: Context): Int = withContext(Dispatchers.IO) {
        try {
            var cleanedCount = 0
            val cacheDir = File(context.cacheDir, ARTWORK_CACHE_DIR)

            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { file ->
                    if (!file.exists() || file.length() == 0L) {
                        file.delete()
                        cleanedCount++
                    }
                }
            }

            Log.i(TAG, "Cleaned up $cleanedCount invalid artwork files")
            cleanedCount
        } catch (e: Exception) {
            Log.e(TAG, "Failed to validate artwork", e)
            0
        }
    }
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
    val memoryCache: Int = 0,
    val failedExtractions: Int = 0,
    val tracksWithArtwork: Int = 0,
    val tracksWithoutArtwork: Int = 0
)
