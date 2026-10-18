package app.hitomoji.data.repo

import android.content.Context
import app.hitomoji.data.db.KanjiEntity
import app.hitomoji.data.db.ProgressEntity
import app.hitomoji.data.prefs.DailyStore
import app.hitomoji.data.seed.DictionarySeeder
import app.hitomoji.data.db.HitomojiDatabase
import app.hitomoji.domain.CardState
import app.hitomoji.domain.Grade
import app.hitomoji.domain.SrsCard
import app.hitomoji.domain.WidgetMode
import app.hitomoji.domain.WidgetSelection
import app.hitomoji.domain.applyGrade
import app.hitomoji.domain.buildSession
import app.hitomoji.domain.chooseDailyNew
import app.hitomoji.domain.pickGradedWidget
import app.hitomoji.domain.withFsrsMemory
import java.time.Instant
import java.time.LocalDate
import app.hitomoji.data.db.CatalogSlice
import app.hitomoji.data.db.DictionaryMetaEntity
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class HomeSnapshot(
    val newCount: Int,
    val reviewCount: Int,
    val startRank: Int?,
)

data class StudyCard(
    val rank: Int,
    val character: String,
    val sentenceJp: String,
    val sentenceKana: String,
    val sentenceRu: String,
    val sentenceEn: String,
    val meaningsRu: List<String>,
    val meaningsEn: List<String>,
    val onReadings: List<String>,
    val kunReadings: List<String>,
)

data class SessionSnapshot(
    val cards: List<StudyCard>,
    val startIndex: Int,
)

data class CatalogRow(
    val rank: Int,
    val character: String,
    val meaningRu: String,
    val meaningEn: String,
    val state: CardState,
)

data class KanjiDetail(
    val card: StudyCard,
    val state: CardState,
)

data class WidgetModel(
    val rank: Int?,
    val character: String,
    val sentenceJp: String,
    val mode: WidgetMode,
)

