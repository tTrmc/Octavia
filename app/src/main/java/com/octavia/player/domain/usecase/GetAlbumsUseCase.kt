package com.octavia.player.domain.usecase

import com.octavia.player.data.model.Album
import com.octavia.player.data.model.Track
import com.octavia.player.domain.repository.AlbumRepository
import com.octavia.player.domain.repository.TrackRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case for retrieving albums with various filters
 */
class GetAlbumsUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val trackRepository: TrackRepository
) {

    fun getAllAlbums(): Flow<List<Album>> =
        albumRepository.getAllAlbums()

    fun getAlbumsByArtist(artistId: Long): Flow<List<Album>> =
        albumRepository.getAlbumsByArtist(artistId)

    fun searchAlbums(query: String): Flow<List<Album>> =
        albumRepository.searchAlbums(query)

    fun getRecentlyAddedAlbums(limit: Int = 50): Flow<List<Album>> =
        albumRepository.getRecentlyAddedAlbums(limit)

    suspend fun getAlbumCount(): Int =
        albumRepository.getAlbumCount()

    /**
     * Get album with its tracks
     * Returns a pair of Album and its tracks ordered by disc/track number
     */
    suspend fun getAlbumWithTracks(albumId: Long): Pair<Album?, List<Track>> {
        val album = albumRepository.getAlbumById(albumId)
        val tracks = if (album != null) {
            trackRepository.getTracksByAlbum(albumId).first()
        } else {
            emptyList()
        }
        return Pair(album, tracks)
    }
}
