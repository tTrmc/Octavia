package com.octavia.player.presentation.screens.library

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.octavia.player.data.model.Track
import com.octavia.player.presentation.components.AddToPlaylistSheet
import com.octavia.player.presentation.components.AlbumCard
import com.octavia.player.presentation.components.ArtistCard
import com.octavia.player.presentation.components.CreatePlaylistDialog
import com.octavia.player.presentation.components.MiniPlayer
import com.octavia.player.presentation.components.PlaylistCardGrid
import com.octavia.player.presentation.components.TrackItem
import kotlinx.coroutines.launch

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
    onNavigateToPlayer: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { LibraryTab.entries.size })
    val coroutineScope = rememberCoroutineScope()

    // Search state
    var searchQuery by remember { mutableStateOf("") }

    // Dialog state
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistSheet by remember { mutableStateOf(false) }
    var selectedTrack by remember { mutableStateOf<Track?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { 
                            Text(
                                "Search library...",
                                style = MaterialTheme.typography.bodyMedium
                            ) 
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search, 
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" }
                                ) {
                                    Icon(
                                        Icons.Default.Close, 
                                        contentDescription = "Clear search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.scanLibrary() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
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
                        selected = false,
                        onClick = onNavigateBack,
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
                        selected = true,
                        onClick = { /* Already on library */ },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            // Show different FABs based on current tab
            when (LibraryTab.entries[pagerState.currentPage]) {
                LibraryTab.TRACKS -> {
                    if (uiState.tracks.isNotEmpty()) {
                        FloatingActionButton(
                            onClick = {
                                // Shuffle all tracks
                                viewModel.playTracks(uiState.tracks.shuffled(), 0)
                            }
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = "Shuffle all")
                        }
                    }
                }

                LibraryTab.PLAYLISTS -> {
                    FloatingActionButton(
                        onClick = { showCreatePlaylistDialog = true }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create playlist")
                    }
                }

                else -> {
                    FloatingActionButton(
                        onClick = { viewModel.scanLibrary() },
                        containerColor = MaterialTheme.colorScheme.secondary
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh library")
                    }
                }
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
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(tab.title)
                                when (tab) {
                                    LibraryTab.TRACKS -> if (uiState.tracks.isNotEmpty()) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ) {
                                            Text(
                                                text = "${uiState.tracks.size}",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }

                                    LibraryTab.ALBUMS -> if (uiState.albums.isNotEmpty()) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ) {
                                            Text(
                                                text = "${uiState.albums.size}",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }

                                    LibraryTab.ARTISTS -> if (uiState.artists.isNotEmpty()) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ) {
                                            Text(
                                                text = "${uiState.artists.size}",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }

                                    LibraryTab.PLAYLISTS -> if (uiState.playlists.isNotEmpty()) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ) {
                                            Text(
                                                text = "${uiState.playlists.size}",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // Content pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (LibraryTab.entries[page]) {
                    LibraryTab.TRACKS -> {
                        var currentSort by remember { mutableStateOf(SortOption.NAME_ASC) }

                        Column {
                            if (uiState.tracks.isNotEmpty()) {
                                SortFilterBar(
                                    currentSort = currentSort,
                                    onSortChange = { currentSort = it }
                                )
                            }

                            TracksTab(
                                tracks = if (searchQuery.isNotEmpty()) {
                                    uiState.tracks.filter { track ->
                                        track.displayTitle.contains(
                                            searchQuery,
                                            ignoreCase = true
                                        ) ||
                                                track.displayArtist.contains(
                                                    searchQuery,
                                                    ignoreCase = true
                                                ) ||
                                                track.displayAlbum.contains(
                                                    searchQuery,
                                                    ignoreCase = true
                                                )
                                    }
                                } else uiState.tracks,
                                isLoading = uiState.isLoading,
                                error = uiState.error,
                                currentlyPlayingTrack = uiState.currentlyPlayingTrack,
                                onTrackClick = { track -> viewModel.playTrack(track) },
                                onMoreClick = { track ->
                                    selectedTrack = track
                                    showAddToPlaylistSheet = true
                                },
                                sortOption = currentSort
                            )
                        }
                    }

                    LibraryTab.ALBUMS -> AlbumsTab(
                        albums = if (searchQuery.isNotEmpty()) {
                            uiState.albums.filter { album ->
                                album.displayName.contains(searchQuery, ignoreCase = true) ||
                                        album.displayArtist.contains(searchQuery, ignoreCase = true)
                            }
                        } else uiState.albums,
                        onAlbumClick = onNavigateToAlbum
                    )

                    LibraryTab.ARTISTS -> ArtistsTab(
                        artists = if (searchQuery.isNotEmpty()) {
                            uiState.artists.filter { artist ->
                                artist.displayName.contains(searchQuery, ignoreCase = true)
                            }
                        } else uiState.artists,
                        onArtistClick = onNavigateToArtist
                    )

                    LibraryTab.PLAYLISTS -> PlaylistsTab(
                        playlists = if (searchQuery.isNotEmpty()) {
                            uiState.playlists.filter { playlist ->
                                playlist.displayName.contains(searchQuery, ignoreCase = true)
                            }
                        } else uiState.playlists,
                        onPlaylistClick = onNavigateToPlaylist
                    )
                }
            }
        }
    }

    // Create Playlist Dialog
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name, description ->
                viewModel.createPlaylist(name, description)
                showCreatePlaylistDialog = false
            }
        )
    }

    // Add to Playlist Sheet
    if (showAddToPlaylistSheet && selectedTrack != null) {
        AddToPlaylistSheet(
            track = selectedTrack!!,
            playlists = uiState.playlists,
            onDismiss = {
                showAddToPlaylistSheet = false
                selectedTrack = null
            },
            onCreateNewPlaylist = {
                showAddToPlaylistSheet = false
                showCreatePlaylistDialog = true
            },
            onAddToPlaylist = { playlist ->
                selectedTrack?.let { track ->
                    viewModel.addTrackToPlaylist(playlist.id, track.id)
                }
                showAddToPlaylistSheet = false
                selectedTrack = null
            }
        )
    }
}

