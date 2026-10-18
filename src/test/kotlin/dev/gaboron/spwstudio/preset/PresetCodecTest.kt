package dev.gaboron.spwstudio.preset

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PresetCodecTest {
    private val sample = Preset(
        "我的预设",
        linkedMapOf(
            "enabled" to PresetValue.Bool(true),
            "corner" to PresetValue.IntValue(60),
            "opacity" to PresetValue.FloatValue(0.6f),
            "title" to PresetValue.Text("hello world"),
        ),
    )

    @Test
    fun `json round trip keeps types and name`() {
        val decoded = PresetCodec.decodeJson(PresetCodec.encodeJson(sample), "fallback")
        assertEquals(sample, decoded)
    }

    @Test
    fun `flat json object is accepted`() {
        val decoded = PresetCodec.decodeJson("""{"enabled":true,"corner":60,"opacity":0.6,"title":"x"}""", "外部")
        assertEquals("外部", decoded.name)
        assertEquals(PresetValue.Bool(true), decoded.values["enabled"])
        assertEquals(PresetValue.IntValue(60), decoded.values["corner"])
        assertEquals(PresetValue.FloatValue(0.6f), decoded.values["opacity"])
        assertEquals(PresetValue.Text("x"), decoded.values["title"])
    }

    @Test
    fun `txt round trip quotes values that look numeric`() {
        val preset = sample.copy(
            values = linkedMapOf(
                "numericText" to PresetValue.Text("123"),
                "boolText" to PresetValue.Text("true"),
                "multiline" to PresetValue.Text("a\nb"),
                "number" to PresetValue.IntValue(7),
            ),
        )
        val decoded = PresetCodec.decodeTxt(PresetCodec.encodeTxt(preset), preset.name)
        assertEquals(preset, decoded)
    }

    @Test
    fun `txt rejects lines without separator`() {
        val failure = runCatching { PresetCodec.decodeTxt("broken", "x") }.exceptionOrNull()
        assertTrue(failure is PresetFormatException)
    }
}
