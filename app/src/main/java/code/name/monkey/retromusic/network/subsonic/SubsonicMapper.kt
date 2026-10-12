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

    private val subsonicToMetroMap = ConcurrentHashMap<String, Long>()
    private val metroToSubsonicMap = ConcurrentHashMap<Long, String>()
    private val songCache = ConcurrentHashMap<Long, SubsonicSong>()

    fun toMetroId(subsonicId: String): Long {
        if (subsonicId.isBlank()) return -1L
        subsonicToMetroMap[subsonicId]?.let { return it }

        var hash = -3750763034362895579L
        for (b in subsonicId.toByteArray(Charsets.UTF_8)) {
            hash = hash xor (b.toLong() and 0xffL)
            hash *= 1099511628211L
        }
        val positiveHash = abs(hash)
        val metroId = SUBSONIC_ID_OFFSET + (positiveHash % (Long.MAX_VALUE / 4))

        subsonicToMetroMap[subsonicId] = metroId
        metroToSubsonicMap[metroId] = subsonicId
        return metroId
    }

    fun toSubsonicId(metroId: Long): String? {
        return metroToSubsonicMap[metroId]
    }

    fun isSubsonicId(id: Long): Boolean {
        return id < 0
    }

    fun getCachedSong(metroId: Long): SubsonicSong? = songCache[metroId]

    suspend fun getSongDetails(metroId: Long): SubsonicSong? {
        songCache[metroId]?.let { return it }
        val subId = toSubsonicId(metroId) ?: return null
        return try {
            val api = SubsonicClientFactory.getApi() ?: return null
            val response = api.getSong(subId)
            val subSong = response.response.song
            if (subSong != null) {
                songCache[metroId] = subSong
            }
            subSong
        } catch (e: Exception) {
            null
        }
    }

    fun SubsonicSong.toSong(): Song {
        val sId = toMetroId(id)
        val aId = albumId?.let { toMetroId(it) } ?: toMetroId("album_${album ?: ""}")
        val artId = artistId?.let { toMetroId(it) } ?: toMetroId("artist_${artist ?: ""}")
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
        val aId = toMetroId(id)
        val artId = artistId?.let { toMetroId(it) } ?: toMetroId("artist_${artist ?: ""}")
        val effectiveSongs = if (songs.isNotEmpty()) {
            songs
        } else {
            val count = if (songCount > 0) songCount else 1
            (0 until count).map { i ->
                Song(
                    id = toMetroId("album_song_${id}_$i"),
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
        val artId = toMetroId(id)
        val artist = Artist(artId, albums)
        artist.name = name
        return artist
    }

    fun SubsonicPlaylist.toPlaylist(): Playlist {
        val pId = toMetroId(id)
        return Playlist(id = pId, name = name)
    }

    fun SubsonicGenre.toGenre(): Genre {
        val gId = toMetroId(value)
        return Genre(id = gId, name = value, songCount = songCount)
    }
}
