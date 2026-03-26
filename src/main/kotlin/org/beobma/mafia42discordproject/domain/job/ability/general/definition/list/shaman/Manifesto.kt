package org.beobma.mafia42discordproject.job.ability.general.definition.list.shaman

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Shaman
import kotlin.reflect.KClass

class Manifesto : Ability, JobSpecificExtraAbility {
    override val name: String = "강령"
    override val description: String = "성불 상태인 플레이어의 대화를 들을 수 있다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484615080682197172/e7773b6b4df39801.png?ex=69bedf0a&is=69bd8d8a&hm=07211110ed28045f754c927fdf77ba500ecf212d43f69cf3dbbcae56d0788725&"
    override val targetJob: List<KClass<out Job>> = listOf(Shaman::class)
}