package com.octavia.player.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.octavia.player.presentation.components.PermissionRequestScreen
import com.octavia.player.presentation.navigation.OctaviaNavHost
import com.octavia.player.presentation.theme.OctaviaTheme
import com.octavia.player.utils.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for Octavia Hi-Fi Music Player
 * Sets up the Compose UI and navigation
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var permissionsGranted by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Configure window for immersive audio experience
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Set up permission launcher
        permissionLauncher = PermissionUtils.createPermissionLauncher(this) { permissions ->
            val mandatoryPermissions = PermissionUtils.getMandatoryPermissions()
            val mandatoryGranted = mandatoryPermissions.all { permission ->
                permissions[permission] ?: PermissionUtils.hasPermission(this, permission)
            }

            permissionsGranted = mandatoryGranted

            if (!mandatoryGranted) {
                // Handle case where some mandatory permissions were denied
                // For now, we'll keep showing the permission screen until essentials are granted
            }
        }

        // Check initial permission state
        permissionsGranted = PermissionUtils.hasAllPermissions(this)

        setContent {
            OctaviaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (permissionsGranted) {
                        OctaviaNavHost()
                    } else {
                        PermissionRequestScreen(
                            onRequestPermissions = {
                                requestPermissions()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun requestPermissions() {
        val mandatory = PermissionUtils.getMandatoryPermissions().toSet()
        val optional = PermissionUtils.getOptionalPermissions().filter { permission ->
            !PermissionUtils.hasPermission(this, permission)
        }

        val toRequest = (mandatory + optional).filter { permission ->
            !PermissionUtils.hasPermission(this, permission)
        }

        if (toRequest.isNotEmpty()) {
            permissionLauncher.launch(toRequest.toTypedArray())
        } else {
            // No additional permissions needed for this Android version
            permissionsGranted = true
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check permissions when returning to the app
        // This handles the case where user grants permissions in settings
        if (!permissionsGranted) {
            permissionsGranted = PermissionUtils.hasAllPermissions(this)
        }
    }
}
