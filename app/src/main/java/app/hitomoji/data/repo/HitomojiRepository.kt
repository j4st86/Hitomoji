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
import app.hitomoji.domain.widgetSelection
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

    suspend fun ensureSeeded() {
        gate.withLock {
            val raw = context.assets.open("kanji.json").bufferedReader().use { it.readText() }
            seeder.seed(raw)
        }
    }

    suspend fun homeSnapshot(today: LocalDate): HomeSnapshot {
        ensureSeeded()
        val newRank = resolveToday(today)
        val queue = queue(today, newRank)
        val fresh = queue.firstOrNull()?.let { it.state == CardState.NEW && it.dueDate == null } == true
        return HomeSnapshot(
            newCount = if (fresh) 1 else 0,
            reviewCount = queue.count { !(it.state == CardState.NEW && it.dueDate == null) },
            startRank = queue.firstOrNull()?.rank,
        )
    }

    suspend fun session(today: LocalDate, startRank: Int): SessionSnapshot {
        ensureSeeded()
        val newRank = resolveToday(today)
        val queue = queue(today, newRank)
        val cards = queue.mapNotNull { progress ->
            val kanji = database.kanjiDao().get(progress.rank) ?: return@mapNotNull null
            kanji.toStudyCard()
        }
        val index = cards.indexOfFirst { it.rank == startRank }.let { if (it >= 0) it else 0 }
        return SessionSnapshot(cards = cards, startIndex = index)
    }

    suspend fun grade(rank: Int, grade: Grade, today: LocalDate, reviewedAt: Instant) {
        ensureSeeded()
        val current = database.progressDao().get(rank) ?: return
        val updated = applyGrade(current.toCard(), grade, today)
        database.progressDao().saveReview(
            rank = rank,
            state = updated.state.name,
            ease = updated.ease,
            intervalDays = updated.intervalDays,
            repetitions = updated.repetitions,
            lapses = updated.lapses,
            dueDate = updated.dueDate?.toString(),
            lastReviewedAt = reviewedAt.toEpochMilli(),
        )
    }

    suspend fun catalog(): List<CatalogRow> {
        ensureSeeded()
        val progress = database.progressDao().all().associateBy { it.kanjiRank }
        return database.kanjiDao().all().map { kanji ->
            val state = progress[kanji.rank]?.state?.let { runCatching { CardState.valueOf(it) }.getOrNull() }
                ?: CardState.NEW
            CatalogRow(
                rank = kanji.rank,
                character = kanji.character,
                meaningRu = kanji.meaningsRu.firstOrNull().orEmpty(),
                meaningEn = kanji.meaningsEn.firstOrNull().orEmpty(),
                state = state,
            )
        }
    }

    suspend fun detail(rank: Int): KanjiDetail? {
        ensureSeeded()
        val kanji = database.kanjiDao().get(rank) ?: return null
        val progress = database.progressDao().get(rank)
        val state = progress?.state?.let { runCatching { CardState.valueOf(it) }.getOrNull() } ?: CardState.NEW
        return KanjiDetail(card = kanji.toStudyCard(), state = state)
    }

    suspend fun widgetModel(today: LocalDate): WidgetModel {
        ensureSeeded()
        val newRank = resolveToday(today)
        val queue = queue(today, newRank)
        val stored = dailyStore.read()
        val selection = widgetSelection(queue, stored.lastShownRank)
        return selection.toModel()
    }

    suspend fun setLastShown(rank: Int) {
        dailyStore.writeLastShown(rank)
    }

    fun dictionaryLanguage(fallback: String): Flow<String> = dailyStore.dictionaryLanguage(fallback)

    suspend fun setDictionaryLanguage(language: String) {
        dailyStore.writeDictionaryLanguage(language)
    }

    private suspend fun resolveToday(today: LocalDate): Int? {
        return gate.withLock {
            val stored = dailyStore.read()
            val progress = database.progressDao().all()
            val introduced = progress.mapNotNull { row ->
                row.introducedOn?.let { date -> row.kanjiRank to LocalDate.parse(date) }
            }.toMap()
            val decision = chooseDailyNew(
                today = today,
                storedDate = stored.date,
                storedRank = stored.rank,
                introducedOn = introduced,
                catalogRanks = progress.map { it.kanjiRank }.toSet(),
            )
            if (decision.markIntroduced && decision.rank != null) {
                database.progressDao().markIntroducedIfEmpty(decision.rank, today.toString())
            } else if (decision.rank != null) {
                database.progressDao().markIntroducedIfEmpty(decision.rank, today.toString())
            }
            if (stored.date != decision.persistDate || stored.rank != decision.persistRank) {
                dailyStore.writeDaily(decision.persistDate, decision.persistRank)
            }
            decision.rank
        }
    }

    private suspend fun queue(today: LocalDate, newRank: Int?): List<SrsCard> {
        val cards = database.progressDao().all().map { it.toCard() }
        return buildSession(today, cards, newRank)
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

private fun ProgressEntity.toCard(): SrsCard = SrsCard(
    rank = kanjiRank,
    state = runCatching { CardState.valueOf(state) }.getOrDefault(CardState.NEW),
    ease = ease,
    intervalDays = intervalDays,
    repetitions = repetitions,
    lapses = lapses,
    dueDate = dueDate?.let { LocalDate.parse(it) },
    introducedOn = introducedOn?.let { LocalDate.parse(it) },
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
