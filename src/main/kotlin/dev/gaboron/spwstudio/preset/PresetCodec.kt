package dev.gaboron.spwstudio.preset

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.nio.file.Path

enum class PresetFormat {
    JSON,
    TXT,
    ;

    companion object {
        fun detect(file: Path): PresetFormat = when (file.fileName.toString().substringAfterLast('.', "").lowercase()) {
            "txt" -> TXT
            "json" -> JSON
            else -> JSON
        }
    }
}

object PresetCodec {
    private const val MODEL_VERSION = 1
    private val json = Json { prettyPrint = true }
    private val integer = Regex("^-?\\d+$")
    private val decimal = Regex("^-?(?:\\d+\\.\\d+|\\d+[eE][-+]?\\d+|\\d+\\.\\d+[eE][-+]?\\d+)$")

    fun decode(text: String, format: PresetFormat, fallbackName: String): Preset = when (format) {
        PresetFormat.JSON -> decodeJson(text, fallbackName)
        PresetFormat.TXT -> decodeTxt(text, fallbackName)
    }

    fun encode(preset: Preset, format: PresetFormat): String = when (format) {
        PresetFormat.JSON -> encodeJson(preset)
        PresetFormat.TXT -> encodeTxt(preset)
    }

    fun decodeJson(text: String, fallbackName: String): Preset {
        val root = runCatching { Json.parseToJsonElement(text) }
            .getOrElse { throw PresetFormatException("JSON 预设解析失败: ${it.message}", it) }
        if (root !is JsonObject) throw PresetFormatException("JSON 预设必须是对象")

        val wrapper = root["values"] as? JsonObject
        val valuesObject = if (wrapper != null && (root.containsKey("version") || root.containsKey("name"))) {
            wrapper
        } else {
            root
        }
        val name = if (valuesObject !== root) {
            (root["name"] as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank) ?: fallbackName
        } else {
            fallbackName
        }
        val values = LinkedHashMap<String, PresetValue>()
        valuesObject.forEach { (key, element) ->
            if (key.isBlank()) throw PresetFormatException("预设键不能为空")
            values[key] = PresetValue.fromJson(element)
        }
        return Preset(name, values)
    }

    fun encodeJson(preset: Preset): String = json.encodeToString(
        JsonObject.serializer(),
        buildJsonObject {
            put("version", JsonPrimitive(MODEL_VERSION))
            put("name", JsonPrimitive(preset.name))
            put(
                "values",
                buildJsonObject {
                    preset.values.forEach { (key, value) -> put(key, value.toJson()) }
                },
            )
        },
    )

    fun decodeTxt(text: String, fallbackName: String): Preset {
        val values = LinkedHashMap<String, PresetValue>()
        text.lineSequence().forEachIndexed { index, raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEachIndexed
            val separator = line.indexOf('=')
            if (separator <= 0) throw PresetFormatException("第 ${index + 1} 行缺少 键=值")
            val key = line.substring(0, separator).trim()
            if (key.isEmpty()) throw PresetFormatException("第 ${index + 1} 行的键为空")
            values[key] = parseScalar(line.substring(separator + 1).trim())
        }
        return Preset(fallbackName, values)
    }

    fun encodeTxt(preset: Preset): String = buildString {
        appendLine("# SPW Lyrics Island Studio preset")
        preset.values.forEach { (key, value) ->
            append(key).append('=').append(encodeScalar(value)).appendLine()
        }
    }

    private fun parseScalar(raw: String): PresetValue {
        if (raw.length >= 2 && raw.startsWith('"') && raw.endsWith('"')) {
            return PresetValue.Text(unescape(raw.substring(1, raw.length - 1)))
        }
        when (raw.lowercase()) {
            "true" -> return PresetValue.Bool(true)
            "false" -> return PresetValue.Bool(false)
        }
        if (integer.matches(raw)) {
            raw.toIntOrNull()?.let { return PresetValue.IntValue(it) }
            throw PresetFormatException("整数超出 Int 范围: $raw")
        }
        if (decimal.matches(raw)) {
            raw.toFloatOrNull()?.let { return PresetValue.FloatValue(it) }
            throw PresetFormatException("浮点数格式无效: $raw")
        }
        return PresetValue.Text(raw)
    }

    private fun encodeScalar(value: PresetValue): String = when (value) {
        is PresetValue.Bool -> value.value.toString()
        is PresetValue.IntValue -> value.value.toString()
        is PresetValue.FloatValue -> value.value.toString()
        is PresetValue.Text -> if (needsQuotes(value.value)) "\"${escape(value.value)}\"" else value.value
    }

    private fun needsQuotes(value: String): Boolean =
        value.isEmpty() || value != value.trim() || value.lowercase() in setOf("true", "false") ||
            integer.matches(value) || decimal.matches(value) || value.any { it in "\"\\\n\r\t" } ||
            value.startsWith("#")

    private fun escape(value: String): String = buildString {
        value.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(character)
            }
        }
    }

    private fun unescape(value: String): String = buildString {
        var index = 0
        while (index < value.length) {
            val character = value[index++]
            if (character != '\\') {
                append(character)
                continue
            }
            if (index >= value.length) throw PresetFormatException("文本值以反斜杠结尾")
            when (val escaped = value[index++]) {
                '\\' -> append('\\')
                '"' -> append('"')
                'n' -> append('\n')
                'r' -> append('\r')
                't' -> append('\t')
                else -> throw PresetFormatException("未知转义: \\$escaped")
            }
        }
    }
}
