package app.hitomoji.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [KanjiEntity::class, ProgressEntity::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class HitomojiDatabase : RoomDatabase() {
    abstract fun kanjiDao(): KanjiDao

    abstract fun progressDao(): ProgressDao

    companion object {
        fun build(context: Context): HitomojiDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                HitomojiDatabase::class.java,
                "hitomoji.db",
            ).addMigrations(MIGRATION_1_2).build()
        }
    }
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE kanji ADD COLUMN meaningsEn TEXT NOT NULL DEFAULT '[]'")
        db.execSQL("ALTER TABLE kanji ADD COLUMN sentenceEn TEXT NOT NULL DEFAULT ''")
    }
}
