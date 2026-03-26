package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.ghoul.GraveRobbing
import org.beobma.mafia42discordproject.job.ability.general.definition.list.ghoul.Looting
import org.beobma.mafia42discordproject.job.definition.Definition

class Ghoul : Job(), Definition {
    override val name: String = "도굴꾼"
    override val description: String = "[도굴] 첫 번째 밤에 마피아팀에게 살해당한 사람의 직업을 얻으며, 도굴당한 대상에게 도굴꾼이 누구인지 알려지게 된다.\n[약탈] 도굴에 성공한 경우, 도굴당한 플레이어를 시민 또는 악인으로 만든다."
    override val jobImage: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(75).webp"

    override val abilities: MutableList<JobUniqueAbility> = mutableListOf(GraveRobbing(), Looting())
}