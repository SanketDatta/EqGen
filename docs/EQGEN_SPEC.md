# EQGEN_SPEC.md — Canonical Specification

Single source of truth for ΣqGen (project and package name: EqGen). If code and this document disagree, one of them is a bug; fix it and log the change in section 07.

**Status tags:** `[C]` confirmed · `[P]` proposed, awaiting approval · `[O]` open question · `[D]` deferred. Baseline: v1, 2026-10-16; scope revised 2026-10-19 (six modes, tile selector, Retry removed). Items still tagged `[P]` or `[O]` are listed in section 07. New ideas enter as `[P]`, `[O]` or `[D]`.

---

## 01 — Overview and design principles

ΣqGen is an offline Android app that generates algebraic equations from a solution the user supplies, and shows the result with an auto-fitted 2D graph where one exists.

- Platform: Android, Kotlin, Jetpack Compose. minSdk 24, targetSdk 37. `[C]`
- Offline-first: no network, accounts or permissions. `[C]`
- Math is separate from text: the domain never stores formatted strings. `[C]`
- The domain is pure Kotlin (no Android, no Compose) and fully testable on the JVM. `[C]`
- Scope discipline: nothing outside section 02 "v1" is built until this document says so. `[C]`
- Tests for the math engine are written before the Compose UI. `[C]`
- Official components first: use an official Jetpack Compose or Material 3 component whenever one does the job, and never write a custom composable that duplicates one. Custom drawing is allowed only where Compose has no equivalent (the graph). `[C]`
- Brand: the in-app name is "ΣqGen", shown in the top app bar title and read aloud as "EqGen". The launcher/home-screen app name (`app_name`) is unchanged. `[C]`

## 02 — Product scope

### v1 modes `[C]`

| # | Mode | User supplies | App generates |
|---|------|---------------|---------------|
| M1 | Single linear, one variable | root `r` | `ax + b = 0` |
| M2 | Single linear, two variables | integer solution `(x0, y0)` | `ax + by = c` |
| M3 | Pair of linear, two variables | solution `(x0, y0)` | two equations, unique solution |
| M4 | Single quadratic, one variable | roots `r1`, `r2` | `ax² + bx + c = 0` |
| M5 | System of three linear equations, three variables | solution `(x0, y0, z0)` | three equations `ax + by + cz = d`, unique solution |
| M6 | Single cubic, one variable | roots `r1`, `r2`, `r3` | `ax³ + bx² + cx + d = 0` |

Also in v1: integer roots only; regeneration by tapping Generate again (section 03); a Randomize button that fills the inputs; a simple static auto-fitted graph where one exists (pan/zoom deferred; M1 plotted as `y = ax + b`, M2 and M3 as lines, M4 and M6 as curves; M5 has no graph).

Repeated roots are never restricted: if the user enters equal roots in M4 or M6, the app generates exactly those roots. `[C]`

### Non-goals for v1
No accounts, no history or saved equations, no export, no custom variable names, no fractions/decimals/radicals/complex numbers, no step-by-step solutions, no navigation between screens, no separate Retry button, no graph for M5.

### Future features (all `[D]`, no commitments)
Biquadratic and higher degrees · 4 variables and larger systems · fraction/decimal/radical/complex roots and automatic conjugate pairing · alternate formats (x/a + y/b) · LaTeX/MathML/MathType/JPG export and Copy/Share actions · math input bar · multiplicity control · multiplicity text in the result ("double root", "triple root") · live input echo (`(x − 2)(x − 3) = 0`) · custom variables (t, h, k, θ) · coefficient selection per variable and coefficient styles (monic, positive-only) · step-by-step solving (factoring, Vieta) · graph pan/zoom · user-configurable coefficient range · starter presets · "generate N variants" and shareable problem numbers (seeds) · history and favorites · tablet two-pane layout · math font.

## 03 — Mathematical specification

