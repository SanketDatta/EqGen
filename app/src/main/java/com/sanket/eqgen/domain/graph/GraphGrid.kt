package com.sanket.eqgen.domain.graph

import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round

/**
 * Grid and label maths for the graph. Pure functions, no Android or Compose dependency, so the
 * renderer only has to draw what these return.
 */
object GraphGrid {

    const val DEFAULT_TARGET_INTERVALS: Int = 6

    private const val MAX_TICKS = 10_000
    private const val MAX_DECIMALS = 6

    /**
     * A "nice" grid step: 1, 2 or 5 times a power of ten, chosen so that about [targetIntervals]
     * steps fit into [span]. The actual number of intervals stays within 0.6x to 1.5x of the target.
     */
    fun niceStep(span: Double, targetIntervals: Int = DEFAULT_TARGET_INTERVALS): Double {
        require(span.isFinite() && span > 0.0) { "span must be positive and finite: $span" }
        require(targetIntervals >= 1) { "targetIntervals must be at least 1: $targetIntervals" }

        val raw = span / targetIntervals
        val magnitude = 10.0.pow(floor(log10(raw)))
        val fraction = raw / magnitude
        val niceFraction = when {
            fraction <= 1.5 -> 1.0
            fraction <= 3.0 -> 2.0
            fraction <= 7.0 -> 5.0
            else -> 10.0
        }
        return niceFraction * magnitude
    }

    /**
     * Every multiple of [step] inside `[min, max]`, ascending. Each tick is computed as `k * step`
     * rather than by repeated addition, so rounding error does not accumulate.
     */
    fun ticks(min: Double, max: Double, step: Double): List<Double> {
        require(min.isFinite() && max.isFinite() && min <= max) { "Invalid range: $min..$max" }
        require(step.isFinite() && step > 0.0) { "step must be positive and finite: $step" }

        val first = ceil(min / step).toLong()
        val last = floor(max / step).toLong()
        require(last - first < MAX_TICKS) { "Too many ticks for range $min..$max with step $step" }
        return (first..last).map { it * step }
    }

    /**
     * Axis label for a tick: as many decimals as [step] needs, and never "-0".
     * Always uses `.` as the decimal separator, whatever the device locale.
     */
    fun formatTick(value: Double, step: Double): String {
        val cleaned = if (abs(value) < step * 1e-6) 0.0 else value
        return String.format(Locale.ROOT, "%.${decimalsFor(step)}f", cleaned)
    }

    /** Coordinate label: whole numbers without decimals, otherwise at most two with trailing zeros removed. */
    fun formatCoordinate(value: Double): String {
        val rounded = round(value * 100.0) / 100.0
        if (rounded == floor(rounded) && abs(rounded) < 1e15) return rounded.toLong().toString()
        return String.format(Locale.ROOT, "%.2f", rounded).trimEnd('0').trimEnd('.')
    }

    private fun decimalsFor(step: Double): Int =
        if (step >= 1.0) 0 else min(MAX_DECIMALS, ceil(-log10(step) - 1e-9).toInt())
}
