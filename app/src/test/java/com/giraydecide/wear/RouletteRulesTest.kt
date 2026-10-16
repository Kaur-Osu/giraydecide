package com.giraydecide.wear

import org.junit.Assert.*
import org.junit.Test

class RouletteRulesTest {
    @Test fun rejectsDuplicatesIgnoringCaseAndWhitespace() {
        assertFalse(RouletteRules.canAdd(listOf("Pizza"), " pizza "))
        assertTrue(RouletteRules.canAdd(listOf("Pizza"), "Sopa"))
    }
    @Test fun boundsAndBlankInputAreEnforced() {
        assertFalse(RouletteRules.canAdd(emptyList(), " "))
        assertFalse(RouletteRules.canAdd(emptyList(), "a".repeat(49)))
        assertFalse(RouletteRules.canAdd(List(16) { "Opción $it" }, "Otra"))
        assertTrue(RouletteRules.canAdd(List(15) { "Opción $it" }, "Otra"))
    }
    @Test fun pointerMatchesEverySelectedSector() {
        for (size in 2..16) for (index in 0 until size) {
            val rotation = RouletteRules.targetRotation(index, size)
            val angleUnderPointer = (360f - rotation) % 360f
            assertEquals(index, (angleUnderPointer / (360f / size)).toInt())
        }
    }
    @Test(expected = IllegalArgumentException::class) fun requiresAtLeastTwoOptions() {
        RouletteRules.pick(1)
    }
}