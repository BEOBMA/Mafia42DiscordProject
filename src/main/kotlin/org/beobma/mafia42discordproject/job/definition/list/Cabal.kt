package org.beobma.mafia42discordproject.job.definition.list

import dev.kord.common.entity.Snowflake
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.cabal.MoonCabalAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.cabal.SunCabalAbility
import org.beobma.mafia42discordproject.job.definition.Definition

enum class CabalRole {
    SUN,
    MOON
}

class Cabal : Job(), Definition {
    override val name: String = "비밀결사"
    override val description: String = "[밀사] 배정에 따라 능력을 사용할 수 있는 시점이 달라지며, 해가 달을 찾은 후 달이 해를 찾을 경우 게임에서 승리한다."
    override val jobImage: String = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(65).webp"

    var role: CabalRole? = null
    var pairedPlayerId: Snowflake? = null

    var selectedTargetId: Snowflake? = null

    var hasFoundMoon: Boolean = false
    var wasFoundBySun: Boolean = false

    var moonMarkedSunTonight: Boolean = false
    var cabalSpecialWinReady: Boolean = false

    val notifiedPartnerDeathIds: MutableSet<Snowflake> = mutableSetOf()

    override val abilities: MutableList<JobUniqueAbility> = mutableListOf(
        SunCabalAbility(),
        MoonCabalAbility()
    )
}
