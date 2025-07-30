package com.octavia.player.domain.usecase

import androidx.paging.PagingData
import com.octavia.player.data.model.Track
import com.octavia.player.domain.repository.TrackRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving tracks with various filters
 */
class GetTracksUseCase @Inject constructor(
    private val trackRepository: TrackRepository
) {
    
    fun getAllTracksPaged(): Flow<PagingData<Track>> = 
        trackRepository.getAllTracksPaged()
    
    fun getAllTracks(): Flow<List<Track>> = 
        trackRepository.getAllTracks()
    
    fun getTracksByAlbum(albumId: Long): Flow<List<Track>> = 
        trackRepository.getTracksByAlbum(albumId)
    
    fun getTracksByArtist(artistId: Long): Flow<List<Track>> = 
        trackRepository.getTracksByArtist(artistId)
    
    fun getFavoriteTracks(): Flow<List<Track>> = 
        trackRepository.getFavoriteTracks()
    
    fun getMostPlayedTracks(limit: Int = 50): Flow<List<Track>> = 
        trackRepository.getMostPlayedTracks(limit)
    
    fun getRecentlyPlayedTracks(limit: Int = 50): Flow<List<Track>> = 
        trackRepository.getRecentlyPlayedTracks(limit)
    
    fun getRecentlyAddedTracks(): Flow<List<Track>> = 
        trackRepository.getRecentlyAddedTracks()
    
    fun getLosslessTracks(): Flow<List<Track>> = 
        trackRepository.getLosslessTracks()
    
    fun getHiResTracks(minSampleRate: Int = 48000): Flow<List<Track>> = 
        trackRepository.getHiResTracks(minSampleRate)
    
    fun searchTracks(query: String): Flow<List<Track>> = 
        trackRepository.searchTracks(query)
}