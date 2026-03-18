package org.beobma.mafia42discordproject.game

import org.beobma.mafia42discordproject.game.player.PlayerData

data class Game(
    var playerDatas: MutableList<PlayerData>
)