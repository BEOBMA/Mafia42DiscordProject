package org.beobma.mafia42discordproject.game.system

import dev.kord.core.behavior.channel.createMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GameLoopManager
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.JobManager
import org.beobma.mafia42discordproject.job.definition.Definition
import org.beobma.mafia42discordproject.job.evil.list.Swindler

object SwindlerManager {
    const val SWINDLER_CONTACT_IMAGE_URL =
        "https://cdn.discordapp.com/attachments/1483977619258212392/1485102540914692256/H8ETeBTIzzrPsPjS0hfbYZoKCkIgNWlsKjD_v29uvxV9Gm1waNRTl4YsmClfkeG_oQYEAlsyJh8fKm4JqZUPzDnCbl5ouVHjYeeiAcGVOfmaU9PYwVfPv1uDKV8JB8nirRsJY1TAVYaQ0E8Pf1rWIg.webp?ex=69c0a505&is=69bf5385&hm=0887e536593a5a9b353dcf4e232c179fed8639ce159f869f796614395884ee49&"

    private val dmScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun disguisedJobOf(player: PlayerData): Job? {
        val swindler = player.job as? Swindler ?: return null
        val disguisedJobName = swindler.disguisedJobName ?: return null
        return JobManager.findByName(disguisedJobName)
    }

    fun shouldTriggerNegotiation(game: Game, mafiaTarget: PlayerData): Pair<PlayerData, Boolean>? {
        val aliveSwindlers = game.playerDatas.filter { !it.state.isDead && it.job is Swindler }
        val triggered = aliveSwindlers.firstOrNull { swindlerPlayer ->
            val swindlerJob = swindlerPlayer.job as? Swindler ?: return@firstOrNull false
            mafiaTarget.member.id == swindlerPlayer.member.id ||
                mafiaTarget.member.id == swindlerJob.disguisedTargetId
        } ?: return null

        val swindlerWasMafiaTarget = mafiaTarget.member.id == triggered.member.id
        return triggered to swindlerWasMafiaTarget
    }

    suspend fun contactMafia(game: Game, swindlerPlayer: PlayerData) {
        val swindler = swindlerPlayer.job as? Swindler ?: return
        if (swindler.hasContactedMafia) return
        swindler.hasContactedMafia = true

        runCatching {
            GameLoopManager.announceMafiaSupportContact(game, swindlerPlayer, SWINDLER_CONTACT_IMAGE_URL)
        }
        runCatching {
            GameLoopManager.refreshMafiaChannelContactState(game)
        }
    }

    fun notifyFooledByDiscovery(event: GameEvent.JobDiscovered) {
        if (event.discoverer.member.id == event.target.member.id) return
        val swindlerTarget = event.target.job as? Swindler ?: return
        val disguisedJobName = swindlerTarget.disguisedJobName ?: return
        if (event.revealedJob.name != disguisedJobName) return

        notifyFooled(event.target, event.discoverer)
    }

    fun notifyFooled(target: PlayerData, discoverer: PlayerData) {
        val swindlerTarget = target.job as? Swindler ?: return
        if (swindlerTarget.disguisedJobName == null) return

        dmScope.launch {
            runCatching {
                target.member.getDmChannel().createMessage("**${discoverer.member.effectiveName}님을 속였습니다.**")
            }
        }
    }

    fun notifyBeautyTrap(target: PlayerData, discoverer: PlayerData) {
        if (discoverer.job !is Definition) return
        if (target.job !is Swindler && target.job !is org.beobma.mafia42discordproject.job.evil.list.Spy) return
        if (target.state.isDead) return

        val discovererJobName = discoverer.job?.name ?: "알 수 없음"
        dmScope.launch {
            runCatching {
                target.member.getDmChannel().createMessage("${discoverer.member.effectiveName}님의 직업은 ${discovererJobName}")
            }
        }
    }
}
