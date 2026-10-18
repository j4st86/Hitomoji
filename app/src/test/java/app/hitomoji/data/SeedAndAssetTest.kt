package app.hitomoji.data

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.hitomoji.data.db.HitomojiDatabase
import app.hitomoji.data.prefs.DailyStore
import app.hitomoji.data.repo.HitomojiRepository
import app.hitomoji.data.seed.DictionarySeeder
import app.hitomoji.data.seed.KanjiJsonParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SeedAndAssetTest {
    @Test
    fun secondSeedKeepsEditedProgress() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, HitomojiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val seeder = DictionarySeeder(db)
        seeder.seed(sample("первый перевод"))
        db.progressDao().saveReview(
            rank = 1,
            state = "RELEARNING",
            ease = 1.4,
            intervalDays = 1,
            repetitions = 0,
            lapses = 3,
            dueDate = "2026-10-16",
            lastReviewedAt = 123L,
            stability = 0.0,
            difficulty = 0.0,
        )
        db.progressDao().markIntroducedIfEmpty(1, "2026-10-15")

        seeder.seed(sample("второй перевод"))

        val progress = db.progressDao().get(1)!!
        assertEquals("RELEARNING", progress.state)
        assertEquals(1.4, progress.ease, 0.001)
        assertEquals(1, progress.intervalDays)
        assertEquals(0, progress.repetitions)
        assertEquals(3, progress.lapses)
        assertEquals("2026-10-16", progress.dueDate)
        assertEquals("2026-10-15", progress.introducedOn)
        assertEquals(123L, progress.lastReviewedAt)
        assertEquals("второй перевод", db.kanjiDao().get(1)!!.sentenceRu)
        val untouched = db.progressDao().get(2)!!
        assertEquals("NEW", untouched.state)
        assertNull(untouched.dueDate)
        assertNull(untouched.introducedOn)
        db.close()
    }

    @Test
    fun secondEnsureSeededKeepsDatabaseEdits() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, HitomojiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repository = HitomojiRepository(
            context = context,
            database = db,
            dailyStore = DailyStore(context),
            seeder = DictionarySeeder(db),
        )
        repository.ensureSeeded()
        val edited = db.kanjiDao().get(1)!!.copy(sentenceRu = "правка в базе")
        db.kanjiDao().upsertAll(listOf(edited))
        db.progressDao().saveReview(
            rank = 1,
            state = "RELEARNING",
            ease = 1.4,
            intervalDays = 1,
            repetitions = 0,
            lapses = 2,
            dueDate = "2026-10-16",
            lastReviewedAt = 99L,
            stability = 0.0,
            difficulty = 0.0,
        )
        repository.ensureSeeded()
        assertEquals("правка в базе", db.kanjiDao().get(1)!!.sentenceRu)
        val progress = db.progressDao().get(1)!!
        assertEquals("RELEARNING", progress.state)
        assertEquals(1.4, progress.ease, 0.001)
        assertEquals(99L, progress.lastReviewedAt)
        assertTrue(db.dictionaryMetaDao().contentHash() != null)
        db.close()
    }

    @Test
    fun openingFromCatalogDoesNotWriteProgress() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, HitomojiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repository = HitomojiRepository(
            context = context,
            database = db,
            dailyStore = DailyStore(context),
            seeder = DictionarySeeder(db),
        )
        repository.ensureSeeded()
        val before = db.progressDao().all().associateBy { it.kanjiRank }
        val target = before.keys.first { rank -> before.getValue(rank).introducedOn == null && rank != 1 }
        val introducedBefore = before.getValue(target).introducedOn
        val dueBefore = before.getValue(target).dueDate
        repository.detail(target)
        var loaded = 0
        while (loaded < 500) {
            val page = repository.catalogPage(loaded, 40)
            assertTrue(page.isNotEmpty())
            loaded += page.size
        }
        assertEquals(500, loaded)
        assertTrue(repository.catalogPage(loaded, 40).isEmpty())
        val after = db.progressDao().get(target)!!
        assertEquals(introducedBefore, after.introducedOn)
        assertEquals(dueBefore, after.dueDate)
        assertEquals(before.getValue(target).state, after.state)
        assertEquals(before.getValue(target).ease, after.ease, 0.001)
        db.close()
    }

    @Test
    fun assetHasFiveHundredKanji() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val raw = context.assets.open("kanji.json").bufferedReader().use { it.readText() }
        val entries = KanjiJsonParser.parse(raw)
        assertEquals(500, entries.size)
        assertEquals((1..500).toList(), entries.map { it.rank }.sorted())
        assertEquals(500, entries.map { it.character }.toSet().size)
        entries.forEach { entry ->
            assertEquals(1, entry.character.length)
            assertTrue(entry.meaningsRu.isNotEmpty() && entry.meaningsRu.size <= 3)
            assertTrue(entry.meaningsRu.all { it.isNotBlank() })
            assertTrue(entry.meaningsEn.isNotEmpty() && entry.meaningsEn.size <= 3)
            assertTrue(entry.meaningsEn.all { it.isNotBlank() })
            assertTrue(entry.sentenceJp.isNotBlank())
            assertTrue(entry.sentenceJp.contains(entry.character))
            assertTrue(entry.sentenceKana.isNotBlank())
            assertTrue(entry.sentenceRu.isNotBlank())
            assertTrue(entry.sentenceEn.isNotBlank())
            assertTrue(entry.strokeCount > 0)
            assertTrue(entry.onReadings.all { it.isNotBlank() })
            assertTrue(entry.kunReadings.all { it.isNotBlank() })
        }
    }

    private fun sample(sentenceRu: String): String {
        fun row(rank: Int, character: String) = """
            {
              "rank": $rank,
              "character": "$character",
              "meaningsRu": ["значение"],
              "onReadings": ["オン"],
              "kunReadings": ["くん"],
              "strokeCount": 4,
              "sentenceJp": "${character}です。",
              "sentenceKana": "かなです。",
              "sentenceRu": "$sentenceRu"
            }
        """.trimIndent()
        return "[${row(1, "日")},${row(2, "一")}]"
    }
}
