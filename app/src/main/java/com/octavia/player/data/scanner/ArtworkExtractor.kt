package com.octavia.player.data.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Utility class for extracting and managing album artwork
 */
object ArtworkExtractor {

    private const val TAG = "ArtworkExtractor"
    private const val ARTWORK_CACHE_DIR = "artwork_cache"
    private const val MAX_ARTWORK_SIZE = 1024 // Max width/height in pixels

    /**
     * Extracts artwork for a track, trying both embedded and external sources
     * Returns the path to the artwork file if found, null otherwise
     */
    fun extractArtwork(context: Context, filePath: String, albumId: Long?): String? {
        try {
            // First try to find cached artwork
            val cachedPath = getCachedArtworkPath(context, albumId, filePath)
            if (cachedPath != null && File(cachedPath).exists()) {
                return cachedPath
            }

            // Try embedded artwork first
            val embeddedArtwork = extractEmbeddedArtwork(filePath)
            if (embeddedArtwork != null) {
                val savedPath = saveArtworkToCache(context, embeddedArtwork, albumId, filePath)
                if (savedPath != null) {
                    return savedPath
                }
            }

            // Try external artwork files
            val externalArtwork = findExternalArtwork(filePath)
            if (externalArtwork != null) {
                // Copy external artwork to cache for consistency
                val bitmap = BitmapFactory.decodeFile(externalArtwork)
                if (bitmap != null) {
                    val savedPath = saveArtworkToCache(context, bitmap, albumId, filePath)
                    bitmap.recycle()
                    return savedPath
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error extracting artwork for $filePath", e)
        }

        return null
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
    fun clearArtworkCache(context: Context) {
        try {
            val cacheDir = File(context.cacheDir, ARTWORK_CACHE_DIR)
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing artwork cache", e)
        }
    }
}
