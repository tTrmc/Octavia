package com.octavia.player.domain.usecase

import android.content.Context
import com.octavia.player.data.database.dao.AlbumDao
import com.octavia.player.data.database.dao.ArtistDao
import com.octavia.player.data.model.Album
import com.octavia.player.data.model.Artist
import com.octavia.player.data.scanner.MediaScanner
import com.octavia.player.domain.repository.TrackRepository
import javax.inject.Inject

/**
 * Use case for scanning and updating the media library
 * Now uses the consolidated MediaScanner for better performance
 */
class MediaLibraryScanUseCase @Inject constructor(
    private val trackRepository: TrackRepository,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao
) {
    
    suspend fun scanLibrary(context: Context): Result<Int> {
        return MediaScanner.scanLibrary(context, extractArtworkInBackground = false)
            .mapCatching { tracks ->
                if (tracks.isNotEmpty()) {
                    trackRepository.insertTracks(tracks)
                    buildAndInsertAlbums(tracks)
                    buildAndInsertArtists(tracks)
                    tracks.size
                } else {
                    0
                }
            }
    }

    suspend fun scanLibraryWithArtwork(context: Context): Result<Int> {
        return MediaScanner.scanLibrary(context, extractArtworkInBackground = true)
            .mapCatching { tracks ->
                if (tracks.isNotEmpty()) {
                    trackRepository.insertTracks(tracks)
                    buildAndInsertAlbums(tracks)
                    buildAndInsertArtists(tracks)
                    tracks.size
                } else {
                    0
                }
            }
    }

    /**
     * Build Album entities from scanned tracks and insert them into database
     */
    private suspend fun buildAndInsertAlbums(tracks: List<com.octavia.player.data.model.Track>) {
        // Group tracks by albumId (MediaStore album ID)
        val tracksByAlbum = tracks
            .filter { it.albumId != null } // Only tracks with album IDs
            .groupBy { it.albumId!! }

        // Build Album entity for each group
        val albums = tracksByAlbum.map { (albumId, albumTracks) ->
            val firstTrack = albumTracks.first()

            Album(
                id = albumId,
                name = firstTrack.album.takeIf { !it.isNullOrBlank() } ?: "Unknown Album",
                artist = firstTrack.artist,
                artistId = firstTrack.artistId,
                year = firstTrack.year,
                trackCount = albumTracks.size,
                totalDurationMs = albumTracks.sumOf { it.durationMs },
                artworkPath = albumTracks.firstOrNull()?.artworkPath, // Use any track's artwork
                dateAdded = albumTracks.minOfOrNull { it.dateAdded } ?: System.currentTimeMillis()
            )
        }

        // Insert all albums
        if (albums.isNotEmpty()) {
            albumDao.insertAlbums(albums)
            android.util.Log.d("MediaLibraryScan", "Built and inserted ${albums.size} albums from ${tracks.size} tracks")
        }
    }

    /**
     * Build Artist entities from scanned tracks and insert them into database
     */
    private suspend fun buildAndInsertArtists(tracks: List<com.octavia.player.data.model.Track>) {
        // Group tracks by artistId (MediaStore artist ID)
        val tracksByArtist = tracks
            .filter { it.artistId != null } // Only tracks with artist IDs
            .groupBy { it.artistId!! }

        // Build Artist entity for each group
        val artists = tracksByArtist.map { (artistId, artistTracks) ->
            val firstTrack = artistTracks.first()
            val albumCount = artistTracks.mapNotNull { it.albumId }.distinct().size

            Artist(
                id = artistId,
                name = firstTrack.artist.takeIf { !it.isNullOrBlank() } ?: "Unknown Artist",
                albumCount = albumCount,
                trackCount = artistTracks.size,
                artworkPath = null, // No artwork - will use placeholder icon
                dateAdded = artistTracks.minOfOrNull { it.dateAdded } ?: System.currentTimeMillis()
            )
        }

        // Insert all artists
        if (artists.isNotEmpty()) {
            artistDao.insertArtists(artists)
            android.util.Log.d("MediaLibraryScan", "Built and inserted ${artists.size} artists from ${tracks.size} tracks")
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
        MediaScanner.clearArtworkCache(context)
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