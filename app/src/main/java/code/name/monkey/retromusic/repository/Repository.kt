/*
 * Copyright (c) 2019 Hemanth Savarala.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by
 *  the Free Software Foundation either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */

package code.name.monkey.retromusic.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import code.name.monkey.retromusic.*
import code.name.monkey.retromusic.db.*
import code.name.monkey.retromusic.db.catalog.MusicCatalogSyncEngine
import code.name.monkey.retromusic.db.catalog.UnifiedMusicDao
import code.name.monkey.retromusic.fragments.search.Filter
import code.name.monkey.retromusic.helper.SortOrder
import code.name.monkey.retromusic.model.*
import code.name.monkey.retromusic.model.AbsCustomPlaylist
import code.name.monkey.retromusic.model.smartplaylist.NotPlayedPlaylist
import code.name.monkey.retromusic.network.LastFMService
import code.name.monkey.retromusic.network.Result
import code.name.monkey.retromusic.network.Result.Error
import code.name.monkey.retromusic.network.Result.Success
import code.name.monkey.retromusic.network.model.LastFmAlbum
import code.name.monkey.retromusic.network.model.LastFmArtist
import code.name.monkey.retromusic.network.subsonic.SubsonicPreferences
import code.name.monkey.retromusic.util.PreferenceUtil
import code.name.monkey.retromusic.util.logE
import java.text.Collator

interface Repository {

    fun historySong(): List<HistoryEntity>
    fun favorites(): LiveData<List<SongEntity>>
    fun observableHistorySongs(): LiveData<List<Song>>
    fun albumById(albumId: Long): Album
    fun playlistSongs(playListId: Long): LiveData<List<SongEntity>>
    suspend fun fetchAlbums(): List<Album>
    suspend fun albumByIdAsync(albumId: Long): Album
    suspend fun allSongs(): List<Song>
    suspend fun fetchArtists(): List<Artist>
    suspend fun albumArtists(): List<Artist>
    suspend fun fetchLegacyPlaylist(): List<Playlist>
    suspend fun fetchGenres(): List<Genre>
    suspend fun search(query: String?, filter: Filter): MutableList<Any>
    suspend fun getPlaylistSongs(playlist: Playlist): List<Song>
    suspend fun getGenre(genreId: Long): List<Song>
    suspend fun artistInfo(name: String, lang: String?, cache: String?): Result<LastFmArtist>
    suspend fun albumInfo(artist: String, album: String): Result<LastFmAlbum>
    suspend fun artistById(artistId: Long): Artist
    suspend fun albumArtistByName(name: String): Artist
    suspend fun recentArtists(): List<Artist>
    suspend fun topArtists(): List<Artist>
    suspend fun topAlbums(): List<Album>
    suspend fun recentAlbums(): List<Album>
    suspend fun recentArtistsHome(): Home
    suspend fun topArtistsHome(): Home
    suspend fun topAlbumsHome(): Home
    suspend fun recentAlbumsHome(): Home
    suspend fun favoritePlaylistHome(): Home
    suspend fun suggestions(): List<Song>
    suspend fun genresHome(): Home
    suspend fun playlists(): Home
    suspend fun homeSections(): List<Home>
    suspend fun playlist(playlistId: Long): Playlist
    suspend fun fetchPlaylistWithSongs(): List<PlaylistWithSongs>
    suspend fun playlistSongs(playlistWithSongs: PlaylistWithSongs): List<Song>
    suspend fun insertSongs(songs: List<SongEntity>)
    suspend fun checkPlaylistExists(playlistName: String): List<PlaylistEntity>
    suspend fun createPlaylist(playlistEntity: PlaylistEntity): Long
    suspend fun fetchPlaylists(): List<PlaylistEntity>
    suspend fun deleteRoomPlaylist(playlists: List<PlaylistEntity>)
    suspend fun renameRoomPlaylist(playlistId: Long, name: String)
    suspend fun deleteSongsInPlaylist(songs: List<SongEntity>)
    suspend fun removeSongFromPlaylist(songEntity: SongEntity)
    suspend fun deletePlaylistSongs(playlists: List<PlaylistEntity>)
    suspend fun favoritePlaylist(): PlaylistEntity
    suspend fun isFavoriteSong(songEntity: SongEntity): List<SongEntity>
    suspend fun upsertSongInHistory(currentSong: Song)
    suspend fun favoritePlaylistSongs(): List<SongEntity>
    suspend fun recentSongs(): List<Song>
    suspend fun topPlayedSongs(): List<Song>
    suspend fun upsertSongInPlayCount(playCountEntity: PlayCountEntity)
    suspend fun deleteSongInPlayCount(playCountEntity: PlayCountEntity)
    suspend fun deleteSongInHistory(songId: Long)
    suspend fun clearSongHistory()
    suspend fun findSongExistInPlayCount(songId: Long): PlayCountEntity?
    suspend fun playCountSongs(): List<PlayCountEntity>
    suspend fun deleteSongs(songs: List<Song>)
    suspend fun contributor(): List<Contributor>
    suspend fun searchArtists(query: String): List<Artist>
    suspend fun searchSongs(query: String): List<Song>
    suspend fun searchAlbums(query: String): List<Album>
    val subsonicRepository: SubsonicRepository
    suspend fun isSongFavorite(songId: Long): Boolean
    fun getSongByGenre(genreId: Long): Song
    fun checkPlaylistExists(playListId: Long): LiveData<Boolean>
    fun getPlaylist(playlistId: Long): LiveData<PlaylistWithSongs>
}

