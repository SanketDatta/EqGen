package com.sanket.eqgen.domain.model

import kotlin.random.Random

/** Largest absolute value a user may enter as a root or solution coordinate in v1 (spec D-017). */
const val MAX_INPUT_ABS: Int = 100

/**
 * Largest absolute value a configured coefficient bound may take.
 *
 * With inputs capped at [MAX_INPUT_ABS], this keeps every derived value (for example the quadratic
 * `c = a * r1 * r2`, at most 1000 * 100 * 100 = 10,000,000) comfortably inside `Int`, so plain
 * `Int` arithmetic is exact when the range becomes user-configurable.
 */
const val MAX_COEFFICIENT_ABS: Int = 1_000

/** True when this value is an acceptable user-entered root or solution coordinate. */
fun Int.isWithinInputLimit(): Boolean = this in -MAX_INPUT_ABS..MAX_INPUT_ABS

/**
 * Rules for drawing generated coefficients (spec section 03, "Conventions").
 *
 * Zero is always excluded, so generated `a` and `b` are never zero.
 * The default range `-10..20` is intentionally asymmetric.
 */
data class GenerationConfig(
    val coefficientRange: IntRange = DEFAULT_COEFFICIENT_RANGE,
) {
    init {
        require(!coefficientRange.isEmpty()) { "coefficientRange must not be empty: $coefficientRange" }
        require(coefficientRange.first >= -MAX_COEFFICIENT_ABS && coefficientRange.last <= MAX_COEFFICIENT_ABS) {
            "coefficientRange must stay within ±$MAX_COEFFICIENT_ABS: $coefficientRange"
        }
    }

    /** Every integer in [coefficientRange] except 0, in ascending order. */
    val allowedCoefficients: List<Int> = coefficientRange.filter { it != 0 }

    init {
        require(allowedCoefficients.isNotEmpty()) {
            "coefficientRange must contain a non-zero value: $coefficientRange"
        }
    }

    /** Draws one allowed coefficient, uniformly. */
    fun randomCoefficient(random: Random): Int =
        allowedCoefficients[random.nextInt(allowedCoefficients.size)]

    companion object {
        val DEFAULT_COEFFICIENT_RANGE: IntRange = -10..20
    }
}
