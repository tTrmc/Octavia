package com.octavia.player.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.octavia.player.data.database.dao.TrackDao
import com.octavia.player.data.database.dao.TrackFileInfo
import com.octavia.player.data.model.Track
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing track data
 * Provides a clean API for accessing track information
 */
@Singleton
class TrackRepository @Inject constructor(
    private val trackDao: TrackDao
) {
    
    companion object {
        private const val PAGE_SIZE = 50
    }
    
    /**
     * Get all tracks with paging support
     */
    fun getAllTracksPaged(): Flow<PagingData<Track>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                prefetchDistance = PAGE_SIZE / 2
            ),
            pagingSourceFactory = { trackDao.getAllTracksPaged() }
        ).flow
    }
    
    /**
     * Get all tracks as a flow
     */
    fun getAllTracks(): Flow<List<Track>> = trackDao.getAllTracksFlow()
    
    /**
     * Get a track by ID
     */
    suspend fun getTrackById(id: Long): Track? = trackDao.getTrackById(id)
    
    /**
     * Get a track by file path
     */
    suspend fun getTrackByPath(filePath: String): Track? = trackDao.getTrackByPath(filePath)
    
    /**
     * Get tracks by album
     */
    fun getTracksByAlbum(albumId: Long): Flow<List<Track>> = trackDao.getTracksByAlbum(albumId)
    
    /**
     * Get tracks by artist
     */
    fun getTracksByArtist(artistId: Long): Flow<List<Track>> = trackDao.getTracksByArtist(artistId)
    
    /**
     * Get tracks by genre
     */
    fun getTracksByGenre(genreId: Long): Flow<List<Track>> = trackDao.getTracksByGenre(genreId)
    
    /**
     * Get favorite tracks
     */
    fun getFavoriteTracks(): Flow<List<Track>> = trackDao.getFavoriteTracks()
    
    /**
     * Get most played tracks
     */
    fun getMostPlayedTracks(limit: Int = 50): Flow<List<Track>> =
        trackDao.getMostPlayedTracks(limit)

    /**
     * Get recently played tracks
     */
    fun getRecentlyPlayedTracks(limit: Int = 50): Flow<List<Track>> =
        trackDao.getRecentlyPlayedTracks(limit)
    
    /**
     * Get recently added tracks
     */
    fun getRecentlyAddedTracks(
        since: Long = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L,
    ): Flow<List<Track>> =
        trackDao.getRecentlyAddedTracks(since)
    
    /**
     * Search for tracks
     */
    fun searchTracks(query: String): Flow<List<Track>> = trackDao.searchTracks(query)
    
    /**
     * Get tracks by codec
     */
    fun getTracksByCodec(codecs: List<String>): Flow<List<Track>> = 
        trackDao.getTracksByCodec(codecs)
    
    /**
     * Get lossless tracks
     */
    fun getLosslessTracks(): Flow<List<Track>> = trackDao.getLosslessTracks()
    
    /**
     * Get hi-res tracks (>= 48kHz)
     */
    fun getHiResTracks(minSampleRate: Int = 48000): Flow<List<Track>> = 
        trackDao.getHiResTracks(minSampleRate)
    
    /**
     * Insert a single track
     */
    suspend fun insertTrack(track: Track): Long = trackDao.insertTrack(track)
    
    /**
     * Insert multiple tracks
     */
    suspend fun insertTracks(tracks: List<Track>): List<Long> = trackDao.insertTracks(tracks)
    
    /**
     * Update a track
     */
    suspend fun updateTrack(track: Track) = trackDao.updateTrack(track)
    
    /**
     * Increment play count for a track
     */
    suspend fun incrementPlayCount(trackId: Long) = 
        trackDao.incrementPlayCount(trackId, System.currentTimeMillis())
    
    /**
     * Update favorite status
     */
    suspend fun updateFavoriteStatus(trackId: Long, isFavorite: Boolean) = 
        trackDao.updateFavoriteStatus(trackId, isFavorite)
    
    /**
     * Delete a track
     */
    suspend fun deleteTrack(track: Track) = trackDao.deleteTrack(track)
    
    /**
     * Delete track by ID
     */
    suspend fun deleteTrackById(trackId: Long) = trackDao.deleteTrackById(trackId)
    
    /**
     * Delete tracks not in the provided paths (for cleanup after scan)
     */
    suspend fun deleteTracksNotInPaths(existingPaths: List<String>) = 
        trackDao.deleteTracksNotInPaths(existingPaths)
    
    /**
     * Get track count
     */
    suspend fun getTrackCount(): Int = trackDao.getTrackCount()
    
    /**
     * Get total duration of all tracks
     */
    suspend fun getTotalDuration(): Long = trackDao.getTotalDuration() ?: 0L
    
    /**
     * Get lossless track count
     */
    suspend fun getLosslessTrackCount(): Int = trackDao.getLosslessTrackCount()
    
    /**
     * Get all available codecs
     */
    suspend fun getAllCodecs(): List<String> = trackDao.getAllCodecs()
    
    /**
     * Get file information for all tracks (for scanning)
     */
    suspend fun getAllFileInfo(): List<TrackFileInfo> = trackDao.getAllFileInfo()
    
    /**
     * Check if a track is up to date
     */
    suspend fun isTrackUpToDate(filePath: String, lastModified: Long): Boolean = 
        trackDao.isTrackUpToDate(filePath, lastModified)
}
