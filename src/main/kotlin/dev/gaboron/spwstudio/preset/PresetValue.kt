package dev.gaboron.spwstudio.preset

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull

/** A single persisted setting value. Presets never store nested objects. */
sealed interface PresetValue {
    fun toJson(): JsonPrimitive

    data class Bool(val value: Boolean) : PresetValue {
        override fun toJson(): JsonPrimitive = JsonPrimitive(value)
    }

    data class IntValue(val value: Int) : PresetValue {
        override fun toJson(): JsonPrimitive = JsonPrimitive(value)
    }

    data class FloatValue(val value: Float) : PresetValue {
        override fun toJson(): JsonPrimitive = JsonPrimitive(value)
    }

    data class Text(val value: String) : PresetValue {
        override fun toJson(): JsonPrimitive = JsonPrimitive(value)
    }

    companion object {
        fun fromJson(element: JsonElement): PresetValue {
            if (element !is JsonPrimitive) {
                throw PresetFormatException("预设值只能是布尔、数字或字符串")
            }
            if (element.isString) return Text(element.content)
            element.booleanOrNull?.let { return Bool(it) }
            element.intOrNull?.let { return IntValue(it) }
            element.floatOrNull?.let { return FloatValue(it) }
            throw PresetFormatException("无法识别的预设值: ${element.content}")
        }
    }
}

class PresetFormatException(message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)
