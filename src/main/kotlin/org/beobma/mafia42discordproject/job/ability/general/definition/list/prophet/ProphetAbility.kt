package org.beobma.mafia42discordproject.job.ability.general.definition.list.prophet

import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility

class ProphetAbility : Ability, JobUniqueAbility {
    override val name: String = "계시"
    override val description: String = "네번째 낮까지 생존할 경우 자신이 속한 팀이 승리한다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(180).webp"
}