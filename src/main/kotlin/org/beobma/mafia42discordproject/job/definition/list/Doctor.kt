package org.beobma.mafia42discordproject.job.definition.list

import dev.kord.common.entity.Snowflake
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.doctor.DoctorAbility
import org.beobma.mafia42discordproject.job.definition.Definition

class Doctor : Job(), Definition {
    override val name: String = "의사"
    override val description: String = "[치료] 밤이 되면 플레이어 한 명을 처형으로부터 치료한다."
    override val jobImage: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(74).webp"
    var currentHealTarget: Snowflake? = null
    var hasContactedNurse: Boolean = false
    override val abilities: MutableList<JobUniqueAbility> = mutableListOf(DoctorAbility())
}
