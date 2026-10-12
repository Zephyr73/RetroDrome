package code.name.monkey.retromusic.network.subsonic.model

import com.google.gson.annotations.SerializedName

data class SubsonicSearchResult3(
    @SerializedName("artist") val artists: List<SubsonicArtist>? = null,
    @SerializedName("album") val albums: List<SubsonicAlbum>? = null,
    @SerializedName("song") val songs: List<SubsonicSong>? = null
)