### Conventions
- All inputs, coefficients and roots are `Int` in v1. `[C]`
- Generation uses an injected `kotlin.random.Random` built from a `Long` seed; no global randomness. `[C]`
- Coefficient range: the generated coefficients are drawn from the integers in `[-10, 20]`, excluding 0. They are `a` in M1, M4 and M6; `a` and `b` in M2 and M3; and all nine coefficients in M5. The range is intentionally asymmetric and applies only to those coefficients. `[C]`
- Derived values (`b` in M1; `b`, `c` in M4; `b`, `c`, `d` in M6; `c` in M2 and M3; `d` in M5) are never range-limited. `[C]`
- The range is held in a `GenerationConfig` (default `-10..20`) passed to generators, so it can become user-configurable later without touching generator logic. `[C]`
- `GenerationConfig` rejects ranges outside `[-1000, 1000]` (`MAX_COEFFICIENT_ABS`), so every derived value stays exact even if the range becomes configurable. Worst cases: M6 `d = 1000·100·100·100 = 1,000,000,000` (fits `Int`, with little headroom; coefficients are computed in `Long` and narrowed with an overflow check); M5 determinant up to `6·1000³ = 6,000,000,000` (computed and stored as `Long`). `[C]`
- `MAX_POLYNOMIAL_DEGREE = 3`. The overflow analysis above holds only up to degree 3; raising the limit means redoing it. `[C]`
- User-entered roots and solutions are not subject to the coefficient range; they are bounded only by `MAX_INPUT_ABS = 100` (see Validation). `[C]`

### M1 — Linear, one variable
`ax + b = 0` is the only display form. `[C]` `a` comes from the coefficient range; `b = −a·r` is derived. The root is `r`.
Invariant: `a·r + b == 0`.

### M2 — Linear, two variables
`ax + by = c`, where `a` and `b` come from the coefficient range (so both are non-zero and no axis-parallel lines are generated) and `c = a·x0 + b·y0` is derived. `[C]`
Invariant: `a·x0 + b·y0 == c`.
Note: `c` may be 0 and may fall outside the coefficient range, by design.

### M3 — Pair of linear, two variables
`a1x + b1y = c1` and `a2x + b2y = c2`, each built as in M2, with determinant `a1·b2 − a2·b1 ≠ 0`.
Invariants: `(x0, y0)` satisfies both equations; the determinant is non-zero (so lines are never parallel or identical and the solution is unique). v1 never generates no-solution or infinite-solution pairs. `[C]` Generation needs at least two distinct allowed coefficients (a one-value config is rejected for M3 only); `b2` is drawn from values that keep the determinant non-zero, so no retry loop is needed.

### M4 and M6 — Polynomial, one variable
`a·(x − r1)(x − r2)(…)` expanded, where the number of roots is the degree (2 for M4, 3 for M6) and the roots are exactly what the user entered, in any order, repeats included. `[C]` `a ≠ 0` comes from the coefficient range; the other coefficients are derived, so they are not range-limited.
- M4: `b = −a(r1 + r2)`, `c = a·r1·r2`.
- M6: `b = −a(r1 + r2 + r3)`, `c = a(r1r2 + r1r3 + r2r3)`, `d = −a·r1·r2·r3`.
- In general the coefficient of `x^(n−k)` is `a·(−1)^k·e_k`, where `e_k` is the k-th elementary symmetric polynomial of the roots (Vieta).

Invariants: `p(r) == 0` for every root; the coefficients match Vieta; for a root entered `m ≥ 2` times, `p'(r) == 0` (it is a genuine repeated root); the leading coefficient is `a`.

### M5 — System of three linear equations, three variables
`a_i·x + b_i·y + c_i·z = d_i` for `i = 1..3`. All nine `a_i, b_i, c_i` come from the coefficient range (so none is zero) and `d_i = a_i·x0 + b_i·y0 + c_i·z0` is derived. `[C]`
The 3×3 determinant of the coefficients must be non-zero, so the solution is unique. Generation draws all nine coefficients and redraws the whole matrix while the determinant is 0. This is seeded and deterministic, and bounded at 1,000 attempts (exhaustion throws `IllegalStateException`). It needs at least two distinct allowed coefficients. `[C]`
Invariants: `(x0, y0, z0)` satisfies all three equations; the determinant (`Long`) is non-zero; Cramer's rule recovers `(x0, y0, z0)` exactly.

### Regeneration (replaces the Retry button)
"Retry with the same input" is an explicit requirement `[C]`; it is met by tapping Generate again with unchanged input. Semantics `[C]`:
- Generate always uses a new seed. Every result records its seed so a bug report can reproduce it.
- If the parsed inputs equal the displayed result's request ("007" and "7" are equal), Generate regenerates through `EquationEngine.retry`: up to 20 new seeds are tried until the equation differs from the displayed one (best effort, so a config with a single possible outcome returns it). Otherwise it generates normally.
- Without this rule, M1, M4 and M6 would repeat the same equation about 1 time in 30 (30 allowed `a` values).
- Consequence: regenerating the displayed request after editing the fields without generating means restoring the old values by hand. Accepted (D-022).

