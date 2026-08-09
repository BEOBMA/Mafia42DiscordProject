package org.beobma.mafia42discordproject.game

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.beobma.mafia42discordproject.util.AtomicTextFileWriter
import java.nio.file.Files
import java.nio.file.Path

object MafiaExecutionProtectionManager {
    private val store = MafiaExecutionProtectionStore(
        storagePath = Path.of("data", "mafia-execution-protected-targets.json")
    )

    fun load() = store.load()

    fun record(guildId: ULong, targetId: ULong) = store.record(guildId, targetId)

    fun consume(guildId: ULong): ULong? = store.consume(guildId)
}

internal class MafiaExecutionProtectionStore(
    private val storagePath: Path
) {
    private val protectedTargetByGuildId: MutableMap<ULong, ULong> = mutableMapOf()
    private val json = Json { prettyPrint = true }

    @Synchronized
    fun record(guildId: ULong, targetId: ULong) {
        val updated = protectedTargetByGuildId.toMutableMap().apply {
            this[guildId] = targetId
        }
        persist(updated)
        protectedTargetByGuildId.clear()
        protectedTargetByGuildId.putAll(updated)
    }

    @Synchronized
    fun consume(guildId: ULong): ULong? {
        val targetId = protectedTargetByGuildId[guildId] ?: return null
        val updated = protectedTargetByGuildId.toMutableMap().apply {
            remove(guildId)
        }
        persist(updated)
        protectedTargetByGuildId.clear()
        protectedTargetByGuildId.putAll(updated)
        return targetId
    }

    @Synchronized
    fun load() {
        if (!Files.exists(storagePath)) return

        val raw = Files.readString(storagePath)
        val root = runCatching { json.parseToJsonElement(raw) }
            .getOrElse {
                println("[MafiaExecutionProtectionManager] failed to parse storage file: ${it.message}")
                return
            }
        val jsonObject = root as? JsonObject ?: return
        val loaded = mutableMapOf<ULong, ULong>()

        jsonObject.forEach { (guildIdText, targetElement) ->
            val guildId = guildIdText.toULongOrNull()
            val targetId = targetElement.jsonPrimitive.contentOrNull?.toULongOrNull()
            if (guildId == null || targetId == null) {
                println("[MafiaExecutionProtectionManager] skipping invalid entry: $guildIdText")
                return@forEach
            }
            loaded[guildId] = targetId
        }

        protectedTargetByGuildId.clear()
        protectedTargetByGuildId.putAll(loaded)
        println("[MafiaExecutionProtectionManager] loaded ${loaded.size} protected target entries.")
    }

    private fun persist(snapshot: Map<ULong, ULong>) {
        val root = buildJsonObject {
            snapshot.entries
                .sortedBy { it.key }
                .forEach { (guildId, targetId) ->
                    put(guildId.toString(), JsonPrimitive(targetId.toString()))
                }
        }
        AtomicTextFileWriter.write(storagePath, json.encodeToString(JsonObject.serializer(), root))
    }
}
