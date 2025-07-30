package com.octavia.player.data.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Represents an album in the database
 */
@Entity(
    tableName = "albums",
    indices = [
        Index(value = ["name", "artist"], unique = true),
        Index(value = ["artist_id"])
    ]
)
@Parcelize
@Serializable
data class Album(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val name: String,
    val artist: String? = null,
    @ColumnInfo(name = "artist_id")
    val artistId: Long? = null,

    val year: Int? = null,
    @ColumnInfo(name = "track_count")
    val trackCount: Int = 0,
    @ColumnInfo(name = "total_duration_ms")
    val totalDurationMs: Long = 0L,

    @ColumnInfo(name = "artwork_path")
    val artworkPath: String? = null,
    @ColumnInfo(name = "date_added")
    val dateAdded: Long = System.currentTimeMillis()
) : Parcelable {

    val displayName: String
        get() = name.takeIf { it.isNotBlank() } ?: "Unknown Album"

    val displayArtist: String
        get() = artist?.takeIf { it.isNotBlank() } ?: "Unknown Artist"

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