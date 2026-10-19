package com.sanket.eqgen.domain.format

import com.sanket.eqgen.domain.generator.EquationEngine
import com.sanket.eqgen.domain.model.GeneratedEquation
import com.sanket.eqgen.domain.model.GenerationRequest
import com.sanket.eqgen.domain.model.LinearEquation1D
import com.sanket.eqgen.domain.model.LinearEquation2D
import com.sanket.eqgen.domain.model.LinearEquation3D
import com.sanket.eqgen.domain.model.PolynomialEquation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EquationFormatterTest {

    private val minus = "−"
    private val square = "²"
    private val cube = "³"
    private val standaloneOne = Regex("(?<![0-9])1[xyz]")
    private val standaloneZero = Regex("(?<![0-9])0[xyz]")

    private fun quad(r1: Int, r2: Int, a: Int) = EquationFormatter.polynomial(PolynomialEquation(listOf(r1, r2), a)).plain
    private fun cubic(r1: Int, r2: Int, r3: Int, a: Int) =
        EquationFormatter.polynomial(PolynomialEquation(listOf(r1, r2, r3), a)).plain

    // ---- M1 ----

    @Test
    fun linearOneVar_commonShapes() {
        assertEquals("2x $minus 6 = 0", EquationFormatter.linearOneVar(LinearEquation1D(a = 2, b = -6)).plain)
        assertEquals("3x + 12 = 0", EquationFormatter.linearOneVar(LinearEquation1D(a = 3, b = 12)).plain)
    }

    @Test
    fun linearOneVar_omitsCoefficientOne_andShowsLeadingMinus() {
        assertEquals("x $minus 3 = 0", EquationFormatter.linearOneVar(LinearEquation1D(a = 1, b = -3)).plain)
        assertEquals("${minus}x + 4 = 0", EquationFormatter.linearOneVar(LinearEquation1D(a = -1, b = 4)).plain)
        assertEquals("${minus}5x $minus 10 = 0", EquationFormatter.linearOneVar(LinearEquation1D(a = -5, b = -10)).plain)
    }

    @Test
    fun linearOneVar_zeroConstantIsSkipped() {
        assertEquals("5x = 0", EquationFormatter.linearOneVar(LinearEquation1D(a = 5, b = 0)).plain)
    }

    // ---- M2 / M3 ----

    @Test
    fun linearTwoVar_commonShapes() {
        assertEquals("2x $minus 3y = 7", EquationFormatter.linearTwoVar(LinearEquation2D(2, -3, 7)).plain)
        assertEquals("x + y = ${minus}4", EquationFormatter.linearTwoVar(LinearEquation2D(1, 1, -4)).plain)
        assertEquals("${minus}x + y = 0", EquationFormatter.linearTwoVar(LinearEquation2D(-1, 1, 0)).plain)
        assertEquals("${minus}7x $minus y = 12", EquationFormatter.linearTwoVar(LinearEquation2D(-7, -1, 12)).plain)
    }

    @Test
    fun linearTwoVar_zeroCoefficientIsSkipped() {
        assertEquals("3y = 6", EquationFormatter.linearTwoVar(LinearEquation2D(0, 3, 6)).plain)
        assertEquals("4x = 8", EquationFormatter.linearTwoVar(LinearEquation2D(4, 0, 8)).plain)
    }

    // ---- M4 ----

    @Test
    fun quadratic_commonShapes() {
        // roots 2 and 3, a = 1
        assertEquals("x$square $minus 5x + 6 = 0", quad(2, 3, 1))
        // roots -4 and 6, a = -3: b = 6, c = 72
        assertEquals("${minus}3x$square + 6x + 72 = 0", quad(-4, 6, -3))
        // roots 1 and 2, a = 2: b = -6, c = 4
        assertEquals("2x$square $minus 6x + 4 = 0", quad(1, 2, 2))
    }

    @Test
    fun quadratic_zeroTermsAreSkipped() {
        // roots -3 and 3, a = 1: b = 0, c = -9
        assertEquals("x$square $minus 9 = 0", quad(-3, 3, 1))
        // roots 0 and 5, a = 2: b = -10, c = 0
        assertEquals("2x$square $minus 10x = 0", quad(0, 5, 2))
        // roots 0 and 0: b = 0, c = 0
        assertEquals("4x$square = 0", quad(0, 0, 4))
        assertEquals("${minus}x$square = 0", quad(0, 0, -1))
    }

    // ---- format(): solutions and structure ----

    @Test
    fun format_linearOneVar() {
        val text = EquationFormatter.format(
            GeneratedEquation.LinearOneVar(LinearEquation1D(2, 6), root = -3),
        )
        assertEquals(listOf("2x + 6 = 0"), text.equations.map { it.plain })
        assertEquals(listOf("x = ${minus}3"), text.solutions)
    }

    @Test
    fun format_linearTwoVar() {
        val text = EquationFormatter.format(
            GeneratedEquation.LinearTwoVar(LinearEquation2D(2, -3, 7), x = 2, y = -1),
        )
        assertEquals(listOf("2x $minus 3y = 7"), text.equations.map { it.plain })
        assertEquals(listOf("x = 2", "y = ${minus}1"), text.solutions)
    }

    @Test
    fun format_pair_hasTwoEquationLines() {
        val text = EquationFormatter.format(
            GeneratedEquation.LinearPair(
                first = LinearEquation2D(1, 1, 3),
                second = LinearEquation2D(1, -1, 1),
                x = 2,
                y = 1,
            ),
        )
        assertEquals(listOf("x + y = 3", "x $minus y = 1"), text.equations.map { it.plain })
        assertEquals(listOf("x = 2", "y = 1"), text.solutions)
    }

    @Test
    fun format_quadratic_listsRootsAsEntered() {
        val text = EquationFormatter.format(GeneratedEquation.Polynomial(PolynomialEquation(listOf(2, -3), 1)))
        assertEquals(listOf("x$square + x $minus 6 = 0"), text.equations.map { it.plain })
        assertEquals(listOf("x = 2", "x = ${minus}3"), text.solutions)
    }

    @Test
    fun format_repeatedRoots_appearRepeatedly() {
        val quad = EquationFormatter.format(GeneratedEquation.Polynomial(PolynomialEquation(listOf(4, 4), 1)))
        assertEquals(listOf("x$square $minus 8x + 16 = 0"), quad.equations.map { it.plain })
        assertEquals(listOf("x = 4", "x = 4"), quad.solutions)
        val cube = EquationFormatter.format(GeneratedEquation.Polynomial(PolynomialEquation(listOf(1, 1, 1), 1)))
        assertEquals(listOf("x = 1", "x = 1", "x = 1"), cube.solutions)
    }

    // ---- M6 ----

    @Test
    fun cubic_commonShapes() {
        // (x-1)(x-2)(x-3) = x^3 - 6x^2 + 11x - 6
        assertEquals("x$cube $minus 6x$square + 11x $minus 6 = 0", cubic(1, 2, 3, 1))
        // 2(x+1)(x-1)(x) = 2x^3 - 2x
        assertEquals("2x$cube $minus 2x = 0", cubic(-1, 1, 0, 2))
        // -(x)^3
        assertEquals("${minus}x$cube = 0", cubic(0, 0, 0, -1))
        // (x-5)^3 = x^3 - 15x^2 + 75x - 125
        assertEquals("x$cube $minus 15x$square + 75x $minus 125 = 0", cubic(5, 5, 5, 1))
    }

    @Test
    fun cubic_isOneEquationLineWithRightSideZero() {
        val line = EquationFormatter.polynomial(PolynomialEquation(listOf(1, 2, 3), 1))
        assertEquals("0", line.right)
        assertEquals("x$cube $minus 6x$square + 11x $minus 6", line.left)
    }

    // ---- M5 ----

    @Test
    fun linearThreeVar_commonShapes() {
        assertEquals("2x $minus 3y + z = 5", EquationFormatter.linearThreeVar(LinearEquation3D(2, -3, 1, 5)).plain)
        assertEquals("${minus}x + y $minus z = ${minus}4", EquationFormatter.linearThreeVar(LinearEquation3D(-1, 1, -1, -4)).plain)
        assertEquals("3z = 0", EquationFormatter.linearThreeVar(LinearEquation3D(0, 0, 3, 0)).plain)
    }

    @Test
    fun linearThreeVar_splitsAtTheEqualsSign() {
        val line = EquationFormatter.linearThreeVar(LinearEquation3D(2, -3, 1, -5))
        assertEquals("2x $minus 3y + z", line.left)
        assertEquals("${minus}5", line.right)
    }

    @Test
    fun format_system3_hasThreeEquationsAndThreeSolutions() {
        val text = EquationFormatter.format(
            GeneratedEquation.LinearSystem3(
                equations = listOf(
                    LinearEquation3D(1, 1, 1, 6),
                    LinearEquation3D(1, -1, 0, -1),
                    LinearEquation3D(0, 1, -1, -1),
                ),
                x = 1, y = 2, z = 3,
            ),
        )
        assertEquals(listOf("x + y + z = 6", "x $minus y = ${minus}1", "y $minus z = ${minus}1"), text.equations.map { it.plain })
        assertEquals(listOf("x = 1", "y = 2", "z = 3"), text.solutions)
    }

    // ---- every generated equation renders sensibly ----

    @Test
    fun format_ofGeneratedEquations_neverEmitsAsciiHyphenOrAwkwardCoefficients() {
        val engine = EquationEngine()
        val coordinates = listOf(-100, -7, 0, 1, 100)
        val requests = buildList {
            for (c in coordinates) add(GenerationRequest.LinearOneVar(c))
            for (x in coordinates) for (y in coordinates) {
                add(GenerationRequest.LinearTwoVar(x, y))
                add(GenerationRequest.LinearPair(x, y))
                add(GenerationRequest.Polynomial(x, y))
                for (z in coordinates) add(GenerationRequest.Polynomial(x, y, z))
                add(GenerationRequest.LinearSystem3(x, y, (x + y) / 2))
            }
        }
        for (request in requests) for (seed in 0L until 20L) {
            val text = EquationFormatter.format(engine.generate(request, seed).equation)
            val lines = text.equations.map { it.plain } + text.solutions
            for (line in lines) {
                assertFalse("ASCII hyphen in '$line'", line.contains('-'))
                // A coefficient of exactly 1 or 0 (not part of a longer number like 11y) must not be printed.
                assertFalse("'$line'", standaloneOne.containsMatchIn(line))
                assertFalse("'$line'", standaloneZero.containsMatchIn(line))
                assertFalse("'$line'", line.contains("+ $minus") || line.contains("$minus $minus"))
                assertTrue("'$line'", line.contains(" = "))
            }
            assertTrue(text.equations.isNotEmpty())
            assertTrue(text.solutions.isNotEmpty())
        }
    }
}
