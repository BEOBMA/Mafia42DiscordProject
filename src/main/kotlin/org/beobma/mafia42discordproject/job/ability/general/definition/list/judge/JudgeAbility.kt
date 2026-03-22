package org.beobma.mafia42discordproject.job.ability.general.definition.list.judge

import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility

class JudgeAbility : Ability, JobUniqueAbility {
    override val name: String = "선고"
    override val description: String = "찬성 반대를 정할 수 있으며 찬성 반대의 결과가 자신의 선택과 다를 경우, 모습을 드러내며 모습을 드러낸 이후 모든 투표의 결과는 판사에 의해서만 결정된다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1485331072542834830/e4c4fd417a14ceb1.png?ex=69c179db&is=69c0285b&hm=8ba14d001ef990b5f75f894cec96ae9ef6792688132b4dfdb939ddf30c484ff3&"
}