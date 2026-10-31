package com.sanket.eqgen.domain.graph

import com.sanket.eqgen.domain.generator.EquationEngine
import com.sanket.eqgen.domain.model.GeneratedEquation
import com.sanket.eqgen.domain.model.GenerationRequest
import com.sanket.eqgen.domain.model.LinearEquation1D
import com.sanket.eqgen.domain.model.LinearEquation2D
import com.sanket.eqgen.domain.model.PolynomialEquation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max

class GraphSamplerTest {

    private val engine = EquationEngine()

    /** Includes zero, negatives, and both input limits. */
    private val coordinates = listOf(-100, -23, -1, 0, 1, 8, 100)
    private val seeds = 0L until 20L
    private val origin = Point(0.0, 0.0)

    /** Zoomed in, zoomed far out, and panned away from the origin. */
    private val otherViewports = listOf(
        Viewport(-3.0, 4.0, -2.0, 9.0),
        Viewport(-1000.0, 1000.0, -1000.0, 1000.0),
        Viewport(50.0, 60.0, -5.0, 5.0),
    )

    private inline fun <reified T : GeneratedEquation> generated(request: GenerationRequest, seed: Long): T =
        engine.generate(request, seed).equation as T

    private fun assertOnLine(equation: LinearEquation2D, point: Point, message: String = "") {
        val residual = equation.a * point.x + equation.b * point.y - equation.c
        assertEquals("$message point=$point", 0.0, residual, 1e-6)
    }

    private fun assertOnCurve(equation: PolynomialEquation, point: Point) {
        val expected = equation.coefficients.fold(0.0) { acc, c -> acc * point.x + c }
        assertEquals("point=$point", expected, point.y, 1e-6 * max(1.0, abs(expected)))
    }

    private fun assertMinimumSpan(viewport: Viewport) {
        assertTrue("viewport=$viewport", viewport.width >= GraphSampler.MIN_SPAN - 1e-9)
        assertTrue("viewport=$viewport", viewport.height >= GraphSampler.MIN_SPAN - 1e-9)
    }

    // ---- fitViewport ----

    @Test
    fun fit_linearOneVar_showsRootAndOrigin() {
        for (root in coordinates) for (seed in seeds) {
            val equation = generated<GeneratedEquation.LinearOneVar>(GenerationRequest.LinearOneVar(root), seed)
            val viewport = GraphSampler.fitViewport(equation)
            assertTrue("root=$root seed=$seed $viewport", Point(root.toDouble(), 0.0) in viewport)
            assertTrue("root=$root seed=$seed $viewport", origin in viewport)
            assertMinimumSpan(viewport)
        }
    }

    @Test
    fun fit_linearOneVar_lineCrossesFullWidthInsideViewport() {
        for (root in coordinates) for (seed in seeds) {
            val equation = generated<GeneratedEquation.LinearOneVar>(GenerationRequest.LinearOneVar(root), seed)
            val viewport = GraphSampler.fitViewport(equation)
            val line = equation.equation
            assertTrue(line.a * viewport.xMin + line.b in viewport.yMin..viewport.yMax)
            assertTrue(line.a * viewport.xMax + line.b in viewport.yMin..viewport.yMax)
        }
    }

    @Test
    fun fit_linearTwoVar_showsSolutionAndOrigin() {
        for (x in coordinates) for (y in coordinates) for (seed in seeds) {
            val equation = generated<GeneratedEquation.LinearTwoVar>(GenerationRequest.LinearTwoVar(x, y), seed)
            val viewport = GraphSampler.fitViewport(equation)
            assertTrue("x=$x y=$y $viewport", Point(x.toDouble(), y.toDouble()) in viewport)
            assertTrue(origin in viewport)
            assertMinimumSpan(viewport)
        }
    }

    @Test
    fun fit_linearPair_showsIntersectionAndOrigin() {
        for (x in coordinates) for (y in coordinates) for (seed in seeds) {
            val equation = generated<GeneratedEquation.LinearPair>(GenerationRequest.LinearPair(x, y), seed)
            val viewport = GraphSampler.fitViewport(equation)
            assertTrue("x=$x y=$y $viewport", Point(x.toDouble(), y.toDouble()) in viewport)
            assertTrue(origin in viewport)
            assertMinimumSpan(viewport)
        }
    }

