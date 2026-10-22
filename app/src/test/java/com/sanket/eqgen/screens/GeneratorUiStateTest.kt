package com.sanket.eqgen.screens

import com.sanket.eqgen.domain.generator.EquationEngine
import com.sanket.eqgen.domain.model.GenerationRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import kotlin.random.Random
import org.junit.Test

class GeneratorUiStateTest {

    private val initial = GeneratorUiState()

    private fun GeneratorUiState.type(field: InputField, text: String) = withInput(field, text)

    // ---- initial state ----

    @Test
    fun initialState_isLinearOneVar_withEmptyField_andNothingEnabled() {
        assertEquals(GeneratorMode.LinearOneVar, initial.mode)
        assertEquals(listOf(FieldState(InputField.Root, "", null)), initial.fields)
        assertNull(initial.request)
        assertFalse(initial.canGenerate)
        assertFalse(initial.canToggleSign)
        assertTrue(initial.setupExpanded)
        assertNull(initial.result)
    }

    @Test
    fun modes_listTheirFieldsInOrder() {
        assertEquals(listOf(InputField.Root), GeneratorMode.LinearOneVar.fields)
        assertEquals(listOf(InputField.X, InputField.Y), GeneratorMode.LinearTwoVar.fields)
        assertEquals(listOf(InputField.X, InputField.Y), GeneratorMode.LinearPair.fields)
        assertEquals(listOf(InputField.Root1, InputField.Root2), GeneratorMode.Quadratic.fields)
        assertEquals(listOf(InputField.X, InputField.Y, InputField.Z), GeneratorMode.LinearSystem3.fields)
        assertEquals(
            listOf(InputField.Root1, InputField.Root2, InputField.Root3),
            GeneratorMode.Cubic.fields,
        )
        assertEquals(6, GeneratorMode.entries.size)
    }

    @Test
    fun system3_andCubic_buildTheirRequests_andNeedAllThreeFields() {
        val system = initial.withMode(GeneratorMode.LinearSystem3)
            .type(InputField.X, "1").type(InputField.Y, "-2")
        assertNull(system.request)
        assertEquals(GenerationRequest.LinearSystem3(1, -2, 3), system.type(InputField.Z, "3").request)

        val cubic = initial.withMode(GeneratorMode.Cubic)
            .type(InputField.Root1, "4").type(InputField.Root2, "4")
        assertNull(cubic.request)
        assertEquals(GenerationRequest.Polynomial(4, 4, 0), cubic.type(InputField.Root3, "0").request)
    }

    @Test
    fun sharedXY_carryBetweenPairAndSystem3() {
        val state = initial.withMode(GeneratorMode.LinearPair).type(InputField.X, "2").type(InputField.Y, "5")
            .withMode(GeneratorMode.LinearSystem3)
        assertEquals(listOf("2", "5", ""), state.fields.map { it.text })
    }

    // ---- building requests ----

    @Test
    fun request_isBuiltForEveryMode_whenAllFieldsAreValid() {
        assertEquals(
            GenerationRequest.LinearOneVar(-3),
            initial.type(InputField.Root, "-3").request,
        )
        assertEquals(
            GenerationRequest.LinearTwoVar(2, -5),
            initial.withMode(GeneratorMode.LinearTwoVar).type(InputField.X, "2").type(InputField.Y, "-5").request,
        )
        assertEquals(
            GenerationRequest.LinearPair(0, 100),
            initial.withMode(GeneratorMode.LinearPair).type(InputField.X, "0").type(InputField.Y, "100").request,
        )
        assertEquals(
            GenerationRequest.Polynomial(4, 4),
            initial.withMode(GeneratorMode.Quadratic).type(InputField.Root1, "4").type(InputField.Root2, "4").request,
        )
    }

    @Test
    fun canGenerate_requiresEveryFieldOfTheMode() {
        val twoVar = initial.withMode(GeneratorMode.LinearTwoVar)
        assertFalse(twoVar.canGenerate)
        assertFalse(twoVar.type(InputField.X, "1").canGenerate)
        assertFalse(twoVar.type(InputField.Y, "1").canGenerate)
        assertTrue(twoVar.type(InputField.X, "1").type(InputField.Y, "1").canGenerate)
    }

    @Test
    fun incompleteFields_blockGenerate_butShowNoError() {
        val state = initial.type(InputField.Root, "-")
        assertFalse(state.canGenerate)
        assertNull(state.fields.single().error)
    }

    @Test
    fun outOfRangeFields_blockGenerate_andShowTheError() {
        val state = initial.type(InputField.Root, "101")
        assertFalse(state.canGenerate)
        assertEquals(InputError.OutOfRange, state.fields.single().error)

        val fixed = state.type(InputField.Root, "100")
        assertTrue(fixed.canGenerate)
        assertNull(fixed.fields.single().error)
    }

    @Test
    fun errorsAreReportedPerField() {
        val state = initial.withMode(GeneratorMode.Quadratic)
            .type(InputField.Root1, "5")
            .type(InputField.Root2, "999")
        assertNull(state.fields[0].error)
        assertEquals(InputError.OutOfRange, state.fields[1].error)
        assertFalse(state.canGenerate)
    }

    // ---- input cleaning ----

    @Test
    fun withInput_sanitizesText() {
        assertEquals("12", initial.type(InputField.Root, "1.2").fields.single().text)
        assertEquals("-5", initial.type(InputField.Root, "-5x").fields.single().text)
        assertEquals("1234", initial.type(InputField.Root, "12345").fields.single().text)
    }

