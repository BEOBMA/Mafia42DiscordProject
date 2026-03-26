package org.beobma.mafia42discordproject.job.ability.general.definition.list.prophet

import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility

class ProphetAbility : Ability, JobUniqueAbility {
    override val name: String = "계시"
    override val description: String = "네번째 낮까지 생존할 경우 자신이 속한 팀이 승리한다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1485334154920067192/55c3d8aefaed3adf.png?ex=69c17cba&is=69c02b3a&hm=349f9f4062aa20238073e3382a4c5641e2e1d1d77aeee5f3b30da89e6d8b5752&"
}