    @Test
    fun fit_quadratic_showsRootsVertexAndOrigin() {
        for (r1 in coordinates) for (r2 in coordinates) for (seed in seeds) {
            val equation = generated<GeneratedEquation.Polynomial>(GenerationRequest.Polynomial(r1, r2), seed)
            val a = equation.equation.leadingCoefficient
            val viewport = GraphSampler.fitViewport(equation)
            val vertex = Point((r1 + r2) / 2.0, -a * (r1 - r2).toDouble() * (r1 - r2) / 4.0)
            assertTrue("r1=$r1 r2=$r2 $viewport", Point(r1.toDouble(), 0.0) in viewport)
            assertTrue("r1=$r1 r2=$r2 $viewport", Point(r2.toDouble(), 0.0) in viewport)
            assertTrue("vertex=$vertex $viewport", vertex in viewport)
            assertTrue(origin in viewport)
            assertMinimumSpan(viewport)
        }
    }

    @Test
    fun fit_quadratic_leavesRoomOnTheOpeningSide() {
        val opensUp = GeneratedEquation.Polynomial(PolynomialEquation(listOf(1, 5), 2))
        val opensDown = GeneratedEquation.Polynomial(PolynomialEquation(listOf(1, 5), -2))
        val up = GraphSampler.fitViewport(opensUp)
        val down = GraphSampler.fitViewport(opensDown)
        // The vertex is at y = -8 (opens up) or y = +8 (opens down). The bounding box of {vertex, axis}
        // stops just past y = 0 on the opening side, so the arms need clearly more room than that.
        assertTrue("up=$up", up.yMax > 5.0)
        assertTrue("down=$down", down.yMin < -5.0)
    }

    @Test
    fun fit_isDeterministic() {
        val equation = generated<GeneratedEquation.Polynomial>(GenerationRequest.Polynomial(-4, 9), 5L)
        assertEquals(GraphSampler.fitViewport(equation), GraphSampler.fitViewport(equation))
    }

    // ---- sample: M1 ----

    @Test
    fun sample_linearOneVar_isTheLineThroughTheRoot() {
        for (root in coordinates) for (seed in seeds) {
            val equation = generated<GeneratedEquation.LinearOneVar>(GenerationRequest.LinearOneVar(root), seed)
            val viewport = GraphSampler.fitViewport(equation)
            val plot = GraphSampler.sample(equation, viewport)

            assertEquals(1, plot.polylines.size)
            val line = plot.polylines.single()
            assertEquals(2, line.size)
            assertEquals(viewport.xMin, line.first().x, 1e-9)
            assertEquals(viewport.xMax, line.last().x, 1e-9)
            for (point in line) {
                assertEquals("root=$root seed=$seed", equation.equation.a * point.x + equation.equation.b, point.y, 1e-6)
                assertTrue(point in viewport)
            }
            assertEquals(listOf(Point(root.toDouble(), 0.0)), plot.markers)
        }
    }

    // ---- sample: M2 ----

    @Test
    fun sample_linearTwoVar_lineThroughTheSolution() {
        for (x in coordinates) for (y in coordinates) for (seed in seeds) {
            val equation = generated<GeneratedEquation.LinearTwoVar>(GenerationRequest.LinearTwoVar(x, y), seed)
            val viewport = GraphSampler.fitViewport(equation)
            val plot = GraphSampler.sample(equation, viewport)

            assertEquals("x=$x y=$y seed=$seed", 1, plot.polylines.size)
            val line = plot.polylines.single()
            assertEquals(2, line.size)
            for (point in line) {
                assertOnLine(equation.equation, point, "x=$x y=$y seed=$seed")
                assertTrue(point in viewport)
            }
            assertEquals(listOf(Point(x.toDouble(), y.toDouble())), plot.markers)
            assertOnLine(equation.equation, plot.markers.single())
        }
    }

    // ---- sample: M3 ----

    @Test
    fun sample_linearPair_twoLinesMeetAtTheMarker() {
        for (x in coordinates) for (y in coordinates) for (seed in seeds) {
            val equation = generated<GeneratedEquation.LinearPair>(GenerationRequest.LinearPair(x, y), seed)
            val viewport = GraphSampler.fitViewport(equation)
            val plot = GraphSampler.sample(equation, viewport)

            assertEquals("x=$x y=$y seed=$seed", 2, plot.polylines.size)
            plot.polylines[0].forEach { assertOnLine(equation.first, it, "first") }
            plot.polylines[1].forEach { assertOnLine(equation.second, it, "second") }
            assertEquals(listOf(Point(x.toDouble(), y.toDouble())), plot.markers)
            assertOnLine(equation.first, plot.markers.single())
            assertOnLine(equation.second, plot.markers.single())
        }
    }

    // ---- sample: M4 ----

