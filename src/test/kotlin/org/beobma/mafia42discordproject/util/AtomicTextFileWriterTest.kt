package org.beobma.mafia42discordproject.util

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AtomicTextFileWriterTest {
    @Test
    fun writeCreatesParentDirectoriesAndOverwritesExistingFile() {
        val outputPath = Files.createTempDirectory("atomic-writer-test")
            .resolve("nested")
            .resolve("value.txt")

        AtomicTextFileWriter.write(outputPath, "first")
        AtomicTextFileWriter.write(outputPath, "second")

        assertEquals("second", Files.readString(outputPath))
        assertFalse(hasTempSiblings(outputPath.parent, ".value.txt."))
    }

    @Test
    fun writeRemovesTempFileWhenMoveFails() {
        val directory = Files.createTempDirectory("atomic-writer-failure-test")
        val target = directory.resolve("target.txt")
        Files.createDirectory(target)
        Files.writeString(target.resolve("child.txt"), "kept")

        assertFailsWith<Exception> {
            AtomicTextFileWriter.write(target, "replacement")
        }

        assertTrue(Files.isDirectory(target))
        assertEquals("kept", Files.readString(target.resolve("child.txt")))
        assertFalse(hasTempSiblings(directory, ".target.txt."))
    }

    private fun hasTempSiblings(directory: Path, prefix: String): Boolean {
        Files.newDirectoryStream(directory).use { entries ->
            return entries.any { path ->
                val name = path.fileName.toString()
                name.startsWith(prefix) && name.endsWith(".tmp")
            }
        }
    }
}