### Validation
- Inputs must parse as `Int` with absolute value at most `MAX_INPUT_ABS` (100). This keeps every derived value inside its type and keeps the graph readable. `[C]`
- Invalid input never reaches the generator; the UI blocks Generate and shows an inline message.
- Generators throw `IllegalArgumentException` for requests outside the limit or with an unsupported degree (programmer error, not a UI path).

## 04 — Architecture and data models

### Package layout `[C]`
```
com.sanket.eqgen/
├── MainActivity.kt
├── domain/                      pure Kotlin
│   ├── model/
│   │   ├── GenerationConfig.kt
│   │   ├── GenerationRequest.kt
│   │   └── GeneratedEquation.kt        (equation types, GeneratedEquation, GenerationResult)
│   ├── generator/
│   │   ├── PolynomialGenerator.kt      (M1, M4, M6)
│   │   ├── LinearSystemGenerator.kt    (M2, M3, M5)
│   │   └── EquationEngine.kt           (dispatch by request type, regeneration rule)
│   ├── graph/
│   │   ├── GraphModels.kt              (Viewport, Point, PlotData)
│   │   ├── GraphSampler.kt             (equation → plot data, viewport fitting)
│   │   ├── GraphGrid.kt                (nice tick steps, ticks, labels)
│   │   └── GraphTransform.kt           (graph → pixel mapping)
│   └── format/
│       └── EquationFormatter.kt        (equation → display text)
├── screens/                     state and coordination
│   ├── GeneratorScreen.kt
│   ├── GeneratorViewModel.kt
│   ├── GeneratorUiState.kt
│   ├── ModeTileSpecs.kt                (mode → string resources for each tile)
│   └── IntInput.kt                     (sanitize, validate, toggle sign, random value)
└── presentation/                stateless Compose
    ├── theme/
    ├── components/              EquationTypeSection, EquationCard, IntegerField
    └── graph/                   GraphView (Canvas)
```
`EquationCard` is a stateless composable, so it lives in `presentation/components`. The tile list is data (`ModeTileSpecs.kt`): adding a mode is one entry there plus one `GeneratorMode` entry.

### Dependency direction
`MainActivity → screens → presentation → domain`. `domain` depends on nothing. `presentation` may read domain models but never the ViewModel or `screens` types; it receives plain strings and callbacks.

### Models (sketch) `[C]`
```kotlin
const val MAX_POLYNOMIAL_DEGREE = 3

sealed interface GenerationRequest {
    data class LinearOneVar(val root: Int) : GenerationRequest
    data class LinearTwoVar(val x: Int, val y: Int) : GenerationRequest
    data class LinearPair(val x: Int, val y: Int) : GenerationRequest
    data class LinearSystem3(val x: Int, val y: Int, val z: Int) : GenerationRequest
    data class Polynomial(val roots: List<Int>) : GenerationRequest   // 2..MAX_POLYNOMIAL_DEGREE roots
}

data class LinearEquation1D(val a: Int, val b: Int)                       // ax + b = 0
data class LinearEquation2D(val a: Int, val b: Int, val c: Int)           // ax + by = c
data class LinearEquation3D(val a: Int, val b: Int, val c: Int, val d: Int) // ax + by + cz = d
data class PolynomialEquation(val roots: List<Int>, val leadingCoefficient: Int) {
    val degree: Int                    // roots.size
    val coefficients: List<Int>        // highest degree first, size degree + 1, derived from the roots
}

sealed interface GeneratedEquation {
    data class LinearOneVar(val equation: LinearEquation1D, val root: Int) : GeneratedEquation
    data class LinearTwoVar(val equation: LinearEquation2D, val x: Int, val y: Int) : GeneratedEquation
    data class LinearPair(val first: LinearEquation2D, val second: LinearEquation2D, val x: Int, val y: Int) : GeneratedEquation
    data class LinearSystem3(val equations: List<LinearEquation3D>, val x: Int, val y: Int, val z: Int) : GeneratedEquation
    data class Polynomial(val equation: PolynomialEquation) : GeneratedEquation
}

data class GenerationResult(val request: GenerationRequest, val equation: GeneratedEquation, val seed: Long)
```
```kotlin
data class GenerationConfig(val coefficientRange: IntRange = -10..20) // 0 is always excluded
```

