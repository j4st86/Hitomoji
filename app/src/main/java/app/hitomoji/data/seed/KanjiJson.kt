package app.hitomoji.data.seed

import app.hitomoji.data.db.KanjiEntity
import app.hitomoji.data.db.ProgressEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class KanjiJson(
    val rank: Int,
    val character: String,
    val meaningsRu: List<String>,
    val onReadings: List<String>,
    val kunReadings: List<String>,
    val strokeCount: Int,
    val sentenceJp: String,
    val sentenceKana: String,
    val sentenceRu: String,
    val reviewNeeded: Boolean = false,
)

object KanjiJsonParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(raw: String): List<KanjiJson> = json.decodeFromString(raw)
}

fun KanjiJson.toEntity(): KanjiEntity = KanjiEntity(
    rank = rank,
    character = character,
    meaningsRu = meaningsRu,
    onReadings = onReadings,
    kunReadings = kunReadings,
    strokeCount = strokeCount,
    sentenceJp = sentenceJp,
    sentenceKana = sentenceKana,
    sentenceRu = sentenceRu,
    reviewNeeded = reviewNeeded,
)

fun KanjiJson.toFreshProgress(): ProgressEntity = ProgressEntity(
    kanjiRank = rank,
    state = "NEW",
    ease = 2.5,
    intervalDays = 0,
    repetitions = 0,
    lapses = 0,
    dueDate = null,
    introducedOn = null,
    lastReviewedAt = null,
)
