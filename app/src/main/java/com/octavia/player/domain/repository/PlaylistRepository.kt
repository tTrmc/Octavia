package com.octavia.player.domain.repository

import com.octavia.player.data.model.Playlist
import com.octavia.player.data.model.Track
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for playlist operations
 * Provides access to playlist data and management
 */
interface PlaylistRepository {

    // Read operations

    /**
     * Get all playlists as a Flow
     */
    fun getAllPlaylists(): Flow<List<Playlist>>

    /**
     * Get a specific playlist by ID
     */
    suspend fun getPlaylistById(id: Long): Playlist?

    /**
     * Get all tracks in a playlist ordered by position
     */
    fun getPlaylistTracks(playlistId: Long): Flow<List<Track>>

    /**
     * Search playlists by name
     */
    fun searchPlaylists(query: String): Flow<List<Playlist>>

    // Create/Update operations

    /**
     * Create a new playlist
     * @return Result with playlist ID on success, error on failure
     */
    suspend fun createPlaylist(name: String, description: String?): Result<Long>

    /**
     * Update existing playlist metadata
     */
    suspend fun updatePlaylist(playlist: Playlist): Result<Unit>

    // Delete operations

    /**
     * Delete a playlist and all its track associations
     */
    suspend fun deletePlaylist(playlistId: Long): Result<Unit>

    // Track management operations

    /**
     * Add a single track to playlist
     */
    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long): Result<Unit>

    /**
     * Add multiple tracks to playlist
     */
    suspend fun addTracksToPlaylist(playlistId: Long, trackIds: List<Long>): Result<Unit>

    /**
     * Remove a track from playlist
     */
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long): Result<Unit>

    /**
     * Clear all tracks from playlist
     */
    suspend fun clearPlaylist(playlistId: Long): Result<Unit>

    /**
     * Reorder tracks in playlist
     * @param trackIds List of track IDs in desired order
     */
    suspend fun reorderPlaylistTracks(playlistId: Long, trackIds: List<Long>): Result<Unit>

    // Stats

    /**
     * Get total number of playlists
     */
    suspend fun getPlaylistCount(): Int
}