package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition

class Cabal : Job(), Definition {
    override val name: String = "비밀결사"
    override val description: String = "[밀사] 배정에 따라 능력을 사용할 수 있는 시점이 달라지며, 해가 달을 찾은 후 달이 해를 찾을 경우 게임에서 승리한다."
}