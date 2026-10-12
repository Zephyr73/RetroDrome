package code.name.monkey.retromusic.network.subsonic.model

import com.google.gson.annotations.SerializedName

data class SubsonicGenre(
    @SerializedName("value") val value: String,
    @SerializedName("songCount") val songCount: Int = 0,
    @SerializedName("albumCount") val albumCount: Int = 0
)

data class SubsonicGenres(
    @SerializedName("genre") val genre: List<SubsonicGenre>? = null
)
