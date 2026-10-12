package code.name.monkey.retromusic.db.catalog

import androidx.room.*

@Dao
interface UnifiedMusicDao {

    // --- Songs ---
    @Query("SELECT * FROM unified_songs ORDER BY title COLLATE NOCASE ASC")
    suspend fun getAllSongs(): List<UnifiedSongEntity>

    @Query("SELECT * FROM unified_songs WHERE (source & :sourceMask) != 0 ORDER BY title COLLATE NOCASE ASC")
    suspend fun getSongsFiltered(sourceMask: Int): List<UnifiedSongEntity>

    @Query("SELECT * FROM unified_songs WHERE id = :id LIMIT 1")
    suspend fun getSongById(id: Long): UnifiedSongEntity?

    @Query("SELECT * FROM unified_songs WHERE id IN (:ids)")
    suspend fun getSongsByIds(ids: List<Long>): List<UnifiedSongEntity>

    @Query("SELECT * FROM unified_songs WHERE album_id = :albumId ORDER BY disc_number ASC, track_number ASC, title COLLATE NOCASE ASC")
    suspend fun getSongsByAlbumId(albumId: Long): List<UnifiedSongEntity>

    @Query("SELECT * FROM unified_songs WHERE album_id = :albumId AND (source & :sourceMask) != 0 ORDER BY disc_number ASC, track_number ASC, title COLLATE NOCASE ASC")
    suspend fun getSongsByAlbumIdFiltered(albumId: Long, sourceMask: Int): List<UnifiedSongEntity>

    @Query("SELECT * FROM unified_songs WHERE artist_id = :artistId ORDER BY title COLLATE NOCASE ASC")
    suspend fun getSongsByArtistId(artistId: Long): List<UnifiedSongEntity>

    @Query("SELECT * FROM unified_songs WHERE artist_id = :artistId AND (source & :sourceMask) != 0 ORDER BY title COLLATE NOCASE ASC")
    suspend fun getSongsByArtistIdFiltered(artistId: Long, sourceMask: Int): List<UnifiedSongEntity>

    @Query("SELECT * FROM unified_songs WHERE genre_id = :genreId ORDER BY title COLLATE NOCASE ASC")
    suspend fun getSongsByGenreId(genreId: Long): List<UnifiedSongEntity>

    @Query("SELECT * FROM unified_songs WHERE local_media_store_id = :mediaStoreId LIMIT 1")
    suspend fun getSongByMediaStoreId(mediaStoreId: Long): UnifiedSongEntity?

    @Query("SELECT * FROM unified_songs WHERE subsonic_id = :subsonicId LIMIT 1")
    suspend fun getSongBySubsonicId(subsonicId: String): UnifiedSongEntity?

    @Query("SELECT * FROM unified_songs WHERE fingerprint = :fingerprint LIMIT 1")
    suspend fun getSongByFingerprint(fingerprint: String): UnifiedSongEntity?

    @Query("SELECT * FROM unified_songs WHERE (source & :sourceMask) != 0 AND (title LIKE '%' || :query || '%' OR artist_name LIKE '%' || :query || '%' OR album_name LIKE '%' || :query || '%') ORDER BY title COLLATE NOCASE ASC")
    suspend fun searchSongs(query: String, sourceMask: Int): List<UnifiedSongEntity>

    @Query("SELECT COUNT(*) FROM unified_songs")
    suspend fun getSongCount(): Int

    // --- Albums ---
    @Query("SELECT * FROM unified_albums ORDER BY title COLLATE NOCASE ASC")
    suspend fun getAllAlbums(): List<UnifiedAlbumEntity>

    @Query("SELECT * FROM unified_albums WHERE (source & :sourceMask) != 0 ORDER BY title COLLATE NOCASE ASC")
    suspend fun getAlbumsFiltered(sourceMask: Int): List<UnifiedAlbumEntity>

