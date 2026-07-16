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
import org.beobma.mafia42discordproject.game.GameLoopManager
import org.beobma.mafia42discordproject.game.GameManager
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.replay.GameReplayLogger
import org.beobma.mafia42discordproject.game.replay.ReplayLogType
import org.beobma.mafia42discordproject.game.replay.ReplayVisibility
import org.beobma.mafia42discordproject.game.system.FrogCurseManager
import org.beobma.mafia42discordproject.game.system.HackerRedirectManager
import org.beobma.mafia42discordproject.game.system.SwindlerManager
import org.beobma.mafia42discordproject.job.JobManager
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.administrator.AdministratorAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.administrator.AdministratorInvestigationPolicy
import org.beobma.mafia42discordproject.job.ability.general.definition.list.administrator.Cooperation
import org.beobma.mafia42discordproject.job.ability.general.definition.list.administrator.Identification
import org.beobma.mafia42discordproject.job.ability.general.definition.list.agent.Humint
import org.beobma.mafia42discordproject.job.ability.general.definition.list.detective.DetectiveAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.doctor.DoctorAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.fortuneteller.FortunetellerAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.hacker.HackerAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.inspector.InspectorInvestigation
import org.beobma.mafia42discordproject.job.ability.general.definition.list.mentalist.MentalistAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.nurse.NurseAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.police.PoliceAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.reporter.ReporterAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.cabal.SunCabalAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.other.UnwrittenRule
import org.beobma.mafia42discordproject.job.ability.general.definition.list.shaman.SoulRelease
import org.beobma.mafia42discordproject.job.ability.general.definition.list.vigilante.VigilantePurgeDayAbility
import org.beobma.mafia42discordproject.job.ability.general.evil.list.godfather.GodfatherAbility
import org.beobma.mafia42discordproject.job.ability.general.evil.list.godfather.GodfatherContactPolicy
import org.beobma.mafia42discordproject.job.ability.general.evil.list.hitman.HitManAbility
import org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.MafiaAbility
import org.beobma.mafia42discordproject.job.ability.general.evil.list.spy.SpyAbility
import org.beobma.mafia42discordproject.job.definition.list.Cabal
import org.beobma.mafia42discordproject.job.definition.list.CabalRole
import org.beobma.mafia42discordproject.job.definition.list.MentalPatient
import org.beobma.mafia42discordproject.job.evil.Evil
import org.beobma.mafia42discordproject.job.evil.list.Mafia

object AbilityUseCommand : DiscordCommand {
    override val name: String = "use"
    override val description: String = "use ability"
    override val koreanName: String = "사용"
    override val aliases: Set<String> = setOf("사용")

    private const val ABILITY_OPTION_NAME = "use_ability"
    private const val TARGET_OPTION_NAME = "use_target"
    private const val JOB_OPTION_NAME = "use_job"
    private const val MAX_AUTO_COMPLETE_CHOICES = 25

