package org.beobma.mafia42discordproject.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.suggestString
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.interaction.StringOptionValue
import dev.kord.core.event.interaction.GuildAutoCompleteInteractionCreateEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.interaction.user
import org.beobma.mafia42discordproject.discord.DiscordMessageManager
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.GameManager
import org.beobma.mafia42discordproject.game.GameLoopManager
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.HackerRedirectManager
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.administrator.AdministratorAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.administrator.AdministratorInvestigationPolicy
import org.beobma.mafia42discordproject.job.ability.general.definition.list.administrator.Cooperation
import org.beobma.mafia42discordproject.job.ability.general.definition.list.agent.Humint
import org.beobma.mafia42discordproject.job.ability.general.definition.list.administrator.Identification
import org.beobma.mafia42discordproject.job.ability.general.definition.list.detective.DetectiveAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.doctor.DoctorAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.nurse.NurseAbility
import org.beobma.mafia42discordproject.job.ability.general.evil.list.beastman.BeastmanAbility
import org.beobma.mafia42discordproject.job.ability.general.evil.list.godfather.GodfatherAbility
import org.beobma.mafia42discordproject.job.ability.general.evil.list.godfather.GodfatherContactPolicy
import org.beobma.mafia42discordproject.job.ability.general.evil.list.hitman.HitManAbility
import org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.MafiaAbility
import org.beobma.mafia42discordproject.job.definition.list.Judge
import org.beobma.mafia42discordproject.job.definition.list.Politician
import org.beobma.mafia42discordproject.job.JobManager
import org.beobma.mafia42discordproject.job.evil.list.Beastman
import org.beobma.mafia42discordproject.job.evil.list.Mafia

object AbilityUseCommand : DiscordCommand {
    override val name: String = "use"
    override val description: String = "use ability"

    private const val abilityOptionName = "use_ability"
    private const val targetOptionName = "use_target"
    private const val jobOptionName = "use_job"
    private const val maxAutoCompleteChoices = 25

