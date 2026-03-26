package org.beobma.mafia42discordproject.job.ability.general.definition.list.mentalist

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Mentalist
import kotlin.reflect.KClass

class Profiling : Ability, JobSpecificExtraAbility {
    override val name: String = "프로파일링"
    override val description: String = "관찰을 통해 팀이 같은 플레이어를 알아낸 경우, 처음과 마지막으로 조사한 플레이어 둘 중 한 명이 능력을 사용한 플레이어를 알 수 있다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(98).webp"
    override val targetJob: List<KClass<out Job>> = listOf(Mentalist::class)
}