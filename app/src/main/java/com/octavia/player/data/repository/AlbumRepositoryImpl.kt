package com.octavia.player.data.repository

import com.octavia.player.data.database.dao.AlbumDao
import com.octavia.player.data.model.Album
import com.octavia.player.domain.repository.AlbumRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AlbumRepository
 * Handles all album-related data operations
 */
@Singleton
class AlbumRepositoryImpl @Inject constructor(
    private val albumDao: AlbumDao
) : AlbumRepository {

    override fun getAllAlbums(): Flow<List<Album>> {
        return albumDao.getAllAlbumsFlow()
    }

    override suspend fun getAlbumById(id: Long): Album? {
        return albumDao.getAlbumById(id)
    }

    override fun getAlbumsByArtist(artistId: Long): Flow<List<Album>> {
        return albumDao.getAlbumsByArtist(artistId)
    }

    override fun searchAlbums(query: String): Flow<List<Album>> {
        return albumDao.searchAlbums(query)
    }

    override fun getRecentlyAddedAlbums(limit: Int): Flow<List<Album>> {
        return albumDao.getRecentlyAddedAlbums(limit)
    }

    override suspend fun getAlbumCount(): Int {
        return albumDao.getAlbumCount()
    }
}
