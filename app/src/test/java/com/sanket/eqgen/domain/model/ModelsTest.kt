package com.sanket.eqgen.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsTest {

    // ---- linear equations ----

    @Test
    fun linearEquation1D_rejectsZeroA() {
        assertThrows(IllegalArgumentException::class.java) { LinearEquation1D(a = 0, b = 3) }
    }

    @Test
    fun linearEquation1D_residualIsZeroAtRoot() {
        val equation = LinearEquation1D(a = 4, b = -12) // root 3
        assertEquals(0, equation.residual(3))
        assertEquals(4, equation.residual(4))
    }

    @Test
    fun linearEquation2D_rejectsBothCoefficientsZero() {
        assertThrows(IllegalArgumentException::class.java) { LinearEquation2D(a = 0, b = 0, c = 5) }
    }

    @Test
    fun linearEquation2D_residualIsZeroOnLine() {
        val equation = LinearEquation2D(a = 2, b = -3, c = 7) // (2, -1): 4 + 3 = 7
        assertEquals(0, equation.residual(2, -1))
        assertEquals(2, equation.residual(3, -1))
    }

    @Test
    fun linearEquation3D_rejectsAllCoefficientsZero() {
        assertThrows(IllegalArgumentException::class.java) { LinearEquation3D(a = 0, b = 0, c = 0, d = 5) }
        LinearEquation3D(a = 0, b = 0, c = 1, d = 5) // one non-zero coefficient is enough
    }

    @Test
    fun linearEquation3D_residualIsZeroOnPlane() {
        val equation = LinearEquation3D(a = 1, b = -2, c = 3, d = 9) // (4, 2, 3): 4 - 4 + 9 = 9
        assertEquals(0, equation.residual(4, 2, 3))
        assertEquals(1, equation.residual(5, 2, 3))
    }

    // ---- polynomials ----

    @Test
    fun polynomial_rejectsZeroLeadingCoefficient() {
        assertThrows(IllegalArgumentException::class.java) {
            PolynomialEquation(roots = listOf(1, 2), leadingCoefficient = 0)
        }
    }

    @Test
    fun polynomial_rejectsUnsupportedDegrees() {
        assertThrows(IllegalArgumentException::class.java) { PolynomialEquation(listOf(1), 1) }
        assertThrows(IllegalArgumentException::class.java) { PolynomialEquation(emptyList(), 1) }
        assertThrows(IllegalArgumentException::class.java) { PolynomialEquation(listOf(1, 2, 3, 4), 1) }
        assertEquals(MAX_POLYNOMIAL_DEGREE, PolynomialEquation(listOf(1, 2, 3), 1).degree)
    }

    @Test
    fun polynomial_quadraticCoefficientsFollowVieta() {
        val equation = PolynomialEquation(roots = listOf(2, 3), leadingCoefficient = 1)
        assertEquals(2, equation.degree)
        assertEquals(listOf(1, -5, 6), equation.coefficients)

        val scaled = PolynomialEquation(roots = listOf(-4, 6), leadingCoefficient = -3)
        assertEquals(listOf(-3, 6, 72), scaled.coefficients) // b = -a(r1+r2) = 6, c = a·r1·r2 = 72
        assertEquals(0L, scaled.residual(-4))
        assertEquals(0L, scaled.residual(6))
    }

    @Test
    fun polynomial_cubicCoefficientsFollowVieta() {
        // (x - 1)(x - 2)(x - 3) = x³ - 6x² + 11x - 6
        assertEquals(listOf(1, -6, 11, -6), PolynomialEquation(listOf(1, 2, 3), 1).coefficients)
        // roots -1, 4, 5: e1 = 8, e2 = -4 - 5 + 20 = 11, e3 = -20  ->  2x³ - 16x² + 22x + 40
        assertEquals(listOf(2, -16, 22, 40), PolynomialEquation(listOf(-1, 4, 5), 2).coefficients)
    }

    @Test
    fun polynomial_rootOrderDoesNotChangeTheCoefficients() {
        val a = PolynomialEquation(listOf(5, -2, 7), 3).coefficients
        assertEquals(a, PolynomialEquation(listOf(-2, 7, 5), 3).coefficients)
        assertEquals(a, PolynomialEquation(listOf(7, 5, -2), 3).coefficients)
    }

    @Test
    fun polynomial_repeatedRootsAreKeptAsGiven() {
        val double = PolynomialEquation(listOf(4, 4), 1)
        assertEquals(listOf(4, 4), double.roots)
        assertEquals(listOf(1, -8, 16), double.coefficients)

        val triple = PolynomialEquation(listOf(2, 2, 2), 1)
        assertEquals(listOf(1, -6, 12, -8), triple.coefficients) // (x - 2)³
    }

    @Test
    fun polynomial_derivativeIsZeroExactlyAtRepeatedRoots() {
        val doubleAndSimple = PolynomialEquation(listOf(3, 3, -5), 2)
        assertEquals(0L, doubleAndSimple.derivativeAt(3))
        assertNotEquals(0L, doubleAndSimple.derivativeAt(-5))

        val triple = PolynomialEquation(listOf(-7, -7, -7), 4)
        assertEquals(0L, triple.derivativeAt(-7))

        val distinct = PolynomialEquation(listOf(1, 2, 3), 1)
        for (root in distinct.roots) assertNotEquals(0L, distinct.derivativeAt(root))
    }

    @Test
    fun polynomial_derivativeMatchesTheHandComputedFormula() {
        // p(x) = x³ - 6x² + 11x - 6  ->  p'(x) = 3x² - 12x + 11
        val p = PolynomialEquation(listOf(1, 2, 3), 1)
        for (x in -5..5) assertEquals((3 * x * x - 12 * x + 11).toLong(), p.derivativeAt(x))
        // p(x) = 2(x - 1)(x - 4) = 2x² - 10x + 8  ->  p'(x) = 4x - 10
        val q = PolynomialEquation(listOf(1, 4), 2)
        for (x in -5..5) assertEquals((4 * x - 10).toLong(), q.derivativeAt(x))
    }

    @Test
    fun polynomial_residualIsZeroAtEveryRoot_andNowhereElseNearby() {
        val p = PolynomialEquation(listOf(-3, 0, 8), 5)
        for (root in p.roots) assertEquals(0L, p.residual(root))
        assertNotEquals(0L, p.residual(1))
    }

    @Test
    fun polynomial_worstCaseStaysExact() {
        // 1000 * 100 * 100 * 100 = 1e9: the largest constant term the widest config can produce.
        val widest = PolynomialEquation(listOf(100, 100, 100), 1000)
        assertEquals(1_000_000_000, widest.coefficients.last().let { -it })
        assertEquals(0L, widest.residual(100))
        assertEquals(1000, widest.coefficients.first())

        val negative = PolynomialEquation(listOf(-100, -100, -100), 1000)
        assertEquals(1_000_000_000, negative.coefficients.last())
        assertEquals(0L, negative.residual(-100))
    }

    @Test
    fun polynomial_coefficientsBeyondIntAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            PolynomialEquation(listOf(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE), 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PolynomialEquation(listOf(1_000_000, 1_000_000, 1_000_000), 1)
        }
    }

    // ---- systems ----

    @Test
    fun linearPair_rejectsZeroDeterminant() {
        val first = LinearEquation2D(a = 1, b = 2, c = 5)
        val parallel = LinearEquation2D(a = 2, b = 4, c = 10)
        assertThrows(IllegalArgumentException::class.java) {
            GeneratedEquation.LinearPair(first, parallel, x = 1, y = 2)
        }
    }

    @Test
    fun linearPair_computesDeterminant() {
        val pair = GeneratedEquation.LinearPair(
            first = LinearEquation2D(a = 1, b = 1, c = 3),
            second = LinearEquation2D(a = 1, b = -1, c = 1),
            x = 2,
            y = 1,
        )
        assertEquals(-2, pair.determinant) // 1*(-1) - 1*1
    }

    private val identity = listOf(
        LinearEquation3D(1, 0, 0, 1),
        LinearEquation3D(0, 1, 0, 2),
        LinearEquation3D(0, 0, 1, 3),
    )

    @Test
    fun linearSystem3_computesDeterminant() {
        assertEquals(1L, GeneratedEquation.LinearSystem3.determinantOf(identity))
        // rows (2,1,1), (1,3,2), (1,0,0): det = 2(0-0) - 1(0-2) + 1(0-3) = 2 - 3 = -1
        val rows = listOf(
            LinearEquation3D(2, 1, 1, 0),
            LinearEquation3D(1, 3, 2, 0),
            LinearEquation3D(1, 0, 0, 0),
        )
        assertEquals(-1L, GeneratedEquation.LinearSystem3.determinantOf(rows))
    }

    @Test
    fun linearSystem3_determinantDoesNotOverflowInt() {
        // With entries of 1000 the determinant is 1000³ · det([[1,1,1],[1,-1,1],[1,1,-1]]) = 4 · 10⁹,
        // which is above Int.MAX_VALUE, so it only comes out right in Long arithmetic.
        val rows = listOf(
            LinearEquation3D(1000, 1000, 1000, 0),
            LinearEquation3D(1000, -1000, 1000, 0),
            LinearEquation3D(1000, 1000, -1000, 0),
        )
        val determinant = GeneratedEquation.LinearSystem3.determinantOf(rows)
        assertEquals(4_000_000_000L, determinant)
        assertTrue(determinant > Int.MAX_VALUE.toLong())
    }

    @Test
    fun linearSystem3_rejectsSingularSystems() {
        val twoEqual = listOf(
            LinearEquation3D(1, 2, 3, 6),
            LinearEquation3D(1, 2, 3, 6),
            LinearEquation3D(0, 1, 0, 1),
        )
        assertThrows(IllegalArgumentException::class.java) {
            GeneratedEquation.LinearSystem3(twoEqual, x = 1, y = 1, z = 1)
        }
        val dependent = listOf( // third row = first + second
            LinearEquation3D(1, 0, 1, 4),
            LinearEquation3D(0, 1, 1, 5),
            LinearEquation3D(1, 1, 2, 9),
        )
        assertThrows(IllegalArgumentException::class.java) {
            GeneratedEquation.LinearSystem3(dependent, x = 1, y = 2, z = 3)
        }
    }

    @Test
    fun linearSystem3_needsExactlyThreeEquations() {
        assertThrows(IllegalArgumentException::class.java) {
            GeneratedEquation.LinearSystem3(identity.take(2), x = 1, y = 2, z = 3)
        }
        assertThrows(IllegalArgumentException::class.java) {
            GeneratedEquation.LinearSystem3(identity + identity.first(), x = 1, y = 2, z = 3)
        }
        GeneratedEquation.LinearSystem3(identity, x = 1, y = 2, z = 3)
    }

    // ---- requests ----

    @Test
    fun requests_exposeInputs() {
        assertEquals(listOf(5), GenerationRequest.LinearOneVar(5).inputs)
        assertEquals(listOf(1, -2), GenerationRequest.LinearTwoVar(1, -2).inputs)
        assertEquals(listOf(3, 4), GenerationRequest.LinearPair(3, 4).inputs)
        assertEquals(listOf(7, 8, 9), GenerationRequest.LinearSystem3(7, 8, 9).inputs)
        assertEquals(listOf(-7, 7), GenerationRequest.Polynomial(-7, 7).inputs)
        assertEquals(listOf(1, 2, 3), GenerationRequest.Polynomial(listOf(1, 2, 3)).inputs)
    }

    @Test
    fun polynomialRequest_varargAndListConstructorsAgree() {
        assertEquals(GenerationRequest.Polynomial(listOf(2, 3)), GenerationRequest.Polynomial(2, 3))
        assertEquals(
            GenerationRequest.Polynomial(listOf(2, 2, 2)).hashCode(),
            GenerationRequest.Polynomial(2, 2, 2).hashCode(),
        )
        assertNotEquals(GenerationRequest.Polynomial(2, 3), GenerationRequest.Polynomial(3, 2))
    }

    @Test
    fun polynomialRequest_enforcesTheSupportedDegrees() {
        assertThrows(IllegalArgumentException::class.java) { GenerationRequest.Polynomial(5) }
        assertThrows(IllegalArgumentException::class.java) { GenerationRequest.Polynomial() }
        assertThrows(IllegalArgumentException::class.java) { GenerationRequest.Polynomial(1, 2, 3, 4) }
        GenerationRequest.Polynomial(1, 2)
        GenerationRequest.Polynomial(1, 2, 3)
    }

    @Test
    fun polynomialRequest_neverRejectsOrAltersRepeatedRoots() {
        assertEquals(listOf(5, 5), GenerationRequest.Polynomial(5, 5).roots)
        assertEquals(listOf(0, 0, 0), GenerationRequest.Polynomial(0, 0, 0).roots)
        assertEquals(listOf(-9, 4, -9), GenerationRequest.Polynomial(-9, 4, -9).roots)
    }

    @Test
    fun inputLimit_isInclusiveAndOverflowSafe() {
        assertTrue(100.isWithinInputLimit())
        assertTrue((-100).isWithinInputLimit())
        assertTrue(0.isWithinInputLimit())
        assertFalse(101.isWithinInputLimit())
        assertFalse((-101).isWithinInputLimit())
        assertFalse(Int.MIN_VALUE.isWithinInputLimit())
        assertFalse(Int.MAX_VALUE.isWithinInputLimit())
    }

    @Test
    fun requireWithinInputLimit_checksEveryInput() {
        GenerationRequest.Polynomial(100, -100).requireWithinInputLimit()
        GenerationRequest.LinearSystem3(100, -100, 0).requireWithinInputLimit()
        assertThrows(IllegalArgumentException::class.java) {
            GenerationRequest.Polynomial(0, 101).requireWithinInputLimit()
        }
        assertThrows(IllegalArgumentException::class.java) {
            GenerationRequest.Polynomial(0, 0, -101).requireWithinInputLimit()
        }
        assertThrows(IllegalArgumentException::class.java) {
            GenerationRequest.LinearTwoVar(-101, 0).requireWithinInputLimit()
        }
        assertThrows(IllegalArgumentException::class.java) {
            GenerationRequest.LinearSystem3(0, 0, 101).requireWithinInputLimit()
        }
    }
}
