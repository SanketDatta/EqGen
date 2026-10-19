package com.sanket.eqgen.domain.generator

import com.sanket.eqgen.domain.model.GeneratedEquation
import com.sanket.eqgen.domain.model.GenerationConfig
import com.sanket.eqgen.domain.model.GenerationRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class EquationEngineTest {

    private val requests = listOf(
        GenerationRequest.LinearOneVar(3),
        GenerationRequest.LinearTwoVar(2, -5),
        GenerationRequest.LinearPair(-4, 7),
        GenerationRequest.Polynomial(2, 2),
        GenerationRequest.LinearSystem3(1, -2, 3),
        GenerationRequest.Polynomial(-1, 0, 4),
    )

    /** A seed source that counts up from [start]. */
    private fun counting(start: Long): () -> Long {
        var next = start
        return { next++ }
    }

    // ---- dispatch and seeds ----

    @Test
    fun generate_dispatchesEachRequestToItsGenerator() {
        val engine = EquationEngine()
        assertTrue(engine.generate(requests[0], 1L).equation is GeneratedEquation.LinearOneVar)
        assertTrue(engine.generate(requests[1], 1L).equation is GeneratedEquation.LinearTwoVar)
        assertTrue(engine.generate(requests[2], 1L).equation is GeneratedEquation.LinearPair)
        assertTrue(engine.generate(requests[3], 1L).equation is GeneratedEquation.Polynomial)
        assertTrue(engine.generate(requests[4], 1L).equation is GeneratedEquation.LinearSystem3)
        assertEquals(3, (engine.generate(requests[5], 1L).equation as GeneratedEquation.Polynomial).equation.degree)
    }

    @Test
    fun generate_keepsRequestAndRecordsGivenSeed() {
        val engine = EquationEngine()
        for (request in requests) {
            val result = engine.generate(request, seed = 42L)
            assertEquals(request, result.request)
            assertEquals(42L, result.seed)
        }
    }

    @Test
    fun generate_withoutSeedUsesSeedSource() {
        val engine = EquationEngine(seedSource = { 777L })
        assertEquals(777L, engine.generate(requests[0]).seed)
    }

    @Test
    fun generate_sameRequestAndSeedGiveIdenticalResult() {
        val first = EquationEngine()
        val second = EquationEngine()
        for (request in requests) for (seed in 0L until 100L) {
            assertEquals(first.generate(request, seed), second.generate(request, seed))
        }
    }

    @Test
    fun generate_differentSeedsGiveVariety() {
        val engine = EquationEngine()
        for (request in requests) {
            val distinct = (0L until 200L).map { engine.generate(request, it).equation }.toSet()
            assertTrue("request=$request distinct=${distinct.size}", distinct.size > 5)
        }
    }

    @Test
    fun generate_outOfRangeRequestThrows() {
        val engine = EquationEngine()
        assertThrows(IllegalArgumentException::class.java) {
            engine.generate(GenerationRequest.Polynomial(101, 0), 1L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            engine.generate(GenerationRequest.LinearOneVar(-101), 1L)
        }
    }

    @Test
    fun generate_pairWithSingleCoefficientConfigThrows() {
        val engine = EquationEngine(GenerationConfig(5..5))
        assertThrows(IllegalArgumentException::class.java) {
            engine.generate(GenerationRequest.LinearPair(1, 2), 1L)
        }
    }

    // ---- Retry ----

    @Test
    fun retry_keepsTheSameRequest() {
        for (request in requests) {
            val engine = EquationEngine(seedSource = counting(1_000L))
            val previous = engine.generate(request, seed = 1L)
            assertEquals(request, engine.retry(previous).request)
        }
    }

    @Test
    fun retry_producesADifferentEquation() {
        for (request in requests) for (trial in 0L until 300L) {
            val engine = EquationEngine(seedSource = counting(trial * 100_000L))
            val previous = engine.generate(request, seed = trial)
            val retried = engine.retry(previous)
            assertNotEquals("request=$request trial=$trial", previous.equation, retried.equation)
        }
    }

    @Test
    fun retry_stillSatisfiesTheRequestedSolution() {
        val engine = EquationEngine(seedSource = counting(5_000L))

        val one = engine.retry(engine.generate(GenerationRequest.LinearOneVar(3), 1L))
        one.equation as GeneratedEquation.LinearOneVar
        assertEquals(0, one.equation.equation.residual(3))

        val quadratic = engine.retry(engine.generate(GenerationRequest.Polynomial(-2, 9), 1L))
        quadratic.equation as GeneratedEquation.Polynomial
        assertEquals(0L, quadratic.equation.equation.residual(-2))
        assertEquals(0L, quadratic.equation.equation.residual(9))

        val cubic = engine.retry(engine.generate(GenerationRequest.Polynomial(-2, 9, 9), 1L))
        cubic.equation as GeneratedEquation.Polynomial
        for (r in listOf(-2, 9)) assertEquals(0L, cubic.equation.equation.residual(r))

        val system = engine.retry(engine.generate(GenerationRequest.LinearSystem3(1, -2, 3), 1L))
        system.equation as GeneratedEquation.LinearSystem3
        assertEquals(listOf(1, -2, 3), listOf(system.equation.x, system.equation.y, system.equation.z))
        for (e in system.equation.equations) assertEquals(e.d, e.a * 1 + e.b * -2 + e.c * 3)

        val pair = engine.retry(engine.generate(GenerationRequest.LinearPair(6, -1), 1L))
        pair.equation as GeneratedEquation.LinearPair
        assertEquals(0, pair.equation.first.residual(6, -1))
        assertEquals(0, pair.equation.second.residual(6, -1))
    }

    @Test
    fun retry_recordsTheSeedItUsed_soTheResultIsReproducible() {
        val engine = EquationEngine(seedSource = counting(9_000L))
        val previous = engine.generate(GenerationRequest.Polynomial(1, 4), seed = 3L)
        val retried = engine.retry(previous)
        assertTrue(retried.seed >= 9_000L)
        assertEquals(retried, EquationEngine().generate(retried.request, retried.seed))
    }

    @Test
    fun retry_whenOnlyOneOutcomeExists_stopsAtMaxAttemptsAndReturnsIt() {
        var calls = 0
        val engine = EquationEngine(
            config = GenerationConfig(5..5),
            seedSource = { (calls++).toLong() },
            maxRetryAttempts = 4,
        )
        val previous = engine.generate(GenerationRequest.LinearOneVar(2), seed = 1L)
        assertEquals(0, calls) // an explicit seed does not consume the seed source

        val retried = engine.retry(previous)

        assertEquals(4, calls)
        assertEquals(previous.equation, retried.equation)
        assertEquals(previous.request, retried.request)
    }

    @Test
    fun retry_stopsAsSoonAsTheEquationDiffers() {
        var calls = 0
        val engine = EquationEngine(seedSource = { (calls++).toLong() }, maxRetryAttempts = 50)
        val previous = engine.generate(GenerationRequest.LinearOneVar(2), seed = 1L)
        val retried = engine.retry(previous)
        assertNotEquals(previous.equation, retried.equation)
        assertTrue("calls=$calls", calls in 1..50)
    }

    @Test
    fun constructor_rejectsNonPositiveMaxRetryAttempts() {
        assertThrows(IllegalArgumentException::class.java) { EquationEngine(maxRetryAttempts = 0) }
    }
}