class HitomojiRepository(
    private val context: Context,
    private val database: HitomojiDatabase,
    private val dailyStore: DailyStore,
    private val seeder: DictionarySeeder,
) {
    private val gate = Mutex()
    private val widgetGate = Mutex()

    @Volatile
    private var appliedHash: String? = null
    private var cachedWidget: Pair<LocalDate, WidgetModel>? = null

    suspend fun ensureSeeded() {
        if (appliedHash != null) return
        gate.withLock {
            if (appliedHash != null) return@withLock
            withContext(Dispatchers.IO) {
                val raw = context.assets.open("kanji.json").bufferedReader().use { it.readText() }
                val hash = sha256(raw)
                val stored = database.dictionaryMetaDao().contentHash()
                if (stored == hash && database.kanjiDao().count() > 0) {
                    appliedHash = hash
                    return@withContext
                }
                seeder.seed(raw)
                database.dictionaryMetaDao().upsert(DictionaryMetaEntity(contentHash = hash))
                appliedHash = hash
            }
        }
    }

    suspend fun homeSnapshot(today: LocalDate): HomeSnapshot = withContext(Dispatchers.IO) {
        ensureSeeded()
        val queue = buildSession(today, resolveToday(today))
        val freshCount = queue.count { it.state == CardState.NEW && it.dueDate == null }
        HomeSnapshot(
            newCount = freshCount,
            reviewCount = queue.size - freshCount,
            startRank = queue.firstOrNull()?.rank,
        )
    }

    suspend fun session(today: LocalDate, startRank: Int): SessionSnapshot = withContext(Dispatchers.IO) {
        ensureSeeded()
        val cards = resolveToday(today)
        val queue = buildSession(today, cards)
        val ordered = if (queue.any { it.rank == startRank }) {
            queue
        } else {
            val extra = cards.firstOrNull { it.rank == startRank }
            if (extra == null) queue else listOf(extra) + queue
        }
        val kanjiByRank = if (ordered.isEmpty()) {
            emptyMap()
        } else {
            database.kanjiDao().getByRanks(ordered.map { it.rank }).associateBy { it.rank }
        }
        val studyCards = ordered.mapNotNull { progress -> kanjiByRank[progress.rank]?.toStudyCard() }
        val index = studyCards.indexOfFirst { it.rank == startRank }.let { if (it >= 0) it else 0 }
        SessionSnapshot(cards = studyCards, startIndex = index)
    }

    suspend fun grade(rank: Int, grade: Grade, today: LocalDate, reviewedAt: Instant) = withContext(Dispatchers.IO) {
        ensureSeeded()
        val current = database.progressDao().get(rank) ?: return@withContext
        val updated = applyGrade(current.toCard(), grade, today)
        widgetGate.withLock {
            database.progressDao().saveReview(
                rank = rank,
                state = updated.state.name,
                ease = updated.ease,
                intervalDays = updated.intervalDays,
                repetitions = updated.repetitions,
                lapses = updated.lapses,
                dueDate = updated.dueDate?.toString(),
                lastReviewedAt = reviewedAt.toEpochMilli(),
                stability = updated.stability,
                difficulty = updated.difficulty,
            )
            cachedWidget = null
        }
    }

    suspend fun catalogPage(offset: Int, limit: Int): List<CatalogRow> = withContext(Dispatchers.IO) {
        if (offset == 0) ensureSeeded()
        database.kanjiDao().catalogPage(limit, offset).map { it.toCatalogRow() }
    }

    suspend fun detail(rank: Int): KanjiDetail? = withContext(Dispatchers.IO) {
        ensureSeeded()
        val kanji = database.kanjiDao().get(rank) ?: return@withContext null
        val progress = database.progressDao().get(rank)
        val state = progress?.state?.let { runCatching { CardState.valueOf(it) }.getOrNull() } ?: CardState.NEW
        KanjiDetail(card = kanji.toStudyCard(), state = state)
    }

    suspend fun widgetModel(today: LocalDate): WidgetModel = withContext(Dispatchers.IO) {
        ensureSeeded()
        widgetGate.withLock {
            cachedWidget?.let { (day, model) ->
                if (day == today) return@withLock model
            }
            val model = pickGradedWidget(loadCards(), today).toModel()
            cachedWidget = today to model
            model
        }
    }

    fun dictionaryLanguage(fallback: String): Flow<String> = dailyStore.dictionaryLanguage(fallback)

    suspend fun setDictionaryLanguage(language: String) {
        dailyStore.writeDictionaryLanguage(language)
    }

    private suspend fun resolveToday(today: LocalDate): List<SrsCard> {
        return gate.withLock {
            val cards = loadCards()
            val introduced = cards.mapNotNull { card ->
                card.introducedOn?.let { card.rank to it }
            }.toMap()
            val waiting = cards
                .filter { card ->
                    card.introducedOn != null &&
                        card.dueDate == null &&
                        card.state == CardState.NEW
                }
                .map { it.rank }
                .toSet()
            val decision = chooseDailyNew(
                today = today,
                introducedOn = introduced,
                waitingRanks = waiting,
                catalogRanks = cards.map { it.rank }.toSet(),
            )
            if (decision.introduce.isEmpty()) return@withLock cards
            val introducedNow = decision.introduce.toSet()
            for (rank in decision.introduce) {
                database.progressDao().markIntroducedIfEmpty(rank, today.toString())
            }
            cards.map { card ->
                if (card.rank in introducedNow && card.introducedOn == null) {
                    card.copy(introducedOn = today)
                } else {
                    card
                }
            }
        }
    }

    private suspend fun loadCards(): List<SrsCard> {
        return database.progressDao().all().map { it.toCard() }
    }

    private suspend fun WidgetSelection.toModel(): WidgetModel {
        val kanji = rank?.let { database.kanjiDao().get(it) }
        return WidgetModel(
            rank = kanji?.rank,
            character = kanji?.character.orEmpty(),
            sentenceJp = kanji?.sentenceJp.orEmpty(),
            mode = mode,
        )
    }
}

private fun sha256(raw: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { byte -> "%02x".format(byte) }
}

private fun ProgressEntity.toCard(): SrsCard = SrsCard(
    rank = kanjiRank,
    state = runCatching { CardState.valueOf(state) }.getOrDefault(CardState.NEW),
    ease = ease,
    intervalDays = intervalDays,
    repetitions = repetitions,
    lapses = lapses,
    dueDate = dueDate?.let { LocalDate.parse(it) },
    introducedOn = introducedOn?.let { LocalDate.parse(it) },
    stability = stability,
    difficulty = difficulty,
).withFsrsMemory()

private fun CatalogSlice.toCatalogRow(): CatalogRow = CatalogRow(
    rank = rank,
    character = character,
    meaningRu = meaningsRu.firstOrNull().orEmpty(),
    meaningEn = meaningsEn.firstOrNull().orEmpty(),
    state = state?.let { runCatching { CardState.valueOf(it) }.getOrNull() } ?: CardState.NEW,
)

private fun KanjiEntity.toStudyCard(): StudyCard = StudyCard(
    rank = rank,
    character = character,
    sentenceJp = sentenceJp,
    sentenceKana = sentenceKana,
    sentenceRu = sentenceRu,
    sentenceEn = sentenceEn,
    meaningsRu = meaningsRu,
    meaningsEn = meaningsEn,
    onReadings = onReadings,
    kunReadings = kunReadings,
)
