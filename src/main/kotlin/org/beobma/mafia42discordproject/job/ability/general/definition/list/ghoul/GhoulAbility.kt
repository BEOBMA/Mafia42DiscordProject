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

// ------------------------------------
// 기본 직업 능력 1 - [도굴]
// ------------------------------------
class GraveRobbing : JobUniqueAbility, PassiveAbility {
    override val name: String = "도굴"
    override val description: String = "첫 번째 밤에 마피아팀에게 살해당한 사람의 직업을 얻으며, 도굴당한 대상에게 도굴꾼이 누구인지 알려지게 된다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1485330050608922930/5cdd63667a7c2d0c.png?ex=69c178e8&is=69c02768&hm=6d57d1c4159137d6ea6c1071008994f1cffbd3277fd3d6839df6b04a3db93ccf&"
    override val priority: Int = 10

    override fun onEventObserved(game: Game, owner: PlayerData, event: GameEvent) {
        if (event !is GameEvent.ResolveDawnPresentation) return
        if (event.dayCount != 1) return

        // 1. 본인이 죽었을 때는 능력 발동 불가 (새벽 페이즈 사망자 목록에 포함된 경우)
        if (owner.state.isDead || owner in event.deaths) return

        val mafiaAttack = event.attacks.firstOrNull { it.attacker.job is Evil } ?: return
        val victim = mafiaAttack.target

        if (victim !in event.deaths) return

        val ownerExtras = owner.job?.extraAbilities?.toList() ?: emptyList()
        val originalVictimJob = game.probationOriginalJobsByPlayer[victim.member.id] ?: victim.job ?: return

        // 2. 대상의 직업으로 변경
        val newJob = JobManager.createByName(originalVictimJob.name) ?: return

        // 3. 도굴꾼 자신의 부가 능력은 항상 유지
        val mergedExtras = LinkedHashMap<String, org.beobma.mafia42discordproject.job.ability.Ability>()
        ownerExtras.forEach { ability -> mergedExtras.putIfAbsent(ability::class.qualifiedName ?: ability.name, ability) }

        // [계승]이 있으면 도굴 대상이 보유한 부가 능력까지 함께 계승
        val hasSuccession = ownerExtras.any { it is Succession }
        if (hasSuccession) {
            originalVictimJob.extraAbilities.forEach { ability ->
                mergedExtras.putIfAbsent(ability::class.qualifiedName ?: ability.name, ability)
            }
        }

        newJob.extraAbilities.addAll(mergedExtras.values)

        owner.job = newJob
        owner.state.isJobPubliclyRevealed = false
        game.graveRobTargetsByGhoul[owner.member.id] = victim.member.id

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

        val originalVictimJob = game.probationOriginalJobsByPlayer[victim.member.id] ?: victim.job ?: return
        val isEvil = originalVictimJob is Evil

        // 팀에 따라 각각 Citizen(), Villain() 클래스 인스턴스로 대체
        victim.job = if (isEvil) Villain() else Citizen()
    }
}
