package org.beobma.mafia42discordproject.job.ability.general.list

import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.CommonAbility

class Megaphone : Ability, CommonAbility {
    override val name: String = "확성기"
    override val description: String = "밤 시간이 15초보다 많이 남았고 이번 밤에 사용한 사람이 없을 경우, 게임에서 단 한 번 메시지를 예약해 밤 종료 15초 전에 모두에게 공개한다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(118).webp"
}
