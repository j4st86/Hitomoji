package app.hitomoji.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Schedules the next review with FSRS-6.
 *
 * A card graded before this scheduler is converted from its ease and interval
 * on the next grade. When the base interval is at least 7 days, it is shifted
 * by about ±5 percent. The shift is a deterministic function of [SrsCard.rank]
 * and the pre-shift due date. The due date is never earlier than tomorrow.
 */
fun applyGrade(card: SrsCard, grade: Grade, today: LocalDate): SrsCard {
    val prepared = card.withFsrsMemory()
    val rating = grade.fsrsRating()
    val elapsed = elapsedDays(prepared, today)
    val stability: Double
    val difficulty: Double
    if (prepared.stability <= 0.0) {
        stability = Fsrs.initialStability(rating)
        difficulty = Fsrs.initialDifficulty(rating)
    } else if (elapsed < 1) {
        stability = Fsrs.shortTermStability(prepared.stability, rating)
        difficulty = Fsrs.nextDifficulty(prepared.difficulty, rating)
    } else {
        val recall = Fsrs.retrievability(elapsed, prepared.stability)
        stability = Fsrs.nextStability(prepared.difficulty, prepared.stability, recall, rating)
        difficulty = Fsrs.nextDifficulty(prepared.difficulty, rating)
    }
    val baseInterval = Fsrs.scheduledInterval(stability)
    val anchor = today.plusDays(baseInterval.toLong())
    val fuzzed = fuzzedInterval(baseInterval, prepared.rank, anchor).coerceAtLeast(1)
    val earliest = today.plusDays(1)
    val due = today.plusDays(fuzzed.toLong()).let { candidate ->
        if (candidate.isBefore(earliest)) earliest else candidate
    }
    val interval = ChronoUnit.DAYS.between(today, due).toInt().coerceAtLeast(1)
    val failed = grade == Grade.AGAIN
    return prepared.copy(
        state = if (failed) CardState.RELEARNING else CardState.REVIEW,
        intervalDays = interval,
        repetitions = if (failed) prepared.repetitions else prepared.repetitions + 1,
        lapses = prepared.lapses + if (failed) 1 else 0,
        dueDate = due,
        stability = stability,
        difficulty = difficulty,
    )
}

/** Fills stability and difficulty for a card that was scheduled by the old ease multiplier. */
fun SrsCard.withFsrsMemory(): SrsCard {
    if (stability > 0.0) return this
    if (!wasGraded() || intervalDays <= 0) return this
    val (stability, difficulty) = Fsrs.memoryFromSm2(ease, intervalDays.toDouble())
    return copy(stability = stability, difficulty = difficulty)
}

private fun elapsedDays(card: SrsCard, today: LocalDate): Int {
    val due = card.dueDate ?: return 0
    if (card.intervalDays <= 0) return 0
    val lastReview = due.minusDays(card.intervalDays.toLong())
    return ChronoUnit.DAYS.between(lastReview, today).toInt().coerceAtLeast(0)
}

/**
 * Magnitude is 5 percent of the interval, rounded half up.
 * Bucket 0 subtracts it, bucket 1 leaves the interval, bucket 2 adds it.
 */
internal fun fuzzedInterval(baseInterval: Int, rank: Int, anchorDue: LocalDate): Int {
    if (baseInterval < 7) return baseInterval.coerceAtLeast(1)
    val magnitude = (baseInterval * 5 + 50) / 100
    if (magnitude == 0) return baseInterval
    val mixed = rank.toLong() * 31L + anchorDue.toEpochDay() * 17L
    val bucket = Math.floorMod(mixed, 3L).toInt()
    val delta = when (bucket) {
        0 -> -magnitude
        1 -> 0
        else -> magnitude
    }
    return (baseInterval + delta).coerceAtLeast(1)
}
