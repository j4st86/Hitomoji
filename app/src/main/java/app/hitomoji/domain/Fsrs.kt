package app.hitomoji.domain

import kotlin.math.exp
import kotlin.math.expm1
import kotlin.math.pow

/**
 * FSRS-6 with the published default parameters and a 90% recall target.
 * The next review is the day when predicted recall falls to that target.
 * There is no Easy grade and no same-day step: the app schedules whole days.
 *
 * Parameter values are the FSRS-6 defaults used by Anki.
 */
internal object Fsrs {
    const val DESIRED_RETENTION = 0.9
    private const val STABILITY_MIN = 0.001
    private const val DIFFICULTY_MIN = 1.0
    private const val DIFFICULTY_MAX = 10.0
    private const val MAX_INTERVAL_DAYS = 36500

    private val w = doubleArrayOf(
        0.212,
        1.2931,
        2.3065,
        8.2956,
        6.4133,
        0.8334,
        3.0194,
        0.001,
        1.8722,
        0.1666,
        0.796,
        1.4835,
        0.0614,
        0.2629,
        1.6483,
        0.6014,
        1.8729,
        0.5425,
        0.0912,
        0.0658,
        0.1542,
    )

    private val decay = -w[20]
    private val factor = 0.9.pow(1.0 / decay) - 1.0

    fun initialStability(rating: Int): Double = w[rating - 1].coerceAtLeast(STABILITY_MIN)

    fun initialDifficulty(rating: Int): Double {
        return clampDifficulty(w[4] - exp(w[5] * (rating - 1)) + 1.0)
    }

    fun scheduledInterval(stability: Double): Int {
        val raw = (stability / factor) * (DESIRED_RETENTION.pow(1.0 / decay) - 1.0)
        return Math.rint(raw).toInt().coerceIn(1, MAX_INTERVAL_DAYS)
    }

    fun retrievability(elapsedDays: Int, stability: Double): Double {
        return (1.0 + factor * elapsedDays / stability).pow(decay)
    }

    fun nextDifficulty(difficulty: Double, rating: Int): Double {
        val delta = -(w[6] * (rating - 3))
        val damped = difficulty + (DIFFICULTY_MAX - difficulty) * delta / 9.0
        val easyTarget = w[4] - exp(w[5] * 3) + 1.0
        val reverted = w[7] * easyTarget + (1.0 - w[7]) * damped
        return clampDifficulty(reverted)
    }

    fun nextStability(difficulty: Double, stability: Double, retrievability: Double, rating: Int): Double {
        val next = if (rating == 1) {
            val forgotten = w[11] *
                difficulty.pow(-w[12]) *
                ((stability + 1.0).pow(w[13]) - 1.0) *
                exp((1.0 - retrievability) * w[14])
            val shortTermCap = stability / exp(w[17] * w[18])
            minOf(forgotten, shortTermCap)
        } else {
            val hardPenalty = if (rating == 2) w[15] else 1.0
            stability * (
                1.0 +
                    exp(w[8]) *
                    (11.0 - difficulty) *
                    stability.pow(-w[9]) *
                    (exp((1.0 - retrievability) * w[10]) - 1.0) *
                    hardPenalty
                )
        }
        return next.coerceAtLeast(STABILITY_MIN)
    }

    fun shortTermStability(stability: Double, rating: Int): Double {
        var gain = exp(w[17] * (rating - 3 + w[18])) * stability.pow(-w[19])
        if (rating >= 2) gain = gain.coerceAtLeast(1.0)
        return (stability * gain).coerceAtLeast(STABILITY_MIN)
    }

    /**
     * Approximates FSRS memory from an SM-2 ease and interval, assuming the old
     * interval was aiming at 90% recall. Ease is the multiplier, such as 2.5.
     */
    fun memoryFromSm2(ease: Double, intervalDays: Double): Pair<Double, Double> {
        val stability = (intervalDays.coerceAtLeast(STABILITY_MIN) * factor / (DESIRED_RETENTION.pow(1.0 / decay) - 1.0))
            .coerceAtLeast(STABILITY_MIN)
        val difficulty = 11.0 - (ease - 1.0) / (
            exp(w[8]) * stability.pow(-w[9]) * expm1((1.0 - DESIRED_RETENTION) * w[10])
            )
        return stability to clampDifficulty(difficulty)
    }

    private fun clampDifficulty(value: Double): Double = value.coerceIn(DIFFICULTY_MIN, DIFFICULTY_MAX)
}

internal fun Grade.fsrsRating(): Int = when (this) {
    Grade.AGAIN -> 1
    Grade.HARD -> 2
    Grade.GOOD -> 3
}
