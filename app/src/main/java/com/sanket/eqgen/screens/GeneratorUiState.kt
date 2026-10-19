package com.sanket.eqgen.screens

import com.sanket.eqgen.domain.model.GenerationRequest
import com.sanket.eqgen.domain.model.GenerationResult
import kotlin.random.Random

/** An input box on the screen. One field can serve several modes (X and Y are shared by M2 and M3). */
enum class InputField { Root, X, Y, Z, Root1, Root2, Root3 }

/** The six v1 modes (spec section 02) in tile order, with the input fields each one needs. */
enum class GeneratorMode(val fields: List<InputField>) {
    LinearOneVar(listOf(InputField.Root)),
    LinearTwoVar(listOf(InputField.X, InputField.Y)),
    LinearPair(listOf(InputField.X, InputField.Y)),
    Quadratic(listOf(InputField.Root1, InputField.Root2)),
    LinearSystem3(listOf(InputField.X, InputField.Y, InputField.Z)),
    Cubic(listOf(InputField.Root1, InputField.Root2, InputField.Root3)),
}

/** What one field needs to display: its text and, when the text is unusable, why. */
data class FieldState(val field: InputField, val text: String, val error: InputError?)

/**
 * Everything the screen shows (spec section 04). Immutable; the `with…` functions return updated copies.
 *
 * Decisions this encodes:
 * - Editing an input never clears [result] (D-013).
 * - Switching mode keeps both the typed inputs and the [result].
 * - There is no Retry: Generate on inputs equal to [result]'s request regenerates (spec section 03).
 * - [setupExpanded] and [focusedField] live here so they survive rotation.
 */
data class GeneratorUiState(
    val mode: GeneratorMode = GeneratorMode.LinearOneVar,
    val inputs: Map<InputField, String> = emptyMap(),
    val result: GenerationResult? = null,
    val setupExpanded: Boolean = true,
    val focusedField: InputField? = null,
) {

    /** The fields of the current [mode], each with its text and validation error. */
    val fields: List<FieldState> = mode.fields.map { field ->
        val text = inputs[field].orEmpty()
        val error = (IntInput.validate(text) as? FieldValidation.Invalid)?.error
        FieldState(field, text, error)
    }

    /** The request built from the current inputs, or null while any field is empty, incomplete or invalid. */
    val request: GenerationRequest? = buildRequest()

    val canGenerate: Boolean get() = request != null

    /** True when the sign button has a field to act on: the focused field belongs to the current mode. */
    val canToggleSign: Boolean get() = focusedField != null && focusedField in mode.fields

    /** True when Generate would regenerate the displayed result rather than create a new request. */
    val isRegeneration: Boolean get() = request != null && request == result?.request

    fun withMode(newMode: GeneratorMode): GeneratorUiState = copy(mode = newMode)

    /** Stores [rawText] after cleaning it with [IntInput.sanitize]. Fields outside the current mode are ignored. */
    fun withInput(field: InputField, rawText: String): GeneratorUiState =
        if (field !in mode.fields) this else copy(inputs = inputs + (field to IntInput.sanitize(rawText)))

    fun withFocus(field: InputField?): GeneratorUiState = copy(focusedField = field)

    fun withSetupExpanded(expanded: Boolean): GeneratorUiState = copy(setupExpanded = expanded)

    /** Flips the sign of the focused field. Does nothing when [canToggleSign] is false. */
    fun withToggledFocusedSign(): GeneratorUiState =
        if (canToggleSign) withToggledSign(focusedField!!) else this

    /** Fills every field of the current mode with [IntInput.randomValue]. Leaves [result] untouched. */
    fun withRandomInputs(random: Random): GeneratorUiState =
        copy(inputs = inputs + mode.fields.associateWith { IntInput.randomValue(random).toString() })

    /** Flips the sign of one field's text. Fields outside the current mode are ignored. */
    fun withToggledSign(field: InputField): GeneratorUiState =
        if (field !in mode.fields) this else withInput(field, IntInput.toggleSign(inputs[field].orEmpty()))

    private fun buildRequest(): GenerationRequest? {
        val values = mode.fields.map { field ->
            val validation = IntInput.validate(inputs[field].orEmpty())
            (validation as? FieldValidation.Valid)?.value ?: return null
        }
        return when (mode) {
            GeneratorMode.LinearOneVar -> GenerationRequest.LinearOneVar(values[0])
            GeneratorMode.LinearTwoVar -> GenerationRequest.LinearTwoVar(values[0], values[1])
            GeneratorMode.LinearPair -> GenerationRequest.LinearPair(values[0], values[1])
            GeneratorMode.Quadratic -> GenerationRequest.Polynomial(values)
            GeneratorMode.Cubic -> GenerationRequest.Polynomial(values)
            GeneratorMode.LinearSystem3 -> GenerationRequest.LinearSystem3(values[0], values[1], values[2])
        }
    }
}