Graph types are the only place `Double` is allowed; the mathematical truth stays `Int`:
```kotlin
data class Viewport(val xMin: Double, val xMax: Double, val yMin: Double, val yMax: Double)
data class Point(val x: Double, val y: Double)
data class PlotData(val polylines: List<List<Point>>, val markers: List<Point>)
```

Formatter output separates the two sides of each equation so the UI can align the "=" signs:
```kotlin
data class EquationLine(val left: String, val right: String)   // plain text = "$left = $right"
data class EquationText(val equations: List<EquationLine>, val solutions: List<String>)
```

Every `Int` field is a v1 choice. Moving to `Rational` or complex roots later changes these types; the sealed hierarchy keeps that change contained to `domain` (see D-007).

### Responsibilities
- **Generators:** `(request, Random) → GeneratedEquation`. Pure, deterministic for a given seed.
- **EquationEngine:** `generate(request, seed)` picks the generator; `retry(previous)` regenerates from `previous.request` with fresh seeds from an injectable `seedSource`, trying up to 20 seeds until the equation differs (best effort). The UI no longer has a Retry button, but the ViewModel calls `retry` for the regeneration rule in section 03.
- **GraphSampler:** pure functions `hasGraph(equation)`, `fitViewport(equation): Viewport` and `sample(equation, viewport): PlotData`. `hasGraph` is false for M5, and the other two reject such equations. Sampling always takes the viewport as a parameter, so pan/zoom later only means supplying a different `Viewport`; equation logic is never touched. `[C]` Behavior: lines are clipped to the viewport as two-point polylines (a line that misses it, or only touches a corner, yields none); polynomial curves are sampled at 200 evenly spaced x positions plus their roots and turning points (the vertex of a parabola; the local extrema of a cubic), unclipped in y; markers (root, given solution, intersection, or distinct roots) are always returned, even outside the viewport. `fitViewport` always contains the origin and the key points and is at least 10 wide and 10 tall. A parabola leaves extra room on the side it opens towards. A cubic's viewport contains its local extrema and its values at the padded x-edges, so both arms are visible.
- **GraphView:** `GraphView(plot: PlotData, viewport: Viewport, modifier)`. Stateless, draws only, no math and no knowledge of equations. v1 passes the auto-fit viewport and has no gestures. Adding pan/zoom later means adding `onViewportChange: (Viewport) -> Unit` and a gesture modifier, with the ViewModel holding the viewport and re-sampling. `[C]`
- **ViewModel:** holds `StateFlow<GeneratorUiState>` and calls the engine. Everything is synchronous (no `viewModelScope`, no coroutines), so tests read `uiState.value` directly and need no `kotlinx-coroutines-test`. Events: mode selected, input changed, field focus changed, toggle sign, randomize, generate, set-up expanded changed. Randomness for Randomize is an injectable `Random`. Text cleaning and validation live in the pure `IntInput` and `GeneratorUiState`. `[C]`
- **GeneratorScreen:** stateless (`state` in, callbacks out), so it can be previewed and UI-tested without a ViewModel. `MainActivity` wires it with `by viewModels()` and `collectAsState()`. `[C]`
- **Strings:** all UI text is in `res/values/eqgen_strings.xml`, a separate file that cannot clash with the template's `strings.xml`. `[C]`

### UI state (sketch) `[C]`
```kotlin
enum class GeneratorMode { LinearOneVar, LinearTwoVar, LinearPair, LinearSystem3, Quadratic, Cubic } // each knows its input fields
enum class InputField { Root, X, Y, Z, Root1, Root2, Root3 }     // X, Y are shared by M2, M3, M5; Root1, Root2 by M4, M6

data class GeneratorUiState(
    val mode: GeneratorMode = GeneratorMode.LinearOneVar,
    val inputs: Map<InputField, String> = emptyMap(),   // raw text, already sanitized
    val result: GenerationResult? = null,
    val setupExpanded: Boolean = true,                  // the "Equation type" section
    val focusedField: InputField? = null,               // target of the sign button
) {
    val fields: List<FieldState>      // current mode's fields with text and error, derived
    val request: GenerationRequest?   // non-null only when every field is valid, derived
    val canGenerate: Boolean          // request != null
    val canToggleSign: Boolean        // focusedField is one of the current mode's fields
}
```
Errors are derived from the text (`InputError.OutOfRange`, `InputError.NotAWholeNumber`), not stored. An empty field or a lone `-` is "incomplete": it blocks Generate but shows no error.

