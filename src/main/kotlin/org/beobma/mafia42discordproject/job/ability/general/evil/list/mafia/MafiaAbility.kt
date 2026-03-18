package org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.AttackEvent
import org.beobma.mafia42discordproject.game.system.AttackTier
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility

class MafiaAbility : ActiveAbility, JobUniqueAbility {
    override val name: String = "처형"
    override val description: String = "밤마다 한 명의 플레이어를 죽일 수 있으며 마피아끼리 대화가 가능하다."
    override val usablePhase: GamePhase = GamePhase.NIGHT

    override fun activate(game: Game, caster: PlayerData, target: PlayerData?): AbilityResult {
        if (target == null) return AbilityResult(false, "대상을 지정해야 합니다.")
        if (target.state.isDead) return AbilityResult(false, "이미 죽은 플레이어입니다.")


        val hasPierce = caster.allAbilities.any { it::class == WinOrDead::class }

        // 관통이 있다면 2티어(PIERCE) 공격, 없다면 1티어(NORMAL) 공격
        val currentAttackTier = if (hasPierce) AttackTier.PIERCE else AttackTier.NORMAL


        // ==========================================
        // 3. 이벤트 큐에 공격 등록 (명령어 덮어쓰기 처리)
        // ==========================================
        // 마피아 팀은 타겟을 공유하므로 "MAFIA_TEAM"이라는 고정 키를 사용합니다.
        // 다른 마피아가 다시 명령어를 입력하면 이 Map의 특성상 마지막 타겟으로 덮어씌워집니다.
        game.nightAttacks["MAFIA_TEAM"] = AttackEvent(
            attacker = caster,
            target = target,
            attackTier = currentAttackTier
        )


        // ==========================================
        // 4. 성공 결과 반환
        // ==========================================
        // 이 메시지는 디스코드 봇을 통해 마피아 팀 채널(또는 개인 DM)로 전송됩니다.
        return AbilityResult(true, "마피아 팀의 처형 대상을 ${target.member.username}님으로 설정했습니다.")
    }
}