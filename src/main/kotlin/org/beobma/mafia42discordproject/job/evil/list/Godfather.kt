package org.beobma.mafia42discordproject.job.evil.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.evil.list.godfather.GodfatherAbility
import org.beobma.mafia42discordproject.job.ability.general.evil.list.godfather.GodfatherAbilityTwo
import org.beobma.mafia42discordproject.job.evil.Evil

class Godfather : Job(), Evil {
    override val name: String = "대부"
    override val description: String = "[배후] 세번째 밤이 될 때 마피아와 접선한다.\n[말살] 접선 후 밤마다 다른 플레이어의 능력을 무시하고 처형할 수 있다."
    override val jobImage: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(52).webp"

    override val abilities: MutableList<JobUniqueAbility> = mutableListOf(GodfatherAbilityTwo(), GodfatherAbility())
}