## 05 — UI and interaction behavior

Single screen, no navigation `[C]`. Top to bottom:

1. **Top app bar** (`TopAppBar`): title "ΣqGen", content description "EqGen". `[C]`
2. **"Equation type" section** (a `Card`). `[C]`
   - Header row: title "Equation type" and a chevron (`R.drawable.ic_expand_more`, rotated 180° when expanded so it points up). The whole header is one tap target, with a Button role, "Collapse"/"Expand" click labels and an expanded/collapsed state description for TalkBack.
   - Body (hidden when collapsed, via `AnimatedVisibility`): six tiles in two columns and three rows, in this order: Linear 1 variable (`ax + b = 0`) · Linear 2 variables (`ax + by = c`) · Pair of linear 2 variables (2 equations) · System of linear 3 variables (3 equations) · Quadratic 1 variable (`ax² + bx + c = 0`) · Cubic 1 variable (`ax³ + … + d = 0`).
   - Each tile is an official `Card` marked as a radio-group item (`selectableGroup` and `selectable(role = RadioButton)`), with a title, a subtitle and an equation form. The selected tile uses `secondaryContainer` and a 2dp primary border. Text wraps, never truncates, and tiles in a row share one height. Tapping the selected tile does nothing.
   - Summary line, always visible (expanded or collapsed): for example "Pair of linear equations · 2 variables".
   - The expanded flag lives in `GeneratorUiState`, so it survives rotation. It starts expanded, collapses after every successful Generate, and can be reopened at any time.
3. **Input card** `[C]`: one rounded card (`Card` with `RoundedCornerShape(16.dp)`) in `primaryContainer`, so it reads as a different area from the "Equation type" card and the result card. Top to bottom:
   - **Mode hint**: one short line about what to enter.
   - **Input fields** for the selected mode, equal width in one row: M1 one field ("Root (x)"); M2 and M3 two (x, y); M5 three (x, y, z); M4 two (Root 1, Root 2); M6 three (Root 1, Root 2, Root 3). Inline error under a field; error text is short ("−100 to 100", "Whole numbers only").
   - **Action row**, left to right: **±** (`OutlinedIconButton`, `R.drawable.ic_plus_minus`, content description "Change sign of the selected field"): flips the sign of the focused field, disabled when no field is focused, and exists because numeric keyboards do not reliably offer a minus key; **Generate** (`Button`, fills the remaining width): enabled only when every field is valid, regenerates under the rule in section 03 when the inputs equal the displayed request, and there is no Retry button; **Randomize** (`OutlinedIconButton`, `R.drawable.ic_casino`, content description "Randomize"): fills every field of the current mode with a random integer from −10 to 10, excluding 0, repeats allowed, without generating and without touching the displayed result.
4. **Result** (after the first Generate): `EquationCard` with the equation text and solution, then the graph when the mode has one (not M5). The card uses `animateContentSize()` so it resizes smoothly between one and three equations. `[C]`

Input rules `[C]`: integers only, optional leading minus, inline error under the field, numeric keyboard. Typed text is cleaned as it arrives. `[P]` Cleaning: ASCII digits only, one leading `-`, at most 4 digits (one more than the 3 a valid value needs, so a pasted 1000 shows the range error instead of becoming 100). Out-of-range values (beyond ±100) show an inline error and block Generate. Typed inputs are kept when switching modes, and fields shared between modes carry over.
IME Done on the last field generates when inputs are valid, then hides the keyboard. `[P]`
Equation text `[P]`: plain text with a true minus sign (U+2212) and superscripts (², ³); coefficients of 1 are omitted and zero terms are skipped (`x² − 5x + 6 = 0`). Solutions list the roots exactly as entered, one per root (a double root reads `x = 4`, `x = 4`); there is no multiplicity text (deferred, D-034). Equations are laid out in three columns (left side right-aligned, "=", right side left-aligned) with tabular numerals, so the "=" signs line up across lines; lines never wrap, and the card scrolls horizontally if a line is too wide. This uses plain `Row`/`Column` (`Modifier.alignBy` cannot align across separate rows without a custom layout). The card text is selectable for copying.
No icons from the Compose icon library `[C]` (`material-icons-core` or `-extended`). Every icon is an XML vector asset in `res/drawable`, referenced as `R.drawable.ic_<name>`, added by the developer (D-018, D-032):

