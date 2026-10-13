package code.name.monkey.retromusic.db.catalog

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.network.subsonic.SubsonicClientFactory

@Entity(
    tableName = "unified_songs",
    indices = [
        Index(value = ["local_media_store_id"]),
        Index(value = ["subsonic_id"]),
        Index(value = ["album_id"]),
        Index(value = ["artist_id"]),
        Index(value = ["genre_id"]),
        Index(value = ["fingerprint"]),
        Index(value = ["title"])
    ]
)
data class UnifiedSongEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "track_number")
    val trackNumber: Int = 0,

    @ColumnInfo(name = "disc_number")
    val discNumber: Int = 0,

    @ColumnInfo(name = "year")
    val year: Int = 0,

    @ColumnInfo(name = "duration")
    val duration: Long = 0L,

    @ColumnInfo(name = "local_path")
    val localPath: String? = null,

    @ColumnInfo(name = "date_modified")
    val dateModified: Long = 0L,

    @ColumnInfo(name = "album_id")
    val albumId: Long = 0L,

    @ColumnInfo(name = "album_name")
    val albumName: String = "",

    @ColumnInfo(name = "artist_id")
    val artistId: Long = 0L,

    @ColumnInfo(name = "artist_name")
    val artistName: String = "",

    @ColumnInfo(name = "composer")
    val composer: String? = null,

    @ColumnInfo(name = "album_artist")
    val albumArtist: String? = null,

    @ColumnInfo(name = "local_media_store_id")
    val localMediaStoreId: Long? = null,

    @ColumnInfo(name = "subsonic_id")
    val subsonicId: String? = null,

    @ColumnInfo(name = "source")
    val source: Int = SOURCE_LOCAL,

    @ColumnInfo(name = "genre_id")
    val genreId: Long = 0L,

    @ColumnInfo(name = "genre_name")
    val genreName: String? = null,

    @ColumnInfo(name = "fingerprint")
    val fingerprint: String = ""
) {
    fun toSong(): Song {
        if (!subsonicId.isNullOrBlank()) {
            code.name.monkey.retromusic.network.subsonic.SubsonicMapper.registerMapping(id, subsonicId)
        }
        if (localMediaStoreId != null && localMediaStoreId > 0) {
            code.name.monkey.retromusic.network.subsonic.SubsonicMapper.registerLocalMapping(id, localMediaStoreId)
        }

        val streamData = if (!localPath.isNullOrBlank()) {
            localPath
        } else if (!subsonicId.isNullOrBlank()) {
            SubsonicClientFactory.buildStreamUrl(subsonicId)
        } else {
            ""
        }

        return Song(
            id = id,
            title = title,
            trackNumber = trackNumber,
            year = year,
            duration = duration,
            data = streamData,
            dateModified = dateModified,
            albumId = albumId,
            albumName = albumName,
            artistId = artistId,
            artistName = artistName,
            composer = composer,
            albumArtist = albumArtist,
            localMediaStoreId = localMediaStoreId
        )
    }

    companion object {
        const val SOURCE_LOCAL = 1
        const val SOURCE_SERVER = 2
        const val SOURCE_BOTH = 3

        const val MASK_ALL = SOURCE_LOCAL or SOURCE_SERVER or SOURCE_BOTH // 3
        const val MASK_LOCAL = SOURCE_LOCAL or SOURCE_BOTH // 3 & 1 or 3 & 3
        const val MASK_SERVER = SOURCE_SERVER or SOURCE_BOTH

        fun normalizeString(str: String?): String {
            if (str == null) return ""
            var s = str.trim().lowercase(java.util.Locale.ROOT)
            s = s.replace(Regex("""\s*[\(\[][^\)\]]*(?:feat|ft\.|remaster|deluxe|version|edition|lyrics|explicit|clean|official|video|audio|www\.)[^\)\]]*[\)\]]""", RegexOption.IGNORE_CASE), "")
            s = s.replace(Regex("""[^\p{L}\p{N}]+"""), " ").trim()
            return s
        }

        fun createFingerprint(title: String?, artist: String?): String {
            val t = normalizeString(title)
            val a = normalizeString(artist)
            return "$a|||$t"
        }
    }
}
