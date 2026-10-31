package com.sanket.eqgen.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sanket.eqgen.R
import com.sanket.eqgen.domain.format.EquationFormatter
import com.sanket.eqgen.domain.generator.EquationEngine
import com.sanket.eqgen.domain.graph.GraphSampler
import com.sanket.eqgen.domain.model.GenerationRequest
import com.sanket.eqgen.domain.model.GenerationResult
import com.sanket.eqgen.domain.model.MAX_INPUT_ABS
import com.sanket.eqgen.presentation.components.EquationCard
import com.sanket.eqgen.presentation.components.IntegerField
import com.sanket.eqgen.presentation.components.EquationTypeSection
import com.sanket.eqgen.presentation.graph.GraphView

/** Test tag of the result card, so UI tests can read the displayed equation. */
const val RESULT_CARD_TAG = "equationCard"

/**
 * The single v1 screen (spec section 05). Stateless: everything it shows comes from [state], and every
 * user action goes out through a callback, so it can be previewed and UI-tested without a ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(
    state: GeneratorUiState,
    onModeSelected: (GeneratorMode) -> Unit,
    onSetupExpandedChange: (Boolean) -> Unit,
    onInputChanged: (InputField, String) -> Unit,
    onFieldFocused: (InputField, Boolean) -> Unit,
    onToggleSign: () -> Unit,
    onRandomize: () -> Unit,
    onGenerate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val appDescription = stringResource(R.string.eqgen_title_description)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.eqgen_title),
                        modifier = Modifier.semantics { contentDescription = appDescription },
                    )
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            EquationTypeSection(
                tiles = modeTiles,
                selectedIndex = modeTileSpecs.indexOfFirst { it.mode == state.mode },
                summary = stringResource(specFor(state.mode).summary),
                expanded = state.setupExpanded,
                onExpandedChange = onSetupExpandedChange,
                onTileSelected = { index -> onModeSelected(modeTileSpecs[index].mode) },
            )

            InputCard(
                hint = stringResource(specFor(state.mode).hint),
                content = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        state.fields.forEachIndexed { index, fieldState ->
                            val isLast = index == state.fields.lastIndex
                            key(fieldState.field) {
                                IntegerField(
                                    value = fieldState.text,
                                    label = stringResource(fieldState.field.labelRes()),
                                    onValueChange = { text -> onInputChanged(fieldState.field, text) },
                                    onFocusChanged = { focused -> onFieldFocused(fieldState.field, focused) },
                                    modifier = Modifier.weight(1f),
                                    errorText = fieldState.error?.let { error -> errorMessage(error) },
                                    imeAction = if (isLast) ImeAction.Done else ImeAction.Next,
                                    keyboardActions = if (isLast) {
                                        KeyboardActions(
                                            onDone = {
                                                if (state.canGenerate) onGenerate()
                                                focusManager.clearFocus()
                                            },
                                        )
                                    } else {
                                        KeyboardActions.Default
                                    },
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Left Pill-shaped Icon Button
                        FilledTonalIconButton(
                            onClick = onToggleSign,
                            enabled = state.canToggleSign,
                            modifier = Modifier
                                .width(56.dp)
                                .height(ButtonDefaults.MinHeight),
                            shape = IconButtonDefaults.filledShape,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_plus_minus),
                                contentDescription = stringResource(R.string.action_change_sign),
                            )
                        }

                        // Middle Text Button
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                onGenerate()
                            },
                            enabled = state.canGenerate,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.action_generate))
                        }

                        // Right Pill-shaped Icon Button
                        FilledTonalIconButton(
                            onClick = onRandomize,
                            modifier = Modifier
                                .width(56.dp)
                                .height(ButtonDefaults.MinHeight),
                            shape = IconButtonDefaults.filledShape,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_casino),
                                contentDescription = stringResource(R.string.action_randomize),
                            )
                        }
                    }
                },
            )

            ResultSection(result = state.result)
        }
    }
}

/**
 * The input section: hint, fields and the tool row in one rounded card. It uses `primaryContainer`, so it
 * reads as a different area from the "Equation type" card and the result card (spec section 05).
 */
@Composable
private fun InputCard(hint: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = hint, style = MaterialTheme.typography.bodyMedium)
            content()
        }
    }
}

@Composable
private fun ColumnScope.ResultSection(result: GenerationResult?) {
    if (result == null) {
        Text(
            text = stringResource(R.string.empty_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    val equation = result.equation
    val text = remember(equation) { EquationFormatter.format(equation) }
    val graph = remember(equation) {
        if (GraphSampler.hasGraph(equation)) {
            val viewport = GraphSampler.fitViewport(equation)
            viewport to GraphSampler.sample(equation, viewport)
        } else {
            null
        }
    }

    EquationCard(text = text, modifier = Modifier.testTag(RESULT_CARD_TAG))
    if (graph != null) GraphView(
        plot = graph.second,
        viewport = graph.first,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 480.dp)
            .aspectRatio(1f)
            .align(Alignment.CenterHorizontally),
        contentDescription = stringResource(R.string.graph_description),
    )
}

@Composable
private fun errorMessage(error: InputError): String = when (error) {
    InputError.OutOfRange -> stringResource(R.string.error_out_of_range, MAX_INPUT_ABS)
    InputError.NotAWholeNumber -> stringResource(R.string.error_not_a_number)
}

@StringRes
private fun InputField.labelRes(): Int = when (this) {
    InputField.Root -> R.string.field_root
    InputField.X -> R.string.field_x
    InputField.Y -> R.string.field_y
    InputField.Z -> R.string.field_z
    InputField.Root1 -> R.string.field_root1
    InputField.Root2 -> R.string.field_root2
    InputField.Root3 -> R.string.field_root3
}

// ---- Previews (sample data only; delete if you don't use previews) ----

@Composable
private fun PreviewScreen(state: GeneratorUiState) {
    MaterialTheme {
        GeneratorScreen(
            state = state,
            onModeSelected = {},
            onSetupExpandedChange = {},
            onInputChanged = { _, _ -> },
            onFieldFocused = { _, _ -> },
            onToggleSign = {},
            onRandomize = {},
            onGenerate = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GeneratorScreenEmptyPreview() = PreviewScreen(GeneratorUiState())

@Preview(showBackground = true)
@Composable
private fun GeneratorScreenCubicPreview() {
    val result = EquationEngine().generate(GenerationRequest.Polynomial(-2, 1, 5), seed = 1L)
    PreviewScreen(
        GeneratorUiState(
            mode = GeneratorMode.Cubic,
            inputs = mapOf(InputField.Root1 to "-2", InputField.Root2 to "1", InputField.Root3 to "5"),
            result = result,
            setupExpanded = false,
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun GeneratorScreenSystemPreview() {
    val result = EquationEngine().generate(GenerationRequest.LinearSystem3(1, -2, 3), seed = 1L)
    PreviewScreen(
        GeneratorUiState(
            mode = GeneratorMode.LinearSystem3,
            inputs = mapOf(InputField.X to "1", InputField.Y to "-2", InputField.Z to "3"),
            result = result,
        ),
    )
}
