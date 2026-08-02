package org.beobma.mafia42discordproject.game.system

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PrivateJobKnowledgePolicyTest {
    @Test
    fun `mutual identity abilities reveal the discoverer job to the target`() {
        assertEquals("형사", PrivateJobKnowledgePolicy.revealedDiscovererJobName("수사"))
        assertEquals("해커", PrivateJobKnowledgePolicy.revealedDiscovererJobName("해킹"))
        assertEquals("도굴꾼", PrivateJobKnowledgePolicy.revealedDiscovererJobName("도굴"))
    }

    @Test
    fun `ordinary discovery does not reveal the discoverer job`() {
        assertNull(PrivateJobKnowledgePolicy.revealedDiscovererJobName("특종"))
        assertNull(PrivateJobKnowledgePolicy.revealedDiscovererJobName(null))
    }
}
