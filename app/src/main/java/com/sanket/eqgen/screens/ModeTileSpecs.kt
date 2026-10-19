package com.sanket.eqgen.screens

import androidx.annotation.StringRes
import com.sanket.eqgen.R

/** The text one mode tile shows, plus the summary line and the hint for the selected mode. */
data class ModeTileSpec(
    val mode: GeneratorMode,
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
    @StringRes val form: Int,
    @StringRes val summary: Int,
    @StringRes val hint: Int,
)

/** Tile order: two columns, three rows (spec section 05). */
val modeTileSpecs: List<ModeTileSpec> = listOf(
    ModeTileSpec(
        GeneratorMode.LinearOneVar, R.string.tile_linear_one_var_title, R.string.tile_linear_one_var_subtitle,
        R.string.tile_form_linear_one_var, R.string.summary_linear_one_var, R.string.mode_hint_linear_one_var,
    ),
    ModeTileSpec(
        GeneratorMode.LinearTwoVar, R.string.tile_linear_two_var_title, R.string.tile_linear_two_var_subtitle,
        R.string.tile_form_linear_two_var, R.string.summary_linear_two_var, R.string.mode_hint_linear_two_var,
    ),
    ModeTileSpec(
        GeneratorMode.LinearPair, R.string.tile_linear_pair_title, R.string.tile_linear_pair_subtitle,
        R.string.tile_form_linear_pair, R.string.summary_linear_pair, R.string.mode_hint_linear_pair,
    ),
    ModeTileSpec(
        GeneratorMode.LinearSystem3, R.string.tile_system3_title, R.string.tile_system3_subtitle,
        R.string.tile_form_system3, R.string.summary_system3, R.string.mode_hint_system3,
    ),
    ModeTileSpec(
        GeneratorMode.Quadratic, R.string.tile_quadratic_title, R.string.tile_quadratic_subtitle,
        R.string.tile_form_quadratic, R.string.summary_quadratic, R.string.mode_hint_quadratic,
    ),
    ModeTileSpec(
        GeneratorMode.Cubic, R.string.tile_cubic_title, R.string.tile_cubic_subtitle,
        R.string.tile_form_cubic, R.string.summary_cubic, R.string.mode_hint_cubic,
    ),
)

fun specFor(mode: GeneratorMode): ModeTileSpec = modeTileSpecs.first { it.mode == mode }
