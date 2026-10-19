package com.sanket.eqgen.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sanket.eqgen.R
import com.sanket.eqgen.domain.format.EquationText

/**
 * Shows the generated equation(s) and their solution. Stateless; the text comes from `EquationFormatter`.
 *
 * Each equation is laid out in three columns (left side right-aligned, "=", right side left-aligned) so
 * the "=" signs line up across lines. Lines never wrap; the equation block scrolls sideways if it is too
 * wide. Numerals are tabular. Long-press selects the text so it can be copied. The card resizes smoothly
 * between one and three equations.
 */
@Composable
fun EquationCard(text: EquationText, modifier: Modifier = Modifier) {
    val equationsLabel = stringResource(
        if (text.equations.size == 1) R.string.label_equation else R.string.label_equations,
    )
    val equationStyle = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum")

    Card(modifier = modifier.fillMaxWidth().animateContentSize()) {
        SelectionContainer {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = equationsLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                        text.equations.forEach { line ->
                            Text(text = line.left, style = equationStyle, softWrap = false, textAlign = TextAlign.End)
                        }
                    }
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        text.equations.forEach { _ ->
                            Text(text = " = ", style = equationStyle, softWrap = false)
                        }
                    }
                    Column {
                        text.equations.forEach { line ->
                            Text(text = line.right, style = equationStyle, softWrap = false)
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = stringResource(R.string.label_solution),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = text.solutions.joinToString(separator = ",  "),
                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                )
            }
        }
    }
}
