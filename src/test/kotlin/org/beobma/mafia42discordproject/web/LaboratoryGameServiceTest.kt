package org.beobma.mafia42discordproject.web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LaboratoryGameServiceTest {
    private val catalog = listOf(
        job("마피아", ability("처형", LaboratoryPhase.NIGHT, requiresTarget = true)),
        job("의사", ability("치료", LaboratoryPhase.NIGHT, requiresTarget = true)),
        job("공무원", ability("조회", LaboratoryPhase.NIGHT, requiresJobSelection = true)),
        job("시민")
    )

    @Test
    fun controlsBotAbilitiesAndResolvesCoreNightActions() {
        val service = LaboratoryGameService(catalogProvider = { catalog })
        val created = service.createSession()
        val token = created.token
        val setup = listOf(
            LaboratoryPlayerSetup("나", "시민"),
            LaboratoryPlayerSetup("마피아 봇", "마피아"),
            LaboratoryPlayerSetup("의사 봇", "의사"),
            LaboratoryPlayerSetup("대상 봇", "시민")
        )

        assertTrue(service.updateSetup(token, setup).success)
        assertTrue(service.start(token).success)
        assertTrue(service.submitAction(token, "bot-1", "처형", "bot-3", null).success)
        assertTrue(service.submitAction(token, "bot-2", "치료", "bot-3", null).success)

        val day = service.advance(token)
        assertTrue(day.success)
        val snapshot = assertNotNull(day.snapshot)
        assertEquals(LaboratoryPhase.DAY, snapshot.phase)
        assertTrue(snapshot.players.first { it.id == "bot-3" }.isAlive)
        assertTrue(snapshot.events.any { it.body.contains("처형을 피했습니다") })
    }

    @Test
    fun manuallyControlsEveryVoteAndProsConsDecision() {
        val service = LaboratoryGameService(catalogProvider = { catalog })
        val token = service.createSession().token
        val setup = listOf(
            LaboratoryPlayerSetup("나", "시민"),
            LaboratoryPlayerSetup("봇 1", "마피아"),
            LaboratoryPlayerSetup("봇 2", "의사"),
            LaboratoryPlayerSetup("봇 3", "시민")
        )
        service.updateSetup(token, setup)
        service.start(token)
        service.advance(token)
        service.advance(token)

        listOf("human", "bot-1", "bot-2", "bot-3").forEach { voterId ->
            assertTrue(service.castMainVote(token, voterId, "bot-3").success)
        }
        val defense = service.advance(token)
        assertEquals(LaboratoryPhase.PROS_CONS, defense.snapshot?.phase)
        assertEquals("bot-3", defense.snapshot?.defenseTargetId)

        assertTrue(service.castProsConsVote(token, "human", true).success)
        assertTrue(service.castProsConsVote(token, "bot-1", true).success)
        assertTrue(service.castProsConsVote(token, "bot-2", true).success)
        assertTrue(service.castProsConsVote(token, "bot-3", false).success)
        val nextNight = service.advance(token)

        assertEquals(LaboratoryPhase.NIGHT, nextNight.snapshot?.phase)
        assertEquals(2, nextNight.snapshot?.dayCount)
        assertFalse(nextNight.snapshot?.players?.first { it.id == "bot-3" }?.isAlive ?: true)
    }

    @Test
    fun administratorUsesJobSelectionWithoutPlayerTarget() {
        val service = LaboratoryGameService(catalogProvider = { catalog })
        val token = service.createSession().token
        service.updateSetup(
            token,
            listOf(
                LaboratoryPlayerSetup("나", "공무원"),
                LaboratoryPlayerSetup("봇 1", "마피아"),
                LaboratoryPlayerSetup("봇 2", "의사"),
                LaboratoryPlayerSetup("봇 3", "시민")
            )
        )
        service.start(token)

        val result = service.submitAction(token, "human", "조회", null, "마피아")
        assertTrue(result.success)
        val action = assertNotNull(result.snapshot?.actions?.single())
        assertEquals(null, action.targetId)
        assertEquals("마피아", action.selectedJobName)
    }

    private fun job(name: String, vararg abilities: LaboratoryAbilityDefinition) = LaboratoryJobDefinition(
        name = name,
        description = "$name 설명",
        image = null,
        isEvil = name == "마피아",
        abilities = abilities.toList()
    )

    private fun ability(
        name: String,
        phase: LaboratoryPhase,
        requiresTarget: Boolean = false,
        requiresJobSelection: Boolean = false
    ) = LaboratoryAbilityDefinition(
        name = name,
        description = "$name 설명",
        image = "",
        phase = phase,
        requiresTarget = requiresTarget,
        requiresJobSelection = requiresJobSelection
    )
}
