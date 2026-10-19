package com.sanket.eqgen.domain.graph

/** A point in graph coordinates. `Double` is allowed here only; the mathematical truth stays `Int`. */
data class Point(val x: Double, val y: Double)

/**
 * The visible window of the graph, in graph coordinates.
 *
 * v1 uses a fixed, auto-fitted viewport. Pan/zoom later means supplying a different [Viewport];
 * nothing in the equation logic changes.
 */
data class Viewport(
    val xMin: Double,
    val xMax: Double,
    val yMin: Double,
    val yMax: Double,
) {
    init {
        require(xMin.isFinite() && xMax.isFinite() && yMin.isFinite() && yMax.isFinite()) {
            "Viewport bounds must be finite: $this"
        }
        require(xMin < xMax) { "xMin must be less than xMax: $xMin, $xMax" }
        require(yMin < yMax) { "yMin must be less than yMax: $yMin, $yMax" }
    }

    val width: Double get() = xMax - xMin
    val height: Double get() = yMax - yMin

    /** True when [point] lies inside the viewport or on its border. */
    operator fun contains(point: Point): Boolean =
        point.x in xMin..xMax && point.y in yMin..yMax
}

/**
 * Everything the renderer needs, with no knowledge of equations.
 *
 * @property polylines curves and lines to draw; each polyline has at least two points.
 * @property markers special points to highlight (roots, the intersection, the given solution).
 */
data class PlotData(
    val polylines: List<List<Point>>,
    val markers: List<Point>,
)
