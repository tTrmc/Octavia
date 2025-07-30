package com.octavia.player.domain.usecase

import android.content.Context
import com.octavia.player.data.scanner.MediaLibraryScanner
import com.octavia.player.domain.repository.TrackRepository
import javax.inject.Inject

/**
 * Use case for scanning and updating the media library
 */
class MediaLibraryScanUseCase @Inject constructor(
    private val trackRepository: TrackRepository,
    private val mediaLibraryScanner: MediaLibraryScanner
) {
    
    suspend fun scanLibrary(context: Context): Result<Int> {
        return mediaLibraryScanner.scanLibrary(context, includeArtwork = false)
            .mapCatching { tracks ->
                if (tracks.isNotEmpty()) {
                    trackRepository.insertTracks(tracks)
                    tracks.size
                } else {
                    0
                }
            }
    }
    
    suspend fun scanLibraryWithArtwork(context: Context): Result<Int> {
        return mediaLibraryScanner.scanLibrary(context, includeArtwork = true)
            .mapCatching { tracks ->
                if (tracks.isNotEmpty()) {
                    trackRepository.insertTracks(tracks)
                    tracks.size
                } else {
                    0
                }
            }
    }
    
    suspend fun refreshArtwork(context: Context): Result<Unit> {
        return trackRepository.getAllTracks()
            .let { tracksFlow ->
                // Get the current tracks (this is a simplified approach)
                // In a real implementation, you might want to collect the first emission
                Result.success(Unit) // Placeholder - would need proper Flow handling
            }
    }
    
    fun clearArtworkCache(context: Context) {
        mediaLibraryScanner.clearArtworkCache(context)
    }
    
    suspend fun getLibraryStats(): LibraryStats {
        return LibraryStats(
            totalTracks = trackRepository.getTrackCount(),
            totalDuration = trackRepository.getTotalDuration(),
            losslessCount = trackRepository.getLosslessTrackCount(),
            availableCodecs = trackRepository.getAllCodecs()
        )
    }
}

data class LibraryStats(
    val totalTracks: Int,
    val totalDuration: Long,
    val losslessCount: Int,
    val availableCodecs: List<String>
)