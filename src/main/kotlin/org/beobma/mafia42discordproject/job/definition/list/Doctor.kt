package org.beobma.mafia42discordproject.job.definition.list

import dev.kord.common.entity.Snowflake
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.doctor.DoctorAbility
import org.beobma.mafia42discordproject.job.definition.Definition

class Doctor : Job(), Definition {
    override val name: String = "의사"
    override val description: String = "[치료] 밤에 플레이어 한 명을 치료해 일반 공격으로부터 보호합니다."
    override val jobImage: String = "https://media.discordapp.net/attachments/1483977619258212392/1484548916262211685/chrome_AHryc1iYFs.png?ex=69bea16b&is=69bd4feb&hm=f88159583ec160425e96a7eeb341d01a80eb0abd17a0ef58c4f23dbc84fa3486&=&format=webp&quality=lossless"
    var currentHealTarget: Snowflake? = null
    var hasContactedNurse: Boolean = false
    override val abilities: MutableList<JobUniqueAbility> = mutableListOf(DoctorAbility())
}
