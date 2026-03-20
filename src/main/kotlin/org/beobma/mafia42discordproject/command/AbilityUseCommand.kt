package org.beobma.mafia42discordproject.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.suggestString
import dev.kord.core.event.interaction.GuildAutoCompleteInteractionCreateEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.interaction.user
import org.beobma.mafia42discordproject.discord.DiscordMessageManager
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GameManager
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.job.ability.ActiveAbility

object AbilityUseCommand : DiscordCommand {
    override val name: String = "use"
    override val description: String = "현재 사용할 수 있는 능력을 사용합니다."

    private const val abilityOptionName = "능력"
    private const val targetOptionName = "대상"
    private const val maxAutoCompleteChoices = 25

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
        val interaction = event.interaction
        if (interaction.focusedOption.value != abilityOptionName) return

        val game = GameManager.getCurrentGameFor(interaction.user.id) ?: return
        val caster = game.getPlayer(interaction.user.id) ?: return
        val query = interaction.focusedOption.value.trim()

        val suggestions = getUsableActiveAbilities(game, caster)
            .map(ActiveAbility::name)
            .distinct()
            .filter { query.isBlank() || it.contains(query, ignoreCase = true) }
            .take(maxAutoCompleteChoices)

        interaction.suggestString {
            suggestions.forEach { abilityName ->
                choice(abilityName, abilityName)
            }
        }
    }

    override suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent) {
        val interaction = event.interaction
        val game = GameManager.getCurrentGameFor(interaction.user.id)

        if (game == null) {
            DiscordMessageManager.respondEphemeral(event, "You must be in the current game to use an ability.")
            return
        }

        val caster = game.getPlayer(interaction.user.id)
        if (caster == null) {
            DiscordMessageManager.respondEphemeral(event, "You must be in the current game to use an ability.")
            return
        }

        val usableAbilities = getUsableActiveAbilities(game, caster)
        if (usableAbilities.isEmpty()) {
            DiscordMessageManager.respondEphemeral(event, "There is no active ability you can use right now.")
            return
        }

        val abilityName = interaction.command.strings[abilityOptionName]
        if (abilityName == null) {
            DiscordMessageManager.respondEphemeral(event, "You must choose an ability to use.")
            return
        }

        val selectedAbility = usableAbilities.firstOrNull { it.name == abilityName }
        if (selectedAbility == null) {
            DiscordMessageManager.respondEphemeral(event, "That ability cannot be used in the current phase.")
            return
        }

        val targetDiscordUser = interaction.command.users[targetOptionName]
        val target = targetDiscordUser?.let { game.getPlayer(it.id) }
        val result = selectedAbility.activate(game, caster, target)

        val message = if (result.isSuccess) {
            result.message ?: "Your ability was used successfully."
        } else {
            result.message ?: "Failed to use your ability."
        }
        DiscordMessageManager.respondEphemeral(event, message)
    }

    private fun getUsableActiveAbilities(game: Game, caster: PlayerData): List<ActiveAbility> {
        return caster.allAbilities
            .filterIsInstance<ActiveAbility>()
            .filter { it.usablePhase == game.currentPhase }
    }

    private fun dev.kord.rest.builder.interaction.ChatInputCreateBuilder.registerOptions() {
        string(abilityOptionName, "Select which active ability to use.") {
            required = true
            autocomplete = true
        }
        user(targetOptionName, "Select a target if the ability needs one.") {
            required = false
        }
    }
}
