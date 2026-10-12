package code.name.monkey.retromusic.repository

import code.name.monkey.retromusic.fragments.search.Filter
import code.name.monkey.retromusic.model.Album
import code.name.monkey.retromusic.model.Artist
import code.name.monkey.retromusic.model.Genre
import code.name.monkey.retromusic.model.Playlist
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.network.subsonic.SubsonicClientFactory
import code.name.monkey.retromusic.network.subsonic.SubsonicMapper
import code.name.monkey.retromusic.network.subsonic.SubsonicMapper.toAlbum
import code.name.monkey.retromusic.network.subsonic.SubsonicMapper.toArtist
import code.name.monkey.retromusic.network.subsonic.SubsonicMapper.toGenre
import code.name.monkey.retromusic.network.subsonic.SubsonicMapper.toPlaylist
import code.name.monkey.retromusic.network.subsonic.SubsonicMapper.toSong
import code.name.monkey.retromusic.network.subsonic.SubsonicPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SubsonicPingResult(
    val success: Boolean,
    val serverType: String? = null,
    val serverVersion: String? = null,
    val errorMessage: String? = null
)

interface SubsonicRepository {
    suspend fun ping(): Boolean
    suspend fun checkConnection(): SubsonicPingResult
    suspend fun songs(): List<Song>
    suspend fun albums(): List<Album>
    suspend fun album(albumId: Long): Album
    suspend fun artists(): List<Artist>
    suspend fun artist(artistId: Long): Artist
    suspend fun search(query: String?, filter: Filter): MutableList<Any>
    suspend fun searchSongs(query: String): List<Song>
    suspend fun searchAlbums(query: String): List<Album>
    suspend fun searchArtists(query: String): List<Artist>
    suspend fun playlists(): List<Playlist>
    suspend fun playlistSongs(playlistId: Long): List<Song>
    suspend fun genres(): List<Genre>
    suspend fun genreSongs(genreId: Long): List<Song>
    suspend fun scrobble(songId: Long)
    suspend fun star(id: Long)
    suspend fun unstar(id: Long)
}

class RealSubsonicRepository : SubsonicRepository {

    private var cachedSongs: List<Song> = emptyList()
    private var cachedAlbums: List<Album> = emptyList()
    private var cachedArtists: List<Artist> = emptyList()

    override suspend fun checkConnection(): SubsonicPingResult = withContext(Dispatchers.IO) {
        if (!SubsonicPreferences.isConfigured) {
            return@withContext SubsonicPingResult(
                success = false,
                errorMessage = "Server URL, username, or password not set"
            )
        }
        try {
            val api = SubsonicClientFactory.getApi() ?: return@withContext SubsonicPingResult(
                success = false,
                errorMessage = "Could not initialize server connection"
            )
            val response = api.ping()
            val subResponse = response.response
            if (subResponse.isOk) {
                val serverType = subResponse.type?.replaceFirstChar { it.uppercase() } ?: "Subsonic"
                val serverVersion = subResponse.serverVersion ?: subResponse.version
                SubsonicPingResult(
                    success = true,
                    serverType = serverType,
                    serverVersion = serverVersion
                )
            } else {
                val errorMsg = subResponse.error?.message
                    ?: "Server returned status: ${subResponse.status}"
                SubsonicPingResult(
                    success = false,
                    errorMessage = errorMsg
                )
            }
        } catch (e: Exception) {
            val errorMsg = when {
                e is java.net.ConnectException -> "Connection failed: ${e.message ?: "Server unreachable"}"
                e is java.net.UnknownHostException -> "Unknown host: ${e.message}"
                e is java.net.SocketTimeoutException -> "Connection timed out"
                e is retrofit2.HttpException -> "HTTP error ${e.code()}: ${e.message()}"
                e.cause != null -> e.cause?.localizedMessage ?: e.localizedMessage ?: "Connection error"
                else -> e.localizedMessage ?: "Connection error"
            }
            SubsonicPingResult(
                success = false,
                errorMessage = errorMsg
            )
        }
    }

    override suspend fun ping(): Boolean = checkConnection().success

    override suspend fun songs(): List<Song> = withContext(Dispatchers.IO) {
        try {
            val api = SubsonicClientFactory.getApi() ?: return@withContext cachedSongs
            val response = api.getRandomSongs(size = 500)
            val songs = response.response.randomSongs?.song?.map { it.toSong() } ?: emptyList()
            if (songs.isNotEmpty()) {
                cachedSongs = songs
            }
            cachedSongs
        } catch (e: Exception) {
            cachedSongs
        }
    }

