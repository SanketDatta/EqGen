package com.sanket.eqgen.domain.generator

import com.sanket.eqgen.domain.model.GenerationConfig
import com.sanket.eqgen.domain.model.GenerationRequest
import com.sanket.eqgen.domain.model.GenerationResult
import kotlin.random.Random

/**
 * Entry point of the domain layer (spec section 04). It picks the right generator for a request,
 * owns seed handling, and implements Retry.
 *
 * @param seedSource supplies fresh seeds; inject a fixed sequence in tests.
 * @param maxRetryAttempts upper bound on regeneration attempts in [retry] (best effort, see [retry]).
 */
class EquationEngine(
    config: GenerationConfig = GenerationConfig(),
    private val seedSource: () -> Long = { Random.nextLong() },
    private val maxRetryAttempts: Int = DEFAULT_MAX_RETRY_ATTEMPTS,
) {
    private val polynomial = PolynomialGenerator(config)
    private val linearSystem = LinearSystemGenerator(config)

    init {
        require(maxRetryAttempts >= 1) { "maxRetryAttempts must be at least 1: $maxRetryAttempts" }
    }

    /**
     * Generates an equation for [request]. The same request and [seed] always give the same result.
     * Throws [IllegalArgumentException] if the request is outside the input limit.
     */
    fun generate(request: GenerationRequest, seed: Long = seedSource()): GenerationResult {
        val random = Random(seed)
        val equation = when (request) {
            is GenerationRequest.LinearOneVar -> polynomial.generateLinear(request, random)
            is GenerationRequest.LinearTwoVar -> linearSystem.generateTwoVar(request, random)
            is GenerationRequest.LinearPair -> linearSystem.generatePair(request, random)
            is GenerationRequest.LinearSystem3 -> linearSystem.generateSystem3(request, random)
            is GenerationRequest.Polynomial -> polynomial.generatePolynomial(request, random)
        }
        return GenerationResult(request = request, equation = equation, seed = seed)
    }

    /**
     * Retry (spec section 03): regenerates from the request stored in [previous], never from
     * whatever is currently typed in the UI, using a new seed.
     *
     * The new equation differs from the previous one whenever more than one outcome is possible.
     * Up to [maxRetryAttempts] seeds are tried; if every attempt reproduces the previous equation
     * (only possible when the config leaves a single outcome), the last attempt is returned.
     */
    fun retry(previous: GenerationResult): GenerationResult {
        var candidate = generate(previous.request)
        var attempts = 1
        while (candidate.equation == previous.equation && attempts < maxRetryAttempts) {
            candidate = generate(previous.request)
            attempts++
        }
        return candidate
    }

    companion object {
        const val DEFAULT_MAX_RETRY_ATTEMPTS: Int = 20
    }
}
