package org.beobma.mafia42discordproject.job.ability.general.definition.list

import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.CitizenCommonAbility
import org.beobma.mafia42discordproject.job.ability.CommonAbility
import org.beobma.mafia42discordproject.job.ability.EvilCommonAbility

class Belongings : Ability, CitizenCommonAbility {
    override val name: String = "유품"
    override val description: String = "사망 시 자신의 직업을 모든 플레이어에게 공개한다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(182).webp"
}