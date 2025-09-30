package com.octavia.player.domain.usecase

import com.octavia.player.data.model.Playlist
import com.octavia.player.data.model.Track
import com.octavia.player.domain.repository.PlaylistRepository
import com.octavia.player.domain.repository.TrackRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case for managing tracks within playlists
 * Handles adding, removing, reordering, and querying tracks in playlists
 */
class PlaylistTrackManagementUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val trackRepository: TrackRepository
) {

    // Add operations

    /**
     * Add a single track to a playlist
     */
    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long): Result<Unit> {
        // Verify playlist exists
        val playlist = playlistRepository.getPlaylistById(playlistId)
            ?: return Result.failure(IllegalArgumentException("Playlist not found"))

        // Verify track exists
        val track = trackRepository.getTrackById(trackId)
            ?: return Result.failure(IllegalArgumentException("Track not found"))

        // Check if track is already in playlist
        if (isTrackInPlaylist(playlistId, trackId)) {
            android.util.Log.d("PlaylistTrackUseCase", "Track '${track.displayTitle}' already in playlist '${playlist.name}'")
            return Result.success(Unit) // Already exists, return success
        }

        return playlistRepository.addTrackToPlaylist(playlistId, trackId)
    }

    /**
     * Add multiple tracks to a playlist
     */
    suspend fun addTracksToPlaylist(playlistId: Long, trackIds: List<Long>): Result<Unit> {
        if (trackIds.isEmpty()) {
            return Result.failure(IllegalArgumentException("No tracks provided"))
        }

        // Verify playlist exists
        val playlist = playlistRepository.getPlaylistById(playlistId)
            ?: return Result.failure(IllegalArgumentException("Playlist not found"))

        // Filter out tracks that are already in the playlist
        val existingTracks = playlistRepository.getPlaylistTracks(playlistId).first()
        val existingTrackIds = existingTracks.map { it.id }.toSet()
        val newTrackIds = trackIds.filter { it !in existingTrackIds }

        if (newTrackIds.isEmpty()) {
            android.util.Log.d("PlaylistTrackUseCase", "All tracks already in playlist '${playlist.name}'")
            return Result.success(Unit)
        }

        // Verify all tracks exist
        val validTrackIds = newTrackIds.filter { trackId ->
            trackRepository.getTrackById(trackId) != null
        }

        if (validTrackIds.isEmpty()) {
            return Result.failure(IllegalArgumentException("No valid tracks to add"))
        }

        android.util.Log.d("PlaylistTrackUseCase", "Adding ${validTrackIds.size} tracks to playlist '${playlist.name}'")
        return playlistRepository.addTracksToPlaylist(playlistId, validTrackIds)
    }

    /**
     * Add an entire album to a playlist
     */
    suspend fun addAlbumToPlaylist(playlistId: Long, albumId: Long): Result<Unit> {
        // Get all tracks from the album
        val albumTracks = trackRepository.getTracksByAlbum(albumId).first()

        if (albumTracks.isEmpty()) {
            return Result.failure(IllegalArgumentException("Album has no tracks"))
        }

        val trackIds = albumTracks.map { it.id }
        return addTracksToPlaylist(playlistId, trackIds)
    }

    // Remove operations

    /**
     * Remove a track from a playlist
     */
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long): Result<Unit> {
        // Verify playlist exists
        playlistRepository.getPlaylistById(playlistId)
            ?: return Result.failure(IllegalArgumentException("Playlist not found"))

        // Check if track is in playlist
        if (!isTrackInPlaylist(playlistId, trackId)) {
            android.util.Log.w("PlaylistTrackUseCase", "Track not in playlist, skipping removal")
            return Result.success(Unit)
        }

        return playlistRepository.removeTrackFromPlaylist(playlistId, trackId)
    }

    /**
     * Clear all tracks from a playlist
     */
    suspend fun clearPlaylist(playlistId: Long): Result<Unit> {
        // Verify playlist exists
        playlistRepository.getPlaylistById(playlistId)
            ?: return Result.failure(IllegalArgumentException("Playlist not found"))

        return playlistRepository.clearPlaylist(playlistId)
    }

    // Reorder operations

    /**
     * Move a track from one position to another within a playlist
     */
    suspend fun moveTrack(playlistId: Long, fromPosition: Int, toPosition: Int): Result<Unit> {
        val tracks = playlistRepository.getPlaylistTracks(playlistId).first()

        if (fromPosition < 0 || fromPosition >= tracks.size) {
            return Result.failure(IllegalArgumentException("Invalid fromPosition: $fromPosition"))
        }

        if (toPosition < 0 || toPosition >= tracks.size) {
            return Result.failure(IllegalArgumentException("Invalid toPosition: $toPosition"))
        }

        if (fromPosition == toPosition) {
            return Result.success(Unit) // No change needed
        }

        // Reorder the track list
        val reorderedTracks = tracks.toMutableList()
        val trackToMove = reorderedTracks.removeAt(fromPosition)
        reorderedTracks.add(toPosition, trackToMove)

        val newOrder = reorderedTracks.map { it.id }
        return playlistRepository.reorderPlaylistTracks(playlistId, newOrder)
    }

    /**
     * Reorder all tracks in a playlist with a new order
     */
    suspend fun reorderTracks(playlistId: Long, newOrder: List<Long>): Result<Unit> {
        val currentTracks = playlistRepository.getPlaylistTracks(playlistId).first()

        // Validate that new order contains same tracks
        if (newOrder.size != currentTracks.size) {
            return Result.failure(IllegalArgumentException("New order must contain same number of tracks"))
        }

        val currentTrackIds = currentTracks.map { it.id }.toSet()
        if (newOrder.toSet() != currentTrackIds) {
            return Result.failure(IllegalArgumentException("New order must contain same tracks"))
        }

        return playlistRepository.reorderPlaylistTracks(playlistId, newOrder)
    }

    // Query operations

    /**
     * Check if a track is in a specific playlist
     */
    suspend fun isTrackInPlaylist(playlistId: Long, trackId: Long): Boolean {
        val tracks = playlistRepository.getPlaylistTracks(playlistId).first()
        return tracks.any { it.id == trackId }
    }

    /**
     * Get all playlists that contain a specific track
     */
    suspend fun getPlaylistsContainingTrack(trackId: Long): List<Playlist> {
        val allPlaylists = playlistRepository.getAllPlaylists().first()
        return allPlaylists.filter { playlist ->
            isTrackInPlaylist(playlist.id, trackId)
        }
    }

    /**
     * Get the number of tracks in a playlist
     */
    suspend fun getPlaylistTrackCount(playlistId: Long): Int {
        val tracks = playlistRepository.getPlaylistTracks(playlistId).first()
        return tracks.size
    }

    /**
     * Get the total duration of all tracks in a playlist
     */
    suspend fun getPlaylistTotalDuration(playlistId: Long): Long {
        val tracks = playlistRepository.getPlaylistTracks(playlistId).first()
        return tracks.sumOf { it.durationMs }
    }
}