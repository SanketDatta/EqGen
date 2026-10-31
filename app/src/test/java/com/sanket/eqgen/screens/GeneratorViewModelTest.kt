package com.sanket.eqgen.screens

import com.sanket.eqgen.domain.generator.EquationEngine
import com.sanket.eqgen.domain.model.GeneratedEquation
import com.sanket.eqgen.domain.model.GenerationRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import kotlin.random.Random
import org.junit.Test

/**
 * Plain JUnit: Generate and Randomize are synchronous, so there are no coroutines or dispatchers to
 * control. `uiState.value` is read directly.
 */
class GeneratorViewModelTest {

    private fun counting(start: Long): () -> Long {
        var next = start
        return { next++ }
    }

    private fun newViewModel(seedStart: Long = 1_000L) =
        GeneratorViewModel(EquationEngine(seedSource = counting(seedStart)))

    private val GeneratorViewModel.state get() = uiState.value

    // ---- start ----

    @Test
    fun startsEmpty_withGenerateUnavailable() {
        val viewModel = newViewModel()
        assertEquals(GeneratorUiState(), viewModel.state)
        assertFalse(viewModel.state.canGenerate)
    }

    @Test
    fun noArgConstructor_forTheDefaultViewModelFactory_works() {
        val viewModel = GeneratorViewModel()
        viewModel.onInputChanged(InputField.Root, "4")
        viewModel.onGenerate()
        assertNotNull(viewModel.state.result)
    }

    // ---- Generate ----

    @Test
    fun generate_doesNothingWhileInputsAreInvalid() {
        val viewModel = newViewModel()
        val before = viewModel.state
        viewModel.onGenerate()
        assertSame(before, viewModel.state)

        viewModel.onInputChanged(InputField.Root, "-")
        viewModel.onGenerate()
        assertNull(viewModel.state.result)

        viewModel.onInputChanged(InputField.Root, "500")
        viewModel.onGenerate()
        assertNull(viewModel.state.result)
    }

    @Test
    fun generate_producesAResultForTheTypedRequest_inEveryMode() {
        val viewModel = newViewModel()

        viewModel.onInputChanged(InputField.Root, "-3")
        viewModel.onGenerate()
        val one = viewModel.state.result!!
        assertEquals(GenerationRequest.LinearOneVar(-3), one.request)
        assertTrue(one.equation is GeneratedEquation.LinearOneVar)

        viewModel.onModeSelected(GeneratorMode.LinearTwoVar)
        viewModel.onInputChanged(InputField.X, "2")
        viewModel.onInputChanged(InputField.Y, "-5")
        viewModel.onGenerate()
        assertEquals(GenerationRequest.LinearTwoVar(2, -5), viewModel.state.result!!.request)

        viewModel.onModeSelected(GeneratorMode.LinearPair)
        viewModel.onGenerate()
        assertEquals(GenerationRequest.LinearPair(2, -5), viewModel.state.result!!.request)
        assertTrue(viewModel.state.result!!.equation is GeneratedEquation.LinearPair)

        viewModel.onModeSelected(GeneratorMode.Quadratic)
        viewModel.onInputChanged(InputField.Root1, "4")
        viewModel.onInputChanged(InputField.Root2, "4")
        viewModel.onGenerate()
        assertEquals(GenerationRequest.Polynomial(4, 4), viewModel.state.result!!.request)
    }

    @Test
    fun generate_keepsTheInputs() {
        val viewModel = newViewModel()
        viewModel.onInputChanged(InputField.Root, "6")
        viewModel.onGenerate()
        assertEquals("6", viewModel.state.fields.single().text)
    }

    @Test
    fun generate_twice_usesNewSeeds() {
        val viewModel = newViewModel(seedStart = 50L)
        viewModel.onInputChanged(InputField.Root, "6")
        viewModel.onGenerate()
        val first = viewModel.state.result!!
        viewModel.onGenerate()
        val second = viewModel.state.result!!
        assertEquals(50L, first.seed)
        assertEquals(51L, second.seed)
    }

    // ---- regeneration (Generate on unchanged inputs) ----

