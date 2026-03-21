package org.beobma.mafia42discordproject.game.system

import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.job.ability.general.list.EarthboundSpirit

object ShamaningPolicy {
    fun canBeShamaned(target: PlayerData): Boolean {
        return target.allAbilities.none { it is EarthboundSpirit }
    }
}
