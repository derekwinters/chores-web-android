package com.derekwinters.chores.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorHexTest {

    @Test
    fun parseHexColor_sixDigit_assumesFullyOpaque() {
        assertEquals(Color(0xFFFF0000), parseHexColor("#FF0000"))
    }

    @Test
    fun parseHexColor_eightDigit_usesProvidedAlpha() {
        assertEquals(Color(0x80FF0000), parseHexColor("#80FF0000"))
    }

    @Test
    fun parseHexColor_malformed_returnsFallback() {
        assertEquals(Color.Blue, parseHexColor("not-a-color", fallback = Color.Blue))
    }

    @Test
    fun isValidHexColor_acceptsSixAndEightDigitForms() {
        assertTrue(isValidHexColor("#FF0000"))
        assertTrue(isValidHexColor("#80ff0000"))
        assertTrue(isValidHexColor("FF0000")) // leading # optional, mirroring parseHexColor
    }

    @Test
    fun isValidHexColor_rejectsMalformedInput() {
        assertFalse(isValidHexColor("not-a-color"))
        assertFalse(isValidHexColor("#FF00")) // wrong length
        assertFalse(isValidHexColor("#GG0000")) // non-hex digits
        assertFalse(isValidHexColor(""))
    }
}
