package org.beobma.mafia42discordproject.job.ability.general.list

import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.CommonAbility

class Perjury : Ability, CommonAbility {
    override val name: String = "위증"
    override val description: String = "매일 투표 시간에 가짜 투표를 행사할수 있다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(196).webp"
}