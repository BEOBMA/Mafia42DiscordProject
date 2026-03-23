package org.beobma.mafia42discordproject.command

object CommandRegistry {
    private val registeredCommands: List<DiscordCommand> = listOf(
        PingCommand,
        HelloCommand,
        GameStartCommand,
        GameStopCommand,
        AbilityUseCommand,
        DayTimeAdjustCommand,
        JobPreferenceCommand,
        JobPreferenceStatusCommand,
        DebugCommand,
        ShamanRelayCommand,
        ShamanedRelayCommand,
        MegaphoneCommand,
        SecretLetterCommand,
        WillCommand,
        PerjuryCommand,
        PasswordCommand,
    )

    fun all(): List<DiscordCommand> = registeredCommands

    fun find(name: String): DiscordCommand? {
        val normalizedName = name.lowercase()
        return registeredCommands.firstOrNull { command ->
            command.name == normalizedName || command.aliases.contains(normalizedName)
        }
    }
}
