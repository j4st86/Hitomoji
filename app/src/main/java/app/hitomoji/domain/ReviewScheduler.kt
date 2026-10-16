package app.hitomoji.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Schedules the next review.
 *
 * When the base interval is at least 7 days, it is shifted by about ±5 percent.
 * The shift is a deterministic function of [SrsCard.rank] and the pre-shift due
 * date: three buckets, minus, none, or plus. The same input always yields the
 * same due date. The due date is never earlier than tomorrow.
 */
fun applyGrade(card: SrsCard, grade: Grade, today: LocalDate): SrsCard {
    val mature = card.repetitions > 0 && card.intervalDays > 0
    val ease = adjustEase(card.ease, 0)
    val scheduled = if (!mature) {
        scheduleLearning(card, grade, today, ease)
    } else {
        scheduleMature(card, grade, today, ease)
    }
    val earliest = today.plusDays(1)
    val due = if (scheduled.dueDate != null && scheduled.dueDate.isBefore(earliest)) {
        earliest
    } else {
        scheduled.dueDate
    }
    val interval = if (due == null) {
        scheduled.intervalDays
    } else {
        ChronoUnit.DAYS.between(today, due).toInt()
    }
    return scheduled.copy(dueDate = due, intervalDays = interval.coerceAtLeast(1))
}

private fun scheduleLearning(
    card: SrsCard,
    grade: Grade,
    today: LocalDate,
    ease: Double,
): SrsCard {
    return when (grade) {
        Grade.AGAIN -> card.copy(
            state = CardState.RELEARNING,
            ease = adjustEase(ease, -20),
            intervalDays = 1,
            repetitions = 0,
            lapses = card.lapses + 1,
            dueDate = today.plusDays(1),
        )
        Grade.HARD -> card.copy(
            state = CardState.REVIEW,
            ease = adjustEase(ease, -15),
            intervalDays = 2,
            repetitions = 0,
            dueDate = today.plusDays(2),
        )
        Grade.GOOD -> card.copy(
            state = CardState.REVIEW,
            ease = ease,
            intervalDays = 4,
            repetitions = 1,
            dueDate = today.plusDays(4),
        )
    }
}

private fun scheduleMature(
    card: SrsCard,
    grade: Grade,
    today: LocalDate,
    ease: Double,
): SrsCard {
    return when (grade) {
        Grade.AGAIN -> scheduleLearning(card, Grade.AGAIN, today, ease)
        Grade.HARD -> {
            val grown = maxOf(card.intervalDays + 1, roundMulDiv(card.intervalDays, 6, 5))
            finishMature(card, today, baseInterval = grown, ease = adjustEase(ease, -15), repetitions = card.repetitions)
        }
        Grade.GOOD -> {
            val grown = roundIntervalTimesEase(card.intervalDays, ease).coerceAtLeast(1)
            finishMature(card, today, baseInterval = grown, ease = ease, repetitions = card.repetitions + 1)
        }
    }
}

private fun finishMature(
    card: SrsCard,
    today: LocalDate,
    baseInterval: Int,
    ease: Double,
    repetitions: Int,
): SrsCard {
    val safeBase = baseInterval.coerceAtLeast(1)
    val anchor = today.plusDays(safeBase.toLong())
    val interval = fuzzedInterval(safeBase, card.rank, anchor)
    return card.copy(
        state = CardState.REVIEW,
        ease = ease,
        intervalDays = interval,
        repetitions = repetitions,
        dueDate = today.plusDays(interval.toLong()),
    )
}

internal fun roundIntervalTimesEase(interval: Int, ease: Double): Int {
    val cents = easeToCents(ease)
    return ((interval.toLong() * cents + 50) / 100).toInt()
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
