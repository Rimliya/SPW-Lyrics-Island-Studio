package dev.gaboron.spwstudio.clock

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZoneOffset

class ClockTest {
    private val midnight = 1_792_281_600_000L
    private val afternoon = midnight + 13 * 60 * 60 * 1000 + 5 * 60 * 1000

    @Test
    fun `formats 24 and 12 hour clocks`() {
        assertEquals("00:00", ClockFormat.format(midnight, ZoneOffset.UTC, use24Hour = true))
        assertEquals("12:00", ClockFormat.format(midnight, ZoneOffset.UTC, use24Hour = false))
        assertEquals("13:05", ClockFormat.format(afternoon, ZoneOffset.UTC, use24Hour = true))
        assertEquals("01:05", ClockFormat.format(afternoon, ZoneOffset.UTC, use24Hour = false))
    }

    @Test
    fun `glyph tables expose expected shapes`() {
        assertEquals(7, SevenSegmentGlyphs.segmentsFor('8').size)
        assertEquals(2, SevenSegmentGlyphs.segmentsFor('1').size)
        assertEquals(7, DotMatrixGlyphs.patternFor('0').size)
        assertEquals(5, PixelGlyphs.patternFor('0').size)
        assertEquals(3, PixelGlyphs.patternFor('0').first().length)
    }
}
