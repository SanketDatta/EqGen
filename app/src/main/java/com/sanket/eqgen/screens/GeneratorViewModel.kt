package com.sanket.eqgen.screens

import androidx.lifecycle.ViewModel
import com.sanket.eqgen.domain.generator.EquationEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/**
 * Holds the screen state and runs Generate (which also regenerates) (spec section 04).
 *
 * Generation is instant pure maths, so every function here is synchronous and uses no coroutines
 * scope. Call them from the main thread, which is where Compose callbacks run. In tests, build it
 * with a fake [EquationEngine] and read [uiState]`.value` directly.
 */
class GeneratorViewModel(
    private val engine: EquationEngine,
    private val random: Random = Random.Default,
) : ViewModel() {

    /** Used by the default ViewModel factory. */
    constructor() : this(EquationEngine())

    private val _uiState = MutableStateFlow(GeneratorUiState())
    val uiState: StateFlow<GeneratorUiState> = _uiState.asStateFlow()

    fun onModeSelected(mode: GeneratorMode) {
        _uiState.value = _uiState.value.withMode(mode)
    }

    fun onInputChanged(field: InputField, text: String) {
        _uiState.value = _uiState.value.withInput(field, text)
    }

    fun onToggleSign(field: InputField) {
        _uiState.value = _uiState.value.withToggledSign(field)
    }

    fun onFieldFocused(field: InputField) {
        _uiState.value = _uiState.value.withFocus(field)
    }

    fun onToggleSignOfFocused() {
        _uiState.value = _uiState.value.withToggledFocusedSign()
    }

    /** Fills the fields with random values. Does not generate and does not touch the result. */
    fun onRandomize() {
        _uiState.value = _uiState.value.withRandomInputs(random)
    }

    fun onSetupExpandedChanged(expanded: Boolean) {
        _uiState.value = _uiState.value.withSetupExpanded(expanded)
    }

    /**
     * Generates from the current inputs and collapses the "Equation type" section. When the inputs
     * equal the displayed result's request, regenerates through [EquationEngine.retry] so the
     * equation differs. Does nothing while any input is missing or invalid.
     */
    fun onGenerate() {
        val state = _uiState.value
        val request = state.request ?: return
        val previous = state.result
        val next = if (previous != null && previous.request == request) engine.retry(previous) else engine.generate(request)
        _uiState.value = state.copy(result = next, setupExpanded = false)
    }
}
