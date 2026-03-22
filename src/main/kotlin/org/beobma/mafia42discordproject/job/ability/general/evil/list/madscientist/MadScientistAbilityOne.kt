package org.beobma.mafia42discordproject.job.ability.general.evil.list.madscientist

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.AssistanceCommonAbility
import org.beobma.mafia42discordproject.job.ability.JobSpecificExtraAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.evil.list.MadScientist
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import org.beobma.mafia42discordproject.job.evil.list.Spy
import kotlin.reflect.KClass

class MadScientistAbilityOne : PassiveAbility, JobUniqueAbility {
    override val name: String = "재생"
    override val description: String = "사망할 경우, 마피아와 접선하며 한번에 한하여 다음 밤에 부활한다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1485292087103586454/b38a5ab22f4673c3.png?ex=69c1558c&is=69c0040c&hm=3e6edae08a86160825fac6937c59a03558bcb6f69c0d087ffb1fb0f77a744f9d&"
}