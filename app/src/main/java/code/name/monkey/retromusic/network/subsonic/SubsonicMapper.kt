package code.name.monkey.retromusic.network.subsonic

import code.name.monkey.retromusic.model.Album
import code.name.monkey.retromusic.model.Artist
import code.name.monkey.retromusic.model.Genre
import code.name.monkey.retromusic.model.Playlist
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.network.subsonic.model.SubsonicAlbum
import code.name.monkey.retromusic.network.subsonic.model.SubsonicArtist
import code.name.monkey.retromusic.network.subsonic.model.SubsonicGenre
import code.name.monkey.retromusic.network.subsonic.model.SubsonicPlaylist
import code.name.monkey.retromusic.network.subsonic.model.SubsonicSong
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

object SubsonicMapper {
    private const val SUBSONIC_ID_OFFSET = Long.MIN_VALUE / 2

    private val subsonicToRetroMap = ConcurrentHashMap<String, Long>()
    private val retroToSubsonicMap = ConcurrentHashMap<Long, String>()
    private val retroToLocalSongMap = ConcurrentHashMap<Long, Long>()
    private val retroToLocalAlbumMap = ConcurrentHashMap<Long, Long>()
    private val retroToLocalArtistMap = ConcurrentHashMap<Long, Long>()
    private val songCache = ConcurrentHashMap<Long, SubsonicSong>()

    fun registerMapping(retroId: Long, subsonicId: String) {
        if (subsonicId.isNotBlank()) {
            subsonicToRetroMap[subsonicId] = retroId
            retroToSubsonicMap[retroId] = subsonicId
        }
    }

    fun registerLocalMapping(retroId: Long, localMediaStoreId: Long) {
        if (localMediaStoreId > 0) {
            retroToLocalSongMap[retroId] = localMediaStoreId
        }
    }

    fun toLocalMediaStoreId(retroId: Long): Long? = retroToLocalSongMap[retroId]

    fun registerLocalAlbumMapping(retroId: Long, localAlbumId: Long) {
        if (localAlbumId > 0) {
            retroToLocalAlbumMap[retroId] = localAlbumId
        }
    }

    fun toLocalAlbumId(retroId: Long): Long? = retroToLocalAlbumMap[retroId]

    fun registerLocalArtistMapping(retroId: Long, localArtistId: Long) {
        if (localArtistId > 0) {
            retroToLocalArtistMap[retroId] = localArtistId
        }
    }

    fun toLocalArtistId(retroId: Long): Long? = retroToLocalArtistMap[retroId]

    fun toRetroId(subsonicId: String): Long {
        if (subsonicId.isBlank()) return -1L
        subsonicToRetroMap[subsonicId]?.let { return it }

        var hash = -3750763034362895579L
        for (b in subsonicId.toByteArray(Charsets.UTF_8)) {
            hash = hash xor (b.toLong() and 0xffL)
            hash *= 1099511628211L
        }
        val positiveHash = abs(hash)
        val retroId = SUBSONIC_ID_OFFSET + (positiveHash % (Long.MAX_VALUE / 4))

        subsonicToRetroMap[subsonicId] = retroId
        retroToSubsonicMap[retroId] = subsonicId
        return retroId
    }

    fun toSubsonicId(retroId: Long): String? {
        return retroToSubsonicMap[retroId]
    }

    fun isSubsonicId(id: Long): Boolean {
        return id < 0 || retroToSubsonicMap.containsKey(id)
    }

    fun getCachedSong(retroId: Long): SubsonicSong? = songCache[retroId]

    suspend fun getSongDetails(retroId: Long): SubsonicSong? {
        songCache[retroId]?.let { return it }
        val subId = toSubsonicId(retroId) ?: return null
        return try {
            val api = SubsonicClientFactory.getApi() ?: return null
            val response = api.getSong(subId)
            val subSong = response.response.song
            if (subSong != null) {
                songCache[retroId] = subSong
            }
            subSong
        } catch (e: Exception) {
            null
        }
    }

    fun SubsonicSong.toSong(): Song {
        val sId = toRetroId(id)
        val aId = albumId?.let { toRetroId(it) } ?: toRetroId("album_${album ?: ""}")
        val artId = artistId?.let { toRetroId(it) } ?: toRetroId("artist_${artist ?: ""}")
        val streamUrl = SubsonicClientFactory.buildStreamUrl(id)

        songCache[sId] = this

        return Song(
            id = sId,
            title = title,
            trackNumber = track ?: 0,
            year = year ?: 0,
            duration = duration * 1000L,
            data = streamUrl,
            dateModified = System.currentTimeMillis() / 1000L,
            albumId = aId,
            albumName = album ?: "",
            artistId = artId,
            artistName = artist ?: "",
            composer = null,
            albumArtist = artist ?: ""
        )
    }

    fun SubsonicAlbum.toAlbum(songs: List<Song> = emptyList()): Album {
        val aId = toRetroId(id)
        val artId = artistId?.let { toRetroId(it) } ?: toRetroId("artist_${artist ?: ""}")
        val effectiveSongs = if (songs.isNotEmpty()) {
            songs
        } else {
            val count = if (songCount > 0) songCount else 1
            (0 until count).map { i ->
                Song(
                    id = toRetroId("album_song_${id}_$i"),
                    title = displayTitle,
                    trackNumber = i + 1,
                    year = year ?: 0,
                    duration = if (duration > 0) (duration * 1000L) / count else 0L,
                    data = SubsonicClientFactory.buildStreamUrl(id),
                    dateModified = 0L,
                    albumId = aId,
                    albumName = displayTitle,
                    artistId = artId,
                    artistName = artist ?: "",
                    composer = null,
                    albumArtist = artist ?: ""
                )
            }
        }
        return Album(id = aId, songs = effectiveSongs)
    }

    fun SubsonicArtist.toArtist(albums: List<Album> = emptyList()): Artist {
        val artId = toRetroId(id)
        val artist = Artist(artId, albums)
        artist.name = name
        return artist
    }

    fun SubsonicPlaylist.toPlaylist(): Playlist {
        val pId = toRetroId(id)
        return Playlist(id = pId, name = name)
    }

    fun SubsonicGenre.toGenre(): Genre {
        val gId = toRetroId(value)
        return Genre(id = gId, name = value, songCount = songCount)
    }
}
