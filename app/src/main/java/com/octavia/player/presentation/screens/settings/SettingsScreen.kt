package com.octavia.player.presentation.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Settings screen for app configuration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                        icon = Icons.Default.VolumeUp,
                        onClick = { /* TODO: Open ReplayGain settings */ }
                    )
                    
                    SettingsItem(
                        title = "Crossfade",
                        subtitle = "Smooth transitions between tracks",
                        icon = Icons.Default.Tune,
                        onClick = { /* TODO: Open crossfade settings */ }
                    )
                }
            }
            
            item {
                SettingsSection(title = "Library") {
                    SettingsItem(
                        title = "Scan Music Library",
                        subtitle = "Refresh your music collection",
                        icon = Icons.Default.Refresh,
                        onClick = { /* TODO: Trigger library scan */ }
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
                        onClick = { /* TODO: Show supported formats */ }
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
                        onClick = { /* TODO: Open now playing settings */ }
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
                        onClick = { /* TODO: Open headphone settings */ }
                    )
                }
            }
            
            item {
                SettingsSection(title = "About") {
                    SettingsItem(
                        title = "Version",
                        subtitle = "Octavia v1.0.0",
                        icon = Icons.Default.Info,
                        onClick = { /* TODO: Show version info */ }
                    )
                    
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
                        onClick = { /* TODO: Open support */ }
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
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
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
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
