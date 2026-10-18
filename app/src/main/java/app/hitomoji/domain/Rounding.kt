package app.hitomoji.domain

import kotlin.math.floor

/** Nearest integer, halves away from zero toward +infinity for positive values. */
internal fun roundHalfUp(value: Double): Int = floor(value + 0.5).toInt()

internal fun easeToCents(ease: Double): Int = roundHalfUp(ease * 100.0)

internal const val EASE_CEILING_CENTS = 250
