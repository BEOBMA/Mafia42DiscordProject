package org.beobma.mafia42discordproject.job.ability.general.definition.list.magician

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Magician
import kotlin.reflect.KClass

class Xray : Ability, JobSpecificExtraAbility {
    override val name: String = "투시"
    override val description: String = "'트릭' 능력이 발동하고 바꿔치기에 성공한 경우, 그 사람이 누구를 투표했는지 알아낸다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/magician_ability_xray_pix_1.webp"
    override val targetJob: List<KClass<out Job>> = listOf()
}
