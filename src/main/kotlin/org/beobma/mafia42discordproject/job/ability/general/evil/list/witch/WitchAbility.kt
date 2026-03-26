package org.beobma.mafia42discordproject.job.ability.general.evil.list.witch

import dev.kord.core.behavior.channel.createMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GameLoopManager
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.FrogCurseManager
import org.beobma.mafia42discordproject.game.system.HackerRedirectManager
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import org.beobma.mafia42discordproject.job.evil.list.Witch

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

        val witch = caster.job as? Witch ?: return AbilityResult(false, "마녀만 사용할 수 있습니다.")
        val effectiveTarget = HackerRedirectManager.resolveTarget(game, target) ?: target
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
        val nightEndsAt = game.nightPhaseStartedAtMillis + NIGHT_DURATION_MS
        val curseAt = nightEndsAt - CURSE_DELAY_BEFORE_NIGHT_END_MS
        val delayMillis = curseAt - now
        if (delayMillis <= 0L) {
            applyCurseNow(game, caster, witch, effectiveTarget, notifyTarget = true)
            return AbilityResult(true, "${target.member.effectiveName}님에게 즉시 저주를 걸었습니다.")
        }

        scope.launch {
            delay(delayMillis)
            if (game.currentPhase != GamePhase.NIGHT) return@launch
            val selectedTargetId = game.pendingWitchCurseByCaster[caster.member.id] ?: return@launch
            if (selectedTargetId != effectiveTarget.member.id) return@launch
            applyCurseNow(game, caster, witch, effectiveTarget, notifyTarget = true)
        }
        return AbilityResult(true, "${target.member.effectiveName}님에게 저주를 걸었습니다.")
    }

    companion object {
        private const val NIGHT_DURATION_MS = 25_000L
        private const val CURSE_DELAY_BEFORE_NIGHT_END_MS = 10_000L
        private const val WITCH_CONTACT_IMAGE_URL =
            "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(12).webp"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        fun applyOblivionCursesAtNightEnd(game: Game) {
            val targets = game.pendingOblivionCurseByCaster.values.toSet()
            game.pendingOblivionCurseByCaster.clear()
            targets.forEach { targetId ->
                val target = game.getPlayer(targetId) ?: return@forEach
                if (target.state.isDead) return@forEach
                FrogCurseManager.applyCurse(target, game.dayCount)
                scope.launch {
                    runCatching {
                        target.member.getDmChannel().createMessage("저주를 받아 개구리가 되었습니다.")
                    }
                }
            }
        }

        private fun applyCurseNow(
            game: Game,
            caster: PlayerData,
            witch: Witch,
            target: PlayerData,
            notifyTarget: Boolean
        ) {
            FrogCurseManager.applyCurse(target, game.dayCount)
            if (notifyTarget) {
                scope.launch {
                    runCatching {
                        target.member.getDmChannel().createMessage("저주를 받아 개구리가 되었습니다.")
                    }
                }
            }
            if (target.job is Mafia && !witch.hasContactedMafia) {
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
        }
    }
}