    override suspend fun albums(): List<Album> = withContext(Dispatchers.IO) {
        try {
            val api = SubsonicClientFactory.getApi() ?: return@withContext cachedAlbums
            val response = api.getAlbumList2(type = "alphabeticalByName", size = 500)
            val albums = response.response.albumList2?.album?.map { it.toAlbum() } ?: emptyList()
            if (albums.isNotEmpty()) {
                cachedAlbums = albums
            }
            cachedAlbums
        } catch (e: Exception) {
            cachedAlbums
        }
    }

    override suspend fun album(albumId: Long): Album = withContext(Dispatchers.IO) {
        try {
            val subsonicId = SubsonicMapper.toSubsonicId(albumId)
            val api = SubsonicClientFactory.getApi()
            if (api != null && subsonicId != null) {
                val response = api.getAlbum(subsonicId)
                val subAlbum = response.response.album
                if (subAlbum != null) {
                    val songs = subAlbum.songs?.map { it.toSong() } ?: emptyList()
                    return@withContext subAlbum.toAlbum(songs)
                }
            }
        } catch (e: Exception) {
            // fallback
        }
        cachedAlbums.firstOrNull { it.id == albumId } ?: Album.empty
    }

    override suspend fun artists(): List<Artist> = withContext(Dispatchers.IO) {
        try {
            val api = SubsonicClientFactory.getApi() ?: return@withContext cachedArtists
            val response = api.getArtists()
            val artists = mutableListOf<Artist>()
            val albumsList = if (cachedAlbums.isNotEmpty()) cachedAlbums else try { albums() } catch (e: Exception) { emptyList() }
            val albumsByArtistId = albumsList.groupBy { SubsonicMapper.toSubsonicId(it.artistId) }
            val albumsByArtistName = albumsList.groupBy { it.artistName.trim().lowercase(java.util.Locale.ROOT) }

            response.response.artists?.index?.forEach { index ->
                index.artist?.forEach { subArtist ->
                    val artistAlbums = albumsByArtistId[subArtist.id]
                        ?: albumsByArtistName[subArtist.name.trim().lowercase(java.util.Locale.ROOT)]
                        ?: emptyList()
                    artists.add(subArtist.toArtist(artistAlbums))
                }
            }
            if (artists.isNotEmpty()) {
                cachedArtists = artists
            }
            cachedArtists
        } catch (e: Exception) {
            cachedArtists
        }
    }

    override suspend fun artist(artistId: Long): Artist = withContext(Dispatchers.IO) {
        try {
            val subsonicId = SubsonicMapper.toSubsonicId(artistId)
            val api = SubsonicClientFactory.getApi()
            if (api != null && subsonicId != null) {
                val response = api.getArtist(subsonicId)
                val subArtist = response.response.artist
                if (subArtist != null) {
                    val subAlbums = subArtist.albums?.map { it.toAlbum() }?.takeIf { it.isNotEmpty() }
                        ?: cachedAlbums.filter { SubsonicMapper.toSubsonicId(it.artistId) == subArtist.id || it.artistName.equals(subArtist.name, ignoreCase = true) }
                    return@withContext subArtist.toArtist(subAlbums)
                }
            }
        } catch (e: Exception) {
            // fallback
        }
        cachedArtists.firstOrNull { it.id == artistId } ?: Artist("-", emptyList())
    }

    override suspend fun search(query: String?, filter: Filter): MutableList<Any> = withContext(Dispatchers.IO) {
        val results = mutableListOf<Any>()
        if (query.isNullOrBlank()) return@withContext results
        try {
            val api = SubsonicClientFactory.getApi() ?: return@withContext results
            val response = api.search3(query = query)
            val searchResult = response.response.searchResult3 ?: return@withContext results

            when (filter) {
                Filter.SONGS -> {
                    searchResult.songs?.forEach { results.add(it.toSong()) }
                }
                Filter.ALBUMS -> {
                    searchResult.albums?.forEach { results.add(it.toAlbum()) }
                }
                Filter.ARTISTS, Filter.ALBUM_ARTISTS -> {
                    searchResult.artists?.forEach { results.add(it.toArtist()) }
                }
                Filter.NO_FILTER -> {
                    searchResult.songs?.forEach { results.add(it.toSong()) }
                    searchResult.albums?.forEach { results.add(it.toAlbum()) }
                    searchResult.artists?.forEach { results.add(it.toArtist()) }
                }
                else -> {
                    searchResult.songs?.forEach { results.add(it.toSong()) }
                    searchResult.albums?.forEach { results.add(it.toAlbum()) }
                    searchResult.artists?.forEach { results.add(it.toArtist()) }
                }
            }
        } catch (e: Exception) {
            // Return empty results on error
        }
        results
    }