    @Query("SELECT * FROM unified_albums WHERE id = :id LIMIT 1")
    suspend fun getAlbumById(id: Long): UnifiedAlbumEntity?

    @Query("SELECT * FROM unified_albums WHERE artist_id = :artistId ORDER BY year DESC, title COLLATE NOCASE ASC")
    suspend fun getAlbumsByArtistId(artistId: Long): List<UnifiedAlbumEntity>

    @Query("SELECT * FROM unified_albums WHERE artist_id = :artistId AND (source & :sourceMask) != 0 ORDER BY year DESC, title COLLATE NOCASE ASC")
    suspend fun getAlbumsByArtistIdFiltered(artistId: Long, sourceMask: Int): List<UnifiedAlbumEntity>

    @Query("SELECT * FROM unified_albums WHERE album_key = :key LIMIT 1")
    suspend fun getAlbumByKey(key: String): UnifiedAlbumEntity?

    @Query("SELECT * FROM unified_albums WHERE (source & :sourceMask) != 0 AND (title LIKE '%' || :query || '%' OR artist_name LIKE '%' || :query || '%') ORDER BY title COLLATE NOCASE ASC")
    suspend fun searchAlbums(query: String, sourceMask: Int): List<UnifiedAlbumEntity>

    @Query("SELECT COUNT(*) FROM unified_albums")
    suspend fun getAlbumCount(): Int

    // --- Artists ---
    @Query("SELECT * FROM unified_artists ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllArtists(): List<UnifiedArtistEntity>

    @Query("SELECT * FROM unified_artists WHERE (source & :sourceMask) != 0 ORDER BY name COLLATE NOCASE ASC")
    suspend fun getArtistsFiltered(sourceMask: Int): List<UnifiedArtistEntity>

    @Query("SELECT * FROM unified_artists WHERE id = :id LIMIT 1")
    suspend fun getArtistById(id: Long): UnifiedArtistEntity?

    @Query("SELECT * FROM unified_artists WHERE artist_key = :key LIMIT 1")
    suspend fun getArtistByKey(key: String): UnifiedArtistEntity?

    @Query("SELECT * FROM unified_artists WHERE (source & :sourceMask) != 0 AND name LIKE '%' || :query || '%' ORDER BY name COLLATE NOCASE ASC")
    suspend fun searchArtists(query: String, sourceMask: Int): List<UnifiedArtistEntity>

    @Query("SELECT COUNT(*) FROM unified_artists")
    suspend fun getArtistCount(): Int

    // --- Genres ---
    @Query("SELECT * FROM unified_genres ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllGenres(): List<UnifiedGenreEntity>

    @Query("SELECT * FROM unified_genres WHERE id = :id LIMIT 1")
    suspend fun getGenreById(id: Long): UnifiedGenreEntity?

    @Query("SELECT * FROM unified_genres WHERE name = :name LIMIT 1")
    suspend fun getGenreByName(name: String): UnifiedGenreEntity?

    // --- Inserts / Updates ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: UnifiedSongEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<UnifiedSongEntity>): List<Long>

    @Update
    suspend fun updateSong(song: UnifiedSongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: UnifiedAlbumEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbums(albums: List<UnifiedAlbumEntity>): List<Long>

    @Update
    suspend fun updateAlbum(album: UnifiedAlbumEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtist(artist: UnifiedArtistEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtists(artists: List<UnifiedArtistEntity>): List<Long>

    @Update
    suspend fun updateArtist(artist: UnifiedArtistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGenre(genre: UnifiedGenreEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGenres(genres: List<UnifiedGenreEntity>): List<Long>

    // --- Clean up ---
    @Query("DELETE FROM unified_songs")
    suspend fun deleteAllSongs()

    @Query("DELETE FROM unified_albums")
    suspend fun deleteAllAlbums()

    @Query("DELETE FROM unified_artists")
    suspend fun deleteAllArtists()

    @Query("DELETE FROM unified_genres")
    suspend fun deleteAllGenres()
}
