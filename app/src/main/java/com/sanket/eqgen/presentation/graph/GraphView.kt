package com.sanket.eqgen.presentation.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sanket.eqgen.domain.generator.EquationEngine
import com.sanket.eqgen.domain.graph.GraphGrid
import com.sanket.eqgen.domain.graph.GraphSampler
import com.sanket.eqgen.domain.graph.GraphTransform
import com.sanket.eqgen.domain.graph.PlotData
import com.sanket.eqgen.domain.graph.Viewport
import com.sanket.eqgen.domain.model.GenerationRequest
import kotlin.math.max

/**
 * Static 2D graph drawn on a Compose [Canvas] (spec section 04).
 *
 * Stateless and free of equation knowledge: it draws the [plot] inside the given [viewport] and
 * nothing else. All maths (grid steps, labels, graph-to-pixel mapping) lives in the pure domain layer.
 * v1 has no gestures; pan/zoom later means adding an `onViewportChange` callback and a gesture modifier,
 * while the caller keeps the viewport and re-samples.
 *
 * Give it a size from the outside, for example `Modifier.fillMaxWidth().aspectRatio(1f)`.
 * Colors come from [MaterialTheme], so light and dark themes work without extra code.
 */
@Composable
fun GraphView(
    plot: PlotData,
    viewport: Viewport,
    modifier: Modifier = Modifier,
    contentDescription: String = "Graph of the equation",
) {
    val colors = MaterialTheme.colorScheme
    val palette = GraphPalette(
        grid = colors.outlineVariant,
        axis = colors.onSurface.copy(alpha = 0.7f),
        curves = listOf(colors.primary, colors.tertiary),
        marker = colors.secondary,
        halo = colors.surface,
        labelBackground = colors.surface.copy(alpha = 0.85f),
        labelStyle = MaterialTheme.typography.labelSmall.copy(color = colors.onSurfaceVariant),
    )
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier.semantics { this.contentDescription = contentDescription }) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas

        val transform = GraphTransform(viewport, size.width, size.height)
        val xStep = GraphGrid.niceStep(viewport.width)
        val yStep = GraphGrid.niceStep(viewport.height)
        val xTicks = GraphGrid.ticks(viewport.xMin, viewport.xMax, xStep)
        val yTicks = GraphGrid.ticks(viewport.yMin, viewport.yMax, yStep)

        drawGridLines(xTicks, yTicks, transform, palette.grid)
        drawAxes(viewport, transform, palette.axis)
        clipRect { drawPolylines(plot, transform, palette.curves) }

        // Reserve a row at the bottom for x labels so y labels never collide with them.
        val labelRowHeight = textMeasurer.measure("0", palette.labelStyle).size.height + 4.dp.toPx()
        drawXTickLabels(xTicks, xStep, transform, textMeasurer, palette.labelStyle)
        drawYTickLabels(yTicks, yStep, transform, textMeasurer, palette.labelStyle, bottomInset = labelRowHeight)
        drawMarkers(plot, transform, palette, textMeasurer)
    }
}

private class GraphPalette(
    val grid: Color,
    val axis: Color,
    val curves: List<Color>,
    val marker: Color,
    val halo: Color,
    val labelBackground: Color,
    val labelStyle: TextStyle,
)

private fun DrawScope.drawGridLines(
    xTicks: List<Double>,
    yTicks: List<Double>,
    transform: GraphTransform,
    color: Color,
) {
    val strokeWidth = 1.dp.toPx()
    for (x in xTicks) {
        val px = transform.pixelX(x)
        drawLine(color, Offset(px, 0f), Offset(px, size.height), strokeWidth)
    }
    for (y in yTicks) {
        val py = transform.pixelY(y)
        drawLine(color, Offset(0f, py), Offset(size.width, py), strokeWidth)
    }
}

private fun DrawScope.drawAxes(viewport: Viewport, transform: GraphTransform, color: Color) {
    val strokeWidth = 1.5.dp.toPx()
    if (0.0 in viewport.xMin..viewport.xMax) {
        val px = transform.pixelX(0.0)
        drawLine(color, Offset(px, 0f), Offset(px, size.height), strokeWidth)
    }
    if (0.0 in viewport.yMin..viewport.yMax) {
        val py = transform.pixelY(0.0)
        drawLine(color, Offset(0f, py), Offset(size.width, py), strokeWidth)
    }
}

