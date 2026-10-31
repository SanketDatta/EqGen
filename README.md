# ΣqGen — Equation Generator

**Flip the equation! Generate valid algebraic systems directly from your chosen roots.**

Most apps take an equation and give you the answers. **ΣqGen** does the exact opposite! Built from the ground up for Android, ΣqGen takes your roots or solution coordinates and works backward to construct mathematically sound equations—complete with crisp 2D graph visualizations. Built with **Kotlin** and **Jetpack Compose**, ΣqGen focuses on mathematical correctness, a clean interface, and a lightweight architecture.

Whether you are a **teacher** crafting fresh test questions in seconds or a **student** exploring how roots shape curves, ΣqGen makes equation theory visual, instant, and fun!

## Features

- **Reverse Equation Builder:** Enter your desired roots or solution points; ΣqGen delivers valid, ready-to-use equations.
- **Multiple Generation Modes:** Full coverage across linear systems, quadratic equations, and cubic polynomials.
- **Instant Visual Graphs:** Interactive 2D graphing with dynamic axis fitting, grid lines, and key solution points highlighted.
- **One-Tap Variation:** Tap **Generate** to scramble valid coefficient variations for the same input, or hit **Randomize** to spin up entirely new inputs!
- **Smart & Repeated Roots:** Full support for repeated roots in quadratics and cubics, paired with real-time input validation and sign toggling.
- **100% Offline & Private:** No accounts, zero permissions, and complete offline privacy by design.
- **Remembers Your State:** Seamlessly resumes on your last-used generation mode every time you open the app.

## Supported Equation Types

ΣqGen currently supports **six equation modes**, with even more on the horizon:

| Mode                        | Input                        | Generated Form                           |
| --------------------------- | ---------------------------- | ---------------------------------------- |
| Linear (1 variable)         | Root $x$                     | $ax+b=0$                                 |
| Linear (2 variables)        | Point $(x_0,y_0)$            | $ax+by=c$                                |
| Linear System (2 variables) | Shared point $(x_0,y_0)$     | Pair of equations with a unique solution |
| Linear System (3 variables) | Shared point $(x_0,y_0,z_0)$ | Three equations with a unique solution   |
| Quadratic equation          | Roots $x_1,x_2$              | $ax^2+bx+c=0$                            |
| Cubic equation              | Roots $x_1,x_2,x_3$          | $ax^3+bx^2+cx+d=0$                       |
...and many more equation types to come!

### Visual Graphing

Built natively using **Jetpack Compose Canvas**, ΣqGen instantly renders equations with dynamic bounds so you can visualize the theory behind the math:

- **Linear Equations:** Drawn as crisp straight lines.
- **Quadratic & Cubic:** Visualized as beautiful curves and parabolas.
- **2-Variable Systems:** Highlights the exact **intersection point** of both lines.
- **Smart Fitting:** Automatically scales axes, grid lines, and root markers around your inputs.

_(Note: 3-variable linear systems do not render on the 2D canvas. Pan and zoom capabilities are coming in future releases!)_

## Mathematical Constraints

To ensure clean and correct math problems suitable for practice and tests:

- **Integer-Based Math:** Inputs, roots, and primary generated coefficients stay clean with integer values.
- **Input Bounds:** Roots and solution coordinates can be set anywhere within **$[-100, 100]$**.
- **Smart Coefficients:** Non-zero primary coefficients are randomly selected from **$[-10, 20]$**.
- **Guaranteed Solvability:** Linear systems are strictly engineered for **unique solutions** (no solution/infinitely many solutions cases are skipped in current release).
- **Repeated Roots:** Full algebraic accuracy when identical roots are provided for quadratics and cubics.

---
## Technology Stack

