package com.sanket.eqgen.domain.format

import com.sanket.eqgen.domain.model.GeneratedEquation
import com.sanket.eqgen.domain.model.LinearEquation1D
import com.sanket.eqgen.domain.model.LinearEquation2D
import com.sanket.eqgen.domain.model.LinearEquation3D
import com.sanket.eqgen.domain.model.PolynomialEquation

/**
 * One equation as two strings, so the UI can line up the "=" signs of several equations.
 * [plain] is the equation as a single line of text.
 */
data class EquationLine(val left: String, val right: String) {
    val plain: String get() = "$left = $right"
}

/**
 * Display text for a generated equation. Pure data, no Android types, so the UI only lays it out.
 *
 * @property equations one line per equation (two for a pair, three for a 3-variable system).
 * @property solutions one entry per value, for example `x = 2` and `y = −1`. A polynomial lists its
 * roots exactly as entered, one per root, so a repeated root appears repeatedly.
 */
data class EquationText(
    val equations: List<EquationLine>,
    val solutions: List<String>,
)

/**
 * Renders equations as plain text with a true minus sign (U+2212) and superscript exponents.
 * Coefficients of 1 are omitted and zero terms are skipped, so `a = 1, b = −5, c = 6` reads
 * `x² − 5x + 6 = 0`. Variable names are fixed to x, y and z in v1.
 *
 * Plain text only; LaTeX and image export are separate future formatters (spec section 02).
 */
object EquationFormatter {

    private const val MINUS = '−'

    private val SUPERSCRIPT_DIGITS = "⁰¹²³⁴⁵⁶⁷⁸⁹"

    fun format(equation: GeneratedEquation): EquationText = when (equation) {
        is GeneratedEquation.LinearOneVar -> EquationText(
            equations = listOf(linearOneVar(equation.equation)),
            solutions = listOf("x = ${number(equation.root)}"),
        )
        is GeneratedEquation.LinearTwoVar -> EquationText(
            equations = listOf(linearTwoVar(equation.equation)),
            solutions = listOf("x = ${number(equation.x)}", "y = ${number(equation.y)}"),
        )
        is GeneratedEquation.LinearPair -> EquationText(
            equations = listOf(linearTwoVar(equation.first), linearTwoVar(equation.second)),
            solutions = listOf("x = ${number(equation.x)}", "y = ${number(equation.y)}"),
        )
        is GeneratedEquation.LinearSystem3 -> EquationText(
            equations = equation.equations.map { linearThreeVar(it) },
            solutions = listOf(
                "x = ${number(equation.x)}",
                "y = ${number(equation.y)}",
                "z = ${number(equation.z)}",
            ),
        )
        is GeneratedEquation.Polynomial -> EquationText(
            equations = listOf(polynomial(equation.equation)),
            solutions = equation.equation.roots.map { "x = ${number(it)}" },
        )
    }

    /** `ax + b = 0` */
    fun linearOneVar(equation: LinearEquation1D): EquationLine =
        EquationLine(expression(listOf(equation.a to "x", equation.b to "")), "0")

    /** `ax + by = c` */
    fun linearTwoVar(equation: LinearEquation2D): EquationLine =
        EquationLine(expression(listOf(equation.a to "x", equation.b to "y")), number(equation.c))

    /** `ax + by + cz = d` */
    fun linearThreeVar(equation: LinearEquation3D): EquationLine = EquationLine(
        expression(listOf(equation.a to "x", equation.b to "y", equation.c to "z")),
        number(equation.d),
    )

    /** `ax² + bx + c = 0`, `ax³ + bx² + cx + d = 0`, and so on for the polynomial's degree. */
    fun polynomial(equation: PolynomialEquation): EquationLine {
        val degree = equation.degree
        val terms = equation.coefficients.mapIndexed { index, coefficient ->
            val power = degree - index
            coefficient to when {
                power == 0 -> ""
                power == 1 -> "x"
                else -> "x" + superscript(power)
            }
        }
        return EquationLine(expression(terms), "0")
    }

    /** Joins `coefficient to symbol` terms; an empty symbol marks the constant term. */
    private fun expression(terms: List<Pair<Int, String>>): String {
        val builder = StringBuilder()
        for ((coefficient, symbol) in terms) {
            if (coefficient == 0) continue
            val magnitude = magnitudeOf(coefficient)
            val body = if (symbol.isNotEmpty() && magnitude == 1L) symbol else "$magnitude$symbol"
            if (builder.isEmpty()) {
                if (coefficient < 0) builder.append(MINUS)
            } else {
                builder.append(if (coefficient < 0) " $MINUS " else " + ")
            }
            builder.append(body)
        }
        return if (builder.isEmpty()) "0" else builder.toString()
    }

    private fun superscript(power: Int): String =
        power.toString().map { SUPERSCRIPT_DIGITS[it - '0'] }.joinToString("")

    private fun number(value: Int): String =
        if (value < 0) "$MINUS${magnitudeOf(value)}" else value.toString()

    /** Absolute value as a `Long`, so `Int.MIN_VALUE` cannot overflow. */
    private fun magnitudeOf(value: Int): Long = if (value < 0) -value.toLong() else value.toLong()
}
