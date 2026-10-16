package app.hitomoji.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class StudyQueueTest {
    @Test
    fun freshNewCardLeadsThenRelearningThenDueEaseAndRank() {
        val today = LocalDate.ofEpochDay(100)
        val cards = listOf(
            card(5, CardState.REVIEW, ease = 2.5, due = today.minusDays(1)),
            card(2, CardState.RELEARNING, ease = 2.0, due = today),
            card(9, CardState.REVIEW, ease = 1.5, due = today.minusDays(1)),
            card(3, CardState.NEW, ease = 2.5, due = null, introduced = today),
            card(7, CardState.REVIEW, ease = 2.5, due = today.plusDays(3)),
            card(4, CardState.RELEARNING, ease = 1.8, due = today.minusDays(2)),
        )
        val queue = buildSession(today, cards, todayNewRank = 3)
        assertEquals(listOf(3, 4, 2, 9, 5), queue.map { it.rank })
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
        val queue = buildSession(today, listOf(graded), todayNewRank = 1)
        assertEquals(emptyList<Int>(), queue.map { it.rank })
        val widget = widgetSelection(queue, lastShownRank = 1)
        assertEquals(WidgetMode.DONE, widget.mode)
        assertEquals(1, widget.rank)
    }

    @Test
    fun widgetPrefersUngradedNewThenTheMostUrgentReview() {
        val today = LocalDate.ofEpochDay(30)
        val fresh = card(1, CardState.NEW, 2.5, due = null, introduced = today)
        val review = card(2, CardState.RELEARNING, 2.0, due = today)
        val withNew = buildSession(today, listOf(review, fresh), todayNewRank = 1)
        assertEquals(WidgetMode.NEW, widgetSelection(withNew, null).mode)
        assertEquals(1, widgetSelection(withNew, null).rank)

        val reviewsOnly = buildSession(today, listOf(review), todayNewRank = null)
        val pick = widgetSelection(reviewsOnly, null)
        assertEquals(WidgetMode.REVIEW, pick.mode)
        assertEquals(2, pick.rank)
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