    override suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        try {
            val api = SubsonicClientFactory.getApi() ?: return@withContext emptyList()
            val response = api.search3(query = query, artistCount = 0, albumCount = 0, songCount = 50)
            response.response.searchResult3?.songs?.map { it.toSong() } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun searchAlbums(query: String): List<Album> = withContext(Dispatchers.IO) {
        try {
            val api = SubsonicClientFactory.getApi() ?: return@withContext emptyList()
            val response = api.search3(query = query, artistCount = 0, albumCount = 50, songCount = 0)
            response.response.searchResult3?.albums?.map { it.toAlbum() } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun searchArtists(query: String): List<Artist> = withContext(Dispatchers.IO) {
        try {
            val api = SubsonicClientFactory.getApi() ?: return@withContext emptyList()
            val response = api.search3(query = query, artistCount = 50, albumCount = 0, songCount = 0)
            val albumsList = if (cachedAlbums.isNotEmpty()) cachedAlbums else try { albums() } catch (e: Exception) { emptyList() }
            val albumsByArtistId = albumsList.groupBy { SubsonicMapper.toSubsonicId(it.artistId) }
            val albumsByArtistName = albumsList.groupBy { it.artistName.trim().lowercase(java.util.Locale.ROOT) }

            response.response.searchResult3?.artists?.map { subArtist ->
                val artistAlbums = albumsByArtistId[subArtist.id]
                    ?: albumsByArtistName[subArtist.name.trim().lowercase(java.util.Locale.ROOT)]
                    ?: emptyList()
                subArtist.toArtist(artistAlbums)
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun playlists(): List<Playlist> = withContext(Dispatchers.IO) {
        try {
            val api = SubsonicClientFactory.getApi() ?: return@withContext emptyList()
            val response = api.getPlaylists()
            response.response.playlists?.playlist?.map { it.toPlaylist() } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun playlistSongs(playlistId: Long): List<Song> = withContext(Dispatchers.IO) {
        try {
            val subsonicId = SubsonicMapper.toSubsonicId(playlistId)
            val api = SubsonicClientFactory.getApi()
            if (api != null && subsonicId != null) {
                val response = api.getPlaylist(subsonicId)
                return@withContext response.response.playlist?.entry?.map { it.toSong() } ?: emptyList()
            }
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun genres(): List<Genre> = withContext(Dispatchers.IO) {
        try {
            val api = SubsonicClientFactory.getApi() ?: return@withContext emptyList()
            val response = api.getGenres()
            response.response.genres?.genre?.map { it.toGenre() } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun genreSongs(genreId: Long): List<Song> = withContext(Dispatchers.IO) {
        try {
            val genreName = SubsonicMapper.toSubsonicId(genreId)
            val api = SubsonicClientFactory.getApi()
            if (api != null && genreName != null) {
                val response = api.getSongsByGenre(genre = genreName)
                return@withContext response.response.songsByGenre?.song?.map { it.toSong() } ?: emptyList()
            }
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun scrobble(songId: Long): Unit = withContext(Dispatchers.IO) {
        try {
            val subsonicId = SubsonicMapper.toSubsonicId(songId)
            val api = SubsonicClientFactory.getApi()
            if (api != null && subsonicId != null) {
                api.scrobble(id = subsonicId)
            }
        } catch (e: Exception) {
            // scrobble is best effort
        }
    }

    override suspend fun star(id: Long): Unit = withContext(Dispatchers.IO) {
        try {
            val subsonicId = SubsonicMapper.toSubsonicId(id)
            val api = SubsonicClientFactory.getApi()
            if (api != null && subsonicId != null) {
                api.star(id = subsonicId)
            }
        } catch (e: Exception) {
            // best effort
        }
    }

    override suspend fun unstar(id: Long): Unit = withContext(Dispatchers.IO) {
        try {
            val subsonicId = SubsonicMapper.toSubsonicId(id)
            val api = SubsonicClientFactory.getApi()
            if (api != null && subsonicId != null) {
                api.unstar(id = subsonicId)
            }
        } catch (e: Exception) {
            // best effort
        }
    }
}
