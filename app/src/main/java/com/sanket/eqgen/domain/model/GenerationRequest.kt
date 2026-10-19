package com.sanket.eqgen.domain.model

/** Fewest roots a [GenerationRequest.Polynomial] accepts: a quadratic (a single root is mode M1). */
const val MIN_POLYNOMIAL_DEGREE: Int = 2

/**
 * Most roots a [GenerationRequest.Polynomial] accepts in v1.
 *
 * The overflow analysis in the spec (section 03) holds only up to degree 3: the constant term of a
 * cubic is at most `1000 * 100 * 100 * 100 = 1,000,000,000`, which still fits in `Int`. Raising this
 * limit means redoing that analysis.
 */
const val MAX_POLYNOMIAL_DEGREE: Int = 3

/**
 * What the user asked for: the solution the generated equation must have (spec section 02).
 *
 * Requests hold only user input. They never hold generated coefficients or formatted text.
 */
sealed interface GenerationRequest {

    /** Every user-entered number in this request, used for input-limit validation. */
    val inputs: List<Int>

    /** M1: `ax + b = 0` with the given root. */
    data class LinearOneVar(val root: Int) : GenerationRequest {
        override val inputs: List<Int> get() = listOf(root)
    }

    /** M2: `ax + by = c` passing through the integer solution `(x, y)`. */
    data class LinearTwoVar(val x: Int, val y: Int) : GenerationRequest {
        override val inputs: List<Int> get() = listOf(x, y)
    }

    /** M3: a pair of `ax + by = c` equations whose unique solution is `(x, y)`. */
    data class LinearPair(val x: Int, val y: Int) : GenerationRequest {
        override val inputs: List<Int> get() = listOf(x, y)
    }

    /** M5: three `ax + by + cz = d` equations whose unique solution is `(x, y, z)`. */
    data class LinearSystem3(val x: Int, val y: Int, val z: Int) : GenerationRequest {
        override val inputs: List<Int> get() = listOf(x, y, z)
    }

    /**
     * M4 and M6: `a(x − r1)(x − r2)(…)` expanded, with exactly the given [roots] (the degree is the
     * number of roots). Repeated roots are allowed and are never altered (spec section 03).
     */
    data class Polynomial(val roots: List<Int>) : GenerationRequest {
        init {
            require(roots.size in MIN_POLYNOMIAL_DEGREE..MAX_POLYNOMIAL_DEGREE) {
                "A polynomial needs $MIN_POLYNOMIAL_DEGREE to $MAX_POLYNOMIAL_DEGREE roots: $roots"
            }
        }

        /** Convenience for `Polynomial(2, 3)`. */
        constructor(vararg roots: Int) : this(roots.toList())

        override val inputs: List<Int> get() = roots
    }
}

/**
 * Throws [IllegalArgumentException] if any input is outside `[-MAX_INPUT_ABS, MAX_INPUT_ABS]`.
 * The UI is expected to block such input first, so reaching this is a programmer error (spec: Validation).
 */
fun GenerationRequest.requireWithinInputLimit() {
    require(inputs.all { it.isWithinInputLimit() }) {
        "Inputs must be within ±$MAX_INPUT_ABS: $this"
    }
}