    override suspend fun handleAutoComplete(event: GuildAutoCompleteInteractionCreateEvent) {
        val interaction = event.interaction
        val focusedEntry = interaction.command.options.entries
            .firstOrNull { it.value.focused } ?: return

        val game = GameManager.getCurrentGameFor(interaction.user.id) ?: return
        val caster = game.getPlayer(interaction.user.id) ?: return
        val query = (focusedEntry.value as? StringOptionValue)?.value?.trim().orEmpty()

        when (focusedEntry.key) {
            ABILITY_OPTION_NAME -> {
                val suggestions = getUsableActiveAbilities(game, caster)
                    .map(ActiveAbility::name)
                    .distinct()
                    .filter { query.isBlank() || it.contains(query, ignoreCase = true) }
                    .take(MAX_AUTO_COMPLETE_CHOICES)

                interaction.suggestString {
                    suggestions.forEach { abilityName ->
                        choice(abilityName, abilityName)
                    }
                }
            }

            JOB_OPTION_NAME -> {
                val selectedAbilityName = interaction.command.strings[ABILITY_OPTION_NAME]
                val selectedAbility = getUsableActiveAbilities(game, caster).firstOrNull { it.name == selectedAbilityName }
                val suggestions = when (selectedAbility) {
                    is AdministratorAbility -> {
                        val hasCooperation = caster.allAbilities.any { it is Cooperation }
                        val hasIdentification = caster.allAbilities.any { it is Identification }
                        JobManager.getAll()
                            .filter { AdministratorInvestigationPolicy.isJobSelectable(it, hasCooperation, hasIdentification) }
                            .map { it.name }
                    }
                    is HitManAbility -> JobManager.getAll()
                        .map { it.name }
                        .filterNot { it in game.publiclyRevealedJobNames }
                    else -> return
                }
                    .distinct()
                    .filter { query.isBlank() || it.contains(query, ignoreCase = true) }
                    .take(MAX_AUTO_COMPLETE_CHOICES)

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

        val abilityName = interaction.command.strings[ABILITY_OPTION_NAME]
        if (abilityName == null) {
            DiscordMessageManager.respondEphemeral(event, "You must choose an ability to use.")
            return
        }

        val selectedAbility = usableAbilities.firstOrNull { it.name == abilityName }
        if (selectedAbility == null) {
            DiscordMessageManager.respondEphemeral(event, "That ability cannot be used in the current phase.")
            return
        }
        if (!FrogCurseManager.canUseActiveAbility(caster, selectedAbility)) {
            DiscordMessageManager.respondEphemeral(event, "개구리 상태에서는 능력을 사용할 수 없습니다.")
            return
        }

        val targetDiscordUser = interaction.command.users[TARGET_OPTION_NAME]
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
        if (isBlockedByBlessing(game, target, effectiveTarget)) {
            DiscordMessageManager.respondEphemeral(event, "축복으로 오늘은 해당 플레이어를 대상으로 지정할 수 없습니다.")
            return
        }
        if (effectiveTarget != null && GameLoopManager.isMadScientistDistortionHidden(effectiveTarget)) {
            DiscordMessageManager.respondEphemeral(event, deadTargetRejectedMessage(selectedAbility))
            return
        }
        if (caster.job is MentalPatient) {
            val selectedJobName = interaction.command.strings[JOB_OPTION_NAME]
            val result = activateMentalPatientFakeAbility(game, caster, selectedAbility, target, selectedJobName)
            val message = if (result.isSuccess) {
                result.message?.takeIf { it.isNotBlank() } ?: "Your ability was used successfully."
            } else {
                result.message?.takeIf { it.isNotBlank() } ?: "Failed to use your ability."
            }
            GameReplayLogger.log(
                game = game,
                type = ReplayLogType.ABILITY_USED,
                visibility = ReplayVisibility.EPHEMERAL,
                title = "능력 사용",
                body = buildAbilityReplayBody(selectedAbility.name, target, null, selectedJobName, result.isSuccess, message),
                actor = caster,
                recipients = listOf(GameReplayLogger.recipient(caster, ReplayVisibility.EPHEMERAL))
            )
            DiscordMessageManager.respondEphemeral(event, message)
            return
        }
        val previousMafiaTarget = if (selectedAbility is MafiaAbility) {
            game.nightAttacks["MAFIA_TEAM"]?.target
        } else {
            null
        }
        val previousAbilityTargetId = game.abilityTargetByUserThisPhase[caster.member.id]

        val result = when (selectedAbility) {
            is AdministratorAbility -> {
                val selectedJobName = interaction.command.strings[JOB_OPTION_NAME]
                selectedAbility.activateWithJobName(game, caster, selectedJobName)
            }
            is HitManAbility -> {
                val selectedJobName = interaction.command.strings[JOB_OPTION_NAME]
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

        if (result.isSuccess && selectedAbility is MafiaAbility && target != null) {
            notifyMafiaTargetSelection(game, caster, target, previousMafiaTarget)
        }
        if (result.isSuccess && effectiveTarget != null) {
            SwindlerManager.notifyBeautyTrap(effectiveTarget, caster)
            Humint.notifyIfTriggered(game, caster, effectiveTarget)
            DetectiveAbility.notifyTargetSelection(
                game = game,
                caster = caster,
                selectedTarget = effectiveTarget,
                previousTargetId = previousAbilityTargetId
            )
        }

        val message = if (result.isSuccess) {
            result.message?.takeIf { it.isNotBlank() } ?: "Your ability was used successfully."
        } else {
            result.message?.takeIf { it.isNotBlank() } ?: "Failed to use your ability."
        }
        GameReplayLogger.log(
            game = game,
            type = ReplayLogType.ABILITY_USED,
            visibility = ReplayVisibility.EPHEMERAL,
            title = "능력 사용",
            body = buildAbilityReplayBody(
                abilityName = selectedAbility.name,
                target = target,
                effectiveTarget = effectiveTarget,
                selectedJobName = interaction.command.strings[JOB_OPTION_NAME],
                isSuccess = result.isSuccess,
                message = message
            ),
            actor = caster,
            recipients = listOf(GameReplayLogger.recipient(caster, ReplayVisibility.EPHEMERAL))
        )
        DiscordMessageManager.respondEphemeral(event, message)
    }

    private fun buildAbilityReplayBody(
        abilityName: String,
        target: PlayerData?,
        effectiveTarget: PlayerData?,
        selectedJobName: String?,
        isSuccess: Boolean,
        message: String
    ): String {
        return buildString {
            appendLine("능력: $abilityName")
            target?.let { appendLine("대상: ${it.member.effectiveName}") }
            if (effectiveTarget != null && effectiveTarget.member.id != target?.member?.id) {
                appendLine("실제 적용 대상: ${effectiveTarget.member.effectiveName}")
            }
            selectedJobName?.takeIf { it.isNotBlank() }?.let { appendLine("선택 직업: $it") }
            appendLine("결과: ${if (isSuccess) "성공" else "실패"}")
            append(message)
        }
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

    private fun activateMentalPatientFakeAbility(
        game: Game,
        caster: PlayerData,
        selectedAbility: ActiveAbility,
        target: PlayerData?,
        selectedJobName: String?
    ): AbilityResult {
        if (caster.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 능력을 사용할 수 없습니다.")
        }

        return when (selectedAbility) {
            is PoliceAbility -> buildMentalPatientMafiaCheckResult(caster, target, abilityName = "수색")
            is VigilantePurgeDayAbility -> buildMentalPatientMafiaCheckResult(caster, target, abilityName = "숙청")
            is NurseAbility -> buildMentalPatientDoctorCheckResult(game, caster, target)
            is SpyAbility -> buildMentalPatientJobDiscoveryResult(game, caster, target, "첩보")
            is ReporterAbility -> buildMentalPatientJobDiscoveryResult(game, caster, target, "특종")
            is InspectorInvestigation -> buildMentalPatientJobDiscoveryResult(game, caster, target, "수사")
            is HackerAbility -> buildMentalPatientJobDiscoveryResult(game, caster, target, "해킹")
            is SoulRelease -> buildMentalPatientSoulReleaseResult(game, target)
            is FortunetellerAbility -> buildMentalPatientFortuneResult(game, target)
            is MentalistAbility -> buildMentalPatientMentalistResult(game, caster, target)
            is AdministratorAbility -> {
                val jobName = selectedJobName?.takeIf { it.isNotBlank() }
                    ?: return AbilityResult(true, "이번 밤의 조회 대상을 해제했습니다.")
                buildMentalPatientAdministratorResult(game, caster, jobName)
            }
            else -> {
                val targetName = target?.member?.effectiveName
                val message = if (targetName == null) {
                    "${selectedAbility.name} 능력을 사용했습니다."
                } else {
                    "${targetName}님에게 ${selectedAbility.name} 능력을 사용했습니다."
                }
                AbilityResult(true, message)
            }
        }
    }

    private fun buildMentalPatientMafiaCheckResult(
        caster: PlayerData,
        target: PlayerData?,
        abilityName: String
    ): AbilityResult {
        val checkedTarget = validateMentalPatientInvestigationTarget(target) ?: return targetRequiredFailure(abilityName)
        if (abilityName == "숙청" && checkedTarget.member.id == caster.member.id) {
            return AbilityResult(false, "자기 자신은 대상으로 지정할 수 없습니다.")
        }

        val isMafia = kotlin.random.Random.nextBoolean()
        val result = if (isMafia) "마피아 입니다." else "마피아가 아닙니다."
        return AbilityResult(true, "${checkedTarget.member.effectiveName}님은 $result")
    }

    private fun buildMentalPatientDoctorCheckResult(
        game: Game,
        caster: PlayerData,
        target: PlayerData?
    ): AbilityResult {
        val checkedTarget = validateMentalPatientInvestigationTarget(target)
            ?: return AbilityResult(false, "처방 대상을 지정해야 합니다.")
        if (checkedTarget.member.id == caster.member.id) {
            return AbilityResult(false, "자기 자신은 처방 대상으로 지정할 수 없습니다.")
        }

        val doctorJobName = JobManager.findByName("의사")?.name ?: "의사"
        val shownJobName = if (kotlin.random.Random.nextBoolean()) {
            doctorJobName
        } else {
            randomDisplayedJobName(game) { it != doctorJobName }
                ?: randomJobName { it != doctorJobName }
                ?: doctorJobName
        }

        return AbilityResult(
            true,
            "${checkedTarget.member.effectiveName}님을 처방 대상으로 지정했습니다.\n${checkedTarget.member.effectiveName}님의 직업은 $shownJobName"
        )
    }

    private fun buildMentalPatientJobDiscoveryResult(
        game: Game,
        caster: PlayerData,
        target: PlayerData?,
        abilityName: String
    ): AbilityResult {
        val checkedTarget = validateMentalPatientInvestigationTarget(target) ?: return targetRequiredFailure(abilityName)
        if (checkedTarget.member.id == caster.member.id && abilityName in setOf("첩보", "해킹")) {
            return AbilityResult(false, "자기 자신은 조사할 수 없습니다.")
        }

        val shownJobName = randomDisplayedJobName(game) ?: randomJobName() ?: "시민"
        val message = when (abilityName) {
            "첩보" -> "${checkedTarget.member.effectiveName}님의 직업은 $shownJobName"
            "특종" -> "특종입니다! ${checkedTarget.member.effectiveName}님이 $shownJobName(이)라는 소식입니다!"
            "수사" -> "그 사람의 직업은 $shownJobName."
            "해킹" -> "${checkedTarget.member.effectiveName}님의 직업은 $shownJobName"
            else -> "${checkedTarget.member.effectiveName}님의 직업은 $shownJobName"
        }
        return AbilityResult(true, message)
    }

    private fun buildMentalPatientSoulReleaseResult(game: Game, target: PlayerData?): AbilityResult {
        if (target == null) {
            return AbilityResult(false, "성불할 대상을 지정해야 합니다.")
        }
        if (!target.state.isDead) {
            return AbilityResult(false, "사망한 플레이어만 성불할 수 있습니다.")
        }

        val shownJobName = randomDisplayedJobName(game) ?: randomJobName() ?: "시민"
        return AbilityResult(
            true,
            "${target.member.effectiveName}님을 성불했습니다.\n${target.member.effectiveName}님의 직업은 $shownJobName 입니다."
        )
    }

    private fun buildMentalPatientAdministratorResult(
        game: Game,
        caster: PlayerData,
        selectedJobName: String
    ): AbilityResult {
        val selectedJob = JobManager.findByName(selectedJobName)
            ?: return AbilityResult(false, "선택한 직업을 찾을 수 없습니다.")
        val hasCooperation = caster.allAbilities.any { it is Cooperation }
        val hasIdentification = caster.allAbilities.any { it is Identification }
        if (!AdministratorInvestigationPolicy.isJobSelectable(selectedJob, hasCooperation, hasIdentification)) {
            return AbilityResult(false, "현재 보유한 능력으로는 해당 직업을 조회할 수 없습니다.")
        }

        val candidates = game.playerDatas.filter { !it.state.isDead }
        val selectedPlayerName = candidates.randomOrNull()?.member?.effectiveName
        val hasResult = selectedPlayerName != null && kotlin.random.Random.nextBoolean()
        return if (hasResult) {
            AbilityResult(true, "${selectedPlayerName}님이 ${selectedJobName}로 조회되었습니다.")
        } else {
            AbilityResult(true, "$selectedJobName 직업과 일치하는 내용이 없습니다.")
        }
    }

    private fun buildMentalPatientFortuneResult(game: Game, target: PlayerData?): AbilityResult {
        if (target == null) {
            return AbilityResult(false, "운세 대상을 지정해야 합니다.")
        }
        if (target.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 운세 대상으로 지정할 수 없습니다.")
        }

        val firstJob = randomDisplayedJobName(game) ?: randomJobName() ?: "시민"
        val firstIsEvil = JobManager.findByName(firstJob) is Evil
        val secondJob = randomDisplayedJobName(game) { jobName ->
            val job = JobManager.findByName(jobName)
            job != null && job.name != firstJob && (job is Evil) != firstIsEvil
        } ?: randomJobName { jobName ->
            val job = JobManager.findByName(jobName)
            job != null && job.name != firstJob && (job is Evil) != firstIsEvil
        } ?: firstJob
        val shownJobs = listOf(firstJob, secondJob).shuffled()

        return AbilityResult(
            true,
            "${target.member.effectiveName}의 직업은 ${shownJobs[0]} 또는 ${shownJobs[1]}"
        )
    }

    private fun randomDisplayedJobName(game: Game, predicate: (String) -> Boolean = { true }): String? {
        return game.playerDatas
            .mapNotNull { FrogCurseManager.displayedJob(it)?.name }
            .filter { it != MentalPatient.JOB_NAME }
            .distinct()
            .filter(predicate)
            .randomOrNull()
    }

    private fun randomJobName(predicate: (String) -> Boolean = { true }): String? {
        return JobManager.getAll()
            .filter { it.name != MentalPatient.JOB_NAME }
            .map { it.name }
            .distinct()
            .filter(predicate)
            .randomOrNull()
    }

    private fun buildMentalPatientMentalistResult(
        game: Game,
        caster: PlayerData,
        target: PlayerData?
    ): AbilityResult {
        if (target == null) {
            return AbilityResult(false, "관찰할 대상을 지정해야 합니다.")
        }
        if (target.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 관찰 대상으로 지정할 수 없습니다.")
        }
        if (target.member.id == caster.member.id) {
            return AbilityResult(false, "자기 자신은 관찰 대상으로 지정할 수 없습니다.")
        }

        val comparisonTarget = game.playerDatas
            .filter { !it.state.isDead }
            .filter { it.member.id != caster.member.id && it.member.id != target.member.id }
            .randomOrNull()
        val comparisonName = comparisonTarget?.member?.effectiveName ?: "이전 관찰 대상"
        val relation = if (kotlin.random.Random.nextBoolean()) "같은 팀" else "다른 팀"

        return AbilityResult(
            true,
            "관찰 결과: ${comparisonName}님과 ${target.member.effectiveName}님은 서로 **${relation}**입니다."
        )
    }

    private fun validateMentalPatientInvestigationTarget(target: PlayerData?): PlayerData? {
        if (target == null) return null
        if (target.state.isDead) return null
        return target
    }

    private fun targetRequiredFailure(abilityName: String): AbilityResult {
        val message = when (abilityName) {
            "수색" -> "수색할 대상을 지정해야 합니다."
            "숙청" -> "확인할 대상을 지정해야 합니다."
            "첩보" -> "첩보 대상을 지정해야 합니다."
            "특종" -> "취재할 대상을 지정해야 합니다."
            "수사" -> "수사할 대상을 지정해야 합니다."
            "해킹" -> "해킹할 대상을 지정해야 합니다."
            else -> "조사할 대상을 지정해야 합니다."
        }
        return AbilityResult(false, message)
    }

    private fun getUsableActiveAbilities(game: Game, caster: PlayerData): List<ActiveAbility> {
        val abilitySource = (caster.job as? MentalPatient)?.activeAbilitySourceAbilities()
            ?: caster.allAbilities
        return abilitySource
            .filterIsInstance<ActiveAbility>()
            .filter { it.usablePhase == game.currentPhase }
            .filter { canUseCabalActiveAbility(game, caster, it) }
            .filter { ability ->
                if (ability is GodfatherAbility) {
                    GodfatherContactPolicy.canUseExecution(game, caster) &&
                        FrogCurseManager.canUseActiveAbility(caster, ability)
                } else {
                    FrogCurseManager.canUseActiveAbility(caster, ability)
                }
            }
    }

    private fun canUseCabalActiveAbility(game: Game, caster: PlayerData, ability: ActiveAbility): Boolean {
        if (ability !is SunCabalAbility) return true

        val sunCabal = caster.job as? Cabal ?: return true
        if (sunCabal.role != CabalRole.SUN) return true

        val moonCabal = sunCabal.pairedPlayerId
            ?.let(game::getPlayer)
            ?.job as? Cabal
        return moonCabal?.role == CabalRole.MOON && moonCabal.hasFoundSun
    }

    private fun isBlockedByUnwrittenRule(game: Game, directTarget: PlayerData?): Boolean {
        if (game.currentPhase != GamePhase.NIGHT) return false
        val blockedTargetId = game.unwrittenRuleBlockedTargetIdTonight ?: return false
        val target = directTarget ?: return false
        if (target.member.id != blockedTargetId) return false
        return target.allAbilities.any { it is UnwrittenRule }
    }

    private fun isBlockedByBlessing(
        game: Game,
        directTarget: PlayerData?,
        effectiveTarget: PlayerData?
    ): Boolean {
        return game.isBlessingProtectedTarget(directTarget) ||
            game.isBlessingProtectedTarget(effectiveTarget)
    }

    private fun deadTargetRejectedMessage(selectedAbility: ActiveAbility): String {
        return when (selectedAbility) {
            is DoctorAbility -> "이미 사망한 플레이어는 치료할 수 없습니다."
            is MafiaAbility, is GodfatherAbility -> "이미 사망한 플레이어는 처형 대상으로 지정할 수 없습니다."
            is NurseAbility -> "사망한 플레이어는 처방 대상으로 지정할 수 없습니다."
            is DetectiveAbility -> "사망한 플레이어는 추리 대상으로 지정할 수 없습니다."
            is SpyAbility -> "사망한 플레이어는 첩보 대상으로 지정할 수 없습니다."
            else -> "이미 사망한 플레이어는 대상으로 지정할 수 없습니다."
        }
    }

    private fun dev.kord.rest.builder.interaction.ChatInputCreateBuilder.registerOptions() {
        string(ABILITY_OPTION_NAME, "Select which active ability to use.") {
            required = true
            autocomplete = true
        }
        user(TARGET_OPTION_NAME, "Select a target if the ability needs one.") {
            required = false
        }
        string(JOB_OPTION_NAME, "Select a job if the ability targets a job.") {
            required = false
            autocomplete = true
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
