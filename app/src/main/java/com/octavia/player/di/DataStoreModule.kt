package com.octavia.player.di

import android.content.Context
import com.octavia.player.data.datastore.PlaybackStateDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for DataStore dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun providePlaybackStateDataStore(
        @ApplicationContext context: Context
    ): PlaybackStateDataStore {
        return PlaybackStateDataStore(context)
    }
}
