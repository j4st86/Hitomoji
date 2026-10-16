package app.hitomoji.di

import android.content.Context
import app.hitomoji.data.db.HitomojiDatabase
import app.hitomoji.data.prefs.DailyStore
import app.hitomoji.data.repo.HitomojiRepository
import app.hitomoji.data.seed.DictionarySeeder

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val database: HitomojiDatabase = HitomojiDatabase.build(appContext)
    val dailyStore = DailyStore(appContext)
    val repository = HitomojiRepository(
        context = appContext,
        database = database,
        dailyStore = dailyStore,
        seeder = DictionarySeeder(database),
    )
}