    override suspend fun handleAutoComplete(event: GuildAutoCompleteInteractionCreateEvent) {
        val interaction = event.interaction
        val focusedEntry = interaction.command.options.entries
            .firstOrNull { it.value.focused } ?: return

        val game = GameManager.getCurrentGameFor(interaction.user.id) ?: return
        val caster = game.getPlayer(interaction.user.id) ?: return
        val query = (focusedEntry.value as? StringOptionValue)?.value?.trim().orEmpty()

        when (focusedEntry.key) {
            abilityOptionName -> {
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

            jobOptionName -> {
                val selectedAbilityName = interaction.command.strings[abilityOptionName]
                val selectedAbility = getUsableActiveAbilities(game, caster).firstOrNull { it.name == selectedAbilityName }
                val suggestions = when (selectedAbility) {
                    is AdministratorAbility -> {
                        val hasCooperation = caster.allAbilities.any { it is Cooperation }
                        val hasIdentification = caster.allAbilities.any { it is Identification }
                        JobManager.getAll()
                            .filter { AdministratorInvestigationPolicy.isJobSelectable(it, hasCooperation, hasIdentification) }
                            .map { it.name }
                    }
                    is HitManAbility -> JobManager.getAll().map { it.name }
                    else -> return
                }
                    .distinct()
                    .filter { query.isBlank() || it.contains(query, ignoreCase = true) }
                    .take(maxAutoCompleteChoices)

                interaction.suggestString {
                    suggestions.forEach { jobName ->
                        choice(jobName, jobName)
                    }
                }
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
        if (caster.member.id in game.pendingNightDeathPlayerIds) {
            DiscordMessageManager.respondEphemeral(event, "이미 암살당해 능력을 사용할 수 없습니다.")
            return
        }
        if (caster.state.isSilenced && caster.job !is Mafia) {
            DiscordMessageManager.respondEphemeral(event, "유혹 상태에서는 능력을 사용할 수 없습니다.")
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
        if (isBlockedByUnwrittenRule(game, target)) {
            DiscordMessageManager.respondEphemeral(event, "불문율에 의해 불가능합니다.")
            return
        }
        if (target != null && GameLoopManager.isMadScientistDistortionHidden(target)) {
            DiscordMessageManager.respondEphemeral(event, deadTargetRejectedMessage(selectedAbility))
            return
        }
        val effectiveTarget = HackerRedirectManager.resolveTarget(game, target)
        if (effectiveTarget != null && GameLoopManager.isMadScientistDistortionHidden(effectiveTarget)) {
            DiscordMessageManager.respondEphemeral(event, deadTargetRejectedMessage(selectedAbility))
            return
        }
        val previousMafiaTarget = if (selectedAbility is MafiaAbility || selectedAbility is BeastmanAbility) {
            game.nightAttacks["MAFIA_TEAM"]?.target
        } else {
            null
        }

        val result = when (selectedAbility) {
            is AdministratorAbility -> {
                val selectedJobName = interaction.command.strings[jobOptionName]
                selectedAbility.activateWithJobName(game, caster, selectedJobName)
            }
            is HitManAbility -> {
                val selectedJobName = interaction.command.strings[jobOptionName]
                selectedAbility.activateWithJobName(game, caster, target, selectedJobName)
            }
            else -> selectedAbility.activate(game, caster, target)
        }

        if (result.isSuccess) {
            game.abilityUsersThisPhase += caster.member.id
            if (effectiveTarget != null) {
                game.abilityTargetByUserThisPhase[caster.member.id] = effectiveTarget.member.id
            } else {
                game.abilityTargetByUserThisPhase.remove(caster.member.id)
            }
        }

        if (result.isSuccess && (selectedAbility is NurseAbility || selectedAbility is DoctorAbility)) {
            GameLoopManager.notifyNurseDoctorContactImmediately(game)
        }

        if (result.isSuccess && selectedAbility is MafiaAbility && target != null) {
            notifyMafiaTargetSelection(game, caster, target, previousMafiaTarget)
        }
        if (result.isSuccess && selectedAbility is BeastmanAbility && target != null && caster.job is Beastman && caster.state.isTamed) {
            notifyMafiaTargetSelection(game, caster, target, previousMafiaTarget)
        }
        if (result.isSuccess && effectiveTarget != null) {
            Humint.notifyIfTriggered(game, caster, effectiveTarget, selectedAbility)
            DetectiveAbility.notifyTargetSelection(game, caster, effectiveTarget, selectedAbility)
        }

        val message = if (result.isSuccess) {
            result.message?.takeIf { it.isNotBlank() } ?: "Your ability was used successfully."
        } else {
            result.message?.takeIf { it.isNotBlank() } ?: "Failed to use your ability."
        }
        DiscordMessageManager.respondEphemeral(event, message)
    }

    private suspend fun notifyMafiaTargetSelection(
        game: Game,
        caster: PlayerData,
        target: PlayerData,
        previousTarget: PlayerData?
    ) {
        val mafiaChannel = game.mafiaChannel ?: return
        val action = if (previousTarget != null && previousTarget != target) "변경" else "결정"
        mafiaChannel.createMessage(
            "마피아의 처형 대상을 ${caster.member.effectiveName}이(가) ${target.member.effectiveName}으로 ${action}했습니다."
        )
    }

    private fun getUsableActiveAbilities(game: Game, caster: PlayerData): List<ActiveAbility> {
        return caster.allAbilities
            .filterIsInstance<ActiveAbility>()
            .filter { it.usablePhase == game.currentPhase }
            .filter { ability ->
                if (ability is GodfatherAbility) {
                    GodfatherContactPolicy.canUseExecution(game, caster)
                } else {
                    true
                }
            }
    }

    private fun isBlockedByUnwrittenRule(game: Game, directTarget: PlayerData?): Boolean {
        if (game.currentPhase != GamePhase.NIGHT) return false
        val blockedTargetId = game.unwrittenRuleBlockedTargetIdTonight ?: return false
        val target = directTarget ?: return false
        if (target.member.id != blockedTargetId) return false
        return target.job is Judge || target.job is Politician
    }

    private fun deadTargetRejectedMessage(selectedAbility: ActiveAbility): String {
        return when (selectedAbility) {
            is DoctorAbility -> "이미 사망한 플레이어는 치료할 수 없습니다."
            is MafiaAbility, is GodfatherAbility, is BeastmanAbility -> "이미 사망한 플레이어는 처형 대상으로 지정할 수 없습니다."
            is NurseAbility -> "사망한 플레이어는 처방 대상으로 지정할 수 없습니다."
            is DetectiveAbility -> "사망한 플레이어는 추리 대상으로 지정할 수 없습니다."
            else -> "이미 사망한 플레이어는 대상으로 지정할 수 없습니다."
        }
    }

    private fun dev.kord.rest.builder.interaction.ChatInputCreateBuilder.registerOptions() {
        string(abilityOptionName, "Select which active ability to use.") {
            required = true
            autocomplete = true
        }
        user(targetOptionName, "Select a target if the ability needs one.") {
            required = false
        }
        string(jobOptionName, "Select a job if the ability targets a job.") {
            required = false
            autocomplete = true
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
