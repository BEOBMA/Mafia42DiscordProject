package org.beobma.mafia42discordproject.job.ability

import org.beobma.mafia42discordproject.job.Job
import kotlin.reflect.KClass

interface Ability {
    val name: String
    val description: String
    val image: String
}

// 마커용
interface JobUniqueAbility : Ability            // 1. 직업 기본 능력
interface CommonAbility : Ability               // 2. 전 직업 공용 능력
interface EvilCommonAbility : Ability           // 3. 악인 직업 공용 능력
interface CitizenCommonAbility : Ability        // 4. 시민 직업 공용 능력
interface JobSpecificExtraAbility : Ability {   // 5. 특정 직업 고유 능력
    val targetJob: List<KClass<out Job>>        // 이 능력을 얻을 수 있는 타겟 직업
}
interface AssistanceCommonAbility : Ability           // 6. 보조 직업 공용 능력

// 능력 사용 결과 반환용
data class AbilityResult(
    val isSuccess: Boolean,
    val message: String? = null
)