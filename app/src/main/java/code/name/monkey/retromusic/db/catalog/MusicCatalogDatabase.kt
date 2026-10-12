package code.name.monkey.retromusic.db.catalog

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        UnifiedSongEntity::class,
        UnifiedAlbumEntity::class,
        UnifiedArtistEntity::class,
        UnifiedGenreEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MusicCatalogDatabase : RoomDatabase() {
    abstract fun unifiedMusicDao(): UnifiedMusicDao
}
