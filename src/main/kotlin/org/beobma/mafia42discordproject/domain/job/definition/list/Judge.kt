package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.judge.JudgeAbility
import org.beobma.mafia42discordproject.job.definition.Definition

class Judge : Job(), Definition {
    override val name: String = "판사"
    override val description: String = "[선고] 찬성/반대의 결과가 자신의 선택과 다를 경우, 모습을 드러내며, 판사의 선택에 의해 찬성/반대가 결정된다. 모습을 드러낸 이후, 모든 투표의 결과는 판사에 의해서만 결정된다."
    override val jobImage: String = "https://media.discordapp.net/attachments/1483977619258212392/1484548885946040381/chrome_RulMoVbMnM.png?ex=69bea163&is=69bd4fe3&hm=4158fba6f3ff90640b4c7bdfa57891532975dd8b00cad09526b69bc10846cca4&=&format=webp&quality=lossless"
    override val abilities: MutableList<JobUniqueAbility> = mutableListOf(JudgeAbility())

    var hasRevealedAuthority: Boolean = false
}
