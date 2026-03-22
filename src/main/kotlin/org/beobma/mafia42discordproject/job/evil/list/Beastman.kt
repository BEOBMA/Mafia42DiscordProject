package org.beobma.mafia42discordproject.job.evil.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.evil.list.beastman.BeastmanAbility
import org.beobma.mafia42discordproject.job.evil.Evil
import dev.kord.common.entity.Snowflake

class Beastman : Job(), Evil {
    override val name: String = "짐승인간"
    override val description: String = "[갈망] 밤에 선택한 플레이어에게 표식을 새긴다. 표식이 새겨진 대상이 마피아에게 선택되면 마피아에게 길들여진다. 길들여진 후 플레이어를 제거할 수 있다.\n[민첩] 마피아의 공격으로부터 죽지 않는다."
    override val jobImage: String = "https://media.discordapp.net/attachments/1483977619258212392/1484548929646366750/chrome_5ZpXersD6w.png?ex=69bea16e&is=69bd4fee&hm=3d3ec337bdcbf72b785a1b5abe95a0cee739359161f3263e979f5cec7029ee5c&=&format=webp&quality=lossless"
    override val abilities: MutableList<JobUniqueAbility> = mutableListOf(BeastmanAbility())

    val markedTargetIds: MutableSet<Snowflake> = mutableSetOf()
}
