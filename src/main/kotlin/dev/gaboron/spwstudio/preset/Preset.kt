package dev.gaboron.spwstudio.preset

data class Preset(
    val name: String,
    val values: Map<String, PresetValue>,
) {
    init {
        require(name.isNotBlank()) { "预设名不能为空" }
    }

    /** Compares this preset with the current values. Missing current keys are additions, not removals. */
    fun diff(current: Map<String, PresetValue>): PresetDiff {
        val added = LinkedHashMap<String, PresetValue>()
        val changed = LinkedHashMap<String, PresetValueChange>()
        values.forEach { (key, target) ->
            val source = current[key]
            when {
                source == null -> added[key] = target
                source != target -> changed[key] = PresetValueChange(source, target)
            }
        }
        return PresetDiff(
            added = added,
            changed = changed,
            unchanged = values.keys.count { current[it] == values[it] },
            missingFromPreset = current.keys.filterNot(values::containsKey).toSet(),
        )
    }

    fun applyTo(current: Map<String, PresetValue>, knownKeys: Set<String>? = null): Map<String, PresetValue> {
        val result = LinkedHashMap(current)
        values.forEach { (key, value) ->
            if (knownKeys == null || key in knownKeys) result[key] = value
        }
        return result
    }
}

data class PresetValueChange(val from: PresetValue, val to: PresetValue)

data class PresetDiff(
    val added: Map<String, PresetValue>,
    val changed: Map<String, PresetValueChange>,
    val unchanged: Int,
    val missingFromPreset: Set<String>,
) {
    val totalChanges: Int get() = added.size + changed.size
    val isEmpty: Boolean get() = totalChanges == 0
}
