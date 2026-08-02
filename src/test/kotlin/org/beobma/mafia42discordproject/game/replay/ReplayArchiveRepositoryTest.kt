package org.beobma.mafia42discordproject.game.replay

import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ReplayArchiveRepositoryTest {
    @TempDir
    lateinit var tempDirectory: Path

    @Test
    fun indexesLegacyReplayAndExposesItByStableUuid() {
        val replayFile = tempDirectory.resolve("legacy-replay.json")
        replayFile.writeText(
            """
            {
              "schemaVersion": 1,
              "generatedAtMillis": 2000,
              "endReason": "WIN_CONDITION_MET",
              "winningTeamName": "시민 팀",
              "guildId": "42",
              "guildName": "테스트 서버",
              "replayStartedAtMillis": 1000,
              "dayCount": 2,
              "initialPlayerCount": 1,
              "players": [{"id":"1","name":"플레이어","jobName":"의사","isDead":false}],
              "logs": [{"sequence":1,"timestampMillis":4500,"dayCount":1,"phase":"DAY","type":"GAME_END"}]
            }
            """.trimIndent()
        )

        val expectedUuid = GameReplayRenderDataStore.replayUuid("42", 1000)
        val listing = ReplayArchiveRepository.list(tempDirectory)
        val summary = listing.single().jsonObject

        assertEquals(expectedUuid, summary["replayUuid"]?.jsonPrimitive?.content)
        assertEquals("3500", summary["durationMillis"]?.jsonPrimitive?.content)
        assertEquals("플레이어", summary["players"]?.jsonArray?.single()?.jsonObject?.get("name")?.jsonPrimitive?.content)

        val replay = ReplayArchiveRepository.find(expectedUuid, tempDirectory)
        assertNotNull(replay)
        assertEquals(expectedUuid, replay["replayUuid"]?.jsonPrimitive?.content)
        assertTrue(ReplayArchiveRepository.find("../../invalid", tempDirectory) == null)
    }
}
