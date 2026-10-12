package code.name.monkey.retromusic.network.subsonic.model

import com.google.gson.annotations.SerializedName

data class SubsonicPlaylist(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("comment") val comment: String? = null,
    @SerializedName("owner") val owner: String? = null,
    @SerializedName("public") val isPublic: Boolean = false,
    @SerializedName("songCount") val songCount: Int = 0,
    @SerializedName("duration") val duration: Int = 0,
    @SerializedName("created") val created: String? = null,
    @SerializedName("coverArt") val coverArt: String? = null,
    @SerializedName("entry") val entry: List<SubsonicSong>? = null
)

data class SubsonicPlaylists(
    @SerializedName("playlist") val playlist: List<SubsonicPlaylist>? = null
)
