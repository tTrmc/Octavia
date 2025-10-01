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
    private val getAlbumsUseCase: com.octavia.player.domain.usecase.GetAlbumsUseCase,
    private val playlistManagementUseCase: com.octavia.player.domain.usecase.PlaylistManagementUseCase,
    private val playbackControlUseCase: PlaybackControlUseCase,
    private val mediaPlaybackRepository: MediaPlaybackRepository,
    private val playbackStateDataStore: com.octavia.player.data.datastore.PlaybackStateDataStore
) : ViewModel() {

    // Separate the library data (slow-changing) from playback state (fast-changing)
    private val libraryData: StateFlow<LibraryData> = combine(
        getTracksUseCase.getAllTracks(),
        getTracksUseCase.getRecentlyPlayedTracks(10),
        getTracksUseCase.getRecentlyAddedTracks(),
        getTracksUseCase.getFavoriteTracks(),
        getTracksUseCase.getHiResTracks(),
        getAlbumsUseCase.getRecentlyAddedAlbums(10),
        playbackStateDataStore.getPlaybackState()
    ) { flows ->
        val tracks = flows[0] as List<Track>
        val recentlyPlayed = flows[1] as List<Track>
        val recentlyAdded = flows[2] as List<Track>
        val favorites = flows[3] as List<Track>
        val hiRes = flows[4] as List<Track>
        val recentlyAddedAlbums = flows[5] as List<Album>
        val savedState = flows[6] as com.octavia.player.data.datastore.SavedPlaybackState?

        // Cache expensive calculations
        val albumCount = tracks.groupBy { it.album ?: "Unknown Album" }.keys.size
        val artistCount = tracks.groupBy { it.artist ?: "Unknown Artist" }.keys.size
        val totalDurationMs = tracks.sumOf { it.durationMs }

        // Find last played track from saved state
        val lastPlayedTrack = savedState?.let { state ->
            tracks.find { it.id == state.trackId }
        }

        LibraryData(
            tracks = tracks,
            recentlyPlayed = recentlyPlayed,
            recentlyAdded = recentlyAdded.take(10),
            favoriteTracksPreview = favorites.take(5),
            hiResTracksPreview = hiRes.take(5),
            recentlyAddedAlbums = recentlyAddedAlbums,
            lastPlayedTrack = lastPlayedTrack,
            lastPlaybackPosition = savedState?.position ?: 0L,
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
        playbackState,
        playlistManagementUseCase.getAllPlaylists()
    ) { library, playback, playlists ->
        HomeUiState(
            recentlyPlayed = library.recentlyPlayed,
            recentlyAdded = library.recentlyAdded,
            favoriteTracksPreview = library.favoriteTracksPreview,
            hiResTracksPreview = library.hiResTracksPreview,
            recentlyAddedAlbums = library.recentlyAddedAlbums,
            lastPlayedTrack = library.lastPlayedTrack,
            lastPlaybackPosition = library.lastPlaybackPosition,
            currentlyPlayingTrack = playback.currentlyPlayingTrack,
            isPlaying = playback.isPlaying,
            currentPosition = playback.currentPosition,
            duration = playback.duration,
            progress = playback.progress,
            trackCount = library.trackCount,
            albumCount = library.albumCount,
            artistCount = library.artistCount,
            playlistCount = playlists.size,
            totalDuration = library.totalDuration
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun resumePlayback() {
        viewModelScope.launch {
            try {
                val lastPlayed = uiState.value.lastPlayedTrack
                val lastPosition = uiState.value.lastPlaybackPosition

                if (lastPlayed != null) {
                    android.util.Log.d("HomeViewModel", "Resuming playback: ${lastPlayed.displayTitle} at ${lastPosition}ms")
                    playbackControlUseCase.playTrack(lastPlayed)
                    // Seek to last position after a short delay to ensure track is loaded
                    kotlinx.coroutines.delay(200)
                    mediaPlaybackRepository.seekTo(lastPosition)
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Failed to resume playback", e)
            }
        }
    }

    fun playTrack(track: Track) {
        viewModelScope.launch {
            try {
                // Get current UI state to determine the best track context
                val currentState = uiState.value

                // Try to find the track in different contexts for better queue management
                val contextTracks = when {
                    // Check if track is in recently played list
                    currentState.recentlyPlayed.any { it.id == track.id } -> {
                        android.util.Log.d("HomeViewModel", "Playing track from recently played context")
                        currentState.recentlyPlayed
                    }
                    // Check if track is in favorites
                    currentState.favoriteTracksPreview.any { it.id == track.id } -> {
                        android.util.Log.d("HomeViewModel", "Playing track from favorites context")
                        // Use all tracks for now - getting live favorites is async
                        currentState.favoriteTracksPreview
                    }
                    // Check if track is in hi-res preview
                    currentState.hiResTracksPreview.any { it.id == track.id } -> {
                        android.util.Log.d("HomeViewModel", "Playing track from hi-res context")
                        // Use all tracks for now - getting live hi-res is async
                        currentState.hiResTracksPreview
                    }
                    // Fallback to all tracks for full library context
                    else -> {
                        android.util.Log.d("HomeViewModel", "Playing track with full library context")
                        libraryData.value.tracks
                    }
                }

                // Find track index in the selected context
                val trackIndex = contextTracks.indexOf(track)

                if (trackIndex >= 0 && contextTracks.size > 1) {
                    // Play with queue context for skip functionality
                    android.util.Log.d("HomeViewModel", "Playing track '${track.displayTitle}' at index $trackIndex of ${contextTracks.size} tracks")
                    playbackControlUseCase.playTracks(contextTracks, trackIndex)
                } else {
                    // Fallback to single track if context is too small or track not found
                    android.util.Log.w("HomeViewModel", "Using single track playback for: ${track.displayTitle}")
                    playbackControlUseCase.playTrack(track)
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Failed to play track: ${track.displayTitle}", e)
                // Final fallback
                playbackControlUseCase.playTrack(track)
            }
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
    val recentlyAddedAlbums: List<Album> = emptyList(),
    val lastPlayedTrack: Track? = null,
    val lastPlaybackPosition: Long = 0L,
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
    val lastPlayedTrack: Track? = null,
    val lastPlaybackPosition: Long = 0L,
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
) {
    val canResume: Boolean
        get() = lastPlayedTrack != null && lastPlaybackPosition > 0
}
