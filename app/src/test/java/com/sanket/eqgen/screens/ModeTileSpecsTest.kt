package com.sanket.eqgen.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class ModeTileSpecsTest {

    @Test
    fun everyModeHasExactlyOneTileSpec_inTileOrder() {
        assertEquals(GeneratorMode.entries, modeTileSpecs.map { it.mode })
    }

    @Test
    fun specFor_returnsTheSpecOfThatMode() {
        for (mode in GeneratorMode.entries) assertEquals(mode, specFor(mode).mode)
    }

    @Test
    fun modeTiles_matchTheSpecs() {
        assertEquals(modeTileSpecs.map { it.tile }, modeTiles)
        assertEquals(6, modeTiles.size)
    }
}
