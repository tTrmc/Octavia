package com.octavia.player.domain.usecase

import com.octavia.player.data.model.Playlist
import com.octavia.player.data.model.Track
import com.octavia.player.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case for playlist lifecycle management
 * Handles creation, updating, deletion, and validation of playlists
 */
class PlaylistManagementUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {

    companion object {
        private const val MAX_PLAYLIST_NAME_LENGTH = 100
        private const val MAX_DESCRIPTION_LENGTH = 500
    }

    // Queries

    /**
     * Get all playlists
     */
    fun getAllPlaylists(): Flow<List<Playlist>> {
        return playlistRepository.getAllPlaylists()
    }

    /**
     * Get a playlist by ID
     */
    suspend fun getPlaylistById(id: Long): Playlist? {
        return playlistRepository.getPlaylistById(id)
    }

    /**
     * Get playlist with its tracks
     * Returns a flow of Pair<Playlist?, List<Track>>
     */
    fun getPlaylistWithTracks(playlistId: Long): Flow<Pair<Playlist?, List<Track>>> {
        return combine(
            playlistRepository.getAllPlaylists(),
            playlistRepository.getPlaylistTracks(playlistId)
        ) { playlists, tracks ->
            val playlist = playlists.find { it.id == playlistId }
            Pair(playlist, tracks)
        }
    }

    /**
     * Search playlists by name
     */
    fun searchPlaylists(query: String): Flow<List<Playlist>> {
        return playlistRepository.searchPlaylists(query)
    }

    // Create/Update operations

    /**
     * Create a new playlist with validation
     */
    suspend fun createPlaylist(name: String, description: String? = null): Result<Long> {
        // Validate name
        val validation = validatePlaylistName(name)
        if (!validation.isValid) {
            return Result.failure(IllegalArgumentException(validation.errorMessage ?: "Invalid playlist name"))
        }

        // Check for duplicate name
        if (!isPlaylistNameUnique(name)) {
            return Result.failure(IllegalArgumentException("A playlist with this name already exists"))
        }

        // Validate description if provided
        if (description != null && description.length > MAX_DESCRIPTION_LENGTH) {
            return Result.failure(
                IllegalArgumentException("Description must be less than $MAX_DESCRIPTION_LENGTH characters")
            )
        }

        return playlistRepository.createPlaylist(name, description)
    }

    /**
     * Update playlist metadata (name and description)
     */
    suspend fun updatePlaylistMetadata(
        playlistId: Long,
        name: String,
        description: String?
    ): Result<Unit> {
        // Get current playlist
        val currentPlaylist = playlistRepository.getPlaylistById(playlistId)
            ?: return Result.failure(IllegalArgumentException("Playlist not found"))

        // Validate new name
        val validation = validatePlaylistName(name)
        if (!validation.isValid) {
            return Result.failure(IllegalArgumentException(validation.errorMessage ?: "Invalid playlist name"))
        }

        // Check for duplicate name (excluding current playlist)
        if (!isPlaylistNameUnique(name, excludeId = playlistId)) {
            return Result.failure(IllegalArgumentException("A playlist with this name already exists"))
        }

        // Validate description
        if (description != null && description.length > MAX_DESCRIPTION_LENGTH) {
            return Result.failure(
                IllegalArgumentException("Description must be less than $MAX_DESCRIPTION_LENGTH characters")
            )
        }

        val updatedPlaylist = currentPlaylist.copy(
            name = name,
            description = description
        )

        return playlistRepository.updatePlaylist(updatedPlaylist)
    }

    /**
     * Delete a playlist
     */
    suspend fun deletePlaylist(playlistId: Long): Result<Unit> {
        return playlistRepository.deletePlaylist(playlistId)
    }

    /**
     * Duplicate a playlist with a new name
     */
    suspend fun duplicatePlaylist(playlistId: Long, newName: String): Result<Long> {
        // Get original playlist
        val original = playlistRepository.getPlaylistById(playlistId)
            ?: return Result.failure(IllegalArgumentException("Playlist not found"))

        // Get original tracks
        val tracks = playlistRepository.getPlaylistTracks(playlistId).first()

        // Validate new name
        val validation = validatePlaylistName(newName)
        if (!validation.isValid) {
            return Result.failure(IllegalArgumentException(validation.errorMessage ?: "Invalid playlist name"))
        }

        // Check for duplicate name
        if (!isPlaylistNameUnique(newName)) {
            return Result.failure(IllegalArgumentException("A playlist with this name already exists"))
        }

        // Create new playlist
        val createResult = playlistRepository.createPlaylist(
            newName,
            original.description?.let { "Copy of $it" }
        )

        return createResult.fold(
            onSuccess = { newPlaylistId ->
                // Add tracks to new playlist
                val trackIds = tracks.map { it.id }
                playlistRepository.addTracksToPlaylist(newPlaylistId, trackIds)
                    .fold(
                        onSuccess = { Result.success(newPlaylistId) },
                        onFailure = { error -> Result.failure(error) }
                    )
            },
            onFailure = { error -> Result.failure(error) }
        )
    }

    // Validation

    /**
     * Validate a playlist name
     */
    fun validatePlaylistName(name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult(false, "Playlist name cannot be empty")
            name.length > MAX_PLAYLIST_NAME_LENGTH -> ValidationResult(
                false,
                "Playlist name must be less than $MAX_PLAYLIST_NAME_LENGTH characters"
            )
            else -> ValidationResult(true)
        }
    }

    /**
     * Check if playlist name is unique
     */
    suspend fun isPlaylistNameUnique(name: String, excludeId: Long? = null): Boolean {
        val allPlaylists = playlistRepository.getAllPlaylists().first()
        return allPlaylists.none { playlist ->
            playlist.name.equals(name, ignoreCase = true) && playlist.id != excludeId
        }
    }

    /**
     * Get total number of playlists
     */
    suspend fun getPlaylistCount(): Int {
        return playlistRepository.getPlaylistCount()
    }
}

/**
 * Data class for validation results
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)