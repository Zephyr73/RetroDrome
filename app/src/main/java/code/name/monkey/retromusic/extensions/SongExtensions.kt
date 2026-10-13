package code.name.monkey.retromusic.extensions

import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat.QueueItem
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.MusicUtil

import android.net.Uri
import code.name.monkey.retromusic.network.subsonic.SubsonicClientFactory
import code.name.monkey.retromusic.network.subsonic.SubsonicMapper

val Song.uri: Uri
    get() = if (data.startsWith("http://") || data.startsWith("https://")) {
        Uri.parse(data)
    } else {
        val effectiveLocalId = localMediaStoreId ?: SubsonicMapper.toLocalMediaStoreId(id)
        if (effectiveLocalId != null && effectiveLocalId > 0 && (data.isBlank() || java.io.File(data).exists())) {
            MusicUtil.getSongFileUri(songId = effectiveLocalId)
        } else if (SubsonicMapper.isSubsonicId(id)) {
            val subsonicId = SubsonicMapper.toSubsonicId(id) ?: ""
            Uri.parse(SubsonicClientFactory.buildStreamUrl(subsonicId))
        } else if (effectiveLocalId != null && effectiveLocalId > 0) {
            MusicUtil.getSongFileUri(songId = effectiveLocalId)
        } else if (data.isNotBlank() && java.io.File(data).exists()) {
            Uri.fromFile(java.io.File(data))
        } else {
            MusicUtil.getSongFileUri(songId = id)
        }
    }

val Song.albumArtUri: Uri
    get() = if (SubsonicMapper.isSubsonicId(id)) {
        val subsonicAlbumId = SubsonicMapper.toSubsonicId(albumId)
            ?: SubsonicMapper.toSubsonicId(id) ?: ""
        Uri.parse(SubsonicClientFactory.buildCoverArtUrl(subsonicAlbumId))
    } else if (MusicUtil.isAlbumNameUnknown(albumName, artistName)) {
        Uri.EMPTY
    } else {
        val effectiveAlbumId = SubsonicMapper.toLocalAlbumId(albumId) ?: albumId
        MusicUtil.getMediaStoreAlbumCoverUri(effectiveAlbumId)
    }

fun ArrayList<Song>.toMediaSessionQueue(): List<QueueItem> {
    return map { song ->
        val mediaDescription = MediaDescriptionCompat.Builder()
            .setMediaId(song.id.toString())
            .setTitle(song.title)
            .setSubtitle(song.artistName)
            .setIconUri(song.albumArtUri)
            .build()
        QueueItem(mediaDescription, song.hashCode().toLong())
    }
}
