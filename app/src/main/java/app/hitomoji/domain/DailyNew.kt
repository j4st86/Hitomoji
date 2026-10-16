package app.hitomoji.domain

import java.time.LocalDate

data class DailyNewDecision(
    val rank: Int?,
    val markIntroduced: Boolean,
    val persistDate: LocalDate,
    val persistRank: Int?,
)

/**
 * One new kanji per local date, in frequency order.
 * A stored choice for [today] is returned unchanged.
 * A gap in calendar days does not introduce more than one kanji.
 */
fun chooseDailyNew(
    today: LocalDate,
    storedDate: LocalDate?,
    storedRank: Int?,
    introducedOn: Map<Int, LocalDate>,
    catalogRanks: Set<Int>,
): DailyNewDecision {
    if (storedDate == today) {
        return DailyNewDecision(
            rank = storedRank,
            markIntroduced = false,
            persistDate = today,
            persistRank = storedRank,
        )
    }
    val alreadyToday = introducedOn
        .filterValues { it == today }
        .keys
        .minOrNull()
    if (alreadyToday != null) {
        return DailyNewDecision(
            rank = alreadyToday,
            markIntroduced = false,
            persistDate = today,
            persistRank = alreadyToday,
        )
    }
    val next = catalogRanks
        .asSequence()
        .filter { it !in introducedOn }
        .minOrNull()
    return DailyNewDecision(
        rank = next,
        markIntroduced = next != null,
        persistDate = today,
        persistRank = next,
    )
}
