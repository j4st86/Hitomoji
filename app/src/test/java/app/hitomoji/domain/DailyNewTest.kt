package app.hitomoji.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyNewTest {
    private val catalog = (1..500).toSet()
    private val day1 = LocalDate.of(2026, 3, 1)
    private val day3 = LocalDate.of(2026, 3, 3)

    @Test
    fun aDayIntroducesAtMostFiveInFrequencyOrder() {
        val first = chooseDailyNew(
            today = day1,
            introducedOn = emptyMap(),
            waitingRanks = emptySet(),
            catalogRanks = catalog,
        )
        assertEquals(listOf(1, 2, 3, 4, 5), first.introduce)
    }

    @Test
    fun skippedDaysDoNotAddAnotherSetWhileFiveAreStillWaiting() {
        val introduced = (1..5).associateWith { day1 }
        val second = chooseDailyNew(
            today = day3,
            introducedOn = introduced,
            waitingRanks = introduced.keys,
            catalogRanks = catalog,
        )
        assertEquals(emptyList<Int>(), second.introduce)
    }

    @Test
    fun theNextDayIntroducesTheNextFiveAfterThePreviousSetWasGraded() {
        val introduced = (1..5).associateWith { day1 }
        val second = chooseDailyNew(
            today = day3,
            introducedOn = introduced,
            waitingRanks = emptySet(),
            catalogRanks = catalog,
        )
        assertEquals(listOf(6, 7, 8, 9, 10), second.introduce)
    }

    @Test
    fun gradingPartOfTodayDoesNotOpenMoreThanFive() {
        val introduced = (1..5).associateWith { day1 }
        val again = chooseDailyNew(
            today = day1,
            introducedOn = introduced,
            waitingRanks = setOf(4, 5),
            catalogRanks = catalog,
        )
        assertEquals(emptyList<Int>(), again.introduce)
    }

    @Test
    fun aPartialDayFillsOnlyTheRemainingPlaces() {
        val introduced = mapOf(1 to day1, 2 to day1)
        val decision = chooseDailyNew(
            today = day1,
            introducedOn = introduced,
            waitingRanks = setOf(1, 2),
            catalogRanks = catalog,
        )
        assertEquals(listOf(3, 4, 5), decision.introduce)
    }

    @Test
    fun noKanjiLeftReturnsNothing() {
        val today = LocalDate.of(2026, 8, 1)
        val introduced = catalog.associateWith { today.minusDays(1) }
        val decision = chooseDailyNew(today, introduced, emptySet(), catalog)
        assertEquals(emptyList<Int>(), decision.introduce)
    }
}
