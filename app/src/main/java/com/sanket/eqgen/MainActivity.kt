package com.sanket.eqgen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sanket.eqgen.data.SharedPrefsModeStore
import com.sanket.eqgen.domain.generator.EquationEngine
import com.sanket.eqgen.presentation.theme.EqGenTheme
import com.sanket.eqgen.screens.GeneratorScreen
import com.sanket.eqgen.screens.GeneratorViewModel

/**
 * Wires the ViewModel to the screen. All state lives in [GeneratorViewModel], so it survives rotation.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: GeneratorViewModel by viewModels {
        viewModelFactory {
            initializer {
                GeneratorViewModel(EquationEngine(), modeStore = SharedPrefsModeStore(applicationContext))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EqGenTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                GeneratorScreen(
                    state = state,
                    onModeSelected = viewModel::onModeSelected,
                    onInputChanged = viewModel::onInputChanged,
                    onSetupExpandedChange = viewModel::onSetupExpandedChanged,
                    onFieldFocused = viewModel::onFieldFocused,
                    onToggleSign = viewModel::onToggleSignOfFocused,
                    onRandomize = viewModel::onRandomize,
                    onGenerate = viewModel::onGenerate,
                )
            }
        }
    }
}
