package org.beobma.mafia42discordproject.command

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CommandRegistryTest {
    @Test
    fun allKeepsRegisteredOrder() {
        val commands = CommandRegistry.all()

        assertTrue(commands.isNotEmpty())
        assertSame(PingCommand, commands.first())
        assertTrue(commands.contains(JobInfoImageCommand))
    }

    @Test
    fun findLooksUpCommandNamesCaseInsensitivelyAndTrimsWhitespace() {
        assertSame(PingCommand, CommandRegistry.find("ping"))
        assertSame(PingCommand, CommandRegistry.find(" PING "))
    }

    @Test
    fun findLooksUpAliasesCaseInsensitively() {
        assertSame(BestJobCommand, CommandRegistry.find("bestjob"))
        assertSame(BestJobCommand, CommandRegistry.find(" BESTJOB "))
        assertSame(ReadyCommand, CommandRegistry.find("준비"))
        assertSame(SpectateCommand, CommandRegistry.find("관전"))
        assertSame(JobPreferenceCommand, CommandRegistry.find("선호직업"))
        assertSame(JobPreferenceCommand, CommandRegistry.find("직업선호"))
        assertSame(PreferencePresetCommand, CommandRegistry.find("preset"))
        assertSame(PreferencePresetCommand, CommandRegistry.find("프리셋"))
    }

    @Test
    fun findReturnsNullForUnknownCommand() {
        assertNull(CommandRegistry.find("not-a-command"))
        assertNotNull(CommandRegistry.find("ping"))
    }
}
