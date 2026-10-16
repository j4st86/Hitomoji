package app.hitomoji.domain

import kotlin.math.floor

/** Nearest integer, halves away from zero toward +infinity for positive values. */
internal fun roundHalfUp(value: Double): Int = floor(value + 0.5).toInt()

/** Round(value * numerator / denominator), halves up. */
internal fun roundMulDiv(value: Int, numerator: Int, denominator: Int): Int {
    return ((value.toLong() * numerator + denominator / 2) / denominator).toInt()
}

internal fun easeToCents(ease: Double): Int = roundHalfUp(ease * 100.0)

internal fun adjustEase(ease: Double, deltaCents: Int): Double {
    val cents = (easeToCents(ease) + deltaCents).coerceIn(EASE_FLOOR_CENTS, EASE_CEILING_CENTS)
    return cents / 100.0
}

internal const val EASE_FLOOR_CENTS = 130
internal const val EASE_CEILING_CENTS = 250
