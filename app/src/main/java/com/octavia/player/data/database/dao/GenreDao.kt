package com.octavia.player.data.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.octavia.player.data.model.Genre
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Genre operations
 */
@Dao
interface GenreDao {

    @Query("SELECT * FROM genres ORDER BY name ASC")
    fun getAllGenresPaged(): PagingSource<Int, Genre>

    @Query("SELECT * FROM genres ORDER BY name ASC")
    fun getAllGenresFlow(): Flow<List<Genre>>

    @Query("SELECT * FROM genres WHERE id = :id")
    suspend fun getGenreById(id: Long): Genre?

    @Query("SELECT * FROM genres WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchGenres(query: String): Flow<List<Genre>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGenre(genre: Genre): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGenres(genres: List<Genre>): List<Long>

    @Update
    suspend fun updateGenre(genre: Genre)

    @Delete
    suspend fun deleteGenre(genre: Genre)

    @Query("DELETE FROM genres WHERE id = :genreId")
    suspend fun deleteGenreById(genreId: Long)

    @Query("DELETE FROM genres")
    suspend fun deleteAllGenres()

    @Query("SELECT COUNT(*) FROM genres")
    suspend fun getGenreCount(): Int
}