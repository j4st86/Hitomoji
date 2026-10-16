package app.hitomoji.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.LocalDate
import kotlinx.coroutines.flow.first

private val Context.hitomojiDataStore by preferencesDataStore(name = "hitomoji")

data class StoredDaily(
    val date: LocalDate?,
    val rank: Int?,
    val lastShownRank: Int?,
)

class DailyStore(context: Context) {
    private val dataStore = context.applicationContext.hitomojiDataStore

    suspend fun read(): StoredDaily {
        val prefs = dataStore.data.first()
        val date = prefs[DATE]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val rank = prefs[RANK]
        val last = prefs[LAST_SHOWN]
        return StoredDaily(date = date, rank = rank, lastShownRank = last)
    }

    suspend fun writeDaily(date: LocalDate, rank: Int?) {
        dataStore.edit { prefs ->
            prefs[DATE] = date.toString()
            if (rank == null) {
                prefs.remove(RANK)
            } else {
                prefs[RANK] = rank
            }
        }
    }

    suspend fun writeLastShown(rank: Int) {
        dataStore.edit { prefs -> prefs[LAST_SHOWN] = rank }
    }

    private companion object {
        val DATE = stringPreferencesKey("daily_date")
        val RANK = intPreferencesKey("daily_rank")
        val LAST_SHOWN = intPreferencesKey("last_shown_rank")
    }
}
