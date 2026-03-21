package org.beobma.mafia42discordproject.job.definition.list

import dev.kord.common.entity.Snowflake
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.ability.JobUniqueAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.police.PoliceAbility
import org.beobma.mafia42discordproject.job.definition.Definition

class Police : Job(), Definition {
    override val name: String = "寃쎌같"
    override val description: String = "[?섏깋] 諛ㅻ쭏???뚮젅?댁뼱 ??紐낆쓣 議곗궗?섏뿬 洹??뚮젅?댁뼱??留덊뵾???щ?瑜??뚯븘?????덈떎."
    override val jobImage: String = "https://media.discordapp.net/attachments/1483977619258212392/1484548811341959208/chrome_WYFnipB5Ox.png?ex=69bea152&is=69bd4fd2&hm=036b6ff6a21169f03b591b9678a8db7ef520aade90b58af8c94e1d557e9083ab&=&format=webp&quality=lossless"
    override val abilities: MutableList<JobUniqueAbility> = mutableListOf(PoliceAbility())

    var currentSearchTarget: Snowflake? = null
    var eavesdroppingTargetId: Snowflake? = null
    var hasUsedSearchThisNight: Boolean = false
    var hasUsedConfidential: Boolean = false
    val searchedTargets: MutableSet<Snowflake> = mutableSetOf()
}