private fun DrawScope.drawPolylines(plot: PlotData, transform: GraphTransform, curveColors: List<Color>) {
    // Far off-screen points (a steep parabola) are limited to a margin so the path stays well-behaved.
    val overscan = 4f * max(size.width, size.height)
    val stroke = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

    plot.polylines.forEachIndexed { index, polyline ->
        if (polyline.size < 2) return@forEachIndexed
        val path = Path()
        polyline.forEachIndexed { i, point ->
            val x = transform.pixelX(point.x, overscan)
            val y = transform.pixelY(point.y, overscan)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = curveColors[index % curveColors.size], style = stroke)
    }
}

private fun DrawScope.drawXTickLabels(
    ticks: List<Double>,
    step: Double,
    transform: GraphTransform,
    measurer: TextMeasurer,
    style: TextStyle,
) {
    val pad = 2.dp.toPx()
    var lastRight = Float.NEGATIVE_INFINITY
    for (tick in ticks) {
        val layout = measurer.measure(GraphGrid.formatTick(tick, step), style)
        val width = layout.size.width.toFloat()
        val height = layout.size.height.toFloat()
        val left = (transform.pixelX(tick) - width / 2f).coerceIn(0f, max(0f, size.width - width))
        if (left < lastRight + pad) continue // would overlap the previous label
        drawText(layout, topLeft = Offset(left, size.height - height - pad))
        lastRight = left + width
    }
}

private fun DrawScope.drawYTickLabels(
    ticks: List<Double>,
    step: Double,
    transform: GraphTransform,
    measurer: TextMeasurer,
    style: TextStyle,
    bottomInset: Float,
) {
    val pad = 2.dp.toPx()
    var lastBottom = Float.NEGATIVE_INFINITY
    for (tick in ticks.asReversed()) { // highest value (top of the screen) first
        val layout = measurer.measure(GraphGrid.formatTick(tick, step), style)
        val height = layout.size.height.toFloat()
        val top = (transform.pixelY(tick) - height / 2f).coerceIn(0f, max(0f, size.height - height))
        if (top < lastBottom + pad) continue // would overlap the previous label
        if (top + height > size.height - bottomInset) continue // would collide with the x label row
        drawText(layout, topLeft = Offset(pad, top))
        lastBottom = top + height
    }
}

private fun DrawScope.drawMarkers(
    plot: PlotData,
    transform: GraphTransform,
    palette: GraphPalette,
    measurer: TextMeasurer,
) {
    val radius = 5.dp.toPx()
    val haloRadius = radius + 2.dp.toPx()
    val gap = 6.dp.toPx()

    for (marker in plot.markers) {
        val center = Offset(transform.pixelX(marker.x), transform.pixelY(marker.y))
        val offScreen = center.x < -haloRadius || center.x > size.width + haloRadius ||
            center.y < -haloRadius || center.y > size.height + haloRadius
        if (offScreen) continue

        drawCircle(palette.halo, haloRadius, center)
        drawCircle(palette.marker, radius, center)

        val label = "(${GraphGrid.formatCoordinate(marker.x)}, ${GraphGrid.formatCoordinate(marker.y)})"
        val layout = measurer.measure(label, palette.labelStyle)
        val width = layout.size.width.toFloat()
        val height = layout.size.height.toFloat()

        // Prefer upper right of the marker; flip to the left or below when it would leave the canvas.
        var left = center.x + gap
        if (left + width > size.width) left = center.x - gap - width
        left = left.coerceIn(0f, max(0f, size.width - width))
        var top = center.y - gap - height
        if (top < 0f) top = center.y + gap
        top = top.coerceIn(0f, max(0f, size.height - height))

        drawRect(palette.labelBackground, topLeft = Offset(left, top), size = Size(width, height))
        drawText(layout, topLeft = Offset(left, top))
    }
}

// ---- Previews (sample data only; delete if you don't use previews) ----

@Preview(showBackground = true)
@Composable
private fun GraphViewQuadraticPreview() {
    MaterialTheme {
        val equation = EquationEngine().generate(GenerationRequest.Polynomial(-2, 5), seed = 1L).equation
        val viewport = GraphSampler.fitViewport(equation)
        GraphView(
            plot = GraphSampler.sample(equation, viewport),
            viewport = viewport,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GraphViewPairPreview() {
    MaterialTheme {
        val equation = EquationEngine().generate(GenerationRequest.LinearPair(3, -4), seed = 7L).equation
        val viewport = GraphSampler.fitViewport(equation)
        GraphView(
            plot = GraphSampler.sample(equation, viewport),
            viewport = viewport,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        )
    }
}
