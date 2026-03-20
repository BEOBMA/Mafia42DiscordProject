package org.beobma.mafia42discordproject.job.ability.general.evil.list.hostess

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.evil.list.Hostess
import kotlin.reflect.KClass

class Debut : Ability, JobSpecificExtraAbility {
    override val name: String = "데뷔"
    override val description: String = "첫날 투표시간에 처음 지목한 대상을 실제 투표 여부와 관계 없이 유혹한다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484617340925640885/a66dac24f885cc5e.png?ex=69bee124&is=69bd8fa4&hm=9b041731c3bba96f9dfcec0d4ab7b12ddc52a651eb9910cb125b1bdaa17d1feb&"
    override val targetJob: List<KClass<out Job>> = listOf(Hostess::class)
}