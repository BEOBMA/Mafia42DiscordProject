package org.beobma.mafia42discordproject.job.ability.general.list

import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.CommonAbility

class Innocence : Ability, CommonAbility {
    override val name: String = "결백"
    override val description: String = "내가 다른 팀 플레이어를 투표했을 때 그 사람이 나를 투표하지 않았을 경우, 결백상태가 되어 나에 대한 투표가 무산된다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(105).webp"
}