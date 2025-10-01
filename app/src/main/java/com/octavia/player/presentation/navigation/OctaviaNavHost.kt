package com.octavia.player.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.octavia.player.presentation.screens.album.AlbumDetailScreen
import com.octavia.player.presentation.screens.artist.ArtistDetailScreen
import com.octavia.player.presentation.screens.home.HomeScreen
import com.octavia.player.presentation.screens.library.LibraryScreen
import com.octavia.player.presentation.screens.player.PlayerScreen
import com.octavia.player.presentation.screens.playlist.PlaylistDetailScreen
import com.octavia.player.presentation.screens.search.SearchScreen
import com.octavia.player.presentation.screens.settings.SettingsScreen

/**
 * Navigation routes for the app
 */
object Routes {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val SEARCH = "search"
    const val PLAYER = "player"
    const val QUEUE = "queue"
    const val SETTINGS = "settings"
    const val ALBUM_DETAIL = "album_detail/{albumId}"
    const val ARTIST_DETAIL = "artist_detail/{artistId}"
    const val PLAYLIST_DETAIL = "playlist_detail/{playlistId}"
}

/**
 * Main navigation host for Octavia
 */
@Composable
fun OctaviaNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToLibrary = { navController.navigate(Routes.LIBRARY) },
                onNavigateToSearch = { navController.navigate(Routes.SEARCH) },
                onNavigateToPlayer = { navController.navigate(Routes.PLAYER) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.LIBRARY) {
            LibraryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAlbum = { albumId ->
                    navController.navigate("album_detail/$albumId")
                },
                onNavigateToArtist = { artistId ->
                    navController.navigate("artist_detail/$artistId")
                },
                onNavigateToPlaylist = { playlistId ->
                    navController.navigate("playlist_detail/$playlistId")
                },
                onNavigateToPlayer = { navController.navigate(Routes.PLAYER) }
            )
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PLAYER) {
            PlayerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Detail screens
        composable("album_detail/{albumId}") { backStackEntry ->
            AlbumDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("artist_detail/{artistId}") { backStackEntry ->
            ArtistDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("playlist_detail/{playlistId}") { backStackEntry ->
            PlaylistDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