    @Test
    fun sample_quadratic_pointsLieOnTheParabola_inAscendingX() {
        for (r1 in coordinates) for (r2 in coordinates) for (seed in seeds) {
            val equation = generated<GeneratedEquation.Polynomial>(GenerationRequest.Polynomial(r1, r2), seed)
            val viewport = GraphSampler.fitViewport(equation)
            val plot = GraphSampler.sample(equation, viewport)

            assertEquals(1, plot.polylines.size)
            val curve = plot.polylines.single()
            assertTrue(curve.size >= GraphSampler.DEFAULT_SAMPLE_COUNT)
            assertEquals(viewport.xMin, curve.first().x, 1e-9)
            assertEquals(viewport.xMax, curve.last().x, 1e-9)
            curve.forEach { assertOnCurve(equation.equation, it) }
            curve.zipWithNext().forEach { (left, right) -> assertTrue(left.x < right.x) }
        }
    }

    @Test
    fun sample_quadratic_includesRootsAndVertexExactly() {
        for (r1 in coordinates) for (r2 in coordinates) {
            val equation = generated<GeneratedEquation.Polynomial>(GenerationRequest.Polynomial(r1, r2), 3L)
            val curve = GraphSampler.sample(equation, GraphSampler.fitViewport(equation)).polylines.single()
            val xs = curve.map { it.x }.toSet()
            assertTrue("r1=$r1", r1.toDouble() in xs)
            assertTrue("r2=$r2", r2.toDouble() in xs)
            assertTrue("vertex", (r1 + r2) / 2.0 in xs)
            assertEquals(0.0, curve.first { it.x == r1.toDouble() }.y, 1e-9)
            assertEquals(0.0, curve.first { it.x == r2.toDouble() }.y, 1e-9)
        }
    }

    @Test
    fun sample_quadratic_markersAreTheDistinctRoots() {
        val distinct = generated<GeneratedEquation.Polynomial>(GenerationRequest.Polynomial(-2, 7), 1L)
        assertEquals(
            listOf(Point(-2.0, 0.0), Point(7.0, 0.0)),
            GraphSampler.sample(distinct, GraphSampler.fitViewport(distinct)).markers,
        )
        val equal = generated<GeneratedEquation.Polynomial>(GenerationRequest.Polynomial(4, 4), 1L)
        assertEquals(
            listOf(Point(4.0, 0.0)),
            GraphSampler.sample(equal, GraphSampler.fitViewport(equal)).markers,
        )
    }

    @Test
    fun sample_quadratic_respectsSampleCount() {
        val equation = generated<GeneratedEquation.Polynomial>(GenerationRequest.Polynomial(1, 2), 1L)
        val viewport = Viewport(-100.0, 100.0, -10.0, 10.0)
        val curve = GraphSampler.sample(equation, viewport, sampleCount = 11).polylines.single()
        // 11 grid points plus the exact roots and vertex, none of which fall on the grid.
        assertEquals(14, curve.size)
        assertEquals(2, GraphSampler.sample(equation, viewport, sampleCount = 2).polylines.single().size - 3)
    }

    // ---- sample: consistency across viewports (pan/zoom readiness) ----

    @Test
    fun sample_overAnyViewport_everyPointStillSatisfiesTheEquation() {
        for (viewport in otherViewports) for (seed in 0L until 60L) {
            val one = generated<GeneratedEquation.LinearOneVar>(GenerationRequest.LinearOneVar(7), seed)
            GraphSampler.sample(one, viewport).polylines.flatten().forEach { point ->
                assertEquals(one.equation.a * point.x + one.equation.b, point.y, 1e-6)
                assertTrue(point in viewport)
            }

            val two = generated<GeneratedEquation.LinearTwoVar>(GenerationRequest.LinearTwoVar(-5, 12), seed)
            GraphSampler.sample(two, viewport).polylines.flatten().forEach { point ->
                assertOnLine(two.equation, point)
                assertTrue(point in viewport)
            }

            val pair = generated<GeneratedEquation.LinearPair>(GenerationRequest.LinearPair(30, -40), seed)
            val pairPlot = GraphSampler.sample(pair, viewport)
            for (point in pairPlot.polylines.flatten()) assertTrue(point in viewport)

            val quadratic = generated<GeneratedEquation.Polynomial>(GenerationRequest.Polynomial(-8, 3), seed)
            val curve = GraphSampler.sample(quadratic, viewport).polylines.single()
            curve.forEach { assertOnCurve(quadratic.equation, it) }
            assertEquals(viewport.xMin, curve.first().x, 1e-9)
            assertEquals(viewport.xMax, curve.last().x, 1e-9)
        }
    }

