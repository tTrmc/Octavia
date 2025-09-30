package com.octavia.player.domain.repository

import com.octavia.player.data.model.Album
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for album operations
 * Provides access to album data
 */
interface AlbumRepository {

    /**
     * Get all albums as a Flow
     */
    fun getAllAlbums(): Flow<List<Album>>

    /**
     * Get a specific album by ID
     */
    suspend fun getAlbumById(id: Long): Album?

    /**
     * Get all albums by a specific artist
     */
    fun getAlbumsByArtist(artistId: Long): Flow<List<Album>>

    /**
     * Search albums by name or artist
     */
    fun searchAlbums(query: String): Flow<List<Album>>

    /**
     * Get recently added albums
     */
    fun getRecentlyAddedAlbums(limit: Int = 50): Flow<List<Album>>

    /**
     * Get album count
     */
    suspend fun getAlbumCount(): Int
}
