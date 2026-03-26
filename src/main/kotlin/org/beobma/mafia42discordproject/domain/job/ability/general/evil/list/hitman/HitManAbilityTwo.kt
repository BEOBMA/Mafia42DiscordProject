package org.beobma.mafia42discordproject.job.ability.general.evil.list.hitman

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GameLoopManager
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.HackerRedirectManager
import org.beobma.mafia42discordproject.job.JobManager
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import org.beobma.mafia42discordproject.job.evil.Evil
import org.beobma.mafia42discordproject.job.evil.list.HitMan
import org.beobma.mafia42discordproject.job.evil.list.Mafia

class HitManAbilityTwo : PassiveAbility, JobUniqueAbility {
    override val name: String = "동업"
    override val description: String = "마피아를 지목할 경우 접선한다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1485288011712167976/aca592148cbcc566.png?ex=69c151c1&is=69c00041&hm=b923d6c5b533bb4fda83102fd9afafd5234d56a5c0694ae0bd5f4fe445040cd3&"
}