    @Test
    fun generate_onUnchangedInputs_regeneratesADifferentEquation_forTheSameRequest() {
        val viewModel = newViewModel()
        viewModel.onModeSelected(GeneratorMode.Quadratic)
        viewModel.onInputChanged(InputField.Root1, "2")
        viewModel.onInputChanged(InputField.Root2, "3")
        viewModel.onGenerate()
        val first = viewModel.state.result!!
        val inputsBefore = viewModel.state.inputs
        assertTrue(viewModel.state.isRegeneration)

        for (i in 0 until 50) {
            val before = viewModel.state.result!!
            viewModel.onGenerate()
            val after = viewModel.state.result!!
            assertEquals(first.request, after.request)
            assertNotEquals("round $i", before.equation, after.equation)
        }
        assertEquals(inputsBefore, viewModel.state.inputs)
    }

    @Test
    fun generate_treatsEquivalentTextAsUnchanged() {
        val viewModel = newViewModel()
        viewModel.onInputChanged(InputField.Root, "7")
        viewModel.onGenerate()
        val before = viewModel.state.result!!
        viewModel.onInputChanged(InputField.Root, "007")
        viewModel.onGenerate()
        assertNotEquals(before.equation, viewModel.state.result!!.equation)
        assertEquals(before.request, viewModel.state.result!!.request)
    }

    @Test
    fun generate_afterEditingTheInputs_usesTheNewRequest() {
        val viewModel = newViewModel()
        viewModel.onInputChanged(InputField.Root, "3")
        viewModel.onGenerate()
        viewModel.onInputChanged(InputField.Root, "99")
        assertFalse(viewModel.state.isRegeneration)
        viewModel.onGenerate()
        assertEquals(GenerationRequest.LinearOneVar(99), viewModel.state.result!!.request)
    }

    @Test
    fun generate_blockedWhileInputsInvalid_keepsTheOldResult() {
        val viewModel = newViewModel()
        viewModel.onInputChanged(InputField.Root, "3")
        viewModel.onGenerate()
        val result = viewModel.state.result
        viewModel.onInputChanged(InputField.Root, "")
        viewModel.onGenerate()
        assertSame(result, viewModel.state.result)
    }

    @Test
    fun generate_afterAModeSwitchWithSharedInputs_isANewRequest() {
        val viewModel = newViewModel()
        viewModel.onInputChanged(InputField.Root, "3")
        viewModel.onGenerate()
        viewModel.onModeSelected(GeneratorMode.Quadratic)
        viewModel.onInputChanged(InputField.Root1, "3")
        viewModel.onInputChanged(InputField.Root2, "3")
        viewModel.onGenerate()
        assertEquals(GenerationRequest.Polynomial(3, 3), viewModel.state.result!!.request)
    }

    @Test
    fun generate_collapsesTheSetupSection_andItCanBeReopened() {
        val viewModel = newViewModel()
        assertTrue(viewModel.state.setupExpanded)
        viewModel.onInputChanged(InputField.Root, "3")
        viewModel.onGenerate()
        assertFalse(viewModel.state.setupExpanded)
        viewModel.onSetupExpandedChanged(true)
        assertTrue(viewModel.state.setupExpanded)
        viewModel.onGenerate() // regeneration collapses again
        assertFalse(viewModel.state.setupExpanded)
    }

    @Test
    fun blockedGenerate_doesNotCollapse() {
        val viewModel = newViewModel()
        viewModel.onGenerate()
        assertTrue(viewModel.state.setupExpanded)
    }

    @Test
    fun generate_systemAndCubicModes() {
        val viewModel = newViewModel()
        viewModel.onModeSelected(GeneratorMode.LinearSystem3)
        viewModel.onInputChanged(InputField.X, "1")
        viewModel.onInputChanged(InputField.Y, "-2")
        viewModel.onInputChanged(InputField.Z, "3")
        viewModel.onGenerate()
        assertEquals(GenerationRequest.LinearSystem3(1, -2, 3), viewModel.state.result!!.request)
        assertTrue(viewModel.state.result!!.equation is GeneratedEquation.LinearSystem3)

        viewModel.onModeSelected(GeneratorMode.Cubic)
        for ((f, v) in listOf(InputField.Root1 to "4", InputField.Root2 to "4", InputField.Root3 to "-1")) {
            viewModel.onInputChanged(f, v)
        }
        viewModel.onGenerate()
        assertEquals(GenerationRequest.Polynomial(4, 4, -1), viewModel.state.result!!.request)
    }

