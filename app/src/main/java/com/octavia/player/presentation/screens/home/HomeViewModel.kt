package com.octavia.player.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.octavia.player.data.model.Album
import com.octavia.player.data.model.Track
import com.octavia.player.domain.repository.MediaPlaybackRepository
import com.octavia.player.domain.usecase.GetTracksUseCase
import com.octavia.player.domain.usecase.PlaybackControlUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Home screen
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTracksUseCase: GetTracksUseCase,
    private val playbackControlUseCase: PlaybackControlUseCase,
    private val mediaPlaybackRepository: MediaPlaybackRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> =
        getTracksUseCase.getAllTracks()
            .combine(getTracksUseCase.getRecentlyPlayedTracks(10)) { tracks, recentlyPlayed ->
                tracks to recentlyPlayed
            }
            .combine(getTracksUseCase.getRecentlyAddedTracks()) { (tracks, recentlyPlayed), recentlyAdded ->
                Triple(tracks, recentlyPlayed, recentlyAdded)
            }
            .combine(getTracksUseCase.getFavoriteTracks()) { (tracks, recentlyPlayed, recentlyAdded), favorites ->
                listOf(tracks, recentlyPlayed, recentlyAdded, favorites)
            }
            .combine(getTracksUseCase.getHiResTracks()) { trackLists, hiRes ->
                trackLists + listOf(hiRes)
            }
            .combine(mediaPlaybackRepository.currentTrack) { trackLists, currentTrack ->
                trackLists to currentTrack
            }
            .combine(mediaPlaybackRepository.playerState) { (trackLists, currentTrack), playerState ->
                (trackLists to currentTrack) to playerState
            }
            .combine(mediaPlaybackRepository.currentPosition) { combined, currentPosition ->
                val trackListsAndCurrentTrack = combined.first
                val playerState = combined.second
                val trackLists = trackListsAndCurrentTrack.first
                val currentTrack = trackListsAndCurrentTrack.second
                val (tracks, recentlyPlayed, recentlyAdded, favorites, hiRes) = trackLists

                // Calculate stats from tracks
                val albums = tracks.groupBy { it.album ?: "Unknown Album" }.keys.size
                val artists = tracks.groupBy { it.artist ?: "Unknown Artist" }.keys.size

                HomeUiState(
                    recentlyPlayed = recentlyPlayed,
                    recentlyAdded = recentlyAdded.take(10),
                    favoriteTracksPreview = favorites.take(5),
                    hiResTracksPreview = hiRes.take(5),
                    recentlyAddedAlbums = emptyList(), // TODO: Implement album data
                    currentlyPlayingTrack = currentTrack,
                    isPlaying = playerState.isPlaying,
                    currentPosition = currentPosition,
                    duration = playerState.duration,
                    progress = if (playerState.duration > 0) currentPosition.toFloat() / playerState.duration else 0f,
                    trackCount = tracks.size,
                    albumCount = albums,
                    artistCount = artists,
                    playlistCount = 0, // TODO: Implement playlists
                    totalDuration = formatDuration(tracks.sumOf { it.durationMs })
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = HomeUiState()
            )

    init {
        // Stats will be loaded separately if needed
    }

    fun playTrack(track: Track) {
        viewModelScope.launch {
            playbackControlUseCase.playTrack(track)
        }
    }

    fun togglePlayPause() {
        playbackControlUseCase.togglePlayPause()
    }

    fun skipToNext() {
        viewModelScope.launch {
            playbackControlUseCase.skipToNext()
        }
    }

    fun skipToPrevious() {
        viewModelScope.launch {
            playbackControlUseCase.skipToPrevious()
        }
    }

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val days = totalSeconds / (24 * 3600)
        val hours = (totalSeconds % (24 * 3600)) / 3600
        val minutes = (totalSeconds % 3600) / 60

        return when {
            days > 0 -> "${days}d ${hours}h"
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }
}

/**
 * UI state for the Home screen
 */
data class HomeUiState(
    val recentlyPlayed: List<Track> = emptyList(),
    val recentlyAdded: List<Track> = emptyList(),
    val favoriteTracksPreview: List<Track> = emptyList(),
    val hiResTracksPreview: List<Track> = emptyList(),
    val recentlyAddedAlbums: List<Album> = emptyList(),
    val currentlyPlayingTrack: Track? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val progress: Float = 0f,
    val trackCount: Int = 0,
    val albumCount: Int = 0,
    val artistCount: Int = 0,
    val playlistCount: Int = 0,
    val totalDuration: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)
