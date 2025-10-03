package com.octavia.player.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * Utility class for managing runtime permissions in Octavia Music Player
 */
object PermissionUtils {

    /**
     * Required permissions for the music player based on Android version
     */
    fun getRequiredPermissions(): Array<String> = getMandatoryPermissions()

    /**
     * Mandatory permissions needed for core playback features
     */
    fun getMandatoryPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO
            )

            else -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }

    /**
     * Optional permissions that enhance – but do not gate – the experience
     */
    fun getOptionalPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyArray()
        }
    }

    /**
     * All permissions we may request in a single pass
     */
    fun getAllPermissions(): Array<String> {
        val mandatory = getMandatoryPermissions()
        val optional = getOptionalPermissions()
        return (mandatory + optional).distinct().toTypedArray()
    }

    /**
     * Check if all required permissions are granted
     */
    fun hasAllPermissions(context: Context): Boolean {
        val requiredPermissions = getMandatoryPermissions()
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check if a specific permission is granted
     */
    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get the list of permissions that are not yet granted
     */
    fun getMissingPermissions(context: Context): List<String> {
        return getMandatoryPermissions().filter { permission ->
            !hasPermission(context, permission)
        }
    }

    /**
     * Get optional permissions that are still missing
     */
    fun getMissingOptionalPermissions(context: Context): List<String> {
        return getOptionalPermissions().filter { permission ->
            !hasPermission(context, permission)
        }
    }

    /**
     * Check if we should show rationale for any permission
     */
    fun shouldShowRationale(activity: Activity): Boolean {
        return getMandatoryPermissions().any { permission ->
            activity.shouldShowRequestPermissionRationale(permission)
        }
    }

    /**
     * Get user-friendly permission descriptions
     */
    fun getPermissionDescription(permission: String): String {
        return when (permission) {
            Manifest.permission.READ_EXTERNAL_STORAGE ->
                "Access to storage is needed to scan and play your music files."

            Manifest.permission.READ_MEDIA_AUDIO ->
                "Access to audio files is needed to scan and play your music library."

            Manifest.permission.POST_NOTIFICATIONS ->
                "Notification permission is needed to show playback controls and song information."

            else -> "This permission is required for the app to function properly."
        }
    }

    /**
     * Create permission request launcher
     */
    fun createPermissionLauncher(
        activity: androidx.activity.ComponentActivity,
        onPermissionsResult: (Map<String, Boolean>) -> Unit
    ): ActivityResultLauncher<Array<String>> {
        return activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            onPermissionsResult(permissions)
        }
    }
}
