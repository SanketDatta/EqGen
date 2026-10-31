package com.sanket.eqgen

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.platform.app.InstrumentationRegistry
import com.sanket.eqgen.domain.generator.EquationEngine
import com.sanket.eqgen.presentation.theme.EqGenTheme
import com.sanket.eqgen.screens.GeneratorScreen
import com.sanket.eqgen.screens.GeneratorViewModel
import com.sanket.eqgen.screens.RESULT_CARD_TAG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Layer 2 (spec section 06): the real screen and a real ViewModel with a seeded engine, driven through
 * the UI with `createComposeRule`. Strings are read from resources, so the tests survive copy edits.
 */
class GeneratorScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private fun str(@StringRes id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun launch(): GeneratorViewModel {
        var next = 100L
        val viewModel = GeneratorViewModel(EquationEngine(seedSource = { next++ }))
        rule.setContent { Harness(viewModel) }
        return viewModel
    }

    // ---- helpers ----

    private fun field(label: String) = rule.onNode(hasSetTextAction() and hasText(label))

    private fun type(label: String, text: String) = field(label).performTextReplacement(text)

    private val generate get() = rule.onNode(hasText(str(R.string.action_generate)) and hasClickAction())
    private val randomize get() = rule.onNode(hasContentDescription(str(R.string.action_randomize)) and hasClickAction())
    private val signButton get() = rule.onNode(hasContentDescription(str(R.string.action_change_sign)))
    private val header get() = rule.onNode(hasText(str(R.string.section_equation_type)) and hasClickAction())
    private val graph get() = rule.onNodeWithContentDescription(str(R.string.graph_description))

    private fun tile(@StringRes title: Int, @StringRes subtitle: Int) =
        rule.onNode(isSelectable() and hasText(str(title)) and hasText(str(subtitle)))

    private fun selectLinearOneVar() = tile(R.string.tile_linear_one_var_title, R.string.tile_linear_one_var_subtitle)
    private fun selectLinearTwoVar() = tile(R.string.tile_linear_two_var_title, R.string.tile_linear_two_var_subtitle)
    private fun selectSystem3() = tile(R.string.tile_system3_title, R.string.tile_system3_subtitle)
    private fun selectQuadratic() = tile(R.string.tile_quadratic_title, R.string.tile_quadratic_subtitle)
    private fun selectCubic() = tile(R.string.tile_cubic_title, R.string.tile_cubic_subtitle)

    /** All text inside the result card, equation lines and solution, as one string. */
    private fun cardText(): String = rule
        .onAllNodes(
            hasAnyAncestor(hasTestTag(RESULT_CARD_TAG)) and
                SemanticsMatcher.keyIsDefined(SemanticsProperties.Text),
            useUnmergedTree = true,
        )
        .fetchSemanticsNodes()
        .flatMap { it.config[SemanticsProperties.Text] }
        .joinToString("|") { it.text }

    private fun fieldCount() = rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size

    // ---- tests ----

    @Test
    fun invalidInput_showsAnError_andKeepsGenerateDisabled() {
        launch()
        generate.assertIsNotEnabled()

        type(str(R.string.field_root), "500") // above 100
        rule.onNodeWithText(str(R.string.error_out_of_range, 100)).assertIsDisplayed()
        generate.assertIsNotEnabled()

        type(str(R.string.field_root), "7")
        rule.onNodeWithText(str(R.string.error_out_of_range, 100)).assertDoesNotExist()
        generate.assertIsEnabled()
    }

    @Test
    fun generate_showsCardAndGraph_collapsesTheSection_andHeaderReopensIt() {
        launch()
        type(str(R.string.field_root), "4")
        generate.performClick()

        rule.onNodeWithText(str(R.string.label_solution)).assertIsDisplayed()
        assertTrue(cardText().contains("4"))
        graph.assertIsDisplayed()
        selectLinearOneVar().assertDoesNotExist() // section collapsed

        header.performClick()
        selectLinearOneVar().assertIsDisplayed()
    }

    @Test
    fun system3_showsNoGraph() {
        launch()
        selectSystem3().performClick()
        type("x", "1")
        type("y", "-2")
        type("z", "3")
        generate.performClick()

        rule.onNodeWithText(str(R.string.label_solution)).assertIsDisplayed()
        graph.assertDoesNotExist()
    }

    @Test
    fun generateAgainWithUnchangedInput_changesTheEquation_butNotTheInputs() {
        launch()
        selectQuadratic().performClick()
        type(str(R.string.field_root1), "2")
        type(str(R.string.field_root2), "3")
        generate.performClick()
        val before = cardText()

        repeat(5) {
            val previous = cardText()
            generate.performClick()
            rule.waitForIdle()
            assertNotEquals(previous, cardText())
        }
        assertNotEquals("", before)
        // The inputs were not touched.
        field(str(R.string.field_root1)).assertTextContains("2")
        field(str(R.string.field_root2)).assertTextContains("3")
    }

    @Test
    fun editingInputsAfterAResult_keepsTheCardAndGraph_untilGenerate() {
        launch()
        type(str(R.string.field_root), "4")
        generate.performClick()
        val before = cardText()

        type(str(R.string.field_root), "9")
        assertEquals(before, cardText())
        graph.assertIsDisplayed()

        generate.performClick()
        assertNotEquals(before, cardText())
    }

    @Test
    fun tileSelection_showsTheRightNumberOfFields_andTappingTheSelectedTileChangesNothing() {
        launch()
        assertEquals(1, fieldCount())
        selectLinearOneVar().assertIsSelected()

        selectLinearTwoVar().performClick()
        assertEquals(2, fieldCount())
        selectLinearTwoVar().assertIsSelected()
        selectLinearOneVar().assertIsNotSelected()

        selectSystem3().performClick()
        assertEquals(3, fieldCount())

        selectCubic().performClick()
        assertEquals(3, fieldCount())

        selectQuadratic().performClick()
        assertEquals(2, fieldCount())

        type(str(R.string.field_root1), "5")
        selectQuadratic().performClick() // already selected: nothing changes
        selectQuadratic().assertIsSelected()
        assertEquals(2, fieldCount())
        field(str(R.string.field_root1)).assertTextContains("5")
    }

    @Test
    fun signButton_isDisabledWithoutFocus_andFlipsTheFocusedField() {
        launch()
        signButton.assertIsNotEnabled()

        selectLinearTwoVar().performClick()
        type("x", "5")
        type("y", "6")
        field("y").performClick() // focus y
        signButton.assertIsEnabled()
        signButton.performClick()
        field("y").assertTextContains("-6")
        field("x").assertTextContains("5")

        // Generate clears focus, so the button disables again.
        generate.performClick()
        signButton.assertIsNotEnabled()
    }

    @Test
    fun randomize_fillsTheFields_withoutProducingAResult() {
        val viewModel = launch()
        selectCubic().performClick()
        randomize.performClick()

        val state = viewModel.uiState.value
        assertEquals(3, state.fields.size)
        assertTrue(state.fields.all { it.text.isNotEmpty() && it.error == null })
        assertEquals(null, state.result)
        rule.onNodeWithText(str(R.string.empty_hint)).assertIsDisplayed()
        generate.assertIsEnabled()
    }
}

