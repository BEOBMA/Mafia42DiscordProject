package org.beobma.mafia42discordproject.command

object CommandRegistry {
    private val registeredCommands: List<DiscordCommand> = listOf(
        PingCommand,
        PlayCommand,
        HelloCommand,
        ReadyCommand,
        SpectateCommand,
        RefreshLobbyCommand,
        GameStartCommand,
        GameStopCommand,
        AbilityUseCommand,
        DayTimeAdjustCommand,
        BestJobCommand,
        JobPreferenceCommand,
        JobPreferenceStatusCommand,
        JobAssignmentSimulationCommand,
        GameStatisticsCommand,
        GameStatisticsImageCommand,
        JobInfoImageCommand,
        DebugCommand,
        ShamanRelayCommand,
        ShamanedRelayCommand,
        MegaphoneCommand,
        SecretLetterCommand,
        WillCommand,
        PerjuryCommand,
        PasswordCommand,
    )
    private val commandsByName: Map<String, DiscordCommand> = buildMap {
        registeredCommands.forEach { command ->
            register(command.name, command)
            command.aliases.forEach { alias ->
                register(alias, command)
            }
        }
    }

    fun all(): List<DiscordCommand> = registeredCommands

    fun find(name: String): DiscordCommand? {
        val normalizedName = name.trim().lowercase()
        return commandsByName[normalizedName]
    }

    private fun MutableMap<String, DiscordCommand>.register(rawName: String, command: DiscordCommand) {
        val normalizedName = rawName.trim().lowercase()
        require(normalizedName.isNotBlank()) {
            "Blank command name or alias is not allowed for /${command.name}"
        }

        val existing = putIfAbsent(normalizedName, command)
        require(existing == null || existing == command) {
            "Duplicate command name or alias '$normalizedName' for /${existing?.name} and /${command.name}"
        }
    }
}
