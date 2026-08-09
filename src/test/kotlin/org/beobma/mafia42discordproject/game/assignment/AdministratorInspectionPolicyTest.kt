package org.beobma.mafia42discordproject.game.assignment

import org.beobma.mafia42discordproject.game.GameManager
import org.beobma.mafia42discordproject.game.mode.GameStartMode
import org.beobma.mafia42discordproject.job.definition.list.Administrator
import org.beobma.mafia42discordproject.job.definition.list.Doctor
import org.beobma.mafia42discordproject.job.definition.list.MentalPatient
import org.beobma.mafia42discordproject.job.definition.list.Police
import org.beobma.mafia42discordproject.job.definition.list.Reporter
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import kotlin.test.Test
import kotlin.test.assertEquals

class AdministratorInspectionPolicyTest {
    @Test
    fun excludesJobsWhoseExactPresenceIsGuaranteed() {
        val candidates = AdministratorInspectionPolicy.candidates(
            jobs = sequenceOf(Mafia(), Doctor(), Police(), Reporter(), Administrator(), MentalPatient()),
            requiredRoleCounts = GameManager.resolveRequiredRoleCounts(
                playerCount = 8,
                mode = GameStartMode.NORMAL
            )
        )

        assertEquals(setOf("경찰", "기자"), candidates.map { it.name }.toSet())
    }

    @Test
    fun keepsDoctorWhenTheCurrentRulesDoNotGuaranteeOne() {
        val candidates = AdministratorInspectionPolicy.candidates(
            jobs = sequenceOf(Doctor(), Reporter()),
            requiredRoleCounts = GameManager.resolveRequiredRoleCounts(
                playerCount = 7,
                mode = GameStartMode.NORMAL
            )
        )

        assertEquals(setOf("의사", "기자"), candidates.map { it.name }.toSet())
    }

    @Test
    fun excludesDoctorInSevenPlayerMadnessMode() {
        val candidates = AdministratorInspectionPolicy.candidates(
            jobs = sequenceOf(Doctor(), Reporter()),
            requiredRoleCounts = GameManager.resolveRequiredRoleCounts(
                playerCount = 7,
                mode = GameStartMode.MADNESS
            )
        )

        assertEquals(listOf("기자"), candidates.map { it.name })
    }
}
