package com.sanket.eqgen.domain.graph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphTransformTest {

    private val viewport = Viewport(xMin = -10.0, xMax = 10.0, yMin = -5.0, yMax = 15.0)
    private val transform = GraphTransform(viewport, widthPx = 400f, heightPx = 200f)

    @Test
    fun corners_mapToCanvasCorners_withYFlipped() {
        // top-left of the graph is (xMin, yMax); bottom-right is (xMax, yMin)
        assertEquals(0f, transform.pixelX(-10.0), 1e-4f)
        assertEquals(0f, transform.pixelY(15.0), 1e-4f)
        assertEquals(400f, transform.pixelX(10.0), 1e-4f)
        assertEquals(200f, transform.pixelY(-5.0), 1e-4f)
    }

    @Test
    fun origin_mapsToTheRightPlace() {
        assertEquals(200f, transform.pixelX(0.0), 1e-4f) // halfway across
        assertEquals(150f, transform.pixelY(0.0), 1e-4f) // y = 0 is 15 of 20 units below the top
    }

    @Test
    fun largerY_isHigherOnScreen() {
        assertTrue(transform.pixelY(5.0) < transform.pixelY(1.0))
        assertTrue(transform.pixelX(5.0) > transform.pixelX(1.0))
    }

    @Test
    fun outsideTheViewport_isNotClampedByDefault() {
        assertEquals(-20f, transform.pixelX(-11.0), 1e-3f)
        assertEquals(420f, transform.pixelX(11.0), 1e-3f)
        assertEquals(-10f, transform.pixelY(16.0), 1e-3f)
    }

    @Test
    fun overscan_limitsFarOffScreenPoints() {
        assertEquals(-100f, transform.pixelX(-1e9, overscan = 100f), 0f)
        assertEquals(500f, transform.pixelX(1e9, overscan = 100f), 0f)
        assertEquals(-100f, transform.pixelY(1e12, overscan = 100f), 0f)
        assertEquals(300f, transform.pixelY(-1e12, overscan = 100f), 0f)
        // Points within the overscan margin are untouched.
        assertEquals(-20f, transform.pixelX(-11.0, overscan = 100f), 1e-3f)
    }

    @Test
    fun overscan_keepsHugeParabolaValuesFinite() {
        val big = transform.pixelY(2.0e7, overscan = 800f)
        assertTrue(big.isFinite())
        assertEquals(-800f, big, 0f)
    }

    @Test
    fun rejectsNonPositiveOrNonFiniteCanvasSizes() {
        assertThrows(IllegalArgumentException::class.java) { GraphTransform(viewport, 0f, 100f) }
        assertThrows(IllegalArgumentException::class.java) { GraphTransform(viewport, 100f, -1f) }
        assertThrows(IllegalArgumentException::class.java) { GraphTransform(viewport, Float.NaN, 100f) }
        assertThrows(IllegalArgumentException::class.java) { GraphTransform(viewport, 100f, Float.POSITIVE_INFINITY) }
    }
}
