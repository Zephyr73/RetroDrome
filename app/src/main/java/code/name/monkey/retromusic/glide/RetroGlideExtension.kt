package code.name.monkey.retromusic.glide

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import code.name.monkey.appthemehelper.util.TintHelper
import code.name.monkey.retromusic.App.Companion.getContext
import code.name.monkey.retromusic.Constants.USER_BANNER
import code.name.monkey.retromusic.Constants.USER_PROFILE
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.extensions.accentColor
import code.name.monkey.retromusic.glide.artistimage.ArtistImage
import code.name.monkey.retromusic.glide.audiocover.AudioFileCover
import code.name.monkey.retromusic.glide.palette.BitmapPaletteWrapper
import code.name.monkey.retromusic.model.Album
import code.name.monkey.retromusic.model.Artist
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.ArtistSignatureUtil
import code.name.monkey.retromusic.util.CustomArtistImageUtil.Companion.getFile
import code.name.monkey.retromusic.util.CustomArtistImageUtil.Companion.getInstance
import code.name.monkey.retromusic.util.MusicUtil
import code.name.monkey.retromusic.util.MusicUtil.getMediaStoreAlbumCoverUri
import code.name.monkey.retromusic.util.PreferenceUtil
import com.bumptech.glide.GenericTransitionOptions
import com.bumptech.glide.Priority
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Key
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.target.Target.SIZE_ORIGINAL
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.bumptech.glide.request.transition.Transition
import com.bumptech.glide.signature.MediaStoreSignature
import java.io.File


object RetroGlideExtension {

    private val DEFAULT_ARTIST_IMAGE
        get() = R.drawable.default_artist_art
    private val DEFAULT_SONG_IMAGE: Int
        get() = R.drawable.default_audio_art
    private val DEFAULT_ALBUM_IMAGE
        get() = R.drawable.default_album_art
    private val DEFAULT_ERROR_IMAGE_BANNER
        get() = R.drawable.material_design_default

    private val DEFAULT_DISK_CACHE_STRATEGY_ARTIST = DiskCacheStrategy.RESOURCE
    private val DEFAULT_DISK_CACHE_STRATEGY = DiskCacheStrategy.NONE

    private const val DEFAULT_ANIMATION = android.R.anim.fade_in

    fun RequestManager.asBitmapPalette(): RequestBuilder<BitmapPaletteWrapper> {
        return this.`as`(BitmapPaletteWrapper::class.java)
    }

    private fun getSongModel(song: Song, ignoreMediaStore: Boolean): Any {
        if (code.name.monkey.retromusic.network.subsonic.SubsonicMapper.isSubsonicId(song.id)) {
            val subsonicAlbumId = code.name.monkey.retromusic.network.subsonic.SubsonicMapper.toSubsonicId(song.albumId)
                ?: code.name.monkey.retromusic.network.subsonic.SubsonicMapper.toSubsonicId(song.id) ?: ""
            return code.name.monkey.retromusic.network.subsonic.SubsonicClientFactory.buildCoverArtUrl(subsonicAlbumId)
        }
        return if (ignoreMediaStore || MusicUtil.isAlbumNameUnknown(song.albumName, song.artistName)) {
            AudioFileCover(song.data)
        } else {
            getMediaStoreAlbumCoverUri(song.albumId)
        }
    }

    fun getSongModel(song: Song): Any {
        return getSongModel(song, PreferenceUtil.isIgnoreMediaStoreArtwork)
    }

    fun getAlbumModel(album: Album): Any? {
        if (code.name.monkey.retromusic.network.subsonic.SubsonicMapper.isSubsonicId(album.id)) {
            val subsonicAlbumId = code.name.monkey.retromusic.network.subsonic.SubsonicMapper.toSubsonicId(album.id) ?: ""
            return code.name.monkey.retromusic.network.subsonic.SubsonicClientFactory.buildCoverArtUrl(subsonicAlbumId)
        }
        if (MusicUtil.isAlbumNameUnknown(album.title, album.artistName)) {
            return null
        }
        val localSong = album.songs.firstOrNull { !code.name.monkey.retromusic.network.subsonic.SubsonicMapper.isSubsonicId(it.id) }
        return getSongModel(localSong ?: album.safeGetFirstSong())
    }

    fun getArtistModel(artist: Artist): Any {
        val hasCustomImage = getInstance(getContext()).hasCustomArtistImage(artist)
        if (hasCustomImage) {
            return getFile(artist)
        }
        if (code.name.monkey.retromusic.network.subsonic.SubsonicMapper.isSubsonicId(artist.id)) {
            val subsonicArtistId = code.name.monkey.retromusic.network.subsonic.SubsonicMapper.toSubsonicId(artist.id) ?: ""
            return code.name.monkey.retromusic.network.subsonic.SubsonicClientFactory.buildCoverArtUrl(subsonicArtistId)
        }
        return getArtistModel(
            artist,
            false,
            false
        )
    }

    fun getArtistModel(artist: Artist, forceDownload: Boolean): Any {
        val hasCustomImage = getInstance(getContext()).hasCustomArtistImage(artist)
        if (hasCustomImage) {
            return getFile(artist)
        }
        if (code.name.monkey.retromusic.network.subsonic.SubsonicMapper.isSubsonicId(artist.id)) {
            val subsonicArtistId = code.name.monkey.retromusic.network.subsonic.SubsonicMapper.toSubsonicId(artist.id) ?: ""
            return code.name.monkey.retromusic.network.subsonic.SubsonicClientFactory.buildCoverArtUrl(subsonicArtistId)
        }
        return getArtistModel(
            artist,
            false,
            forceDownload
        )
    }

