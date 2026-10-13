package code.name.monkey.retromusic.db.catalog

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import code.name.monkey.retromusic.model.Album
import code.name.monkey.retromusic.model.Song

@Entity(
    tableName = "unified_albums",
    indices = [
        Index(value = ["local_album_id"]),
        Index(value = ["subsonic_album_id"]),
        Index(value = ["artist_id"]),
        Index(value = ["album_key"]),
        Index(value = ["title"])
    ]
)
data class UnifiedAlbumEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "artist_id")
    val artistId: Long = 0L,

    @ColumnInfo(name = "artist_name")
    val artistName: String = "",

    @ColumnInfo(name = "album_artist")
    val albumArtist: String? = null,

    @ColumnInfo(name = "year")
    val year: Int = 0,

    @ColumnInfo(name = "song_count")
    val songCount: Int = 0,

    @ColumnInfo(name = "date_modified")
    val dateModified: Long = 0L,

    @ColumnInfo(name = "local_album_id")
    val localAlbumId: Long? = null,

    @ColumnInfo(name = "subsonic_album_id")
    val subsonicAlbumId: String? = null,

    @ColumnInfo(name = "cover_art_url")
    val coverArtUrl: String? = null,

    @ColumnInfo(name = "source")
    val source: Int = UnifiedSongEntity.SOURCE_LOCAL,

    @ColumnInfo(name = "album_key")
    val albumKey: String = ""
) {
    fun toAlbum(songs: List<Song>): Album {
        if (!subsonicAlbumId.isNullOrBlank()) {
            code.name.monkey.retromusic.network.subsonic.SubsonicMapper.registerMapping(id, subsonicAlbumId)
        }
        if (localAlbumId != null && localAlbumId > 0) {
            code.name.monkey.retromusic.network.subsonic.SubsonicMapper.registerLocalAlbumMapping(id, localAlbumId)
        }
        return Album(id = id, songs = songs)
    }

    companion object {
        fun createAlbumKey(title: String?, artist: String?): String {
            val t = UnifiedSongEntity.normalizeString(title)
            val a = UnifiedSongEntity.normalizeString(artist)
            return "$a|||$t"
        }
    }
}
