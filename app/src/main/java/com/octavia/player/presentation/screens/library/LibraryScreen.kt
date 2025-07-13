package com.octavia.player.presentation.screens.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.octavia.player.presentation.components.TrackItem
import com.octavia.player.presentation.components.AlbumCard

/**
 * Library screen with tabs for different media types
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAlbum: (Long) -> Unit,
    onNavigateToArtist: (Long) -> Unit,
    onNavigateToPlaylist: (Long) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { LibraryTab.entries.size })
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.scanLibrary() },
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Scan for music"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab row
            TabRow(selectedTabIndex = pagerState.currentPage) {
                LibraryTab.entries.forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            // Handle tab click - would need to implement scroll to page
                        },
                        text = { Text(tab.title) }
                    )
                }
            }
            
            // Content pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (LibraryTab.entries[page]) {
                    LibraryTab.TRACKS -> TracksTab(
                        tracks = uiState.tracks,
                        isLoading = uiState.isLoading,
                        error = uiState.error,
                        onTrackClick = { track -> viewModel.playTrack(track) }
                    )
                    LibraryTab.ALBUMS -> AlbumsTab(
                        albums = uiState.albums,
                        onAlbumClick = onNavigateToAlbum
                    )
                    LibraryTab.ARTISTS -> ArtistsTab(
                        artists = uiState.artists,
                        onArtistClick = onNavigateToArtist
                    )
                    LibraryTab.PLAYLISTS -> PlaylistsTab(
                        playlists = uiState.playlists,
                        onPlaylistClick = onNavigateToPlaylist
                    )
                }
            }
        }
    }
}

@Composable
private fun TracksTab(
    tracks: List<com.octavia.player.data.model.Track>,
    isLoading: Boolean,
    error: String?,
    onTrackClick: (com.octavia.player.data.model.Track) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading && tracks.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Scanning for music...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Error: $error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            tracks.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "No music found",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "Tap the refresh button to scan for music files on your device",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tracks) { track ->
                        TrackItem(
                            track = track,
                            onClick = { onTrackClick(track) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumsTab(
    albums: List<com.octavia.player.data.model.Album>,
    onAlbumClick: (Long) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(albums) { album ->
            AlbumCard(
                album = album,
                onClick = { onAlbumClick(album.id) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ArtistsTab(
    artists: List<com.octavia.player.data.model.Artist>,
    onArtistClick: (Long) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(artists) { artist ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                onClick = { onArtistClick(artist.id) }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = artist.displayName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${artist.albumCount} albums • ${artist.trackCount} tracks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistsTab(
    playlists: List<com.octavia.player.data.model.Playlist>,
    onPlaylistClick: (Long) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(playlists) { playlist ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                onClick = { onPlaylistClick(playlist.id) }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = playlist.displayName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${playlist.trackCount} tracks • ${playlist.formattedDuration}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Library tab enumeration
 */
enum class LibraryTab(val title: String) {
    TRACKS("Tracks"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    PLAYLISTS("Playlists")
}
