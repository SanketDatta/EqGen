package com.sanket.eqgen.domain.graph

import com.sanket.eqgen.domain.model.GeneratedEquation
import com.sanket.eqgen.domain.model.PolynomialEquation
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Turns a [GeneratedEquation] into plot data (spec section 04). Pure functions, no Android dependency.
 *
 * Sampling always takes the [Viewport] as a parameter, so panning or zooming later only means
 * calling [sample] with a different viewport.
 *
 * What is drawn:
 * - M1 `ax + b = 0`: the line `y = ax + b`, with a marker at the root `(r, 0)`.
 * - M2 `ax + by = c`: the line, with a marker at the given solution.
 * - M3: both lines, with a marker at their intersection.
 * - M4: the parabola `y = ax² + bx + c`, with a marker at each distinct root.
 * - M6: the cubic `y = ax³ + bx² + cx + d`, with a marker at each distinct root.
 * - M5 (three variables) has no 2D graph: see [hasGraph].
 *
 * Lines are clipped to the viewport and become a two-point polyline. A line that misses the viewport
 * (or only touches a corner) produces no polyline. Polynomial curves are sampled across the viewport's
 * x-range and are not clipped in y; the renderer clips to its canvas. Markers are always returned, even when
 * they lie outside the viewport.
 */
object GraphSampler {

    const val DEFAULT_SAMPLE_COUNT: Int = 200

    /** Smallest width and height [fitViewport] produces, so degenerate inputs still give a usable graph. */
    const val MIN_SPAN: Double = 10.0

    private const val PADDING_FRACTION = 0.15
    private const val OPENING_EXTENSION_FRACTION = 0.6
    private const val CLIP_EPSILON = 1e-9

    private val ORIGIN = Point(0.0, 0.0)

    /** The line `a·x + b·y = c`. */
    private data class Line(val a: Double, val b: Double, val c: Double)

    /** True when [equation] can be drawn in 2D; false for the three-variable system (M5). */
    fun hasGraph(equation: GeneratedEquation): Boolean = equation !is GeneratedEquation.LinearSystem3

    // ---- Viewport fitting ----

    /**
     * A viewport that contains the origin (so the axes are visible) and the equation's key points:
     * the root (M1), the given solution (M2), the intersection (M3), the roots and vertex (M4), or the
     * roots, local extrema and edge values (M6).
     *
     * @throws IllegalArgumentException if the equation has no graph (see [hasGraph]).
     */
    fun fitViewport(equation: GeneratedEquation): Viewport = when (equation) {
        is GeneratedEquation.LinearOneVar -> fitLinearOneVar(equation)
        is GeneratedEquation.LinearTwoVar ->
            fitPoints(listOf(Point(equation.x.toDouble(), equation.y.toDouble()), ORIGIN))
        is GeneratedEquation.LinearPair ->
            fitPoints(listOf(Point(equation.x.toDouble(), equation.y.toDouble()), ORIGIN))
        is GeneratedEquation.Polynomial -> fitPolynomial(equation.equation)
        is GeneratedEquation.LinearSystem3 -> throw noGraph()
    }

    private fun fitLinearOneVar(equation: GeneratedEquation.LinearOneVar): Viewport {
        val line = equation.equation
        val (xLo, xHi) = padded(min(equation.root, 0).toDouble(), max(equation.root, 0).toDouble())
        // The line spans the full width, so its end heights must fit in the y-range too.
        val yAtLeft = line.a * xLo + line.b
        val yAtRight = line.a * xHi + line.b
        val (yLo, yHi) = padded(minOf(0.0, yAtLeft, yAtRight), maxOf(0.0, yAtLeft, yAtRight))
        return Viewport(xLo, xHi, yLo, yHi)
    }

    private fun fitPolynomial(equation: PolynomialEquation): Viewport =
        if (equation.degree == 2) fitQuadratic(equation) else fitHigherDegree(equation)

