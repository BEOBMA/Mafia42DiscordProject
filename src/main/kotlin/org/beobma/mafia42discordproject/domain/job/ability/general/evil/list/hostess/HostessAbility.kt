package org.beobma.mafia42discordproject.job.ability.general.evil.list.hostess

import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility

class HostessAbility : PassiveAbility, JobUniqueAbility {
    override val name: String = "유혹"
    override val description: String = "투표한 사람을 마담에게 유혹 당한 상태로 만들며, 마피아라면 접선한다. 유혹 당한 상태에서는 직업 능력을 사용할 수 없으며, 일시적으로 말을 할 수 없게 된다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1485292623462928507/9b93a3210a6cd47f.png?ex=69c1560c&is=69c0048c&hm=bfe93e6655b3a9240a3ac763a9cbe368c980ec0937fc5af6689de7620bd9b7ed&"
}