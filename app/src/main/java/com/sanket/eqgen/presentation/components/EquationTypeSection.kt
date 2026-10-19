package com.sanket.eqgen.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.sanket.eqgen.R
import com.sanket.eqgen.screens.GeneratorMode
import com.sanket.eqgen.screens.ModeTileSpec
import com.sanket.eqgen.screens.modeTileSpecs
import com.sanket.eqgen.screens.specFor

/**
 * The collapsible "Equation type" card: a header (tap to expand or collapse), a six-tile grid of modes in
 * two columns, and a summary line that stays visible when collapsed. Stateless.
 */
@Composable
fun EquationTypeSection(
    selected: GeneratorMode,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onModeSelected: (GeneratorMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val actionLabel = stringResource(if (expanded) R.string.section_collapse else R.string.section_expand)
    val stateLabel = stringResource(
        if (expanded) R.string.section_state_expanded else R.string.section_state_collapsed,
    )

    Card(modifier = modifier.fillMaxWidth().animateContentSize()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClickLabel = actionLabel, role = Role.Button) { onExpandedChange(!expanded) }
                    .semantics { stateDescription = stateLabel },
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.section_equation_type),
                    style = MaterialTheme.typography.titleMedium,
                )
                Icon(
                    painter = painterResource(R.drawable.ic_expand_more),
                    contentDescription = null,
                    modifier = Modifier.rotate(if (expanded) 180f else 0f),
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    modeTileSpecs.chunked(2).forEach { rowSpecs ->
                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowSpecs.forEach { spec ->
                                ModeTile(
                                    spec = spec,
                                    selected = spec.mode == selected,
                                    onClick = { if (spec.mode != selected) onModeSelected(spec.mode) },
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = stringResource(specFor(selected).summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ModeTile(
    spec: ModeTileSpec,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(spec.title), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(spec.subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(stringResource(spec.form), style = MaterialTheme.typography.labelMedium)
        }
    }
}
