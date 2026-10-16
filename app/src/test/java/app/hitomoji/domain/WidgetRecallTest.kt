package app.hitomoji.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetRecallTest {
    private val today = LocalDate.ofEpochDay(40)

    @Test
    fun nothingGradedKeepsTheFirstKanji() {
        val later = card(4, CardState.NEW, ease = 2.5, due = null, lapses = 0, interval = 0, repetitions = 0)
        val first = card(1, CardState.NEW, ease = 2.5, due = null, lapses = 0, interval = 0, repetitions = 0)
        val pick = pickGradedWidget(listOf(later, first), today)
        assertEquals(1, pick.rank)
        assertEquals(WidgetMode.NEW, pick.mode)
    }

    @Test
    fun theWeakerKanjiStaysOnTheWidget() {
        val strong = card(1, CardState.REVIEW, ease = 2.5, due = today.plusDays(20), lapses = 0, interval = 30)
        val weak = card(2, CardState.RELEARNING, ease = 1.3, due = today.minusDays(2), lapses = 4, interval = 1)
        assertEquals(2, pickGradedWidget(listOf(strong, weak), today).rank)
        assertEquals(2, pickGradedWidget(listOf(weak, strong), today).rank)
    }

    @Test
    fun oneGradedKanjiIsAlwaysThatKanji() {
        val only = card(7, CardState.REVIEW, ease = 2.2, due = today.plusDays(4), lapses = 1, interval = 4)
        val pick = pickGradedWidget(listOf(only), today)
        assertEquals(7, pick.rank)
        assertEquals(WidgetMode.REVIEW, pick.mode)
    }

    private fun card(
        rank: Int,
        state: CardState,
        ease: Double,
        due: LocalDate?,
        lapses: Int,
        interval: Int,
        repetitions: Int = 1,
    ) = SrsCard(
        rank = rank,
        state = state,
        ease = ease,
        intervalDays = interval,
        repetitions = repetitions,
        lapses = lapses,
        dueDate = due,
        introducedOn = today.minusDays(3),
    )
}
