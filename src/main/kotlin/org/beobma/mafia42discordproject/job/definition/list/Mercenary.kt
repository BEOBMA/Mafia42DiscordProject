package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition

class Mercenary : Job(), Definition {
    override val name: String = "용병"
    override val description: String = "[의뢰] 게임 시작 시 시민 팀 플레이어 한 명이 의뢰인으로 지정되며 낮이 될 때 의뢰를 받는다. 의뢰를 받은 후 의뢰인이 밤에 살해당할 경우, 밤마다 플레이어 한 명을 처형할 수 있게 된다."
    override val jobImage: String = "https://media.discordapp.net/attachments/1483977619258212392/1484548810255499386/chrome_sQMgt71QPo.png?ex=69bea151&is=69bd4fd1&hm=dd7d00c688ae91f600f033ef942cb102bf79437fe09fb383cd9906bfa8a288ec&=&format=webp&quality=lossless"
}