/** Mirrors `MainActivity`'s wiring so the test can hold the ViewModel. */
@Composable
private fun Harness(viewModel: GeneratorViewModel) {
    EqGenTheme {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        GeneratorScreen(
            state = state,
            onModeSelected = viewModel::onModeSelected,
            onSetupExpandedChange = viewModel::onSetupExpandedChanged,
            onInputChanged = viewModel::onInputChanged,
            onFieldFocused = viewModel::onFieldFocused,
            onToggleSign = viewModel::onToggleSignOfFocused,
            onRandomize = viewModel::onRandomize,
            onGenerate = viewModel::onGenerate,
        )
    }
}

/** Rotation needs the real Activity, whose ViewModel survives recreation. */
class GeneratorRotationTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun recreatingTheActivity_preservesInputsResultAndCollapsedSection() {
        val label = rule.activity.getString(R.string.field_root)
        val generateLabel = rule.activity.getString(R.string.action_generate)
        rule.onNode(hasSetTextAction() and hasText(label)).performTextReplacement("8")
        rule.onNode(hasText(generateLabel) and hasClickAction()).performClick()
        rule.onNodeWithText(rule.activity.getString(R.string.label_solution)).assertIsDisplayed()

        rule.activityRule.scenario.recreate()

        rule.onNodeWithText(rule.activity.getString(R.string.label_solution)).assertIsDisplayed()
        rule.onNode(hasSetTextAction() and hasText(label)).assertTextContains("8")
        // The section stayed collapsed across the recreation.
        rule.onNode(
            isSelectable() and hasText(rule.activity.getString(R.string.tile_quadratic_title)),
        ).assertDoesNotExist()
    }
}
