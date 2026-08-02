package org.beobma.mafia42discordproject.job.ability

import org.beobma.mafia42discordproject.job.evil.list.Godfather
import org.beobma.mafia42discordproject.job.evil.list.Witch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AbilityManagerTest {
    @Test
    fun `godfather can select discipline`() {
        AbilityManager.registerAll()

        val discipline = AbilityManager.getAvailableExtraAbilitiesFor(Godfather())
            .single { it.name == "규율" }

        assertTrue(discipline.image.endsWith("/godfather_ability_discipline.webp"))
    }

    @Test
    fun `witch can select hallucination`() {
        AbilityManager.registerAll()

        val hallucination = AbilityManager.getAvailableExtraAbilitiesFor(Witch())
            .single { it.name == "환각" }

        assertEquals(
            "마녀의 저주를 받아 개구리가 된 대상이 시민팀이라면 개구리 대신 마피아로 표시된다.",
            hallucination.description
        )
        assertTrue(hallucination.image.endsWith("/witch_ability_hallucination.webp"))
    }
}
