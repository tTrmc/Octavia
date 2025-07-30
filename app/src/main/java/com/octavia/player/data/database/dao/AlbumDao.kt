package com.octavia.player.data.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.octavia.player.data.model.Album
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Album operations
 */
@Dao
interface AlbumDao {

    @Query("SELECT * FROM albums ORDER BY name ASC")
    fun getAllAlbumsPaged(): PagingSource<Int, Album>

    @Query("SELECT * FROM albums ORDER BY name ASC")
    fun getAllAlbumsFlow(): Flow<List<Album>>

    @Query("SELECT * FROM albums WHERE id = :id")
    suspend fun getAlbumById(id: Long): Album?

    @Query("SELECT * FROM albums WHERE artist_id = :artistId ORDER BY year DESC, name ASC")
    fun getAlbumsByArtist(artistId: Long): Flow<List<Album>>

    @Query("SELECT * FROM albums WHERE year = :year ORDER BY artist ASC, name ASC")
    fun getAlbumsByYear(year: Int): Flow<List<Album>>

    @Query("SELECT * FROM albums WHERE name LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchAlbums(query: String): Flow<List<Album>>

    @Query("SELECT * FROM albums ORDER BY date_added DESC LIMIT :limit")
    fun getRecentlyAddedAlbums(limit: Int = 50): Flow<List<Album>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: Album): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbums(albums: List<Album>): List<Long>

    @Update
    suspend fun updateAlbum(album: Album)

    @Delete
    suspend fun deleteAlbum(album: Album)

    @Query("DELETE FROM albums WHERE id = :albumId")
    suspend fun deleteAlbumById(albumId: Long)

    @Query("DELETE FROM albums")
    suspend fun deleteAllAlbums()

    @Query("SELECT COUNT(*) FROM albums")
    suspend fun getAlbumCount(): Int

    @Query("SELECT DISTINCT year FROM albums WHERE year IS NOT NULL ORDER BY year DESC")
    suspend fun getAllYears(): List<Int>
}