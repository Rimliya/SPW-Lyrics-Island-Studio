package dev.gaboron.spwstudio.color

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

class ColorTest {
    @Test
    fun `hex parsing supports short and alpha forms`() {
        assertEquals(ArgbColor(red = 255, green = 0, blue = 0), ArgbColor.parse("#F00"))
        assertEquals(ArgbColor(alpha = 128, red = 255, green = 0, blue = 0), ArgbColor.parse("#80FF0000"))
        assertEquals("#FF0000", ArgbColor.parse("#f00").toHex())
    }

    @Test
    fun `rgb and hsv round trip`() {
        val colors = listOf(
            ArgbColor(red = 255, green = 0, blue = 0),
            ArgbColor(red = 0, green = 255, blue = 0),
            ArgbColor(red = 0, green = 0, blue = 255),
            ArgbColor(red = 103, green = 80, blue = 164),
        )
        colors.forEach { color ->
            val roundTrip = ArgbColor.fromHsv(color.toHsv(), color.alpha)
            assertEquals(color, roundTrip)
        }
    }

    @Test
    fun `grayscale keeps the previous hue`() {
        val selection = HsvSelection(ArgbColor(red = 255, green = 0, blue = 0))
        selection.setSaturation(0.0)
        val gray = selection.toArgb()
        assertTrue(abs(gray.red - gray.green) <= 1 && abs(gray.green - gray.blue) <= 1)

        selection.setSaturation(1.0)
        assertEquals(ArgbColor(red = 255, green = 0, blue = 0), selection.toArgb())
    }
}
