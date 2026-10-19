package com.sanket.eqgen.domain.model

/** `ax + b = 0`. [a] is never zero. */
data class LinearEquation1D(val a: Int, val b: Int) {
    init {
        require(a != 0) { "a must be non-zero" }
    }

    /** `a·x + b`; zero exactly when [x] is the root. */
    fun residual(x: Int): Int = a * x + b
}

/** `ax + by = c`. [a] and [b] are never both zero. */
data class LinearEquation2D(val a: Int, val b: Int, val c: Int) {
    init {
        require(a != 0 || b != 0) { "a and b must not both be zero" }
    }

    /** `a·x + b·y − c`; zero exactly when `(x, y)` lies on the line. */
    fun residual(x: Int, y: Int): Int = a * x + b * y - c
}

/** `ax + by + cz = d`. [a], [b] and [c] are never all zero. */
data class LinearEquation3D(val a: Int, val b: Int, val c: Int, val d: Int) {
    init {
        require(a != 0 || b != 0 || c != 0) { "a, b and c must not all be zero" }
    }

    /** `a·x + b·y + c·z − d`; zero exactly when `(x, y, z)` lies on the plane. */
    fun residual(x: Int, y: Int, z: Int): Int = a * x + b * y + c * z - d
}

/**
 * `a(x − r1)(x − r2)(…)` expanded, stored as its roots and leading coefficient. The degree is the
 * number of roots. The other coefficients are derived (Vieta), so they cannot disagree with the roots.
 * Repeated roots are kept exactly as given.
 *
 * Coefficients are computed with overflow-checked `Long` arithmetic and narrowed to `Int`; a polynomial
 * whose coefficients do not fit is rejected with [IllegalArgumentException], which cannot happen for
 * inputs within the limits in the spec.
 */
data class PolynomialEquation(val roots: List<Int>, val leadingCoefficient: Int) {
    init {
        require(roots.size in MIN_POLYNOMIAL_DEGREE..MAX_POLYNOMIAL_DEGREE) {
            "Degree must be $MIN_POLYNOMIAL_DEGREE to $MAX_POLYNOMIAL_DEGREE: ${roots.size}"
        }
        require(leadingCoefficient != 0) { "leadingCoefficient must be non-zero" }
    }

    /** Number of roots. */
    val degree: Int get() = roots.size

    /** Coefficients from the highest power down to the constant term; size is `degree + 1`. */
    val coefficients: List<Int> = expand(roots, leadingCoefficient)

    /** `p(x)` evaluated exactly; zero exactly when [x] is a root. */
    fun residual(x: Int): Long = coefficients.fold(0L) { acc, coefficient -> acc * x + coefficient }

    /**
     * `p'(x)` evaluated exactly. It is also zero at a root that was entered two or more times,
     * which is how repeated roots are told apart from simple ones.
     */
    fun derivativeAt(x: Int): Long {
        val power = degree
        return coefficients.dropLast(1).foldIndexed(0L) { index, acc, coefficient ->
            acc * x + coefficient.toLong() * (power - index)
        }
    }

    private companion object {
        fun expand(roots: List<Int>, leadingCoefficient: Int): List<Int> {
            try {
                var terms = longArrayOf(leadingCoefficient.toLong())
                for (root in roots) {
                    val next = LongArray(terms.size + 1)
                    for (i in terms.indices) {
                        next[i] = Math.addExact(next[i], terms[i])
                        next[i + 1] = Math.subtractExact(next[i + 1], Math.multiplyExact(root.toLong(), terms[i]))
                    }
                    terms = next
                }
                return terms.map { Math.toIntExact(it) }
            } catch (e: ArithmeticException) {
                throw IllegalArgumentException("Coefficients do not fit in Int: roots=$roots a=$leadingCoefficient", e)
            }
        }
    }
}

/** A generated equation (or system) together with the solution it was built around. */
sealed interface GeneratedEquation {

    data class LinearOneVar(
        val equation: LinearEquation1D,
        val root: Int,
    ) : GeneratedEquation

    data class LinearTwoVar(
        val equation: LinearEquation2D,
        val x: Int,
        val y: Int,
    ) : GeneratedEquation

    data class LinearPair(
        val first: LinearEquation2D,
        val second: LinearEquation2D,
        val x: Int,
        val y: Int,
    ) : GeneratedEquation {
        /** `a1·b2 − a2·b1`; non-zero means the two lines meet in exactly one point. */
        val determinant: Int get() = first.a * second.b - second.a * first.b

        init {
            require(determinant != 0) { "Pair must have a unique solution (non-zero determinant)" }
        }
    }

    /** Three planes meeting in the single point `(x, y, z)`. */
    data class LinearSystem3(
        val equations: List<LinearEquation3D>,
        val x: Int,
        val y: Int,
        val z: Int,
    ) : GeneratedEquation {
        /**
         * The 3×3 determinant of the coefficients, computed as `Long` because with the widest allowed
         * config it can exceed `Int`. Non-zero means a unique solution.
         */
        val determinant: Long get() = determinantOf(equations)

        init {
            require(equations.size == 3) { "A system needs exactly 3 equations: ${equations.size}" }
            require(determinant != 0L) { "System must have a unique solution (non-zero determinant)" }
        }

        companion object {
            /** Determinant of the coefficient matrix of three [LinearEquation3D] rows. */
            fun determinantOf(rows: List<LinearEquation3D>): Long {
                require(rows.size == 3) { "Needs exactly 3 rows: ${rows.size}" }
                val (r1, r2, r3) = rows
                return r1.a.toLong() * (r2.b.toLong() * r3.c - r3.b.toLong() * r2.c) -
                    r1.b.toLong() * (r2.a.toLong() * r3.c - r3.a.toLong() * r2.c) +
                    r1.c.toLong() * (r2.a.toLong() * r3.b - r3.a.toLong() * r2.b)
            }
        }
    }

    /** M4 (quadratic) and M6 (cubic). */
    data class Polynomial(val equation: PolynomialEquation) : GeneratedEquation
}

/** One generation outcome: the request, what was generated, and the seed that reproduces it. */
data class GenerationResult(
    val request: GenerationRequest,
    val equation: GeneratedEquation,
    val seed: Long,
)
