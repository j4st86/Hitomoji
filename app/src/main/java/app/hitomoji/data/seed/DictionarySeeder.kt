package app.hitomoji.data.seed

import app.hitomoji.data.db.HitomojiDatabase

/**
 * Upserts dictionary text and inserts missing progress rows.
 * Existing progress is left untouched, including on a later launch.
 */
class DictionarySeeder(
    private val database: HitomojiDatabase,
) {
    suspend fun seed(rawJson: String) {
        val entries = KanjiJsonParser.parse(rawJson)
        database.kanjiDao().upsertAll(entries.map { it.toEntity() })
        database.progressDao().insertIgnore(entries.map { it.toFreshProgress() })
    }
}
