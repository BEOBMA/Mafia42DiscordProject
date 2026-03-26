package org.beobma.mafia42discordproject.job.evil.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.evil.list.witch.WitchAbility
import org.beobma.mafia42discordproject.job.evil.Evil

class Witch : Job(), Evil {
    override val name: String = "마녀"
    override val description: String = "[저주] 밤마다 플레이어 한 명의 닉네임을 적어 다음날 낮이 완전히 종료될 때까지 개구리로 변신시킨다. 마피아를 저주할 경우, 마피아와 접선한다."
    override val jobImage: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(59).webp"
    override val abilities: MutableList<JobUniqueAbility> = mutableListOf(WitchAbility())

    var hasContactedMafia: Boolean = false
}
