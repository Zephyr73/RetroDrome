package code.name.monkey.retromusic.network.subsonic.model

import com.google.gson.annotations.SerializedName

data class SubsonicResponseWrapper(
    @SerializedName("subsonic-response") val response: SubsonicResponse
)

data class SubsonicResponse(
    @SerializedName("status") val status: String,
    @SerializedName("version") val version: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("serverVersion") val serverVersion: String? = null,
    @SerializedName("openSubsonic") val openSubsonic: Boolean? = null,
    @SerializedName("error") val error: SubsonicError? = null,
    @SerializedName("artists") val artists: SubsonicArtistsRoot? = null,
    @SerializedName("artist") val artist: SubsonicArtist? = null,
    @SerializedName("album") val album: SubsonicAlbum? = null,
    @SerializedName("albumList2") val albumList2: SubsonicAlbumList2? = null,
    @SerializedName("searchResult3") val searchResult3: SubsonicSearchResult3? = null,
    @SerializedName("playlists") val playlists: SubsonicPlaylists? = null,
    @SerializedName("playlist") val playlist: SubsonicPlaylist? = null,
    @SerializedName("genres") val genres: SubsonicGenres? = null,
    @SerializedName("randomSongs") val randomSongs: SubsonicSongList? = null,
    @SerializedName("songsByGenre") val songsByGenre: SubsonicSongList? = null,
    @SerializedName("starred2") val starred2: SubsonicSearchResult3? = null,
    @SerializedName("song") val song: SubsonicSong? = null
) {
    val isOk: Boolean
        get() = status.equals("ok", ignoreCase = true)
}

data class SubsonicSongList(
    @SerializedName("song") val song: List<SubsonicSong>? = null
)
