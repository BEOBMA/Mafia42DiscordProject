package org.beobma.mafia42discordproject.job.ability.general.list

import org.beobma.mafia42discordproject.job.ability.Ability
import org.beobma.mafia42discordproject.job.ability.CommonAbility

class Jury : Ability, CommonAbility {
    override val name: String = "배심원"
    override val description: String = "최다 득표자가 생존자의 절반 미만의 득표를 한 상태에서 여러명일 경우, 그 중 배심원이 더 많이 투표한 플레이어가 최후의 반론에 올라간다."
    override val image: String = "https://cdn.discordapp.com/attachments/1483977619258212392/1484597887307612300/0a9025db8c29a54e.png?ex=69becf06&is=69bd7d86&hm=d59f11a7d28c78692af53745c97c97f69a17b32113b2ea3eb0c247142f845be6&"
}