package com.octavia.player.presentation.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Support
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Settings screen for app configuration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val artworkProgress by viewModel.artworkExtractionProgress.collectAsState()
    val cacheStats by viewModel.cacheStats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show messages in snackbar
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.headlineSmall
                    ) 
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Progress indicator for ongoing operations
            if (isLoading || artworkProgress != null) {
                item {
                    ProgressSection(
                        isLoading = isLoading,
                        artworkProgress = artworkProgress
                    )
                }
            }
            item {
                // Header with app info
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AudioFile,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Octavia",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Version 1.0.0",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            item {
                SettingsSection(title = "Audio") {
                    SettingsItem(
                        title = "Audio Quality",
                        subtitle = "High-resolution audio settings",
                        icon = Icons.Default.HighQuality,
                        onClick = { /* TODO: Open audio quality settings */ }
                    )

                    SettingsItem(
                        title = "Equalizer",
                        subtitle = "Customize audio frequencies",
                        icon = Icons.Default.Equalizer,
                        onClick = { /* TODO: Open equalizer */ }
                    )

                    SettingsItem(
                        title = "ReplayGain",
                        subtitle = "Normalize volume levels",
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        onClick = { /* TODO: Open ReplayGain settings */ }
                    )

                    SettingsItem(
                        title = "Crossfade",
                        subtitle = "Smooth transitions between tracks",
                        icon = Icons.Default.Tune,
                        onClick = { /* TODO: Open crossfade settings */ },
                        showDivider = false
                    )
                }
            }

            item {
                SettingsSection(title = "Library") {
                    SettingsItem(
                        title = "Scan Music Library",
                        subtitle = "Refresh your music collection",
                        icon = Icons.Default.Refresh,
                        onClick = { viewModel.triggerLibraryScan() }
                    )

                    SettingsItem(
                        title = "Extract Album Artwork",
                        subtitle = buildString {
                            append("Extract artwork for tracks without it")
                            cacheStats?.let { stats ->
                                append(" (${stats.tracksWithoutArtwork} missing)")
                            }
                        },
                        icon = Icons.Default.Image,
                        onClick = { viewModel.extractArtworkForMissingTracks() }
                    )

                    SettingsItem(
                        title = "Clear Artwork Cache",
                        subtitle = buildString {
                            append("Remove cached album artwork")
                            cacheStats?.let { stats ->
                                val sizeMB = stats.cacheSize / (1024 * 1024)
                                append(" (${stats.totalCachedFiles} files, ${sizeMB}MB)")
                            }
                        },
                        icon = Icons.Default.CleaningServices,
                        onClick = { viewModel.clearArtworkCache() }
                    )

                    SettingsItem(
                        title = "Music Folders",
                        subtitle = "Choose where to find music",
                        icon = Icons.Default.Folder,
                        onClick = { /* TODO: Open folder selection */ }
                    )

                    SettingsItem(
                        title = "File Formats",
                        subtitle = "Supported audio formats",
                        icon = Icons.Default.AudioFile,
                        onClick = { /* TODO: Show supported formats */ },
                        showDivider = false
                    )
                }
            }

            item {
                SettingsSection(title = "Appearance") {
                    SettingsItem(
                        title = "Theme",
                        subtitle = "Light, dark, or system default",
                        icon = Icons.Default.Palette,
                        onClick = { /* TODO: Open theme selection */ }
                    )

                    SettingsItem(
                        title = "Now Playing",
                        subtitle = "Customize player interface",
                        icon = Icons.Default.Tune,
                        onClick = { /* TODO: Open now playing settings */ },
                        showDivider = false
                    )
                }
            }

            item {
                SettingsSection(title = "Playback") {
                    SettingsItem(
                        title = "Gapless Playback",
                        subtitle = "Seamless track transitions",
                        icon = Icons.Default.SkipNext,
                        onClick = { /* TODO: Toggle gapless playback */ }
                    )

                    SettingsItem(
                        title = "Resume Playback",
                        subtitle = "Continue from where you left off",
                        icon = Icons.Default.PlayArrow,
                        onClick = { /* TODO: Toggle resume playback */ }
                    )

                    SettingsItem(
                        title = "Headphone Controls",
                        subtitle = "Configure media button behavior",
                        icon = Icons.Default.Headphones,
                        onClick = { /* TODO: Open headphone settings */ },
                        showDivider = false
                    )
                }
            }

            item {
                SettingsSection(title = "About") {
                    SettingsItem(
                        title = "Open Source Licenses",
                        subtitle = "View third-party software licenses",
                        icon = Icons.Default.Code,
                        onClick = { /* TODO: Show licenses */ }
                    )

                    SettingsItem(
                        title = "Support",
                        subtitle = "Get help and report issues",
                        icon = Icons.Default.Support,
                        onClick = { /* TODO: Open support */ },
                        showDivider = false
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            content()
        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    showDivider: Boolean = true
) {
    var isPressed by remember { mutableStateOf(false) }
    
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        label = "scale"
    )
    
    val animatedBackgroundColor by animateColorAsState(
        targetValue = if (isPressed) 
            MaterialTheme.colorScheme.surfaceContainer 
        else 
            Color.Transparent,
        label = "backgroundColor"
    )

    Column {
        Surface(
            onClick = {
                isPressed = true
                onClick()
                // Reset press state after a short delay
            },
            modifier = Modifier
                .fillMaxWidth()
                .scale(animatedScale),
            color = animatedBackgroundColor,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainer,
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        if (showDivider) {
            Spacer(modifier = Modifier.height(8.dp))
            Divider(
                color = MaterialTheme.colorScheme.surfaceVariant,
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 64.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Progress section for ongoing operations
 */
@Composable
private fun ProgressSection(
    isLoading: Boolean,
    artworkProgress: com.octavia.player.data.scanner.ArtworkExtractionProgress?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            if (artworkProgress != null) {
                Text(
                    text = "Extracting Artwork",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = artworkProgress.progressPercentage / 100f,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${artworkProgress.completed} / ${artworkProgress.total} tracks",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )

                artworkProgress.currentTrack?.let { track ->
                    Text(
                        text = "Processing: ${track.displayTitle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else if (isLoading) {
                Text(
                    text = "Processing...",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
