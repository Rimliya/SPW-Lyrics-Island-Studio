package dev.gaboron.spwstudio.font

import java.awt.Font
import java.awt.GraphicsEnvironment
import java.io.InputStream
import java.nio.file.Path
import java.util.zip.ZipFile

data class HostFontFace(
    val family: String,
    val styleName: String,
    val source: String,
    val font: Font,
) {
    val key: String get() = "$family\u0000${font.isBold}\u0000${font.isItalic}"
}

/**
 * Reads fonts from the host JAR without touching the global font table.
 * Holding [Font] instances keeps host MiSans distinct from a system-installed MiSans.
 */
class HostFontCatalog {
    @Volatile
    private var loadedFaces: List<HostFontFace> = emptyList()

    @Volatile
    var lastFailureReason: String? = null
        private set

    fun faces(): List<HostFontFace> = loadedFaces

    fun families(): List<String> = loadedFaces.map(HostFontFace::family).distinct().sortedBy { it.lowercase() }

    fun resolve(family: String): Font? = loadedFaces.firstOrNull { it.family.equals(family, ignoreCase = true) }?.font

    fun loadFromJar(jar: Path): List<HostFontFace> = load(HostFontJarIndex.scanJar(jar), jar.toString())

    fun loadFromDirectory(directory: Path): List<HostFontFace> =
        load(HostFontJarIndex.scanDirectory(directory), directory.toString())

    fun load(entries: List<HostFontEntry>, source: String): List<HostFontFace> {
        val faces = LinkedHashMap<String, HostFontFace>()
        val failures = mutableListOf<String>()
        entries.forEach { entry ->
            runCatching { readFaces(source, entry) }
                .onSuccess { loaded -> loaded.forEach { face -> faces.putIfAbsent(face.key, face) } }
                .onFailure { failure -> failures += "${entry.path}: ${failure.message ?: failure::class.simpleName}" }
        }
        loadedFaces = faces.values.toList()
        lastFailureReason = failures.takeIf { it.isNotEmpty() }?.joinToString("; ")
        return loadedFaces
    }

    private fun readFaces(source: String, entry: HostFontEntry): List<HostFontFace> {
        if (source.endsWith(".jar", ignoreCase = true)) {
            ZipFile(Path.of(source).toFile()).use { zip ->
                val zipEntry = zip.getEntry(entry.path) ?: return emptyList()
                return zip.getInputStream(zipEntry).use { stream -> createFaces(stream, "$source!/${entry.path}") }
            }
        }
        val path = Path.of(source).resolve(entry.path)
        return path.toFile().inputStream().use { stream -> createFaces(stream, path.toString()) }
    }

    private fun createFaces(stream: InputStream, source: String): List<HostFontFace> =
        Font.createFonts(stream).map { font ->
            HostFontFace(
                family = font.family,
                styleName = buildString {
                    append(if (font.isBold) "Bold" else "Regular")
                    if (font.isItalic) append(" Italic")
                },
                source = source,
                font = font,
            )
        }

    companion object {
        fun systemFamilies(): List<String> =
            GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames.sortedBy { it.lowercase() }
    }
}
