package code.name.monkey.retromusic.network.subsonic.model

import com.google.gson.annotations.SerializedName

data class SubsonicError(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String
)
