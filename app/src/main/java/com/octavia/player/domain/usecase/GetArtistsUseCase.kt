package com.octavia.player.domain.usecase

import com.octavia.player.data.model.Artist
import com.octavia.player.data.model.Track
import com.octavia.player.domain.repository.ArtistRepository
import com.octavia.player.domain.repository.TrackRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetArtistsUseCase @Inject constructor(
    private val artistRepository: ArtistRepository,
    private val trackRepository: TrackRepository
) {

    fun getAllArtists(): Flow<List<Artist>> =
        artistRepository.getAllArtists()

    fun searchArtists(query: String): Flow<List<Artist>> =
        artistRepository.searchArtists(query)

    fun getRecentlyAddedArtists(limit: Int = 50): Flow<List<Artist>> =
        artistRepository.getRecentlyAddedArtists(limit)

    suspend fun getArtistCount(): Int =
        artistRepository.getArtistCount()

    /**
     * Get artist with their tracks
     * Returns a pair of Artist and their tracks ordered by album/track number
     */
    suspend fun getArtistWithTracks(artistId: Long): Pair<Artist?, List<Track>> {
        val artist = artistRepository.getArtistById(artistId)
        val tracks = if (artist != null) {
            trackRepository.getTracksByArtist(artistId).first()
        } else {
            emptyList()
        }
        return Pair(artist, tracks)
    }
}
