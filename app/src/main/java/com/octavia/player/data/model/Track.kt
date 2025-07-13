package com.octavia.player.data.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Represents a music track in the database
 */
@Entity(
    tableName = "tracks",
    indices = [
        Index(value = ["file_path"], unique = true),
        Index(value = ["album_id"]),
        Index(value = ["artist_id"]),
        Index(value = ["genre_id"])
    ]
)
@Parcelize
@Serializable
data class Track(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    
    @ColumnInfo(name = "file_path")
    val filePath: String,
    
    @ColumnInfo(name = "file_name")
    val fileName: String,
    
    @ColumnInfo(name = "file_size")
    val fileSize: Long,
    
    @ColumnInfo(name = "last_modified")
    val lastModified: Long,
    
    @ColumnInfo(name = "file_hash")
    val fileHash: String? = null,
    
    // Metadata
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    @ColumnInfo(name = "album_artist")
    val albumArtist: String? = null,
    val genre: String? = null,
    val year: Int? = null,
    @ColumnInfo(name = "track_number")
    val trackNumber: Int? = null,
    @ColumnInfo(name = "disc_number")
    val discNumber: Int? = null,
    
    // Audio properties
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long,
    @ColumnInfo(name = "bitrate_kbps")
    val bitrateKbps: Int? = null,
    @ColumnInfo(name = "sample_rate_hz")
    val sampleRateHz: Int? = null,
    @ColumnInfo(name = "bit_depth")
    val bitDepth: Int? = null,
    @ColumnInfo(name = "channel_count")
    val channelCount: Int? = null,
    @ColumnInfo(name = "codec_name")
    val codecName: String? = null,
    @ColumnInfo(name = "is_lossless")
    val isLossless: Boolean = false,
    
    // ReplayGain
    @ColumnInfo(name = "replay_gain_track")
    val replayGainTrack: Float? = null,
    @ColumnInfo(name = "replay_gain_album")
    val replayGainAlbum: Float? = null,
    @ColumnInfo(name = "replay_gain_peak")
    val replayGainPeak: Float? = null,
    
    // Artwork
    @ColumnInfo(name = "artwork_path")
    val artworkPath: String? = null,
    
    // Relationships
    @ColumnInfo(name = "album_id")
    val albumId: Long? = null,
    @ColumnInfo(name = "artist_id")
    val artistId: Long? = null,
    @ColumnInfo(name = "genre_id")
    val genreId: Long? = null,
    
    // Playback statistics
    @ColumnInfo(name = "play_count")
    val playCount: Int = 0,
    @ColumnInfo(name = "last_played")
    val lastPlayed: Long? = null,
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,
    
    // Scanner metadata
    @ColumnInfo(name = "date_added")
    val dateAdded: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "date_scanned")
    val dateScanned: Long = System.currentTimeMillis()
) : Parcelable {
    
    /**
     * Gets the display title, falling back to filename if title is empty
     */
    val displayTitle: String
        get() = title.takeIf { it.isNotBlank() } ?: fileName.substringBeforeLast('.')
    
    /**
     * Gets the display artist, falling back to "Unknown Artist" if empty
     */
    val displayArtist: String
        get() = artist?.takeIf { it.isNotBlank() } ?: "Unknown Artist"
    
    /**
     * Gets the display album, falling back to "Unknown Album" if empty
     */
    val displayAlbum: String
        get() = album?.takeIf { it.isNotBlank() } ?: "Unknown Album"
    
    /**
     * Formats duration in mm:ss or h:mm:ss format
     */
    val formattedDuration: String
        get() {
            val totalSeconds = durationMs / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%d:%02d", minutes, seconds)
            }
        }
    
    /**
     * Gets a quality description based on audio properties
     */
    val qualityDescription: String
        get() = buildString {
            codecName?.let { append(it.uppercase()) }
            
            if (isLossless) {
                if (isNotEmpty()) append(" • ")
                append("Lossless")
            }
            
            sampleRateHz?.let { rate ->
                if (isNotEmpty()) append(" • ")
                when {
                    rate >= 192000 -> append("192 kHz")
                    rate >= 96000 -> append("96 kHz")
                    rate >= 48000 -> append("48 kHz")
                    rate >= 44100 -> append("44.1 kHz")
                    else -> append("${rate / 1000} kHz")
                }
            }
            
            bitDepth?.let { depth ->
                if (isNotEmpty()) append(" • ")
                append("${depth}-bit")
            }
        }
}