class RealRepository(
    private val context: Context,
    private val lastFMService: LastFMService,
    private val songRepository: SongRepository,
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository,
    private val genreRepository: GenreRepository,
    private val lastAddedRepository: LastAddedRepository,
    private val playlistRepository: PlaylistRepository,
    private val searchRepository: RealSearchRepository,
    private val topPlayedRepository: TopPlayedRepository,
    private val roomRepository: RoomRepository,
    private val localDataRepository: LocalDataRepository,
    override val subsonicRepository: SubsonicRepository,
    private val catalogDao: UnifiedMusicDao,
    private val syncEngine: MusicCatalogSyncEngine
) : Repository {

    private fun getSourceMask(): Int {
        return when (SubsonicPreferences.libraryFilter) {
            SubsonicPreferences.FILTER_LOCAL -> 1
            SubsonicPreferences.FILTER_SERVER -> 2
            else -> 3
        }
    }

    private suspend fun ensureCatalogLoaded() {
        if (catalogDao.getSongCount() == 0) {
            syncEngine.syncAll(songRepository, albumRepository, artistRepository, genreRepository)
        }
    }

    override suspend fun deleteSongs(songs: List<Song>) {
        roomRepository.deleteSongs(songs)
        syncEngine.syncLocalCatalog(songRepository, albumRepository, artistRepository, genreRepository)
    }

    override suspend fun contributor(): List<Contributor> = localDataRepository.contributors()

    override suspend fun searchArtists(query: String): List<Artist> =
        search(query, Filter.ARTISTS).filterIsInstance<Artist>()

    override suspend fun searchSongs(query: String): List<Song> =
        search(query, Filter.SONGS).filterIsInstance<Song>()

    override suspend fun searchAlbums(query: String): List<Album> =
        search(query, Filter.ALBUMS).filterIsInstance<Album>()

    override suspend fun fetchAlbums(): List<Album> {
        ensureCatalogLoaded()
        val mask = getSourceMask()
        val albumEntities = catalogDao.getAlbumsFiltered(mask)
        val albums = albumEntities.map { albumEntity ->
            val songs = catalogDao.getSongsByAlbumIdFiltered(albumEntity.id, mask).map { it.toSong() }
            albumEntity.toAlbum(songs)
        }
        val collator = Collator.getInstance()
        return when (PreferenceUtil.albumSortOrder) {
            SortOrder.AlbumSortOrder.ALBUM_A_Z -> albums.sortedWith { a1, a2 -> collator.compare(a1.title, a2.title) }
            SortOrder.AlbumSortOrder.ALBUM_Z_A -> albums.sortedWith { a1, a2 -> collator.compare(a2.title, a1.title) }
            SortOrder.AlbumSortOrder.ALBUM_ARTIST -> albums.sortedWith { a1, a2 -> collator.compare(a1.artistName, a2.artistName) }
            SortOrder.AlbumSortOrder.ALBUM_NUMBER_OF_SONGS -> albums.sortedByDescending { it.songCount }
            SortOrder.AlbumSortOrder.ALBUM_YEAR -> albums.sortedByDescending { it.year }
            SortOrder.AlbumSortOrder.ALBUM_DATE_ADDED -> albums.sortedByDescending { it.dateModified }
            else -> albums
        }
    }

    fun albumsForSongs(songs: List<Song>): List<Album> = albumRepository.albums(songs)

    override suspend fun albumByIdAsync(albumId: Long): Album {
        val mask = getSourceMask()
        val albumEntity = catalogDao.getAlbumById(albumId)
        if (albumEntity != null) {
            val songs = catalogDao.getSongsByAlbumIdFiltered(albumId, mask).map { it.toSong() }
            return albumEntity.toAlbum(songs)
        }
        return albumRepository.album(albumId)
    }

    override fun albumById(albumId: Long): Album =
        kotlinx.coroutines.runBlocking { albumByIdAsync(albumId) }

    override suspend fun fetchArtists(): List<Artist> {
        ensureCatalogLoaded()
        val mask = getSourceMask()
        val artistEntities = catalogDao.getArtistsFiltered(mask)
        val artists = artistEntities.map { artistEntity ->
            val albums = catalogDao.getAlbumsByArtistIdFiltered(artistEntity.id, mask).map { albumEntity ->
                val songs = catalogDao.getSongsByAlbumIdFiltered(albumEntity.id, mask).map { it.toSong() }
                albumEntity.toAlbum(songs)
            }
            artistEntity.toArtist(albums)
        }
        val collator = Collator.getInstance()
        return when (PreferenceUtil.artistSortOrder) {
            SortOrder.ArtistSortOrder.ARTIST_A_Z -> artists.sortedWith { a1, a2 -> collator.compare(a1.name, a2.name) }
            SortOrder.ArtistSortOrder.ARTIST_Z_A -> artists.sortedWith { a1, a2 -> collator.compare(a2.name, a1.name) }
            SortOrder.ArtistSortOrder.ARTIST_NUMBER_OF_SONGS -> artists.sortedByDescending { it.songCount }
            SortOrder.ArtistSortOrder.ARTIST_NUMBER_OF_ALBUMS -> artists.sortedByDescending { it.albumCount }
            else -> artists
        }
    }

    fun artistsForAlbums(albums: List<Album>): List<Artist> =
        artistRepository.artists(albums, PreferenceUtil.albumArtistsOnly)

    override suspend fun albumArtists(): List<Artist> = fetchArtists()

    override suspend fun artistById(artistId: Long): Artist {
        val mask = getSourceMask()
        val artistEntity = catalogDao.getArtistById(artistId)
        if (artistEntity != null) {
            val albums = catalogDao.getAlbumsByArtistIdFiltered(artistId, mask).map { albumEntity ->
                val songs = catalogDao.getSongsByAlbumIdFiltered(albumEntity.id, mask).map { it.toSong() }
                albumEntity.toAlbum(songs)
            }
            return artistEntity.toArtist(albums)
        }
        return artistRepository.artist(artistId)
    }

    override suspend fun albumArtistByName(name: String): Artist =
        artistRepository.albumArtist(name)

    override suspend fun recentArtists(): List<Artist> = lastAddedRepository.recentArtists()

    override suspend fun recentAlbums(): List<Album> = lastAddedRepository.recentAlbums()

    override suspend fun topArtists(): List<Artist> = topPlayedRepository.topArtists()

    override suspend fun topAlbums(): List<Album> = topPlayedRepository.topAlbums()

    override suspend fun fetchLegacyPlaylist(): List<Playlist> = playlistRepository.playlists()

    override suspend fun fetchGenres(): List<Genre> {
        val genres = catalogDao.getAllGenres().map { it.toGenre() }
        return genres.ifEmpty { genreRepository.genres() }
    }

    override suspend fun allSongs(): List<Song> {
        ensureCatalogLoaded()
        val mask = getSourceMask()
        val songs = catalogDao.getSongsFiltered(mask).map { it.toSong() }
        val collator = Collator.getInstance()
        return when (PreferenceUtil.songSortOrder) {
            SortOrder.SongSortOrder.SONG_A_Z -> songs.sortedWith { s1, s2 -> collator.compare(s1.title, s2.title) }
            SortOrder.SongSortOrder.SONG_Z_A -> songs.sortedWith { s1, s2 -> collator.compare(s2.title, s1.title) }
            SortOrder.SongSortOrder.SONG_ARTIST -> songs.sortedWith { s1, s2 -> collator.compare(s1.artistName, s2.artistName) }
            SortOrder.SongSortOrder.SONG_ALBUM -> songs.sortedWith { s1, s2 -> collator.compare(s1.albumName, s2.albumName) }
            SortOrder.SongSortOrder.SONG_YEAR -> songs.sortedByDescending { it.year }
            SortOrder.SongSortOrder.SONG_DURATION -> songs.sortedByDescending { it.duration }
            SortOrder.SongSortOrder.SONG_DATE_MODIFIED -> songs.sortedByDescending { it.dateModified }
            else -> songs
        }
    }

    override suspend fun search(query: String?, filter: Filter): MutableList<Any> {
        if (query.isNullOrBlank()) return mutableListOf()
        val mask = getSourceMask()
        val results = mutableListOf<Any>()
        when (filter) {
            Filter.SONGS -> results.addAll(catalogDao.searchSongs(query, mask).map { it.toSong() })
            Filter.ALBUMS -> {
                val albums = catalogDao.searchAlbums(query, mask).map { albumEntity ->
                    val songs = catalogDao.getSongsByAlbumIdFiltered(albumEntity.id, mask).map { it.toSong() }
                    albumEntity.toAlbum(songs)
                }
                results.addAll(albums)
            }
            Filter.ARTISTS -> {
                val artists = catalogDao.searchArtists(query, mask).map { artistEntity ->
                    val albums = catalogDao.getAlbumsByArtistIdFiltered(artistEntity.id, mask).map { albumEntity ->
                        val songs = catalogDao.getSongsByAlbumIdFiltered(albumEntity.id, mask).map { it.toSong() }
                        albumEntity.toAlbum(songs)
                    }
                    artistEntity.toArtist(albums)
                }
                results.addAll(artists)
            }
            Filter.NO_FILTER -> {
                results.addAll(catalogDao.searchSongs(query, mask).map { it.toSong() })
                val albums = catalogDao.searchAlbums(query, mask).map { albumEntity ->
                    val songs = catalogDao.getSongsByAlbumIdFiltered(albumEntity.id, mask).map { it.toSong() }
                    albumEntity.toAlbum(songs)
                }
                results.addAll(albums)
                val artists = catalogDao.searchArtists(query, mask).map { artistEntity ->
                    val albums = catalogDao.getAlbumsByArtistIdFiltered(artistEntity.id, mask).map { albumEntity ->
                        val songs = catalogDao.getSongsByAlbumIdFiltered(albumEntity.id, mask).map { it.toSong() }
                        albumEntity.toAlbum(songs)
                    }
                    artistEntity.toArtist(albums)
                }
                results.addAll(artists)
            }
            else -> {}
        }
        return results
    }

    override suspend fun getPlaylistSongs(playlist: Playlist): List<Song> =
        if (playlist is AbsCustomPlaylist) {
            playlist.songs()
        } else {
            PlaylistSongsLoader.getPlaylistSongList(context, playlist.id)
        }

    override suspend fun getGenre(genreId: Long): List<Song> {
        val songs = catalogDao.getSongsByGenreId(genreId).map { it.toSong() }
        return songs.ifEmpty { genreRepository.songs(genreId) }
    }

    override fun getSongByGenre(genreId: Long): Song = kotlinx.coroutines.runBlocking {
        catalogDao.getSongsByGenreId(genreId).firstOrNull()?.toSong() ?: Song.emptySong
    }

    override suspend fun isSongFavorite(songId: Long): Boolean {
        return favoritePlaylistSongs().any { it.id == songId }
    }

    override suspend fun artistInfo(
        name: String,
        lang: String?,
        cache: String?,
    ): Result<LastFmArtist> {
        return try {
            Success(lastFMService.artistInfo(name, lang, cache))
        } catch (e: Exception) {
            logE(e)
            Error(e)
        }
    }

    override suspend fun albumInfo(
        artist: String,
        album: String,
    ): Result<LastFmAlbum> {
        return try {
            val lastFmAlbum = lastFMService.albumInfo(artist, album)
            Success(lastFmAlbum)
        } catch (e: Exception) {
            logE(e)
            Error(e)
        }
    }

    override suspend fun homeSections(): List<Home> {
        val homeSections = mutableListOf<Home>()
        val sections: List<Home> = listOf(
            topArtistsHome(),
            topAlbumsHome(),
            recentArtistsHome(),
            recentAlbumsHome(),
            favoritePlaylistHome()
        )
        for (section in sections) {
            if (section.arrayList.isNotEmpty()) {
                homeSections.add(section)
            }
        }
        return homeSections
    }

    override suspend fun playlist(playlistId: Long) =
        playlistRepository.playlist(playlistId)

    override suspend fun fetchPlaylistWithSongs(): List<PlaylistWithSongs> =
        roomRepository.playlistWithSongs()

    override fun getPlaylist(playlistId: Long): LiveData<PlaylistWithSongs> = roomRepository.getPlaylist(playlistId)

    override suspend fun playlistSongs(playlistWithSongs: PlaylistWithSongs): List<Song> =
        playlistWithSongs.songs.map {
            it.toSong()
        }

    override fun playlistSongs(playListId: Long): LiveData<List<SongEntity>> =
        roomRepository.getSongs(playListId)

    override suspend fun insertSongs(songs: List<SongEntity>) =
        roomRepository.insertSongs(songs)

    override suspend fun checkPlaylistExists(playlistName: String): List<PlaylistEntity> =
        roomRepository.checkPlaylistExists(playlistName)

    override fun checkPlaylistExists(playListId: Long): LiveData<Boolean> =
        roomRepository.checkPlaylistExists(playListId)

    override suspend fun createPlaylist(playlistEntity: PlaylistEntity): Long =
        roomRepository.createPlaylist(playlistEntity)

    override suspend fun fetchPlaylists(): List<PlaylistEntity> = roomRepository.playlists()

    override suspend fun deleteRoomPlaylist(playlists: List<PlaylistEntity>) =
        roomRepository.deletePlaylistEntities(playlists)

    override suspend fun renameRoomPlaylist(playlistId: Long, name: String) =
        roomRepository.renamePlaylistEntity(playlistId, name)

    override suspend fun deleteSongsInPlaylist(songs: List<SongEntity>) =
        roomRepository.deleteSongsInPlaylist(songs)

    override suspend fun removeSongFromPlaylist(songEntity: SongEntity) =
        roomRepository.removeSongFromPlaylist(songEntity)

    override suspend fun deletePlaylistSongs(playlists: List<PlaylistEntity>) =
        roomRepository.deletePlaylistSongs(playlists)

    override suspend fun favoritePlaylist(): PlaylistEntity =
        roomRepository.favoritePlaylist(context.getString(R.string.favorites))

    override suspend fun isFavoriteSong(songEntity: SongEntity): List<SongEntity> =
        roomRepository.isFavoriteSong(songEntity)

    override suspend fun upsertSongInHistory(currentSong: Song) =
        roomRepository.upsertSongInHistory(currentSong)

    override suspend fun favoritePlaylistSongs(): List<SongEntity> =
        roomRepository.favoritePlaylistSongs(context.getString(R.string.favorites))

    override suspend fun recentSongs(): List<Song> = lastAddedRepository.recentSongs()

    override suspend fun topPlayedSongs(): List<Song> = topPlayedRepository.topTracks()

    override suspend fun upsertSongInPlayCount(playCountEntity: PlayCountEntity) =
        roomRepository.upsertSongInPlayCount(playCountEntity)

    override suspend fun deleteSongInPlayCount(playCountEntity: PlayCountEntity) =
        roomRepository.deleteSongInPlayCount(playCountEntity)

    override suspend fun deleteSongInHistory(songId: Long) =
        roomRepository.deleteSongInHistory(songId)

    override suspend fun clearSongHistory() {
        roomRepository.clearSongHistory()
    }

    override suspend fun findSongExistInPlayCount(songId: Long): PlayCountEntity? =
        roomRepository.findSongExistInPlayCount(songId)

    override suspend fun playCountSongs(): List<PlayCountEntity> =
        roomRepository.playCountSongs()

    override fun observableHistorySongs(): LiveData<List<Song>> =
        roomRepository.observableHistorySongs().map {
            it.fromHistoryToSongs()
        }

    override fun historySong(): List<HistoryEntity> =
        roomRepository.historySongs()

    override fun favorites(): LiveData<List<SongEntity>> =
        roomRepository.favoritePlaylistLiveData(context.getString(R.string.favorites))

    override suspend fun suggestions(): List<Song> {
        return NotPlayedPlaylist().songs().shuffled().takeIf {
            it.size > 9
        } ?: emptyList()
    }

    override suspend fun genresHome(): Home {
        val genres = genreRepository.genres().shuffled()
        return Home(genres, GENRES, R.string.genres)
    }

    override suspend fun playlists(): Home {
        val playlist = playlistRepository.playlists()
        return Home(playlist, PLAYLISTS, R.string.playlists)
    }

    override suspend fun recentArtistsHome(): Home {
        val artists = lastAddedRepository.recentArtists().take(5)
        return Home(artists, RECENT_ARTISTS, R.string.recent_artists)
    }

    override suspend fun recentAlbumsHome(): Home {
        val albums = lastAddedRepository.recentAlbums().take(5)
        return Home(albums, RECENT_ALBUMS, R.string.recent_albums)
    }

    override suspend fun topAlbumsHome(): Home {
        val albums = topPlayedRepository.topAlbums().take(5)
        return Home(albums, TOP_ALBUMS, R.string.top_albums)
    }

    override suspend fun topArtistsHome(): Home {
        val artists = topPlayedRepository.topArtists().take(5)
        return Home(artists, TOP_ARTISTS, R.string.top_artists)
    }

    override suspend fun favoritePlaylistHome(): Home {
        val songs = favoritePlaylistSongs().map {
            it.toSong()
        }
        return Home(songs, FAVOURITES, R.string.favorites)
    }
}
