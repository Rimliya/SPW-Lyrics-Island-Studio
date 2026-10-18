package dev.gaboron.spwstudio.preset

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class PresetStoreTest {
    @TempDir
    lateinit var directory: Path

    private val builtIn = Preset("内置", mapOf("a" to PresetValue.IntValue(0)))
    private val knownKeys = setOf("a", "b", "c")

    @Test
    fun `user presets coexist with built ins and built ins are protected`() {
        val store = PresetStore(directory, listOf(builtIn), knownKeys)
        store.save(Preset("我的", mapOf("a" to PresetValue.IntValue(1))))

        assertEquals(listOf("内置", "我的"), store.list().map { it.name })
        assertThrows(PresetProtectedException::class.java) {
            store.delete("内置")
        }
        assertThrows(PresetProtectedException::class.java) {
            store.save(builtIn, overwrite = true)
        }
    }

    @Test
    fun `delete moves the preset into the recoverable directory`() {
        val store = PresetStore(directory, listOf(builtIn), knownKeys)
        store.save(Preset("临时", mapOf("a" to PresetValue.IntValue(1))))

        assertTrue(store.delete("临时"))
        assertFalse(store.list().any { it.name == "临时" })
        val deleted = Files.list(directory.resolve("presets/_deleted")).use { it.toList() }
        assertEquals(1, deleted.size)
    }

    @Test
    fun `rename preserves values and refuses duplicates`() {
        val store = PresetStore(directory, listOf(builtIn), knownKeys)
        store.save(Preset("旧名", mapOf("a" to PresetValue.IntValue(3))))
        store.save(Preset("已存在", mapOf("a" to PresetValue.IntValue(4))))

        assertTrue(store.rename("旧名", "新名"))
        assertEquals(PresetValue.IntValue(3), store.load("新名")?.values?.get("a"))
        assertThrows(PresetExistsException::class.java) {
            store.rename("新名", "已存在")
        }
    }

    @Test
    fun `import reports changes and unknown keys without applying them`() {
        val store = PresetStore(directory, listOf(builtIn), knownKeys)
        val file = directory.resolve("导入.json")
        Files.writeString(
            file,
            """{"version":1,"name":"导入","values":{"a":2,"b":true,"unknown":9}}""",
        )
        val preview = store.previewImport(file, mapOf("a" to PresetValue.IntValue(1), "c" to PresetValue.IntValue(5)))

        assertEquals(setOf("unknown"), preview.unknownKeys)
        assertEquals(PresetValue.IntValue(2), preview.diff.changed["a"]?.to)
        assertEquals(PresetValue.Bool(true), preview.diff.added["b"])
        val applied = preview.preset.applyTo(mapOf("c" to PresetValue.IntValue(5)), knownKeys)
        assertEquals(PresetValue.IntValue(2), applied["a"])
        assertEquals(PresetValue.Bool(true), applied["b"])
        assertFalse(applied.containsKey("unknown"))
        assertEquals(PresetValue.IntValue(5), applied["c"])
    }

    @Test
    fun `invalid names and invalid files are handled`() {
        val store = PresetStore(directory, listOf(builtIn), knownKeys)
        assertThrows(IllegalArgumentException::class.java) {
            store.save(Preset("../evil", emptyMap()))
        }
        Files.writeString(directory.resolve("presets/broken.json"), "{not json")
        assertTrue(store.list().none { it.name == "broken" })
        assertNotNull(store.problems().singleOrNull())
    }
}
