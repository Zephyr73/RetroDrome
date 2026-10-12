package code.name.monkey.retromusic.network.subsonic.model

import com.google.gson.annotations.SerializedName

data class SubsonicArtist(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("coverArt") val coverArt: String? = null,
    @SerializedName("artistImageUrl") val artistImageUrl: String? = null,
    @SerializedName("albumCount") val albumCount: Int = 0,
    @SerializedName("starred") val starred: String? = null,
    @SerializedName("album") val albums: List<SubsonicAlbum>? = null
)

data class SubsonicArtistsIndex(
    @SerializedName("name") val name: String,
    @SerializedName("artist") val artist: List<SubsonicArtist>? = null
)

data class SubsonicArtistsRoot(
    @SerializedName("ignoredArticles") val ignoredArticles: String? = null,
    @SerializedName("index") val index: List<SubsonicArtistsIndex>? = null
)
