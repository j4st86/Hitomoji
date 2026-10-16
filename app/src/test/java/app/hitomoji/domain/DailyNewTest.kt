package app.hitomoji.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class DailyNewTest {
    private val catalog = (1..500).toSet()

    @Test
    fun skippedDaysIntroduceOnlyTheNextKanji() {
        val day1 = LocalDate.of(2026, 3, 1)
        val day3 = LocalDate.of(2026, 3, 3)
        val first = chooseDailyNew(
            today = day1,
            storedDate = null,
            storedRank = null,
            introducedOn = emptyMap(),
            catalogRanks = catalog,
        )
        assertEquals(1, first.rank)
        assertEquals(true, first.markIntroduced)

        val second = chooseDailyNew(
            today = day3,
            storedDate = day1,
            storedRank = 1,
            introducedOn = mapOf(1 to day1),
            catalogRanks = catalog,
        )
        assertEquals(2, second.rank)
        assertEquals(true, second.markIntroduced)
        assertFalse(3 in listOfNotNull(second.rank))
    }

    @Test
    fun sameDayRequestReturnsTheStoredRank() {
        val today = LocalDate.of(2026, 5, 2)
        val again = chooseDailyNew(
            today = today,
            storedDate = today,
            storedRank = 7,
            introducedOn = mapOf(7 to today),
            catalogRanks = catalog,
        )
        assertEquals(7, again.rank)
        assertEquals(false, again.markIntroduced)
        assertEquals(7, again.persistRank)
    }

    @Test
    fun recoveryUsesTheKanjiAlreadyIntroducedToday() {
        val today = LocalDate.of(2026, 5, 2)
        val decision = chooseDailyNew(
            today = today,
            storedDate = null,
            storedRank = null,
            introducedOn = mapOf(1 to today.minusDays(1), 2 to today),
            catalogRanks = catalog,
        )
        assertEquals(2, decision.rank)
        assertEquals(false, decision.markIntroduced)
    }

    @Test
    fun noKanjiLeftReturnsNull() {
        val today = LocalDate.of(2026, 8, 1)
        val introduced = catalog.associateWith { today.minusDays(1) }
        val decision = chooseDailyNew(today, null, null, introduced, catalog)
        assertNull(decision.rank)
        assertEquals(false, decision.markIntroduced)
    }
}
