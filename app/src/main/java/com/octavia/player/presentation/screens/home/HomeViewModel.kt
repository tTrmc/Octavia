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

    // Separate the library data (slow-changing) from playback state (fast-changing)
    private val libraryData: StateFlow<LibraryData> = combine(
        getTracksUseCase.getAllTracks(),
        getTracksUseCase.getRecentlyPlayedTracks(10),
        getTracksUseCase.getRecentlyAddedTracks(),
        getTracksUseCase.getFavoriteTracks(),
        getTracksUseCase.getHiResTracks()
    ) { tracks, recentlyPlayed, recentlyAdded, favorites, hiRes ->
        // Cache expensive calculations
        val albumCount = tracks.groupBy { it.album ?: "Unknown Album" }.keys.size
        val artistCount = tracks.groupBy { it.artist ?: "Unknown Artist" }.keys.size
        val totalDurationMs = tracks.sumOf { it.durationMs }

        LibraryData(
            tracks = tracks,
            recentlyPlayed = recentlyPlayed,
            recentlyAdded = recentlyAdded.take(10),
            favoriteTracksPreview = favorites.take(5),
            hiResTracksPreview = hiRes.take(5),
            trackCount = tracks.size,
            albumCount = albumCount,
            artistCount = artistCount,
            totalDuration = formatDuration(totalDurationMs)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryData()
    )

    // Separate playback state (fast-changing)
    private val playbackState: StateFlow<PlaybackData> = combine(
        mediaPlaybackRepository.currentTrack,
        mediaPlaybackRepository.playerState,
        mediaPlaybackRepository.currentPosition
    ) { currentTrack, playerState, currentPosition ->
        PlaybackData(
            currentlyPlayingTrack = currentTrack,
            isPlaying = playerState.isPlaying,
            currentPosition = currentPosition,
            duration = playerState.duration,
            progress = if (playerState.duration > 0) currentPosition.toFloat() / playerState.duration else 0f
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlaybackData()
    )

    // Combine only when needed
    val uiState: StateFlow<HomeUiState> = combine(
        libraryData,
        playbackState
    ) { library, playback ->
        HomeUiState(
            recentlyPlayed = library.recentlyPlayed,
            recentlyAdded = library.recentlyAdded,
            favoriteTracksPreview = library.favoriteTracksPreview,
            hiResTracksPreview = library.hiResTracksPreview,
            recentlyAddedAlbums = emptyList(), // TODO: Implement album data
            currentlyPlayingTrack = playback.currentlyPlayingTrack,
            isPlaying = playback.isPlaying,
            currentPosition = playback.currentPosition,
            duration = playback.duration,
            progress = playback.progress,
            trackCount = library.trackCount,
            albumCount = library.albumCount,
            artistCount = library.artistCount,
            playlistCount = 0, // TODO: Implement playlists
            totalDuration = library.totalDuration
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
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
 * Cached library data (slow-changing)
 */
private data class LibraryData(
    val tracks: List<Track> = emptyList(),
    val recentlyPlayed: List<Track> = emptyList(),
    val recentlyAdded: List<Track> = emptyList(),
    val favoriteTracksPreview: List<Track> = emptyList(),
    val hiResTracksPreview: List<Track> = emptyList(),
    val trackCount: Int = 0,
    val albumCount: Int = 0,
    val artistCount: Int = 0,
    val totalDuration: String = ""
)

/**
 * Playback state data (fast-changing)
 */
private data class PlaybackData(
    val currentlyPlayingTrack: Track? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val progress: Float = 0f
)

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
