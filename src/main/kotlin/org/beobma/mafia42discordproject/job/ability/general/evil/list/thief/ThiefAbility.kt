package org.beobma.mafia42discordproject.job.ability.general.evil.list.thief

import dev.kord.core.behavior.channel.createMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GameLoopManager
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.MafiaAbility
import org.beobma.mafia42discordproject.job.definition.list.Judge
import org.beobma.mafia42discordproject.job.definition.list.Politician
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import org.beobma.mafia42discordproject.job.evil.list.Thief

class ThiefAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "도벽"
    override val description: String = "투표시간마다 원하는 플레이어의 고유 능력을 훔쳐 밤까지 사용할 수 있다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(133).webp"
    override val usablePhase: GamePhase = GamePhase.VOTE

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (game.currentPhase != usablePhase) {
            return AbilityResult(false, "도벽은 투표 시간에만 사용할 수 있습니다.")
        }
        if (caster.state.isDead) {
            return AbilityResult(false, "사망한 플레이어는 도벽을 사용할 수 없습니다.")
        }

        val thief = caster.job as? Thief ?: return AbilityResult(false, "도둑만 사용할 수 있습니다.")
        if (target == null) {
            return AbilityResult(false, "대상을 지정해야 합니다.")
        }
        if (target.member.id == caster.member.id) {
            return AbilityResult(false, "자기 자신의 능력은 훔칠 수 없습니다.")
        }
        if (target.state.isDead && !thief.hasCondolences()) {
            return AbilityResult(false, "사망한 플레이어의 능력은 훔칠 수 없습니다.")
        }

        val targetJob = target.job ?: return AbilityResult(false, "대상의 직업 정보를 확인할 수 없습니다.")

        if (targetJob is Mafia) {
            if (thief.hasSuccessor() && isAliveMafiaAbsent(game)) {
                val successorAbility = instantiateAbility(MafiaAbility())
                thief.setStolenAbility(successorAbility)
                notifyStealSuccess(caster, target, targetJob.name)
                return AbilityResult(true, "**${target.member.effectiveName}님의 직업 ${targetJob.name}을 훔쳤습니다.**")
            }
            thief.hasContactedMafia = true
            notifyThiefContact(game, caster)
            return AbilityResult(true, "마피아 팀과 접선했습니다.")
        }

        if (targetJob is Soldier) {
            notifyStealFailedOnSoldier(caster, target)
            return AbilityResult(true, "훔치는 데 실패했습니다.")
        }

        if (targetJob is Politician && thief.hasStolenPoliticianAbility) {
            return AbilityResult(false, "정치인의 능력은 게임당 1회만 훔칠 수 있습니다.")
        }
        if (targetJob is Judge && thief.hasStolenJudgeAbility) {
            return AbilityResult(false, "판사의 능력은 게임당 1회만 훔칠 수 있습니다.")
        }

        val targetAbility = targetJob.abilities
            .filterIsInstance<ActiveAbility>()
            .firstOrNull { it.name != name } as? JobUniqueAbility
            ?: return AbilityResult(false, "훔칠 수 있는 고유 능력이 없습니다.")
        val stolenAbility = instantiateAbility(targetAbility)

        thief.setStolenAbility(stolenAbility)
        if (targetJob is Politician) {
            thief.hasStolenPoliticianAbility = true
        }
        if (targetJob is Judge) {
            thief.hasStolenJudgeAbility = true
        }

        notifyStealSuccess(caster, target, targetJob.name)
        return AbilityResult(true, "**${target.member.effectiveName}님의 직업 ${targetJob.name}을 훔쳤습니다.**")
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        private const val THIEF_STEAL_IMAGE_URL =
            "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(10).webp"
        private const val THIEF_CONTACT_IMAGE_URL =
            "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(26).webp"
        private const val THIEF_SOLDIER_FAIL_IMAGE_URL =
            "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(39).webp"

        private fun notifyStealSuccess(caster: PlayerData, target: PlayerData, targetJobName: String) {
            scope.launch {
                runCatching {
                    caster.member.getDmChannel().createMessage(
                        "**${target.member.effectiveName}님의 직업 ${targetJobName}을 훔쳤습니다.**\n$THIEF_STEAL_IMAGE_URL"
                    )
                }
            }
        }

        private fun notifyStealFailedOnSoldier(caster: PlayerData, soldierTarget: PlayerData) {
            scope.launch {
                runCatching {
                    caster.member.getDmChannel().createMessage("**훔치는 데 실패했습니다.**\n$THIEF_SOLDIER_FAIL_IMAGE_URL")
                }
                runCatching {
                    soldierTarget.member.getDmChannel().createMessage(
                        "**${caster.member.effectiveName}님이 직업을 훔치려고 시도했습니다.**\n$THIEF_SOLDIER_FAIL_IMAGE_URL"
                    )
                }
            }
        }

        private fun notifyThiefContact(game: Game, thiefPlayer: PlayerData) {
            scope.launch {
                runCatching {
                    if (!thiefPlayer.state.hasAnnouncedThiefContact) {
                        thiefPlayer.state.hasAnnouncedThiefContact = true
                        GameLoopManager.announceMafiaSupportContact(game, thiefPlayer, THIEF_CONTACT_IMAGE_URL)
                    }
                    GameLoopManager.refreshMafiaChannelContactState(game)
                }
            }
        }
    }

    private fun instantiateAbility(ability: JobUniqueAbility): JobUniqueAbility {
        return runCatching {
            val constructor = ability::class.java.getDeclaredConstructor()
            constructor.isAccessible = true
            constructor.newInstance()
        }.getOrElse {
            ability
        }
    }

    private fun isAliveMafiaAbsent(game: Game): Boolean {
        return game.playerDatas.none { !it.state.isDead && it.job is Mafia }
    }
}
