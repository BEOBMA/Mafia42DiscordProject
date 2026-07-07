package org.beobma.mafia42discordproject.job.ability.general.definition.list.shaman

import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility

class ShamanAbilityOne : PassiveAbility, JobUniqueAbility {
    override val name: String = "접신"
    override val description: String = "밤에 죽은 사람과 대화를 할 수 있다."
    override val image: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(49).webp"
}
