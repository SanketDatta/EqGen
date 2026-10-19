package com.sanket.eqgen.domain.graph

/**
 * Maps graph coordinates to canvas pixels for a canvas of [widthPx] x [heightPx].
 * Pixel (0, 0) is the top-left corner, so the y axis is flipped.
 *
 * Pure `Float`/`Double` maths with no Compose types, so it is unit-tested on the JVM.
 */
class GraphTransform(
    val viewport: Viewport,
    val widthPx: Float,
    val heightPx: Float,
) {
    init {
        require(widthPx.isFinite() && widthPx > 0f) { "widthPx must be positive and finite: $widthPx" }
        require(heightPx.isFinite() && heightPx > 0f) { "heightPx must be positive and finite: $heightPx" }
    }

    /**
     * Pixel x for graph [x]. With a finite [overscan] the result is limited to
     * `[-overscan, widthPx + overscan]`, which keeps far off-screen points from producing huge
     * coordinates that a renderer may not handle.
     */
    fun pixelX(x: Double, overscan: Float = Float.POSITIVE_INFINITY): Float {
        val raw = (x - viewport.xMin) / viewport.width * widthPx
        return raw.coerceIn(-overscan.toDouble(), widthPx.toDouble() + overscan).toFloat()
    }

    /** Pixel y for graph [y] (flipped: larger y is higher on screen). See [pixelX] for [overscan]. */
    fun pixelY(y: Double, overscan: Float = Float.POSITIVE_INFINITY): Float {
        val raw = (viewport.yMax - y) / viewport.height * heightPx
        return raw.coerceIn(-overscan.toDouble(), heightPx.toDouble() + overscan).toFloat()
    }
}
