package dev.gaboron.spwstudio.preset

import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Clock
import java.time.format.DateTimeFormatter

class PresetStore(
    root: Path,
    builtIns: List<Preset> = emptyList(),
    private val knownKeys: Set<String>? = null,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val userDirectory = root.resolve("presets")
    private val deletedDirectory = userDirectory.resolve("_deleted")
    private val builtInPresets = builtIns.associateBy { it.name }

    init {
        Files.createDirectories(userDirectory)
        Files.createDirectories(deletedDirectory)
    }

    @Synchronized
    fun list(): List<Preset> {
        problems.clear()
        val users = LinkedHashMap<String, Preset>()
        Files.list(userDirectory).use { paths ->
            paths.filter { Files.isRegularFile(it) }
                .sorted()
                .forEach { path ->
                    val name = path.fileName.toString().substringBeforeLast('.')
                    if (name.startsWith("_")) return@forEach
                    val parsed = runCatching { readFile(path, name) }
                        .onFailure { problems += PresetProblem(path, it.message ?: it::class.simpleName.orEmpty()) }
                        .getOrNull() ?: return@forEach
                    val previous = users.putIfAbsent(name, parsed)
                    if (previous != null && path.fileName.toString().endsWith(".json", ignoreCase = true)) {
                        users[name] = parsed
                    }
                }
        }
        return (builtInPresets.values + users.values).sortedBy { it.name.lowercase() }
    }

    private val problems = mutableListOf<PresetProblem>()

    @Synchronized
    fun problems(): List<PresetProblem> = problems.toList()

    @Synchronized
    fun load(name: String): Preset? = builtInPresets[name] ?: readUser(name)

    @Synchronized
    fun save(preset: Preset, overwrite: Boolean = true): Path {
        validateName(preset.name)
        if (builtInPresets.containsKey(preset.name)) throw PresetProtectedException("内置预设不能覆盖: ${preset.name}")
        val target = userDirectory.resolve("${preset.name}.json")
        if (Files.exists(target) && !overwrite) throw PresetExistsException("预设已存在: ${preset.name}")
        writeAtomically(target, PresetCodec.encodeJson(preset))
        return target
    }

    @Synchronized
    fun delete(name: String): Boolean {
        if (builtInPresets.containsKey(name)) throw PresetProtectedException("内置预设不能删除: $name")
        val source = userFile(name) ?: return false
        val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(java.time.LocalDateTime.now(clock))
        val target = deletedDirectory.resolve("$name-$stamp.${source.fileName.toString().substringAfterLast('.')}")
        moveAtomically(source, target)
        return true
    }

    @Synchronized
    fun rename(oldName: String, newName: String): Boolean {
        if (builtInPresets.containsKey(oldName)) throw PresetProtectedException("内置预设不能重命名: $oldName")
        validateName(newName)
        if (builtInPresets.containsKey(newName) || userFile(newName) != null) {
            throw PresetExistsException("预设已存在: $newName")
        }
        val source = userFile(oldName) ?: return false
        val target = userDirectory.resolve("$newName.json")
        val preset = readFile(source, oldName)
        writeAtomically(target, PresetCodec.encodeJson(preset.copy(name = newName)))
        Files.deleteIfExists(source)
        return true
    }

    @Synchronized
    fun previewImport(file: Path, current: Map<String, PresetValue>): ImportPreview {
        val preset = readExternal(file)
        val unknown = knownKeys?.let { keys -> preset.values.keys.filterNot(keys::contains).toSet() }.orEmpty()
        val conflict = builtInPresets.containsKey(preset.name) || userFile(preset.name) != null
        return ImportPreview(preset, preset.diff(current), unknown, conflict)
    }

    @Synchronized
    fun applyImport(file: Path, current: Map<String, PresetValue>, overwrite: Boolean = false): ImportPreview {
        val preview = previewImport(file, current)
        save(preview.preset, overwrite)
        return preview
    }

    @Synchronized
    fun export(name: String, target: Path) {
        val preset = load(name) ?: throw PresetNotFoundException("预设不存在: $name")
        val format = PresetFormat.detect(target)
        writeAtomically(target, PresetCodec.encode(preset, format))
    }

    private fun readUser(name: String): Preset? = userFile(name)?.let { readFile(it, name) }

    private fun userFile(name: String): Path? {
        val json = userDirectory.resolve("$name.json")
        if (Files.isRegularFile(json)) return json
        val txt = userDirectory.resolve("$name.txt")
        if (Files.isRegularFile(txt)) return txt
        return null
    }

    private fun readFile(path: Path, name: String): Preset =
        PresetCodec.decode(Files.readString(path, StandardCharsets.UTF_8), PresetFormat.detect(path), name)

    private fun readExternal(path: Path): Preset {
        val text = Files.readString(path, StandardCharsets.UTF_8)
        val format = if (path.fileName.toString().substringAfterLast('.', "").isEmpty()) {
            if (text.trimStart().startsWith("{")) PresetFormat.JSON else PresetFormat.TXT
        } else {
            PresetFormat.detect(path)
        }
        return PresetCodec.decode(text, format, path.fileName.toString().substringBeforeLast('.'))
    }

    private fun validateName(name: String) {
        require(name.isNotBlank()) { "预设名不能为空" }
        require(name == name.trim()) { "预设名不能以空白开头或结尾" }
        require(name.length <= 80) { "预设名过长" }
        require(!name.startsWith("_")) { "预设名不能以 _ 开头" }
        require(name.none { it in "\\/:*?\"<>|" }) { "预设名不能包含 \\ / : * ? \" < > |" }
        require(name != "." && name != "..") { "预设名无效" }
        require(name.substringBefore('.').uppercase() !in RESERVED_NAMES) { "预设名是 Windows 保留名" }
    }

    private fun writeAtomically(target: Path, content: String) {
        Files.createDirectories(target.parent)
        val temporary = Files.createTempFile(target.parent, "preset", ".tmp")
        try {
            Files.writeString(temporary, content, StandardCharsets.UTF_8)
            moveAtomically(temporary, target)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private fun moveAtomically(source: Path, target: Path) {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private companion object {
        val RESERVED_NAMES = (listOf("CON", "PRN", "AUX", "NUL") + (1..9).flatMap { listOf("COM$it", "LPT$it") }).toSet()
    }
}

data class PresetProblem(val file: Path, val message: String)

data class ImportPreview(
    val preset: Preset,
    val diff: PresetDiff,
    val unknownKeys: Set<String>,
    val nameConflict: Boolean,
)

open class PresetStoreException(message: String) : IllegalStateException(message)
class PresetProtectedException(message: String) : PresetStoreException(message)
class PresetExistsException(message: String) : PresetStoreException(message)
class PresetNotFoundException(message: String) : PresetStoreException(message)
