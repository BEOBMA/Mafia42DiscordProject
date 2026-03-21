package org.beobma.mafia42discordproject.job.ability.general.definition.list.ghoul

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.GameEvent
import org.beobma.mafia42discordproject.job.JobManager
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.definition.list.Citizen
import org.beobma.mafia42discordproject.job.evil.Evil
import org.beobma.mafia42discordproject.job.evil.list.Villain
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

// ------------------------------------
// 기본 직업 능력 1 - [도굴]
// ------------------------------------
class GraveRobbing : JobUniqueAbility, PassiveAbility {
    override val name: String = "도굴"
    override val description: String = "첫 번째 밤에 마피아팀에게 살해당한 사람의 직업을 얻으며, 도굴당한 대상에게 도굴꾼이 누구인지 알려지게 된다."
    override val image: String = "" 
    override val priority: Int = 10 

    override fun onEventObserved(game: Game, owner: PlayerData, event: GameEvent) {
        if (event !is GameEvent.ResolveDawnPresentation) return
        if (event.dayCount != 1) return

        // 1. 본인이 죽었을 때는 능력 발동 불가 (새벽 페이즈 사망자 목록에 포함된 경우)
        if (owner.state.isDead || owner in event.deaths) return

        val mafiaAttack = event.attacks.firstOrNull { it.attacker.job is Evil } ?: return
        val victim = mafiaAttack.target

        if (victim !in event.deaths) return

        val originalVictimJob = victim.job ?: return

        // 2. 대상의 직업으로 변경
        val newJob = JobManager.createByName(originalVictimJob.name) ?: return
        
        // 3. 대상의 기본 직업(고유) 능력만 가져오며, 희생자의 추가 능력(듀얼 스킬)은 빼앗지 않음
        // 단, 기존 도굴꾼 자신이 보유하고 있던 특수 능력(약탈, 유저가 선택한 듀얼 스킬 등)은 소실되지 않고 새 직업 연계
        newJob.extraAbilities.addAll(owner.job?.extraAbilities ?: emptyList())
        
        owner.job = newJob
        owner.state.isJobPubliclyRevealed = false

        // 4. 이벤트 큐를 통한 공식 직업 확인(JobDiscovered) 발생 & 피장자 일방 알림
        game.nightEvents += GameEvent.JobDiscovered(
            discoverer = owner,
            target = victim,
            actualJob = originalVictimJob,
            revealedJob = originalVictimJob,
            sourceAbilityName = name,
            resolvedAt = org.beobma.mafia42discordproject.game.system.DiscoveryStep.DAWN
        )
    }
}

// ------------------------------------
// 기본 직업 능력 2 - [약탈]
// ------------------------------------
class Looting : JobUniqueAbility, PassiveAbility {
    override val name: String = "약탈"
    override val description: String = "도굴에 성공한 경우, 도굴당한 플레이어를 시민 또는 악인으로 만든다."
    override val image: String = "" 
    override val priority: Int = 9

    override fun onEventObserved(game: Game, owner: PlayerData, event: GameEvent) {
        if (event !is GameEvent.ResolveDawnPresentation) return
        if (event.dayCount != 1) return
        
        // 도굴꾼 본인이 사망했으면 발동 안함
        if (owner.state.isDead || owner in event.deaths) return
        
        val mafiaAttack = event.attacks.firstOrNull { it.attacker.job is Evil } ?: return
        val victim = mafiaAttack.target
        if (victim !in event.deaths) return
        
        val isEvil = victim.job is Evil

        // 팀에 따라 각각 Citizen(), Villain() 클래스 인스턴스로 대체
        victim.job = if (isEvil) Villain() else Citizen()
    }
}