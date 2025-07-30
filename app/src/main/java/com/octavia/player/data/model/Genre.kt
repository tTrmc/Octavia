package com.octavia.player.data.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Represents a genre in the database
 */
@Entity(
    tableName = "genres",
    indices = [Index(value = ["name"], unique = true)]
)
@Parcelize
@Serializable
data class Genre(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val name: String,
    @ColumnInfo(name = "track_count")
    val trackCount: Int = 0,

    @ColumnInfo(name = "date_added")
    val dateAdded: Long = System.currentTimeMillis()
) : Parcelable {

    val displayName: String
        get() = name.takeIf { it.isNotBlank() } ?: "Unknown Genre"
}