package com.octavia.player.presentation.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.octavia.player.presentation.components.AlbumCard
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
            LargeTopAppBar(
                title = { 
                    Text(
                        "Good evening",
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
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                NavigationBarItem(
                    icon = { 
                        Icon(
                            Icons.Default.Home, 
                            contentDescription = "Home",
                            modifier = Modifier.size(24.dp)
                        ) 
                    },
                    label = { Text("Home", style = MaterialTheme.typography.labelSmall) },
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
                            modifier = Modifier.size(24.dp)
                        ) 
                    },
                    label = { Text("Library", style = MaterialTheme.typography.labelSmall) },
                    selected = false,
                    onClick = onNavigateToLibrary,
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
                            Icons.Default.PlayArrow, 
                            contentDescription = "Now Playing",
                            modifier = Modifier.size(24.dp)
                        ) 
                    },
                    label = { Text("Now Playing", style = MaterialTheme.typography.labelSmall) },
                    selected = false,
                    onClick = onNavigateToPlayer,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Quick access tiles - Spotify-style
            item {
                QuickAccessSection(onNavigateToLibrary = onNavigateToLibrary)
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
                        showFavoriteIcon = false
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
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
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
private fun QuickAccessSection(onNavigateToLibrary: () -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(4) { index ->
            val (title, icon, backgroundColor, iconColor) = when (index) {
                0 -> Quadruple(
                    "Liked Songs",
                    Icons.Default.Favorite,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    MaterialTheme.colorScheme.primary
                )
                1 -> Quadruple(
                    "Recently Played",
                    Icons.Default.History,
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                    MaterialTheme.colorScheme.secondary
                )
                2 -> Quadruple(
                    "Hi-Res Audio",
                    Icons.Default.HighQuality,
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                    MaterialTheme.colorScheme.tertiary
                )
                else -> Quadruple(
                    "Downloaded",
                    Icons.Default.Download,
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    MaterialTheme.colorScheme.onSurface
                )
            }
            
            QuickAccessTile(
                title = title,
                icon = icon,
                backgroundColor = backgroundColor,
                iconColor = iconColor,
                onClick = onNavigateToLibrary
            )
        }
    }
}

@Composable
private fun QuickAccessTile(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: androidx.compose.ui.graphics.Color,
    iconColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
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

// Helper data class for multiple values
private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