    // ---- focus and sign ----

    @Test
    fun signButton_actsOnTheFocusedField_only() {
        val viewModel = newViewModel()
        viewModel.onModeSelected(GeneratorMode.LinearTwoVar)
        viewModel.onInputChanged(InputField.X, "5")
        viewModel.onInputChanged(InputField.Y, "6")
        assertFalse(viewModel.state.canToggleSign)
        val before = viewModel.state
        viewModel.onToggleSignOfFocused()
        assertSame(before, viewModel.state)

        viewModel.onFieldFocused(InputField.Y, true)
        assertTrue(viewModel.state.canToggleSign)
        viewModel.onToggleSignOfFocused()
        assertEquals(listOf("5", "-6"), viewModel.state.fields.map { it.text })
    }

    @Test
    fun signButton_isDisabledWhenFocusedFieldIsNotInTheMode() {
        val viewModel = newViewModel()
        viewModel.onModeSelected(GeneratorMode.LinearSystem3)
        viewModel.onFieldFocused(InputField.Z, true)
        assertTrue(viewModel.state.canToggleSign)
        viewModel.onModeSelected(GeneratorMode.LinearTwoVar)
        assertFalse(viewModel.state.canToggleSign)
        viewModel.onToggleSignOfFocused()
        assertEquals(listOf("", ""), viewModel.state.fields.map { it.text })
    }

    @Test
    fun blur_clearsFocus_andDisablesTheSignButton() {
        val viewModel = newViewModel()
        viewModel.onFieldFocused(InputField.Root, true)
        assertTrue(viewModel.state.canToggleSign)
        viewModel.onFieldFocused(InputField.Root, false)
        assertNull(viewModel.state.focusedField)
        assertFalse(viewModel.state.canToggleSign)
    }

    @Test
    fun staleBlurFromAnotherField_doesNotClearTheNewFocus() {
        val viewModel = newViewModel()
        viewModel.onModeSelected(GeneratorMode.LinearTwoVar)
        viewModel.onFieldFocused(InputField.X, true)
        viewModel.onFieldFocused(InputField.Y, true)  // focus moves X -> Y
        viewModel.onFieldFocused(InputField.X, false) // X's blur arrives late
        assertEquals(InputField.Y, viewModel.state.focusedField)
        assertTrue(viewModel.state.canToggleSign)
    }

    @Test
    fun modeSwitch_clearsFocus() {
        val viewModel = newViewModel()
        viewModel.onModeSelected(GeneratorMode.LinearTwoVar)
        viewModel.onFieldFocused(InputField.X, true)
        viewModel.onModeSelected(GeneratorMode.LinearPair)
        assertFalse(viewModel.state.canToggleSign)
    }

    @Test
    fun pastingFourDigits_showsTheRangeError_andBlocksGenerate() {
        val viewModel = newViewModel()
        viewModel.onInputChanged(InputField.Root, "1000")
        assertEquals("1000", viewModel.state.fields.single().text)
        assertEquals(InputError.OutOfRange, viewModel.state.fields.single().error)
        assertFalse(viewModel.state.canGenerate)
    }

    // ---- Randomize ----

    @Test
    fun randomize_fillsEveryFieldOfTheMode_withoutGeneratingOrTouchingTheResult() {
        val viewModel = GeneratorViewModel(EquationEngine(), Random(1))
        viewModel.onInputChanged(InputField.Root, "3")
        viewModel.onGenerate()
        val result = viewModel.state.result
        viewModel.onModeSelected(GeneratorMode.Cubic)
        for (round in 0 until 200) {
            viewModel.onRandomize()
            val values = viewModel.state.fields.map { it.text.toInt() }
            assertEquals(3, values.size)
            assertTrue("$values", values.all { it != 0 && it in -10..10 })
            assertTrue(viewModel.state.canGenerate)
            assertSame(result, viewModel.state.result)
        }
    }

    @Test
    fun randomize_onlyFillsTheCurrentModesFields() {
        val viewModel = GeneratorViewModel(EquationEngine(), Random(2))
        viewModel.onModeSelected(GeneratorMode.LinearTwoVar)
        viewModel.onRandomize()
        assertNull(viewModel.state.inputs[InputField.Z])
        assertNull(viewModel.state.inputs[InputField.Root])
    }

