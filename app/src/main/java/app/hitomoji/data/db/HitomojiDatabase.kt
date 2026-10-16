package app.hitomoji.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [KanjiEntity::class, ProgressEntity::class],
    version = 1,
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
            ).build()
        }
    }
}
