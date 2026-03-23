package org.beobma.mafia42discordproject.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.interaction.string
import org.beobma.mafia42discordproject.discord.DiscordMessageManager
import org.beobma.mafia42discordproject.game.GameManager

object ShamanRelayCommand : DiscordCommand {
    override val name: String = "shaman-relay"
    override val description: String = "영매가 죽은 자들의 채널로 접신 메시지를 전달합니다."
    override val koreanName: String = "접신"
    override val aliases: Set<String> = setOf("접신")
    private const val messageOptionName = "message"

    override suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent) {
        val message = event.interaction.command.strings[messageOptionName].orEmpty()
        val result = GameManager.relayShamanMessage(event.interaction.user.id, message)
        DiscordMessageManager.respondEphemeral(event, result.message)
    }

    override suspend fun handleMessage(event: MessageCreateEvent, args: List<String>) {
        val memberId = event.member?.id ?: return
        val result = GameManager.relayShamanMessage(memberId, args.joinToString(" "))
        event.message.channel.createMessage(result.message)
    }

    override suspend fun registerGlobal(kord: Kord) {
        kord.createGlobalChatInputCommand(name, description) {
            applyKoreanLocalization(this)
            string(messageOptionName, "죽은 자들에게 전달할 메시지") {
                required = true
            }
        }
    }

    override suspend fun registerGuild(kord: Kord, guildId: Snowflake) {
        kord.createGuildChatInputCommand(guildId, name, description) {
            applyKoreanLocalization(this)
            string(messageOptionName, "죽은 자들에게 전달할 메시지") {
                required = true
            }
        }
    }
}
