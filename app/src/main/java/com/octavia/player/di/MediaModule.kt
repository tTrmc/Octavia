package com.octavia.player.di

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.trackselection.TrackSelector
import com.octavia.player.data.repository.MediaPlaybackRepositoryImpl
import com.octavia.player.domain.repository.MediaPlaybackRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt module for Media3/ExoPlayer dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object MediaModule {

    @Provides
    @Singleton
    @Named("ApplicationScope")
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }

    @Provides
    @Singleton
    fun provideAudioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
    }

    @Provides
    @Singleton
    fun provideAudioSink(@ApplicationContext context: Context): AudioSink {
        return DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(true) // Enable high-resolution float output
            .setEnableAudioTrackPlaybackParams(true)
            // Note: setOffloadMode may not be available in all Media3 versions
            .setAudioProcessorChain(
                // Custom audio processor chain for ReplayGain, EQ, etc.
                DefaultAudioSink.DefaultAudioProcessorChain()
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideTrackSelector(@ApplicationContext context: Context): TrackSelector {
        return DefaultTrackSelector(context, AdaptiveTrackSelection.Factory())
    }

    @Provides
    @Singleton
    fun provideLoadControl(): LoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .setTargetBufferBytes(DefaultLoadControl.DEFAULT_TARGET_BUFFER_BYTES)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        audioAttributes: AudioAttributes,
        trackSelector: TrackSelector,
        loadControl: LoadControl
    ): ExoPlayer {
        return ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setUseLazyPreparation(true)
            .setPauseAtEndOfMediaItems(false) // Enable gapless playback
            .setSeekBackIncrementMs(10_000) // 10 second seek back
            .setSeekForwardIncrementMs(30_000) // 30 second seek forward
            .build()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaRepositoryModule {
    
    @Binds
    abstract fun bindMediaPlaybackRepository(
        mediaPlaybackRepositoryImpl: MediaPlaybackRepositoryImpl
    ): MediaPlaybackRepository
}