    @Test
    fun sample_markersAreIndependentOfTheViewport() {
        val equation = generated<GeneratedEquation.LinearPair>(GenerationRequest.LinearPair(90, -80), 2L)
        val expected = listOf(Point(90.0, -80.0))
        assertEquals(expected, GraphSampler.sample(equation, GraphSampler.fitViewport(equation)).markers)
        for (viewport in otherViewports) {
            assertEquals(expected, GraphSampler.sample(equation, viewport).markers)
        }
    }

    // ---- sample: line clipping edge cases ----

    @Test
    fun sample_lineMissingTheViewport_hasNoPolyline() {
        // x + y = 0 never reaches the square [10, 20] x [10, 20].
        val equation = GeneratedEquation.LinearTwoVar(LinearEquation2D(a = 1, b = 1, c = 0), x = 1, y = -1)
        val plot = GraphSampler.sample(equation, Viewport(10.0, 20.0, 10.0, 20.0))
        assertTrue(plot.polylines.isEmpty())
        assertEquals(listOf(Point(1.0, -1.0)), plot.markers)
    }

    @Test
    fun sample_lineTouchingOnlyACorner_hasNoPolyline() {
        // x + y = 0 touches [0, 5] x [0, 5] only at the origin corner.
        val equation = GeneratedEquation.LinearTwoVar(LinearEquation2D(a = 1, b = 1, c = 0), x = 0, y = 0)
        val plot = GraphSampler.sample(equation, Viewport(0.0, 5.0, 0.0, 5.0))
        assertTrue(plot.polylines.isEmpty())
    }

    @Test
    fun sample_horizontalLine_spansTheViewportWidth() {
        // 0·x + 2·y = 6  ->  y = 3
        val equation = GeneratedEquation.LinearTwoVar(LinearEquation2D(a = 0, b = 2, c = 6), x = 0, y = 3)
        val line = GraphSampler.sample(equation, Viewport(-4.0, 8.0, -1.0, 5.0)).polylines.single()
        assertEquals(listOf(Point(-4.0, 3.0), Point(8.0, 3.0)), line)
    }

    @Test
    fun sample_verticalLine_spansTheViewportHeight() {
        // 3·x + 0·y = 6  ->  x = 2
        val equation = GeneratedEquation.LinearTwoVar(LinearEquation2D(a = 3, b = 0, c = 6), x = 2, y = 0)
        val line = GraphSampler.sample(equation, Viewport(-4.0, 8.0, -1.0, 5.0)).polylines.single()
        assertEquals(listOf(Point(2.0, -1.0), Point(2.0, 5.0)), line)
    }

    @Test
    fun sample_diagonalLine_isCutAtTheViewportEdges() {
        // y = x crosses [-2, 6] x [-5, 3] from (-2, -2) to (3, 3).
        val equation = GeneratedEquation.LinearOneVar(LinearEquation1D(a = 1, b = 0), root = 0)
        val line = GraphSampler.sample(equation, Viewport(-2.0, 6.0, -5.0, 3.0)).polylines.single()
        assertEquals(listOf(Point(-2.0, -2.0), Point(3.0, 3.0)), line)
    }

    // ---- cubic (M6) and the no-graph system (M5) ----

    private val cubicRoots = listOf(-100, -23, 0, 1, 8, 100)

    @Test
    fun fit_cubic_showsRootsExtremaOriginAndBothArms() {
        for (r1 in cubicRoots) for (r2 in cubicRoots) for (r3 in cubicRoots) for (seed in 0L until 3L) {
            val equation = generated<GeneratedEquation.Polynomial>(GenerationRequest.Polynomial(r1, r2, r3), seed)
            val viewport = GraphSampler.fitViewport(equation)
            for (r in listOf(r1, r2, r3)) assertTrue("roots=$r1,$r2,$r3 $viewport", Point(r.toDouble(), 0.0) in viewport)
            assertTrue(origin in viewport)
            assertMinimumSpan(viewport)
            // every extremum inside the x-range is inside the y-range
            val c = equation.equation.coefficients.map { it.toDouble() }
            val disc = c[1] * c[1] - 3 * c[0] * c[2]
            if (disc > 0) for (sign in listOf(-1.0, 1.0)) {
                val x = (-c[1] + sign * kotlin.math.sqrt(disc)) / (3 * c[0])
                if (x in viewport.xMin..viewport.xMax) {
                    val y = c.fold(0.0) { acc, k -> acc * x + k }
                    assertTrue("extremum ($x, $y) $viewport", y in viewport.yMin..viewport.yMax)
                }
            }
            // both ends of the curve are on screen
            for (x in listOf(viewport.xMin, viewport.xMax)) {
                val y = equation.equation.coefficients.fold(0.0) { acc, k -> acc * x + k }
                assertTrue("end ($x, $y) $viewport", y in viewport.yMin..viewport.yMax)
            }
        }
    }

