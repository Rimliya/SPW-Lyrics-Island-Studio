package dev.gaboron.spwstudio.font

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile

data class HostFontEntry(val path: String, val fileName: String)

object HostFontJarIndex {
    private val extensions = setOf("ttf", "otf", "ttc")

    fun scan(jar: Path): List<HostFontEntry> {
        if (!Files.isRegularFile(jar)) return emptyList()
        return runCatching {
            ZipFile(jar.toFile()).use { zip ->
                zip.entries().asSequence()
                    .filterNot { it.isDirectory }
                    .filter { entry -> entry.name.substringAfterLast('.', "").lowercase() in extensions }
                    .map { entry -> HostFontEntry(entry.name, entry.name.substringAfterLast('/')) }
                    .sortedBy { it.path.lowercase() }
                    .toList()
            }
        }.getOrElse { emptyList() }
    }

    fun scan(directory: Path): List<HostFontEntry> {
        if (!Files.isDirectory(directory)) return emptyList()
        return runCatching {
            Files.walk(directory).use { paths ->
                paths.filter { Files.isRegularFile(it) }
                    .filter { path -> path.fileName.toString().substringAfterLast('.', "").lowercase() in extensions }
                    .map { path -> HostFontEntry(directory.relativize(path).toString(), path.fileName.toString()) }
                    .sorted()
                    .toList()
            }
        }.getOrElse { emptyList() }
    }
}
