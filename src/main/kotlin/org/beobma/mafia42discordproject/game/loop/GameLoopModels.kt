package org.beobma.mafia42discordproject.game.loop

import dev.kord.common.entity.Snowflake
import org.beobma.mafia42discordproject.game.GamePhase

internal const val PROS_CONS_VOTE_COMPONENT_ID_PREFIX = "pros_cons_vote_select"
internal const val NIGHT_DURATION_MS = 60_000L
internal const val DAWN_DURATION_MS = 5_000L
internal const val VOTE_DURATION_MS = 30_000L
internal const val INITIAL_VOTE_REVEAL_DURATION_MS = 5_000L
internal const val FINAL_VOTE_TALLY_STEP_MS = 500L
internal const val DEFENSE_DURATION_MS = 15_000L
internal const val PROS_CONS_VOTE_DURATION_MS = 10_000L
internal const val DAY_TIME_ADJUSTMENT_MS = 20_000L
internal const val TIME_THREAD_NAME = "시간"

internal const val PROBATION_DISCOVERY_IMAGE_URL = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(22).webp"
internal const val NURSE_DOCTOR_CONTACT_IMAGE_URL = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(35).webp"
internal const val BELONGINGS_REVEAL_IMAGE_URL = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(48).webp"
internal const val ESCAPE_IMAGE_URL = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(34).webp"
internal const val ESCAPE_DEATH_IMAGE_URL = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(8).webp"
internal const val INNOCENCE_IMAGE_URL = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(32).webp"
internal const val BEASTMAN_ATTACK_IMAGE_URL = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(50).webp"
internal const val BEASTMAN_TAMED_IMAGE_URL = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(57).webp"
internal const val BEASTMAN_ROAR_IMAGE_URL = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/roar_image.webp"
internal const val VIGILANTE_EXECUTION_IMAGE_URL = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(21).webp"
internal const val GODFATHER_CONTACT_IMAGE_URL = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(17).webp"
internal const val GODFATHER_EXECUTION_IMAGE_URL = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(13).webp"
internal const val HITMAN_CONTACT_IMAGE_URL = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(27).webp"
internal const val HOSTESS_CONTACT_IMAGE_URL = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(45).webp"
internal const val MAD_SCIENTIST_CONTACT_IMAGE_URL = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(5).webp"
internal const val SPY_CONTACT_IMAGE_URL = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(30).webp"
internal const val THIEF_CONTACT_IMAGE_URL = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(26).webp"
internal const val WITCH_CONTACT_IMAGE_URL = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(12).webp"
internal const val SWINDLER_CONTACT_IMAGE_URL = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(25).webp"
internal const val SPY_ASSASSIN_IMAGE_URL = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(29).webp"
internal const val MAD_SCIENTIST_REVIVE_IMAGE_URL = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(1).webp"
internal const val MAGICIAN_TRICK_SUCCESS_IMAGE_URL = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/magician_ability_image.webp"

private const val SOUND_BASE_URL = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/sound"
internal const val NIGHT_START_SOUND_PATH = "$SOUND_BASE_URL/mafia%20(5).mp3"
internal const val DAY_START_SOUND_PATH = "$SOUND_BASE_URL/mafia%20(3).mp3"
internal const val VOTE_PHASE_SOUND_PATH = "$SOUND_BASE_URL/mafia%20(13).mp3"
internal const val JUDGE_VERDICT_IMAGE_URL = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(46).webp"
internal const val MAFIA_EXECUTION_SOUND_PATH = "$SOUND_BASE_URL/mafia%20(4).mp3"
internal const val MAD_SCIENTIST_REVIVE_SOUND_PATH = "$SOUND_BASE_URL/mafia%20(1).mp3"
internal const val SOLDIER_BULLETPROOF_SOUND_PATH = "$SOUND_BASE_URL/mafia%20(2).mp3"
internal const val PRIEST_RESURRECTION_SOUND_PATH = "$SOUND_BASE_URL/mafia%20(9).mp3"
internal const val COUPLE_SACRIFICE_SOUND_PATH = "$SOUND_BASE_URL/mafia%20(8).mp3"
internal const val DOCTOR_HEAL_SOUND_PATH = "$SOUND_BASE_URL/mafia%20(7).mp3"
internal const val POLITICIAN_SURVIVAL_SOUND_PATH = "$SOUND_BASE_URL/mafia%20(10).mp3"
internal const val TERRORIST_EXPLOSION_SOUND_PATH = "$SOUND_BASE_URL/mafia%20(12).mp3"
internal const val TERRORIST_NIGHT_MAFIA_BOMB_IMAGE_URL = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(24).webp"
internal const val TERRORIST_NIGHT_EXPLOSION_IMAGE_URL = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(16).webp"
internal const val TERRORIST_VOTE_EXPLOSION_IMAGE_URL = "https://lsvptosgnbwgsteuwstf.supabase.co/storage/v1/object/public/mafia/mafia%20(31).webp"
internal const val REPORTER_SCOOP_SOUND_PATH = "$SOUND_BASE_URL/mafia%20(14).mp3"
internal const val CABAL_SPECIAL_WIN_SOUND_PATH = "$SOUND_BASE_URL/mafia%20(6).mp3"

data class DayTimeAdjustmentResult(
    val isSuccess: Boolean,
    val message: String
)

internal data class ActiveCountdown(
    val guildId: Snowflake,
    val phase: GamePhase,
    val label: String,
    var endAtMillis: Long,
    var forceFinished: Boolean = false
)