| Component               | Technology                                  |
| ----------------------- | ------------------------------------------- |
| Platform                | Android                                     |
| Language                | Kotlin                                      |
| UI                      | Jetpack Compose                             |
| Design system           | Material 3 (with Dynamic Colors on API 31+) |
| State management        | ViewModel and StateFlow                     |
| Graph rendering         | Compose Canvas                              |
| Mathematical engine     | Pure Kotlin                                 |
| Unit testing            | JUnit                                       |
| Minimum Android version | Android 7.0 (API 24)                        |
| Target Android SDK      | API 37                                      |
**Privacy First:** ΣqGen collects zero data. The only state saved is your last-used equation mode in `SharedPreferences`.

## Architecture

ΣqGen separates mathematical logic from Android UI code.

```text
com.sanket.eqgen/
├── MainActivity.kt
├── domain/
│   ├── model/       # Requests, equations, configuration, results
│   ├── generator/   # Equation generation and regeneration
│   ├── graph/       # Graph sampling, grids, and transforms
│   └── format/      # Equation-to-text formatting
├── screens/         # Screen state, ViewModel, and coordination
└── presentation/
    ├── theme/       # App theme
    ├── components/  # Reusable Compose components
    └── graph/       # Graph rendering
```

### Core Design Principles

- **Pure Mathematical Domain:** Equation generation and graph calculations are entirely independent of Android and Compose frameworks.
- **Separation of Concerns:** Mathematical models, equation formatting, UI state, and custom rendering have strict, distinct boundaries.
- **Deterministic Generation:** Generators accept an explicit random source, making results completely reproducible from a request and seed.
- **Testable Mathematics:** The core engine runs pure on the JVM, allowing fast unit testing without needing an Android device or emulator.
- **Minimal Dependencies:** Zero bloat, avoiding unnecessary abstraction layers or heavy third-party frameworks.
- **Designed for Growth:** Sealed models and isolated generation logic make adding future equation types effortless.

## Getting started

### Prerequisites

- Android Studio
- A compatible JDK and Android SDK
- Android SDK Platform 37
- An Android device or emulator running API 24 or later

### Build from Source

1. Clone the repository:

```bash
git clone <repository-url>
cd <repository-directory>
```

2. Open the project in Android Studio and allow Gradle to sync. Then build the debug APK:

```bash
./gradlew assembleDebug
```

  On Windows (PowerShell), use:

```powershell
.\gradlew.bat assembleDebug
```

3. Install and run the application on a connected device or emulator through Android Studio.

### Run tests

Run the JVM unit tests (core math domain):

```bash
./gradlew testDebugUnitTest
```

Run the instrumented Android tests on a connected device or emulator:

```bash
./gradlew connectedDebugAndroidTest
```

_(Note: Exact task names can vary depending on your build variants and configuration.)_

---
## Project Status & Roadmap

The current version of **ΣqGen** delivers a solid core foundation: all 6 generation modes, pure Kotlin mathematical models, automatic 2D graph rendering, and a clean Material 3 single-screen Compose UI.

### Development Roadmap

- [x] Implement 6 core equation generation modes
- [x] 2D graph visualizer via Compose Canvas
- [x] Modern Material 3 UI components
- [ ] Render equations with standard math fonts
- [ ] Support fractional, decimal, radical, and complex roots
- [ ] Support additional variables
- [ ] Higher-degree polynomial equations
- [ ] Larger systems of linear equations
- [ ] Step-by-step derivations and solution breakdowns
- [ ] Graph panning and zooming controls
- [ ] Equation export and sharing options
- [ ] Equation history and favorites
- [ ] Configurable coefficient ranges
- [ ] Support for non-polynomial function types
- [ ] Information cards for well-known mathematical curves

These are possibilities rather than committed release milestones.

## Contributing

Contributions, bug reports, and suggestions are welcome!

For changes to the mathematical engine, correctness is the priority: generated equations must satisfy the supplied roots or solution, and systems that promise a unique solution must have a non-zero determinant.

When proposing a new equation type, keep mathematical logic independent of the UI and include tests for its invariants and edge cases.

## License

This project is licensed under the **MIT License** — feel free to use, modify, and distribute it as specified in the repository's `LICENSE` file.
