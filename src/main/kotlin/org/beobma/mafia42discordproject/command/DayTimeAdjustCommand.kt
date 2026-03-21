package org.beobma.mafia42discordproject.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.interaction.string
import org.beobma.mafia42discordproject.discord.DiscordMessageManager
import org.beobma.mafia42discordproject.game.GameManager
import org.beobma.mafia42discordproject.game.GameLoopManager

object DayTimeAdjustCommand : DiscordCommand {
    override val name: String = "daytime"
    override val description: String = "낮 시간 15초 증가/감소 (하루 1회)"

    private const val actionOptionName = "action"
    private const val increaseValue = "increase"
    private const val decreaseValue = "decrease"

    override suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent) {
        val interaction = event.interaction
        val game = GameManager.getCurrentGameFor(interaction.user.id)
        if (game == null) {
            DiscordMessageManager.respondEphemeral(event, "현재 진행 중인 게임 참가자만 사용할 수 있습니다.")
            return
        }

        val action = interaction.command.strings[actionOptionName]
        if (action == null) {
            DiscordMessageManager.respondEphemeral(event, "사용법: /daytime action:<increase|decrease>")
            return
        }

        val result = when (action) {
            increaseValue -> GameLoopManager.adjustDayTimeByPlayer(game, interaction.user.id, isIncrease = true)
            decreaseValue -> GameLoopManager.adjustDayTimeByPlayer(game, interaction.user.id, isIncrease = false)
            else -> GameLoopManager.DayTimeAdjustmentResult(false, "잘못된 action 값입니다.")
        }

        DiscordMessageManager.respondEphemeral(event, result.message)
    }

    override suspend fun handleMessage(event: MessageCreateEvent, args: List<String>) {
        val game = event.member?.id?.let(GameManager::getCurrentGameFor)
        if (game == null) {
            event.message.channel.createMessage("현재 진행 중인 게임 참가자만 사용할 수 있습니다.")
            return
        }

        val actorId = event.member?.id
        if (actorId == null) {
            event.message.channel.createMessage("길드 멤버만 사용할 수 있습니다.")
            return
        }

        val action = args.firstOrNull()?.lowercase()
        if (action !in setOf("up", "down", increaseValue, decreaseValue)) {
            event.message.channel.createMessage("사용법: !daytime <up|down>")
            return
        }

        val isIncrease = action == "up" || action == increaseValue
        val result = GameLoopManager.adjustDayTimeByPlayer(game, actorId, isIncrease)
        event.message.channel.createMessage(result.message)
    }

    private fun dev.kord.rest.builder.interaction.ChatInputCreateBuilder.registerOptions() {
        string(actionOptionName, "increase: +15초, decrease: -15초") {
            required = true
            choice("increase (+15초)", increaseValue)
            choice("decrease (-15초)", decreaseValue)
        }
    }

    override suspend fun registerGlobal(kord: Kord) {
        kord.createGlobalChatInputCommand(name, description) {
            registerOptions()
        }
    }

    override suspend fun registerGuild(kord: Kord, guildId: Snowflake) {
        kord.createGuildChatInputCommand(guildId, name, description) {
            registerOptions()
        }
    }
}
