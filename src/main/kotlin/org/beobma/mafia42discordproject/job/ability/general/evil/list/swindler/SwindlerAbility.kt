package org.beobma.mafia42discordproject.job.ability.general.evil.list.swindler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.definition.Definition
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import org.beobma.mafia42discordproject.job.evil.list.Swindler

class SwindlerAbility : JobUniqueAbility, PassiveAbility {
    override val name: String = "교섭"
    override val description: String = "사기 대상 또는 사기꾼이 처형 능력 대상이 된 경우, 사기꾼은 처형되지 않으며 처형 여부와 관계 없이 마피아와 접선한다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1485290082557497579/f1ffbe11c7479792.png?ex=69c153af&is=69c0022f&hm=7d4b8f5c75b50f9c995156badc9bc99cef20046cf7b3a4dcd08bbab16530a7ec&"
}
