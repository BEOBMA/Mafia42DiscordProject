package org.beobma.mafia42discordproject.job.ability.general.list

import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.CommonAbility

class Megaphone : Ability, CommonAbility {
    override val name: String = "확성기"
    override val description: String = "이번 밤에 확성기 능력을 사용한 사람이 없을 경우, 게임에서 단 한번만 모두를 향해 채팅을 사용할 수 있다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(118).webp"
}