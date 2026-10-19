package com.sanket.eqgen.domain.generator

import com.sanket.eqgen.domain.model.GenerationConfig
import com.sanket.eqgen.domain.model.GenerationRequest
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PolynomialGeneratorTest {

    private val generator = PolynomialGenerator()

    /** Includes zero, negatives, and both input limits. */
    private val roots = listOf(-100, -37, -10, -1, 0, 1, 7, 20, 99, 100)
    private val seeds = 0L until 100L

    private fun isValidCoefficient(value: Int) = value != 0 && value in -10..20

    private fun generate(request: GenerationRequest.Polynomial, seed: Long) =
        generator.generatePolynomial(request, Random(seed)).equation

    /** Vieta computed independently of the model: coefficient of x^(n-k) is a·(-1)^k·e_k. */
    private fun vieta(rootList: List<Int>, a: Int): List<Long> {
        val r = rootList.map { it.toLong() }
        val e1 = r.sum()
        return when (r.size) {
            2 -> listOf(a.toLong(), -a * e1, a * r[0] * r[1])
            3 -> {
                val e2 = r[0] * r[1] + r[0] * r[2] + r[1] * r[2]
                val e3 = r[0] * r[1] * r[2]
                listOf(a.toLong(), -a * e1, a * e2, -a * e3)
            }
            else -> error("unsupported degree")
        }
    }

    // ---- M1: ax + b = 0 ----

    @Test
    fun linear_rootSatisfiesEquation() {
        for (root in roots) for (seed in seeds) {
            val result = generator.generateLinear(GenerationRequest.LinearOneVar(root), Random(seed))
            assertEquals("root=$root seed=$seed", 0, result.equation.residual(root))
            assertEquals(root, result.root)
        }
    }

    @Test
    fun linear_aIsInRangeAndNonZero_bIsDerived() {
        for (root in roots) for (seed in seeds) {
            val equation = generator
                .generateLinear(GenerationRequest.LinearOneVar(root), Random(seed))
                .equation
            assertTrue("a=${equation.a}", isValidCoefficient(equation.a))
            assertEquals(-equation.a * root, equation.b)
        }
    }

    @Test
    fun linear_isDeterministicForSameSeed() {
        val request = GenerationRequest.LinearOneVar(-6)
        assertEquals(
            generator.generateLinear(request, Random(99L)),
            generator.generateLinear(request, Random(99L)),
        )
    }

    @Test
    fun linear_outOfRangeInputThrows() {
        for (root in listOf(101, -101, Int.MAX_VALUE, Int.MIN_VALUE)) {
            assertThrows(IllegalArgumentException::class.java) {
                generator.generateLinear(GenerationRequest.LinearOneVar(root), Random(1L))
            }
        }
    }

    // ---- M4: quadratic ----

    @Test
    fun quadratic_bothRootsSatisfyEquation() {
        for (r1 in roots) for (r2 in roots) for (seed in 0L until 20L) {
            val equation = generate(GenerationRequest.Polynomial(r1, r2), seed)
            assertEquals("r1=$r1 r2=$r2 seed=$seed", 0L, equation.residual(r1))
            assertEquals("r1=$r1 r2=$r2 seed=$seed", 0L, equation.residual(r2))
            assertEquals(listOf(r1, r2), equation.roots)
            assertEquals(2, equation.degree)
        }
    }

    @Test
    fun quadratic_leadingCoefficientInRangeAndNonZero() {
        for (r1 in roots) for (r2 in roots) for (seed in 0L until 20L) {
            val a = generate(GenerationRequest.Polynomial(r1, r2), seed).leadingCoefficient
            assertTrue("a=$a", isValidCoefficient(a))
        }
    }

    @Test
    fun quadratic_coefficientsSatisfyVieta() {
        for (r1 in roots) for (r2 in roots) for (seed in 0L until 20L) {
            val equation = generate(GenerationRequest.Polynomial(r1, r2), seed)
            assertEquals(
                vieta(listOf(r1, r2), equation.leadingCoefficient),
                equation.coefficients.map { it.toLong() },
            )
        }
    }

    @Test
    fun quadratic_equalRootsAreAllowed_andGiveZeroDiscriminant() {
        for (root in roots) for (seed in seeds) {
            val equation = generate(GenerationRequest.Polynomial(root, root), seed)
            assertEquals(0L, equation.residual(root))
            assertEquals(0L, equation.derivativeAt(root))
            val (a, b, c) = equation.coefficients.map { it.toLong() }
            assertEquals(0L, b * b - 4L * a * c)
        }
    }

    @Test
    fun quadratic_isDeterministicForSameSeed() {
        val request = GenerationRequest.Polynomial(-3, 8)
        assertEquals(
            generator.generatePolynomial(request, Random(7L)),
            generator.generatePolynomial(request, Random(7L)),
        )
    }

    // ---- M6: cubic ----

    @Test
    fun cubic_everyRootSatisfiesEquation_forEveryTriple() {
        for (r1 in roots) for (r2 in roots) for (r3 in roots) for (seed in 0L until 3L) {
            val equation = generate(GenerationRequest.Polynomial(r1, r2, r3), seed)
            for (root in listOf(r1, r2, r3)) {
                assertEquals("roots=$r1,$r2,$r3 seed=$seed", 0L, equation.residual(root))
            }
            assertEquals(listOf(r1, r2, r3), equation.roots)
            assertEquals(3, equation.degree)
        }
    }

    @Test
    fun cubic_coefficientsSatisfyVieta() {
        for (r1 in roots) for (r2 in roots) for (r3 in roots) for (seed in 0L until 3L) {
            val equation = generate(GenerationRequest.Polynomial(r1, r2, r3), seed)
            assertEquals(
                "roots=$r1,$r2,$r3",
                vieta(listOf(r1, r2, r3), equation.leadingCoefficient),
                equation.coefficients.map { it.toLong() },
            )
        }
    }

    @Test
    fun cubic_leadingCoefficientInRangeAndNonZero_otherCoefficientsAreDerived() {
        for (seed in 0L until 500L) {
            val equation = generate(GenerationRequest.Polynomial(3, -8, 12), seed)
            val a = equation.leadingCoefficient
            assertTrue("a=$a", isValidCoefficient(a))
            assertEquals(a, equation.coefficients.first())
            assertEquals(4, equation.coefficients.size)
        }
    }

    @Test
    fun cubic_leadingCoefficientCoversTheWholeAllowedRange() {
        val seen = (0L until 2_000L)
            .map { generate(GenerationRequest.Polynomial(1, 2, 3), it).leadingCoefficient }
            .toSet()
        assertEquals(GenerationConfig().allowedCoefficients.toSet(), seen)
    }

    @Test
    fun cubic_isDeterministicForSameSeed() {
        val request = GenerationRequest.Polynomial(-3, 8, 0)
        assertEquals(
            generator.generatePolynomial(request, Random(7L)),
            generator.generatePolynomial(request, Random(7L)),
        )
    }

    // ---- repeated roots are never restricted or altered ----

    @Test
    fun repeatedRoots_areUsedExactlyAsEntered_inBothDegrees() {
        for (root in roots) for (seed in 0L until 5L) {
            assertEquals(listOf(root, root), generate(GenerationRequest.Polynomial(root, root), seed).roots)
            assertEquals(
                listOf(root, root, root),
                generate(GenerationRequest.Polynomial(root, root, root), seed).roots,
            )
        }
    }

    @Test
    fun repeatedRoots_inACubic_areGenuineMultipleRoots() {
        for (r in roots) for (s in roots) for (seed in 0L until 3L) {
            // (x - r)² (x - s): r is at least a double root, wherever s is.
            val doubled = generate(GenerationRequest.Polynomial(r, r, s), seed)
            assertEquals("r=$r s=$s", 0L, doubled.residual(r))
            assertEquals("r=$r s=$s", 0L, doubled.derivativeAt(r))
            assertEquals(0L, doubled.residual(s))

            // The same set of roots in a different order gives the same equation.
            val reordered = generate(GenerationRequest.Polynomial(s, r, r), seed)
            assertEquals(doubled.coefficients, reordered.coefficients)
        }
    }

    @Test
    fun repeatedRoots_tripleRootGivesAPerfectCube() {
        for (root in roots) for (seed in 0L until 5L) {
            val equation = generate(GenerationRequest.Polynomial(root, root, root), seed)
            assertEquals(0L, equation.residual(root))
            assertEquals(0L, equation.derivativeAt(root))
            // p''(root) = 0 too: for a·(x − r)³ the second derivative is 6a(x − r).
            val (a, b) = equation.coefficients.map { it.toLong() }
            assertEquals(0L, 6L * a * root + 2L * b)
        }
    }

    @Test
    fun simpleRoots_areNotMistakenForRepeatedOnes() {
        val equation = generate(GenerationRequest.Polynomial(1, 2, 3), 4L)
        for (root in equation.roots) assertNotEquals(0L, equation.derivativeAt(root))
    }

    // ---- overflow and limits ----

    @Test
    fun worstCaseDefaultConfigStaysExact() {
        for (seed in 0L until 500L) {
            val quadratic = generate(GenerationRequest.Polynomial(100, 100), seed)
            val a = quadratic.leadingCoefficient.toLong()
            assertEquals(listOf(a, -a * 200L, a * 100L * 100L), quadratic.coefficients.map { it.toLong() })
            assertEquals(0L, quadratic.residual(100))

            val cubic = generate(GenerationRequest.Polynomial(100, 100, 100), seed)
            val c = cubic.leadingCoefficient.toLong()
            assertEquals(
                listOf(c, -c * 300L, c * 30_000L, -c * 1_000_000L),
                cubic.coefficients.map { it.toLong() },
            )
            assertEquals(0L, cubic.residual(100))
        }
    }

    @Test
    fun worstCaseWidestConfigStaysExact() {
        val widest = PolynomialGenerator(GenerationConfig(-1000..1000))
        for (seed in 0L until 500L) {
            for (rootList in listOf(listOf(100, 100, 100), listOf(-100, -100, -100), listOf(100, -100, 100))) {
                val equation = widest
                    .generatePolynomial(GenerationRequest.Polynomial(rootList), Random(seed))
                    .equation
                assertEquals(
                    vieta(rootList, equation.leadingCoefficient),
                    equation.coefficients.map { it.toLong() },
                )
                for (root in rootList) assertEquals(0L, equation.residual(root))
            }
        }
    }

    @Test
    fun outOfRangeInputThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            generator.generatePolynomial(GenerationRequest.Polynomial(101, 0), Random(1L))
        }
        assertThrows(IllegalArgumentException::class.java) {
            generator.generatePolynomial(GenerationRequest.Polynomial(0, -101), Random(1L))
        }
        assertThrows(IllegalArgumentException::class.java) {
            generator.generatePolynomial(GenerationRequest.Polynomial(0, 0, 101), Random(1L))
        }
        assertThrows(IllegalArgumentException::class.java) {
            generator.generatePolynomial(GenerationRequest.Polynomial(Int.MAX_VALUE, 0, 0), Random(1L))
        }
    }

    @Test
    fun customConfig_isRespected() {
        val narrow = PolynomialGenerator(GenerationConfig(2..3))
        for (seed in seeds) {
            val a = narrow.generateLinear(GenerationRequest.LinearOneVar(4), Random(seed)).equation.a
            assertTrue(a == 2 || a == 3)
            val p = narrow.generatePolynomial(GenerationRequest.Polynomial(4, 5, 6), Random(seed)).equation
            assertTrue(p.leadingCoefficient == 2 || p.leadingCoefficient == 3)
        }
    }
}
