package code.name.monkey.retromusic.network.subsonic.model

import com.google.gson.annotations.SerializedName

data class SubsonicAlbum(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("artist") val artist: String? = null,
    @SerializedName("artistId") val artistId: String? = null,
    @SerializedName("coverArt") val coverArt: String? = null,
    @SerializedName("songCount") val songCount: Int = 0,
    @SerializedName("duration") val duration: Int = 0,
    @SerializedName("year") val year: Int? = null,
    @SerializedName("genre") val genre: String? = null,
    @SerializedName("starred") val starred: String? = null,
    @SerializedName("song") val songs: List<SubsonicSong>? = null
) {
    val displayTitle: String
        get() = name ?: title ?: ""
}

data class SubsonicAlbumList2(
    @SerializedName("album") val album: List<SubsonicAlbum>? = null
)