    private fun getArtistModel(
        artist: Artist,
        hasCustomImage: Boolean,
        forceDownload: Boolean
    ): Any {
        return if (!hasCustomImage) {
            ArtistImage(artist)
        } else {
            getFile(artist)
        }
    }

    fun <T> RequestBuilder<T>.artistImageOptions(
        artist: Artist
    ): RequestBuilder<T> {
        return diskCacheStrategy(DEFAULT_DISK_CACHE_STRATEGY_ARTIST)
            .priority(Priority.LOW)
            .error(getDrawable(DEFAULT_ARTIST_IMAGE))
            .placeholder(getDrawable(DEFAULT_ARTIST_IMAGE))
            .override(SIZE_ORIGINAL, SIZE_ORIGINAL)
            .signature(createSignature(artist))
    }

    fun <T> RequestBuilder<T>.songCoverOptions(
        song: Song
    ): RequestBuilder<T> {
        val strategy = if (code.name.monkey.retromusic.network.subsonic.SubsonicMapper.isSubsonicId(song.id)) {
            DiskCacheStrategy.AUTOMATIC
        } else {
            DEFAULT_DISK_CACHE_STRATEGY
        }
        return diskCacheStrategy(strategy)
            .error(getDrawable(DEFAULT_SONG_IMAGE))
            .placeholder(getDrawable(DEFAULT_SONG_IMAGE))
            .signature(createSignature(song))
    }

    fun <T> RequestBuilder<T>.simpleSongCoverOptions(
        song: Song
    ): RequestBuilder<T> {
        return diskCacheStrategy(DEFAULT_DISK_CACHE_STRATEGY)
            .signature(createSignature(song))
    }

    fun <T> RequestBuilder<T>.albumCoverOptions(
        song: Song
    ): RequestBuilder<T> {
        val strategy = if (code.name.monkey.retromusic.network.subsonic.SubsonicMapper.isSubsonicId(song.id)) {
            DiskCacheStrategy.AUTOMATIC
        } else {
            DEFAULT_DISK_CACHE_STRATEGY
        }
        return diskCacheStrategy(strategy)
            .error(ContextCompat.getDrawable(getContext(), DEFAULT_ALBUM_IMAGE))
            .placeholder(ContextCompat.getDrawable(getContext(), DEFAULT_ALBUM_IMAGE))
            .signature(createSignature(song))
    }

    fun <T> RequestBuilder<T>.userProfileOptions(
        file: File,
        context: Context
    ): RequestBuilder<T> {
        return diskCacheStrategy(DEFAULT_DISK_CACHE_STRATEGY)
            .error(getErrorUserProfile(context))
            .signature(createSignature(file))
    }

    fun <T> RequestBuilder<T>.profileBannerOptions(
        file: File
    ): RequestBuilder<T> {
        return diskCacheStrategy(DEFAULT_DISK_CACHE_STRATEGY)
            .placeholder(DEFAULT_ERROR_IMAGE_BANNER)
            .error(DEFAULT_ERROR_IMAGE_BANNER)
            .signature(createSignature(file))
    }

    fun <T> RequestBuilder<T>.playlistOptions(): RequestBuilder<T> {
        return diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .placeholder(getDrawable(DEFAULT_ALBUM_IMAGE))
            .error(getDrawable(DEFAULT_ALBUM_IMAGE))
    }

    private fun createSignature(song: Song): Key {
        return MediaStoreSignature("", song.dateModified, 0)
    }

    private fun createSignature(file: File): Key {
        return MediaStoreSignature("", file.lastModified(), 0)
    }

    private fun createSignature(artist: Artist): Key {
        return ArtistSignatureUtil.getInstance(getContext())
            .getArtistSignature(artist.name)
    }

    fun getUserModel(): File {
        val dir = getContext().filesDir
        return File(dir, USER_PROFILE)
    }

    fun getBannerModel(): File {
        val dir = getContext().filesDir
        return File(dir, USER_BANNER)
    }

    private fun getErrorUserProfile(context: Context): Drawable {
        return TintHelper.createTintedDrawable(
            context,
            R.drawable.ic_account,
            context.accentColor()
        )
    }

    fun <TranscodeType> getDefaultTransition(): GenericTransitionOptions<TranscodeType> {
        return GenericTransitionOptions<TranscodeType>().transition(DEFAULT_ANIMATION)
    }

    fun getDrawable(@DrawableRes id: Int): Drawable? {
        return ContextCompat.getDrawable(getContext(), id)
    }
}

// https://github.com/bumptech/glide/issues/527#issuecomment-148840717
fun RequestBuilder<Drawable>.crossfadeListener(): RequestBuilder<Drawable> {
    return listener(object : RequestListener<Drawable> {
        override fun onLoadFailed(
            e: GlideException?,
            model: Any?,
            target: Target<Drawable>?,
            isFirstResource: Boolean
        ): Boolean {
            return false
        }

        override fun onResourceReady(
            resource: Drawable?,
            model: Any?,
            target: Target<Drawable>?,
            dataSource: DataSource?,
            isFirstResource: Boolean
        ): Boolean {
            return if (isFirstResource) {
                false // thumbnail was not shown, do as usual
            } else DrawableCrossFadeFactory.Builder()
                .setCrossFadeEnabled(true).build()
                .build(dataSource, isFirstResource)
                .transition(resource, target as Transition.ViewAdapter)
        }
    })
}