package org.beobma.mafia42discordproject.job.ability.general.definition.list.reporter

import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility

class Embargo : JobUniqueAbility {
    override val name: String = "엠바고"
    override val description: String = "첫 번째 낮에는 기사를 낼 수 없다."
    override val image: String = ReporterAssets.EMBARGO_ABILITY_IMAGE_URL
}
