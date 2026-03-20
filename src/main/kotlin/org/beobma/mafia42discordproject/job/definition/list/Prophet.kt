package org.beobma.mafia42discordproject.job.definition.list

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition

class Prophet : Job(), Definition {
    override val name: String = "예언자"
    override val description: String = "[계시] 네번째 낮까지 생존 할 경우, 자신이 속한 팀이 승리한다."
    override val jobImage: String = "https://media.discordapp.net/attachments/1483977619258212392/1484548881726439534/chrome_iVJSQE83my.png?ex=69bea162&is=69bd4fe2&hm=932b1373ba3fb9e9fbe536759ed8c07803518b408ece71fb15d26c8afa7f7835&=&format=webp&quality=lossless"
}