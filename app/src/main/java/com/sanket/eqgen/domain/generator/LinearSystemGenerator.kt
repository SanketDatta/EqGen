package com.sanket.eqgen.domain.generator

import com.sanket.eqgen.domain.model.GeneratedEquation
import com.sanket.eqgen.domain.model.GenerationConfig
import com.sanket.eqgen.domain.model.GenerationRequest
import com.sanket.eqgen.domain.model.LinearEquation2D
import com.sanket.eqgen.domain.model.LinearEquation3D
import com.sanket.eqgen.domain.model.requireWithinInputLimit
import kotlin.random.Random

/**
 * Linear generators: M2 (single equation in two variables), M3 (pair with a unique solution) and
 * M5 (three equations in three variables with a unique solution).
 *
 * Pure and deterministic: the same request, config and [Random] state give the same result.
 * Throws [IllegalArgumentException] if the request is outside the input limit.
 */
class LinearSystemGenerator(private val config: GenerationConfig = GenerationConfig()) {

    /** M2: draws `a` and `b`, then derives `c = a·x + b·y` so the line passes through `(x, y)`. */
    fun generateTwoVar(
        request: GenerationRequest.LinearTwoVar,
        random: Random,
    ): GeneratedEquation.LinearTwoVar {
        request.requireWithinInputLimit()
        val equation = lineThrough(
            a = config.randomCoefficient(random),
            b = config.randomCoefficient(random),
            x = request.x,
            y = request.y,
        )
        return GeneratedEquation.LinearTwoVar(equation = equation, x = request.x, y = request.y)
    }

    /**
     * M3: two lines through `(x, y)` whose determinant `a1·b2 − a2·b1` is never zero, so the lines are
     * neither parallel nor identical and `(x, y)` is the unique solution.
     *
     * Needs at least two distinct allowed coefficients; with a single one every pair would be parallel.
     * No retry loop is needed: for fixed `a1`, `b1`, `a2` at most one value of `b2` gives a zero
     * determinant, and that value is excluded before drawing.
     */
    fun generatePair(
        request: GenerationRequest.LinearPair,
        random: Random,
    ): GeneratedEquation.LinearPair {
        request.requireWithinInputLimit()
        val allowed = config.allowedCoefficients
        require(allowed.size >= 2) {
            "A pair with a unique solution needs at least two distinct allowed coefficients: ${config.coefficientRange}"
        }

        val a1 = config.randomCoefficient(random)
        val b1 = config.randomCoefficient(random)
        val a2 = config.randomCoefficient(random)
        val b2Candidates = allowed.filter { a1 * it != a2 * b1 }
        val b2 = b2Candidates[random.nextInt(b2Candidates.size)]

        return GeneratedEquation.LinearPair(
            first = lineThrough(a1, b1, request.x, request.y),
            second = lineThrough(a2, b2, request.x, request.y),
            x = request.x,
            y = request.y,
        )
    }

    /**
     * M5: three planes through `(x, y, z)` whose coefficient determinant is never zero, so the planes
     * meet in exactly that one point.
     *
     * All nine coefficients are drawn from the allowed range; the whole matrix is redrawn while the
     * determinant is zero. This stays seeded and deterministic (the same [Random] state always walks
     * the same draws) and is bounded by [MAX_SYSTEM_ATTEMPTS]. Needs at least two distinct allowed
     * coefficients, because a single value makes every matrix singular.
     *
     * @throws IllegalStateException if no non-singular matrix turns up within [MAX_SYSTEM_ATTEMPTS].
     */
    fun generateSystem3(
        request: GenerationRequest.LinearSystem3,
        random: Random,
    ): GeneratedEquation.LinearSystem3 {
        request.requireWithinInputLimit()
        require(config.allowedCoefficients.size >= 2) {
            "A system with a unique solution needs at least two distinct allowed coefficients: ${config.coefficientRange}"
        }

        repeat(MAX_SYSTEM_ATTEMPTS) {
            val equations = List(3) {
                val a = config.randomCoefficient(random)
                val b = config.randomCoefficient(random)
                val c = config.randomCoefficient(random)
                LinearEquation3D(a = a, b = b, c = c, d = a * request.x + b * request.y + c * request.z)
            }
            if (GeneratedEquation.LinearSystem3.determinantOf(equations) != 0L) {
                return GeneratedEquation.LinearSystem3(
                    equations = equations,
                    x = request.x,
                    y = request.y,
                    z = request.z,
                )
            }
        }
        throw IllegalStateException(
            "No non-singular 3x3 system found in $MAX_SYSTEM_ATTEMPTS attempts for ${config.coefficientRange}",
        )
    }

    private fun lineThrough(a: Int, b: Int, x: Int, y: Int): LinearEquation2D =
        LinearEquation2D(a = a, b = b, c = a * x + b * y)

    companion object {
        /** Upper bound on matrix redraws in [generateSystem3]. */
        const val MAX_SYSTEM_ATTEMPTS: Int = 1_000
    }
}
