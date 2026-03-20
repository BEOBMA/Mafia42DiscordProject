package org.beobma.mafia42discordproject.job.ability.general.definition.list.administrator

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.definition.list.Administrator
import org.beobma.mafia42discordproject.job.definition.list.Detective
import org.beobma.mafia42discordproject.job.definition.list.Gangster
import org.beobma.mafia42discordproject.job.definition.list.Ghoul
import org.beobma.mafia42discordproject.job.definition.list.Martyr
import org.beobma.mafia42discordproject.job.definition.list.Nurse
import org.beobma.mafia42discordproject.job.definition.list.Shaman
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import kotlin.reflect.KClass

class Identification : Ability, JobSpecificExtraAbility {
    override val name: String = "색출"
    override val description: String = "'조회' 능력 대상으로 마피아팀 보조직업을 선택할 수 있다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484602826129801216/7e864bd1ba9365ab.png?ex=69bed3a0&is=69bd8220&hm=92b566f1729cf2f01cf23afb825eccdba07dbd080d94790576093194f3d6e2a4&"
    override val targetJob: List<KClass<out Job>> = listOf(Administrator::class)
}