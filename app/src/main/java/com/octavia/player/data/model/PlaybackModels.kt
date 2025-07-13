package com.octavia.player.data.model

import kotlinx.serialization.Serializable

/**
 * Playback state for the media player
 */
@Serializable
enum class PlaybackState {
    IDLE,
    BUFFERING,
    READY,
    ENDED,
    ERROR
}

/**
 * Repeat mode for playback
 */
@Serializable
enum class RepeatMode {
    OFF,
    ONE,
    ALL
}

/**
 * Shuffle mode for playback
 */
@Serializable
enum class ShuffleMode {
    OFF,
    ON
}

/**
 * Current player state
 */
@Serializable
data class PlayerState(
    val isPlaying: Boolean = false,
    val playbackState: PlaybackState = PlaybackState.IDLE,
    val currentTrack: Track? = null,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleMode: ShuffleMode = ShuffleMode.OFF,
    val playbackSpeed: Float = 1.0f,
    val volume: Float = 1.0f
) {
    val progress: Float
        get() = if (duration > 0) currentPosition.toFloat() / duration else 0f
}

/**
 * Queue information
 */
@Serializable
data class PlaybackQueue(
    val tracks: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val originalOrder: List<Track> = emptyList(),
    val shuffledOrder: List<Track> = emptyList(),
    val isShuffled: Boolean = false
) {
    val currentTrack: Track?
        get() = tracks.getOrNull(currentIndex)
    
    val hasNext: Boolean
        get() = currentIndex < tracks.size - 1
    
    val hasPrevious: Boolean
        get() = currentIndex > 0
    
    val isEmpty: Boolean
        get() = tracks.isEmpty()
    
    val size: Int
        get() = tracks.size
}

/**
 * Audio effects configuration
 */
@Serializable
data class AudioEffects(
    val isEqualizerEnabled: Boolean = false,
    val equalizerPreset: String? = null,
    val equalizerBands: List<Float> = emptyList(),
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val replayGainMode: ReplayGainMode = ReplayGainMode.OFF,
    val replayGainPreamp: Float = 0f
)

/**
 * ReplayGain processing mode
 */
@Serializable
enum class ReplayGainMode {
    OFF,
    TRACK,
    ALBUM
}

/**
 * Crossfade configuration
 */
@Serializable
data class CrossfadeSettings(
    val isEnabled: Boolean = false,
    val durationMs: Int = 3000,
    val onlyOnTrackChange: Boolean = true
)

/**
 * Gapless playback configuration
 */
@Serializable
data class GaplessSettings(
    val isEnabled: Boolean = true,
    val bufferSizeMs: Int = 500
)
