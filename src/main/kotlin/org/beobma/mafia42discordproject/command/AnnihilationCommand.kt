package org.beobma.mafia42discordproject.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.suggestString
import dev.kord.core.entity.interaction.StringOptionValue
import dev.kord.core.event.interaction.GuildAutoCompleteInteractionCreateEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.interaction.user
import org.beobma.mafia42discordproject.discord.DiscordMessageManager
import org.beobma.mafia42discordproject.game.GameManager
import org.beobma.mafia42discordproject.game.annihilation.AnnihilationModeManager

object AnnihilationCommand : DiscordCommand {
    override val name: String = "annihilation"
    override val description: String = "말살 모드 전용 행동을 수행합니다."
    override val koreanName: String = "말살"
    override val aliases: Set<String> = setOf("말살")

    private const val ACTION_OPTION_NAME = "action"
    private const val SECRET_OPTION_NAME = "secret"
    private const val NOTE_OPTION_NAME = "note"
    private const val LOCATION_OPTION_NAME = "location"
    private const val LOCATION2_OPTION_NAME = "location2"
    private const val LOCATION3_OPTION_NAME = "location3"
    private const val MESSAGE_OPTION_NAME = "message"
    private const val ABILITY_OPTION_NAME = "ability"
    private const val TARGET_OPTION_NAME = "target"
    private const val TARGET2_OPTION_NAME = "target2"

    override suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent) {
        val interaction = event.interaction
        val message = GameManager.handleAnnihilationCommand(
            userId = interaction.user.id,
            action = interaction.command.strings[ACTION_OPTION_NAME],
            secret = interaction.command.strings[SECRET_OPTION_NAME],
            note = interaction.command.strings[NOTE_OPTION_NAME],
            location = interaction.command.strings[LOCATION_OPTION_NAME],
            location2 = interaction.command.strings[LOCATION2_OPTION_NAME],
            location3 = interaction.command.strings[LOCATION3_OPTION_NAME],
            anonymousMessage = interaction.command.strings[MESSAGE_OPTION_NAME],
            anonymousAbility = interaction.command.strings[ABILITY_OPTION_NAME],
            targetId = interaction.command.users[TARGET_OPTION_NAME]?.id,
            target2Id = interaction.command.users[TARGET2_OPTION_NAME]?.id
        )
        DiscordMessageManager.respondEphemeral(event, message)
    }

    override suspend fun handleAutoComplete(event: GuildAutoCompleteInteractionCreateEvent) {
        val interaction = event.interaction
        val focusedEntry = interaction.command.options.entries.firstOrNull { it.value.focused } ?: return
        val query = (focusedEntry.value as? StringOptionValue)?.value?.trim().orEmpty()
        val suggestions = when (focusedEntry.key) {
            MESSAGE_OPTION_NAME -> AnnihilationModeManager.anonymousMessageSuggestions(query)
            ABILITY_OPTION_NAME -> {
                val selectedMessage = interaction.command.strings[MESSAGE_OPTION_NAME]
                AnnihilationModeManager.anonymousAbilitySuggestions(selectedMessage, query)
            }
            else -> return
        }

        interaction.suggestString {
            suggestions.forEach { suggestion ->
                choice(suggestion, suggestion)
            }
        }
    }

    override suspend fun handleMessage(event: MessageCreateEvent, args: List<String>) {
        val member = event.member ?: return
        val action = args.getOrNull(0)
        val secret = when (action) {
            "처형", "실행", "execute" -> args.drop(1).joinToString(" ")
            "신분증전달", "give-id", "give_id" -> args.drop(2).joinToString(" ")
            else -> null
        }?.takeIf { it.isNotBlank() }
        val note = when (action) {
            "수첩작성", "메모", "notebook-add", "notebook_add", "note", "memo" -> args.drop(1).joinToString(" ")
            else -> null
        }?.takeIf { it.isNotBlank() }
        val anonymousMessage = when (action) {
            "익명메시지", "요원메시지", "agent-message", "agent_message", "anonymous-message", "anonymous_message" -> {
                args.drop(1).joinToString(" ")
            }
            else -> null
        }?.takeIf { it.isNotBlank() }
        val message = GameManager.handleAnnihilationCommand(
            userId = member.id,
            action = action,
            secret = secret,
            note = note,
            location = args.getOrNull(1),
            location2 = args.getOrNull(2),
            location3 = args.getOrNull(3),
            anonymousMessage = anonymousMessage,
            anonymousAbility = null,
            targetId = parseMention(args.getOrNull(1)),
            target2Id = parseMention(args.getOrNull(2))
        )
        event.message.channel.createMessage(message)
    }

    private fun parseMention(raw: String?): Snowflake? {
        val value = raw
            ?.trim()
            ?.removePrefix("<@")
            ?.removePrefix("!")
            ?.removeSuffix(">")
            ?: return null
        return value.toULongOrNull()?.let(::Snowflake)
    }

    private fun dev.kord.rest.builder.interaction.ChatInputCreateBuilder.registerOptions() {
        string(ACTION_OPTION_NAME, "행동") {
            required = true
            choice("상태", "상태")
            choice("도움", "도움")
            choice("수첩", "수첩")
            choice("수첩작성", "수첩작성")
            choice("수첩줍기", "수첩줍기")
            choice("익명메시지", "익명메시지")
            choice("처형", "처형")
            choice("탐문", "탐문")
            choice("마피아미션", "마피아미션")
            choice("증명", "증명")
            choice("사칭", "사칭")
            choice("합동수사", "합동수사")
            choice("카포미션", "카포미션")
            choice("솔다토미션", "솔다토미션")
            choice("직위양도", "직위양도")
            choice("신분증전달", "신분증전달")
        }
        string(SECRET_OPTION_NAME, "비밀 신원 또는 신분증 코드") {
            required = false
        }
        string(NOTE_OPTION_NAME, "수첩에 추가할 메모") {
            required = false
        }
        string(LOCATION_OPTION_NAME, "장소 1") {
            required = false
            registerLocationChoices()
        }
        string(LOCATION2_OPTION_NAME, "장소 2") {
            required = false
            registerLocationChoices()
        }
        string(LOCATION3_OPTION_NAME, "장소 3") {
            required = false
            registerLocationChoices()
        }
        string(MESSAGE_OPTION_NAME, "요원 익명 메시지 문구") {
            required = false
            autocomplete = true
        }
        string(ABILITY_OPTION_NAME, "익명 메시지에 넣을 능력") {
            required = false
            autocomplete = true
        }
        user(TARGET_OPTION_NAME, "대상 A") {
            required = false
        }
        user(TARGET2_OPTION_NAME, "대상 B") {
            required = false
        }
    }

    private fun dev.kord.rest.builder.interaction.StringChoiceBuilder.registerLocationChoices() {
        choice("광장", "광장")
        choice("자료실", "자료실")
        choice("경찰서", "경찰서")
        choice("상가", "상가")
        choice("병원", "병원")
        choice("편의점", "편의점")
        choice("주택가", "주택가")
        choice("골목길", "골목길")
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