    private fun fitQuadratic(equation: PolynomialEquation): Viewport {
        val r1 = equation.roots[0].toDouble()
        val r2 = equation.roots[1].toDouble()
        val a = equation.leadingCoefficient.toDouble()
        // Vertex height: a·(h − r1)(h − r2) with h = (r1 + r2) / 2, which simplifies to −a(r1 − r2)² / 4.
        val vertexY = -a * (r1 - r2) * (r1 - r2) / 4.0

        val (xLo, xHi) = padded(minOf(r1, r2, 0.0), maxOf(r1, r2, 0.0))
        var (yLo, yHi) = padded(min(0.0, vertexY), max(0.0, vertexY))

        // Leave room on the side the parabola opens towards so its arms are visible.
        val extension = (yHi - yLo) * OPENING_EXTENSION_FRACTION
        if (a > 0) yHi += extension else yLo -= extension
        return Viewport(xLo, xHi, yLo, yHi)
    }

    /**
     * Cubic (and any higher degree): the x-range covers the roots and the origin; the y-range covers the
     * origin, every local extremum and the curve's height at both padded x-edges, so both arms are visible.
     */
    private fun fitHigherDegree(equation: PolynomialEquation): Viewport {
        val roots = equation.roots.map { it.toDouble() }
        val (xLo, xHi) = padded(min(roots.min(), 0.0), max(roots.max(), 0.0))
        val heights = mutableListOf(0.0, valueAt(equation, xLo), valueAt(equation, xHi))
        for (x in turningPoints(equation)) {
            if (x >= xLo && x <= xHi) heights += valueAt(equation, x)
        }
        val (yLo, yHi) = padded(heights.min(), heights.max())
        return Viewport(xLo, xHi, yLo, yHi)
    }

    private fun fitPoints(points: List<Point>): Viewport {
        val (xLo, xHi) = padded(points.minOf { it.x }, points.maxOf { it.x })
        val (yLo, yHi) = padded(points.minOf { it.y }, points.maxOf { it.y })
        return Viewport(xLo, xHi, yLo, yHi)
    }

    /** Pads [low]..[high] on both sides and widens to at least [MIN_SPAN] around the centre. */
    private fun padded(low: Double, high: Double): Pair<Double, Double> {
        val padding = (high - low) * PADDING_FRACTION
        var lo = low - padding
        var hi = high + padding
        if (hi - lo < MIN_SPAN) {
            val middle = (lo + hi) / 2.0
            lo = middle - MIN_SPAN / 2.0
            hi = middle + MIN_SPAN / 2.0
        }
        return lo to hi
    }

    // ---- Sampling ----

    /**
     * Plot data for [equation] over [viewport].
     *
     * @param sampleCount number of evenly spaced x positions for curves (M4, M6); at least 2. The roots
     * and the turning points are added exactly when they fall inside the viewport.
     * @throws IllegalArgumentException if the equation has no graph (see [hasGraph]).
     */
    fun sample(
        equation: GeneratedEquation,
        viewport: Viewport,
        sampleCount: Int = DEFAULT_SAMPLE_COUNT,
    ): PlotData {
        require(sampleCount >= 2) { "sampleCount must be at least 2: $sampleCount" }
        return when (equation) {
            is GeneratedEquation.LinearOneVar -> {
                // y = a·x + b  is the line  a·x − y = −b.
                val line = Line(equation.equation.a.toDouble(), -1.0, -equation.equation.b.toDouble())
                PlotData(
                    polylines = clippedLines(viewport, line),
                    markers = listOf(Point(equation.root.toDouble(), 0.0)),
                )
            }
            is GeneratedEquation.LinearTwoVar -> PlotData(
                polylines = clippedLines(viewport, lineOf(equation.equation.a, equation.equation.b, equation.equation.c)),
                markers = listOf(Point(equation.x.toDouble(), equation.y.toDouble())),
            )
            is GeneratedEquation.LinearPair -> PlotData(
                polylines = clippedLines(
                    viewport,
                    lineOf(equation.first.a, equation.first.b, equation.first.c),
                    lineOf(equation.second.a, equation.second.b, equation.second.c),
                ),
                markers = listOf(Point(equation.x.toDouble(), equation.y.toDouble())),
            )
            is GeneratedEquation.Polynomial -> samplePolynomial(equation.equation, viewport, sampleCount)
            is GeneratedEquation.LinearSystem3 -> throw noGraph()
        }
    }

    private fun noGraph() = IllegalArgumentException("A three-variable system has no 2D graph; check hasGraph first")

    private fun lineOf(a: Int, b: Int, c: Int) = Line(a.toDouble(), b.toDouble(), c.toDouble())

