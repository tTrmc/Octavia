package com.octavia.player.data.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Represents an artist in the database
 */
@Entity(
    tableName = "artists",
    indices = [Index(value = ["name"], unique = true)]
)
@Parcelize
@Serializable
data class Artist(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val name: String,
    @ColumnInfo(name = "album_count")
    val albumCount: Int = 0,
    @ColumnInfo(name = "track_count")
    val trackCount: Int = 0,

    @ColumnInfo(name = "artwork_path")
    val artworkPath: String? = null,
    @ColumnInfo(name = "date_added")
    val dateAdded: Long = System.currentTimeMillis()
) : Parcelable {

    val displayName: String
        get() = name.takeIf { it.isNotBlank() } ?: "Unknown Artist"
}