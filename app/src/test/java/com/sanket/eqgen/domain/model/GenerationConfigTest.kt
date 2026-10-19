package com.sanket.eqgen.domain.model

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerationConfigTest {

    @Test
    fun default_isAsymmetricRangeWithoutZero() {
        val config = GenerationConfig()
        assertEquals(-10..20, config.coefficientRange)
        assertEquals((-10..-1).toList() + (1..20).toList(), config.allowedCoefficients)
        assertEquals(30, config.allowedCoefficients.size)
        assertFalse(0 in config.allowedCoefficients)
    }

    @Test
    fun customRange_excludesZero() {
        assertEquals(listOf(-1, 1), GenerationConfig(-1..1).allowedCoefficients)
        assertEquals(listOf(5), GenerationConfig(5..5).allowedCoefficients)
    }

    @Test
    fun emptyRange_isRejected() {
        assertThrows(IllegalArgumentException::class.java) { GenerationConfig(5..1) }
    }

    @Test
    fun rangeWithOnlyZero_isRejected() {
        assertThrows(IllegalArgumentException::class.java) { GenerationConfig(0..0) }
    }

    @Test
    fun rangeBeyondOverflowSafeLimit_isRejected() {
        assertThrows(IllegalArgumentException::class.java) { GenerationConfig(-1001..5) }
        assertThrows(IllegalArgumentException::class.java) { GenerationConfig(1..1001) }
        assertThrows(IllegalArgumentException::class.java) { GenerationConfig(Int.MIN_VALUE..Int.MAX_VALUE) }
        GenerationConfig(-1000..1000) // boundary is allowed
    }

    @Test
    fun randomCoefficient_staysInRange_neverZero_andCoversWholeRange() {
        val config = GenerationConfig()
        val random = Random(12345L)
        val seen = mutableSetOf<Int>()
        repeat(5_000) {
            val value = config.randomCoefficient(random)
            assertTrue(value != 0 && value in -10..20)
            seen += value
        }
        assertEquals(config.allowedCoefficients.toSet(), seen)
    }
}
