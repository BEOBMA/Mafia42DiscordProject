package org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.AssistanceCommonAbility
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import kotlin.reflect.KClass

class Poisoning : Ability, JobSpecificExtraAbility {
    override val name: String = "독살"
    override val description: String = "밤에 시민팀 처형에 실패한 경우, 중독 상태로 만들어 하루 뒤에 죽게 한다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484621072199319682/afc9976f1fba1d54.png?ex=69bee49e&is=69bd931e&hm=3d13c64b1511dea1c41efe56ffa15245dd9e3868500ec9180d9f82002684b6d5&"
    override val targetJob: List<KClass<out Job>> = listOf(Mafia::class)
}