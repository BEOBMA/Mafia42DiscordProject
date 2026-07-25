package org.beobma.mafia42discordproject.job.ability.general.evil.list.godfather

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.evil.list.Godfather
import kotlin.reflect.KClass

class Discipline : Ability, JobSpecificExtraAbility {
    override val name: String = "규율"
    override val description: String =
        "접선하기 전, 마피아에게 처형되면 처형되지 않고 해당 마피아를 처형하고 접선한다. " +
            "남은 마피아가 1명이라면 마피아를 처형하는 대신 다음날 밤 능력을 사용하지 못하게 하고 접선한다."
    override val image: String =
        "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/godfather_ability_discipline.webp"
    override val targetJob: List<KClass<out Job>> = listOf(Godfather::class)
}
