package code.name.monkey.retromusic.db.catalog

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import code.name.monkey.retromusic.model.Album
import code.name.monkey.retromusic.model.Artist

@Entity(
    tableName = "unified_artists",
    indices = [
        Index(value = ["local_artist_id"]),
        Index(value = ["subsonic_artist_id"]),
        Index(value = ["artist_key"]),
        Index(value = ["name"])
    ]
)
data class UnifiedArtistEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "song_count")
    val songCount: Int = 0,

    @ColumnInfo(name = "album_count")
    val albumCount: Int = 0,

    @ColumnInfo(name = "local_artist_id")
    val localArtistId: Long? = null,

    @ColumnInfo(name = "subsonic_artist_id")
    val subsonicArtistId: String? = null,

    @ColumnInfo(name = "cover_art_url")
    val coverArtUrl: String? = null,

    @ColumnInfo(name = "source")
    val source: Int = UnifiedSongEntity.SOURCE_LOCAL,

    @ColumnInfo(name = "artist_key")
    val artistKey: String = ""
) {
    fun toArtist(albums: List<Album>): Artist {
        if (!subsonicArtistId.isNullOrBlank()) {
            code.name.monkey.retromusic.network.subsonic.SubsonicMapper.registerMapping(id, subsonicArtistId)
        }
        if (localArtistId != null && localArtistId > 0) {
            code.name.monkey.retromusic.network.subsonic.SubsonicMapper.registerLocalArtistMapping(id, localArtistId)
        }
        val artist = Artist(id, albums)
        artist.name = name
        return artist
    }

    companion object {
        fun createArtistKey(name: String?): String {
            return UnifiedSongEntity.normalizeString(name)
        }
    }
}
