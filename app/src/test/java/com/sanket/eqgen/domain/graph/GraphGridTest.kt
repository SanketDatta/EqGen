package com.sanket.eqgen.domain.graph

import com.sanket.eqgen.domain.generator.EquationEngine
import com.sanket.eqgen.domain.model.GenerationRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

class GraphGridTest {

    // ---- niceStep ----

    @Test
    fun niceStep_knownValues() {
        assertEquals(2.0, GraphGrid.niceStep(10.0), 1e-12)
        assertEquals(20.0, GraphGrid.niceStep(120.0), 1e-12)
        assertEquals(1.0, GraphGrid.niceStep(6.0), 1e-12)
        assertEquals(5.0, GraphGrid.niceStep(30.0), 1e-12)
        assertEquals(10.0, GraphGrid.niceStep(45.0), 1e-12)
        assertEquals(200.0, GraphGrid.niceStep(1000.0), 1e-9)
        assertEquals(0.1, GraphGrid.niceStep(0.7), 1e-12)
    }

    @Test
    fun niceStep_isOneTwoOrFiveTimesAPowerOfTen_acrossManyMagnitudes() {
        var span = 0.001
        while (span < 1e7) {
            val step = GraphGrid.niceStep(span)
            val mantissa = step / 10.0.pow(floor(log10(step)))
            assertTrue(
                "span=$span step=$step mantissa=$mantissa",
                listOf(1.0, 2.0, 5.0, 10.0).any { abs(mantissa - it) < 1e-9 },
            )
            span *= 1.37
        }
    }

    @Test
    fun niceStep_givesRoughlyTheTargetNumberOfIntervals() {
        for (target in listOf(3, 6, 10)) {
            var span = 0.001
            while (span < 1e7) {
                val intervals = span / GraphGrid.niceStep(span, target)
                assertTrue(
                    "span=$span target=$target intervals=$intervals",
                    intervals >= 0.6 * target - 1e-9 && intervals <= 1.5 * target + 1e-9,
                )
                span *= 1.37
            }
        }
    }

    @Test
    fun niceStep_rejectsInvalidInput() {
        assertThrows(IllegalArgumentException::class.java) { GraphGrid.niceStep(0.0) }
        assertThrows(IllegalArgumentException::class.java) { GraphGrid.niceStep(-1.0) }
        assertThrows(IllegalArgumentException::class.java) { GraphGrid.niceStep(Double.NaN) }
        assertThrows(IllegalArgumentException::class.java) { GraphGrid.niceStep(Double.POSITIVE_INFINITY) }
        assertThrows(IllegalArgumentException::class.java) { GraphGrid.niceStep(10.0, targetIntervals = 0) }
    }

    // ---- ticks ----

    @Test
    fun ticks_areTheMultiplesOfStepInsideTheRange() {
        assertEquals(listOf(-4.0, -2.0, 0.0, 2.0, 4.0), GraphGrid.ticks(-5.0, 5.0, 2.0))
        assertEquals(listOf(0.0, 0.25, 0.5, 0.75, 1.0), GraphGrid.ticks(0.0, 1.0, 0.25))
        assertEquals(listOf(-10.0, 0.0, 10.0), GraphGrid.ticks(-10.0, 10.0, 10.0))
        assertEquals(listOf(100.0, 120.0), GraphGrid.ticks(95.0, 125.0, 20.0))
    }

    @Test
    fun ticks_canBeEmpty() {
        assertTrue(GraphGrid.ticks(1.0, 1.5, 2.0).isEmpty())
    }

    @Test
    fun ticks_rejectInvalidInput() {
        assertThrows(IllegalArgumentException::class.java) { GraphGrid.ticks(0.0, 1.0, 0.0) }
        assertThrows(IllegalArgumentException::class.java) { GraphGrid.ticks(0.0, 1.0, -1.0) }
        assertThrows(IllegalArgumentException::class.java) { GraphGrid.ticks(2.0, 1.0, 1.0) }
        assertThrows(IllegalArgumentException::class.java) { GraphGrid.ticks(Double.NaN, 1.0, 1.0) }
        assertThrows(IllegalArgumentException::class.java) { GraphGrid.ticks(0.0, 1e9, 1.0) }
    }

