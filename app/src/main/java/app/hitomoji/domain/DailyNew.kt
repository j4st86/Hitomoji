package app.hitomoji.domain

import java.time.LocalDate
import kotlin.math.min

const val MAX_NEW_PER_DAY = 5

data class DailyNewDecision(
    val introduce: List<Int>,
)

/**
 * At most [limit] new kanji on a local date, in frequency order.
 * Kanji already introduced but not yet graded keep their place, so a missed day
 * does not add another full set on top of them.
 * Grading some of today's kanji does not open more places once [limit] were introduced today.
 */
fun chooseDailyNew(
    today: LocalDate,
    introducedOn: Map<Int, LocalDate>,
    waitingRanks: Set<Int>,
    catalogRanks: Set<Int>,
    limit: Int = MAX_NEW_PER_DAY,
): DailyNewDecision {
    val introducedToday = introducedOn.count { it.value == today }
    val room = min(limit - waitingRanks.size, limit - introducedToday).coerceAtLeast(0)
    val introduce = catalogRanks
        .asSequence()
        .filter { it !in introducedOn }
        .sorted()
        .take(room)
        .toList()
    return DailyNewDecision(introduce = introduce)
}
