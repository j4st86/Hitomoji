package app.hitomoji.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewSchedulerTest {
    @Test
    fun goodGoodAgainGood_matchesTheWorkedExample() {
        // Rank 3 with epoch dates lands in the zero fuzz bucket, so day 4 stays at 10.
        val day0 = LocalDate.ofEpochDay(0)
        var card = fresh(rank = 3)

        card = applyGrade(card, Grade.GOOD, day0)
        assertEquals(4, card.intervalDays)
        assertEquals(day0.plusDays(4), card.dueDate)
        assertEquals(1, card.repetitions)
        assertEquals(2.5, card.ease, 0.001)
        assertEquals(CardState.REVIEW, card.state)

        val day4 = day0.plusDays(4)
        card = applyGrade(card, Grade.GOOD, day4)
        assertEquals(10, card.intervalDays)
        assertEquals(day0.plusDays(14), card.dueDate)
        assertEquals(2, card.repetitions)
        assertEquals(2.5, card.ease, 0.001)

        val day14 = day0.plusDays(14)
        card = applyGrade(card, Grade.AGAIN, day14)
        assertEquals(day0.plusDays(15), card.dueDate)
        assertEquals(1, card.intervalDays)
        assertEquals(0, card.repetitions)
        assertEquals(2.3, card.ease, 0.001)
        assertEquals(1, card.lapses)
        assertEquals(CardState.RELEARNING, card.state)

        val day15 = day0.plusDays(15)
        card = applyGrade(card, Grade.GOOD, day15)
        assertEquals(4, card.intervalDays)
        assertEquals(1, card.repetitions)
        assertEquals(2.3, card.ease, 0.001)
        assertEquals(CardState.REVIEW, card.state)
        assertEquals(day15.plusDays(4), card.dueDate)
    }

    @Test
    fun againSeriesFloorsEaseAtOnePointThree() {
        var card = fresh(rank = 1)
        var today = LocalDate.ofEpochDay(0)
        repeat(12) {
            card = applyGrade(card, Grade.AGAIN, today)
            today = card.dueDate!!
        }
        assertEquals(1.3, card.ease, 0.001)
        assertEquals(CardState.RELEARNING, card.state)
        assertTrue(card.lapses >= 12)
    }

    @Test
    fun hardOnMatureIntervalTenYieldsTwelve() {
        val today = LocalDate.ofEpochDay(2)
        val card = fresh(rank = 3).copy(
            state = CardState.REVIEW,
            ease = 2.5,
            intervalDays = 10,
            repetitions = 2,
            dueDate = today,
        )
        val graded = applyGrade(card, Grade.HARD, today)
        assertEquals(12, graded.intervalDays)
        assertEquals(today.plusDays(12), graded.dueDate)
        assertEquals(2, graded.repetitions)
        assertEquals(CardState.REVIEW, graded.state)
        assertEquals(2.35, graded.ease, 0.001)
    }

    @Test
    fun fuzzIsDeterministicAndCanShiftEitherWay() {
        val day0 = LocalDate.ofEpochDay(0)
        val day4 = day0.plusDays(4)
        val plus = applyTwiceGood(rank = 1, day0 = day0)
        val minus = applyTwiceGood(rank = 2, day0 = day0)
        val same = applyTwiceGood(rank = 1, day0 = day0)
        assertEquals(11, plus.intervalDays)
        assertEquals(day4.plusDays(11), plus.dueDate)
        assertEquals(9, minus.intervalDays)
        assertEquals(day4.plusDays(9), minus.dueDate)
        assertEquals(plus, same)
    }

    @Test
    fun dueDateIsNeverEarlierThanTomorrow() {
        val today = LocalDate.of(2026, 10, 15)
        var card = fresh(rank = 8)
        listOf(Grade.AGAIN, Grade.HARD, Grade.GOOD).forEach { grade ->
            val graded = applyGrade(card, grade, today)
            assertTrue(!graded.dueDate!!.isBefore(today.plusDays(1)))
            assertTrue(graded.intervalDays >= 1)
        }
        card = card.copy(state = CardState.REVIEW, intervalDays = 40, repetitions = 4, ease = 1.3)
        val graded = applyGrade(card, Grade.GOOD, today)
        assertTrue(!graded.dueDate!!.isBefore(today.plusDays(1)))
    }

    @Test
    fun learningGradesUseFixedIntervals() {
        val today = LocalDate.of(2026, 1, 1)
        val again = applyGrade(fresh(4), Grade.AGAIN, today)
        assertEquals(1, again.intervalDays)
        assertEquals(0, again.repetitions)
        assertEquals(CardState.RELEARNING, again.state)
        assertEquals(today.plusDays(1), again.dueDate)

        val hard = applyGrade(fresh(4), Grade.HARD, today)
        assertEquals(2, hard.intervalDays)
        assertEquals(0, hard.repetitions)
        assertEquals(CardState.REVIEW, hard.state)
        assertEquals(2.35, hard.ease, 0.001)
        assertEquals(today.plusDays(2), hard.dueDate)

        val good = applyGrade(fresh(4), Grade.GOOD, today)
        assertEquals(4, good.intervalDays)
        assertEquals(1, good.repetitions)
        assertEquals(2.5, good.ease, 0.001)
    }

    private fun applyTwiceGood(rank: Int, day0: LocalDate): SrsCard {
        val once = applyGrade(fresh(rank), Grade.GOOD, day0)
        return applyGrade(once, Grade.GOOD, day0.plusDays(4))
    }

    private fun fresh(rank: Int) = SrsCard(
        rank = rank,
        state = CardState.NEW,
        ease = 2.5,
        intervalDays = 0,
        repetitions = 0,
        lapses = 0,
        dueDate = null,
        introducedOn = null,
    )
}
