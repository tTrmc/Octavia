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
            // Step 1: Scan media files from MediaStore
            val tracks = MediaScanner.scanMusicLibrary(context, extractArtworkInBackground = false)
            
            if (tracks.isEmpty()) {
                return@withContext Result.success(emptyList())
            }
            
            // Step 2: If artwork is requested, extract it in parallel
            if (includeArtwork) {
                async {
                    ArtworkExtractor.preloadArtwork(context, tracks)
                }.start() // Fire and forget - artwork will be cached for UI
            }
            
            Result.success(tracks)
        } catch (e: Exception) {
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