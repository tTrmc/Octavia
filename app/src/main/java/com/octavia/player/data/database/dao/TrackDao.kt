package com.octavia.player.data.database.dao

import androidx.paging.PagingSource
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.octavia.player.data.model.Track
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Track operations
 */
@Dao
interface TrackDao {

    @Query("SELECT * FROM tracks ORDER BY title ASC")
    fun getAllTracksPaged(): PagingSource<Int, Track>

    @Query("SELECT * FROM tracks ORDER BY title ASC")
    fun getAllTracksFlow(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getTrackById(id: Long): Track?

    @Query("SELECT * FROM tracks WHERE file_path = :filePath")
    suspend fun getTrackByPath(filePath: String): Track?

    @Query("SELECT * FROM tracks WHERE album_id = :albumId ORDER BY disc_number ASC, track_number ASC, title ASC")
    fun getTracksByAlbum(albumId: Long): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE artist_id = :artistId ORDER BY year DESC, album ASC, disc_number ASC, track_number ASC")
    fun getTracksByArtist(artistId: Long): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE genre_id = :genreId ORDER BY artist ASC, album ASC, disc_number ASC, track_number ASC")
    fun getTracksByGenre(genreId: Long): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE is_favorite = 1 ORDER BY last_played DESC, title ASC")
    fun getFavoriteTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE play_count > 0 ORDER BY play_count DESC, last_played DESC LIMIT :limit")
    fun getMostPlayedTracks(limit: Int = 50): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE last_played IS NOT NULL ORDER BY last_played DESC LIMIT :limit")
    fun getRecentlyPlayedTracks(limit: Int = 50): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE date_added > :since ORDER BY date_added DESC")
    fun getRecentlyAddedTracks(since: Long): Flow<List<Track>>

    @Query(
        """
        SELECT * FROM tracks 
        WHERE title LIKE '%' || :query || '%' 
        OR artist LIKE '%' || :query || '%' 
        OR album LIKE '%' || :query || '%'
        OR album_artist LIKE '%' || :query || '%'
        ORDER BY 
            CASE 
                WHEN title LIKE :query || '%' THEN 1
                WHEN artist LIKE :query || '%' THEN 2
                WHEN album LIKE :query || '%' THEN 3
                ELSE 4
            END,
            title ASC
    """
    )
    fun searchTracks(query: String): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE codec_name IN (:codecs) ORDER BY title ASC")
    fun getTracksByCodec(codecs: List<String>): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE is_lossless = 1 ORDER BY bit_depth DESC, sample_rate_hz DESC, title ASC")
    fun getLosslessTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE sample_rate_hz >= :minSampleRate ORDER BY sample_rate_hz DESC, bit_depth DESC, title ASC")
    fun getHiResTracks(minSampleRate: Int = 48000): Flow<List<Track>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: Track): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<Track>): List<Long>

    @Update
    suspend fun updateTrack(track: Track)

    @Query("UPDATE tracks SET play_count = play_count + 1, last_played = :timestamp WHERE id = :trackId")
    suspend fun incrementPlayCount(trackId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE tracks SET is_favorite = :isFavorite WHERE id = :trackId")
    suspend fun updateFavoriteStatus(trackId: Long, isFavorite: Boolean)

    @Delete
    suspend fun deleteTrack(track: Track)

    @Query("DELETE FROM tracks WHERE id = :trackId")
    suspend fun deleteTrackById(trackId: Long)

    @Query("DELETE FROM tracks WHERE file_path NOT IN (SELECT file_path FROM tracks WHERE file_path IN (:existingPaths))")
    suspend fun deleteTracksNotInPaths(existingPaths: List<String>)

    @Query("DELETE FROM tracks")
    suspend fun deleteAllTracks()

    @Query("SELECT COUNT(*) FROM tracks")
    suspend fun getTrackCount(): Int

    @Query("SELECT SUM(duration_ms) FROM tracks")
    suspend fun getTotalDuration(): Long?

    @Query("SELECT COUNT(*) FROM tracks WHERE is_lossless = 1")
    suspend fun getLosslessTrackCount(): Int

    @Query("SELECT DISTINCT codec_name FROM tracks WHERE codec_name IS NOT NULL ORDER BY codec_name ASC")
    suspend fun getAllCodecs(): List<String>

    @Query("SELECT file_path, last_modified FROM tracks")
    suspend fun getAllFileInfo(): List<TrackFileInfo>

    @Query("SELECT EXISTS(SELECT 1 FROM tracks WHERE file_path = :filePath AND last_modified = :lastModified)")
    suspend fun isTrackUpToDate(filePath: String, lastModified: Long): Boolean
}

/**
 * Simple data class for file information queries
 */
data class TrackFileInfo(
    @ColumnInfo(name = "file_path")
    val filePath: String,
    @ColumnInfo(name = "last_modified")
    val lastModified: Long
)