    @Test
    fun randomize_isDeterministicForTheSameRandom() {
        fun run(seed: Int) = GeneratorViewModel(EquationEngine(), Random(seed)).also {
            it.onModeSelected(GeneratorMode.LinearSystem3)
            it.onRandomize()
        }.state.inputs
        assertEquals(run(9), run(9))
    }

    // ---- input events ----

    @Test
    fun inputEvents_updateTheStateFlow() {
        val viewModel = newViewModel()
        viewModel.onInputChanged(InputField.Root, "1.5")
        assertEquals("15", viewModel.state.fields.single().text)

        viewModel.onToggleSign(InputField.Root)
        assertEquals("-15", viewModel.state.fields.single().text)

        viewModel.onModeSelected(GeneratorMode.Quadratic)
        assertEquals(GeneratorMode.Quadratic, viewModel.state.mode)
        assertEquals(listOf(InputField.Root1, InputField.Root2), viewModel.state.fields.map { it.field })
    }

    @Test
    fun editingInputs_neverClearsTheResult() {
        val viewModel = newViewModel()
        viewModel.onInputChanged(InputField.Root, "3")
        viewModel.onGenerate()
        val result = viewModel.state.result

        viewModel.onInputChanged(InputField.Root, "7")
        viewModel.onToggleSign(InputField.Root)
        viewModel.onModeSelected(GeneratorMode.Quadratic)

        assertSame(result, viewModel.state.result)
    }

    // ---- remembered equation type (D-039) ----

    private class FakeStore(var saved: GeneratorMode? = null) : ModeStore {
        var saves = 0
        override fun load() = saved
        override fun save(mode: GeneratorMode) {
            saved = mode
            saves++
        }
    }

    @Test
    fun startsWithTheSavedMode_orLinearOneVarWhenNothingIsSaved() {
        assertEquals(GeneratorMode.LinearOneVar, GeneratorViewModel(EquationEngine(), modeStore = FakeStore()).state.mode)
        val saved = GeneratorViewModel(EquationEngine(), modeStore = FakeStore(GeneratorMode.Cubic))
        assertEquals(GeneratorMode.Cubic, saved.state.mode)
        assertEquals(listOf(InputField.Root1, InputField.Root2, InputField.Root3), saved.state.fields.map { it.field })
    }

    @Test
    fun selectingAMode_savesIt_andANewViewModelRestoresIt() {
        val store = FakeStore()
        val first = GeneratorViewModel(EquationEngine(), modeStore = store)
        first.onModeSelected(GeneratorMode.LinearSystem3)
        assertEquals(GeneratorMode.LinearSystem3, store.saved)

        val second = GeneratorViewModel(EquationEngine(), modeStore = store) // a fresh app launch
        assertEquals(GeneratorMode.LinearSystem3, second.state.mode)
    }

    @Test
    fun typedValuesAndResultsAreNotRestored() {
        val store = FakeStore()
        val first = GeneratorViewModel(EquationEngine(), modeStore = store)
        first.onModeSelected(GeneratorMode.Quadratic)
        first.onInputChanged(InputField.Root1, "4")
        first.onInputChanged(InputField.Root2, "5")
        first.onGenerate()

        val second = GeneratorViewModel(EquationEngine(), modeStore = store)
        assertEquals(GeneratorMode.Quadratic, second.state.mode)
        assertEquals(emptyMap<InputField, String>(), second.state.inputs)
        assertNull(second.state.result)
        assertTrue(second.state.setupExpanded)
    }

    @Test
    fun otherEvents_doNotWriteToTheStore() {
        val store = FakeStore()
        val viewModel = GeneratorViewModel(EquationEngine(), modeStore = store)
        viewModel.onInputChanged(InputField.Root, "3")
        viewModel.onRandomize()
        viewModel.onGenerate()
        viewModel.onSetupExpandedChanged(true)
        assertEquals(0, store.saves)
    }

    @Test
    fun modeFromName_knowsEveryModeAndIgnoresUnknownNames() {
        for (mode in GeneratorMode.entries) assertEquals(mode, modeFromName(mode.name))
        assertNull(modeFromName(null))
        assertNull(modeFromName(""))
        assertNull(modeFromName("Quartic"))
    }
}
