package com.sanket.eqgen.domain.graph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphModelsTest {

    @Test
    fun viewport_rejectsEmptyOrReversedRanges() {
        assertThrows(IllegalArgumentException::class.java) { Viewport(1.0, 1.0, 0.0, 1.0) }
        assertThrows(IllegalArgumentException::class.java) { Viewport(2.0, 1.0, 0.0, 1.0) }
        assertThrows(IllegalArgumentException::class.java) { Viewport(0.0, 1.0, 3.0, 3.0) }
        assertThrows(IllegalArgumentException::class.java) { Viewport(0.0, 1.0, 5.0, -5.0) }
    }

    @Test
    fun viewport_rejectsNonFiniteBounds() {
        assertThrows(IllegalArgumentException::class.java) { Viewport(Double.NaN, 1.0, 0.0, 1.0) }
        assertThrows(IllegalArgumentException::class.java) { Viewport(0.0, Double.POSITIVE_INFINITY, 0.0, 1.0) }
        assertThrows(IllegalArgumentException::class.java) { Viewport(0.0, 1.0, Double.NEGATIVE_INFINITY, 1.0) }
    }

    @Test
    fun viewport_exposesWidthAndHeight() {
        val viewport = Viewport(-2.0, 6.0, 1.0, 4.0)
        assertEquals(8.0, viewport.width, 0.0)
        assertEquals(3.0, viewport.height, 0.0)
    }

    @Test
    fun viewport_containsIsInclusiveOfBorder() {
        val viewport = Viewport(-1.0, 1.0, -2.0, 2.0)
        assertTrue(Point(0.0, 0.0) in viewport)
        assertTrue(Point(-1.0, 2.0) in viewport)
        assertTrue(Point(1.0, -2.0) in viewport)
        assertFalse(Point(1.0001, 0.0) in viewport)
        assertFalse(Point(0.0, -2.0001) in viewport)
    }
}
