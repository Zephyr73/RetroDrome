package code.name.monkey.retromusic.db.catalog

import android.content.Context
import code.name.monkey.retromusic.db.catalog.UnifiedSongEntity.Companion.SOURCE_BOTH
import code.name.monkey.retromusic.db.catalog.UnifiedSongEntity.Companion.SOURCE_LOCAL
import code.name.monkey.retromusic.db.catalog.UnifiedSongEntity.Companion.SOURCE_SERVER
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.network.subsonic.SubsonicClientFactory
import code.name.monkey.retromusic.network.subsonic.SubsonicMapper
import code.name.monkey.retromusic.network.subsonic.SubsonicPreferences
import code.name.monkey.retromusic.repository.AlbumRepository
import code.name.monkey.retromusic.repository.ArtistRepository
import code.name.monkey.retromusic.repository.GenreRepository
import code.name.monkey.retromusic.repository.SongRepository
import code.name.monkey.retromusic.repository.SubsonicRepository
import code.name.monkey.retromusic.util.logD
import code.name.monkey.retromusic.util.logE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.abs

class MusicCatalogSyncEngine(
    private val context: Context,
    private val dao: UnifiedMusicDao,
    private val subsonicRepo: SubsonicRepository
) {

    private val syncMutex = Mutex()
    private val localAlbumIds = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val localArtistIds = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val subsonicAlbumIds = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val subsonicArtistIds = java.util.concurrent.ConcurrentHashMap<String, String>()

    @Volatile
    var isSyncing: Boolean = false
        private set

    suspend fun syncAll(
        songRepo: SongRepository,
        albumRepo: AlbumRepository,
        artistRepo: ArtistRepository,
        genreRepo: GenreRepository,
        forceServerSync: Boolean = false
    ) = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            isSyncing = true
            try {
                logD("MusicCatalogSyncEngine: Starting full sync...")
                syncLocalCatalog(songRepo, albumRepo, artistRepo, genreRepo)
                if (SubsonicPreferences.isConfigured || forceServerSync) {
                    syncNavidromeCatalog()
                }
                logD("MusicCatalogSyncEngine: Full sync completed successfully.")
            } catch (e: Exception) {
                logE("MusicCatalogSyncEngine: Full sync failed: ${e.message}")
            } finally {
                isSyncing = false
            }
        }
    }

    suspend fun syncLocalCatalog(
        songRepo: SongRepository,
        albumRepo: AlbumRepository,
        artistRepo: ArtistRepository,
        genreRepo: GenreRepository
    ) = withContext(Dispatchers.IO) {
        try {
            val localSongs = songRepo.songs()
            if (localSongs.isEmpty()) {
                logD("MusicCatalogSyncEngine: No local songs found in MediaStore.")
                return@withContext
            }

            logD("MusicCatalogSyncEngine: Syncing ${localSongs.size} local songs...")
            for (local in localSongs) {
                if (local.albumId > 0) {
                    val albumKey = UnifiedAlbumEntity.createAlbumKey(
                        local.albumName,
                        local.albumArtist?.takeIf { it.isNotBlank() } ?: local.artistName
                    )
                    localAlbumIds[albumKey] = local.albumId
                }
                if (local.artistId > 0) {
                    val artistKey = UnifiedArtistEntity.createArtistKey(
                        local.albumArtist?.takeIf { it.isNotBlank() } ?: local.artistName
                    )
                    localArtistIds[artistKey] = local.artistId
                }
            }

            try {
                for (album in albumRepo.albums()) {
                    if (album.id > 0) {
                        val key = UnifiedAlbumEntity.createAlbumKey(album.title, album.artistName)
                        localAlbumIds[key] = album.id
                    }
                }
                for (artist in artistRepo.artists()) {
                    if (artist.id > 0) {
                        val key = UnifiedArtistEntity.createArtistKey(artist.name)
                        localArtistIds[key] = artist.id
                    }
                }
            } catch (e: Exception) {
                logE("MusicCatalogSyncEngine: Error mapping local album/artist repos: ${e.message}")
            }

            val existingSongs = dao.getAllSongs().associateBy { it.fingerprint }

            val songsToSave = mutableListOf<UnifiedSongEntity>()
            for (local in localSongs) {
                val fp = UnifiedSongEntity.createFingerprint(local.title, local.artistName)
                val existing = existingSongs[fp]

                val source = if (existing != null && !existing.subsonicId.isNullOrBlank()) {
                    SOURCE_BOTH
                } else {
                    SOURCE_LOCAL
                }

                val entity = UnifiedSongEntity(
                    id = existing?.id ?: 0L,
                    title = local.title,
                    trackNumber = local.trackNumber,
                    year = local.year,
                    duration = local.duration,
                    localPath = local.data,
                    dateModified = local.dateModified,
                    albumId = existing?.albumId ?: 0L,
                    albumName = local.albumName,
                    artistId = existing?.artistId ?: 0L,
                    artistName = local.artistName,
                    composer = local.composer,
                    albumArtist = local.albumArtist,
                    localMediaStoreId = local.id,
                    subsonicId = existing?.subsonicId,
                    source = source,
                    fingerprint = fp
                )
                songsToSave.add(entity)
            }

            dao.insertSongs(songsToSave)
            rebuildAlbumsAndArtists()

            // Sync genres
            try {
                val localGenres = genreRepo.genres()
                val genreEntities = localGenres.map {
                    UnifiedGenreEntity(id = it.id, name = it.name, songCount = it.songCount)
                }
                dao.insertGenres(genreEntities)
            } catch (e: Exception) {
                logE("MusicCatalogSyncEngine: Error syncing genres: ${e.message}")
            }

        } catch (e: Exception) {
            logE("MusicCatalogSyncEngine: Error in syncLocalCatalog: ${e.message}")
        }
    }

    suspend fun syncNavidromeCatalog() = withContext(Dispatchers.IO) {
        if (!SubsonicPreferences.isConfigured) {
            logD("MusicCatalogSyncEngine: Navidrome not configured, skipping.")
            return@withContext
        }

        try {
            logD("MusicCatalogSyncEngine: Syncing Navidrome catalog...")
            val remoteAlbums = subsonicRepo.albums()
            if (remoteAlbums.isEmpty()) {
                logD("MusicCatalogSyncEngine: No albums returned from Navidrome.")
                return@withContext
            }

            val existingSongs = dao.getAllSongs().associateBy { it.fingerprint }
            val songsToUpsert = mutableListOf<UnifiedSongEntity>()

            for (albumSummary in remoteAlbums) {
                val fullAlbum = try {
                    subsonicRepo.album(albumSummary.id)
                } catch (e: Exception) {
                    null
                } ?: continue

                val albumKey = UnifiedAlbumEntity.createAlbumKey(
                    fullAlbum.title,
                    fullAlbum.artistName
                )
                val subAlbumIdStr = SubsonicMapper.toSubsonicId(fullAlbum.id)
                    ?: SubsonicMapper.toSubsonicId(albumSummary.id)
                if (!subAlbumIdStr.isNullOrBlank()) {
                    subsonicAlbumIds[albumKey] = subAlbumIdStr
                }
                val artistKey = UnifiedArtistEntity.createArtistKey(fullAlbum.artistName)
                val subArtistIdStr = SubsonicMapper.toSubsonicId(fullAlbum.artistId)
                if (!subArtistIdStr.isNullOrBlank()) {
                    subsonicArtistIds[artistKey] = subArtistIdStr
                }

                for (remoteSong in fullAlbum.songs) {
                    val fp = UnifiedSongEntity.createFingerprint(remoteSong.title, remoteSong.artistName)
                    val existing = existingSongs[fp]
                    val subId = SubsonicMapper.toSubsonicId(remoteSong.id) ?: "sub_${remoteSong.id}"

                    val isMatch = existing != null && (
                        existing.trackNumber == remoteSong.trackNumber ||
                        abs(existing.duration - remoteSong.duration) <= 4000L ||
                        existing.albumName.equals(remoteSong.albumName, ignoreCase = true)
                    )

                    if (isMatch && existing != null) {
                        // Unify existing local song with server song!
                        val updated = existing.copy(
                            subsonicId = subId,
                            source = SOURCE_BOTH
                        )
                        songsToUpsert.add(updated)
                    } else {
                        // Pure server song
                        val newSong = UnifiedSongEntity(
                            id = existing?.takeIf { it.source == SOURCE_SERVER }?.id ?: 0L,
                            title = remoteSong.title,
                            trackNumber = remoteSong.trackNumber,
                            year = remoteSong.year,
                            duration = remoteSong.duration,
                            localPath = null,
                            dateModified = remoteSong.dateModified,
                            albumId = 0L,
                            albumName = remoteSong.albumName,
                            artistId = 0L,
                            artistName = remoteSong.artistName,
                            composer = remoteSong.composer,
                            albumArtist = remoteSong.albumArtist,
                            localMediaStoreId = null,
                            subsonicId = subId,
                            source = SOURCE_SERVER,
                            fingerprint = fp
                        )
                        songsToUpsert.add(newSong)
                    }
                }
            }

            if (songsToUpsert.isNotEmpty()) {
                dao.insertSongs(songsToUpsert)
            }
            rebuildAlbumsAndArtists()
            logD("MusicCatalogSyncEngine: Navidrome catalog sync completed.")
        } catch (e: Exception) {
            logE("MusicCatalogSyncEngine: Error in syncNavidromeCatalog: ${e.message}")
        }
    }

    private suspend fun rebuildAlbumsAndArtists() {
        val allSongs = dao.getAllSongs()
        if (allSongs.isEmpty()) return

        val existingAlbums = dao.getAllAlbums().associateBy { it.albumKey }
        val existingArtists = dao.getAllArtists().associateBy { it.artistKey }

        val albumsToSave = mutableListOf<UnifiedAlbumEntity>()
        val songsGroupedByAlbum = allSongs.groupBy {
            UnifiedAlbumEntity.createAlbumKey(it.albumName, it.albumArtist?.takeIf { a -> a.isNotBlank() } ?: it.artistName)
        }

        for ((albumKey, songs) in songsGroupedByAlbum) {
            val sample = songs.first()
            val existing = existingAlbums[albumKey]

            val isLocal = songs.any { it.source == SOURCE_LOCAL || it.source == SOURCE_BOTH }
            val isServer = songs.any { it.source == SOURCE_SERVER || it.source == SOURCE_BOTH }
            val source = when {
                isLocal && isServer -> SOURCE_BOTH
                isServer -> SOURCE_SERVER
                else -> SOURCE_LOCAL
            }

            val resolvedLocalAlbumId = localAlbumIds[albumKey] ?: existing?.localAlbumId
            val resolvedSubsonicAlbumId = subsonicAlbumIds[albumKey] ?: existing?.subsonicAlbumId

            val coverArtUrl = resolvedSubsonicAlbumId?.let {
                SubsonicClientFactory.buildCoverArtUrl(it)
            } ?: songs.firstOrNull { !it.subsonicId.isNullOrBlank() }?.subsonicId?.let {
                SubsonicClientFactory.buildCoverArtUrl(it)
            } ?: existing?.coverArtUrl

            val albumEntity = UnifiedAlbumEntity(
                id = existing?.id ?: 0L,
                title = sample.albumName,
                artistId = 0L,
                artistName = sample.artistName,
                albumArtist = sample.albumArtist,
                year = sample.year,
                songCount = songs.size,
                dateModified = songs.maxOfOrNull { it.dateModified } ?: 0L,
                localAlbumId = resolvedLocalAlbumId,
                subsonicAlbumId = resolvedSubsonicAlbumId,
                coverArtUrl = coverArtUrl,
                source = source,
                albumKey = albumKey
            )
            albumsToSave.add(albumEntity)
        }

        dao.insertAlbums(albumsToSave)

        // Reload saved albums to have auto-generated IDs
        val savedAlbums = dao.getAllAlbums().associateBy { it.albumKey }

        // Update songs with correct albumId
        val updatedSongs = mutableListOf<UnifiedSongEntity>()
        for (song in allSongs) {
            val key = UnifiedAlbumEntity.createAlbumKey(song.albumName, song.albumArtist?.takeIf { a -> a.isNotBlank() } ?: song.artistName)
            val album = savedAlbums[key]
            if (album != null && song.albumId != album.id) {
                updatedSongs.add(song.copy(albumId = album.id))
            }
        }
        if (updatedSongs.isNotEmpty()) {
            dao.insertSongs(updatedSongs)
        }

        // Rebuild Artists
        val artistsToSave = mutableListOf<UnifiedArtistEntity>()
        val albumsGroupedByArtist = savedAlbums.values.groupBy {
            UnifiedArtistEntity.createArtistKey(it.albumArtist?.takeIf { a -> a.isNotBlank() } ?: it.artistName)
        }

        for ((artistKey, albums) in albumsGroupedByArtist) {
            val sample = albums.first()
            val existing = existingArtists[artistKey]

            val isLocal = albums.any { it.source == SOURCE_LOCAL || it.source == SOURCE_BOTH }
            val isServer = albums.any { it.source == SOURCE_SERVER || it.source == SOURCE_BOTH }
            val source = when {
                isLocal && isServer -> SOURCE_BOTH
                isServer -> SOURCE_SERVER
                else -> SOURCE_LOCAL
            }

            val totalSongs = albums.sumOf { it.songCount }
            val resolvedLocalArtistId = localArtistIds[artistKey] ?: existing?.localArtistId
            val resolvedSubsonicArtistId = subsonicArtistIds[artistKey] ?: existing?.subsonicArtistId

            val coverArtUrl = resolvedSubsonicArtistId?.let {
                SubsonicClientFactory.buildCoverArtUrl(it)
            } ?: albums.firstOrNull { !it.coverArtUrl.isNullOrBlank() }?.coverArtUrl

            val artistEntity = UnifiedArtistEntity(
                id = existing?.id ?: 0L,
                name = sample.artistName,
                songCount = totalSongs,
                albumCount = albums.size,
                localArtistId = resolvedLocalArtistId,
                subsonicArtistId = resolvedSubsonicArtistId,
                coverArtUrl = coverArtUrl,
                source = source,
                artistKey = artistKey
            )
            artistsToSave.add(artistEntity)
        }

        dao.insertArtists(artistsToSave)

        // Reload saved artists and update album artistId and song artistId
        val savedArtists = dao.getAllArtists().associateBy { it.artistKey }

        val albumsToUpdate = mutableListOf<UnifiedAlbumEntity>()
        for (album in savedAlbums.values) {
            val key = UnifiedArtistEntity.createArtistKey(album.albumArtist?.takeIf { a -> a.isNotBlank() } ?: album.artistName)
            val artist = savedArtists[key]
            if (artist != null && album.artistId != artist.id) {
                albumsToUpdate.add(album.copy(artistId = artist.id))
            }
        }
        if (albumsToUpdate.isNotEmpty()) {
            dao.insertAlbums(albumsToUpdate)
        }

        val songsArtistToUpdate = mutableListOf<UnifiedSongEntity>()
        for (song in dao.getAllSongs()) {
            val key = UnifiedArtistEntity.createArtistKey(song.albumArtist?.takeIf { a -> a.isNotBlank() } ?: song.artistName)
            val artist = savedArtists[key]
            if (artist != null && song.artistId != artist.id) {
                songsArtistToUpdate.add(song.copy(artistId = artist.id))
            }
        }
        if (songsArtistToUpdate.isNotEmpty()) {
            dao.insertSongs(songsArtistToUpdate)
        }
    }
}
