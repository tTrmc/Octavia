package com.octavia.player.data.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.octavia.player.data.model.Artist
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Artist operations
 */
@Dao
interface ArtistDao {

    @Query("SELECT * FROM artists ORDER BY name ASC")
    fun getAllArtistsPaged(): PagingSource<Int, Artist>

    @Query("SELECT * FROM artists ORDER BY name ASC")
    fun getAllArtistsFlow(): Flow<List<Artist>>

    @Query("SELECT * FROM artists WHERE id = :id")
    suspend fun getArtistById(id: Long): Artist?

    @Query("SELECT * FROM artists WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchArtists(query: String): Flow<List<Artist>>

    @Query("SELECT * FROM artists ORDER BY date_added DESC LIMIT :limit")
    fun getRecentlyAddedArtists(limit: Int = 50): Flow<List<Artist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtist(artist: Artist): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtists(artists: List<Artist>): List<Long>

    @Update
    suspend fun updateArtist(artist: Artist)

    @Delete
    suspend fun deleteArtist(artist: Artist)

    @Query("DELETE FROM artists WHERE id = :artistId")
    suspend fun deleteArtistById(artistId: Long)

    @Query("DELETE FROM artists")
    suspend fun deleteAllArtists()

    @Query("SELECT id FROM artists")
    suspend fun getAllArtistIds(): List<Long>

    @Query("DELETE FROM artists WHERE id IN (:artistIds)")
    suspend fun deleteArtistsByIds(artistIds: List<Long>)

    @Query("SELECT COUNT(*) FROM artists")
    suspend fun getArtistCount(): Int
}
