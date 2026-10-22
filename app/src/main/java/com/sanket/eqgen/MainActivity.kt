package com.sanket.eqgen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sanket.eqgen.presentation.theme.EqGenTheme
import com.sanket.eqgen.screens.GeneratorScreen
import com.sanket.eqgen.screens.GeneratorViewModel

/**
 * Wires the ViewModel to the screen. All state lives in [GeneratorViewModel], so it survives rotation.
 *
 * NOTE: `EqGenTheme` is the usual name Android Studio generates from the project name. If your theme
 * function (in presentation/theme/Theme.kt) is named differently, change the import and the call below.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: GeneratorViewModel by viewModels()

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
