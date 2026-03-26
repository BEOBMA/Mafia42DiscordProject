package org.beobma.mafia42discordproject.job.ability.general.definition.list.priest

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Police
import org.beobma.mafia42discordproject.job.definition.list.Priest
import kotlin.reflect.KClass

class Exorcism : Ability, JobSpecificExtraAbility {
    override val name: String = "구마"
    override val description: String = "부활 대상이 '성불' 상태일 경우 성불을 해제하고 부활시킨다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484613754564902995/90b6a15f985f0ef9.png?ex=69beddcd&is=69bd8c4d&hm=6ca1fb761508571d3fe2bc90e89eb476b9745f68e4b4091b10b2919f9904876f&"
    override val targetJob: List<KClass<out Job>> = listOf(Priest::class)
}