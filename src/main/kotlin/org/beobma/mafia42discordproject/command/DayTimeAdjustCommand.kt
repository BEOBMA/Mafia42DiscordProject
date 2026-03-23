package org.beobma.mafia42discordproject.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.interaction.string
import org.beobma.mafia42discordproject.discord.DiscordMessageManager
import org.beobma.mafia42discordproject.discord.DiscordMessageManager.sendMainChannerMessage
import org.beobma.mafia42discordproject.game.GameManager
import org.beobma.mafia42discordproject.game.GameLoopManager

object DayTimeAdjustCommand : DiscordCommand {
    override val name: String = "daytime"
    override val description: String = "낮 시간 15초 증가/감소 (하루 1회)"
    override val koreanName: String = "낮시간"
    override val aliases: Set<String> = setOf("낮시간")

    private const val actionOptionName = "action"
    private const val increaseValue = "increase"
    private const val decreaseValue = "decrease"
    private const val increaseKoValue = "증가"
    private const val decreaseKoValue = "감소"

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

        val isIncrease = when (action) {
            increaseValue, increaseKoValue -> true
            decreaseValue, decreaseKoValue -> false
            else -> null
        }
        val result = when (isIncrease) {
            true -> GameLoopManager.adjustDayTimeByPlayer(game, interaction.user.id, isIncrease = true)
            false -> GameLoopManager.adjustDayTimeByPlayer(game, interaction.user.id, isIncrease = false)
            else -> GameLoopManager.DayTimeAdjustmentResult(false, "잘못된 action 값입니다.")
        }

        DiscordMessageManager.respondEphemeral(event, result.message)
        if (result.isSuccess && isIncrease != null) {
            val playerName = game.getPlayer(interaction.user.id)?.member?.effectiveName ?: interaction.user.username
            val actionText = if (isIncrease) "증가" else "단축"
            game.sendMainChannerMessage("${playerName}님이 시간을 ${actionText}시켰습니다.")
        }
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
        if (action !in setOf("up", "down", increaseValue, decreaseValue, increaseKoValue, decreaseKoValue)) {
            event.message.channel.createMessage("사용법: !daytime <up|down>")
            return
        }

        val isIncrease = action == "up" || action == increaseValue || action == increaseKoValue
        val result = GameLoopManager.adjustDayTimeByPlayer(game, actorId, isIncrease)
        event.message.channel.createMessage(result.message)
        if (result.isSuccess) {
            val playerName = game.getPlayer(actorId)?.member?.effectiveName ?: event.message.author?.username ?: "알 수 없는 플레이어"
            val actionText = if (isIncrease) "증가" else "단축"
            game.sendMainChannerMessage("${playerName}님이 시간을 ${actionText}시켰습니다.")
        }
    }

    private fun dev.kord.rest.builder.interaction.ChatInputCreateBuilder.registerOptions() {
        string(actionOptionName, "increase: +15초, decrease: -15초") {
            required = true
            choice("increase (+15초)", increaseValue)
            choice("decrease (-15초)", decreaseValue)
            choice("증가 (+15초)", increaseKoValue)
            choice("감소 (-15초)", decreaseKoValue)
        }
    }

    override suspend fun registerGlobal(kord: Kord) {
        kord.createGlobalChatInputCommand(name, description) {
            applyKoreanLocalization(this)
            registerOptions()
        }
    }

    override suspend fun registerGuild(kord: Kord, guildId: Snowflake) {
        kord.createGuildChatInputCommand(guildId, name, description) {
            applyKoreanLocalization(this)
            registerOptions()
        }
    }
}
