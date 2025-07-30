package com.octavia.player.data.scanner

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import com.octavia.player.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

/**
 * Utility class for scanning media files
 * Optimized for better performance and background artwork extraction
 */
object MediaScanner {

    suspend fun scanMusicLibrary(context: Context, extractArtworkInBackground: Boolean = true): List<Track> = withContext(Dispatchers.IO) {
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.GENRE,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA, // File path
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.BITRATE
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} = ? AND ${MediaStore.Audio.Media.DURATION} > ?"
        val selectionArgs = arrayOf("1", "10000") // Filter out very short tracks (< 10 seconds)
        val sortOrder = "${MediaStore.Audio.Media.ALBUM_ID} ASC, ${MediaStore.Audio.Media.TRACK} ASC"

        val scannedTracks = mutableListOf<Track>()

        try {
            val cursor: Cursor? = context.contentResolver.query(
                uri, projection, selection, selectionArgs, sortOrder
            )

            cursor?.use {
                while (it.moveToNext()) {
                    yield() // Allow other coroutines to run
                    val track = createTrackFromCursor(context, it, extractArtworkInBackground)
                    track?.let { validTrack ->
                        scannedTracks.add(validTrack)
                    }
                }
            }
        } catch (e: Exception) {
            // Log error but don't crash
            e.printStackTrace()
        }

        // If extracting artwork in background, preload artwork for grouped albums
        if (extractArtworkInBackground && scannedTracks.isNotEmpty()) {
            async {
                ArtworkExtractor.preloadArtwork(context, scannedTracks)
            }
        }

        scannedTracks
    }

    private fun createTrackFromCursor(context: Context, cursor: Cursor, extractArtwork: Boolean = true): Track? {
        return try {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
            val displayName =
                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME))
                    ?: ""
            val title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                ?: displayName
            val artist =
                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                    ?: "Unknown Artist"
            val album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM))
                ?: "Unknown Album"
            val albumId =
                cursor.getLongOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
            val artistId =
                cursor.getLongOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID))
            val genre = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.GENRE))
            val trackNumber =
                cursor.getIntOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK))
            val year =
                cursor.getIntOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR))
            val duration =
                cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
            val fileSize = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE))
            val filePath =
                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
                    ?: return null
            val dateAdded =
                cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)) * 1000 // Convert to milliseconds
            val lastModified =
                cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)) * 1000
            val mimeType =
                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE))
                    ?: ""
            val bitrate =
                cursor.getIntOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.BITRATE))

            // Determine if it's lossless based on mime type
            val isLossless = when {
                mimeType.contains("flac", ignoreCase = true) -> true
                mimeType.contains("alac", ignoreCase = true) -> true
                mimeType.contains("wav", ignoreCase = true) -> true
                mimeType.contains("aiff", ignoreCase = true) -> true
                else -> false
            }

            // Extract codec from mime type
            val codecName = when {
                mimeType.contains("flac", ignoreCase = true) -> "FLAC"
                mimeType.contains("mp3", ignoreCase = true) -> "MP3"
                mimeType.contains("aac", ignoreCase = true) -> "AAC"
                mimeType.contains("ogg", ignoreCase = true) -> "OGG"
                mimeType.contains("wav", ignoreCase = true) -> "WAV"
                mimeType.contains("m4a", ignoreCase = true) -> "AAC"
                else -> "Unknown"
            }

            // Extract artwork conditionally
            val artworkPath = if (extractArtwork) {
                ArtworkExtractor.extractArtwork(context, filePath, albumId)
            } else {
                null // Will be extracted later in background
            }

            Track(
                id = 0, // Will be auto-generated by Room
                title = title,
                artist = artist,
                album = album,
                albumArtist = null, // Would need additional metadata parsing
                genre = genre,
                year = year,
                trackNumber = trackNumber,
                discNumber = null,
                durationMs = duration,
                bitrateKbps = bitrate,
                sampleRateHz = null, // Would need MediaMetadataRetriever for this
                bitDepth = null,
                channelCount = null,
                codecName = codecName,
                isLossless = isLossless,
                replayGainTrack = null,
                replayGainAlbum = null,
                replayGainPeak = null,
                artworkPath = artworkPath,
                albumId = albumId,
                artistId = artistId,
                genreId = null,
                filePath = filePath,
                fileName = filePath.substringAfterLast('/'),
                fileSize = fileSize,
                lastModified = lastModified,
                playCount = 0,
                lastPlayed = null,
                isFavorite = false,
                dateAdded = dateAdded,
                dateScanned = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun Cursor.getLongOrNull(columnIndex: Int): Long? {
        return if (isNull(columnIndex)) null else getLong(columnIndex)
    }

    private fun Cursor.getIntOrNull(columnIndex: Int): Int? {
        return if (isNull(columnIndex)) null else getInt(columnIndex)
    }
}
