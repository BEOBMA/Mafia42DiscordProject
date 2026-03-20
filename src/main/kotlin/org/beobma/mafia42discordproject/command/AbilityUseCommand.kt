package org.beobma.mafia42discordproject.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.suggestString
import dev.kord.core.event.interaction.GuildAutoCompleteInteractionCreateEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.interaction.user
import org.beobma.mafia42discordproject.discord.DiscordMessageManager
import org.beobma.mafia42discordproject.game.GameManager

object AbilityUseCommand : DiscordCommand {
    override val name: String = "능력사용"
    override val description: String = "현재 직업이 가진 능력을 사용합니다."

    private const val abilityOption = "ability"
    private const val targetOption = "target"
    private const val maxAutoCompleteChoices = 25
    private val mentionRegex = Regex("<@!?(\\d+)>")

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

    override suspend fun handleAutoComplete(event: GuildAutoCompleteInteractionCreateEvent) {
        if (event.interaction.focusedOption.name != abilityOption) return

        val query = event.interaction.focusedOption.value.trim()
        val suggestions = GameManager.getUsableActiveAbilityNames(event.interaction.user.id)
            .asSequence()
            .filter { query.isBlank() || it.contains(query, ignoreCase = true) }
            .take(maxAutoCompleteChoices)
            .toList()

        event.interaction.suggestString {
            suggestions.forEach { abilityName ->
                choice(abilityName, abilityName)
            }
        }
    }

    override suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent) {
        val abilityName = event.interaction.command.strings[abilityOption]
        if (abilityName.isNullOrBlank()) {
            DiscordMessageManager.respondEphemeral(event, "사용할 능력을 선택해야 합니다.")
            return
        }

        val targetUserId = event.interaction.command.users[targetOption]?.id
        val result = GameManager.activateAbility(
            userId = event.interaction.user.id,
            abilityName = abilityName,
            targetUserId = targetUserId
        )
        DiscordMessageManager.respondEphemeral(event, result.message ?: "능력 사용 결과를 확인할 수 없습니다.")
    }

    override suspend fun handleMessage(event: MessageCreateEvent, args: List<String>) {
        val userId = event.message.author?.id ?: return
        val abilityName = args.firstOrNull()
        if (abilityName.isNullOrBlank()) {
            event.message.channel.createMessage("${event.message.author?.mention.orEmpty()} 사용법: `!능력사용 <능력명> [@대상]`")
            return
        }

        val targetUserId = parseTargetUserId(args.getOrNull(1))
        val result = GameManager.activateAbility(
            userId = userId,
            abilityName = abilityName,
            targetUserId = targetUserId
        )
        event.message.channel.createMessage("${event.message.author?.mention.orEmpty()} ${result.message.orEmpty()}")
    }

    private fun dev.kord.rest.builder.interaction.ChatInputCreateBuilder.registerOptions() {
        string(abilityOption, "사용할 능력 이름") {
            required = true
            autocomplete = true
        }
        user(targetOption, "대상 플레이어") {
            required = false
        }
    }

    private fun parseTargetUserId(raw: String?): Snowflake? {
        if (raw.isNullOrBlank()) return null

        val mentionId = mentionRegex.matchEntire(raw)?.groupValues?.getOrNull(1)
        val snowflake = mentionId ?: raw
        return snowflake.toULongOrNull()?.let(::Snowflake)
    }
}
