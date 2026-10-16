package app.hitomoji.domain

import java.time.LocalDate

/**
 * Today's session: the ungraded new card first, then reviews due today or earlier.
 * Reviews are ordered by relearning, earlier due date, lower ease, then lower rank.
 */
fun buildSession(
    today: LocalDate,
    cards: List<SrsCard>,
    todayNewRank: Int?,
): List<SrsCard> {
    val fresh = cards.firstOrNull { card ->
        card.rank == todayNewRank && card.state == CardState.NEW && card.dueDate == null
    }
    val reviews = cards
        .asSequence()
        .filter { it.rank != fresh?.rank }
        .filter { card -> card.dueDate != null && !card.dueDate.isAfter(today) }
        .sortedWith(reviewOrder)
        .toList()
    return listOfNotNull(fresh) + reviews
}

private val reviewOrder = compareBy<SrsCard>(
    { if (it.state == CardState.RELEARNING) 0 else 1 },
    { it.dueDate },
    { it.ease },
    { it.rank },
)

fun widgetSelection(queue: List<SrsCard>, lastShownRank: Int?): WidgetSelection {
    val head = queue.firstOrNull() ?: return WidgetSelection(lastShownRank, WidgetMode.DONE)
    val mode = if (head.state == CardState.NEW && head.dueDate == null) {
        WidgetMode.NEW
    } else {
        WidgetMode.REVIEW
    }
    return WidgetSelection(head.rank, mode)
}
