package dev.gaboron.spwstudio.color

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class ArgbColor(
    val alpha: Int = 255,
    val red: Int,
    val green: Int,
    val blue: Int,
) {
    init {
        require(alpha in 0..255 && red in 0..255 && green in 0..255 && blue in 0..255) {
            "颜色分量必须在 0..255"
        }
    }

    fun toHsv(): HsvColor {
        val r = red / 255.0
        val g = green / 255.0
        val b = blue / 255.0
        val maxValue = max(r, max(g, b))
        val minValue = min(r, min(g, b))
        val delta = maxValue - minValue
        val hue = when {
            delta == 0.0 -> 0.0
            maxValue == r -> 60.0 * (((g - b) / delta) % 6.0)
            maxValue == g -> 60.0 * (((b - r) / delta) + 2.0)
            else -> 60.0 * (((r - g) / delta) + 4.0)
        }
        return HsvColor(
            hue = if (hue < 0) hue + 360.0 else hue,
            saturation = if (maxValue == 0.0) 0.0 else delta / maxValue,
            value = maxValue,
        )
    }

    fun toHex(includeAlpha: Boolean = alpha < 255): String = buildString {
        append('#')
        if (includeAlpha) append("%02X".format(alpha))
        append("%02X%02X%02X".format(red, green, blue))
    }

    companion object {
        fun fromHsv(hsv: HsvColor, alpha: Int = 255): ArgbColor {
            val h = ((hsv.hue % 360.0) + 360.0) % 360.0
            val s = hsv.saturation.coerceIn(0.0, 1.0)
            val v = hsv.value.coerceIn(0.0, 1.0)
            val chroma = v * s
            val x = chroma * (1 - abs((h / 60.0) % 2 - 1))
            val m = v - chroma
            val (r, g, b) = when {
                h < 60 -> Triple(chroma, x, 0.0)
                h < 120 -> Triple(x, chroma, 0.0)
                h < 180 -> Triple(0.0, chroma, x)
                h < 240 -> Triple(0.0, x, chroma)
                h < 300 -> Triple(x, 0.0, chroma)
                else -> Triple(chroma, 0.0, x)
            }
            return ArgbColor(
                alpha = alpha.coerceIn(0, 255),
                red = ((r + m) * 255).roundToInt().coerceIn(0, 255),
                green = ((g + m) * 255).roundToInt().coerceIn(0, 255),
                blue = ((b + m) * 255).roundToInt().coerceIn(0, 255),
            )
        }

        fun parse(value: String): ArgbColor {
            val text = value.trim().removePrefix("#")
            val expanded = when (text.length) {
                3 -> text.map { "$it$it" }.joinToString("")
                4 -> text.map { "$it$it" }.joinToString("")
                else -> text
            }
            return when (expanded.length) {
                6 -> ArgbColor(
                    red = expanded.substring(0, 2).toInt(16),
                    green = expanded.substring(2, 4).toInt(16),
                    blue = expanded.substring(4, 6).toInt(16),
                )
                8 -> ArgbColor(
                    alpha = expanded.substring(0, 2).toInt(16),
                    red = expanded.substring(2, 4).toInt(16),
                    green = expanded.substring(4, 6).toInt(16),
                    blue = expanded.substring(6, 8).toInt(16),
                )
                else -> throw IllegalArgumentException("颜色格式无效: $value")
            }
        }
    }
}

data class HsvColor(
    val hue: Double,
    val saturation: Double,
    val value: Double,
)

/**
 * Keeps the last chromatic hue while the user is in grayscale, so raising saturation
 * again does not jump back to red.
 */
class HsvSelection(initial: ArgbColor = ArgbColor(red = 255, green = 255, blue = 255)) {
    var hue: Double = initial.toHsv().hue
        private set
    var saturation: Double = initial.toHsv().saturation
        private set
    var value: Double = initial.toHsv().value
        private set
    var alpha: Int = initial.alpha
        private set

    fun setArgb(color: ArgbColor) {
        val hsv = color.toHsv()
        if (hsv.saturation > HUE_EPSILON) hue = hsv.hue
        saturation = hsv.saturation
        value = hsv.value
        alpha = color.alpha
    }

    fun setHue(hue: Double) {
        this.hue = ((hue % 360.0) + 360.0) % 360.0
    }

    fun setSaturation(saturation: Double) {
        this.saturation = saturation.coerceIn(0.0, 1.0)
    }

    fun setValue(value: Double) {
        this.value = value.coerceIn(0.0, 1.0)
    }

    fun setAlpha(alpha: Int) {
        this.alpha = alpha.coerceIn(0, 255)
    }

    fun toArgb(): ArgbColor = ArgbColor.fromHsv(HsvColor(hue, saturation, value), alpha)

    private companion object {
        const val HUE_EPSILON = 0.0001
    }
}
