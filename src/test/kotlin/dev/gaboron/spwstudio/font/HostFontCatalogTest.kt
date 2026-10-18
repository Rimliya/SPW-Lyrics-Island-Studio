package dev.gaboron.spwstudio.font

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

class HostFontCatalogTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `jar index finds font entries case insensitively`() {
        val jar = directory.resolve("host.jar")
        JarOutputStream(Files.newOutputStream(jar)).use { output ->
            listOf("fonts/a.ttf", "fonts/b.otf", "fonts/c.TTC", "readme.txt").forEach { name ->
                output.putNextEntry(JarEntry(name))
                output.write("x".toByteArray())
                output.closeEntry()
            }
        }

        assertEquals(listOf("fonts/a.ttf", "fonts/b.otf", "fonts/c.TTC"), HostFontJarIndex.scanJar(jar).map { it.path })
    }

    @Test
    fun `directory index ignores unrelated files`() {
        Files.createDirectories(directory.resolve("fonts"))
        Files.writeString(directory.resolve("fonts/a.ttf"), "x")
        Files.writeString(directory.resolve("fonts/readme.txt"), "x")

        assertEquals(listOf("fonts/a.ttf"), HostFontJarIndex.scanDirectory(directory).map { it.path.replace('\\', '/') })
    }

    @Test
    fun `invalid fonts are skipped and reported`() {
        val jar = directory.resolve("bad.jar")
        JarOutputStream(Files.newOutputStream(jar)).use { output ->
            output.putNextEntry(JarEntry("bad.ttf"))
            output.write("not a font".toByteArray())
            output.closeEntry()
        }

        val catalog = HostFontCatalog()
        assertTrue(catalog.loadFromJar(jar).isEmpty())
        assertNotNull(catalog.lastFailureReason)
        assertTrue(catalog.lastFailureReason!!.contains("bad.ttf"))
    }
}
