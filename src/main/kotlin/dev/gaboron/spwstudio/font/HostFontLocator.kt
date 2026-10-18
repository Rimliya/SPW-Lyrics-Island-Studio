package dev.gaboron.spwstudio.font

import java.io.File
import java.nio.file.Files
import java.nio.file.Path

object HostFontLocator {
    private val hostHints = listOf("salt", "spw", "player")

    fun candidates(explicit: List<Path> = emptyList()): List<Path> {
        val result = LinkedHashSet<Path>()
        explicit.filterTo(result) { Files.isRegularFile(it) }
        System.getProperty("spw.host.jar")?.takeIf(String::isNotBlank)?.let { Path.of(it) }
            ?.takeIf { Files.isRegularFile(it) }?.let(result::add)
        System.getProperty("java.class.path").orEmpty()
            .split(File.pathSeparatorChar)
            .asSequence()
            .filter { it.endsWith(".jar", ignoreCase = true) }
            .map(::Path)
            .filter { path ->
                val name = path.fileName.toString().lowercase()
                hostHints.any(name::contains)
            }
            .filter { Files.isRegularFile(it) }
            .forEach(result::add)
        return result.toList()
    }
}
