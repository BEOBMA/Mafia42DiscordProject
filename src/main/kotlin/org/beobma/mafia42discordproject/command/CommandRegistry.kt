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

    private val commandAliasesByName: Map<String, List<String>> = mapOf(
        "ping" to listOf("핑"),
        "hello" to listOf("안녕"),
        "gamestart" to listOf("게임시작"),
        "gamestop" to listOf("게임종료"),
        "use" to listOf("사용"),
        "daytime" to listOf("낮시간"),
        "jobpreference" to listOf("직업선호"),
        "jobpreference-status" to listOf("직업선호현황"),
        "debug" to listOf("디버그"),
        "shaman-relay" to listOf("샤먼중계"),
        "spirit-relay" to listOf("영혼중계"),
        "megaphone" to listOf("확성기"),
        "secret-letter" to listOf("밀서"),
        "will" to listOf("유언"),
        "perjury" to listOf("위증"),
        "password" to listOf("비밀번호")
    )

    private val lookupTable: Map<String, DiscordCommand> = buildMap {
        registeredCommands.forEach { command ->
            put(command.name.lowercase(), command)
            commandAliasesByName[command.name].orEmpty().forEach { alias ->
                put(alias.lowercase(), command)
            }
        }
    }

    fun all(): List<DiscordCommand> = registeredCommands

    fun allNames(command: DiscordCommand): List<String> =
        listOf(command.name) + commandAliasesByName[command.name].orEmpty()

    fun find(name: String): DiscordCommand? = lookupTable[name.lowercase()]
}
