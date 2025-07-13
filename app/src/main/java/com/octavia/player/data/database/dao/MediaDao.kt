package com.octavia.player.data.database.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.octavia.player.data.model.*
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
    
    @Query("SELECT COUNT(*) FROM artists")
    suspend fun getArtistCount(): Int
}

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

/**
 * Data Access Object for Playlist operations
 */
@Dao
interface PlaylistDao {
    
    @Query("SELECT * FROM playlists ORDER BY name ASC")
    fun getAllPlaylistsFlow(): Flow<List<Playlist>>
    
    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: Long): Playlist?
    
    @Query("SELECT * FROM playlists WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchPlaylists(query: String): Flow<List<Playlist>>
    
    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN playlist_tracks pt ON t.id = pt.track_id
        WHERE pt.playlist_id = :playlistId
        ORDER BY pt.position ASC
    """)
    fun getPlaylistTracks(playlistId: Long): Flow<List<Track>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long
    
    @Update
    suspend fun updatePlaylist(playlist: Playlist)
    
    @Delete
    suspend fun deletePlaylist(playlist: Playlist)
    
    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylistById(playlistId: Long)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTrack(playlistTrack: PlaylistTrack)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTracks(playlistTracks: List<PlaylistTrack>)
    
    @Query("DELETE FROM playlist_tracks WHERE playlist_id = :playlistId AND track_id = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long)
    
    @Query("DELETE FROM playlist_tracks WHERE playlist_id = :playlistId")
    suspend fun clearPlaylist(playlistId: Long)
    
    @Query("SELECT MAX(position) FROM playlist_tracks WHERE playlist_id = :playlistId")
    suspend fun getLastPositionInPlaylist(playlistId: Long): Int?
    
    @Query("SELECT COUNT(*) FROM playlists")
    suspend fun getPlaylistCount(): Int
}
