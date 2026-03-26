package org.beobma.mafia42discordproject.job.ability.general.evil.list.hitman

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.beobma.mafia42discordproject.discord.DiscordMessageManager.sendMainChannerMessageAndSound
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GameLoopManager
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.HackerRedirectManager
import org.beobma.mafia42discordproject.job.JobManager
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import org.beobma.mafia42discordproject.job.evil.Evil
import org.beobma.mafia42discordproject.job.evil.list.HitMan
import org.beobma.mafia42discordproject.job.evil.list.Mafia

class HitManAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "청부"
    override val description: String = "두 번째 밤부터 공개적으로 능력이 사용된 대상을 제외한 시민 두 명을 지목하여 직업을 맞출 경우 둘 다 암살한다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1485287583767593093/683eeae42023178e.png?ex=69c1515b&is=69bfffdb&hm=b56804106cb11e70e684403213a6acef4fe8b650c898da7920ea31eb0daa1b11&"
    override val usablePhase: GamePhase = GamePhase.NIGHT

    fun activateWithJobName(game: Game, caster: PlayerData, target: PlayerData?, guessedJobName: String?): AbilityResult {
        if (game.currentPhase != usablePhase) return AbilityResult(false, "청부는 밤에만 사용할 수 있습니다.")
        if (game.dayCount < 2) return AbilityResult(false, "첫째 날 밤에는 청부를 사용할 수 없습니다.")
        if (caster.state.isDead) return AbilityResult(false, "사망한 플레이어는 능력을 사용할 수 없습니다.")
        if (target == null) return AbilityResult(false, "청부 대상을 지정해야 합니다.")
        if (target.state.isDead) return AbilityResult(false, "이미 사망한 플레이어는 지정할 수 없습니다.")
        if (target.member.id == caster.member.id) {
            return AbilityResult(false, "자기 자신은 청부할 수 없습니다.")
        }
        val effectiveTarget = HackerRedirectManager.resolveTarget(game, target) ?: target
        if (effectiveTarget.member.id in game.publiclyRevealedAbilityTargetIds || effectiveTarget.member.id in game.pendingEscapedPlayerIds) {
            return AbilityResult(false, "공개적으로 능력이 사용된 대상은 청부 대상으로 지정할 수 없습니다.")
        }

        val guessedJob = guessedJobName?.takeIf { it.isNotBlank() }?.let(JobManager::findByName)
            ?: return AbilityResult(false, "직업을 정확히 선택해야 합니다.")

        val hitManJob = caster.job as? HitMan ?: return AbilityResult(false, "청부업자만 사용할 수 있습니다.")
        val firstTargetId = hitManJob.firstContractTargetId

        if (firstTargetId == null) {
            hitManJob.firstContractTargetId = effectiveTarget.member.id
            hitManJob.firstContractGuessedJobName = guessedJob.name
            return AbilityResult(true, contractSelectionMessage(caster, target, effectiveTarget, guessedJob.name))
        }

        if (firstTargetId == effectiveTarget.member.id) {
            return AbilityResult(false, "서로 다른 두 명을 지목해야 합니다.")
        }

        val firstTarget = game.getPlayer(firstTargetId)
            ?: return AbilityResult(false, "첫 번째 지목 대상 정보를 찾을 수 없습니다. 다시 시도해 주세요.")
        val firstJobName = hitManJob.firstContractGuessedJobName
            ?: return AbilityResult(false, "첫 번째 직업 정보가 유실되었습니다. 다시 시도해 주세요.")

        val secondIntuition = contractSelectionMessage(caster, target, effectiveTarget, guessedJob.name)

        hitManJob.firstContractTargetId = null
        hitManJob.firstContractGuessedJobName = null

        scheduleResolution(game, caster, firstTarget, firstJobName, effectiveTarget, guessedJob.name)
        return AbilityResult(true, secondIntuition)
    }

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        return AbilityResult(false, "청부는 /use 명령에서 대상과 직업을 함께 선택해야 합니다.")
    }

    private fun contractSelectionMessage(
        caster: PlayerData,
        originalTarget: PlayerData,
        effectiveTarget: PlayerData,
        guessedJobName: String
    ): String {
        val hasIntuition = caster.job?.extraAbilities?.any { it is Intuition } == true
        if (!hasIntuition) {
            return "${originalTarget.member.effectiveName}님을 ${guessedJobName}(으)로 지목했습니다."
        }

        val isCorrect = effectiveTarget.job?.name == guessedJobName
        return "${originalTarget.member.effectiveName}님의 정체는 ${guessedJobName}이(가) ${if (isCorrect) "맞습니다." else "아닙니다."}"
    }

    private fun scheduleResolution(
        game: Game,
        caster: PlayerData,
        firstTarget: PlayerData,
        firstGuessedJobName: String,
        secondTarget: PlayerData,
        secondGuessedJobName: String
    ) {
        val elapsed = (System.currentTimeMillis() - game.nightPhaseStartedAtMillis).coerceAtLeast(0L)
        val delayMillis = if (elapsed < CONTRACT_TRIGGER_MILLIS) CONTRACT_TRIGGER_MILLIS - elapsed else 0L

        scope.launch {
            if (delayMillis > 0L) {
                delay(delayMillis)
            }
            resolveContract(game, caster, firstTarget, firstGuessedJobName, secondTarget, secondGuessedJobName)
        }
    }

    private suspend fun resolveContract(
        game: Game,
        caster: PlayerData,
        firstTarget: PlayerData,
        firstGuessedJobName: String,
        secondTarget: PlayerData,
        secondGuessedJobName: String
    ) {
        if (!game.isRunning || game.currentPhase != GamePhase.NIGHT || caster.state.isDead) return

        if (firstTarget.state.isDead || secondTarget.state.isDead) return
        if (firstTarget.member.id in game.pendingNightDeathPlayerIds || secondTarget.member.id in game.pendingNightDeathPlayerIds) return

        val soldierTarget = listOf(firstTarget, secondTarget).firstOrNull { it.job is Soldier }
        if (soldierTarget != null) {
            sendSoldierCriticalMessages(caster, soldierTarget)
            return
        }

        val hasMafiaGuess = (firstTarget.job is Mafia && firstGuessedJobName == firstTarget.job?.name) ||
            (secondTarget.job is Mafia && secondGuessedJobName == secondTarget.job?.name)

        if (hasMafiaGuess) {
            val hitManJob = caster.job as? HitMan ?: return
            hitManJob.hasContactedMafia = true
            GameLoopManager.notifyHitmanContact(game, caster)
            return
        }

        val firstCorrect = firstTarget.job?.name == firstGuessedJobName
        val secondCorrect = secondTarget.job?.name == secondGuessedJobName
        if (!firstCorrect || !secondCorrect) return
        if (firstTarget.job is Evil || secondTarget.job is Evil) return

        killByContract(game, listOf(firstTarget, secondTarget))
    }

    private suspend fun killByContract(game: Game, targets: List<PlayerData>) {
        val aliveTargets = targets.filterNot { it.state.isDead }
        if (aliveTargets.isEmpty()) return

        aliveTargets.forEach { target ->
            game.pendingNightDeathPlayerIds += target.member.id
            if (target !in game.nightDeathCandidates) {
                game.nightDeathCandidates += target
            }
        }

        val victims = aliveTargets.joinToString(separator = "\n") {
            "${it.member.effectiveName}님이 청부업자에게 정체를 들켜 암살 당했습니다."
        }
        val message = "$CONTRACT_KILL_IMAGE_URL\n$victims"
        game.sendMainChannerMessageAndSound(message, CONTRACT_SUCCESS_SOUND_PATH)
    }

    private fun sendSoldierCriticalMessages(caster: PlayerData, soldierTarget: PlayerData) {
        scope.launch {
            runCatching {
                caster.member.getDmChannel().createMessage(
                    "$SOLDIER_CRITICAL_IMAGE_URL\n군인 ${soldierTarget.member.effectiveName}님에게 간파당하여 암살에 실패했습니다."
                )
            }
            runCatching {
                soldierTarget.member.getDmChannel().createMessage(
                    "$SOLDIER_CRITICAL_IMAGE_URL\n청부업자 ${caster.member.effectiveName}님의 정체를 알아냈습니다."
                )
            }
        }
    }

    companion object {
        private const val CONTRACT_TRIGGER_MILLIS = 10_000L
        private const val CONTRACT_KILL_IMAGE_URL = "https://cdn.discordapp.com/attachments/1483977619258212392/1485091092427833406/5z9EhKho2HssoyyRznzH5XjK_7lKYQC3u18N9CAvPlOljQqpD6rnNZqyJgj7PaMLy3qCs327-KWX7XG_8Go_MmWHxZqFI5o8n8UJhJwJP7m4o_5TVKJluxpw9-F9Bp0HyzK2IhOxShVigiYTl_JdeA.webp?ex=69c09a5c&is=69bf48dc&hm=7c25a110ad9ff8df979f7923450772100e1a7bd2d5c32b0d60c25efd94800f81&"
        private const val CONTRACT_SUCCESS_SOUND_PATH = "C:/Users/ssdss/Desktop/유틸리티/마피아/청부업자 암살.mp3"
        private const val SOLDIER_CRITICAL_IMAGE_URL = "https://cdn.discordapp.com/attachments/1483977619258212392/1485090345401454592/OtwXuKPgGL4H1g3iEOvsMx3Yva11Kov5MqnNVWhdJjUiJjsAQ9xy-0g3DTtKXK7ajqUXDd01al63a1KAQGZGDl09lRt5tWeJNZH7Pe3dh8x2f2DwlA82gWa7n0QnqdTbrtVgjcL_S12mfM_vs1siYQ.webp?ex=69c099a9&is=69bf4829&hm=1486b687b5177efc46f0bbe2c0718bb3b9c7d78785d2cf4ff60343cf53ee359c&"
        val scope = CoroutineScope(Dispatchers.Default)
    }
}
