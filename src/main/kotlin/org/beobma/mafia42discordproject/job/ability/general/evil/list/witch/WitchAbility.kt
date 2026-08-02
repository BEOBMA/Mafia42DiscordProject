package org.beobma.mafia42discordproject.job.ability.general.evil.list.witch

import kotlinx.coroutines.*
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GameLoopManager
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.loop.NIGHT_DURATION_MS
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.replay.GameReplayLogger
import org.beobma.mafia42discordproject.game.system.FrogCurseManager
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.definition.Definition
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import org.beobma.mafia42discordproject.job.evil.list.Thief
import org.beobma.mafia42discordproject.job.evil.list.Witch
import kotlin.time.Duration.Companion.milliseconds

class WitchAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "저주"
    override val description: String = "밤마다 플레이어 한 명의 닉네임을 적어 다음날 낮이 완전히 종료될 때까지 개구리로 변신시킨다. 마피아를 저주할 경우, 마피아와 접선한다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(103).webp"
    override val usablePhase: GamePhase = GamePhase.NIGHT

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase) {
            return AbilityResult(false, "저주는 밤에만 사용할 수 있습니다.")
        }
        if (caster.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 저주를 사용할 수 없습니다.")
        }
        if (target == null) {
            return AbilityResult(false, "저주 대상을 지정해야 합니다.")
        }
        if (target.member.id == caster.member.id) {
            return AbilityResult(false, "자기 자신을 저주할 수 없습니다.")
        }
        if (target.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 저주할 수 없습니다.")
        }

        val witch = caster.job as? Witch
        val thief = caster.job as? Thief
        if (witch == null && thief == null) {
            return AbilityResult(false, "마녀 또는 저주 능력을 훔친 도둑만 사용할 수 있습니다.")
        }
        val effectiveTarget = target
        if (effectiveTarget.member.id == caster.member.id) {
            return AbilityResult(false, "자기 자신을 저주할 수 없습니다.")
        }
        if (effectiveTarget.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 저주할 수 없습니다.")
        }

        game.pendingWitchCurseByCaster[caster.member.id] = effectiveTarget.member.id
        val hasOblivion = caster.allAbilities.any { it is Oblivion }
        if (hasOblivion) {
            game.pendingOblivionCurseByCaster[caster.member.id] = effectiveTarget.member.id
            return AbilityResult(true, "${target.member.effectiveName}님에게 저주를 걸었습니다.")
        }
        game.pendingOblivionCurseByCaster.remove(caster.member.id)

        val now = System.currentTimeMillis()
        val delayMillis = delayUntilNormalCurse(
            nightStartedAtMillis = game.nightPhaseStartedAtMillis,
            nowMillis = now
        )
        if (delayMillis <= 0L) {
            applyCurseNow(
                game = game,
                caster = caster,
                witch = witch,
                thief = thief,
                target = effectiveTarget,
                notifyTarget = true,
                hiddenFromTarget = false
            )
            return AbilityResult(true, "${target.member.effectiveName}님에게 즉시 저주를 걸었습니다.")
        }

        scope.launch {
            delay(delayMillis.milliseconds)
            if (game.currentPhase != GamePhase.NIGHT) return@launch
            val selectedTargetId = game.pendingWitchCurseByCaster[caster.member.id] ?: return@launch
            if (selectedTargetId != effectiveTarget.member.id) return@launch
            applyCurseNow(
                game = game,
                caster = caster,
                witch = witch,
                thief = thief,
                target = effectiveTarget,
                notifyTarget = true,
                hiddenFromTarget = false
            )
        }
        return AbilityResult(true, "${target.member.effectiveName}님에게 저주를 걸었습니다.")
    }

    companion object {
        private const val CURSE_DELAY_BEFORE_NIGHT_END_MS = 10_000L
        private const val WITCH_CONTACT_IMAGE_URL =
            "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(12).webp"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        internal fun delayUntilNormalCurse(
            nightStartedAtMillis: Long,
            nowMillis: Long
        ): Long {
            val nightEndsAt = nightStartedAtMillis + NIGHT_DURATION_MS
            val remainingNightMillis = (nightEndsAt - nowMillis).coerceAtLeast(0L)
            return (remainingNightMillis - CURSE_DELAY_BEFORE_NIGHT_END_MS).coerceAtLeast(0L)
        }

        fun applyOblivionCursesAtNightEnd(game: Game) {
            val pendingCurses = game.pendingOblivionCurseByCaster.toList()
            game.pendingOblivionCurseByCaster.clear()
            pendingCurses.forEach { (casterId, targetId) ->
                val caster = game.getPlayer(casterId) ?: return@forEach
                if (FrogCurseManager.shouldSuppressPassive(caster)) return@forEach
                val target = game.getPlayer(targetId) ?: return@forEach
                if (target.state.isDead) return@forEach
                applyCurseNow(
                    game = game,
                    caster = caster,
                    witch = caster.job as? Witch,
                    thief = caster.job as? Thief,
                    target = target,
                    notifyTarget = false,
                    hiddenFromTarget = true
                )
            }
        }

        private fun applyCurseNow(
            game: Game,
            caster: PlayerData,
            witch: Witch?,
            thief: Thief?,
            target: PlayerData,
            notifyTarget: Boolean,
            hiddenFromTarget: Boolean
        ) {
            if (FrogCurseManager.shouldSuppressPassive(caster)) return
            if (GameLoopManager.shouldIgnoreHarmfulEffectByMentalStrength(game, target)) return
            FrogCurseManager.applyCurse(
                target = target,
                currentDay = game.dayCount,
                hiddenFromTarget = hiddenFromTarget,
                hallucinatedAsMafia =
                    caster.allAbilities.any { it is Hallucination } &&
                        target.job is Definition
            )
            scope.launch {
                GameLoopManager.refreshCoupleChannelAccess(game)
            }
            if (notifyTarget) {
                scope.launch {
                    runCatching {
                        val message = "저주를 받아 개구리가 되었습니다."
                        GameReplayLogger.logDirectMessage(game, target, message, "마녀 저주")
                        target.member.getDmChannel().createMessage(message)
                    }
                }
            }
            if (target.job is Mafia && witch != null && !witch.hasContactedMafia) {
                witch.hasContactedMafia = true
                scope.launch {
                    runCatching {
                        GameLoopManager.announceMafiaSupportContact(game, caster, WITCH_CONTACT_IMAGE_URL)
                    }
                    runCatching {
                        GameLoopManager.refreshMafiaChannelContactState(game)
                    }
                }
            }
            if (target.job is Mafia && thief != null && !thief.hasContactedMafiaByStolenWitch) {
                thief.hasContactedMafiaByStolenWitch = true
                thief.hasContactedMafia = true
                scope.launch {
                    runCatching {
                        GameLoopManager.announceMafiaSupportContact(game, caster, WITCH_CONTACT_IMAGE_URL, "도둑")
                    }
                    runCatching {
                        GameLoopManager.refreshMafiaChannelContactState(game)
                    }
                }
            }
        }
    }
}
