package com.octavia.player.data.repository

import com.octavia.player.data.database.dao.ArtistDao
import com.octavia.player.data.model.Artist
import com.octavia.player.domain.repository.ArtistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtistRepositoryImpl @Inject constructor(
    private val artistDao: ArtistDao
) : ArtistRepository {

    override fun getAllArtists(): Flow<List<Artist>> {
        return artistDao.getAllArtistsFlow()
    }

    override suspend fun getArtistById(id: Long): Artist? {
        return artistDao.getArtistById(id)
    }

    override fun searchArtists(query: String): Flow<List<Artist>> {
        return artistDao.searchArtists(query)
    }

    override fun getRecentlyAddedArtists(limit: Int): Flow<List<Artist>> {
        return artistDao.getRecentlyAddedArtists(limit)
    }

    override suspend fun getArtistCount(): Int {
        return artistDao.getArtistCount()
    }
}
