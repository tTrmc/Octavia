package com.octavia.player.data.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Log
import com.octavia.player.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Utility class for extracting and managing album artwork
 * Optimized for performance with better caching strategy
 */
object ArtworkExtractor {

    private const val TAG = "ArtworkExtractor"
    private const val ARTWORK_CACHE_DIR = "artwork_cache"
    private const val MAX_ARTWORK_SIZE = 1024 // Max width/height in pixels
    private const val MAX_CACHE_SIZE = 500 // Maximum number of cached items
    private const val MAX_FAILED_CACHE_SIZE = 100 // Maximum failed extractions to remember

    // LRU cache for artwork paths with size limit
    private val artworkCache = object : LinkedHashMap<String, String?>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String?>?): Boolean {
            return size > MAX_CACHE_SIZE
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
        artworkCache[cacheKey]?.let { return it }

        // Check if we've already failed to extract this artwork
        if (failedExtractions.contains(cacheKey)) {
            return null
        }
        
        try {
            // First try to find cached artwork on disk
            val cachedPath = getCachedArtworkPath(context, albumId, filePath)
            if (cachedPath != null && File(cachedPath).exists()) {
                artworkCache[cacheKey] = cachedPath
                return cachedPath
            }

            // Try external artwork files first (faster than embedded extraction)
            val externalArtwork = findExternalArtwork(filePath)
            if (externalArtwork != null) {
                // For external files, just cache the path directly if it's a reasonable size
                val externalFile = File(externalArtwork)
                if (externalFile.length() < 10 * 1024 * 1024) { // Skip very large files (>10MB)
                    artworkCache[cacheKey] = externalArtwork
                    return externalArtwork
                }
            }
            
            // Try embedded artwork (more expensive operation)
            val embeddedArtwork = extractEmbeddedArtwork(filePath)
            if (embeddedArtwork != null) {
                val savedPath = saveArtworkToCache(context, embeddedArtwork, albumId, filePath)
                if (savedPath != null) {
                    artworkCache[cacheKey] = savedPath
                    return savedPath
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error extracting artwork for $filePath", e)
        }
        
        // Cache the failure to avoid repeated attempts
        failedExtractions.add(cacheKey)
        artworkCache[cacheKey] = null
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

        val resized = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
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
            artworkCache.clear()
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
        // The LinkedHashMap will automatically remove oldest entries when limit is exceeded
        // But we can manually trim if memory pressure is detected
        if (artworkCache.size > MAX_CACHE_SIZE * 0.8) {
            val iterator = artworkCache.entries.iterator()
            var removed = 0
            while (iterator.hasNext() && removed < MAX_CACHE_SIZE / 4) {
                iterator.next()
                iterator.remove()
                removed++
            }
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
}
