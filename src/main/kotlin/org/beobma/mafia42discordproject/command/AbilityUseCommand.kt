package org.beobma.mafia42discordproject.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.user
import org.beobma.mafia42discordproject.discord.DiscordMessageManager
import org.beobma.mafia42discordproject.game.GameManager
import org.beobma.mafia42discordproject.job.ability.ActiveAbility

object AbilityUseCommand : DiscordCommand {
    override val name: String = "use"
    override val description: String = "Use your active ability for the current phase."

    private const val targetOptionName = "target"

    override suspend fun registerGlobal(kord: Kord) {
        kord.createGlobalChatInputCommand(name, description) {
            user(targetOptionName, "Select a target if the ability needs one.") {
                required = false
            }
        }
    }

    override suspend fun registerGuild(kord: Kord, guildId: Snowflake) {
        kord.createGuildChatInputCommand(guildId, name, description) {
            user(targetOptionName, "Select a target if the ability needs one.") {
                required = false
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

        val activeAbility = caster.job?.abilities
            ?.filterIsInstance<ActiveAbility>()
            ?.firstOrNull { it.usablePhase == game.currentPhase }

        if (activeAbility == null) {
            DiscordMessageManager.respondEphemeral(event, "There is no active ability you can use right now.")
            return
        }

        val targetDiscordUser = interaction.command.users[targetOptionName]
        val target = targetDiscordUser?.let { game.getPlayer(it.id) }
        val result = activeAbility.activate(game, caster, target)

        val message = if (result.isSuccess) {
            result.message ?: "Your ability was used successfully."
        } else {
            result.message ?: "Failed to use your ability."
        }
        DiscordMessageManager.respondEphemeral(event, message)
    }
}
