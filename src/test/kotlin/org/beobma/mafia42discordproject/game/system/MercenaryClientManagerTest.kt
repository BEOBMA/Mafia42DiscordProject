package org.beobma.mafia42discordproject.game.system

import org.beobma.mafia42discordproject.job.definition.list.Citizen
import org.beobma.mafia42discordproject.job.definition.list.Hacker
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MercenaryClientManagerTest {
    @Test
    fun `living citizen team player is eligible as a client`() {
        assertTrue(
            MercenaryClientManager.isEligibleClient(
                job = Citizen(),
                isDead = false,
                isSelf = false
            )
        )
    }

    @Test
    fun `dead player and mercenary themself are ineligible as a client`() {
        assertFalse(
            MercenaryClientManager.isEligibleClient(
                job = Citizen(),
                isDead = true,
                isSelf = false
            )
        )
        assertFalse(
            MercenaryClientManager.isEligibleClient(
                job = Citizen(),
                isDead = false,
                isSelf = true
            )
        )
    }

    @Test
    fun `mafia team and frog cursed players are ineligible as a client`() {
        assertFalse(
            MercenaryClientManager.isEligibleClient(
                job = Mafia(),
                isDead = false,
                isSelf = false
            )
        )
        assertFalse(
            MercenaryClientManager.isEligibleClient(
                job = Hacker(),
                isDead = false,
                isSelf = false,
                isFrogCursed = true
            )
        )
    }
}
