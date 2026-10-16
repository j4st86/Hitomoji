package app.hitomoji.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class StudyQueueTest {
    @Test
    fun ungradedIntroducedCardsLeadThenRelearningThenDueEaseAndRank() {
        val today = LocalDate.ofEpochDay(100)
        val cards = listOf(
            card(5, CardState.REVIEW, ease = 2.5, due = today.minusDays(1)),
            card(2, CardState.RELEARNING, ease = 2.0, due = today),
            card(9, CardState.REVIEW, ease = 1.5, due = today.minusDays(1)),
            card(3, CardState.NEW, ease = 2.5, due = null, introduced = today),
            card(8, CardState.NEW, ease = 2.5, due = null, introduced = today.minusDays(1)),
            card(1, CardState.NEW, ease = 2.5, due = null, introduced = null),
            card(7, CardState.REVIEW, ease = 2.5, due = today.plusDays(3)),
            card(4, CardState.RELEARNING, ease = 1.8, due = today.minusDays(2)),
        )
        val queue = buildSession(today, cards)
        assertEquals(listOf(3, 8, 4, 2, 9, 5), queue.map { it.rank })
    }

    @Test
    fun gradedNewCardIsNotRepeatedAsNew() {
        val today = LocalDate.ofEpochDay(20)
        val graded = card(
            rank = 1,
            state = CardState.REVIEW,
            ease = 2.5,
            due = today.plusDays(4),
            introduced = today,
            repetitions = 1,
            interval = 4,
        )
        val queue = buildSession(today, listOf(graded))
        assertEquals(emptyList<Int>(), queue.map { it.rank })
    }

    private fun card(
        rank: Int,
        state: CardState,
        ease: Double,
        due: LocalDate?,
        introduced: LocalDate? = null,
        repetitions: Int = if (state == CardState.NEW) 0 else 1,
        interval: Int = if (due == null) 0 else 1,
    ) = SrsCard(
        rank = rank,
        state = state,
        ease = ease,
        intervalDays = interval,
        repetitions = repetitions,
        lapses = 0,
        dueDate = due,
        introducedOn = introduced,
    )
}