| Asset | Used for | Status |
|-------|----------|--------|
| `ic_expand_more.xml` | chevron on the "Equation type" header | to be added by the developer |
| `ic_casino.xml` | Randomize icon button | to be added by the developer |
| `ic_plus_minus.xml` | ± icon button | to be added by the developer |

Empty state: short hint text, no card and no graph.
Error state: inline field messages only; there is no network or generation failure path in v1.
State survives rotation (ViewModel). Editing inputs does not clear the displayed result or graph; the result is replaced only when Generate succeeds. `[C]` Switching mode also keeps it. Generate is disabled while inputs are invalid. The card shows the solution it was built from, so a result that no longer matches the fields is self-explanatory.
Theme `[C]`: Material 3 dynamic color on Android 12 (API 31) and newer; below that, the template's static purple palette stays as is.

Graph v1 `[C]`: static, auto-fitted viewport containing the solution; axes, grid, the curve or lines, and marked roots or intersection. No pan, zoom or reset button. The component is built so gestures can be added later without changing equation logic (section 04). Rendering: theme colors (`primary`, then `tertiary` for a second line; `secondary` for markers); grid at nice steps (1, 2 or 5 × 10ⁿ); tick labels along the bottom and left edges; markers as filled circles with an `(x, y)` label; far off-screen curve points are limited to a margin so steep curves stay renderable. All grid, label and pixel maths lives in the pure domain layer and is JVM-tested.

## 06 — Testing and acceptance criteria

