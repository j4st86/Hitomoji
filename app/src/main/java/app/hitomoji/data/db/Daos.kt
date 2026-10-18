package app.hitomoji.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KanjiDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<KanjiEntity>)

    @Query("SELECT * FROM kanji ORDER BY rank")
    fun observeAll(): Flow<List<KanjiEntity>>

    @Query("SELECT * FROM kanji WHERE `rank` = :rank")
    suspend fun get(rank: Int): KanjiEntity?

    @Query("SELECT * FROM kanji WHERE `rank` IN (:ranks)")
    suspend fun getByRanks(ranks: List<Int>): List<KanjiEntity>

    @Query("SELECT * FROM kanji ORDER BY rank")
    suspend fun all(): List<KanjiEntity>

    @Query(
        """
        SELECT kanji.`rank` AS `rank`,
               kanji.`character` AS `character`,
               kanji.meaningsRu AS meaningsRu,
               kanji.meaningsEn AS meaningsEn,
               progress.state AS state
        FROM kanji
        LEFT JOIN progress ON progress.kanjiRank = kanji.`rank`
        ORDER BY kanji.`rank`
        LIMIT :limit OFFSET :offset
        """,
    )
    suspend fun catalogPage(limit: Int, offset: Int): List<CatalogSlice>

    @Query("SELECT COUNT(*) FROM kanji")
    suspend fun count(): Int
}

data class CatalogSlice(
    val rank: Int,
    val character: String,
    val meaningsRu: List<String>,
    val meaningsEn: List<String>,
    val state: String?,
)

@Dao
interface ProgressDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(items: List<ProgressEntity>)

    @Query("SELECT * FROM progress WHERE kanjiRank = :rank")
    suspend fun get(rank: Int): ProgressEntity?

    @Query("SELECT * FROM progress")
    suspend fun all(): List<ProgressEntity>

    @Query("SELECT * FROM progress")
    fun observeAll(): Flow<List<ProgressEntity>>

    @Query(
        """
        UPDATE progress
        SET state = :state,
            ease = :ease,
            intervalDays = :intervalDays,
            repetitions = :repetitions,
            lapses = :lapses,
            dueDate = :dueDate,
            lastReviewedAt = :lastReviewedAt,
            stability = :stability,
            difficulty = :difficulty
        WHERE kanjiRank = :rank
        """,
    )
    suspend fun saveReview(
        rank: Int,
        state: String,
        ease: Double,
        intervalDays: Int,
        repetitions: Int,
        lapses: Int,
        dueDate: String?,
        lastReviewedAt: Long?,
        stability: Double,
        difficulty: Double,
    )

    @Query(
        """
        UPDATE progress
        SET introducedOn = :day
        WHERE kanjiRank = :rank AND introducedOn IS NULL
        """,
    )
    suspend fun markIntroducedIfEmpty(rank: Int, day: String)

    @Query("SELECT COUNT(*) FROM progress")
    suspend fun count(): Int
}

@Dao
interface DictionaryMetaDao {
    @Query("SELECT contentHash FROM dictionary_meta WHERE id = 1")
    suspend fun contentHash(): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: DictionaryMetaEntity)
}