    @Test
    fun ticks_forEveryFittedViewport_areFewAndIncludeZero() {
        val engine = EquationEngine()
        val coordinates = listOf(-100, -23, -1, 0, 1, 8, 100)
        val requests = buildList {
            for (c in coordinates) add(GenerationRequest.LinearOneVar(c))
            for (x in coordinates) for (y in coordinates) {
                add(GenerationRequest.LinearTwoVar(x, y))
                add(GenerationRequest.LinearPair(x, y))
                add(GenerationRequest.Polynomial(x, y))
                for (z in listOf(-100, -1, 0, 100)) add(GenerationRequest.Polynomial(x, y, z))
            }
        }
        for (request in requests) for (seed in 0L until 3L) {
            val viewport = GraphSampler.fitViewport(engine.generate(request, seed).equation)
            for ((min, max) in listOf(viewport.xMin to viewport.xMax, viewport.yMin to viewport.yMax)) {
                val step = GraphGrid.niceStep(max - min)
                val ticks = GraphGrid.ticks(min, max, step)
                assertTrue("request=$request ticks=${ticks.size}", ticks.size in 3..12)
                assertTrue("request=$request range=$min..$max", 0.0 in ticks)
                assertTrue(ticks.zipWithNext().all { (a, b) -> a < b })
                assertTrue(ticks.all { it >= min && it <= max })
            }
        }
    }

    // ---- formatTick ----

    @Test
    fun formatTick_usesAsManyDecimalsAsTheStepNeeds() {
        assertEquals("2", GraphGrid.formatTick(2.0, 2.0))
        assertEquals("-4", GraphGrid.formatTick(-4.0, 2.0))
        assertEquals("100", GraphGrid.formatTick(100.0, 20.0))
        assertEquals("0.5", GraphGrid.formatTick(0.5, 0.5))
        assertEquals("0.3", GraphGrid.formatTick(0.30000000000000004, 0.1))
        assertEquals("0.25", GraphGrid.formatTick(0.25, 0.05))
        assertEquals("-0.2", GraphGrid.formatTick(-0.2, 0.2))
    }

    @Test
    fun formatTick_neverShowsNegativeZero() {
        assertEquals("0", GraphGrid.formatTick(-0.0, 2.0))
        assertEquals("0", GraphGrid.formatTick(0.0, 2.0))
        assertEquals("0.0", GraphGrid.formatTick(-0.0, 0.5))
    }

    @Test
    fun formatTick_ofGeneratedTicks_isExact() {
        val step = 0.1
        val labels = GraphGrid.ticks(0.0, 1.0, step).map { GraphGrid.formatTick(it, step) }
        assertEquals(listOf("0.0", "0.1", "0.2", "0.3", "0.4", "0.5", "0.6", "0.7", "0.8", "0.9", "1.0"), labels)
    }

    // ---- formatCoordinate ----

    @Test
    fun formatCoordinate_wholeNumbersHaveNoDecimals() {
        assertEquals("3", GraphGrid.formatCoordinate(3.0))
        assertEquals("-2", GraphGrid.formatCoordinate(-2.0))
        assertEquals("0", GraphGrid.formatCoordinate(0.0))
        assertEquals("0", GraphGrid.formatCoordinate(-0.0))
        assertEquals("1234567", GraphGrid.formatCoordinate(1234567.0))
        assertEquals("0", GraphGrid.formatCoordinate(0.004))
    }

    @Test
    fun formatCoordinate_trimsTrailingZeros() {
        assertEquals("2.5", GraphGrid.formatCoordinate(2.5))
        assertEquals("2.05", GraphGrid.formatCoordinate(2.05))
        assertEquals("0.3", GraphGrid.formatCoordinate(0.30000000000000004))
        assertEquals("-1.75", GraphGrid.formatCoordinate(-1.75))
    }

    @Test
    fun formatting_ignoresTheDeviceLocale() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("0.5", GraphGrid.formatTick(0.5, 0.5))
            assertEquals("2.5", GraphGrid.formatCoordinate(2.5))
        } finally {
            Locale.setDefault(original)
        }
    }
}