### Layer 1 — JVM unit tests (written before UI) `[C]`
For each mode, run many seeds (for example 1,000) and assert:
- Solution invariants from section 03, including M5 (all three equations satisfied; Cramer's rule recovers the solution) and repeated roots in M4 and M6 (all combinations of repeats, `p'(r) == 0` for roots entered twice or more).
- All generated coefficients inside the allowed range and non-zero where required.
- M3 and M5 determinants are never 0.
- Determinism: same request and seed give an identical result.
- Regeneration: `EquationEngine.retry` gives a different equation (except where only one exists), keeps the request, and records its seed.
- Range: generated coefficients always lie in the configured range and are non-zero; derived values are not asserted against it.
- Formatter: exact text for every shape; generated equations never show an ASCII hyphen, a coefficient of 1 or 0, or a double sign; the two sides of each equation are separate.
- Input cleaning: `IntInput.sanitize`, `validate`, `toggleSign` and `randomValue`, including range limits, non-ASCII digits and the lone `-`.
- UI state: request building per mode, incomplete versus invalid fields, shared inputs across modes, the result surviving edits and mode switches, focus tracking and sign toggling, Randomize, and the expanded flag.
- ViewModel (plain JUnit, no coroutines test library): Generate blocked on invalid input; Generate on unchanged inputs regenerates a different equation; Generate after editing the inputs uses the new request; Generate collapses the section; Randomize fills the fields without generating.
- Overflow: worst-case inputs (±`MAX_INPUT_ABS` (100)) with the default and the widest config produce exact results.
- Edge inputs: roots of 0, negative roots, equal roots, extreme in-range inputs.
- `GraphSampler`: sampled points satisfy the equation; the fitted viewport contains the solution and, for a cubic, its local extrema; sampling one equation over several viewports gives consistent values (pan/zoom readiness); `hasGraph` is false for M5.

### Layer 2 — Android UI tests `[C]`
Using `createComposeRule`, with the engine faked or seeded:
- Invalid input shows an error and keeps Generate disabled.
- Generate shows the card and graph (no graph for M5) and collapses the "Equation type" section; the header reopens it.
- Tapping Generate again with unchanged input changes the displayed equation but not the inputs.
- Editing inputs after a result keeps the card and graph until Generate is tapped.
- Tile selection shows the correct number of fields; tapping the selected tile changes nothing.
- The ± button is disabled without focus and flips the sign of the focused field.
- Randomize fills the fields without producing a result.
- Rotation preserves state.

### Layer 3 — Device checklist
Run on a real device and an API 24 emulator: the graph renders correctly, nothing clips on small screens, tiles do not truncate at large font sizes, and dark mode is readable.

Status, first pass on an API 28 device (2026-10-17, previous chip-based UI):
- Passed: app builds and runs; dark mode; rotation keeps inputs, result and graph; editing inputs leaves the card and graph unchanged until Generate; no overlapping tick labels; no parabola arms clipped at the edge.
- Found: the "Linear (2 vars)" chip label truncated at large font sizes (resolved by the tiles, D-023); the static purple palette shows below API 31 (kept, D-033).
- Open: the whole checklist must be re-run on the tile UI, including the API 24 emulator, a small screen with large fonts, an API 31+ device for dynamic color, and the cubic and 3-variable modes.

### v1 acceptance
All six modes generate valid, verified equations; regeneration works as specified; Layer 1 passes in full; Layer 2 passes; Layer 3 checklist is signed off.

## 07 — Decisions and roadmap

### Decision log
| ID | Decision | Status |
|----|----------|--------|
| D-001 | Spec first; math tests before Compose | C |
| D-002 | Domain is pure Kotlin | C |
| D-003 | Native Compose Canvas for graphs | C |
| D-004 | minSdk 24, targetSdk 37, no Hilt, no Navigation, ViewModel + StateFlow | C |
| D-005 | Int-only values in v1 | C |
| D-006 | Retry semantics: same request, new seed, result differs (now reached through Generate, see D-022) | C |
| D-007 | Model changes for future number types are accepted as a domain-only migration | C |
| D-008 | Graph data computed in domain (`GraphSampler`), drawn in presentation | C |
| D-009 | Equal roots allowed; pairs are unique-solution only | C |
| D-010 | Coefficient range applies to generated coefficients only (see section 03); derived values unrestricted | C |
| D-011 | Range `[-10, 20]` is intentionally asymmetric; configurable in a future version | C |
| D-012 | Range lives in `GenerationConfig` from v1 | C |
| D-013 | Result and graph persist across input edits until the next Generate | C |
| D-014 | ~~Retry uses the request stored in the displayed result~~ (superseded by D-022) | — |
| D-015 | v1 graph is static and auto-fitted; `GraphView` and `GraphSampler` are viewport-parameterized so pan/zoom can be added later | C |
| D-016 | M1 displays as `ax + b = 0` only | C |
| D-017 | User-entered roots and solutions capped at `[-100, 100]` (`MAX_INPUT_ABS = 100`) for overflow safety and graph readability | C |
| D-018 | No Compose icon library; icons are XML vector assets in `res/drawable`, imported by the developer | C |
| D-019 | ViewModel is synchronous with no coroutines scope; unit tests need only JUnit | C |
| D-020 | ~~"±" button on each number field~~ (superseded by D-029) | — |
| D-021 | Plain-text equation format: U+2212 minus, superscripts, repeated roots as entered (revised by D-034) | P |
| D-022 | The Retry button is removed; Generate on unchanged input regenerates with the must-differ rule | C |
| D-023 | Mode selector is a collapsible set of six tiles (two columns, three rows); resolves the chip truncation | C |
| D-024 | M5 (three-variable linear system) and M6 (cubic) join v1; M5 has no graph | C |
| D-025 | Quadratic model generalized to `PolynomialEquation(roots, leadingCoefficient)`; `MAX_POLYNOMIAL_DEGREE = 3` | C |
| D-026 | Repeated roots are never restricted or altered | C |
| D-027 | Official Compose/Material components first; no custom composable duplicating one | C |
| D-028 | "Equation type" section collapses via a header with a chevron, an always-visible summary line, and a flag in UI state | C |
| D-029 | One ± button below the fields acts on the focused field; three fields fit in one row | C |
| D-030 | Randomize fills the fields with −10..10 excluding 0 and does not generate | C |
| D-031 | In-app brand "ΣqGen" in the top app bar title only (spoken as "EqGen"); launcher name unchanged | C |
| D-032 | Assets are XML vectors named `ic_<name>.xml`, referenced as `R.drawable.ic_<name>`, added by the developer | C |
| D-033 | Dynamic color on API 31+; the template's static purple palette stays below that | C |
| D-034 | Multiplicity text ("double root") and the live input echo are deferred; roots are listed as entered | C |
| D-035 | "=" alignment uses plain Row/Column columns, not `alignBy` or a custom layout | C |
| D-036 | Input cleaning keeps up to 4 digits so over-range pastes show the range error instead of being cut to a valid number | P |
| D-037 | Focus is a changed event (gain and loss); a blur from a non-focused field is ignored; switching mode clears the focused field | C |
| D-039 | The selected equation type is remembered between launches in `SharedPreferences` through a `ModeStore` interface (default store remembers nothing); typed values, results and the expanded flag are not saved | C |
| D-040 | Input section is one `primaryContainer` card holding hint, fields and an icon-only ± / Generate / icon-only Randomize row | C |
| D-038 | `presentation` never imports `screens`: `EquationTypeSection` takes `TileSpec` data; `screens` maps `ModeTileSpecs` onto it | C |

### Open questions
- **O9 `[O]`:** Items still tagged `[P]` stay unapproved: the input cleaning limits (4 typed digits), Done-key Generate, and the plain-text equation format (D-021).

Resolved: O1 (D-010), O2 (D-011), O3 (D-013), O4 (D-015), O5 (D-016), O6 (D-017), O7 (D-023), O8 (D-033).

### Known limitations (v1)
Integers only; no singular or no-solution systems; no export; one screen; M5 has no graph; entered values and results do not survive process death (they do survive rotation); regenerating the displayed request after editing the fields means restoring the old values by hand (D-022).

### Change history
- 2026-10-16: initial draft from brainstorm.
- 2026-10-16: resolved O1–O5 (D-010 to D-016); added `GenerationConfig`, viewport-parameterized graph design, Retry-from-displayed-request rule, and O6.
- 2026-10-16: resolved O6 (input cap 100) and approved all proposed items; the spec is now the v1 baseline with no open questions.
- 2026-10-17: domain layer implemented with 58 JVM tests. Final generated-type names are `LinearOneVar`, `LinearTwoVar`, `LinearPair`, `Quadratic`; added `MAX_COEFFICIENT_ABS`. The code is authoritative for exact signatures.
- 2026-10-17: added `Viewport`, `Point`, `PlotData` and `GraphSampler` with 27 JVM tests (85 in total).
- 2026-10-17: added `GraphGrid`, `GraphTransform` (21 more JVM tests, 106 in total) and the Compose `GraphView` (static Canvas; Layer 3 device check still pending).
- 2026-10-17: added `EquationFormatter`, `IntInput`, `GeneratorUiState`, `GeneratorViewModel` (59 more JVM tests, 165 in total) and the Compose `IntegerField`, `ModeSelector`, `EquationCard`, `GeneratorScreen`, `MainActivity` and `eqgen_strings.xml`; added D-018 to D-021. Compose files are type-checked only, not built; Layer 2 and 3 pending.
- 2026-10-17: Layer 3 first pass on an API 28 device recorded in section 06 (found chip truncation and the static purple palette).
- 2026-10-19: scope revised after the UI brainstorm. Added M5 and M6, the tile selector with a collapsible "Equation type" section, Randomize, one focused-field ± button and the ΣqGen branding. Removed the Retry button and the model type `QuadraticEquation` (now `PolynomialEquation`). Deferred the live input echo and multiplicity text. Added D-022 to D-035; D-014 and D-020 superseded; D-021 revised; O7 and O8 resolved.
- 2026-10-19: implemented the revision. Domain (`PolynomialEquation`, M5 generator, formatter lines, cubic graph support) and state layer (six modes, focus, ±, Randomize, collapse flag, Generate-as-regenerate) with 231 JVM tests in total; tile UI (`EquationTypeSection`, `ModeTileSpecs`, three-column `EquationCard`) written and type-checked against stubs only. Gradle build, Layer 2 and Layer 3 pending. Fixed a `-0.0` duplicate x in the sampler's exact points.
- 2026-10-19: audit fixes: ± now disables on blur (D-037), 4-digit cleaning (D-036), presentation→screens cycle removed (D-038), `GeneratorMode` order matches tile order and `specFor` fails loudly, duplicate section animation dropped, `collectAsStateWithLifecycle`, U+2212 in the range error. 238 JVM tests plus `ModeTileSpecsTest`.
- 2026-10-21: added the Layer 2 tests (`androidTest/GeneratorScreenTest.kt`, 8 UI tests plus a rotation test in `GeneratorRotationTest`) and the `RESULT_CARD_TAG` test tag on the result card. Not yet run on a device.
- 2026-10-23: remembered equation type (D-039) and the input card with icon buttons (D-040); 246 JVM tests. UI type-checked against stubs only; Layer 2 tests updated for the icon-only Randomize but not yet run.
