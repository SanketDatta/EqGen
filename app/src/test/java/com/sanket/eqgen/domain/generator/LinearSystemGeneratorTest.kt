package com.sanket.eqgen.domain.generator

import com.sanket.eqgen.domain.model.GeneratedEquation
import com.sanket.eqgen.domain.model.GenerationConfig
import com.sanket.eqgen.domain.model.GenerationRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LinearSystemGeneratorTest {

    private val generator = LinearSystemGenerator()

    /** Includes zero, negatives, and both input limits. */
    private val coordinates = listOf(-100, -23, -1, 0, 1, 8, 100)
    private val seeds = 0L until 60L

    private fun isValidCoefficient(value: Int) = value != 0 && value in -10..20

    // ---- M2: ax + by = c ----

    @Test
    fun twoVar_solutionSatisfiesEquation() {
        for (x in coordinates) for (y in coordinates) for (seed in seeds) {
            val result = generator.generateTwoVar(GenerationRequest.LinearTwoVar(x, y), Random(seed))
            assertEquals("x=$x y=$y seed=$seed", 0, result.equation.residual(x, y))
            assertEquals(x, result.x)
            assertEquals(y, result.y)
        }
    }

    @Test
    fun twoVar_aAndBInRangeAndNonZero_cIsDerived() {
        for (x in coordinates) for (y in coordinates) for (seed in seeds) {
            val equation = generator
                .generateTwoVar(GenerationRequest.LinearTwoVar(x, y), Random(seed))
                .equation
            assertTrue("a=${equation.a}", isValidCoefficient(equation.a))
            assertTrue("b=${equation.b}", isValidCoefficient(equation.b))
            assertEquals(equation.a * x + equation.b * y, equation.c)
        }
    }

    @Test
    fun twoVar_zeroConstantIsPossible_andNotRangeLimited() {
        val equation = generator
            .generateTwoVar(GenerationRequest.LinearTwoVar(0, 0), Random(1L))
            .equation
        assertEquals(0, equation.c)

        val large = generator
            .generateTwoVar(GenerationRequest.LinearTwoVar(100, 100), Random(1L))
            .equation
        assertEquals(large.a * 100 + large.b * 100, large.c)
    }

    @Test
    fun twoVar_isDeterministicForSameSeed() {
        val request = GenerationRequest.LinearTwoVar(4, -9)
        assertEquals(
            generator.generateTwoVar(request, Random(5L)),
            generator.generateTwoVar(request, Random(5L)),
        )
    }

    @Test
    fun twoVar_outOfRangeInputThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            generator.generateTwoVar(GenerationRequest.LinearTwoVar(101, 0), Random(1L))
        }
        assertThrows(IllegalArgumentException::class.java) {
            generator.generateTwoVar(GenerationRequest.LinearTwoVar(0, -101), Random(1L))
        }
    }

    // ---- M3: pair with a unique solution ----

    @Test
    fun pair_solutionSatisfiesBothEquations() {
        for (x in coordinates) for (y in coordinates) for (seed in seeds) {
            val pair = generator.generatePair(GenerationRequest.LinearPair(x, y), Random(seed))
            assertEquals("x=$x y=$y seed=$seed", 0, pair.first.residual(x, y))
            assertEquals("x=$x y=$y seed=$seed", 0, pair.second.residual(x, y))
            assertEquals(x, pair.x)
            assertEquals(y, pair.y)
        }
    }

    @Test
    fun pair_determinantIsNeverZero() {
        for (x in coordinates) for (y in coordinates) for (seed in 0L until 200L) {
            val pair = generator.generatePair(GenerationRequest.LinearPair(x, y), Random(seed))
            assertNotEquals("seed=$seed", 0, pair.determinant)
            assertEquals(
                pair.first.a * pair.second.b - pair.second.a * pair.first.b,
                pair.determinant,
            )
        }
    }

    @Test
    fun pair_allFourCoefficientsInRangeAndNonZero() {
        for (x in coordinates) for (y in coordinates) for (seed in seeds) {
            val pair = generator.generatePair(GenerationRequest.LinearPair(x, y), Random(seed))
            for (value in listOf(pair.first.a, pair.first.b, pair.second.a, pair.second.b)) {
                assertTrue("coefficient=$value", isValidCoefficient(value))
            }
        }
    }

    @Test
    fun pair_solutionIsUnique_byCramersRule() {
        for (x in coordinates) for (y in coordinates) for (seed in seeds) {
            val pair = generator.generatePair(GenerationRequest.LinearPair(x, y), Random(seed))
            val (a1, b1, c1) = Triple(pair.first.a, pair.first.b, pair.first.c)
            val (a2, b2, c2) = Triple(pair.second.a, pair.second.b, pair.second.c)
            val det = pair.determinant
            assertEquals(x * det, c1 * b2 - c2 * b1)
            assertEquals(y * det, a1 * c2 - a2 * c1)
        }
    }

    @Test
    fun pair_equationsAreNeverIdenticalOrMultiples() {
        for (seed in 0L until 500L) {
            val pair = generator.generatePair(GenerationRequest.LinearPair(3, -2), Random(seed))
            assertNotEquals(pair.first, pair.second)
        }
    }

    @Test
    fun pair_isDeterministicForSameSeed() {
        val request = GenerationRequest.LinearPair(-5, 12)
        assertEquals(
            generator.generatePair(request, Random(21L)),
            generator.generatePair(request, Random(21L)),
        )
    }

    @Test
    fun pair_worksWithTwoAllowedCoefficients() {
        for (range in listOf(1..2, -1..1)) {
            val tiny = LinearSystemGenerator(GenerationConfig(range))
            for (seed in 0L until 500L) {
                val pair = tiny.generatePair(GenerationRequest.LinearPair(2, 3), Random(seed))
                assertNotEquals("range=$range seed=$seed", 0, pair.determinant)
                assertEquals(0, pair.first.residual(2, 3))
                assertEquals(0, pair.second.residual(2, 3))
            }
        }
    }

    @Test
    fun pair_withSingleAllowedCoefficientThrows_whileTwoVarStillWorks() {
        val single = LinearSystemGenerator(GenerationConfig(5..5))
        assertThrows(IllegalArgumentException::class.java) {
            single.generatePair(GenerationRequest.LinearPair(1, 1), Random(1L))
        }
        val equation = single
            .generateTwoVar(GenerationRequest.LinearTwoVar(1, 1), Random(1L))
            .equation
        assertEquals(5, equation.a)
        assertEquals(5, equation.b)
    }

    @Test
    fun pair_outOfRangeInputThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            generator.generatePair(GenerationRequest.LinearPair(101, 0), Random(1L))
        }
        assertThrows(IllegalArgumentException::class.java) {
            generator.generatePair(GenerationRequest.LinearPair(0, Int.MIN_VALUE), Random(1L))
        }
    }

    // ---- M5: three equations, three variables ----

    private val triples = listOf(-100, -7, 0, 1, 100)

    /** Rule of Sarrus, written out independently of the model's own determinant. */
    private fun sarrus(m: Array<LongArray>): Long =
        m[0][0] * m[1][1] * m[2][2] + m[0][1] * m[1][2] * m[2][0] + m[0][2] * m[1][0] * m[2][1] -
            m[0][2] * m[1][1] * m[2][0] - m[0][1] * m[1][0] * m[2][2] - m[0][0] * m[1][2] * m[2][1]

    private fun matrixOf(system: GeneratedEquation.LinearSystem3): Array<LongArray> =
        Array(3) { row ->
            val e = system.equations[row]
            longArrayOf(e.a.toLong(), e.b.toLong(), e.c.toLong())
        }

    private fun constantsOf(system: GeneratedEquation.LinearSystem3): LongArray =
        LongArray(3) { system.equations[it].d.toLong() }

    /** The matrix with column [column] replaced by the constants (Cramer's rule). */
    private fun withColumnReplaced(system: GeneratedEquation.LinearSystem3, column: Int): Array<LongArray> {
        val matrix = matrixOf(system)
        val constants = constantsOf(system)
        for (row in 0 until 3) matrix[row][column] = constants[row]
        return matrix
    }

    @Test
    fun system3_solutionSatisfiesAllThreeEquations() {
        for (x in triples) for (y in triples) for (z in triples) for (seed in 0L until 20L) {
            val system = generator.generateSystem3(GenerationRequest.LinearSystem3(x, y, z), Random(seed))
            for (equation in system.equations) {
                assertEquals("x=$x y=$y z=$z seed=$seed", 0, equation.residual(x, y, z))
            }
            assertEquals(Triple(x, y, z), Triple(system.x, system.y, system.z))
            assertEquals(3, system.equations.size)
        }
    }

    @Test
    fun system3_allNineCoefficientsInRangeAndNonZero_dIsDerived() {
        for (x in triples) for (y in triples) for (z in triples) for (seed in 0L until 10L) {
            val system = generator.generateSystem3(GenerationRequest.LinearSystem3(x, y, z), Random(seed))
            for (equation in system.equations) {
                for (value in listOf(equation.a, equation.b, equation.c)) {
                    assertTrue("coefficient=$value", isValidCoefficient(value))
                }
                assertEquals(equation.a * x + equation.b * y + equation.c * z, equation.d)
            }
        }
    }

    @Test
    fun system3_determinantIsNeverZero_andMatchesAnIndependentCalculation() {
        for (seed in 0L until 3_000L) {
            val system = generator.generateSystem3(GenerationRequest.LinearSystem3(3, -4, 5), Random(seed))
            assertNotEquals("seed=$seed", 0L, system.determinant)
            assertEquals(sarrus(matrixOf(system)), system.determinant)
        }
    }

    @Test
    fun system3_solutionIsUnique_byCramersRule() {
        for (x in triples) for (y in triples) for (z in triples) for (seed in 0L until 5L) {
            val system = generator.generateSystem3(GenerationRequest.LinearSystem3(x, y, z), Random(seed))
            val det = system.determinant
            assertEquals("x=$x y=$y z=$z", x * det, sarrus(withColumnReplaced(system, 0)))
            assertEquals("x=$x y=$y z=$z", y * det, sarrus(withColumnReplaced(system, 1)))
            assertEquals("x=$x y=$y z=$z", z * det, sarrus(withColumnReplaced(system, 2)))
        }
    }

    @Test
    fun system3_equationsAreAllDistinct() {
        for (seed in 0L until 500L) {
            val system = generator.generateSystem3(GenerationRequest.LinearSystem3(1, 2, 3), Random(seed))
            assertEquals(3, system.equations.toSet().size)
        }
    }

    @Test
    fun system3_isDeterministicForSameSeed() {
        val request = GenerationRequest.LinearSystem3(-5, 12, 0)
        for (seed in 0L until 50L) {
            assertEquals(
                generator.generateSystem3(request, Random(seed)),
                generator.generateSystem3(request, Random(seed)),
            )
        }
    }

    @Test
    fun system3_differentSeedsGiveVariety() {
        val request = GenerationRequest.LinearSystem3(1, 2, 3)
        val distinct = (0L until 200L).map { generator.generateSystem3(request, Random(it)) }.toSet()
        assertTrue("distinct=${distinct.size}", distinct.size > 190)
    }

    @Test
    fun system3_neverNeedsMoreThanAHandfulOfRedraws_forTheDefaultConfig() {
        // The matrix is singular only rarely, so the bounded redraw loop is nowhere near its limit.
        // (Not asserted directly: it is enough that 5,000 seeds all succeed, which would be
        // impossible if singular matrices were common enough to exhaust 1,000 attempts.)
        for (seed in 0L until 5_000L) {
            generator.generateSystem3(GenerationRequest.LinearSystem3(0, 0, 0), Random(seed))
        }
    }

    @Test
    fun system3_worksWithTwoAllowedCoefficients() {
        for (range in listOf(1..2, -1..1, 3..4, -2..-1)) {
            val tiny = LinearSystemGenerator(GenerationConfig(range))
            for (seed in 0L until 300L) {
                val system = tiny.generateSystem3(GenerationRequest.LinearSystem3(2, -3, 4), Random(seed))
                assertNotEquals("range=$range seed=$seed", 0L, system.determinant)
                for (equation in system.equations) assertEquals(0, equation.residual(2, -3, 4))
            }
        }
    }

    @Test
    fun system3_withSingleAllowedCoefficientThrows() {
        val single = LinearSystemGenerator(GenerationConfig(5..5))
        assertThrows(IllegalArgumentException::class.java) {
            single.generateSystem3(GenerationRequest.LinearSystem3(1, 1, 1), Random(1L))
        }
    }

    @Test
    fun system3_worstCaseWidestConfigStaysExact() {
        val widest = LinearSystemGenerator(GenerationConfig(-1000..1000))
        for (seed in 0L until 500L) {
            val system = widest.generateSystem3(GenerationRequest.LinearSystem3(100, -100, 100), Random(seed))
            for (equation in system.equations) {
                assertEquals(0, equation.residual(100, -100, 100))
                assertEquals(
                    equation.a.toLong() * 100 - equation.b.toLong() * 100 + equation.c.toLong() * 100,
                    equation.d.toLong(),
                )
            }
            assertEquals(sarrus(matrixOf(system)), system.determinant)
            assertNotEquals(0L, system.determinant)
        }
    }

    @Test
    fun system3_outOfRangeInputThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            generator.generateSystem3(GenerationRequest.LinearSystem3(101, 0, 0), Random(1L))
        }
        assertThrows(IllegalArgumentException::class.java) {
            generator.generateSystem3(GenerationRequest.LinearSystem3(0, -101, 0), Random(1L))
        }
        assertThrows(IllegalArgumentException::class.java) {
            generator.generateSystem3(GenerationRequest.LinearSystem3(0, 0, Int.MIN_VALUE), Random(1L))
        }
    }
}
