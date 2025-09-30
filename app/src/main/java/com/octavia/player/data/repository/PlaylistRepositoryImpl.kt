package com.octavia.player.data.repository

import com.octavia.player.data.database.dao.PlaylistDao
import com.octavia.player.data.database.dao.TrackDao
import com.octavia.player.data.model.Playlist
import com.octavia.player.data.model.PlaylistTrack
import com.octavia.player.data.model.Track
import com.octavia.player.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PlaylistRepository
 * Handles all playlist-related data operations with automatic metadata updates
 */
@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val trackDao: TrackDao
) : PlaylistRepository {

    override fun getAllPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getAllPlaylistsFlow()
    }

    override suspend fun getPlaylistById(id: Long): Playlist? {
        return playlistDao.getPlaylistById(id)
    }

    override fun getPlaylistTracks(playlistId: Long): Flow<List<Track>> {
        return playlistDao.getPlaylistTracks(playlistId)
    }

    override fun searchPlaylists(query: String): Flow<List<Playlist>> {
        return playlistDao.searchPlaylists(query)
    }

    override suspend fun createPlaylist(name: String, description: String?): Result<Long> {
        return try {
            if (name.isBlank()) {
                return Result.failure(IllegalArgumentException("Playlist name cannot be empty"))
            }

            val playlist = Playlist(
                name = name.trim(),
                description = description?.trim(),
                trackCount = 0,
                totalDurationMs = 0L,
                dateCreated = System.currentTimeMillis(),
                dateModified = System.currentTimeMillis()
            )

            val playlistId = playlistDao.insertPlaylist(playlist)
            android.util.Log.d("PlaylistRepo", "Created playlist: $name with ID: $playlistId")
            Result.success(playlistId)
        } catch (e: Exception) {
            android.util.Log.e("PlaylistRepo", "Failed to create playlist: $name", e)
            Result.failure(e)
        }
    }

    override suspend fun updatePlaylist(playlist: Playlist): Result<Unit> {
        return try {
            if (playlist.name.isBlank()) {
                return Result.failure(IllegalArgumentException("Playlist name cannot be empty"))
            }

            val updatedPlaylist = playlist.copy(
                name = playlist.name.trim(),
                description = playlist.description?.trim(),
                dateModified = System.currentTimeMillis()
            )

            playlistDao.updatePlaylist(updatedPlaylist)
            android.util.Log.d("PlaylistRepo", "Updated playlist: ${playlist.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("PlaylistRepo", "Failed to update playlist: ${playlist.name}", e)
            Result.failure(e)
        }
    }

    override suspend fun deletePlaylist(playlistId: Long): Result<Unit> {
        return try {
            playlistDao.clearPlaylist(playlistId) // Remove all tracks first
            playlistDao.deletePlaylistById(playlistId)
            android.util.Log.d("PlaylistRepo", "Deleted playlist ID: $playlistId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("PlaylistRepo", "Failed to delete playlist ID: $playlistId", e)
            Result.failure(e)
        }
    }

    override suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long): Result<Unit> {
        return try {
            val track = trackDao.getTrackById(trackId)
                ?: return Result.failure(IllegalArgumentException("Track not found: $trackId"))

            // Get next position
            val nextPosition = (playlistDao.getLastPositionInPlaylist(playlistId) ?: -1) + 1

            val playlistTrack = PlaylistTrack(
                playlistId = playlistId,
                trackId = trackId,
                position = nextPosition,
                dateAdded = System.currentTimeMillis()
            )

            playlistDao.insertPlaylistTrack(playlistTrack)
            updatePlaylistMetadata(playlistId)

            android.util.Log.d("PlaylistRepo", "Added track ${track.displayTitle} to playlist $playlistId at position $nextPosition")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("PlaylistRepo", "Failed to add track $trackId to playlist $playlistId", e)
            Result.failure(e)
        }
    }

    override suspend fun addTracksToPlaylist(playlistId: Long, trackIds: List<Long>): Result<Unit> {
        return try {
            var nextPosition = (playlistDao.getLastPositionInPlaylist(playlistId) ?: -1) + 1

            val playlistTracks = trackIds.mapNotNull { trackId ->
                val track = trackDao.getTrackById(trackId)
                if (track != null) {
                    PlaylistTrack(
                        playlistId = playlistId,
                        trackId = trackId,
                        position = nextPosition++,
                        dateAdded = System.currentTimeMillis()
                    )
                } else {
                    android.util.Log.w("PlaylistRepo", "Track not found: $trackId, skipping")
                    null
                }
            }

            if (playlistTracks.isEmpty()) {
                return Result.failure(IllegalArgumentException("No valid tracks to add"))
            }

            playlistDao.insertPlaylistTracks(playlistTracks)
            updatePlaylistMetadata(playlistId)

            android.util.Log.d("PlaylistRepo", "Added ${playlistTracks.size} tracks to playlist $playlistId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("PlaylistRepo", "Failed to add tracks to playlist $playlistId", e)
            Result.failure(e)
        }
    }

    override suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long): Result<Unit> {
        return try {
            playlistDao.removeTrackFromPlaylist(playlistId, trackId)
            updatePlaylistMetadata(playlistId)

            android.util.Log.d("PlaylistRepo", "Removed track $trackId from playlist $playlistId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("PlaylistRepo", "Failed to remove track $trackId from playlist $playlistId", e)
            Result.failure(e)
        }
    }

    override suspend fun clearPlaylist(playlistId: Long): Result<Unit> {
        return try {
            playlistDao.clearPlaylist(playlistId)
            updatePlaylistMetadata(playlistId)

            android.util.Log.d("PlaylistRepo", "Cleared all tracks from playlist $playlistId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("PlaylistRepo", "Failed to clear playlist $playlistId", e)
            Result.failure(e)
        }
    }

    override suspend fun reorderPlaylistTracks(playlistId: Long, trackIds: List<Long>): Result<Unit> {
        return try {
            // Clear existing playlist tracks
            playlistDao.clearPlaylist(playlistId)

            // Insert tracks in new order
            val playlistTracks = trackIds.mapIndexed { index, trackId ->
                PlaylistTrack(
                    playlistId = playlistId,
                    trackId = trackId,
                    position = index,
                    dateAdded = System.currentTimeMillis()
                )
            }

            playlistDao.insertPlaylistTracks(playlistTracks)
            updatePlaylistMetadata(playlistId)

            android.util.Log.d("PlaylistRepo", "Reordered ${trackIds.size} tracks in playlist $playlistId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("PlaylistRepo", "Failed to reorder tracks in playlist $playlistId", e)
            Result.failure(e)
        }
    }

    override suspend fun getPlaylistCount(): Int {
        return playlistDao.getPlaylistCount()
    }

    /**
     * Update playlist metadata (track count and total duration)
     * Called after any track addition/removal
     */
    private suspend fun updatePlaylistMetadata(playlistId: Long) {
        try {
            val playlist = playlistDao.getPlaylistById(playlistId) ?: return
            val tracks = playlistDao.getPlaylistTracks(playlistId).first()

            val updatedPlaylist = playlist.copy(
                trackCount = tracks.size,
                totalDurationMs = tracks.sumOf { it.durationMs },
                dateModified = System.currentTimeMillis()
            )

            playlistDao.updatePlaylist(updatedPlaylist)
        } catch (e: Exception) {
            android.util.Log.e("PlaylistRepo", "Failed to update playlist metadata for $playlistId", e)
        }
    }
}