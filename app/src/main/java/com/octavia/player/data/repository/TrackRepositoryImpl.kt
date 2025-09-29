package com.octavia.player.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.octavia.player.data.database.dao.TrackDao
import com.octavia.player.data.model.Track
import com.octavia.player.domain.repository.TrackRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of TrackRepository interface
 * Handles all track-related data operations
 */
@Singleton
class TrackRepositoryImpl @Inject constructor(
    private val trackDao: TrackDao
) : TrackRepository {

    companion object {
        private const val PAGE_SIZE = 50
    }

    override fun getAllTracksPaged(): Flow<PagingData<Track>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                prefetchDistance = PAGE_SIZE / 2
            ),
            pagingSourceFactory = { trackDao.getAllTracksPaged() }
        ).flow
    }

    override fun getAllTracks(): Flow<List<Track>> = trackDao.getAllTracksFlow()

    override suspend fun getTrackById(id: Long): Track? = trackDao.getTrackById(id)

    override suspend fun getTrackByPath(filePath: String): Track? = trackDao.getTrackByPath(filePath)

    override fun getTracksByAlbum(albumId: Long): Flow<List<Track>> = trackDao.getTracksByAlbum(albumId)

    override fun getTracksByArtist(artistId: Long): Flow<List<Track>> = trackDao.getTracksByArtist(artistId)

    override fun getTracksByGenre(genreId: Long): Flow<List<Track>> = trackDao.getTracksByGenre(genreId)

    override fun getFavoriteTracks(): Flow<List<Track>> = trackDao.getFavoriteTracks()

    override fun getMostPlayedTracks(limit: Int): Flow<List<Track>> =
        trackDao.getMostPlayedTracks(limit)

    override fun getRecentlyPlayedTracks(limit: Int): Flow<List<Track>> =
        trackDao.getRecentlyPlayedTracks(limit)

    override fun getRecentlyAddedTracks(since: Long): Flow<List<Track>> =
        trackDao.getRecentlyAddedTracks(since)

    override fun searchTracks(query: String): Flow<List<Track>> = trackDao.searchTracks(query)

    override fun getTracksByCodec(codecs: List<String>): Flow<List<Track>> =
        trackDao.getTracksByCodec(codecs)

    override fun getLosslessTracks(): Flow<List<Track>> = trackDao.getLosslessTracks()

    override fun getHiResTracks(minSampleRate: Int): Flow<List<Track>> =
        trackDao.getHiResTracks(minSampleRate)

    override suspend fun insertTrack(track: Track): Long = trackDao.insertTrack(track)

    override suspend fun insertTracks(tracks: List<Track>): List<Long> = trackDao.insertTracks(tracks)

    override suspend fun updateTrack(track: Track) = trackDao.updateTrack(track)

    override suspend fun incrementPlayCount(trackId: Long) =
        trackDao.incrementPlayCount(trackId, System.currentTimeMillis())

    override suspend fun updateFavoriteStatus(trackId: Long, isFavorite: Boolean) =
        trackDao.updateFavoriteStatus(trackId, isFavorite)

    override suspend fun deleteTrack(track: Track) = trackDao.deleteTrack(track)

    override suspend fun deleteTrackById(trackId: Long) = trackDao.deleteTrackById(trackId)

    override suspend fun deleteTracksNotInPaths(existingPaths: List<String>) =
        trackDao.deleteTracksNotInPaths(existingPaths)

    override suspend fun getTrackCount(): Int = trackDao.getTrackCount()

    override suspend fun getTotalDuration(): Long = trackDao.getTotalDuration() ?: 0L

    override suspend fun getLosslessTrackCount(): Int = trackDao.getLosslessTrackCount()

    override suspend fun getAllCodecs(): List<String> = trackDao.getAllCodecs()

    override suspend fun isTrackUpToDate(filePath: String, lastModified: Long): Boolean =
        trackDao.isTrackUpToDate(filePath, lastModified)

    override suspend fun getTracksWithoutArtwork(limit: Int): List<Track> =
        trackDao.getTracksWithoutArtwork(limit)

    override suspend fun clearAllArtworkPaths() {
        trackDao.clearAllArtworkPaths()
    }
}