package org.beobma.mafia42discordproject.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.suggestString
import dev.kord.core.entity.interaction.StringOptionValue
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
    override val description: String = "use ability"

    private const val abilityOptionName = "use_ability"
    private const val targetOptionName = "use_target"
    private const val maxAutoCompleteChoices = 25

    override suspend fun handleAutoComplete(event: GuildAutoCompleteInteractionCreateEvent) {
        val interaction = event.interaction
        val focusedEntry = interaction.command.options.entries
            .firstOrNull { it.value.focused } ?: return

        if (focusedEntry.key != abilityOptionName) return

        val game = GameManager.getCurrentGameFor(interaction.user.id) ?: return
        val caster = game.getPlayer(interaction.user.id) ?: return
        val query = (focusedEntry.value as? StringOptionValue)?.value?.trim().orEmpty()

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