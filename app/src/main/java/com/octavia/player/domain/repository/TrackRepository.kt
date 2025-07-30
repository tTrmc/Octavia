package com.octavia.player.domain.repository

import androidx.paging.PagingData
import com.octavia.player.data.model.Track
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for track operations
 * Abstraction layer for data access
 */
interface TrackRepository {
    
    // Read operations
    fun getAllTracksPaged(): Flow<PagingData<Track>>
    fun getAllTracks(): Flow<List<Track>>
    suspend fun getTrackById(id: Long): Track?
    suspend fun getTrackByPath(filePath: String): Track?
    
    // Filtered queries
    fun getTracksByAlbum(albumId: Long): Flow<List<Track>>
    fun getTracksByArtist(artistId: Long): Flow<List<Track>>
    fun getTracksByGenre(genreId: Long): Flow<List<Track>>
    fun getFavoriteTracks(): Flow<List<Track>>
    fun getMostPlayedTracks(limit: Int = 50): Flow<List<Track>>
    fun getRecentlyPlayedTracks(limit: Int = 50): Flow<List<Track>>
    fun getRecentlyAddedTracks(since: Long = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L): Flow<List<Track>>
    fun getLosslessTracks(): Flow<List<Track>>
    fun getHiResTracks(minSampleRate: Int = 48000): Flow<List<Track>>
    
    // Search
    fun searchTracks(query: String): Flow<List<Track>>
    fun getTracksByCodec(codecs: List<String>): Flow<List<Track>>
    
    // Write operations
    suspend fun insertTrack(track: Track): Long
    suspend fun insertTracks(tracks: List<Track>): List<Long>
    suspend fun updateTrack(track: Track)
    suspend fun deleteTrack(track: Track)
    suspend fun deleteTrackById(trackId: Long)
    suspend fun deleteTracksNotInPaths(existingPaths: List<String>)
    
    // Statistics
    suspend fun incrementPlayCount(trackId: Long)
    suspend fun updateFavoriteStatus(trackId: Long, isFavorite: Boolean)
    suspend fun getTrackCount(): Int
    suspend fun getTotalDuration(): Long
    suspend fun getLosslessTrackCount(): Int
    suspend fun getAllCodecs(): List<String>
    
    // File management
    suspend fun isTrackUpToDate(filePath: String, lastModified: Long): Boolean
}