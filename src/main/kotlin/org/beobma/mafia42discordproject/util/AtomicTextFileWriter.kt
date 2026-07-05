package org.beobma.mafia42discordproject.util

import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

internal object AtomicTextFileWriter {
    fun write(path: Path, content: String) {
        val parent = path.parent
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent)
        }

        val tempDirectory = parent ?: Path.of(".")
        val fileName = path.fileName?.toString()?.takeIf(String::isNotBlank) ?: "atomic-write"
        var tempPath: Path? = null

        try {
            tempPath = Files.createTempFile(tempDirectory, ".$fileName.", ".tmp")
            Files.writeString(tempPath, content)
            moveIntoPlace(tempPath, path)
            tempPath = null
        } finally {
            tempPath?.let { Files.deleteIfExists(it) }
        }
    }

    private fun moveIntoPlace(source: Path, target: Path) {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
