package org.beobma.mafia42discordproject.job.ability.general.evil.list

import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.CommonAbility
import org.beobma.mafia42discordproject.job.ability.EvilCommonAbility

class Password : Ability, EvilCommonAbility {
    override val name: String = "암구호"
    override val description: String = "암구호 채팅은 자신의 팀에게만 보이게 된다. 이 채팅은 낮이 된 직후의 제한을 무시할 수 있다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(123).webp"
}