    @Test
    fun withInput_ignoresFieldsOutsideTheCurrentMode() {
        val state = initial.type(InputField.Y, "5")
        assertSame(initial, state)
        assertTrue(state.inputs.isEmpty())
    }

    @Test
    fun withToggledSign_flipsOneFieldOnly() {
        val state = initial.withMode(GeneratorMode.LinearTwoVar)
            .type(InputField.X, "3")
            .type(InputField.Y, "4")
            .withToggledSign(InputField.X)
        assertEquals("-3", state.fields[0].text)
        assertEquals("4", state.fields[1].text)
        assertEquals("3", state.withToggledSign(InputField.X).fields[0].text)
    }

    @Test
    fun withToggledSign_onAnEmptyField_startsANegativeNumber() {
        val state = initial.withToggledSign(InputField.Root)
        assertEquals("-", state.fields.single().text)
        assertNull(state.fields.single().error)
        assertFalse(state.canGenerate)
        assertEquals(GenerationRequest.LinearOneVar(-7), state.type(InputField.Root, "-7").request)
    }

    @Test
    fun withToggledSign_ignoresFieldsOutsideTheCurrentMode() {
        assertSame(initial, initial.withToggledSign(InputField.Root1))
    }

    // ---- switching modes ----

    @Test
    fun switchingMode_keepsSharedInputs() {
        val twoVar = initial.withMode(GeneratorMode.LinearTwoVar)
            .type(InputField.X, "2").type(InputField.Y, "-5")
        val pair = twoVar.withMode(GeneratorMode.LinearPair)
        assertEquals(listOf("2", "-5"), pair.fields.map { it.text })
        assertEquals(GenerationRequest.LinearPair(2, -5), pair.request)
    }

    @Test
    fun switchingMode_keepsInputsOfModesNotCurrentlyShown() {
        val state = initial.type(InputField.Root, "7")
            .withMode(GeneratorMode.Quadratic)
            .withMode(GeneratorMode.LinearOneVar)
        assertEquals("7", state.fields.single().text)
    }

    @Test
    fun switchingMode_toOneWithEmptyFields_disablesGenerate() {
        val ready = initial.type(InputField.Root, "7")
        assertTrue(ready.canGenerate)
        assertFalse(ready.withMode(GeneratorMode.Quadratic).canGenerate)
    }

    // ---- result persistence (D-013) ----

    private val sampleResult = EquationEngine().generate(GenerationRequest.LinearOneVar(3), seed = 1L)

    @Test
    fun editingInputs_doesNotClearTheResult() {
        val state = GeneratorUiState(result = sampleResult)
        assertSame(sampleResult, state.type(InputField.Root, "9").result)
        assertSame(sampleResult, state.withToggledSign(InputField.Root).result)
        assertSame(sampleResult, state.type(InputField.Root, "").result)
    }

    @Test
    fun switchingMode_doesNotClearTheResult() {
        val state = GeneratorUiState(result = sampleResult)
        assertSame(sampleResult, state.withMode(GeneratorMode.Quadratic).result)
    }

    @Test
    fun isRegeneration_whenParsedInputsEqualTheDisplayedRequest() {
        val state = GeneratorUiState(result = sampleResult)
        assertFalse(state.isRegeneration) // fields empty
        assertTrue(state.type(InputField.Root, "3").isRegeneration)
        assertTrue(state.type(InputField.Root, "003").isRegeneration)
        assertFalse(state.type(InputField.Root, "4").isRegeneration)
        assertFalse(state.type(InputField.Root, "-").isRegeneration)
        assertFalse(initial.type(InputField.Root, "3").isRegeneration) // no result yet
    }

    @Test
    fun focus_andSignToggle_followTheFocusedField() {
        val state = initial.withMode(GeneratorMode.LinearTwoVar).type(InputField.X, "5")
        assertFalse(state.canToggleSign)
        assertSame(state, state.withToggledFocusedSign())

        val focused = state.withFocus(InputField.X)
        assertTrue(focused.canToggleSign)
        assertEquals("-5", focused.withToggledFocusedSign().inputs[InputField.X])
        assertEquals("-", focused.type(InputField.X, "").withToggledFocusedSign().inputs[InputField.X])

        assertFalse(state.withFocus(InputField.Z).canToggleSign)
        assertFalse(focused.withFocus(null).canToggleSign)
    }

    @Test
    fun randomInputs_fillOnlyTheModesFields_withValidValues_andKeepTheResult() {
        val state = GeneratorUiState(result = sampleResult).withMode(GeneratorMode.LinearSystem3)
        val random = Random(5)
        repeat(200) {
            val filled = state.withRandomInputs(random)
            assertTrue(filled.canGenerate)
            assertEquals(setOf(InputField.X, InputField.Y, InputField.Z), filled.inputs.keys)
            assertSame(sampleResult, filled.result)
        }
    }

    @Test
    fun switchingMode_clearsTheFocusedField() {
        val state = initial.withMode(GeneratorMode.LinearTwoVar).withFocus(InputField.X)
        assertTrue(state.canToggleSign)
        val switched = state.withMode(GeneratorMode.LinearPair) // X exists here too
        assertNull(switched.focusedField)
        assertFalse(switched.canToggleSign)
    }

    @Test
    fun setupExpanded_isPartOfTheState() {
        assertFalse(initial.withSetupExpanded(false).setupExpanded)
        assertTrue(initial.withSetupExpanded(false).withSetupExpanded(true).setupExpanded)
    }

    @Test
    fun statesWithSameDataAreEqual() {
        val a = initial.type(InputField.Root, "5")
        val b = initial.type(InputField.Root, "5")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }
}
