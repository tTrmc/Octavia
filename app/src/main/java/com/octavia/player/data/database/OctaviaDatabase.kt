package com.octavia.player.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.octavia.player.data.database.dao.AlbumDao
import com.octavia.player.data.database.dao.ArtistDao
import com.octavia.player.data.database.dao.GenreDao
import com.octavia.player.data.database.dao.PlaylistDao
import com.octavia.player.data.database.dao.TrackDao
import com.octavia.player.data.model.Album
import com.octavia.player.data.model.Artist
import com.octavia.player.data.model.Genre
import com.octavia.player.data.model.Playlist
import com.octavia.player.data.model.PlaylistTrack
import com.octavia.player.data.model.Track

/**
 * Room database for Octavia Hi-Fi Music Player
 * Manages all local data storage including tracks, albums, artists, genres, and playlists
 */
@Database(
    entities = [
        Track::class,
        Album::class,
        Artist::class,
        Genre::class,
        Playlist::class,
        PlaylistTrack::class
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = []
)
@TypeConverters(DatabaseConverters::class)
abstract class OctaviaDatabase : RoomDatabase() {

    abstract fun trackDao(): TrackDao
    abstract fun albumDao(): AlbumDao
    abstract fun artistDao(): ArtistDao
    abstract fun genreDao(): GenreDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        const val DATABASE_NAME = "octavia_database"

        /**
         * Migration from version 1 to 2 - Add artwork_path column to tracks table
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add the artwork_path column to the tracks table
                database.execSQL("ALTER TABLE tracks ADD COLUMN artwork_path TEXT")
            }
        }
    }
}

/**
 * Type converters for Room database
 */
class DatabaseConverters {

    @TypeConverter
    fun fromLongList(value: List<Long>): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun toLongList(value: String): List<Long> {
        return if (value.isBlank()) {
            emptyList()
        } else {
            value.split(",").mapNotNull { it.toLongOrNull() }
        }
    }

    @TypeConverter
    fun fromFloatList(value: List<Float>): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun toFloatList(value: String): List<Float> {
        return if (value.isBlank()) {
            emptyList()
        } else {
            value.split(",").mapNotNull { it.toFloatOrNull() }
        }
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString("|")
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isBlank()) {
            emptyList()
        } else {
            value.split("|")
        }
    }
}
