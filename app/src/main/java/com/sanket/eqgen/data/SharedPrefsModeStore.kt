package com.sanket.eqgen.data

import android.content.Context
import androidx.core.content.edit
import com.sanket.eqgen.screens.GeneratorMode
import com.sanket.eqgen.screens.ModeStore
import com.sanket.eqgen.screens.modeFromName

/**
 * Keeps the last selected mode in `SharedPreferences`. Reads and writes one small value, so doing it
 * synchronously on the main thread is fine and keeps the ViewModel free of coroutines.
 */
class SharedPrefsModeStore(context: Context) : ModeStore {

    private val prefs = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override fun load(): GeneratorMode? = modeFromName(prefs.getString(KEY_MODE, null))

    override fun save(mode: GeneratorMode) {
        prefs.edit { putString(KEY_MODE, mode.name) }
    }

    private companion object {
        const val FILE_NAME = "eqgen_settings"
        const val KEY_MODE = "selected_mode"
    }
}
