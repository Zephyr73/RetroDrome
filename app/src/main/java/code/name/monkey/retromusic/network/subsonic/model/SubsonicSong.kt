package code.name.monkey.retromusic.network.subsonic.model

import com.google.gson.annotations.SerializedName

data class SubsonicSong(
    @SerializedName("id") val id: String,
    @SerializedName("parent") val parent: String? = null,
    @SerializedName("title") val title: String,
    @SerializedName("album") val album: String? = null,
    @SerializedName("albumId") val albumId: String? = null,
    @SerializedName("artist") val artist: String? = null,
    @SerializedName("artistId") val artistId: String? = null,
    @SerializedName("track") val track: Int? = null,
    @SerializedName("discNumber") val discNumber: Int? = null,
    @SerializedName("year") val year: Int? = null,
    @SerializedName("genre") val genre: String? = null,
    @SerializedName("coverArt") val coverArt: String? = null,
    @SerializedName("size") val size: Long? = null,
    @SerializedName("contentType") val contentType: String? = null,
    @SerializedName("suffix") val suffix: String? = null,
    @SerializedName("duration") val duration: Int = 0,
    @SerializedName("bitRate") val bitRate: Int? = null,
    @SerializedName("bitrate") val bitrateAlt: Int? = null,
    @SerializedName("bitDepth") val bitDepth: Int? = null,
    @SerializedName("samplingRate") val samplingRate: Int? = null,
    @SerializedName("sampleRate") val sampleRateAlt: Int? = null,
    @SerializedName("channelCount") val channelCount: Int? = null,
    @SerializedName("path") val path: String? = null,
    @SerializedName("starred") val starred: String? = null,
    @SerializedName("playCount") val playCount: Long? = null,
    @SerializedName("created") val created: String? = null
) {
    val effectiveBitRate: Int?
        get() = bitRate ?: bitrateAlt

    val effectiveSampleRate: Int?
        get() = samplingRate ?: sampleRateAlt
}
