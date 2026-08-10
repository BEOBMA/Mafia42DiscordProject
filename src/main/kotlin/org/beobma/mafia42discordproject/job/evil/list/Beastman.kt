package org.beobma.mafia42discordproject.job.evil.list

import dev.kord.common.entity.Snowflake
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.evil.list.beastman.BeastmanAbility
import org.beobma.mafia42discordproject.job.ability.general.evil.list.beastman.BeastmanAgility
import org.beobma.mafia42discordproject.job.evil.Evil

class Beastman : Job(), Evil {
    override val name: String = "짐승인간"
    override val description: String = "[갈망] 밤에 선택한 플레이어가 마피아에게 처형되면 마피아에게 길들여진다. 길들여진 후 마피아의 일반 처형과 동일한 판정으로 플레이어를 제거할 수 있다.\n[민첩] 마피아의 공격으로부터 죽지 않는다."
    override val jobImage: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(87).webp"
    override val abilities: MutableList<JobUniqueAbility> = mutableListOf(BeastmanAbility(), BeastmanAgility())

    var cravingTargetIdTonight: Snowflake? = null
}
