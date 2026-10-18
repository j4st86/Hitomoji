package app.hitomoji.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** A kanji counts once the learner has pressed Again, Hard, or Good. */
fun SrsCard.wasGraded(): Boolean {
    return state != CardState.NEW || dueDate != null || repetitions > 0 || lapses > 0
}

/**
 * Higher weight means the kanji is shown more often.
 * High difficulty, lapses, relearning, a short interval, and an overdue date all add weight.
 * A card that has not been converted to FSRS still uses its old ease multiplier.
 */
fun recallWeight(card: SrsCard, today: LocalDate): Int {
    val memoryPenalty = if (card.stability > 0.0) {
        ((card.difficulty - 1.0) * 16.0).toInt().coerceIn(0, 144)
    } else {
        (EASE_CEILING_CENTS - easeToCents(card.ease)).coerceAtLeast(0)
    }
    val lapsePenalty = card.lapses.coerceAtMost(8) * 20
    val statePenalty = if (card.state == CardState.RELEARNING) 80 else 0
    val intervalPenalty = when {
        card.intervalDays <= 1 -> 60
        card.intervalDays <= 3 -> 30
        card.intervalDays < 7 -> 15
        else -> 0
    }
    val overdue = card.dueDate?.let { due ->
        if (due.isAfter(today)) 0
        else ChronoUnit.DAYS.between(due, today).toInt().coerceIn(0, 30) * 4
    } ?: 0
    return memoryPenalty + lapsePenalty + statePenalty + intervalPenalty + overdue + 10
}

/**
 * The widget shows the graded kanji the learner knows least well.
 * Until something has been graded, the lowest rank stays on the widget.
 * Equal weights keep the lower rank, so the choice does not jump around.
 */
fun pickGradedWidget(cards: List<SrsCard>, today: LocalDate): WidgetSelection {
    val graded = cards.filter { it.wasGraded() }
    if (graded.isEmpty()) {
        val first = cards.minByOrNull { it.rank } ?: return WidgetSelection(rank = null, mode = WidgetMode.WAITING)
        return WidgetSelection(first.rank, WidgetMode.NEW)
    }
    val weakest = graded.maxWith(compareBy<SrsCard> { recallWeight(it, today) }.thenByDescending { it.rank })
    return WidgetSelection(weakest.rank, WidgetMode.REVIEW)
}