@Composable
private fun TracksTab(
    tracks: List<com.octavia.player.data.model.Track>,
    isLoading: Boolean,
    error: String?,
    currentlyPlayingTrack: com.octavia.player.data.model.Track?,
    onTrackClick: (com.octavia.player.data.model.Track) -> Unit,
    onMoreClick: (com.octavia.player.data.model.Track) -> Unit,
    sortOption: SortOption = SortOption.NAME_ASC
) {
    val sortedTracks = remember(tracks, sortOption) {
        when (sortOption) {
            SortOption.NAME_ASC -> tracks.sortedBy { it.displayTitle.lowercase() }
            SortOption.NAME_DESC -> tracks.sortedByDescending { it.displayTitle.lowercase() }
            SortOption.DATE_ADDED_DESC -> tracks.sortedByDescending { it.dateAdded }
            SortOption.DATE_ADDED_ASC -> tracks.sortedBy { it.dateAdded }
            SortOption.DURATION_DESC -> tracks.sortedByDescending { it.durationMs }
            SortOption.DURATION_ASC -> tracks.sortedBy { it.durationMs }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading && tracks.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            modifier = Modifier.size(80.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                                    alpha = 0.3f
                                )
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(40.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Text(
                            text = "Scanning your music library",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Finding all your amazing tracks...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Card(
                            modifier = Modifier.size(80.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Text(
                            text = "Something went wrong",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { /* Retry scanning */ }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Try Again")
                        }
                    }
                }
            }

            tracks.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Card(
                            modifier = Modifier.size(100.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(
                                    alpha = 0.3f
                                )
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.LibraryMusic,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        Text(
                            text = "Your music library is empty",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Add some music files to your device and tap the refresh button to scan for them",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { /* Open file manager or instructions */ }
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Browse Files")
                            }

                            Button(
                                onClick = { /* Trigger scan */ }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scan Library")
                            }
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(
                        items = sortedTracks,
                        key = { track -> track.id }
                    ) { track ->
                        TrackItem(
                            track = track,
                            onClick = { onTrackClick(track) },
                            showQuality = true,
                            isPlaying = currentlyPlayingTrack?.id == track.id,
                            onMoreClick = onMoreClick,
                            modifier = Modifier.animateItem()
                        )
                    }
                    
                    // Add bottom padding for the mini player
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
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
    if (albums.isEmpty()) {
        EmptyTabState(
            icon = Icons.Default.Album,
            title = "No albums found",
            subtitle = "Albums will appear here when you add music to your library"
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
}

@Composable
private fun ArtistsTab(
    artists: List<com.octavia.player.data.model.Artist>,
    onArtistClick: (Long) -> Unit
) {
    if (artists.isEmpty()) {
        EmptyTabState(
            icon = Icons.Default.Person,
            title = "No artists found",
            subtitle = "Artists will appear here when you add music to your library"
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                items = artists,
                key = { it.id }
            ) { artist ->
                ArtistCard(
                    artist = artist,
                    onClick = { onArtistClick(artist.id) }
                )
            }
        }
    }
}

@Composable
private fun PlaylistsTab(
    playlists: List<com.octavia.player.data.model.Playlist>,
    onPlaylistClick: (Long) -> Unit
) {
    if (playlists.isEmpty()) {
        EmptyTabState(
            icon = Icons.AutoMirrored.Filled.PlaylistPlay,
            title = "No playlists yet",
            subtitle = "Create your first playlist to organize your favorite tracks"
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                items = playlists,
                key = { it.id }
            ) { playlist ->
                PlaylistCardGrid(
                    playlist = playlist,
                    onClick = { onPlaylistClick(playlist.id) }
                )
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

enum class SortOption(val displayName: String) {
    NAME_ASC("Name A-Z"),
    NAME_DESC("Name Z-A"),
    DATE_ADDED_DESC("Recently Added"),
    DATE_ADDED_ASC("Oldest First"),
    DURATION_DESC("Longest First"),
    DURATION_ASC("Shortest First")
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
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

@Composable
private fun EmptyTabState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            Card(
                modifier = Modifier.size(80.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun SortFilterBar(
    currentSort: SortOption,
    onSortChange: (SortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Sort by",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = currentSort.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Box {
                Surface(
                    onClick = { showSortMenu = true },
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Sort options",
                        modifier = Modifier.padding(8.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    SortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = option.displayName,
                                    style = MaterialTheme.typography.bodyMedium
                                ) 
                            },
                            onClick = {
                                onSortChange(option)
                                showSortMenu = false
                            },
                            leadingIcon = {
                                if (option == currentSort) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
