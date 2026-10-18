package app.hitomoji.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewSchedulerTest {
    @Test
    fun firstGradesUseFsrsStability() {
        val today = LocalDate.ofEpochDay(0)
        val again = applyGrade(fresh(2), Grade.AGAIN, today)
        assertEquals(1, again.intervalDays)
        assertEquals(0.212, again.stability, 0.001)
        assertEquals(6.4133, again.difficulty, 0.001)
        assertEquals(CardState.RELEARNING, again.state)
        assertEquals(1, again.lapses)
        assertEquals(today.plusDays(1), again.dueDate)

        val hard = applyGrade(fresh(2), Grade.HARD, today)
        assertEquals(1, hard.intervalDays)
        assertEquals(1.2931, hard.stability, 0.001)
        assertEquals(5.112, hard.difficulty, 0.001)
        assertEquals(CardState.REVIEW, hard.state)
        assertEquals(1, hard.repetitions)

        val good = applyGrade(fresh(2), Grade.GOOD, today)
        assertEquals(2, good.intervalDays)
        assertEquals(2.3065, good.stability, 0.001)
        assertEquals(2.118, good.difficulty, 0.001)
        assertEquals(CardState.REVIEW, good.state)
        assertEquals(today.plusDays(2), good.dueDate)
    }

    @Test
    fun goodGoodAgainGoodGrowsAndRecovers() {
        // Rank 2 on these dates lands in the zero fuzz bucket, so day 11 stays 11.
        val day0 = LocalDate.ofEpochDay(0)
        var card = applyGrade(fresh(2), Grade.GOOD, day0)
        assertEquals(2, card.intervalDays)

        card = applyGrade(card, Grade.GOOD, card.dueDate!!)
        assertEquals(10.964, card.stability, 0.01)
        assertEquals(11, card.intervalDays)
        assertEquals(2, card.repetitions)

        card = applyGrade(card, Grade.AGAIN, card.dueDate!!)
        assertEquals(1.538, card.stability, 0.01)
        assertEquals(2, card.intervalDays)
        assertEquals(2, card.repetitions)
        assertEquals(1, card.lapses)
        assertEquals(CardState.RELEARNING, card.state)

        card = applyGrade(card, Grade.GOOD, card.dueDate!!)
        assertEquals(4.876, card.stability, 0.01)
        assertEquals(5, card.intervalDays)
        assertEquals(3, card.repetitions)
        assertEquals(CardState.REVIEW, card.state)
    }

    @Test
    fun aLateSuccessGrowsStabilityMoreThanAnOnTimeSuccess() {
        val first = applyGrade(fresh(1), Grade.GOOD, LocalDate.ofEpochDay(0))
        val due = checkNotNull(first.dueDate)
        val onTime = applyGrade(first, Grade.GOOD, due)
        val late = applyGrade(first, Grade.GOOD, due.plusDays(20))
        assertTrue(late.stability > onTime.stability)
    }

    @Test
    fun repeatedAgainStaysWithinDifficultyBounds() {
        var card = fresh(1)
        var today = LocalDate.ofEpochDay(0)
        repeat(12) {
            card = applyGrade(card, Grade.AGAIN, today)
            today = card.dueDate!!
        }
        assertTrue(card.difficulty in 1.0..10.0)
        assertEquals(1, card.intervalDays)
        assertEquals(CardState.RELEARNING, card.state)
        assertTrue(card.lapses >= 12)
    }

    @Test
    fun legacyEaseConvertsInsteadOfRestarting() {
        val (stability, difficulty) = Fsrs.memoryFromSm2(2.5, 10.0)
        assertEquals(10.0, stability, 0.001)
        assertEquals(6.914, difficulty, 0.001)

        val today = LocalDate.ofEpochDay(30)
        val legacy = fresh(4).copy(
            state = CardState.REVIEW,
            ease = 2.5,
            intervalDays = 10,
            repetitions = 3,
            dueDate = today,
        )
        val graded = applyGrade(legacy, Grade.GOOD, today)
        assertTrue(graded.stability > 10.0)
        assertTrue(graded.intervalDays > 4)
    }

    @Test
    fun fuzzIsDeterministicAndCanShiftEitherWay() {
        val day0 = LocalDate.ofEpochDay(0)
        val plus = applyTwiceGood(rank = 1, day0 = day0)
        val minus = applyTwiceGood(rank = 2, day0 = day0)
        val same = applyTwiceGood(rank = 1, day0 = day0)
        assertEquals(plus, same)
        assertTrue(plus.intervalDays != minus.intervalDays)
        assertTrue(plus.intervalDays >= 7)
        assertTrue(minus.intervalDays >= 7)
    }

    @Test
    fun dueDateIsNeverEarlierThanTomorrow() {
        val today = LocalDate.of(2026, 10, 15)
        listOf(Grade.AGAIN, Grade.HARD, Grade.GOOD).forEach { grade ->
            val graded = applyGrade(fresh(8), grade, today)
            assertTrue(!graded.dueDate!!.isBefore(today.plusDays(1)))
            assertTrue(graded.intervalDays >= 1)
        }
        val mature = fresh(8).copy(
            state = CardState.REVIEW,
            intervalDays = 40,
            repetitions = 4,
            ease = 1.3,
            dueDate = today,
            stability = 40.0,
            difficulty = 8.0,
        )
        val graded = applyGrade(mature, Grade.GOOD, today)
        assertTrue(!graded.dueDate!!.isBefore(today.plusDays(1)))
    }

    private fun applyTwiceGood(rank: Int, day0: LocalDate): SrsCard {
        val once = applyGrade(fresh(rank), Grade.GOOD, day0)
        return applyGrade(once, Grade.GOOD, once.dueDate!!)
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
