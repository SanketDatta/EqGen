package com.sanket.eqgen.screens

import com.sanket.eqgen.domain.model.MAX_INPUT_ABS
import com.sanket.eqgen.domain.model.isWithinInputLimit
import kotlin.random.Random

/** Why a field's text cannot be used. Mapped to a message by the UI. */
enum class InputError {
    /** Not an optional minus followed by digits. Only reachable if text bypasses [IntInput.sanitize]. */
    NotAWholeNumber,

    /** A whole number, but outside `[-MAX_INPUT_ABS, MAX_INPUT_ABS]`. */
    OutOfRange,
}

/** Result of validating one field's text. */
sealed interface FieldValidation {
    /** Empty or just a minus sign: the user is still typing, so no error is shown, but it is not usable yet. */
    data object Incomplete : FieldValidation

    data class Valid(val value: Int) : FieldValidation

    data class Invalid(val error: InputError) : FieldValidation
}

/**
 * Parsing rules for the integer input fields (spec section 05): integers only, optional leading minus.
 * Pure Kotlin, so it is covered by ordinary JVM tests.
 */
object IntInput {

    /** Longest digit run a field accepts; one more than this could never be in range. */
    val MAX_DIGITS: Int = MAX_INPUT_ABS.toString().length

    /**
     * Digits a field keeps while typing or pasting: one more than [MAX_DIGITS], so a value like 1000 stays
     * visible and gets the out-of-range error instead of being silently cut to 100. Any digit run this long
     * is out of range, and cutting a longer one to this length keeps it out of range (leading zeros aside).
     */
    val MAX_TYPED_DIGITS: Int = MAX_DIGITS + 1

    private val WHOLE_NUMBER = Regex("-?[0-9]+")

    /**
     * Cleans raw keyboard text: keeps at most one leading `-`, keeps ASCII digits only, and limits
     * the digit count to [MAX_DIGITS]. Anything else (spaces, dots, commas, letters) is dropped.
     */
    fun sanitize(raw: String): String {
        val digits = raw.filter { it in '0'..'9' }.take(MAX_TYPED_DIGITS)
        return if (raw.startsWith('-')) "-$digits" else digits
    }

    fun validate(text: String): FieldValidation {
        if (text.isEmpty() || text == "-") return FieldValidation.Incomplete
        if (!WHOLE_NUMBER.matches(text)) return FieldValidation.Invalid(InputError.NotAWholeNumber)
        // A digit string too long for Int is certainly out of range.
        val value = text.toIntOrNull() ?: return FieldValidation.Invalid(InputError.OutOfRange)
        return if (value.isWithinInputLimit()) {
            FieldValidation.Valid(value)
        } else {
            FieldValidation.Invalid(InputError.OutOfRange)
        }
    }

    /**
     * Flips the sign of the text: `5` becomes `-5`, `-5` becomes `5`, an empty field becomes `-`.
     * Backs the sign button, because numeric keyboards do not reliably offer a minus key.
     */
    fun toggleSign(text: String): String = if (text.startsWith('-')) text.drop(1) else "-$text"

    /** Largest magnitude Randomize produces. */
    const val RANDOM_LIMIT: Int = 10

    /** A random integer in `-RANDOM_LIMIT..RANDOM_LIMIT`, never 0 (spec D-030). */
    fun randomValue(random: Random): Int {
        val n = random.nextInt(2 * RANDOM_LIMIT)
        return if (n < RANDOM_LIMIT) n - RANDOM_LIMIT else n - RANDOM_LIMIT + 1
    }
}
