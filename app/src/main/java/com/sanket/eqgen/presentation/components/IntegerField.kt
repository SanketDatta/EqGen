package com.sanket.eqgen.presentation.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

/**
 * A single-line whole-number field. Stateless: the caller owns [value] and cleans it in [onValueChange].
 * Shows [errorText] under the field when it is not null. [onFocusChanged] reports every focus gain and loss, so the screen knows which field the ± button acts on.
 */
@Composable
fun IntegerField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.onFocusChanged { onFocusChanged(it.isFocused) },
        label = { Text(label) },
        isError = errorText != null,
        supportingText = if (errorText != null) {
            { Text(errorText) }
        } else {
            null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
        keyboardActions = keyboardActions,
    )
}
