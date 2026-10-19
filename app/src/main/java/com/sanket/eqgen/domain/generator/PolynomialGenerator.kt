package com.sanket.eqgen.domain.generator

import com.sanket.eqgen.domain.model.GeneratedEquation
import com.sanket.eqgen.domain.model.GenerationConfig
import com.sanket.eqgen.domain.model.GenerationRequest
import com.sanket.eqgen.domain.model.LinearEquation1D
import com.sanket.eqgen.domain.model.PolynomialEquation
import com.sanket.eqgen.domain.model.requireWithinInputLimit
import kotlin.random.Random

/**
 * One-variable generators: M1 (linear) and M4/M6 (quadratic and cubic).
 *
 * Pure and deterministic: the same request, config and [Random] state give the same result.
 * Throws [IllegalArgumentException] if the request is outside the input limit.
 */
class PolynomialGenerator(private val config: GenerationConfig = GenerationConfig()) {

    /** M1: draws `a`, then derives `b = −a·root` so that `a·root + b = 0`. */
    fun generateLinear(
        request: GenerationRequest.LinearOneVar,
        random: Random,
    ): GeneratedEquation.LinearOneVar {
        request.requireWithinInputLimit()
        val a = config.randomCoefficient(random)
        val equation = LinearEquation1D(a = a, b = -a * request.root)
        return GeneratedEquation.LinearOneVar(equation = equation, root = request.root)
    }

    /**
     * M4 and M6: draws the leading coefficient `a`; every other coefficient follows from the roots
     * (see [PolynomialEquation]). The roots are used exactly as given, repeats included.
     */
    fun generatePolynomial(
        request: GenerationRequest.Polynomial,
        random: Random,
    ): GeneratedEquation.Polynomial {
        request.requireWithinInputLimit()
        val a = config.randomCoefficient(random)
        val equation = PolynomialEquation(roots = request.roots.toList(), leadingCoefficient = a)
        return GeneratedEquation.Polynomial(equation)
    }
}
