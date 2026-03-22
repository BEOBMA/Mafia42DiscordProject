package org.beobma.mafia42discordproject.job.evil.list

import dev.kord.common.entity.Snowflake
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.evil.list.swindler.SwindlerAbility
import org.beobma.mafia42discordproject.job.ability.general.evil.list.swindler.SwindlerFraud
import org.beobma.mafia42discordproject.job.evil.Evil

class Swindler : Job(), Evil {
    override val name: String = "사기꾼"
    override val description: String = "[사기] 게임 시작 시 시민 한 명의 정체를 알아내고 그 직업으로 변장한다.\n[교섭] 사기 대상 또는 사기꾼이 처형 능력 대상이 된 경우, 사기꾼은 처형되지 않으며 처형 여부와 관계 없이 마피아와 접선한다."
    override val jobImage: String = "https://media.discordapp.net/attachments/1483977619258212392/1484548930426503238/chrome_8Ml52OJmjP.png?ex=69bea16e&is=69bd4fee&hm=f0eed0be0eada5eb5b0efbb785c6588272071eb3648a91519d215a9acbd5efdb&=&format=webp&quality=lossless"
    override val abilities: MutableList<JobUniqueAbility> = mutableListOf(SwindlerAbility(), SwindlerFraud())

    var disguisedTargetId: Snowflake? = null
    var disguisedJobName: String? = null
    var hasAttemptedFraud: Boolean = false
    var hasContactedMafia: Boolean = false
}
