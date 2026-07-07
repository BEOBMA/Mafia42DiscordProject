package org.beobma.mafia42discordproject.game.mode

enum class GameStartMode(
    val optionValue: String,
    val displayName: String
) {
    NORMAL("일반", "일반"),
    MADNESS("미치광이", "미치광이");

    val typeName: String
        get() = name

    companion object {
        fun parse(raw: String?): GameStartMode? {
            val normalized = raw?.trim()?.lowercase() ?: return NORMAL
            return when (normalized) {
                "", "일반", "normal" -> NORMAL
                "미치광이", "madness", "crazy", "mad" -> MADNESS
                else -> null
            }
        }

        fun fromType(raw: String?): GameStartMode? {
            val trimmed = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            return entries.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
                ?: parse(trimmed)
        }

        fun displayNameForType(raw: String): String {
            return fromType(raw)?.displayName ?: raw
        }
    }
}
