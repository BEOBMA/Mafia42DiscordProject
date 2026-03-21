package org.beobma.mafia42discordproject.game.system

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.job.definition.list.Administrator

object AdministratorInvestigationNotificationManager {
    suspend fun notifyResults(game: Game) {
        game.playerDatas.forEach { player ->
            val administrator = player.job as? Administrator ?: return@forEach
            val selectedJob = administrator.selectedInvestigationJobName
            if (selectedJob.isNullOrBlank()) return@forEach

            val resultPlayerId = administrator.investigationResultPlayerId
            val resultMessage = buildResultMessage(game, selectedJob, resultPlayerId)

            runCatching {
                player.member.getDmChannel().createMessage(resultMessage)
            }

            administrator.selectedInvestigationJobName = null
            administrator.investigationResultPlayerId = null
        }
    }

    private fun buildResultMessage(game: Game, selectedJob: String, resultPlayerId: Snowflake?): String {
        val playerName = resultPlayerId
            ?.let { game.getPlayer(it) }
            ?.member
            ?.effectiveName

        val adminSuccessImageUrl = SystemImage.ADMINISTRATOR_NOTICE.imageUrl
        return if (playerName != null) {
            "$adminSuccessImageUrl\n조회 결과: ${playerName}님이 ${selectedJob}로 조회되었습니다."
        } else {
            "조회 결과: ${selectedJob} 직업을 가진 생존자를 찾지 못했습니다."
        }
    }
}
