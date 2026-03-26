package org.beobma.mafia42discordproject.job.ability.general.definition.list.prophet

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Prophet
import kotlin.reflect.KClass

class Apostle : Ability, JobSpecificExtraAbility {
    override val name: String = "사도"
    override val description: String = "시민팀 플레이어 중 마지막 생존자가 된다면 즉시 '계시' 능력이 발동된다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484613954583003156/8ae3e8eaaee9e729.png?ex=69beddfd&is=69bd8c7d&hm=d42f6c4cd61ff2cc6845a8d1b88dea9a87a5e7e72acb30bad71e995fab8702ba&"
    override val targetJob: List<KClass<out Job>> = listOf(Prophet::class)
}