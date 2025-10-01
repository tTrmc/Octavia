package com.octavia.player.presentation.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.octavia.player.presentation.components.AlbumCard
import com.octavia.player.presentation.components.ContinueListeningCard
import com.octavia.player.presentation.components.MiniPlayer
import com.octavia.player.presentation.components.TrackItem

/**
 * Premium home screen with Spotify/Tidal-like design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToLibrary: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Octavia",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Column {
                // MiniPlayer - show when a track is playing
                if (uiState.currentlyPlayingTrack != null) {
                    MiniPlayer(
                        track = uiState.currentlyPlayingTrack,
                        isPlaying = uiState.isPlaying,
                        progress = uiState.progress,
                        onPlayPause = { viewModel.togglePlayPause() },
                        onNext = { viewModel.skipToNext() },
                        onPrevious = { viewModel.skipToPrevious() },
                        onClick = onNavigateToPlayer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    NavigationBarItem(
                        icon = {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = "Home",
                                modifier = Modifier.size(26.dp)
                            )
                        },
                        label = { Text("Home", style = MaterialTheme.typography.labelMedium) },
                        selected = true,
                        onClick = { /* Already on home */ },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                Icons.Default.LibraryMusic,
                                contentDescription = "Library",
                                modifier = Modifier.size(26.dp)
                            )
                        },
                        label = { Text("Library", style = MaterialTheme.typography.labelMedium) },
                        selected = false,
                        onClick = onNavigateToLibrary,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Library Stats Card
            if (uiState.trackCount > 0) {
                item {
                    LibraryStatsCard(
                        trackCount = uiState.trackCount,
                        albumCount = uiState.albumCount,
                        artistCount = uiState.artistCount,
                        playlistCount = uiState.playlistCount,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }

            // Continue Listening Card
            uiState.lastPlayedTrack?.let { lastTrack ->
                if (uiState.canResume) {
                    item {
                        ContinueListeningCard(
                            track = lastTrack,
                            position = uiState.lastPlaybackPosition,
                            onResumeClick = { viewModel.resumePlayback() }
                        )
                    }
                }
            }

            // Recently played
            if (uiState.recentlyPlayed.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Recently played",
                        onSeeAllClick = onNavigateToLibrary
                    )
                }

                items(uiState.recentlyPlayed.take(6)) { track ->
                    TrackItem(
                        track = track,
                        onClick = { viewModel.playTrack(track) },
                        showQuality = true,
                        showFavoriteIcon = false,
                        isPlaying = uiState.currentlyPlayingTrack?.id == track.id
                    )
                }
            }

            // Made for you section
            if (uiState.recentlyAddedAlbums.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Made for you",
                        onSeeAllClick = onNavigateToLibrary
                    )
                }

                // Albums horizontal scroll
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.recentlyAddedAlbums) { album ->
                            AlbumCard(
                                album = album,
                                onClick = { /* TODO: Navigate to album */ }
                            )
                        }
                    }
                }
            }

            // Recently added
            if (uiState.recentlyAdded.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Recently Added",
                        onSeeAllClick = onNavigateToLibrary
                    )
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp)
                    ) {
                        items(uiState.recentlyAddedAlbums.take(10)) { album ->
                            AlbumCard(
                                album = album,
                                onClick = { /* Navigate to album */ }
                            )
                        }
                    }
                }
            }

            // Favorites
            if (uiState.favoriteTracksPreview.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Favorites",
                        onSeeAllClick = onNavigateToLibrary
                    )
                }

                items(uiState.favoriteTracksPreview.take(5)) { track ->
                    TrackItem(
                        track = track,
                        onClick = { viewModel.playTrack(track) },
                        showFavoriteIcon = true,
                        isPlaying = uiState.currentlyPlayingTrack?.id == track.id
                    )
                }
            }

            // Hi-Res tracks
            if (uiState.hiResTracksPreview.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Hi-Res Audio",
                        subtitle = "High-quality lossless tracks",
                        onSeeAllClick = onNavigateToLibrary
                    )
                }
                items(uiState.hiResTracksPreview.take(5)) { track ->
                    TrackItem(
                        track = track,
                        onClick = { viewModel.playTrack(track) },
                        showQuality = true,
                        isPlaying = uiState.currentlyPlayingTrack?.id == track.id
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickStatsCard(
    trackCount: Int,
    albumCount: Int,
    artistCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            QuickStatItem(
                value = trackCount.toString(),
                label = "Songs"
            )
            QuickStatItem(
                value = albumCount.toString(),
                label = "Albums"
            )
            QuickStatItem(
                value = artistCount.toString(),
                label = "Artists"
            )
        }
    }
}

@Composable
private fun QuickStatItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String? = null,
    onSeeAllClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        TextButton(onClick = onSeeAllClick) {
            Text("See All")
        }
    }
}

@Composable
private fun LibraryStatsCard(
    trackCount: Int,
    albumCount: Int,
    artistCount: Int,
    playlistCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                count = trackCount,
                label = "Songs",
                icon = Icons.Default.MusicNote
            )
            StatItem(
                count = albumCount,
                label = "Albums",
                icon = Icons.Default.Album
            )
            StatItem(
                count = artistCount,
                label = "Artists",
                icon = Icons.Default.Person
            )
            StatItem(
                count = playlistCount,
                label = "Playlists",
                icon = Icons.AutoMirrored.Filled.PlaylistPlay
            )
        }
    }
}

@Composable
private fun StatItem(
    count: Int,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
