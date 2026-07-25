package org.beobma.mafia42discordproject.job.ability.general.evil.list.witch

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.evil.list.Witch
import kotlin.reflect.KClass

class Hallucination : Ability, JobSpecificExtraAbility {
    override val name: String = "환각"
    override val description: String =
        "마녀의 저주를 받아 개구리가 된 대상이 시민팀이라면 개구리 대신 마피아로 표시된다."
    override val image: String =
        "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/witch_ability_hallucination.webp"
    override val targetJob: List<KClass<out Job>> = listOf(Witch::class)
}
