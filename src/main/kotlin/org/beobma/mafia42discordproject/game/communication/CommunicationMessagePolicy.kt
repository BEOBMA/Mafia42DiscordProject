package org.beobma.mafia42discordproject.game.communication

internal const val SECRET_LETTER_MAX_CHARACTERS = 20
internal const val WILL_MAX_CHARACTERS = 60
internal const val MEGAPHONE_MAX_CHARACTERS = 10
internal const val NIGHT_COMMUNICATION_DEADLINE_BEFORE_END_MS = 15_000L
internal const val MEGAPHONE_REVEAL_BEFORE_NIGHT_END_MS = NIGHT_COMMUNICATION_DEADLINE_BEFORE_END_MS

internal fun String.truncateToCharacterLimit(maxCharacters: Int): String {
    require(maxCharacters >= 0) { "maxCharacters must not be negative" }
    if (isEmpty() || maxCharacters == 0) return ""

    val characterCount = codePointCount(0, length)
    if (characterCount <= maxCharacters) return this

    return substring(0, offsetByCodePoints(0, maxCharacters))
}

internal fun isNightCommunicationSubmissionOpen(nowMillis: Long, nightEndsAtMillis: Long): Boolean {
    if (nightEndsAtMillis <= 0L) return false
    return nowMillis < nightEndsAtMillis - NIGHT_COMMUNICATION_DEADLINE_BEFORE_END_MS
}