    private fun clippedLines(viewport: Viewport, vararg lines: Line): List<List<Point>> =
        lines.map { clip(it, viewport) }.filter { it.isNotEmpty() }

    /**
     * The part of [line] inside [viewport] as two end points ordered by x then y, or an empty list
     * when the line misses the viewport or touches it at a single point.
     */
    private fun clip(line: Line, viewport: Viewport): List<Point> {
        val epsilon = CLIP_EPSILON * max(1.0, max(viewport.width, viewport.height))
        val candidates = ArrayList<Point>(4)

        fun consider(x: Double, y: Double) {
            val inside = x >= viewport.xMin - epsilon && x <= viewport.xMax + epsilon &&
                y >= viewport.yMin - epsilon && y <= viewport.yMax + epsilon
            if (inside) {
                candidates += Point(
                    x.coerceIn(viewport.xMin, viewport.xMax),
                    y.coerceIn(viewport.yMin, viewport.yMax),
                )
            }
        }

        // Intersections with the four viewport edges.
        if (line.b != 0.0) {
            consider(viewport.xMin, (line.c - line.a * viewport.xMin) / line.b)
            consider(viewport.xMax, (line.c - line.a * viewport.xMax) / line.b)
        }
        if (line.a != 0.0) {
            consider((line.c - line.b * viewport.yMin) / line.a, viewport.yMin)
            consider((line.c - line.b * viewport.yMax) / line.a, viewport.yMax)
        }

        // The visible segment runs between the two farthest-apart intersections.
        var best: List<Point> = emptyList()
        var bestDistanceSquared = epsilon * epsilon
        for (i in candidates.indices) {
            for (j in i + 1 until candidates.size) {
                val dx = candidates[i].x - candidates[j].x
                val dy = candidates[i].y - candidates[j].y
                val distanceSquared = dx * dx + dy * dy
                if (distanceSquared > bestDistanceSquared) {
                    bestDistanceSquared = distanceSquared
                    best = listOf(candidates[i], candidates[j])
                }
            }
        }
        return best.sortedWith(compareBy<Point> { it.x }.thenBy { it.y })
    }

    private fun samplePolynomial(
        equation: PolynomialEquation,
        viewport: Viewport,
        sampleCount: Int,
    ): PlotData {
        val roots = equation.roots.map { it.toDouble() }

        val grid = List(sampleCount) { i ->
            if (i == sampleCount - 1) viewport.xMax else viewport.xMin + viewport.width * i / (sampleCount - 1)
        }
        // `+ 0.0` turns a negative zero into 0.0, so `distinct()` cannot keep both.
        val exact = (roots + turningPoints(equation)).map { it + 0.0 }.filter { it >= viewport.xMin && it <= viewport.xMax }
        val xs = (grid + exact).distinct().sorted()

        return PlotData(
            polylines = listOf(xs.map { x -> Point(x, valueAt(equation, x)) }),
            markers = roots.map { Point(it, 0.0) }.distinct(),
        )
    }

    /** `p(x)` as a `Double`, by Horner's rule over the integer coefficients. */
    private fun valueAt(equation: PolynomialEquation, x: Double): Double =
        equation.coefficients.fold(0.0) { acc, coefficient -> acc * x + coefficient }

    /**
     * The x positions where the slope is zero: the vertex of a parabola, or the local extrema of a cubic
     * (none when its derivative has no real root; one, the stationary inflection, when it has a double
     * root). Roots of the derivative `3a·x² + 2b·x + c` are `(−b ± √(b² − 3ac)) / 3a`.
     */
    private fun turningPoints(equation: PolynomialEquation): List<Double> {
        val c = equation.coefficients.map { it.toDouble() }
        return when (equation.degree) {
            2 -> listOf(-c[1] / (2.0 * c[0]))
            3 -> {
                val discriminant = c[1] * c[1] - 3.0 * c[0] * c[2]
                when {
                    discriminant < 0.0 -> emptyList()
                    discriminant == 0.0 -> listOf(-c[1] / (3.0 * c[0]))
                    else -> {
                        val root = sqrt(discriminant)
                        listOf((-c[1] - root) / (3.0 * c[0]), (-c[1] + root) / (3.0 * c[0]))
                    }
                }
            }
            else -> emptyList()
        }
    }
}
