package com.sanket.eqgen.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IntInputTest {

    // ---- sanitize ----

    @Test
    fun sanitize_keepsDigitsAndLeadingMinus() {
        assertEquals("", IntInput.sanitize(""))
        assertEquals("12", IntInput.sanitize("12"))
        assertEquals("-12", IntInput.sanitize("-12"))
        assertEquals("-", IntInput.sanitize("-"))
    }

    @Test
    fun sanitize_dropsEverythingElse() {
        assertEquals("123", IntInput.sanitize("1.2,3"))
        assertEquals("12", IntInput.sanitize(" 1 2 "))
        assertEquals("5", IntInput.sanitize("a5b"))
        assertEquals("-5", IntInput.sanitize("-5x"))
        assertEquals("", IntInput.sanitize("abc"))
    }

    @Test
    fun sanitize_allowsMinusOnlyAtTheStart() {
        assertEquals("53", IntInput.sanitize("5-3"))
        assertEquals("5", IntInput.sanitize("5-"))
        assertEquals("-5", IntInput.sanitize("--5"))
        assertEquals("5", IntInput.sanitize("+5"))
    }

    @Test
    fun sanitize_limitsDigitCount() {
        assertEquals(3, IntInput.MAX_DIGITS)
        assertEquals(4, IntInput.MAX_TYPED_DIGITS)
        assertEquals("1234", IntInput.sanitize("12345"))
        assertEquals("-1234", IntInput.sanitize("-12345"))
    }

    @Test
    fun sanitize_dropsNonAsciiDigits() {
        assertEquals("", IntInput.sanitize("٣")) // Arabic-Indic three
        assertEquals("", IntInput.sanitize("３")) // fullwidth three
    }

    @Test
    fun sanitize_isIdempotent() {
        for (raw in listOf("", "-", "7", "-7", "1.5", "--9", "12345", "a-b")) {
            val once = IntInput.sanitize(raw)
            assertEquals(once, IntInput.sanitize(once))
        }
    }

    // ---- validate ----

    @Test
    fun validate_emptyAndLoneMinusAreIncomplete_notErrors() {
        assertEquals(FieldValidation.Incomplete, IntInput.validate(""))
        assertEquals(FieldValidation.Incomplete, IntInput.validate("-"))
    }

    @Test
    fun validate_acceptsWholeNumbersInRange() {
        assertEquals(FieldValidation.Valid(0), IntInput.validate("0"))
        assertEquals(FieldValidation.Valid(7), IntInput.validate("7"))
        assertEquals(FieldValidation.Valid(-7), IntInput.validate("-7"))
        assertEquals(FieldValidation.Valid(100), IntInput.validate("100"))
        assertEquals(FieldValidation.Valid(-100), IntInput.validate("-100"))
        assertEquals(FieldValidation.Valid(7), IntInput.validate("007"))
        assertEquals(FieldValidation.Valid(0), IntInput.validate("-0"))
    }

    @Test
    fun validate_rejectsOutOfRange() {
        for (text in listOf("101", "-101", "999", "-999", "2147483648", "99999999999999999999")) {
            assertEquals(text, FieldValidation.Invalid(InputError.OutOfRange), IntInput.validate(text))
        }
    }

    @Test
    fun validate_rejectsNonNumbers() {
        for (text in listOf("abc", "1.5", "1,5", "+5", "5-", "--5", " 5", "5 ", "٣", "1e2")) {
            assertEquals(text, FieldValidation.Invalid(InputError.NotAWholeNumber), IntInput.validate(text))
        }
    }

    @Test
    fun validate_acceptsEverythingSanitizeProducesInRange() {
        for (value in -100..100) {
            assertEquals(FieldValidation.Valid(value), IntInput.validate(IntInput.sanitize(value.toString())))
        }
    }

    // ---- toggleSign ----

    @Test
    fun toggleSign_flipsTheSign() {
        assertEquals("-5", IntInput.toggleSign("5"))
        assertEquals("5", IntInput.toggleSign("-5"))
        assertEquals("-", IntInput.toggleSign(""))
        assertEquals("", IntInput.toggleSign("-"))
        assertEquals("-0", IntInput.toggleSign("0"))
    }

    @Test
    fun toggleSign_twiceRestoresTheText() {
        for (text in listOf("", "5", "-5", "123", "-", "0")) {
            assertEquals(text, IntInput.toggleSign(IntInput.toggleSign(text)))
        }
    }

    @Test
    fun toggleSign_staysAValidSanitizedFormat() {
        for (text in listOf("", "5", "-5", "123", "-123")) {
            val toggled = IntInput.toggleSign(text)
            assertEquals(toggled, IntInput.sanitize(toggled))
        }
    }

    // ---- randomValue ----

    @Test
    fun randomValue_staysInRange_neverZero_andCoversAllTwentyValues() {
        val random = kotlin.random.Random(3)
        val seen = (0 until 5_000).map { IntInput.randomValue(random) }.toSet()
        assertEquals((-10..10).filter { it != 0 }.toSet(), seen)
    }

    @Test
    fun randomValue_isDeterministicForTheSameRandom() {
        assertEquals(
            List(20) { IntInput.randomValue(kotlin.random.Random(4)) },
            List(20) { IntInput.randomValue(kotlin.random.Random(4)) },
        )
    }

    @Test
    fun randomValue_isAlwaysAcceptedByValidation() {
        val random = kotlin.random.Random(8)
        repeat(500) {
            val text = IntInput.randomValue(random).toString()
            assertEquals(text, IntInput.sanitize(text))
            assertTrue(IntInput.validate(text) is FieldValidation.Valid)
        }
    }

    // ---- paste never turns an out-of-range number into a valid one ----

    @Test
    fun sanitize_keepsFourDigits_soTheRangeErrorShows() {
        assertEquals("1000", IntInput.sanitize("1000"))
        assertEquals(FieldValidation.Invalid(InputError.OutOfRange), IntInput.validate(IntInput.sanitize("1000")))
        assertEquals(FieldValidation.Invalid(InputError.OutOfRange), IntInput.validate(IntInput.sanitize("-1000")))
    }

    @Test
    fun sanitize_neverMakesALongNumberValid() {
        val random = kotlin.random.Random(6)
        repeat(2_000) {
            val length = random.nextInt(4, 15)
            val digits = buildString {
                append(random.nextInt(1, 10)) // no leading zero
                repeat(length - 1) { append(random.nextInt(10)) }
            }
            for (raw in listOf(digits, "-$digits")) {
                val result = IntInput.validate(IntInput.sanitize(raw))
                assertTrue("$raw -> $result", result is FieldValidation.Invalid)
            }
        }
    }
}
