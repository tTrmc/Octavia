package com.octavia.player.presentation.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.octavia.player.data.model.Track
import com.octavia.player.presentation.components.TrackItem
import com.octavia.player.presentation.components.AlbumCard

/**
 * Home screen showing recently played, favorite tracks, and quick access
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
                title = { Text("Octavia") },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = true,
                    onClick = { /* Already on home */ }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Library") },
                    label = { Text("Library") },
                    selected = false,
                    onClick = onNavigateToLibrary
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Now Playing") },
                    label = { Text("Now Playing") },
                    selected = false,
                    onClick = onNavigateToPlayer
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick stats
            item {
                QuickStatsCard(
                    trackCount = uiState.trackCount,
                    albumCount = uiState.albumCount,
                    artistCount = uiState.artistCount,
                    totalDuration = uiState.totalDuration
                )
            }
            
            // Recently played
            if (uiState.recentlyPlayed.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Recently Played",
                        onSeeAllClick = onNavigateToLibrary
                    )
                }
                items(uiState.recentlyPlayed.take(5)) { track ->
                    TrackItem(
                        track = track,
                        onClick = { viewModel.playTrack(track) }
                    )
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
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
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
                        showFavoriteIcon = true
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
                        showQuality = true
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
    totalDuration: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Your Library",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(label = "Tracks", value = trackCount.toString())
                StatItem(label = "Albums", value = albumCount.toString())
                StatItem(label = "Artists", value = artistCount.toString())
            }
            
            if (totalDuration.isNotBlank()) {
                Text(
                    text = "Total playtime: $totalDuration",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(
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
        modifier = Modifier.fillMaxWidth(),
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
