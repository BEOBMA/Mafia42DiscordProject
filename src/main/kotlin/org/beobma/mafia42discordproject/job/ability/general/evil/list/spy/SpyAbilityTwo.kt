package org.beobma.mafia42discordproject.job.ability.general.evil.list.spy

import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility

class SpyAbilityTwo : PassiveAbility, JobUniqueAbility {
    override val name: String = "첩보"
    override val description: String = "밤에 선택한 플레이어가 마피아라면 접선한다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(106).webp"
}
