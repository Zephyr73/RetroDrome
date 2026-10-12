package code.name.monkey.retromusic.extensions

import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.RetroUtil
import org.jaudiotagger.audio.AudioFileIO
import java.io.File
import java.net.URLEncoder

fun getSongInfo(song: Song): String {
    val file = File(song.data)
    if (file.exists()) {
        return try {
            val audioHeader = AudioFileIO.read(File(song.data)).audioHeader
            val string: StringBuilder = StringBuilder()
            val uriFile = file.toUri()
            string.append(getMimeType(uriFile.toString())).append(" • ")
            if (audioHeader.isLossless) {
                string.append(audioHeader.bitsPerSample).append("-bit").append(" • ")
            }
            string.append(audioHeader.bitRate).append(" kb/s").append(" • ")
            string.append(RetroUtil.frequencyCount(audioHeader.sampleRate.toInt()))
                .append(" kHz")
            string.toString()
        } catch (er: Exception) {
            " - "
        }
    } else if (code.name.monkey.retromusic.network.subsonic.SubsonicMapper.isSubsonicId(song.id)) {
        val subSong = code.name.monkey.retromusic.network.subsonic.SubsonicMapper.getCachedSong(song.id)
        if (subSong != null) {
            val parts = mutableListOf<String>()
            val format = (subSong.suffix ?: subSong.contentType?.substringAfter('/') ?: "").uppercase()
            if (format.isNotBlank()) parts.add(format)
            val bitRate = subSong.effectiveBitRate
            if (bitRate != null && bitRate > 0) parts.add("$bitRate kb/s")
            val sampleRate = subSong.effectiveSampleRate
            if (sampleRate != null && sampleRate > 0) {
                parts.add("${RetroUtil.frequencyCount(sampleRate)} kHz")
            }
            if (parts.isNotEmpty()) return parts.joinToString(" • ")
        }
    }
    return "-"
}

private fun getMimeType(url: String): String {
    var type: String? = MimeTypeMap.getFileExtensionFromUrl(
        URLEncoder.encode(url, "utf-8")
    ).uppercase()
    if (type == null) {
        type = url.substring(url.lastIndexOf(".") + 1)
    }
    return type
}