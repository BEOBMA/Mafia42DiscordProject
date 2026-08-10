package org.beobma.mafia42discordproject.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.suggestString
import dev.kord.core.event.interaction.GuildAutoCompleteInteractionCreateEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.interaction.string
import org.beobma.mafia42discordproject.discord.DiscordMessageManager
import org.beobma.mafia42discordproject.game.GameManager
import org.beobma.mafia42discordproject.game.player.PreferencePreset
import org.beobma.mafia42discordproject.game.player.PreferencePresetManager
import org.beobma.mafia42discordproject.game.player.PresetDeleteResult
import org.beobma.mafia42discordproject.game.player.PresetLoadResult
import org.beobma.mafia42discordproject.game.player.PresetRenameResult
import org.beobma.mafia42discordproject.game.player.PresetSaveResult

object PreferencePresetCommand : DiscordCommand {
    override val name: String = "preset"
    override val description: String = "선호 직업과 보석 설정을 프리셋으로 저장하고 관리합니다."
    override val koreanName: String = "프리셋"
    override val aliases: Set<String> = setOf("프리셋")

    private const val ACTION_OPTION = "동작"
    private const val NAME_OPTION = "이름"
    private const val NEW_NAME_OPTION = "새이름"
    private const val ACTION_SAVE = "save"
    private const val ACTION_LOAD = "load"
    private const val ACTION_LIST = "list"
    private const val ACTION_RENAME = "rename"
    private const val ACTION_DELETE = "delete"

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

    override suspend fun handleAutoComplete(event: GuildAutoCompleteInteractionCreateEvent) {
        val focusedOption = event.interaction.command.options.entries
            .firstOrNull { it.value.focused }
            ?: return
        if (focusedOption.key != NAME_OPTION) return

        val query = event.interaction.focusedOption.value.trim()
        val presets = PreferencePresetManager.getAll(event.interaction.user.id.value)
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }

        event.interaction.suggestString {
            presets.forEach { preset -> choice(preset.name, preset.name) }
        }
    }

    override suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent) {
        val command = event.interaction.command
        val message = execute(
            userId = event.interaction.user.id,
            action = command.strings[ACTION_OPTION],
            name = command.strings[NAME_OPTION],
            newName = command.strings[NEW_NAME_OPTION]
        )
        DiscordMessageManager.respondEphemeral(event, message)
    }

    override suspend fun handleMessage(event: MessageCreateEvent, args: List<String>) {
        val userId = event.message.author?.id ?: return
        val action = normalizeAction(args.firstOrNull())
        if (action == null) {
            event.message.channel.createMessage(usage())
            return
        }

        val rawArguments = args.drop(1).joinToString(" ").trim()
        val (presetName, newPresetName) = if (action == ACTION_RENAME) {
            val names = rawArguments.split('|', limit = 2)
            names.getOrNull(0)?.trim() to names.getOrNull(1)?.trim()
        } else {
            rawArguments to null
        }

        event.message.channel.createMessage(execute(userId, action, presetName, newPresetName))
    }

    private fun execute(userId: Snowflake, action: String?, name: String?, newName: String?): String = when (action) {
        ACTION_SAVE -> save(userId.value, name)
        ACTION_LOAD -> load(userId, name)
        ACTION_LIST -> list(userId.value)
        ACTION_RENAME -> rename(userId.value, name, newName)
        ACTION_DELETE -> delete(userId.value, name)
        else -> usage()
    }

    private fun save(userId: ULong, name: String?): String = when (PreferencePresetManager.saveCurrent(userId, name)) {
        PresetSaveResult.CREATED -> {
            val preset = PreferencePresetManager.get(userId, name)!!
            "프리셋을 저장했습니다. (${PreferencePresetManager.getAll(userId).size}/${PreferencePresetManager.MAX_PRESETS})\n${formatPreset(preset)}"
        }
        PresetSaveResult.UPDATED -> {
            val preset = PreferencePresetManager.get(userId, name)!!
            "기존 프리셋을 현재 설정으로 덮어썼습니다.\n${formatPreset(preset)}"
        }
        PresetSaveResult.INVALID_NAME -> invalidNameMessage()
        PresetSaveResult.PREFERENCES_NOT_SET ->
            "저장할 선호 직업 7개가 없습니다. `/jobpreference`로 먼저 설정해 주세요."
        PresetSaveResult.BEST_JOB_NOT_SET ->
            "저장할 보석 직업이 없습니다. `/보석`으로 먼저 설정해 주세요."
        PresetSaveResult.BEST_JOB_INVALID ->
            "현재 보석 직업이 선호 직업과 맞지 않습니다. `/보석`으로 다시 설정해 주세요."
        PresetSaveResult.LIMIT_REACHED ->
            "프리셋은 최대 ${PreferencePresetManager.MAX_PRESETS}개까지 저장할 수 있습니다. 기존 프리셋을 덮어쓰거나 삭제해 주세요."
    }

    private fun load(userId: Snowflake, name: String?): String {
        if (GameManager.isInCurrentGame(userId)) {
            return "게임 참여 중에는 프리셋을 불러올 수 없습니다."
        }

        return when (PreferencePresetManager.loadPreset(userId.value, name)) {
            PresetLoadResult.LOADED -> {
                val preset = PreferencePresetManager.get(userId.value, name)!!
                "프리셋을 불러와 현재 설정에 적용했습니다.\n${formatPreset(preset)}"
            }
            PresetLoadResult.INVALID_NAME -> invalidNameMessage()
            PresetLoadResult.NOT_FOUND -> "해당 이름의 프리셋을 찾을 수 없습니다: ${displayName(name)}"
            PresetLoadResult.INVALID_PRESET ->
                "프리셋에 더 이상 사용할 수 없는 직업이 포함되어 있어 불러올 수 없습니다. 현재 설정을 다시 저장해 주세요."
        }
    }

    private fun list(userId: ULong): String {
        val presets = PreferencePresetManager.getAll(userId)
        if (presets.isEmpty()) {
            return "저장된 프리셋이 없습니다. 선호 직업과 보석을 설정한 뒤 `/프리셋 동작:저장 이름:<이름>`으로 저장해 주세요."
        }

        return buildString {
            appendLine("저장된 프리셋 (${presets.size}/${PreferencePresetManager.MAX_PRESETS})")
            presets.forEachIndexed { index, preset ->
                if (index > 0) appendLine()
                append("${index + 1}. ${formatPreset(preset)}")
            }
        }.trimEnd()
    }

    private fun rename(userId: ULong, currentName: String?, newName: String?): String =
        when (PreferencePresetManager.rename(userId, currentName, newName)) {
            PresetRenameResult.RENAMED ->
                "프리셋 이름을 ${displayName(currentName)}에서 ${displayName(newName)}(으)로 변경했습니다."
            PresetRenameResult.INVALID_CURRENT_NAME -> "변경할 프리셋 이름을 입력해 주세요."
            PresetRenameResult.INVALID_NEW_NAME -> invalidNameMessage()
            PresetRenameResult.NOT_FOUND -> "해당 이름의 프리셋을 찾을 수 없습니다: ${displayName(currentName)}"
            PresetRenameResult.NAME_ALREADY_EXISTS -> "이미 같은 이름의 프리셋이 있습니다: ${displayName(newName)}"
        }

    private fun delete(userId: ULong, name: String?): String =
        when (PreferencePresetManager.delete(userId, name)) {
            PresetDeleteResult.DELETED ->
                "프리셋을 삭제했습니다: ${displayName(name)} (${PreferencePresetManager.getAll(userId).size}/${PreferencePresetManager.MAX_PRESETS})"
            PresetDeleteResult.INVALID_NAME -> invalidNameMessage()
            PresetDeleteResult.NOT_FOUND -> "해당 이름의 프리셋을 찾을 수 없습니다: ${displayName(name)}"
        }

    private fun formatPreset(preset: PreferencePreset): String = buildString {
        append(displayName(preset.name))
        append(" — 보석: **${preset.bestJob.name}**")
        append("\n   선호: ")
        append(preset.jobs.joinToString(", ") { it.name })
    }

    private fun displayName(name: String?): String = "`${name?.trim().orEmpty().replace("`", "ˋ")}`"

    private fun invalidNameMessage(): String =
        "프리셋 이름은 1~${PreferencePresetManager.MAX_NAME_LENGTH}자로 입력해 주세요. 줄바꿈과 제어 문자는 사용할 수 없습니다."

    private fun usage(): String = buildString {
        appendLine("사용법:")
        appendLine("• `/프리셋 동작:저장 이름:<이름>`")
        appendLine("• `/프리셋 동작:불러오기 이름:<이름>`")
        appendLine("• `/프리셋 동작:목록`")
        appendLine("• `/프리셋 동작:이름변경 이름:<기존이름> 새이름:<새이름>`")
        append("• `/프리셋 동작:삭제 이름:<이름>`")
    }

    private fun normalizeAction(rawAction: String?): String? = when (rawAction?.trim()?.lowercase()) {
        ACTION_SAVE, "저장" -> ACTION_SAVE
        ACTION_LOAD, "불러오기", "적용" -> ACTION_LOAD
        ACTION_LIST, "목록" -> ACTION_LIST
        ACTION_RENAME, "이름변경", "이름 변경" -> ACTION_RENAME
        ACTION_DELETE, "삭제" -> ACTION_DELETE
        else -> null
    }

    private fun dev.kord.rest.builder.interaction.ChatInputCreateBuilder.registerOptions() {
        string(ACTION_OPTION, "프리셋에서 실행할 동작") {
            required = true
            choice("저장", ACTION_SAVE)
            choice("불러오기", ACTION_LOAD)
            choice("목록", ACTION_LIST)
            choice("이름변경", ACTION_RENAME)
            choice("삭제", ACTION_DELETE)
        }
        string(NAME_OPTION, "저장하거나 관리할 프리셋 이름") {
            required = false
            autocomplete = true
        }
        string(NEW_NAME_OPTION, "이름변경 시 사용할 새 이름") {
            required = false
        }
    }
}
