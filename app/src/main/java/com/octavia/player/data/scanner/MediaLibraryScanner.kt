package com.octavia.player.data.scanner

import android.content.Context
import com.octavia.player.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized media library scanner with optimized workflow
 * Combines media scanning and artwork extraction efficiently
 */
@Singleton
class MediaLibraryScanner @Inject constructor() {
    
    suspend fun scanLibrary(
        context: Context,
        includeArtwork: Boolean = false
    ): Result<List<Track>> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.i("MediaLibraryScanner", "Starting library scan...")

            // Step 1: Scan media files from MediaStore
            val tracks = MediaScanner.scanMusicLibrary(context, extractArtworkInBackground = false)

            android.util.Log.i("MediaLibraryScanner", "Found ${tracks.size} tracks")

            if (tracks.isEmpty()) {
                android.util.Log.w("MediaLibraryScanner", "No music files found")
                return@withContext Result.success(emptyList())
            }

            // Step 2: If artwork is requested, extract it in parallel
            if (includeArtwork) {
                try {
                    async {
                        ArtworkExtractor.preloadArtwork(context, tracks)
                    }.start() // Fire and forget - artwork will be cached for UI
                } catch (e: Exception) {
                    android.util.Log.e("MediaLibraryScanner", "Failed to start artwork extraction", e)
                    // Don't fail the entire scan for artwork issues
                }
            }

            Result.success(tracks)
        } catch (e: SecurityException) {
            android.util.Log.e("MediaLibraryScanner", "Permission denied while scanning library", e)
            Result.failure(SecurityException("Storage permission required to scan music library", e))
        } catch (e: Exception) {
            android.util.Log.e("MediaLibraryScanner", "Unexpected error during library scan", e)
            Result.failure(e)
        }
    }
    
    suspend fun extractArtworkForTracks(
        context: Context, 
        tracks: List<Track>
    ) = withContext(Dispatchers.IO) {
        try {
            ArtworkExtractor.preloadArtwork(context, tracks)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun clearArtworkCache(context: Context) {
        ArtworkExtractor.clearArtworkCache(context)
    }
}