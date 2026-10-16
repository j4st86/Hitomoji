package app.hitomoji.domain

import java.time.LocalDate

/**
 * Today's session: introduced kanji that have not been graded yet, in rank order,
 * then reviews due today or earlier.
 * Reviews are ordered by relearning, earlier due date, lower ease, then lower rank.
 */
fun buildSession(
    today: LocalDate,
    cards: List<SrsCard>,
): List<SrsCard> {
    val fresh = cards
        .filter { it.introducedOn != null && it.state == CardState.NEW && it.dueDate == null }
        .sortedBy { it.rank }
    val freshRanks = fresh.map { it.rank }.toSet()
    val reviews = cards
        .asSequence()
        .filter { it.rank !in freshRanks }
        .filter { card -> card.dueDate != null && !card.dueDate.isAfter(today) }
        .sortedWith(reviewOrder)
        .toList()
    return fresh + reviews
}

private val reviewOrder = compareBy<SrsCard>(
    { if (it.state == CardState.RELEARNING) 0 else 1 },
    { it.dueDate },
    { it.ease },
    { it.rank },
)
