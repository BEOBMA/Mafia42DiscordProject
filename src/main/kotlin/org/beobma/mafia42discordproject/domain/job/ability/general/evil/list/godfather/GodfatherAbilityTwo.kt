package org.beobma.mafia42discordproject.job.ability.general.evil.list.godfather

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.AttackEvent
import org.beobma.mafia42discordproject.game.system.AttackTier
import org.beobma.mafia42discordproject.game.system.HackerRedirectManager
import org.beobma.mafia42discordproject.job.ability.AbilityResult
import org.beobma.mafia42discordproject.job.ability.ActiveAbility
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility

class GodfatherAbilityTwo : PassiveAbility, JobUniqueAbility {
    override val name: String = "배후"
    override val description: String = "세번째 밤이 될 때 마피아와 접선한다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1485287199011377353/76068088065d201a.png?ex=69c150ff&is=69bfff7f&hm=5b15390502ce491217b8cb2023690afaf7ebdff976d17634f2115a34edcfbf3f&"
}
