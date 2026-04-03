package org.beobma.mafia42discordproject.job.ability.general.evil.list.hostess

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.evil.list.Hostess
import kotlin.reflect.KClass

class Debut : Ability, JobSpecificExtraAbility {
    override val name: String = "데뷔"
    override val description: String = "첫날 투표시간에 처음 지목한 대상을 실제 투표 여부와 관계 없이 유혹한다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(151).webp"
    override val targetJob: List<KClass<out Job>> = listOf(Hostess::class)
}