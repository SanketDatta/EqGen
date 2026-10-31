package com.sanket.eqgen.screens

/**
 * Remembers the selected equation type between app launches (spec D-039). Only the mode is stored;
 * typed values and results are not. Pure Kotlin, so the ViewModel can be tested with a fake.
 */
interface ModeStore {
    /** The saved mode, or null when nothing was saved or the saved value is no longer a known mode. */
    fun load(): GeneratorMode?

    fun save(mode: GeneratorMode)
}

/** A store that remembers nothing. The default for tests and previews. */
object NoModeStore : ModeStore {
    override fun load(): GeneratorMode? = null

    override fun save(mode: GeneratorMode) = Unit
}

/** Mode names are the stored form; an unknown or missing name means "no saved mode". */
fun modeFromName(name: String?): GeneratorMode? = GeneratorMode.entries.firstOrNull { it.name == name }
