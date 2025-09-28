package com.octavia.player.presentation.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.octavia.player.domain.usecase.ExtractArtworkUseCase
import com.octavia.player.domain.usecase.MediaLibraryScanUseCase
import com.octavia.player.domain.repository.ArtworkExtractionProgress
import com.octavia.player.domain.repository.ArtworkCacheStats
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val extractArtworkUseCase: ExtractArtworkUseCase,
    private val mediaLibraryScanUseCase: MediaLibraryScanUseCase
) : ViewModel() {

    private val _artworkExtractionProgress = MutableStateFlow<ArtworkExtractionProgress?>(null)
    val artworkExtractionProgress: StateFlow<ArtworkExtractionProgress?> = _artworkExtractionProgress.asStateFlow()

    private val _cacheStats = MutableStateFlow<ArtworkCacheStats?>(null)
    val cacheStats: StateFlow<ArtworkCacheStats?> = _cacheStats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        loadCacheStats()
    }

    fun triggerLibraryScan() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _message.value = "Scanning music library..."

                mediaLibraryScanUseCase.scanLibraryWithArtwork(context).fold(
                    onSuccess = { trackCount ->
                        _message.value = "Library scan completed. Found $trackCount tracks."
                        loadCacheStats()
                    },
                    onFailure = { error ->
                        _message.value = "Library scan failed: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _message.value = "Unexpected error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun extractArtworkForMissingTracks() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _message.value = "Starting artwork extraction..."

                extractArtworkUseCase.extractForTracksWithoutArtwork(limit = 200).collect { progress ->
                    _artworkExtractionProgress.value = progress

                    if (progress.isCompleted) {
                        _message.value = "Artwork extraction completed! Processed ${progress.completed} tracks."
                        loadCacheStats()
                    } else if (progress.error != null) {
                        _message.value = progress.error
                    }
                }
            } catch (e: Exception) {
                _message.value = "Artwork extraction failed: ${e.message}"
            } finally {
                _isLoading.value = false
                _artworkExtractionProgress.value = null
            }
        }
    }

    fun clearArtworkCache() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _message.value = "Clearing artwork cache..."

                extractArtworkUseCase.clearCache()
                _message.value = "Artwork cache cleared successfully."
                loadCacheStats()
            } catch (e: Exception) {
                _message.value = "Failed to clear cache: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun validateAndCleanupArtwork() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _message.value = "Validating artwork..."

                val cleanedCount = extractArtworkUseCase.validateAndCleanup()
                _message.value = "Artwork validation completed. Cleaned up $cleanedCount invalid entries."
                loadCacheStats()
            } catch (e: Exception) {
                _message.value = "Validation failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadCacheStats() {
        viewModelScope.launch {
            try {
                val stats = extractArtworkUseCase.getCacheStats()
                _cacheStats.value = stats
            } catch (e: Exception) {
                // Silently fail for cache stats
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}