package com.octavia.player.di

import android.content.Context
import androidx.room.Room
import com.octavia.player.data.database.OctaviaDatabase
import com.octavia.player.data.database.dao.AlbumDao
import com.octavia.player.data.database.dao.ArtistDao
import com.octavia.player.data.database.dao.GenreDao
import com.octavia.player.data.database.dao.PlaylistDao
import com.octavia.player.data.database.dao.TrackDao
import com.octavia.player.data.repository.TrackRepositoryImpl
import com.octavia.player.domain.repository.TrackRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for database dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideOctaviaDatabase(@ApplicationContext context: Context): OctaviaDatabase {
        return Room.databaseBuilder(
            context,
            OctaviaDatabase::class.java,
            OctaviaDatabase.DATABASE_NAME
        )
            .addMigrations(
                // Add migrations here as database evolves
                OctaviaDatabase.MIGRATION_1_2
            )
            .fallbackToDestructiveMigration(false) // Only for development - remove for production
            .build()
    }

    @Provides
    fun provideTrackDao(database: OctaviaDatabase): TrackDao = database.trackDao()

    @Provides
    fun provideAlbumDao(database: OctaviaDatabase): AlbumDao = database.albumDao()

    @Provides
    fun provideArtistDao(database: OctaviaDatabase): ArtistDao = database.artistDao()

    @Provides
    fun provideGenreDao(database: OctaviaDatabase): GenreDao = database.genreDao()

    @Provides
    fun providePlaylistDao(database: OctaviaDatabase): PlaylistDao = database.playlistDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    abstract fun bindTrackRepository(
        trackRepositoryImpl: TrackRepositoryImpl
    ): TrackRepository
}
