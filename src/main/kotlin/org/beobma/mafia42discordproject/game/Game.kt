package org.beobma.mafia42discordproject.game

import org.beobma.mafia42discordproject.game.player.PlayerData

data class Game(
    val playerDatas: MutableList<PlayerData>
)