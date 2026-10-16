package app.hitomoji.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kanji")
data class KanjiEntity(
    @PrimaryKey val rank: Int,
    val character: String,
    val meaningsRu: List<String>,
    val meaningsEn: List<String>,
    val onReadings: List<String>,
    val kunReadings: List<String>,
    val strokeCount: Int,
    val sentenceJp: String,
    val sentenceKana: String,
    val sentenceRu: String,
    val sentenceEn: String,
    val reviewNeeded: Boolean,
)

@Entity(tableName = "dictionary_meta")
data class DictionaryMetaEntity(
    @PrimaryKey val id: Int = 1,
    val contentHash: String,
)

@Entity(tableName = "progress")
data class ProgressEntity(
    @PrimaryKey val kanjiRank: Int,
    val state: String,
    val ease: Double,
    val intervalDays: Int,
    val repetitions: Int,
    val lapses: Int,
    val dueDate: String?,
    val introducedOn: String?,
    val lastReviewedAt: Long?,
)
