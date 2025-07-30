package com.octavia.player.data.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Represents a playlist in the database
 */
@Entity(
    tableName = "playlists",
    indices = [Index(value = ["name"], unique = true)]
)
@Parcelize
@Serializable
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val name: String,
    val description: String? = null,
    @ColumnInfo(name = "track_count")
    val trackCount: Int = 0,
    @ColumnInfo(name = "total_duration_ms")
    val totalDurationMs: Long = 0L,

    @ColumnInfo(name = "artwork_path")
    val artworkPath: String? = null,
    @ColumnInfo(name = "date_created")
    val dateCreated: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "date_modified")
    val dateModified: Long = System.currentTimeMillis()
) : Parcelable {

    val displayName: String
        get() = name.takeIf { it.isNotBlank() } ?: "Unnamed Playlist"

    val formattedDuration: String
        get() {
            val totalSeconds = totalDurationMs / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60

            return if (hours > 0) {
                String.format("%d:%02d hours", hours, minutes)
            } else {
                String.format("%d minutes", minutes)
            }
        }
}

/**
 * Junction table for playlist tracks
 */
@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlist_id", "track_id", "position"],
    indices = [
        Index(value = ["playlist_id"]),
        Index(value = ["track_id"]),
        Index(value = ["position"])
    ]
)
@Parcelize
@Serializable
data class PlaylistTrack(
    @ColumnInfo(name = "playlist_id")
    val playlistId: Long,
    @ColumnInfo(name = "track_id")
    val trackId: Long,
    val position: Int,
    @ColumnInfo(name = "date_added")
    val dateAdded: Long = System.currentTimeMillis()
) : Parcelable