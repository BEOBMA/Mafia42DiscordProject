package org.beobma.mafia42discordproject.game

import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.channel.VoiceChannel
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import org.beobma.mafia42discordproject.discord.DiscordMessageManager
import org.beobma.mafia42discordproject.game.event.DefenseTier
import org.beobma.mafia42discordproject.game.event.GameEvent
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.job.ability.PassiveAbility

object GameManager {
    private var currentGame: Game? = null

    suspend fun start(event: GuildChatInputCommandInteractionCreateEvent) {
        Game(mutableListOf()).start(event)
    }

    private suspend fun Game.start(event: GuildChatInputCommandInteractionCreateEvent) {
        val interaction = event.interaction
        if (currentGame != null) {
            DiscordMessageManager.respondEphemeral(event, "이미 게임이 진행 중입니다.")
            return
        }

        val guild = interaction.guild
        val commandSender = interaction.user
        val voiceChannelId = commandSender.getVoiceStateOrNull()?.channelId ?: run {
            DiscordMessageManager.respondEphemeral(event, "현재 음성채널에 들어가 있지 않습니다.")
            return
        }
        val voiceChannel = guild.getChannelOfOrNull<VoiceChannel>(voiceChannelId) ?: run {
            DiscordMessageManager.respondEphemeral(event, "음성채널 정보를 가져오지 못했습니다.")
            return
        }

        val membersInSameVoice = guild.members
            .filter { guildMember ->
                guildMember.getVoiceStateOrNull()?.channelId == voiceChannelId
            }
            .toList()

        currentGame = this
        this.playerDatas = membersInSameVoice.map(::PlayerData).toMutableList()

        DiscordMessageManager.respondPublic(
            event,
            buildString {
                appendLine("현재 음성채널: ${voiceChannel.mention}")
                appendLine("인원 수: ${membersInSameVoice.size}")
                appendLine()
                append(DiscordMessageManager.mentions(membersInSameVoice))
            }
        )
    }

    suspend fun stop(event: GuildChatInputCommandInteractionCreateEvent) {
        if (currentGame == null) {
            DiscordMessageManager.respondEphemeral(event, "진행 중인 게임이 없습니다.")
            return
        }
        currentGame = null

        val mention = DiscordMessageManager.mention(event.interaction.user)
        DiscordMessageManager.respondPublic(event, "${mention}이(가) 게임을 종료했습니다.")
    }

    // 밤 시간이 끝나고 낮으로 넘어갈 때 호출되는 파이프라인
    fun resolveNightPhase(game: Game) {

        // 동시성 보장을 위해, 이번 밤에 최종적으로 사망할 플레이어들을 모아두는 Set
        val playersToDie = mutableSetOf<PlayerData>()

        // =========================================================
        // 1. 보조 및 조사 능력 결산
        // =========================================================
        // (의사의 힐은 디스코드 명령어 입력 시점에 이미 state.healTier에 적용되었다고 가정)
        // 예: 경찰의 조사 결과를 판정하고 이벤트로 발행
        // if (경찰 조사 성공) {
        //     game.nightEvents.add(GameEvent.JobDiscovered(경찰, 타겟, 타겟직업))
        // }


        // =========================================================
        // 2. 방어랑 공격 티어 비교함
        // =========================================================
        for ((attackKey, attackEvent) in game.nightAttacks) {
            val target = attackEvent.target
            val attackTier = attackEvent.attackTier

            // 특수 룰: 군인의 방탄 소모 로직
            // 군인이고 방탄이 남아있다면, 이번 판정에 한해 방어 티어를 ABSOLUTE(절대 방어)로 끌어올림
            if (target.job?.name == "군인" && !target.state.hasUsedOneTimeAbility) {
                target.state.healTier = DefenseTier.ABSOLUTE

                // 만약 이 공격을 막아낼 수 있는 수준이라면 방탄을 1회 소모함
                if (target.state.healTier.level >= attackTier.level) {
                    target.state.hasUsedOneTimeAbility = true
                }
            }

            // 타겟의 방어 레벨이 공격 레벨보다 같거나 높으면 생존
            if (target.state.healTier.level >= attackTier.level) {
                // 방어 성공 로직
            } else {
                // 방어 실패 -> 죽을놈 리스트에 추가
                playersToDie.add(target)
            }
        }


        // =========================================================
        // 3. 사망 처리
        // =========================================================
        playersToDie.forEach { victim ->
            victim.state.isDead = true

            // 사망 사실을 시스템 이벤트로 발행 (도굴꾼, 영매 등이 주워갈 수 있도록)
            game.nightEvents.add(GameEvent.PlayerDied(victim))

            // TODO: 디스코드 메시지 큐에 "밤 사이 [victim]님이 살해당했습니다." 추가
        }


        // =========================================================
        // 4. 사후/패시브 능력 분배
        // =========================================================
        // 살아있는 모든 플레이어의 패시브 능력을 순회하며, 이번 밤 일어난 사건들을 던져줍니다.
        // 파파라치(이슈), 도굴꾼(도굴), 테러리스트(유언 자폭), 마술사(트릭 대상 재설정) 등이 여기서 알아서 발동됩니다.
        val alivePlayers = game.playerDatas.filter { !it.state.isDead }

        for (event in game.nightEvents) {
            for (player in alivePlayers) {
                player.allAbilities.filterIsInstance<PassiveAbility>().forEach { passive ->
                    passive.onEventObserved(game, player, event)
                }
            }
        }


        // =========================================================
        // 5. 다음날 준비
        // =========================================================
        // 큐 비우기
        game.nightAttacks.clear()
        game.nightEvents.clear()

        // 살아남은 플레이어들의 일회성 상태(힐, 유혹 등) 초기화
        for (player in game.playerDatas) {
            player.state.resetForNextPhase()
        }

        // 게임 페이즈 업데이트
        game.currentPhase = GamePhase.DAY
        game.dayCount++

        // TODO: 디스코드 채널에 아침 브리핑 출력 및 토론 시간 타이머 시작
    }
}
