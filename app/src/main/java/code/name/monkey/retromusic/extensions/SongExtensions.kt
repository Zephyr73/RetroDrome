package code.name.monkey.retromusic.extensions

import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat.QueueItem
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.MusicUtil

import android.net.Uri
import code.name.monkey.retromusic.network.subsonic.SubsonicClientFactory
import code.name.monkey.retromusic.network.subsonic.SubsonicMapper

val Song.uri
    get() = if (data.startsWith("http://") || data.startsWith("https://")) {
        Uri.parse(data)
    } else if (SubsonicMapper.isSubsonicId(id)) {
        val subsonicId = SubsonicMapper.toSubsonicId(id) ?: ""
        Uri.parse(SubsonicClientFactory.buildStreamUrl(subsonicId))
    } else {
        MusicUtil.getSongFileUri(songId = id)
    }

val Song.albumArtUri
    get() = if (SubsonicMapper.isSubsonicId(id)) {
        val subsonicAlbumId = SubsonicMapper.toSubsonicId(albumId)
            ?: SubsonicMapper.toSubsonicId(id) ?: ""
        Uri.parse(SubsonicClientFactory.buildCoverArtUrl(subsonicAlbumId))
    } else if (MusicUtil.isAlbumNameUnknown(albumName, artistName)) {
        Uri.EMPTY
    } else {
        MusicUtil.getMediaStoreAlbumCoverUri(albumId)
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
