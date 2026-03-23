package org.beobma.mafia42discordproject.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.interaction.string
import org.beobma.mafia42discordproject.discord.DiscordMessageManager
import org.beobma.mafia42discordproject.game.GameManager

object ShamanedRelayCommand : DiscordCommand {
    override val name: String = "spirit-relay"
    override val description: String = "성불된 사망자가 강령 메시지를 전달합니다."
    override val koreanName: String = "강령"
    override val aliases: Set<String> = setOf("강령")
    private const val messageOptionName = "message"

    override suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent) {
        val message = event.interaction.command.strings[messageOptionName].orEmpty()
        val result = GameManager.relayShamanedMessage(
            memberId = event.interaction.user.id,
            channelId = event.interaction.channelId,
            message = message
        )
        DiscordMessageManager.respondEphemeral(event, result.message)
    }

    override suspend fun handleMessage(event: MessageCreateEvent, args: List<String>) {
        val memberId = event.member?.id ?: return
        val result = GameManager.relayShamanedMessage(
            memberId = memberId,
            channelId = event.message.channelId,
            message = args.joinToString(" ")
        )
        event.message.channel.createMessage(result.message)
        if (result.isSuccess) {
            runCatching { event.message.delete("강령 전달 처리") }
        }
    }

    override suspend fun registerGlobal(kord: Kord) {
        kord.createGlobalChatInputCommand(name, description) {
            applyKoreanLocalization(this)
            string(messageOptionName, "강령으로 전달할 메시지") {
                required = true
            }
        }
    }

    override suspend fun registerGuild(kord: Kord, guildId: Snowflake) {
        kord.createGuildChatInputCommand(guildId, name, description) {
            applyKoreanLocalization(this)
            string(messageOptionName, "강령으로 전달할 메시지") {
                required = true
            }
        }
    }
}
