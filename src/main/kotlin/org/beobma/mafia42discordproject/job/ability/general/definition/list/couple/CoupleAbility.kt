package org.beobma.mafia42discordproject.job.ability.general.definition.list.couple

import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility

class CoupleAbility : JobUniqueAbility, PassiveAbility {
    override val name: String = "연애"
    override val description: String = "밤이 되면 둘만의 대화가 가능하며 두 명 모두 살아있는 상태에서 밤에 한 명이 처형당하면 상대방이 대신 죽게된다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(114).webp"
}
