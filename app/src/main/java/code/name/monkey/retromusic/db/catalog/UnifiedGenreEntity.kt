package code.name.monkey.retromusic.db.catalog

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import code.name.monkey.retromusic.model.Genre

@Entity(
    tableName = "unified_genres",
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class UnifiedGenreEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "song_count")
    val songCount: Int = 0
) {
    fun toGenre(): Genre {
        return Genre(id = id, name = name, songCount = songCount)
    }
}
