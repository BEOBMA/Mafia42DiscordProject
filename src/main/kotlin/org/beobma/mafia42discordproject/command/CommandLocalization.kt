package org.beobma.mafia42discordproject.command

import dev.kord.common.Locale
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder

fun DiscordCommand.applyKoreanLocalization(builder: ChatInputCreateBuilder) {
    val localizedName = koreanName ?: return
    builder.name(Locale.KOREAN, localizedName)
    builder.description(Locale.KOREAN, description)
}
