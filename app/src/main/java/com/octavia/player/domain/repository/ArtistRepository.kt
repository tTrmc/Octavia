package com.octavia.player.domain.repository

import com.octavia.player.data.model.Artist
import kotlinx.coroutines.flow.Flow

interface ArtistRepository {
    fun getAllArtists(): Flow<List<Artist>>
    suspend fun getArtistById(id: Long): Artist?
    fun searchArtists(query: String): Flow<List<Artist>>
    fun getRecentlyAddedArtists(limit: Int = 50): Flow<List<Artist>>
    suspend fun getArtistCount(): Int
}
