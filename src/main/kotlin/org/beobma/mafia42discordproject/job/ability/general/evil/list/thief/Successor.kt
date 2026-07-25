package org.beobma.mafia42discordproject.job.ability.general.evil.list.thief

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.evil.list.Thief
import kotlin.reflect.KClass

class Successor : Ability, JobSpecificExtraAbility {
    override val name: String = "후계자"
    override val description: String = "마피아가 존재하지 않을 때 자기자신을 훔칠 경우 마피아의 능력을 얻을 수 있으며, 이 상태에서 쏘는 총은 무조건 적중한다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/thief_ability_successor.webp"
    override val targetJob: List<KClass<out Job>> = listOf(Thief::class)
}