    @Test
    fun sample_cubic_pointsLieOnTheCurve_inAscendingX_andIncludeRootsAndExtrema() {
        for (r1 in cubicRoots) for (r2 in cubicRoots) for (r3 in cubicRoots) {
            val equation = generated<GeneratedEquation.Polynomial>(GenerationRequest.Polynomial(r1, r2, r3), 2L)
            val viewport = GraphSampler.fitViewport(equation)
            val plot = GraphSampler.sample(equation, viewport)
            val curve = plot.polylines.single()
            curve.forEach { assertOnCurve(equation.equation, it) }
            curve.zipWithNext().forEach { (l, r) -> assertTrue(l.x < r.x) }
            assertEquals(viewport.xMin, curve.first().x, 1e-9)
            assertEquals(viewport.xMax, curve.last().x, 1e-9)
            val xs = curve.map { it.x }.toSet()
            for (r in listOf(r1, r2, r3)) assertTrue("root $r", r.toDouble() in xs)
            assertEquals(listOf(r1, r2, r3).map { Point(it.toDouble(), 0.0) }.distinct(), plot.markers)
        }
    }

    @Test
    fun sample_cubic_withRepeatedRoots_hasOneMarkerPerDistinctRoot() {
        val triple = generated<GeneratedEquation.Polynomial>(GenerationRequest.Polynomial(4, 4, 4), 1L)
        assertEquals(listOf(Point(4.0, 0.0)), GraphSampler.sample(triple, GraphSampler.fitViewport(triple)).markers)
        val double = generated<GeneratedEquation.Polynomial>(GenerationRequest.Polynomial(4, 4, -2), 1L)
        assertEquals(
            listOf(Point(4.0, 0.0), Point(-2.0, 0.0)),
            GraphSampler.sample(double, GraphSampler.fitViewport(double)).markers,
        )
    }

    @Test
    fun sample_cubic_extremaAreHitExactly() {
        // (x+1)(x-1)(x) * 1 = x^3 - x: extrema at x = ±1/sqrt(3).
        val equation = generated<GeneratedEquation.Polynomial>(GenerationRequest.Polynomial(-1, 1, 0), 1L)
        val a = equation.equation.leadingCoefficient.toDouble()
        val curve = GraphSampler.sample(equation, GraphSampler.fitViewport(equation)).polylines.single()
        val x = 1.0 / kotlin.math.sqrt(3.0)
        assertTrue(curve.any { abs(it.x - x) < 1e-9 })
        assertTrue(curve.any { abs(it.x + x) < 1e-9 })
        assertTrue(a != 0.0)
    }

    @Test
    fun hasGraph_isFalseOnlyForTheThreeVariableSystem() {
        val system = engine.generate(GenerationRequest.LinearSystem3(1, 2, 3), 1L).equation
        assertEquals(false, GraphSampler.hasGraph(system))
        for (request in listOf(
            GenerationRequest.LinearOneVar(1),
            GenerationRequest.LinearTwoVar(1, 2),
            GenerationRequest.LinearPair(1, 2),
            GenerationRequest.Polynomial(1, 2),
            GenerationRequest.Polynomial(1, 2, 3),
        )) {
            assertTrue("$request", GraphSampler.hasGraph(engine.generate(request, 1L).equation))
        }
    }

    @Test
    fun threeVariableSystem_hasNoGraphAndFittingOrSamplingIsRejected() {
        val system = engine.generate(GenerationRequest.LinearSystem3(1, 2, 3), 1L).equation
        assertThrows(IllegalArgumentException::class.java) { GraphSampler.fitViewport(system) }
        assertThrows(IllegalArgumentException::class.java) { GraphSampler.sample(system, Viewport(-1.0, 1.0, -1.0, 1.0)) }
    }

    // ---- general ----

    @Test
    fun sample_isDeterministic() {
        val equation = generated<GeneratedEquation.Polynomial>(GenerationRequest.Polynomial(-6, 11), 9L)
        val viewport = GraphSampler.fitViewport(equation)
        assertEquals(GraphSampler.sample(equation, viewport), GraphSampler.sample(equation, viewport))
    }

    @Test
    fun sample_rejectsTooFewSamples() {
        val equation = generated<GeneratedEquation.Polynomial>(GenerationRequest.Polynomial(1, 2), 1L)
        assertThrows(IllegalArgumentException::class.java) {
            GraphSampler.sample(equation, GraphSampler.fitViewport(equation), sampleCount = 1)
        }
    }
}
