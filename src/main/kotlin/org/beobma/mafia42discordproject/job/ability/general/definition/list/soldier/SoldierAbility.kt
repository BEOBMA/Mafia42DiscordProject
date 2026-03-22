package org.beobma.mafia42discordproject.job.ability.general.definition.list.soldier

import org.beobma.mafia42discordproject.game.Game
import org.beobma.mafia42discordproject.game.GamePhase
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.DefenseTier
import org.beobma.mafia42discordproject.game.system.DiscoveryStep
import org.beobma.mafia42discordproject.game.system.GameEvent
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.evil.Evil

class Bulletproof : JobUniqueAbility, PassiveAbility {
    override val name: String = "방탄"
    override val description: String = "마피아의 공격을 한 차례 버텨낸다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1485335348472184942/f434b39e3b8e5883.png?ex=69c17dd7&is=69c02c57&hm=33fa8fb23fbabafb85bbe6a513f337f9c1b39abfbe6be0cd1336adab4f219f48&"
    override val priority: Int = 10
    
    // 상태 변수: 오늘 밤 방탄이 터졌는지 여부
    private var wasTriggeredTonight = false
    private var triggeredCount = 0

    override fun onPhaseChanged(game: Game, owner: PlayerData, newPhase: GamePhase) {
        if (newPhase == GamePhase.NIGHT) {
            wasTriggeredTonight = false
        }
    }

    override fun onEventObserved(game: Game, owner: PlayerData, event: GameEvent) {
        when (event) {
            is GameEvent.BeforeAttackEvaluated -> {
                if (event.attackEvent.target != owner) return
                val maxTriggerCount = if (owner.allAbilities.any { it is Indomitable }) 2 else 1
                if (triggeredCount >= maxTriggerCount) return

                // 의사(Doctor)의 힐 등 외부 요인으로 이미 이 공격을 방어할 수 있는 상태라면
                // 군인의 귀중한 방탄 능력을 소모하지 않고 그대로 보존합니다.
                if (owner.state.healTier.level >= event.attackEvent.attackTier.level) {
                    return
                }

                owner.state.healTier = maxOf(owner.state.healTier, DefenseTier.ABSOLUTE)
                if (owner.state.healTier.level >= event.attackEvent.attackTier.level) {
                    triggeredCount += 1
                    owner.state.hasUsedOneTimeAbility = triggeredCount >= maxTriggerCount
                    wasTriggeredTonight = true
                }
            }
            is GameEvent.ResolveDawnPresentation -> {
                if (wasTriggeredTonight) {
                    // 아침 결과 일러스트 및 텍스트 교체 (copy 활용)
                    event.presentation = event.presentation.copy(
                        imageUrl = org.beobma.mafia42discordproject.game.system.SystemImage.SOLDIER_DEFENDED.imageUrl,
                        message = "군인 ${owner.member.effectiveName}님이 공격을 버텨냈습니다."
                    )
                    
                    // 군인임을 모두에게 알리는 직업 공개 이벤트 발생
                    owner.state.isJobPubliclyRevealed = true
                    game.nightEvents += GameEvent.JobDiscovered(
                        discoverer = owner,
                        target = owner,
                        actualJob = owner.job!!,
                        revealedJob = owner.job!!,
                        sourceAbilityName = name,
                        resolvedAt = DiscoveryStep.DAWN,
                        isPublicReveal = true
                    )
                }
            }
            else -> {}
        }
    }
}

class NightWatch : JobUniqueAbility, PassiveAbility {
    override val name: String = "불침번"
    override val description: String = "마피아 팀에게 직업을 조사당할 경우 그 직업의 정체를 알 수 있고 조사의 부가적인 효과도 무효화 시킨다."
    override val image: String = ""
    override val priority: Int = 20 // 조사가 이루어진 직후 즉시 가로채기 위해 높은 우선순위 부여

    // 향후 스파이, 도둑, 청부업자 등 보조 직업별 고유 불침번 이미지를 매핑하기 위한 헬퍼 함수
    private fun getReactionImageUrlFor(discovererJobName: String?): String? {
        return when (discovererJobName) {
            "스파이" -> "" // TODO: 스파이 전용 불침번 적발 이미지 삽입
            "도둑" -> "" // TODO: 도둑 전용 불침번 적발 이미지 삽입
            "청부업자" -> "" // TODO: 청부업자 전용 불침번 적발 이미지 삽입
            "마담" -> "" // TODO: 마담 전용 불침번 적발 이미지 삽입
            else -> null // 매핑이 없으면 기본적으로 널 반환 -> NotificationManager가 알아서 본래 JobImage나 Fallback을 사용할 수 있음
        }
    }

    override fun onEventObserved(game: Game, owner: PlayerData, event: GameEvent) {
        if (event !is GameEvent.JobDiscovered) return
        
        // 1. 군인 본인이 조사의 대상이 되었고,
        // 2. 조사를 수행한 주체가 Evil(스파이 등 마피아 팀)이고,
        // 3. 발동 스킬이 '불침번' 자체인 경우의 피드백 루프 방지
        if (event.target == owner && event.discoverer.job is Evil && event.sourceAbilityName != name) {
            
            // 조사자(스파이, 도둑 등)의 직업에 맞는 전용 일러스트 호출
            val reactionImageUrl = getReactionImageUrlFor(event.discoverer.job?.name)

            // 상대(마피아팀)가 군인의 직업은 정상적으로 알 수 있도록 isCancelled는 true로 만들지 않되, 
            // '부가적인 효과 무효화' 및 '불침번 발동 여부'를 안내하는 메시지와 군인 전용 이미지를 첨부합니다.
            event.note = "군인의 [불침번]이 발동하여 조사의 부가적인 효과(첩보 성공, 도벽 등)는 모두 무효화됩니다."
            
            // 전용 이미지가 있으면 그걸 쓰고, 없으면 방금 전 쓰던 스파이 기본 폴백 이미지를 씁니다.
            event.imageUrl = reactionImageUrl ?: "https://media.discordapp.net/attachments/1483977619258212392/1484824345514606703/image.png?ex=69bf9e51&is=69be4cd1&hm=44cb94f8ca8ebbb145b0a3221ea3bfe7ea309ac4c2780e5572e811340ebbf519&=&format=webp&quality=lossless&width=550&height=413"

            // 침입자의 정체를 알아내어 군인에게 통보하는 역탐지 이벤트 발생
            game.nightEvents += GameEvent.JobDiscovered(
                discoverer = owner,
                target = event.discoverer,
                actualJob = event.discoverer.job!!,
                revealedJob = event.discoverer.job!!,
                sourceAbilityName = name,
                resolvedAt = event.resolvedAt,
                // 군인에게 보여질 침입자(상대방)의 원래 직업 일러스트
                imageUrl = event.discoverer.job?.jobImage
            )
        }
    }
}
