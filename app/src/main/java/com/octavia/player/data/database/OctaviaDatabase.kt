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
                try {
                    // Add the artwork_path column to the tracks table
                    database.execSQL("ALTER TABLE tracks ADD COLUMN artwork_path TEXT")

                    // Add performance indices for common queries
                    database.execSQL("CREATE INDEX IF NOT EXISTS idx_tracks_search ON tracks(title, artist, album)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS idx_tracks_album_id ON tracks(album_id)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS idx_tracks_artist_id ON tracks(artist_id)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS idx_tracks_playback ON tracks(play_count DESC, last_played DESC)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS idx_tracks_metadata ON tracks(sample_rate_hz, bit_depth, is_lossless)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS idx_tracks_date_added ON tracks(date_added DESC)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS idx_tracks_codec ON tracks(codec_name)")

                    // Additional critical indices for performance optimization
                    database.execSQL("CREATE INDEX IF NOT EXISTS idx_tracks_favorites ON tracks(is_favorite, last_played DESC)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS idx_tracks_file_path ON tracks(file_path)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS idx_tracks_duration ON tracks(duration_ms)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS idx_tracks_sorting ON tracks(album, disc_number, track_number)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS idx_tracks_hi_res ON tracks(is_lossless, sample_rate_hz DESC, bit_depth DESC)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS idx_tracks_artwork ON tracks(artwork_path)")

                    // Full-text search optimization (if supported)
                    database.execSQL("CREATE INDEX IF NOT EXISTS idx_tracks_search_optimized ON tracks(title COLLATE NOCASE, artist COLLATE NOCASE, album COLLATE NOCASE)")

                } catch (e: Exception) {
                    // Log the error but don't crash - fallback migration will handle it
                    android.util.Log.e("OctaviaDatabase", "Migration 1->2 failed", e)
                    throw e
                }
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
