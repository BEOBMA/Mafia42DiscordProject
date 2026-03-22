package org.beobma.mafia42discordproject.game

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.channel.edit
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.thread.ThreadChannel
import dev.kord.rest.builder.channel.addMemberOverwrite
import dev.kord.rest.builder.channel.addRoleOverwrite
import dev.kord.rest.builder.component.actionRow
import dev.kord.rest.builder.component.option
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.beobma.mafia42discordproject.discord.DiscordMessageManager.sendMainChannelMessageWithImage
import org.beobma.mafia42discordproject.discord.DiscordMessageManager.sendMainChannerMessage
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.*
import org.beobma.mafia42discordproject.job.ability.PassiveAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.administrator.AdministratorInvestigationPolicy
import org.beobma.mafia42discordproject.job.ability.general.definition.list.Belongings
import org.beobma.mafia42discordproject.job.ability.general.definition.list.Source
import org.beobma.mafia42discordproject.job.ability.general.definition.list.detective.DetectiveAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.mentalist.MentalistAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.doctor.DoctorAbility
import org.beobma.mafia42discordproject.job.ability.general.definition.list.police.Autopsy
import org.beobma.mafia42discordproject.job.ability.general.definition.list.police.Confidential
import org.beobma.mafia42discordproject.job.ability.general.definition.list.reporter.BreakingNews
import org.beobma.mafia42discordproject.job.ability.general.definition.list.reporter.Obituary
import org.beobma.mafia42discordproject.job.ability.general.definition.list.soldier.MentalStrength
import org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.Concealment
import org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.Exorcism
import org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.Poisoning
import org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.Probation
import org.beobma.mafia42discordproject.job.ability.general.evil.list.Instructions
import org.beobma.mafia42discordproject.job.ability.general.evil.list.Terminal
import org.beobma.mafia42discordproject.job.ability.general.evil.list.hostess.Deception
import org.beobma.mafia42discordproject.job.ability.general.evil.list.assistance.TheInformant
import org.beobma.mafia42discordproject.job.ability.general.evil.list.spy.SpyAbility
import org.beobma.mafia42discordproject.job.ability.general.evil.list.madscientist.Analysis
import org.beobma.mafia42discordproject.job.ability.general.evil.list.madscientist.Distortion
import org.beobma.mafia42discordproject.job.ability.general.evil.list.beastman.Roar
import org.beobma.mafia42discordproject.job.ability.general.evil.list.godfather.GodfatherContactPolicy
import org.beobma.mafia42discordproject.job.ability.general.list.EarthboundSpirit
import org.beobma.mafia42discordproject.job.ability.general.list.Escape
import org.beobma.mafia42discordproject.job.ability.general.list.Innocence
import org.beobma.mafia42discordproject.job.ability.general.list.Jury
import org.beobma.mafia42discordproject.job.ability.general.list.MindReading
import org.beobma.mafia42discordproject.job.ability.general.list.Will
import org.beobma.mafia42discordproject.job.ability.general.definition.list.police.Warrant
import org.beobma.mafia42discordproject.job.ability.general.definition.list.prophet.Apostle
import org.beobma.mafia42discordproject.job.ability.general.definition.list.prophet.Pioneer
import org.beobma.mafia42discordproject.job.ability.general.definition.list.doctor.Calm
import org.beobma.mafia42discordproject.job.definition.list.Administrator
import org.beobma.mafia42discordproject.job.definition.list.Cabal
import org.beobma.mafia42discordproject.job.definition.list.CabalRole
import org.beobma.mafia42discordproject.job.definition.list.Citizen
import org.beobma.mafia42discordproject.job.definition.list.Couple
import org.beobma.mafia42discordproject.job.definition.list.CoupleRole
import org.beobma.mafia42discordproject.job.definition.list.Doctor
import org.beobma.mafia42discordproject.job.ability.general.definition.list.hacker.Synchronization
import org.beobma.mafia42discordproject.job.ability.general.definition.list.priest.Blessing
import org.beobma.mafia42discordproject.job.ability.general.definition.list.priest.Exorcism as PriestExorcism
import org.beobma.mafia42discordproject.job.definition.list.Detective
import org.beobma.mafia42discordproject.job.definition.list.Gangster
import org.beobma.mafia42discordproject.job.definition.list.Hacker
import org.beobma.mafia42discordproject.job.definition.list.Hypnotist
import org.beobma.mafia42discordproject.job.definition.list.Judge
import org.beobma.mafia42discordproject.job.definition.list.Mercenary
import org.beobma.mafia42discordproject.job.definition.list.Nurse
import org.beobma.mafia42discordproject.job.definition.list.Police
import org.beobma.mafia42discordproject.job.definition.list.Politician
import org.beobma.mafia42discordproject.job.definition.list.Priest
import org.beobma.mafia42discordproject.job.definition.list.Prophet
import org.beobma.mafia42discordproject.job.definition.list.Reporter
import org.beobma.mafia42discordproject.job.definition.list.Shaman
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import org.beobma.mafia42discordproject.job.ability.general.definition.list.gangster.CombinedAttack
import org.beobma.mafia42discordproject.job.ability.general.definition.list.gangster.TravelCompanion
import org.beobma.mafia42discordproject.job.ability.general.definition.list.martyr.Explosion
import org.beobma.mafia42discordproject.job.ability.general.definition.list.martyr.Flash
import org.beobma.mafia42discordproject.job.ability.general.definition.list.other.Resolute
import org.beobma.mafia42discordproject.job.evil.Evil
import org.beobma.mafia42discordproject.job.evil.list.Beastman
import org.beobma.mafia42discordproject.job.evil.list.Godfather
import org.beobma.mafia42discordproject.job.evil.list.HitMan
import org.beobma.mafia42discordproject.job.evil.list.Hostess
import org.beobma.mafia42discordproject.job.evil.list.MadScientist
import org.beobma.mafia42discordproject.job.evil.list.Mafia
import org.beobma.mafia42discordproject.job.evil.list.Spy
import org.beobma.mafia42discordproject.job.evil.list.Swindler
import org.beobma.mafia42discordproject.job.evil.list.Thief
import org.beobma.mafia42discordproject.job.evil.list.Villain
import org.beobma.mafia42discordproject.job.evil.list.Witch
import org.beobma.mafia42discordproject.job.definition.list.Martyr
import org.beobma.mafia42discordproject.job.definition.list.Mentalist
import org.beobma.mafia42discordproject.job.definition.list.Vigilante

object GameLoopManager {
    private const val NIGHT_DURATION_MS = 25_000L
    private const val DAWN_DURATION_MS = 10_000L
    private const val VOTE_DURATION_MS = 15_000L
    private const val INITIAL_VOTE_REVEAL_DURATION_MS = 5_000L
    private const val FINAL_VOTE_TALLY_STEP_MS = 500L
    private const val DEFENSE_DURATION_MS = 15_000L
    private const val PROS_CONS_VOTE_DURATION_MS = 10_000L
    private const val DAY_TIME_ADJUSTMENT_MS = 15_000L
    private const val TIME_THREAD_NAME = "시간"
    private const val PROBATION_DISCOVERY_IMAGE_URL = "https://cdn.discordapp.com/attachments/1483977619258212392/1485026577610703000/v1-_70Quh-Wmb9ZFVNCbnfkgmA72QZfsKd6CwwUuLiDO25gNgl3l-UiOGyWQNCbxRRfmykJG5UyvAuipvrlfSVWe5mEKilEuBMoaieLofY6Rf5Hdog2Gg7cf-RiqrNrgXRU5GSQxiJwRorEo-JVWIA.webp?ex=69c05e46&is=69bf0cc6&hm=4f084aa32d244df25bafd30549631dc28009f00831b5ad6ed2bbf02df7b5d939&"
    private const val NURSE_DOCTOR_CONTACT_IMAGE_URL = "https://cdn.discordapp.com/attachments/1483977619258212392/1485041686743744632/B3X9qY9DRgztfGRfTvOmaWzHqY-GRAJ8OFxFmU-mJWPq0RalAYlysco8cTNxJ1vTBYkabPX3KX6luBLqKylwb5BwiQKvDpJL_2sBLnZmwyNgklA3GW8tbIzwt3Sjba6jnyy-Rgy4K_0ggw2aFse9qw.webp?ex=69c06c58&is=69bf1ad8&hm=f280af1c360cc62dd6c0bdbe79a3f284824f5c221088cb6571e43924d2b8ec98&"
    private const val BELONGINGS_REVEAL_IMAGE_URL = "https://cdn.discordapp.com/attachments/1483977619258212392/1485064103654326362/RoJTMDyTb8Fsc4xISb-b-FpabFGGEC2lEphQj-TPdy5jqQOPoiglPiBQnN9ZRPnwgAXnpw8NA1cIZe1Owz83imTIj3F7_u5gs_1Xp6kDJxhqHLY40_2WpoS8sqmkWhBM9sC0On5EsConl97VZ5twnQ.webp?ex=69c08139&is=69bf2fb9&hm=8558bb6cbfa42f90d449e7ee4874628383ed3503a4f973b0a71f725207e2973c&"
    private const val ESCAPE_IMAGE_URL = "https://cdn.discordapp.com/attachments/1483977619258212392/1485068268518641804/FGY_RI7iQoyC4rGWNM7VvnxZ7deIbe6jMdxCocerKyOYAEhugy1Al6xM16fuD1wq1Y5cJ4RT2_Mu85JJCF3qygvj56JeMkwlnYSqiG_EeLjMpJYty9OKTXvyYtF-rXdWNY5Qf-hIQOGl3y_IyOXtA.webp?ex=69c0851a&is=69bf339a&hm=03f024962d2a5f70c4fda843ca2baf4b81bacd340b86d5151b7f6d5bdfe4b592&"
    private const val ESCAPE_DEATH_IMAGE_URL = "https://cdn.discordapp.com/attachments/1483977619258212392/1485068400043626496/uyd_Tgfv6NhX8X3b60JHJzKo0V-L40BD_wEhnmhYs4TFroVjc08r_ZwR52fekvW4okZ4zrff6t9lnG42n8cTEomUI5gXJhGQCYC2Hk6FUC62CGt0-C5shAvv_9qHTdNpyvRYkVgBBdiMfT-TtLrPkQ.webp?ex=69c08539&is=69bf33b9&hm=38e7d92b214404036675aba36deb468aa2f6493b2b9b17f69751bcffacdb6dec&"
    private const val INNOCENCE_IMAGE_URL = "https://cdn.discordapp.com/attachments/1483977619258212392/1485069339408335011/yNSYVlFExulsh1yqgC_ocOl1-cnVgzSWTdrT1F6D8PxhJMSKF24PRhZSkVW7ybLC50--Jte3jCS8qlOHLoGYUk5REnG_bWIGZWByWpNTnJcLueDMSBOrjx5ngRD15lIRAxKXyG09Y6G-wFG8oRWyaA.webp?ex=69c08619&is=69bf3499&hm=13cf9f57c385b905c9d01f664ae5cd488d8510c4c15a9b67ca1c6bf9f0ecad66&"
    private const val BEASTMAN_ATTACK_IMAGE_URL = "https://cdn.discordapp.com/attachments/1483977619258212392/1485075367025836173/YzFttk1gOxNI077qHyWhP2998lil0b7GmqzQusuTzBVp3M2LzgmIHAxUH1m7uHOMR5LtQFLkBzZtIYMOu7zxT9vjaf4Uh26up3-i3cJ5wAeEPAeQQoxajm1kMkiRVl0r07pw1eafMIRnV8MkZBGNMA.webp?ex=69c08bb6&is=69bf3a36&hm=0bd9efbd0dd7841dbf2cd2e0531e8e0d26564637d085de9f48ac18aac1492f86&"
    private const val BEASTMAN_TAMED_IMAGE_URL = "https://cdn.discordapp.com/attachments/1483977619258212392/1485075910263570463/XEMOjk-m1HtYEFgz0clURpnMQNiipyYZimPOWIPk6vogykiTNhInvt8W531YXrAjxtYqqnzoWbXKIk1C6nH1wOhkfPxHrCmz6q6LKWoBuR1AFmg2p5pEcApZ0SkwsLjqLjnyckqSMh5kVO9IVn4UHQ.webp?ex=69c08c38&is=69bf3ab8&hm=26ac26dcc50bc338b595319249a45f1cfb20d3a242bf6f6a8f6e740164d0c5de&"
    private const val BEASTMAN_ROAR_IMAGE_URL = "https://cdn.discordapp.com/attachments/1483977619258212392/1485077233570812125/wLyCdbdvcKvKkGkmdkRW6vhDbtnasWFp5qGexUOnT488bF4RZzIXcAul1YGMNyw2pxxh9qJZooXhedNZeOR6eXjRq198saXx3yLZKkc_Oia88BI5rizeBltm0qJjbeHb3YPb4lL_n8UP-1IE2RT9Qg.webp?ex=69c08d73&is=69bf3bf3&hm=0c6ab1e928750c3fcd8355850ddefcab3380f44f519ace364157a5e36026fef9&"
    private const val VIGILANTE_EXECUTION_IMAGE_URL = "https://cdn.discordapp.com/attachments/1483977619258212392/1485082843393687690/Nu_4LgYjmQsjtz9Guhs_Vi6TduYsooqYsidxH3JULrfO9FKUR-bA7XlF_Xt_fmArScHnzeTIbRB1Fi1jbJcfo2ueRPQKC752PcZkqMf9q-F37QTZ2fx_4L7MfMZpQ4baqvVEFiP7-rx9MK48M1ZkLw.webp?ex=69c092ad&is=69bf412d&hm=0f4f08d96f674dc64170d6c252644367505f7290e17d6a05278199de541cb557&"
    private const val GODFATHER_CONTACT_IMAGE_URL = "https://cdn.discordapp.com/attachments/1483977619258212392/1485087325703901365/6Dt6As_ReET4vjOl3djPFyzLrg-v8hvaMe42oBrrf6ROTHOk1ejUYjwk-vn9DfryaLt8v06oG-aRbrGZgELlBM9G8ciLeqIsvKT4OZMroiRIz-6t3GyftqwT67UHpzqiI3o7Ja9CelJpOrgibccDPg.webp?ex=69c096da&is=69bf455a&hm=270ad9182d231294d6116d48e9fd7378731ccbe3553fd8f20a1d8bf282236c92&"
    private const val GODFATHER_EXECUTION_IMAGE_URL = "https://cdn.discordapp.com/attachments/1483977619258212392/1485087458973450440/JMivfRSM1woZcZCwYUiFomJa5e6hG7Nss4xAl5wx1vzzoCkUrdxBlsSLh4M_79MjdKDh4q2kBDhucJpsrvZ7YNkuyVHHr_A32nhIGOsOafwBd0qwarqdazI1Z8mJeFvNMaa7vJX2ywZFd-mxzAtWug.webp?ex=69c096f9&is=69bf4579&hm=a9035324581fb576d6a0bb2c02a8fbae8d28152939f032bea8e0f61af822df61&"
    private const val HOSTESS_CONTACT_IMAGE_URL = "https://cdn.discordapp.com/attachments/1483977619258212392/1485092736318312649/qHqxEk0Ie1w1nYS_fuFrHR5Jo1CsmnD0_0naxqt7UIAYVQSU-8RaF44ld6eH7tVZTQ33iWE9g5Us0MSaagAuzLmDYDN_gkvqZdV1PeM2cDCVPNk8nxM9r91ynjwfTXW0nBSoZlKA2dWkoavBHN2ydw.webp?ex=69c09be4&is=69bf4a64&hm=409a014694a67993257f3d0cebdae9af68066675c7599f90237d21342678152d&"
    private const val MAD_SCIENTIST_CONTACT_IMAGE_URL = "https://cdn.discordapp.com/attachments/1483977619258212392/1485093676290932908/jGBpfxMpUm651gMgzzZYc9NW3p8lk63ct7CIfsVka5QbXqd9A78Zdj6w7Z14zlX5y0u_ynMq77dF33IZkM8ckr0otxYYAd_8CeYTLfvJ_syw2kA5AAMsWLWVO9bFqN-S2joct01Gmf8XvBAYEQTwCA.webp?ex=69c09cc4&is=69bf4b44&hm=1fcd876b2f860eb44be94cb450a1f7953a766db5a602e02312ce19949e312c1e&"
    private const val SPY_CONTACT_IMAGE_URL = "https://cdn.discordapp.com/attachments/1483977619258212392/1485095638931734699/36RXBo7-kuLUExBuE_kWriQbw5wVrunku4S93RbKCqX3p84cQ3DIICEpoeAzvUyyaUWGcqat9QOTar3r6T4nsDO-IYfUuoKVt1aAy7gNse3dAacQ5zYx1Ux3u43o9krFaspF-jD9VGDsgcsnibk6yg.webp?ex=69c09e98&is=69bf4d18&hm=08b19737b9c100da1541e49ec98ddd206247d43627045da5b9b1d534b9ae682e&"
    private const val THIEF_CONTACT_IMAGE_URL = "https://cdn.discordapp.com/attachments/1483977619258212392/1485097745164275963/6KbWZ5FCTcmZQ99obSXQ0HXU2Sq1UOPBlNdTs96-gHlcNTTgv6jyGZdBuaNfu7n2LQqDuYhhrnBTwAJ04Axd8tunc1CO6pGiBJSygpp7-h9HxZVuA0nr7ZUofIdZTsUEFZRwbPXWMq9rDkBJEQ_Qlw.webp?ex=69c0a08e&is=69bf4f0e&hm=7c03af3e798db7ce5e6019206a535c24db4d5ca1a1007d2c5111353d21d59de0&"
    private const val WITCH_CONTACT_IMAGE_URL = "https://cdn.discordapp.com/attachments/1483977619258212392/1485100396031512647/cl_vQE1Go9NQFqAYCyBILvrv-qFYsVe42chdCkpjiznJSYTHROE-kwXb9PJlRRr9uY2yjbLeR6eME2Dh02frBvCzBH1pZiabshT-szLZKU-gsYDjkC1KnJZQ3HAhVA6tJr8B9IAnu6yr9BY6nEbC1w.webp?ex=69c0a306&is=69bf5186&hm=e9eb66bc61eebee17923a4f6283586732332411f6ea4caf35abb4c457623b9d4&"
    private const val SWINDLER_CONTACT_IMAGE_URL = "https://cdn.discordapp.com/attachments/1483977619258212392/1485102540914692256/H8ETeBTIzzrPsPjS0hfbYZoKCkIgNWlsKjD_v29uvxV9Gm1waNRTl4YsmClfkeG_oQYEAlsyJh8fKm4JqZUPzDnCbl5ouVHjYeeiAcGVOfmaU9PYwVfPv1uDKV8JB8nirRsJY1TAVYaQ0E8Pf1rWIg.webp?ex=69c0a505&is=69bf5385&hm=0887e536593a5a9b353dcf4e232c179fed8639ce159f869f796614395884ee49&"
    private const val SPY_ASSASSIN_IMAGE_URL = "https://cdn.discordapp.com/attachments/1483977619258212392/1485096641777238167/vx4XGS33RUMMlC6eBroNoxpzuTPzExTknw3z7OcmjiI_i9eAt4ZfgK3mt_5GjjJou7jk_5IikTyiCwPRIpfWM7V5kFpk9fCd037ffupptkkCFjAKtoM8gyNHAfbs8km0y9Jatqj62P5DT-qTxRhW4w.webp?ex=69c09f87&is=69bf4e07&hm=704b998e8a12a5933c9f247db295a8eda1bff4beccad7e9584226cf2dfa7ac95&"
    private const val MAD_SCIENTIST_REVIVE_IMAGE_URL = "https://cdn.discordapp.com/attachments/1483977619258212392/1485094642797248675/1x0UtdbO43yTodQJcWduasjMRBL-CvRJQDc7MLLI04EjgNoGQvl4oTYrEA8_QbWmzROn3EEiTLxJjgTfSa8QOnE5SZ399XilwE2XVLvQwRa2KRR1PgfKXKiHaFUTul-AFzaxnY9pysnoTjd49VVG1A.webp?ex=69c09daa&is=69bf4c2a&hm=43b40604efc7f42f7fb23f2c8990fa865e9352ea8f07a0b22347ecde753921b8&"

    private var timeThreadChannel: ThreadChannel? = null
    private var timeStatusMessage: Message? = null
    private val countdownLock = Any()
    private var activeCountdown: ActiveCountdown? = null
    private val cabalNotificationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val votePresentationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    data class DayTimeAdjustmentResult(
        val isSuccess: Boolean,
        val message: String
    )

    private data class ActiveCountdown(
        val guildId: Snowflake,
        val phase: GamePhase,
        val label: String,
        var endAtMillis: Long,
        var forceFinished: Boolean = false
    )

    fun resetTimeThreadState() {
        timeThreadChannel = null
        timeStatusMessage = null
    }

    suspend fun clearTimeThread() {
        runCatching {
            timeThreadChannel?.delete("게임 종료로 인한 시간 스레드 정리")
        }
        resetTimeThreadState()
    }

    suspend fun adjustDayTimeByPlayer(game: Game, playerId: Snowflake, isIncrease: Boolean): DayTimeAdjustmentResult {
        val player = game.getPlayer(playerId)
            ?: return DayTimeAdjustmentResult(false, "게임 참가자만 시간을 조정할 수 있습니다.")

        if (player.state.isDead) {
            return DayTimeAdjustmentResult(false, "사망한 플레이어는 시간을 조정할 수 없습니다.")
        }

        if (game.currentPhase != GamePhase.DAY) {
            return DayTimeAdjustmentResult(false, "시간 조정은 낮 페이즈에서만 가능합니다.")
        }

        val delta = if (isIncrease) DAY_TIME_ADJUSTMENT_MS else -DAY_TIME_ADJUSTMENT_MS
        val remainingAfterAdjustment = synchronized(countdownLock) {
            if (game.dayTimeAdjustmentUsedPlayers.contains(playerId)) {
                return@synchronized null
            }

            val countdown = activeCountdown
            if (countdown == null ||
                countdown.guildId != game.guild.id ||
                countdown.phase != GamePhase.DAY ||
                countdown.label != "낮"
            ) {
                return@synchronized null
            }

            countdown.endAtMillis += delta
            val remaining = countdown.endAtMillis - System.currentTimeMillis()
            if (remaining <= 0L) {
                countdown.forceFinished = true
            }
            game.dayTimeAdjustmentUsedPlayers += playerId
            remaining
        } ?: return DayTimeAdjustmentResult(
            false,
            if (game.dayTimeAdjustmentUsedPlayers.contains(playerId)) {
                "하루에 한 번만 시간 조정을 사용할 수 있습니다."
            } else {
                "현재 조정 가능한 낮 카운트다운이 없습니다."
            }
        )

        updateTimeStatusMessage(game, "낮", remainingAfterAdjustment.coerceAtLeast(0L))

        if (!isIncrease && remainingAfterAdjustment <= 0L) {
            return DayTimeAdjustmentResult(
                true,
                "남은 시간이 0초 이하가 되어 즉시 다음 페이즈로 넘어갑니다."
            )
        }

        val actionText = if (isIncrease) "증가" else "감소"
        return DayTimeAdjustmentResult(
            true,
            "낮 시간을 15초 $actionText 했습니다. (하루 1회 사용 완료)"
        )
    }

    private suspend fun runPhaseCountdown(game: Game, label: String, durationMillis: Long) {
        val initialDuration = durationMillis.coerceAtLeast(0L)
        synchronized(countdownLock) {
            activeCountdown = ActiveCountdown(
                guildId = game.guild.id,
                phase = game.currentPhase,
                label = label,
                endAtMillis = System.currentTimeMillis() + initialDuration
            )
        }

        updateTimeStatusMessage(game, label, initialDuration)

        while (true) {
            val remainingMillis = synchronized(countdownLock) {
                val countdown = activeCountdown
                if (countdown == null || countdown.guildId != game.guild.id || countdown.phase != game.currentPhase) {
                    0L
                } else if (countdown.forceFinished) {
                    0L
                } else {
                    countdown.endAtMillis - System.currentTimeMillis()
                }
            }

            if (remainingMillis <= 0L) {
                break
            }

            delay(minOf(remainingMillis, 500L))
        }

        updateTimeStatusMessageAtZero(game, label)
        synchronized(countdownLock) {
            activeCountdown = null
        }
    }

    private suspend fun updateTimeStatusMessage(game: Game, phaseLabel: String, remainingMillis: Long) {
        val targetEpochSeconds = ((System.currentTimeMillis() + remainingMillis) / 1_000L).coerceAtLeast(0L)
        val content = "${game.dayCount}일차 $phaseLabel - <t:${targetEpochSeconds}:R>"

        editTimeStatusMessage(game, content)
    }

    private suspend fun updateTimeStatusMessageAtZero(game: Game, phaseLabel: String) {
        val content = "${game.dayCount}일차 $phaseLabel - 0초"
        editTimeStatusMessage(game, content)
    }

    private suspend fun editTimeStatusMessage(game: Game, content: String) {
        val statusMessage = ensureTimeStatusMessage(game) ?: return

        runCatching {
            statusMessage.edit {
                this.content = content
            }
        }.onFailure {
            timeStatusMessage = null
            val recreated = ensureTimeStatusMessage(game) ?: return
            recreated.edit {
                this.content = content
            }
        }
    }

    private suspend fun ensureTimeStatusMessage(game: Game): Message? {
        if (timeStatusMessage != null) return timeStatusMessage

        val threadChannel = ensureTimeThread(game) ?: return null
        return runCatching {
            threadChannel.createMessage("시간 정보를 준비 중입니다...")
        }.onSuccess {
            timeStatusMessage = it
        }.getOrNull()
    }

    private suspend fun ensureTimeThread(game: Game): ThreadChannel? {
        timeThreadChannel?.let { return it }

        val mainChannel = game.mainChannel ?: return null
        return runCatching {
            mainChannel.startPublicThread(TIME_THREAD_NAME)
        }.onSuccess {
            timeThreadChannel = it
        }.onFailure {
            timeThreadChannel = null
            timeStatusMessage = null
        }.getOrNull()
    }

    suspend fun startNightPhase(game: Game) {
        notifyMindReadingResults(game)
        game.currentPhase = GamePhase.NIGHT
        game.dayCount += 1
        processMadScientistNightTransitions(game)
        game.nightPhaseStartedAtMillis = System.currentTimeMillis()
        FrogCurseManager.clearExpiredAtNightStart(game)
        game.prophetSpecialWinScheduledTeam = null
        game.abilityUsersThisPhase.clear()
        game.abilityTargetByUserThisPhase.clear()
        game.nightAttacks.clear()
        game.nightDeathCandidates.clear()
        game.pendingNightDeathPlayerIds.clear()
        game.nightEvents.clear()
        game.pendingWitchCurseByCaster.clear()
        game.pendingOblivionCurseByCaster.clear()
        game.pendingDayStartDiscoveries.clear()
        game.concealmentForcedQuietNight = false
        game.megaphoneUsedTonight = false
        game.willByPlayerId.clear()
        game.coupleSacrificeMap.clear()
        game.activeThreatenedVoters.clear()
        game.probationOriginalJobsByPlayer.clear()
        game.lastNightSummary = NightResolutionSummary()
        game.playerDatas.forEach { player ->
            player.state.isThreatened = false
        }
        game.playerDatas.forEach { player ->
            (player.job as? Cabal)?.let { cabalJob ->
                cabalJob.moonMarkedSunTonight = false
                cabalJob.cabalSpecialWinReady = false
            }
            (player.job as? Police)?.let { policeJob ->
                policeJob.currentSearchTarget = null
                policeJob.hasUsedSearchThisNight = false
            }
            (player.job as? Detective)?.let {
                DetectiveAbility.resetNightState(player)
            }
            (player.job as? Administrator)?.let { administratorJob ->
                administratorJob.investigationResultPlayerId = null
            }
            (player.job as? Gangster)?.prepareNightThreatSelection()
            (player.job as? Hypnotist)?.selectedTargetIdTonight = null
            (player.job as? HitMan)?.let { hitMan ->
                hitMan.firstContractTargetId = null
                hitMan.firstContractGuessedJobName = null
            }
            (player.job as? Spy)?.remainingIntelUsesTonight = 1
            (player.job as? Mentalist)?.let {
                MentalistAbility.resetDayState(player)
            }
        }
        resolveCabalSunInvestigation(game)
        applyPoliceConfidentialInvestigation(game)

        game.sendMainChannelMessageWithImage(
            imageLink = "https://cdn.discordapp.com/attachments/1483977619258212392/1483978042673070342/43e6c3860a090af9.png?ex=69be8800&is=69bd3680&hm=1dabf5630544f8f8766c7abbb0793a48e3a11e1364a31d1e4e439fff70539e25&",
            message = "밤이 되었습니다."
        )
        announceSourceMafiaCountAtNightStart(game)
        resolveHackerHacks(game)
        val nightStartEvents = dispatchEvents(game)
        JobDiscoveryNotificationManager.notifyDiscoveredTargets(nightStartEvents, game)
        notifyMercenaryClientsAtFirstNight(game)

        val mainChannel = game.mainChannel ?: return
        val mafiaChannel = game.mafiaChannel ?: return
        val coupleChannel = game.coupleChannel ?: return
        val deadChannel = game.deadChannel ?: return
        val alivePlayers = game.playerDatas.filter { !it.state.isDead }

        game.playerDatas.forEach { player ->
            runCatching {
                player.member.edit {
                    muted = true
                }
            }
        }
        applyHostessSeductionStates(game)

        mainChannel.edit {
            addRoleOverwrite(game.guild.id) {
                denied = Permissions(Permission.SendMessages, Permission.ReadMessageHistory)
                allowed = Permissions(Permission.SendMessages, Permission.UseApplicationCommands)
            }
        }
        updateMafiaChannelPermissions(game, mafiaChannel, isNight = true)
        updateCoupleChannelPermissions(game, coupleChannel, isNight = true)
        updateDeadChannelPermissions(game, deadChannel)

        mafiaChannel.edit {
            addRoleOverwrite(game.guild.id) {
                allowed = Permissions(Permission.SendMessages, Permission.UseApplicationCommands)
                denied = Permissions(Permission.ReadMessageHistory)
            }
        }

        alivePlayers.forEach { player ->
            player.allAbilities
                 .filterIsInstance<PassiveAbility>()
                .filterNot { FrogCurseManager.shouldSuppressPassive(player) }
                .sortedByDescending(PassiveAbility::priority)
                .forEach { passive ->
                    passive.onPhaseChanged(game, player, GamePhase.NIGHT)
                }
        }
    }

    suspend fun resolveNightPhase(game: Game): NightResolutionSummary {
        val blockedAttacks = mutableListOf<AttackEvent>()
        val playersToDie = linkedSetOf<PlayerData>().apply {
            addAll(game.nightDeathCandidates)
        }
        game.doctorSavedTargetTonight = null

        resolveGangsterThreats(game)
        resolveNursePrescriptions(game)
        resolveDoctorHeals(game)
        resolveAdministratorInvestigations(game)
        resolveReporterScoops(game)
        applyBeastmanExecutionOverride(game)

        game.nightAttacks.values.forEach { attackEvent ->
            val target = attackEvent.target
            if (target.state.isDead) return@forEach

            if (isExecutionImmuneBeastmanTarget(game, attackEvent)) {
                blockedAttacks += attackEvent
                playersToDie.remove(target)
                return@forEach
            }

            // 패시브(방탄 등)가 방어력(healTier)에 개입할 기회를 주기 위한 평가 이벤트 통보
            game.nightEvents += GameEvent.BeforeAttackEvaluated(attackEvent)
            dispatchEvents(game)

            if (target.state.healTier.level >= attackEvent.attackTier.level) {
                blockedAttacks += attackEvent
                playersToDie.remove(target)

                val healedByDoctor = game.nightEvents.filterIsInstance<GameEvent.PlayerHealed>().any { it.target == target }
                if (healedByDoctor) {
                    game.doctorSavedTargetTonight = target
                }
            } else {
                playersToDie += target
            }
        }

        resolveMercenaryAttackOrder(game, blockedAttacks, playersToDie)
        resolveVigilanteAttackOrder(game, blockedAttacks, playersToDie)
        resolveMercenaryContractDeaths(game, blockedAttacks, playersToDie)

        resolveMartyrNightExplosions(game, playersToDie)

        val mafiaAttack = game.nightAttacks["MAFIA_TEAM"]
        if (mafiaAttack != null) {
            val selectedMafiaTarget = resolveOriginallySelectedMafiaTarget(game, mafiaAttack)
            var swindlerNegotiationBlockedExecution = false

            SwindlerManager.shouldTriggerNegotiation(game, selectedMafiaTarget)?.let { (swindlerPlayer, swindlerWasMafiaTarget) ->
                if (swindlerWasMafiaTarget) {
                    playersToDie.remove(swindlerPlayer)
                    game.concealmentForcedQuietNight = true
                    swindlerNegotiationBlockedExecution = true
                }
                SwindlerManager.contactMafia(game, swindlerPlayer)
            }

            val targetSurvived = mafiaAttack.target !in playersToDie
            game.mafiaAttackFailedPreviousNight = targetSurvived

            if (targetSurvived) {
                if (!swindlerNegotiationBlockedExecution) {
                    applyMafiaExecutionFailureEffects(game, mafiaAttack)
                }
            } else {
                registerCoupleResentment(game, mafiaAttack)
                applyMafiaExecutionSuccessEffects(game, mafiaAttack)
            }
        } else {
            game.mafiaAttackFailedPreviousNight = false
        }
        applyTravelCompanionPenalty(game, playersToDie, mafiaAttack)

        playersToDie.forEach { victim ->
            game.nightEvents += GameEvent.PlayerDied(victim)
        }

        org.beobma.mafia42discordproject.job.ability.general.evil.list.witch.WitchAbility.applyOblivionCursesAtNightEnd(game)
        val processedEvents = dispatchEvents(game)
        cacheReporterDiscoveryResults(processedEvents)
        val deferredProcessedEvents = processedEvents.filterIsInstance<GameEvent.JobDiscovered>()
            .filter(::shouldNotifyAtDayStart)
        if (deferredProcessedEvents.isNotEmpty()) {
            game.pendingDayStartDiscoveries += deferredProcessedEvents
        }
        JobDiscoveryNotificationManager.notifyDiscoveredTargets(processedEvents.filterNot(::shouldNotifyAtDayStart))
        val deaths = playersToDie.toList()
        val dawnPresentation = buildDawnPresentation(game, deaths)

        // 아침 이벤트(예: 도굴꾼 JobDiscovered) 해소를 위한 추가 디스패치 파이프라인 보수 및 유실 파기 방지
        val additionalProcessedEvents = dispatchEvents(game)
        cacheReporterDiscoveryResults(additionalProcessedEvents)
        val additionalDeferredEvents = additionalProcessedEvents.filterIsInstance<GameEvent.JobDiscovered>()
            .filter(::shouldNotifyAtDayStart)
        if (additionalDeferredEvents.isNotEmpty()) {
            game.pendingDayStartDiscoveries += additionalDeferredEvents
        }
        JobDiscoveryNotificationManager.notifyDiscoveredTargets(additionalProcessedEvents.filterNot(::shouldNotifyAtDayStart))

        val summary = NightResolutionSummary(
            processedEvents = processedEvents + additionalProcessedEvents,
            deaths = deaths,
            blockedAttacks = blockedAttacks.toList(),
            dawnPresentation = dawnPresentation
        )
        game.lastNightSummary = summary

        game.nightAttacks.clear()
        game.nightDeathCandidates.clear()
        game.pendingNightDeathPlayerIds.clear()
        game.nightEvents.clear()
        game.playerDatas.forEach { player ->
            (player.job as? Doctor)?.currentHealTarget = null
            (player.job as? Nurse)?.currentHealTarget = null
            (player.job as? Gangster)?.finalizeNightThreatSelection()
            (player.job as? Hypnotist)?.let { hypnotist ->
                if (hypnotist.blockedNightsRemaining > 0) {
                    hypnotist.blockedNightsRemaining -= 1
                }
            }
            player.state.resetForNextPhase()
        }

        return summary
    }

    suspend fun resolveDawnPhase(game: Game, summary: NightResolutionSummary = game.lastNightSummary) {
        game.currentPhase = GamePhase.DAWN

        val poisonedVictims = game.playerDatas.filter { player ->
            !player.state.isDead &&
                player.state.isPoisoned &&
                player.state.poisonedDeathDay != null &&
                game.dayCount >= player.state.poisonedDeathDay!!
        }
        poisonedVictims.forEach { victim ->
            victim.state.isPoisoned = false
            victim.state.poisonedDeathDay = null
            if (victim !in summary.deaths) {
                victim.state.isDead = true
                handleMadScientistDeath(game, victim, isLynch = false)
                game.nightEvents += GameEvent.PlayerDied(victim)
                applyPoliceAutopsy(game, victim)
                SpyAbility.applyAutopsyOnDeath(game, victim)
                revealBelongingsIfNeeded(game, victim)
            }
        }

        summary.deaths.forEach { victim ->
            if (victim.state.isDead) return@forEach
            victim.state.isDead = true
            handleMadScientistDeath(game, victim, isLynch = false)
            game.nightEvents += GameEvent.PlayerDied(victim)
            applyPoliceAutopsy(game, victim)
            SpyAbility.applyAutopsyOnDeath(game, victim)
            revealBelongingsIfNeeded(game, victim)
        }
        resolvePriestResurrection(game, summary)
        notifyPendingBeastmanTaming(game)

        announceCoupleSacrificeReveal(game, summary.deaths)

        val processedDawnEvents = dispatchEvents(game)
        resolveSpyAssassin(game)
        resolveCabalSpecialWinReadiness(game)
        resolveProphetPioneerSpecialWinReadiness(game, summary)
        val dawnDeaths = (summary.deaths + poisonedVictims).distinct()
        revealNightWillIfNeeded(game, dawnDeaths)
        val dawnPresentation = buildDawnPresentation(
            game = game,
            deaths = dawnDeaths,
            poisonedDeaths = poisonedVictims
        )
        if (dawnPresentation.message.isNotBlank() || dawnPresentation.imageUrl.isNotBlank()) {
            game.sendMainChannelMessageWithImage(
                imageLink = dawnPresentation.imageUrl,
                message = dawnPresentation.message
            )
        }

        game.lastNightSummary = summary.copy(
            processedEvents = summary.processedEvents + processedDawnEvents,
            dawnPresentation = dawnPresentation
        )

        game.nightEvents.clear()
        game.coupleSacrificeMap.clear()
    }

    private suspend fun resolvePriestResurrection(game: Game, summary: NightResolutionSummary) {
        game.playerDatas.forEach { priestPlayer ->
            val priestJob = priestPlayer.job as? Priest ?: return@forEach
            val targetId = priestJob.pendingResurrectionTargetId ?: return@forEach
            priestJob.pendingResurrectionTargetId = null

            if (priestPlayer.state.isDead) {
                game.sendMainChannerMessage("성직자 ${priestPlayer.member.effectiveName}님이 사망하여 소생이 취소되었습니다.")
                return@forEach
            }

            val target = game.getPlayer(targetId) ?: return@forEach
            if (!target.state.isDead) {
                game.sendMainChannerMessage("${target.member.effectiveName}님은 이미 생존 상태여서 소생이 실패했습니다.")
                return@forEach
            }

            val hasExorcism = priestPlayer.allAbilities.any { it is PriestExorcism }
            if (target.state.isShamaned && !hasExorcism) {
                game.sendMainChannerMessage("${target.member.effectiveName}님은 성불 상태여서 소생이 실패했습니다.")
                return@forEach
            }

            val hasBlessing = priestPlayer.allAbilities.any { it is Blessing }
            if (!hasBlessing) {
                target.job = Citizen()
            }

            target.state.isDead = false
            target.state.isShamaned = false
            target.state.isPoisoned = false
            target.state.poisonedDeathDay = null
            if (target.job is MadScientist) {
                target.state.pendingMadScientistRevivalNight = null
                target.state.pendingMadScientistPublicRevealNight = null
                target.state.isMadScientistDistortionHidden = false
                target.state.madScientistAnalysisEligibleDay = null
                target.state.hasUsedMadScientistAnalysis = false
            }
            game.publiclyRevealedAbilityTargetIds += target.member.id

            game.sendMainChannerMessage("성직자의 소생으로 ${target.member.effectiveName}님이 부활했습니다.")
        }
    }

    private fun registerCoupleResentment(game: Game, mafiaAttack: AttackEvent) {
        val victimCouple = mafiaAttack.target.job as? Couple ?: return
        val partnerId = victimCouple.pairedPlayerId ?: return
        val partner = game.getPlayer(partnerId) ?: return
        val partnerCouple = partner.job as? Couple ?: return
        if (partner.state.isDead) return

        partnerCouple.avengedMafiaIds += mafiaAttack.attacker.member.id
    }

    private suspend fun announceCoupleSacrificeReveal(game: Game, deaths: List<PlayerData>) {
        val mainChannel = game.mainChannel ?: return

        deaths.forEach { deadPlayer ->
            val originalTargetId = game.coupleSacrificeMap[deadPlayer.member.id] ?: return@forEach
            val originalTarget = game.getPlayer(originalTargetId) ?: return@forEach

            deadPlayer.state.isJobPubliclyRevealed = true
            originalTarget.state.isJobPubliclyRevealed = true

            val deadRole = (deadPlayer.job as? Couple)?.role
            val originalRole = (originalTarget.job as? Couple)?.role.toDisplayName()
            val deadJobName = deadPlayer.job?.name ?: "알 수 없음"
            val originalJobName = originalTarget.job?.name ?: "알 수 없음"

            // 1. 성별에 따른 이미지 URL 선택
            val imageUrl = when (deadRole) {
                CoupleRole.MALE -> SystemImage.DEATH_MALE_COUPLE.imageUrl
                CoupleRole.FEMALE -> SystemImage.DEATH_WOMAN_COUPLE.imageUrl
                else -> SystemImage.DEATH_BY_MAFIA.imageUrl
            }

            // 2. 메시지 구성
            val message = "연인의 희생이 발동했습니다. ${originalTarget.member.effectiveName}(${originalRole})의 대가로 ${deadPlayer.member.effectiveName}(${deadRole?.toDisplayName() ?: "미정"})가 대신 사망했습니다.\n" +
                    "직업 공개: ${originalTarget.member.effectiveName} - ${originalJobName}, ${deadPlayer.member.effectiveName} - ${deadJobName}"

            // 3. 텍스트 대신 이미지와 함께 전송
            game.sendMainChannelMessageWithImage(
                imageLink = imageUrl,
                message = message
            )
        }
    }

    private fun CoupleRole?.toDisplayName(): String = when (this) {
        CoupleRole.MALE -> "남성"
        CoupleRole.FEMALE -> "여성"
        null -> "미정"
    }

    suspend fun startDayPhase(
        game: Game,
        summary: NightResolutionSummary = game.lastNightSummary
    ) {
        game.unwrittenRuleBlockedTargetIdTonight = null
        val mainChannel = game.mainChannel ?: return
        val mafiaChannel = game.mafiaChannel ?: return
        val coupleChannel = game.coupleChannel ?: return
        val deadChannel = game.deadChannel ?: return

        // 1. 게임 상태 및 날짜 변경
        game.currentPhase = GamePhase.DAY
        game.dayTimeAdjustmentUsedPlayers.clear()
        game.abilityUsersThisPhase.clear()
        game.abilityTargetByUserThisPhase.clear()
        notifyMercenaryContractReception(game)
        game.playerDatas.forEach { player ->
            (player.job as? Thief)?.clearStolenAbility()
        }

        game.sendMainChannelMessageWithImage(
            imageLink = SystemImage.DAY_START.imageUrl,
            message = "낮이 되었습니다."
        )
        applyHostessSeductionStates(game)
        if (game.pendingDayStartDiscoveries.isNotEmpty()) {
            JobDiscoveryNotificationManager.notifyDiscoveredTargets(game.pendingDayStartDiscoveries.toList(), game)
            game.pendingDayStartDiscoveries.clear()
        }
        deliverSecretLetters(game)
        notifyPendingPoisonEffects(game)
        notifyInstructionsAtFirstDay(game)
        notifyTheInformantAutoContactAtSecondDay(game)

        mainChannel.edit {
            addRoleOverwrite(game.guild.id) {
                allowed = Permissions(Permission.SendMessages)
                denied = Permissions()
            }

            game.playerDatas.forEach { player ->
                if (shouldRestrictCommunication(player)) {
                    addMemberOverwrite(player.member.id) {
                        denied = Permissions(Permission.SendMessages)
                    }
                } else {
                    addMemberOverwrite(player.member.id) {
                        denied = Permissions()
                    }
                }
            }
        }

        game.playerDatas.forEach { player ->
            val shouldMute = shouldRestrictCommunication(player)
            runCatching {
                player.member.edit {
                    muted = shouldMute
                }
            }
        }

        updateMafiaChannelPermissions(game, mafiaChannel, isNight = false)
        updateCoupleChannelPermissions(game, coupleChannel, isNight = false)
        updateDeadChannelPermissions(game, deadChannel)
        AdministratorInvestigationNotificationManager.notifyResults(game)
        publishReporterArticles(game)

        game.playerDatas
            .filter { !it.state.isDead }
            .forEach { player ->
                player.allAbilities
                     .filterIsInstance<PassiveAbility>()
                    .filterNot { FrogCurseManager.shouldSuppressPassive(player) }
                    .sortedByDescending(PassiveAbility::priority)
                    .forEach { passive ->
                        passive.onPhaseChanged(game, player, GamePhase.DAY)
                    }
            }

        notifyBeastmanRoarAtFirstDay(game)
    }

    private suspend fun updateMafiaChannelPermissions(game: Game, mafiaChannel: TextChannel, isNight: Boolean) {
        mafiaChannel.edit {
            addRoleOverwrite(game.guild.id) {
                denied = Permissions(
                    Permission.ViewChannel,
                    Permission.ReadMessageHistory,
                    Permission.SendMessages
                )
            }

            game.playerDatas.forEach { player ->
                if (player.state.isDead) {
                    addMemberOverwrite(player.member.id) {
                        allowed = Permissions(Permission.ViewChannel, Permission.ReadMessageHistory)
                        denied = Permissions(Permission.SendMessages)
                    }
                    return@forEach
                }

                if (canAccessMafiaChannel(game, player)) {
                    val canSend = isNight && !shouldRestrictCommunication(player)
                    addMemberOverwrite(player.member.id) {
                        allowed = Permissions(Permission.ViewChannel, Permission.ReadMessageHistory)
                        denied = if (canSend) Permissions() else Permissions(Permission.SendMessages)
                    }
                } else {
                    addMemberOverwrite(player.member.id) {
                        denied = Permissions(
                            Permission.ViewChannel,
                            Permission.ReadMessageHistory,
                            Permission.SendMessages
                        )
                    }
                }
            }
        }

        notifyGodfatherContactInMafiaChannel(game, mafiaChannel)
    }

    private suspend fun notifyGodfatherContactInMafiaChannel(game: Game, mafiaChannel: TextChannel) {
        game.playerDatas.forEach { player ->
            if (player.state.isDead) return@forEach
            if (player.job !is Godfather) return@forEach
            if (player.state.hasAnnouncedGodfatherContact) return@forEach
            if (!GodfatherContactPolicy.canContactMafia(game)) return@forEach

            player.state.hasAnnouncedGodfatherContact = true
            mafiaChannel.createMessage("$GODFATHER_CONTACT_IMAGE_URL\n접선했습니다.")
        }
    }

    private fun canAccessMafiaChannel(game: Game, player: PlayerData): Boolean {
        return when {
            player.job is Mafia -> true
            player.job is Beastman && player.state.isTamed -> true
            player.state.hasContactedMafiaByInformant -> true
            player.job is Godfather && GodfatherContactPolicy.canContactMafia(game) -> true
            player.job is HitMan && (player.job as HitMan).hasContactedMafia -> true
            player.job is Hostess && (player.job as Hostess).hasContactedMafia -> true
            player.job is MadScientist && player.state.hasContactedMafiaOnDeath -> true
            player.job is Spy && (player.job as Spy).hasContactedMafia -> true
            player.job is Swindler && (player.job as Swindler).hasContactedMafia -> true
            player.job is Thief && (player.job as Thief).hasContactedMafia -> true
            player.job is Witch && (player.job as Witch).hasContactedMafia -> true
            else -> false
        }
    }

    private suspend fun resolveSpyAssassin(game: Game) {
        val aliveMafia = game.playerDatas.filter { !it.state.isDead && it.job is Mafia }
        if (aliveMafia.isNotEmpty()) return

        val aliveEvil = game.playerDatas.filter { !it.state.isDead && it.job is Evil }
        if (aliveEvil.size != 1) return

        val spyPlayer = aliveEvil.firstOrNull { it.job is Spy } ?: return
        val spyJob = spyPlayer.job as? Spy ?: return
        if (spyJob.hasTriggeredAssassin) return

        val targetId = spyJob.lastInvestigatedTargetId ?: return
        val target = game.getPlayer(targetId) ?: return
        if (target.state.isDead) return
        if (target.member.id == spyPlayer.member.id) return

        spyJob.hasTriggeredAssassin = true
        target.state.isDead = true
        handleMadScientistDeath(game, target, isLynch = false)
        game.nightEvents += GameEvent.PlayerDied(target)
        applyPoliceAutopsy(game, target)
        SpyAbility.applyAutopsyOnDeath(game, target)
        revealBelongingsIfNeeded(game, target)

        game.sendMainChannelMessageWithImage(
            imageLink = SPY_ASSASSIN_IMAGE_URL,
            message = "**${target.member.effectiveName}이(가) 자객에 의해 살해당하였습니다.**"
        )
    }

    fun isMadScientistDistortionHidden(player: PlayerData): Boolean {
        return player.job is MadScientist && player.state.isMadScientistDistortionHidden
    }

    private fun shouldRestrictCommunication(player: PlayerData): Boolean {
        return player.state.isDead || player.state.isSilenced || isMadScientistDistortionHidden(player)
    }

    private fun isMafiaEliminated(game: Game): Boolean {
        return game.playerDatas.none { !it.state.isDead && it.job is Mafia }
    }

    private suspend fun processMadScientistNightTransitions(game: Game) {
        val mainChannel = game.mainChannel
        game.playerDatas.forEach { player ->
            if (player.job !is MadScientist) return@forEach

            val revealNight = player.state.pendingMadScientistPublicRevealNight
            if (revealNight != null && revealNight <= game.dayCount) {
                player.state.pendingMadScientistPublicRevealNight = null
                player.state.isMadScientistDistortionHidden = false
                if (mainChannel != null) {
                    game.sendMainChannelMessageWithImage(
                        imageLink = MAD_SCIENTIST_REVIVE_IMAGE_URL,
                        message = "${player.member.effectiveName}님이 부활하셨습니다!"
                    )
                }
            }

            val reviveNight = player.state.pendingMadScientistRevivalNight
            if (reviveNight == null || reviveNight > game.dayCount) return@forEach
            player.state.pendingMadScientistRevivalNight = null
            if (!player.state.isDead) {
                player.state.pendingMadScientistPublicRevealNight = null
                player.state.isMadScientistDistortionHidden = false
                return@forEach
            }

            if (player.state.isShamaned || isMafiaEliminated(game)) {
                player.state.isMadScientistDistortionHidden = false
                player.state.pendingMadScientistPublicRevealNight = null
                return@forEach
            }

            player.state.isDead = false
            player.state.isShamaned = false
            player.state.isPoisoned = false
            player.state.poisonedDeathDay = null
            player.state.madScientistAnalysisEligibleDay = game.dayCount
            player.state.hasUsedMadScientistAnalysis = false
            game.publiclyRevealedAbilityTargetIds += player.member.id

            val hasDistortion = player.allAbilities.any { it is Distortion }
            if (hasDistortion) {
                player.state.isMadScientistDistortionHidden = true
                player.state.pendingMadScientistPublicRevealNight = game.dayCount + 1
            } else {
                player.state.isMadScientistDistortionHidden = false
                player.state.pendingMadScientistPublicRevealNight = null
                if (mainChannel != null) {
                    game.sendMainChannelMessageWithImage(
                        imageLink = MAD_SCIENTIST_REVIVE_IMAGE_URL,
                        message = "${player.member.effectiveName}님이 부활하셨습니다!"
                    )
                }
            }
        }
    }

    private suspend fun handleMadScientistDeath(game: Game, victim: PlayerData, isLynch: Boolean) {
        if (victim.job !is MadScientist) return
        if (!victim.state.hasUsedMadScientistRegeneration) {
            victim.state.hasUsedMadScientistRegeneration = true
            victim.state.pendingMadScientistRevivalNight = game.dayCount + 1
            victim.state.madScientistLynchedVoteTargetId = if (isLynch) {
                game.currentMainVotes[victim.member.id]?.let(::Snowflake)
            } else {
                null
            }
        }

        victim.state.isMadScientistDistortionHidden = false
        victim.state.pendingMadScientistPublicRevealNight = null

        if (!victim.state.hasContactedMafiaOnDeath) {
            victim.state.hasContactedMafiaOnDeath = true
            game.mafiaChannel?.let { mafiaChannel ->
                if (!victim.state.hasAnnouncedMadScientistContact) {
                    victim.state.hasAnnouncedMadScientistContact = true
                    mafiaChannel.createMessage("$MAD_SCIENTIST_CONTACT_IMAGE_URL\n접선했습니다.")
                }
            }
        }

        refreshMafiaChannelContactState(game)
    }

    suspend fun refreshMafiaChannelContactState(game: Game) {
        val mafiaChannel = game.mafiaChannel ?: return
        updateMafiaChannelPermissions(game, mafiaChannel, isNight = game.currentPhase == GamePhase.NIGHT)
    }

    suspend fun notifyHitmanContact(game: Game, hitmanPlayer: PlayerData) {
        val mafiaChannel = game.mafiaChannel ?: return
        if (hitmanPlayer.state.hasAnnouncedHitmanContact) return
        hitmanPlayer.state.hasAnnouncedHitmanContact = true
        mafiaChannel.createMessage(
            "https://cdn.discordapp.com/attachments/1483977619258212392/1485090211133259897/oRhn9TDiSQ7IEZDLWEVkdWYUpg-z9zOnCQ_eHxm0HDM0NUe21_6HbCdPQFIjCFqMnm38e_wbu4BZlT3Zx__1qU4k9-jkCaMyxCOPeHTxxhdaX3j_BVvsInUZvtVOOUfm5zFotdXpbKKrsg-lvqodkg.webp?ex=69c09989&is=69bf4809&hm=2cbcf46a67886753867f3c144e8eb30185fa3c23c3a97f544097880102a89290&\n접선했습니다."
        )
        refreshMafiaChannelContactState(game)
    }

    private fun applyHostessSeductionStates(game: Game) {
        game.playerDatas.forEach { target ->
            target.state.isSilenced = isSeducedAtCurrentTime(game, target)
        }
    }

    private fun isSeducedAtCurrentTime(game: Game, target: PlayerData): Boolean {
        if (target.state.isDead) return false
        val seduction = game.seductionStatusByTarget[target.member.id] ?: return false
        if (target.job is Soldier && target.allAbilities.any { it is MentalStrength }) return false

        val hostess = game.getPlayer(seduction.hostessId)
        val hostessAlive = hostess != null && !hostess.state.isDead && hostess.job is Hostess
        return seduction.isPermanent || hostessAlive || game.dayCount <= seduction.minimumReleaseDay
    }

    private suspend fun applyHostessSeductionFromVote(game: Game) {
        val seductionTargetsByHostess = mutableMapOf<PlayerData, MutableSet<Snowflake>>()

        game.currentMainVotes.forEach { (voterId, targetIdString) ->
            val voter = game.getPlayer(voterId) ?: return@forEach
            if (voter.state.isDead || voter.job !is Hostess) return@forEach
            val target = game.getPlayer(Snowflake(targetIdString)) ?: return@forEach
            if (target.state.isDead) return@forEach
            seductionTargetsByHostess.getOrPut(voter) { mutableSetOf() } += target.member.id
        }

        if (game.dayCount == 1) {
            game.hostessFirstVoteTargetByDay.forEach { (hostessId, firstTargetId) ->
                val hostessPlayer = game.getPlayer(hostessId) ?: return@forEach
                if (hostessPlayer.state.isDead || hostessPlayer.job !is Hostess) return@forEach
                val firstTarget = game.getPlayer(firstTargetId) ?: return@forEach
                if (firstTarget.state.isDead) return@forEach
                seductionTargetsByHostess.getOrPut(hostessPlayer) { mutableSetOf() } += firstTarget.member.id
            }
        }

        seductionTargetsByHostess.forEach { (hostessPlayer, targetIds) ->
            val hostessJob = hostessPlayer.job as? Hostess ?: return@forEach
            targetIds.forEach { targetId ->
                applyHostessSeduction(
                    game = game,
                    hostessPlayer = hostessPlayer,
                    hostessJob = hostessJob,
                    targetId = targetId
                )
            }
        }
        applyHostessSeductionStates(game)
    }

    private suspend fun applyHostessSeduction(
        game: Game,
        hostessPlayer: PlayerData,
        hostessJob: Hostess,
        targetId: Snowflake
    ) {
        val target = game.getPlayer(targetId) ?: return
        if (target.state.isDead) return
        if (target.job is Soldier && target.allAbilities.any { it is MentalStrength }) return

        val hasDeception = hostessPlayer.allAbilities.any { it is Deception }
        val minimumReleaseDay = game.dayCount + 1
        val existing = game.seductionStatusByTarget[target.member.id]
        if (existing == null || existing.minimumReleaseDay < minimumReleaseDay) {
            game.seductionStatusByTarget[target.member.id] = SeductionStatus(
                hostessId = hostessPlayer.member.id,
                minimumReleaseDay = minimumReleaseDay,
                isPermanent = hasDeception
            )
        } else if (hasDeception) {
            existing.isPermanent = true
        }

        if (!hostessJob.hasContactedMafia && target.job is Mafia) {
            hostessJob.hasContactedMafia = true
            game.mafiaChannel?.createMessage("$HOSTESS_CONTACT_IMAGE_URL\n접선했습니다.")
            refreshMafiaChannelContactState(game)
        }
    }

    private fun applyBeastmanExecutionOverride(game: Game) {
        val mafiaAttack = game.nightAttacks["MAFIA_TEAM"] ?: return
        val selectedTarget = resolveOriginallySelectedMafiaTarget(game, mafiaAttack)

        val triggeredBeastman = game.playerDatas.firstOrNull { player ->
            !player.state.isDead &&
                !player.state.isTamed &&
                player.job is Beastman &&
                selectedTarget.member.id in (player.job as Beastman).markedTargetIds
        } ?: return

        if (selectedTarget != mafiaAttack.target) {
            game.nightDeathCandidates.remove(mafiaAttack.target)
            game.coupleSacrificeMap.remove(mafiaAttack.target.member.id)
        }

        game.nightAttacks["MAFIA_TEAM"] = AttackEvent(
            attacker = triggeredBeastman,
            target = selectedTarget,
            attackTier = AttackTier.ABSOLUTE
        )
        if (selectedTarget !in game.nightDeathCandidates) {
            game.nightDeathCandidates += selectedTarget
        }
        game.pendingBeastmanTameIds += triggeredBeastman.member.id
    }

    private fun resolveOriginallySelectedMafiaTarget(game: Game, mafiaAttack: AttackEvent): PlayerData {
        val selectedTargetId = game.coupleSacrificeMap[mafiaAttack.target.member.id] ?: return mafiaAttack.target
        return game.getPlayer(selectedTargetId) ?: mafiaAttack.target
    }

    private fun isExecutionImmuneBeastmanTarget(game: Game, attackEvent: AttackEvent): Boolean {
        if (attackEvent.target.job !is Beastman) return false

        val attackKey = game.nightAttacks.entries
            .firstOrNull { (_, event) -> event == attackEvent }
            ?.key
            ?: return false

        return attackKey == "MAFIA_TEAM" || attackKey.startsWith("MERCENARY_")
    }

    private suspend fun notifyPendingBeastmanTaming(game: Game) {
        if (game.pendingBeastmanTameIds.isEmpty()) return

        val targetIds = game.pendingBeastmanTameIds.toSet()
        game.pendingBeastmanTameIds.clear()

        val mafiaPlayers = game.playerDatas.filter { it.job is Mafia }
        targetIds.forEach { beastmanId ->
            val beastmanPlayer = game.getPlayer(beastmanId) ?: return@forEach
            if (beastmanPlayer.state.isDead) return@forEach
            if (beastmanPlayer.job !is Beastman) return@forEach

            beastmanPlayer.state.isTamed = true

            runCatching {
                beastmanPlayer.member.getDmChannel().createMessage("$BEASTMAN_TAMED_IMAGE_URL\n길들여졌습니다.")
            }

            mafiaPlayers.forEach { mafiaPlayer ->
                runCatching {
                    mafiaPlayer.member.getDmChannel().createMessage("$BEASTMAN_TAMED_IMAGE_URL\n접선했습니다")
                }
            }
        }
    }

    private suspend fun notifyBeastmanRoarAtFirstDay(game: Game) {
        if (game.dayCount != 1) return
        val mafiaChannel = game.mafiaChannel ?: return

        val hasAliveRoarBeastman = game.playerDatas.any { player ->
            !player.state.isDead && player.job is Beastman && player.allAbilities.any { it is Roar }
        }
        if (!hasAliveRoarBeastman) return

        mafiaChannel.createMessage("$BEASTMAN_ROAR_IMAGE_URL\n짐승의 포효소리가 들려왔습니다!")
    }

    private suspend fun updateCoupleChannelPermissions(game: Game, coupleChannel: TextChannel, isNight: Boolean) {
        coupleChannel.edit {
            addRoleOverwrite(game.guild.id) {
                denied = Permissions(
                    Permission.ViewChannel,
                    Permission.ReadMessageHistory,
                    Permission.SendMessages
                )
            }

            game.playerDatas.forEach { player ->
                if (player.state.isDead) {
                    addMemberOverwrite(player.member.id) {
                        allowed = Permissions(Permission.ViewChannel, Permission.ReadMessageHistory)
                        denied = Permissions(Permission.SendMessages)
                    }
                    return@forEach
                }

                if (player.job is Couple) {
                    val canAccess = isNight && !shouldRestrictCommunication(player)
                    addMemberOverwrite(player.member.id) {
                        allowed = Permissions(Permission.ViewChannel, Permission.ReadMessageHistory)
                        denied = if (canAccess) Permissions() else Permissions(Permission.SendMessages)
                    }
                } else {
                    addMemberOverwrite(player.member.id) {
                        denied = Permissions(
                            Permission.ViewChannel,
                            Permission.ReadMessageHistory,
                            Permission.SendMessages
                        )
                    }
                }
            }
        }
    }

    private suspend fun updateDeadChannelPermissions(game: Game, deadChannel: TextChannel) {
        deadChannel.edit {
            addRoleOverwrite(game.guild.id) {
                denied = Permissions(
                    Permission.ViewChannel,
                    Permission.ReadMessageHistory,
                    Permission.SendMessages
                )
            }

            game.playerDatas.forEach { player ->
                if (!player.state.isDead && player.job !is Shaman) {
                    addMemberOverwrite(player.member.id) {
                        denied = Permissions(
                            Permission.ViewChannel,
                            Permission.ReadMessageHistory,
                            Permission.SendMessages
                        )
                    }
                    return@forEach
                }

                addMemberOverwrite(player.member.id) {
                    allowed = Permissions(Permission.ViewChannel, Permission.ReadMessageHistory)
                    denied = if (player.state.isDead) {
                        Permissions()
                    } else {
                        Permissions(Permission.SendMessages)
                    }
                }
            }
        }
    }

    suspend fun startVotePhase(game: Game) {
        val mainChannel = game.mainChannel ?: return
        processEscapedPlayerDeaths(game)
        game.currentPhase = GamePhase.VOTE
        game.currentMainVotes.clear()
        game.currentFakeVotes.clear()
        game.hostessFirstVoteTargetByDay.clear()
        game.defenseTargetId = null

        val alivePlayers = game.playerDatas.filter { !it.state.isDead }

        game.sendMainChannelMessageWithImage(
            imageLink = "https://cdn.discordapp.com/attachments/1483977619258212392/1483981201428709456/bd6d8d833d736bf2.png?ex=69bfdc71&is=69be8af1&hm=ca26cbd8933d3968240055b67202bfec8b35a278559172435a4515ecf3921ddb&",
            message = "투표 시간입니다. 의심되는 사람을 투표하세요."
        )
        mainChannel.createMessage {
            actionRow {
                stringSelect("main_vote_select") {
                    placeholder = "처형할 플레이어 선택"
                    alivePlayers.forEach { player ->
                        option(player.member.effectiveName, player.member.id.toString()) {
                            description = "이 플레이어에게 투표합니다."
                        }
                    }
                }
            }
        }

        val voteStatusMessage = mainChannel.createMessage {
            content = buildMainVoteStatusContent(game, alivePlayers, isHidden = false)
        }

        votePresentationScope.launch {
            val refreshInterval = 1_000L
            val refreshCount = (INITIAL_VOTE_REVEAL_DURATION_MS / refreshInterval).toInt()

            repeat(refreshCount) {
                delay(refreshInterval)
                runCatching {
                    voteStatusMessage.edit {
                        content = buildMainVoteStatusContent(game, alivePlayers, isHidden = false)
                    }
                }
            }

            runCatching {
                voteStatusMessage.edit {
                    content = buildMainVoteStatusContent(game, alivePlayers, isHidden = true)
                }
            }
        }
    }

    suspend fun resolveVotePhase(game: Game): PlayerData? {
        val mainChannel = game.mainChannel ?: return null
        val alivePlayers = game.playerDatas.filter { !it.state.isDead }
        applyHostessSeductionFromVote(game)
        val dictatorshipPolitician = findAliveDictatorshipPolitician(game)
        if (dictatorshipPolitician != null) {
            val politicianVoteTargetId = game.currentMainVotes[dictatorshipPolitician.member.id]
            val politicianTarget = politicianVoteTargetId
                ?.let { targetId -> game.getPlayer(Snowflake(targetId)) }
                ?.takeUnless { it.state.isDead }
            return if (politicianTarget != null) {
                mainChannel.createMessage(
                    "독재가 발동되어 ${dictatorshipPolitician.member.effectiveName}님의 선택으로 ${politicianTarget.member.effectiveName}님이 최후 변론 대상자로 지목되었습니다."
                )
                politicianTarget
            } else {
                game.sendMainChannelMessageWithImage(
                    imageLink = "https://cdn.discordapp.com/attachments/1483977619258212392/1484594233653465122/K5WjViOFIiajx3YUfctCF-wkTWwg-DnerBQ09EXEd5-Jxz6Yy0vAmAuM5XDOMIWqHpYOXk85dCobA6CkwzPxOILsPNTbKJgtpYa1DtnVqhceybFNoLK5kdEtPJr6x7rCpn5F3Au_wTeTK0zWtRNArQ.webp?ex=69becb9f&is=69bd7a1f&hm=95cc33354d29bf53d2a74db6ca5ac622b88ef11bfe5b9e419f6e7b38a6f2a8b4&",
                    message = "독재 상태에서 정치인의 투표가 없어 처형될 대상을 고르지 못했습니다."
                )
                null
            }
        }
        val authorityJudge = findRevealedAliveJudge(game)
        if (authorityJudge != null) {
            val judgeVoteTargetId = game.currentMainVotes[authorityJudge.member.id]
            val judgeTarget = judgeVoteTargetId
                ?.let { targetId -> game.getPlayer(Snowflake(targetId)) }
                ?.takeUnless { it.state.isDead }
            return if (judgeTarget != null) {
                mainChannel.createMessage(
                    "판사의 선고로 ${judgeTarget.member.effectiveName}님이 최후 변론 대상자로 지목되었습니다."
                )
                judgeTarget
            } else {
                game.sendMainChannelMessageWithImage(
                    imageLink = "https://cdn.discordapp.com/attachments/1483977619258212392/1484594233653465122/K5WjViOFIiajx3YUfctCF-wkTWwg-DnerBQ09EXEd5-Jxz6Yy0vAmAuM5XDOMIWqHpYOXk85dCobA6CkwzPxOILsPNTbKJgtpYa1DtnVqhceybFNoLK5kdEtPJr6x7rCpn5F3Au_wTeTK0zWtRNArQ.webp?ex=69becb9f&is=69bd7a1f&hm=95cc33354d29bf53d2a74db6ca5ac622b88ef11bfe5b9e419f6e7b38a6f2a8b4&",
                    message = "판사의 선고가 없어 처형될 대상을 고르지 못했습니다."
                )
                null
            }
        }
        val voteCounts = mutableMapOf<PlayerData, Int>()
        val fakeVoteCounts = mutableMapOf<PlayerData, Int>()
        var invalidVoteCount = 0
        val weightedVoteTargets = mutableListOf<PlayerData>()
        val gangsterTransferredVoteWeights = mutableMapOf<Snowflake, Int>()
        game.activeThreatenedVoters.forEach { (threatenedId, gangsterId) ->
            val threatened = game.getPlayer(threatenedId) ?: return@forEach
            val gangster = game.getPlayer(gangsterId) ?: return@forEach
            if (threatened.state.isDead || gangster.state.isDead) return@forEach
            gangsterTransferredVoteWeights[gangsterId] =
                (gangsterTransferredVoteWeights[gangsterId] ?: 0) + 1
        }

        alivePlayers.forEach { voter ->
            if (voter.member.id in game.permanentlyDisenfranchisedVoters) {
                return@forEach
            }
            if (game.activeThreatenedVoters.containsKey(voter.member.id)) {
                return@forEach
            }

            val baseWeight = if (voter.job is Politician) 2 else 1
            val weightEvent = GameEvent.CalculateVoteWeight(voter, weight = baseWeight)
            voter.allAbilities
                 .filterIsInstance<PassiveAbility>()
                .filterNot { FrogCurseManager.shouldSuppressPassive(voter) }
                .sortedByDescending(PassiveAbility::priority)
                .forEach { passive ->
                    passive.onEventObserved(game, voter, weightEvent)
                }
            weightEvent.weight += gangsterTransferredVoteWeights[voter.member.id] ?: 0

            if (
                voter.job is MadScientist &&
                voter.allAbilities.any { it is Analysis } &&
                voter.state.madScientistAnalysisEligibleDay == game.dayCount &&
                !voter.state.hasUsedMadScientistAnalysis
            ) {
                val targetId = game.currentMainVotes[voter.member.id]?.let(::Snowflake)
                if (targetId != null && targetId == voter.state.madScientistLynchedVoteTargetId) {
                    weightEvent.weight += 1
                    voter.state.hasUsedMadScientistAnalysis = true
                }
            }

            if (weightEvent.weight <= 0) {
                return@forEach
            }

            val targetIdString = game.currentMainVotes[voter.member.id]
            if (targetIdString == null) {
                invalidVoteCount += weightEvent.weight
                return@forEach
            }

            val target = game.getPlayer(Snowflake(targetIdString)) ?: return@forEach
            if (target.state.isDead) {
                invalidVoteCount += weightEvent.weight
                return@forEach
            }
            voteCounts[target] = (voteCounts[target] ?: 0) + weightEvent.weight
            repeat(weightEvent.weight.coerceAtLeast(0)) {
                weightedVoteTargets += target
            }
        }

        game.currentFakeVotes.forEach { (voterId, targetId) ->
            val voter = game.getPlayer(voterId) ?: return@forEach
            val target = game.getPlayer(targetId) ?: return@forEach
            if (voter.state.isDead || target.state.isDead) return@forEach
            fakeVoteCounts[target] = (fakeVoteCounts[target] ?: 0) + 1
            weightedVoteTargets += target
        }

        if (weightedVoteTargets.isNotEmpty()) {
            delay(1_000L)
            val progressiveVoteCounts = mutableMapOf<PlayerData, Int>()
            val tallyMessage = mainChannel.createMessage {
                content = buildFinalVoteTallyContent(alivePlayers, progressiveVoteCounts)
            }

            weightedVoteTargets.forEach { target ->
                progressiveVoteCounts[target] = (progressiveVoteCounts[target] ?: 0) + 1
                runCatching {
                    tallyMessage.edit {
                        content = buildFinalVoteTallyContent(
                            alivePlayers = alivePlayers,
                            voteCounts = progressiveVoteCounts,
                            fakeVoteCounts = fakeVoteCounts
                        )
                    }
                }
                delay(FINAL_VOTE_TALLY_STEP_MS)
            }
        }

        val maxVotes = voteCounts.values.maxOrNull() ?: 0
        if (invalidVoteCount >= maxVotes || maxVotes == 0) {
            game.sendMainChannelMessageWithImage(
                imageLink = "https://cdn.discordapp.com/attachments/1483977619258212392/1484594233653465122/K5WjViOFIiajx3YUfctCF-wkTWwg-DnerBQ09EXEd5-Jxz6Yy0vAmAuM5XDOMIWqHpYOXk85dCobA6CkwzPxOILsPNTbKJgtpYa1DtnVqhceybFNoLK5kdEtPJr6x7rCpn5F3Au_wTeTK0zWtRNArQ.webp?ex=69becb9f&is=69bd7a1f&hm=95cc33354d29bf53d2a74db6ca5ac622b88ef11bfe5b9e419f6e7b38a6f2a8b4&",
                message = "처형될 대상을 고르지 못했습니다."
            )
            return null
        }

        val maxVotedPlayers = voteCounts.filter { it.value == maxVotes }.keys.toList()

        if (maxVotedPlayers.size > 1) {
            val halfThreshold = (alivePlayers.size + 1) / 2
            if (maxVotes < halfThreshold) {
                val juryResolved = resolveJuryTarget(game, maxVotedPlayers)
                if (juryResolved != null) {
                    game.sendMainChannerMessage("배심원의 결정으로 인해 투표 대상이 정해졌습니다!")
                    if (isInnocentTarget(game, juryResolved)) {
                        game.sendMainChannelMessageWithImage(
                            imageLink = INNOCENCE_IMAGE_URL,
                            message = "${juryResolved.member.effectiveName}님은 결백합니다!"
                        )
                        return null
                    }
                    return juryResolved
                }
            }
            game.sendMainChannelMessageWithImage(
                imageLink = "https://cdn.discordapp.com/attachments/1483977619258212392/1484594233653465122/K5WjViOFIiajx3YUfctCF-wkTWwg-DnerBQ09EXEd5-Jxz6Yy0vAmAuM5XDOMIWqHpYOXk85dCobA6CkwzPxOILsPNTbKJgtpYa1DtnVqhceybFNoLK5kdEtPJr6x7rCpn5F3Au_wTeTK0zWtRNArQ.webp?ex=69becb9f&is=69bd7a1f&hm=95cc33354d29bf53d2a74db6ca5ac622b88ef11bfe5b9e419f6e7b38a6f2a8b4&",
                message = "처형될 대상을 고르지 못했습니다."
            )
            return null
        }

        val finalTarget = maxVotedPlayers.first()
        if (isInnocentTarget(game, finalTarget)) {
            game.sendMainChannelMessageWithImage(
                imageLink = INNOCENCE_IMAGE_URL,
                message = "${finalTarget.member.effectiveName}님은 결백합니다!"
            )
            return null
        }
        return finalTarget
    }

    private fun buildMainVoteStatusContent(
        game: Game,
        alivePlayers: List<PlayerData>,
        isHidden: Boolean
    ): String {
        val currentVoteCounts = mutableMapOf<PlayerData, Int>()
        game.currentMainVotes.values.forEach { targetId ->
            val target = game.getPlayer(Snowflake(targetId)) ?: return@forEach
            if (target.state.isDead) return@forEach
            currentVoteCounts[target] = (currentVoteCounts[target] ?: 0) + 1
        }
        game.currentFakeVotes.values.forEach { targetId ->
            val target = game.getPlayer(targetId) ?: return@forEach
            if (target.state.isDead) return@forEach
            currentVoteCounts[target] = (currentVoteCounts[target] ?: 0) + 1
        }

        return buildString {
            alivePlayers.forEach { player ->
                val voteDisplay = if (isHidden) "?" else (currentVoteCounts[player] ?: 0).toString()
                appendLine("- ${player.member.effectiveName}: ${voteDisplay}표")
            }
        }
    }

    private fun buildFinalVoteTallyContent(
        alivePlayers: List<PlayerData>,
        voteCounts: Map<PlayerData, Int>,
        fakeVoteCounts: Map<PlayerData, Int> = emptyMap()
    ): String {
        return buildString {
            alivePlayers.forEach { player ->
                val total = (voteCounts[player] ?: 0)
                val fakeCount = fakeVoteCounts[player] ?: 0
                if (fakeCount > 0) {
                    appendLine("- ${player.member.effectiveName}: ${total}표 (위증 ${fakeCount}표 포함)")
                } else {
                    appendLine("- ${player.member.effectiveName}: ${total}표")
                }
            }
        }
    }

    private fun resolveJuryTarget(game: Game, tiedTargets: List<PlayerData>): PlayerData? {
        val juryVoteCounts = mutableMapOf<PlayerData, Int>()
        game.currentMainVotes.forEach { (voterId, targetIdString) ->
            val voter = game.getPlayer(voterId) ?: return@forEach
            if (voter.state.isDead || voter.allAbilities.none { it is Jury }) return@forEach
            val target = game.getPlayer(Snowflake(targetIdString)) ?: return@forEach
            if (target !in tiedTargets) return@forEach
            juryVoteCounts[target] = (juryVoteCounts[target] ?: 0) + 1
        }

        if (juryVoteCounts.isEmpty()) return null
        val maxJuryVotes = juryVoteCounts.values.maxOrNull() ?: return null
        val topTargets = juryVoteCounts.filterValues { it == maxJuryVotes }.keys
        return topTargets.singleOrNull()
    }

    private fun isInnocentTarget(game: Game, candidate: PlayerData): Boolean {
        if (candidate.allAbilities.none { it is Innocence }) return false
        val candidateVoteTargetId = game.currentMainVotes[candidate.member.id] ?: return false
        val candidateVoteTarget = game.getPlayer(Snowflake(candidateVoteTargetId)) ?: return false
        if (candidateVoteTarget.state.isDead) return false

        val sameTeam = (candidate.job is Evil) == (candidateVoteTarget.job is Evil)
        if (sameTeam) return false

        val reverseVoteTargetId = game.currentMainVotes[candidateVoteTarget.member.id]
        return reverseVoteTargetId != candidate.member.id.toString()
    }

    private fun notifyMindReadingResults(game: Game) {
        if (game.currentMainVotes.isEmpty()) return

        game.playerDatas
            .filter { !it.state.isDead && it.allAbilities.any { ability -> ability is MindReading } }
            .forEach { mindReader ->
                val voters = game.currentMainVotes
                    .filterValues { it == mindReader.member.id.toString() }
                    .keys
                    .mapNotNull { voterId -> game.getPlayer(voterId)?.member?.effectiveName }

                cabalNotificationScope.launch {
                    runCatching {
                        val message = if (voters.isEmpty()) {
                            "독심술 결과: 당신에게 투표한 사람이 없습니다."
                        } else {
                            "독심술 결과: 당신에게 투표한 사람 - ${voters.joinToString(", ")}"
                        }
                        mindReader.member.getDmChannel().createMessage(message)
                    }
                }
            }
    }

    private suspend fun processEscapedPlayerDeaths(game: Game) {
        val pendingTargets = game.pendingEscapedPlayerIds.toList()
        if (pendingTargets.isEmpty()) return

        pendingTargets.forEach { escapedPlayerId ->
            val escapedPlayer = game.getPlayer(escapedPlayerId) ?: run {
                game.pendingEscapedPlayerIds.remove(escapedPlayerId)
                return@forEach
            }
            if (escapedPlayer.state.isDead) {
                game.pendingEscapedPlayerIds.remove(escapedPlayerId)
                return@forEach
            }

            escapedPlayer.state.isDead = true
            handleMadScientistDeath(game, escapedPlayer, isLynch = true)
            game.pendingEscapedPlayerIds.remove(escapedPlayerId)
            game.nightEvents += GameEvent.PlayerDied(escapedPlayer, isLynch = true)
            applyPoliceAutopsy(game, escapedPlayer)
            SpyAbility.applyAutopsyOnDeath(game, escapedPlayer)
            game.sendMainChannelMessageWithImage(
                imageLink = ESCAPE_DEATH_IMAGE_URL,
                message = "투표에서 도주한 ${escapedPlayer.member.effectiveName}님이 사망하였습니다."
            )
            revealBelongingsIfNeeded(game, escapedPlayer)
        }

        dispatchEvents(game)
        game.nightEvents.clear()
        game.deadChannel?.let { updateDeadChannelPermissions(game, it) }
        refreshMafiaChannelContactState(game)
    }

    private fun deliverSecretLetters(game: Game) {
        if (game.pendingLettersByRecipient.isEmpty()) return
        val deliveries = game.pendingLettersByRecipient.toMap()
        game.pendingLettersByRecipient.clear()

        deliveries.forEach { (recipientId, letters) ->
            val recipient = game.getPlayer(recipientId) ?: return@forEach
            cabalNotificationScope.launch {
                runCatching {
                    recipient.member.getDmChannel().createMessage("[밀서 도착]\n${letters.joinToString("\n\n")}")
                }
            }
        }
    }

    private suspend fun revealNightWillIfNeeded(game: Game, deadPlayers: List<PlayerData>) {
        val willOwners = deadPlayers.filter { player ->
            player.allAbilities.any { it is Will } && game.willByPlayerId[player.member.id]?.isNotBlank() == true
        }

        willOwners.forEach { player ->
            val willMessage = game.willByPlayerId.remove(player.member.id) ?: return@forEach
            game.sendMainChannerMessage("[유언] ${player.member.effectiveName}: $willMessage")
        }
    }

    suspend fun startDefensePhase(game: Game, target: PlayerData) {
        val mainChannel = game.mainChannel ?: return
        game.defenseTargetId = target.member.id
        (target.job as? Martyr)?.defenseBombTargetId = null
        game.sendMainChannelMessageWithImage(
            imageLink = "https://cdn.discordapp.com/attachments/1483977619258212392/1484595217796567092/b1bb8f82a19e45e3.png?ex=69becc8a&is=69bd7b0a&hm=0facb3df92275cbd87534a5c337cb4c774643de1c0ec93529a105c1573f30f35&",
            message = "${target.member.effectiveName}의 최후의 변론"
        )

        mainChannel.edit {
            addRoleOverwrite(game.guild.id) {
                denied = Permissions(Permission.SendMessages)
            }

            if (!target.state.isSilenced) {
                addMemberOverwrite(target.member.id) {
                    allowed = Permissions(Permission.SendMessages)
                }
            }
        }
    }

    suspend fun startProsConsVotePhase(game: Game, target: PlayerData) {
        val mainChannel = game.mainChannel ?: return
        game.currentPhase = GamePhase.VOTE
        game.currentProsConsVotes.clear()

        mainChannel.createMessage {
            actionRow {
                interactionButton(ButtonStyle.Success, "vote_pros") {
                    label = "찬성"
                }
                interactionButton(ButtonStyle.Danger, "vote_cons") {
                    label = "반대"
                }
            }
        }
    }

    suspend fun resolveExecutionPhase(game: Game, target: PlayerData) {
        val mainChannel = game.mainChannel ?: return
        val deadChannel = game.deadChannel
        val dictatorshipPolitician = findAliveDictatorshipPolitician(game)
        val prosCount = game.currentProsConsVotes
            .filterValues { it }
            .keys
            .sumOf { voterId ->
                val voter = game.getPlayer(voterId)
                when {
                    voter == null || voter.state.isDead -> 0
                    voter.job is Politician -> 2
                    else -> 1
                }
            }
        val consCount = game.currentProsConsVotes
            .filterValues { !it }
            .keys
            .sumOf { voterId ->
                val voter = game.getPlayer(voterId)
                when {
                    voter == null || voter.state.isDead -> 0
                    voter.job is Politician -> 2
                    else -> 1
                }
            } +
            game.playerDatas.count { player ->
                !player.state.isDead &&
                    (player.member.id in game.permanentlyDisenfranchisedVoters ||
                        game.activeThreatenedVoters.containsKey(player.member.id))
            }
        val judgePlayer = findAliveJudge(game)
        val judgeVote = judgePlayer?.let { game.currentProsConsVotes[it.member.id] }
        val aggregateDecision = prosCount > consCount
        val judgeJob = judgePlayer?.job as? Judge
        val shouldRevealJudge = judgePlayer != null &&
            judgeJob != null &&
            !judgeJob.hasRevealedAuthority &&
            judgeVote != null &&
            judgeVote != aggregateDecision

        if (shouldRevealJudge) {
            judgeJob.hasRevealedAuthority = true
            judgePlayer.state.isJobPubliclyRevealed = true
            game.unwrittenRuleBlockedTargetIdTonight = judgePlayer.member.id

            mainChannel.createMessage(
                "판사 ${judgePlayer.member.effectiveName}님이 모습을 드러냈습니다. 선고에 따라 이번 투표는 ${if (judgeVote == true) "찬성" else "반대"}로 결정됩니다."
            )
        }

        notifyJudgeProsVoters(game, target)

        if (findRevealedAliveJudge(game) != null && judgeVote == null) {
            mainChannel.createMessage("판사가 찬반 선고를 하지 않아 이번 처형은 자동으로 반대로 처리됩니다.")
        }

        val finalDecision = when {
            dictatorshipPolitician != null -> game.currentProsConsVotes[dictatorshipPolitician.member.id] ?: false
            findRevealedAliveJudge(game) != null -> judgeVote ?: false
            else -> aggregateDecision
        }

        val executionEvent = GameEvent.DecideExecution(target, finalDecision)
        val alivePlayers = game.playerDatas.filter { !it.state.isDead }

        alivePlayers.forEach { player ->
            player.allAbilities
                 .filterIsInstance<PassiveAbility>()
                .filterNot { FrogCurseManager.shouldSuppressPassive(player) }
                .sortedByDescending(PassiveAbility::priority)
                .forEach { passive ->
                    passive.onEventObserved(game, player, executionEvent)
                }
        }

        if (!executionEvent.isApproved) {
            game.sendMainChannelMessageWithImage(
                imageLink = SystemImage.VOTING_FAILURE.imageUrl,
                message = buildString {
                    executionEvent.overrideReason?.let { reason ->
                        appendLine(reason)
                    }
                    append("${target.member.effectiveName}님의 처형이 부결되었습니다.")
                }
            )
            game.defenseTargetId = null
            return
        }

        val voteExecutionEvent = GameEvent.VoteExecution(target)
        alivePlayers.forEach { player ->
            player.allAbilities
                 .filterIsInstance<PassiveAbility>()
                .filterNot { FrogCurseManager.shouldSuppressPassive(player) }
                .sortedByDescending(PassiveAbility::priority)
                .forEach { passive ->
                    passive.onEventObserved(game, player, voteExecutionEvent)
                }
        }

        if (voteExecutionEvent.isCancelled) {
            mainChannel.createMessage(voteExecutionEvent.cancelReason ?: "처형 무효")
            game.defenseTargetId = null
            return
        }

        if (target.job is Politician) {
            if (!target.state.isJobPubliclyRevealed) {
                target.state.isJobPubliclyRevealed = true
                game.unwrittenRuleBlockedTargetIdTonight = target.member.id
            }
            mainChannel.createMessage("정치인은 투표로 죽지 않습니다")
            game.defenseTargetId = null
            return
        }

        val hasMartyrExplosionTarget = ((target.job as? Martyr)?.defenseBombTargetId != null)
        if (
            !hasMartyrExplosionTarget &&
            target.allAbilities.any { it is Escape } &&
            target.member.id !in game.pendingEscapedPlayerIds
        ) {
            game.pendingEscapedPlayerIds += target.member.id
            game.publiclyRevealedAbilityTargetIds += target.member.id
            game.sendMainChannelMessageWithImage(
                imageLink = ESCAPE_IMAGE_URL,
                message = "${target.member.effectiveName}님이 투표에서 도주하였습니다!"
            )
            game.defenseTargetId = null
            return
        }

        target.state.isDead = true
        handleMadScientistDeath(game, target, isLynch = true)
        game.nightEvents += GameEvent.PlayerDied(target, isLynch = true)
        applyPoliceAutopsy(game, target)
        SpyAbility.applyAutopsyOnDeath(game, target)
        resolveMartyrDefenseExplosion(game, target)
        dispatchEvents(game)
        game.nightEvents.clear()
        game.sendMainChannelMessageWithImage(
            imageLink = SystemImage.VOTE_EXECUTION.imageUrl,
            message = "${target.member.effectiveName}님이 투표로 처형당하였습니다."
        )
        revealBelongingsIfNeeded(game, target)

        if (deadChannel != null) {
            updateDeadChannelPermissions(game, deadChannel)
        }
        refreshMafiaChannelContactState(game)
        game.defenseTargetId = null
    }

    fun checkWinCondition(game: Game): Team? {
        if (isCabalSpecialWinReady(game)) {
            return Team.CABAL_SPECIAL
        }
        resolveProphetSpecialWin(game)?.let { return it }
        resolveTerminalSpecialWin(game)?.let { return it }

        val alivePlayers = game.playerDatas.filter { !it.state.isDead }
        val mafiaCount = alivePlayers.count { it.job is Evil }
        val citizenCount = alivePlayers.sumOf { player ->
            if (player.job is Evil) {
                0
            } else {
                when (player.job) {
                    is Gangster -> 3
                    is Politician -> 2
                    else -> 1
                }
            }
        }
        val aliveCabals = alivePlayers.count { it.job is Cabal }

        val activeMercenaryExecution = game.playerDatas.any { player ->
            val mercenary = player.job as? Mercenary ?: return@any false
            mercenary.hasExecutionAuthority
        }

        return when {
            mafiaCount == 0 -> Team.CITIZEN
            mafiaCount >= citizenCount &&
                aliveCabals < 2 &&
                alivePlayers.none { it.job is Prophet } &&
                !isRevealedJudgeAlive(game) &&
                !activeMercenaryExecution &&
                findAliveDictatorshipPolitician(game) == null -> Team.MAFIA
            else -> null
        }
    }

    private fun findAliveDictatorshipPolitician(game: Game): PlayerData? {
        val aliveCitizens = game.playerDatas.filter { !it.state.isDead && it.job !is Evil }
        if (aliveCitizens.size != 1) return null
        return aliveCitizens.firstOrNull { it.job is Politician }
    }

    private fun findAliveJudge(game: Game): PlayerData? {
        return game.playerDatas.firstOrNull { !it.state.isDead && it.job is Judge }
    }

    private fun findRevealedAliveJudge(game: Game): PlayerData? {
        return findAliveJudge(game)?.takeIf { player ->
            val judgeJob = player.job as? Judge ?: return@takeIf false
            judgeJob.hasRevealedAuthority
        }
    }

    private fun isRevealedJudgeAlive(game: Game): Boolean {
        return findRevealedAliveJudge(game) != null
    }

    private fun resolveTerminalSpecialWin(game: Game): Team? {
        if (game.dayCount < 2) return null
        if (game.initialPlayerCount <= 0) return null

        val requiredDayCount = (game.initialPlayerCount / 2) + 2
        if (game.dayCount < requiredDayCount) return null

        val hasAliveTerminalOwner = game.playerDatas.any { player ->
            !player.state.isDead &&
                player.job is Evil &&
                player.job !is Villain &&
                player.allAbilities.any { it is Terminal }
        }

        return if (hasAliveTerminalOwner) Team.MAFIA else null
    }

    private fun notifyInstructionsAtFirstDay(game: Game) {
        if (game.dayCount != 1) return

        val policePlayers = game.playerDatas.filter { target ->
            !target.state.isDead && (target.job is Police || target.job is org.beobma.mafia42discordproject.job.definition.list.Agent || target.job is Vigilante)
        }

        val instructionOwners = game.playerDatas.filter { player ->
            !player.state.isDead &&
                player.job !is Villain &&
                player.allAbilities.any { it is Instructions }
        }

        instructionOwners.forEach { owner ->
            votePresentationScope.launch {
                runCatching {
                    val dm = owner.member.getDmChannel()
                    if (policePlayers.isEmpty()) {
                        dm.createMessage("경찰 계열 직업이 없습니다.")
                        return@runCatching
                    }

                    val lines = policePlayers.joinToString("\n") { target ->
                        "${target.member.effectiveName}은 경찰 계열 직업."
                    }
                    dm.createMessage(lines)
                }
            }
        }
    }

    private suspend fun notifyTheInformantAutoContactAtSecondDay(game: Game) {
        if (game.dayCount != 2) return

        game.playerDatas.forEach { player ->
            if (player.state.isDead) return@forEach
            if (player.state.hasContactedMafiaByInformant) return@forEach
            if (player.job !is Evil || player.job is Mafia) return@forEach
            if (player.allAbilities.none { it is TheInformant }) return@forEach

            player.state.hasContactedMafiaByInformant = true
            notifyInformantContactByJob(game, player)
        }
    }

    private suspend fun notifyInformantContactByJob(game: Game, player: PlayerData) {
        val mafiaChannel = game.mafiaChannel ?: return

        when (val job = player.job) {
            is HitMan -> {
                if (!job.hasContactedMafia) {
                    job.hasContactedMafia = true
                    notifyHitmanContact(game, player)
                }
            }
            is Spy -> {
                if (!job.hasContactedMafia) {
                    job.hasContactedMafia = true
                    mafiaChannel.createMessage("$SPY_CONTACT_IMAGE_URL\n**접선했습니다.**")
                }
            }
            is Thief -> {
                if (!job.hasContactedMafia) {
                    job.hasContactedMafia = true
                    if (!player.state.hasAnnouncedThiefContact) {
                        player.state.hasAnnouncedThiefContact = true
                        mafiaChannel.createMessage("$THIEF_CONTACT_IMAGE_URL\n**접선했습니다.**")
                    }
                }
            }
            is Witch -> {
                if (!job.hasContactedMafia) {
                    job.hasContactedMafia = true
                    mafiaChannel.createMessage("$WITCH_CONTACT_IMAGE_URL\n**접선했습니다.**")
                }
            }
            is Hostess -> {
                if (!job.hasContactedMafia) {
                    job.hasContactedMafia = true
                    mafiaChannel.createMessage("$HOSTESS_CONTACT_IMAGE_URL\n접선했습니다.")
                }
            }
            is Swindler -> {
                if (!job.hasContactedMafia) {
                    job.hasContactedMafia = true
                    mafiaChannel.createMessage("$SWINDLER_CONTACT_IMAGE_URL\n**접선했습니다.**")
                }
            }
            is Godfather -> {
                if (!player.state.hasAnnouncedGodfatherContact) {
                    player.state.hasAnnouncedGodfatherContact = true
                    mafiaChannel.createMessage("$GODFATHER_CONTACT_IMAGE_URL\n접선했습니다.")
                }
            }
            is MadScientist -> {
                if (!player.state.hasContactedMafiaOnDeath) {
                    player.state.hasContactedMafiaOnDeath = true
                }
                if (!player.state.hasAnnouncedMadScientistContact) {
                    player.state.hasAnnouncedMadScientistContact = true
                    mafiaChannel.createMessage("$MAD_SCIENTIST_CONTACT_IMAGE_URL\n접선했습니다.")
                }
            }
            is Beastman -> {
                mafiaChannel.createMessage("$BEASTMAN_TAMED_IMAGE_URL\n접선했습니다.")
            }
            else -> {
                mafiaChannel.createMessage("**${player.member.effectiveName}님이 밀정 능력으로 접선했습니다.**")
            }
        }

        refreshMafiaChannelContactState(game)
    }

    private fun notifyJudgeProsVoters(game: Game, target: PlayerData) {
        val judgePlayer = findAliveJudge(game) ?: return
        val prosVoters = game.currentProsConsVotes
            .filterValues { it }
            .keys
            .mapNotNull { voterId -> game.getPlayer(voterId) }
            .map { voter -> voter.member.effectiveName }

        val prosMessage = if (prosVoters.isEmpty()) {
            "없음"
        } else {
            prosVoters.joinToString(", ")
        }

        cabalNotificationScope.launch {
            runCatching {
                judgePlayer.member.getDmChannel().createMessage(
                    "관권 발동 정보: ${target.member.effectiveName} 처형 찬성 투표자 - $prosMessage"
                )
            }
        }
    }

    private suspend fun resolveMartyrNightExplosions(game: Game, playersToDie: MutableSet<PlayerData>) {
        val mainChannel = game.mainChannel

        game.playerDatas.forEach { player ->
            val martyr = player.job as? Martyr ?: return@forEach
            if (player !in playersToDie) return@forEach

            val selectedTargetId = martyr.nightBombTargetId ?: return@forEach
            val selectedTarget = game.getPlayer(selectedTargetId) ?: return@forEach
            if (selectedTarget.state.isDead) return@forEach

            val mafiaExecutionTarget = game.nightAttacks["MAFIA_TEAM"]?.target
            val isNightBombTriggered = mafiaExecutionTarget == player && selectedTarget.job is Mafia

            val hasExplosion = player.allAbilities.any { it is Explosion }
            val attackBySelectedNonMafiaEvil = game.nightAttacks.values.any { attack ->
                attack.target == player &&
                    attack.attacker.member.id == selectedTarget.member.id &&
                    attack.attacker.job is Evil &&
                    attack.attacker.job !is Mafia
            }
            val isExplosionTriggered = hasExplosion && attackBySelectedNonMafiaEvil

            if (!isNightBombTriggered && !isExplosionTriggered) return@forEach

            playersToDie += selectedTarget

            val hasFlash = player.allAbilities.any { it is Flash }
            if (hasFlash) {
                playersToDie -= player
            }

            player.state.isJobPubliclyRevealed = true
            selectedTarget.state.isJobPubliclyRevealed = true

            mainChannel?.createMessage(
                "테러리스트의 자폭이 발동했습니다. ${player.member.effectiveName}님과 ${selectedTarget.member.effectiveName}님의 정체가 공개됩니다.\n" +
                    "직업 공개: ${player.member.effectiveName} - ${player.job?.name ?: "알 수 없음"}, " +
                    "${selectedTarget.member.effectiveName} - ${selectedTarget.job?.name ?: "알 수 없음"}"
            )
        }
    }

    private suspend fun resolveMartyrDefenseExplosion(game: Game, executedTarget: PlayerData) {
        val martyr = executedTarget.job as? Martyr ?: return
        val selectedTargetId = martyr.defenseBombTargetId ?: return
        val selectedTarget = game.getPlayer(selectedTargetId) ?: return
        if (selectedTarget.state.isDead) return
        if (selectedTarget.member.id == executedTarget.member.id) return

        selectedTarget.state.isDead = true
        handleMadScientistDeath(game, selectedTarget, isLynch = true)
        game.nightEvents += GameEvent.PlayerDied(selectedTarget, isLynch = true)
        applyPoliceAutopsy(game, selectedTarget)
        SpyAbility.applyAutopsyOnDeath(game, selectedTarget)

        executedTarget.state.isJobPubliclyRevealed = true
        selectedTarget.state.isJobPubliclyRevealed = true

        game.mainChannel?.createMessage(
            "테러리스트의 산화가 발동했습니다. ${executedTarget.member.effectiveName}님과 ${selectedTarget.member.effectiveName}님이 함께 사망합니다.\n" +
                "직업 공개: ${executedTarget.member.effectiveName} - ${executedTarget.job?.name ?: "알 수 없음"}, " +
                "${selectedTarget.member.effectiveName} - ${selectedTarget.job?.name ?: "알 수 없음"}"
        )
        refreshMafiaChannelContactState(game)
    }

    suspend fun endGame(game: Game, winningTeam: Team) {
        game.isRunning = false
        game.currentPhase = GamePhase.END
        val resultMessage = "${winningTeam.displayName} 승리: ${winningTeam.winMessage}"

        if (winningTeam.winImageUrl != null) {
            game.sendMainChannelMessageWithImage(
                imageLink = winningTeam.winImageUrl,
                message = resultMessage
            )
        } else {
            // 이미지가 없다면 기존처럼 텍스트만 전송
            game.sendMainChannerMessage(resultMessage)
        }
    }

    suspend fun runGameLoop(game: Game) {
        while (game.isRunning) {
            startNightPhase(game)
            runPhaseCountdown(game, "밤", NIGHT_DURATION_MS)

            val nightSummary = resolveNightPhase(game)

            resolveDawnPhase(game, nightSummary)
            runPhaseCountdown(game, "새벽", DAWN_DURATION_MS)
//            checkWinCondition(game)?.let { winner ->
//                endGame(game, winner)
//                break
//            }

            startDayPhase(game, nightSummary)
            val discussionMillis = game.playerDatas.count { !it.state.isDead } * 15_000L
            runPhaseCountdown(game, "낮", discussionMillis)

            startVotePhase(game)
            runPhaseCountdown(game, "투표", VOTE_DURATION_MS)

            val target = resolveVotePhase(game)
            if (target != null) {
                startDefensePhase(game, target)
                runPhaseCountdown(game, "변론", DEFENSE_DURATION_MS)

                startProsConsVotePhase(game, target)
                runPhaseCountdown(game, "찬반 투표", PROS_CONS_VOTE_DURATION_MS)

                resolveExecutionPhase(game, target)
            }

//            checkWinCondition(game)?.let { winner ->
//                endGame(game, winner)
//                break
//            }
        }
    }

    private fun resolveCabalSunInvestigation(game: Game) {
        val cabalPlayers = game.playerDatas.filter { it.job is Cabal }
        cabalPlayers.forEach { sunPlayer ->
            val sunCabal = sunPlayer.job as? Cabal ?: return@forEach
            if (sunCabal.role != CabalRole.SUN || sunPlayer.state.isDead) return@forEach

            val selectedTargetId = sunCabal.selectedTargetId ?: return@forEach
            val selectedTarget = game.getPlayer(selectedTargetId)

            val isMoon = selectedTarget?.job is Cabal &&
                (selectedTarget.job as? Cabal)?.role == CabalRole.MOON &&
                selectedTarget.member.id == sunCabal.pairedPlayerId

            if (isMoon) {
                val newlyFoundMoon = !sunCabal.hasFoundMoon
                sunCabal.hasFoundMoon = true
                val moonCabal = selectedTarget.job as? Cabal
                moonCabal?.wasFoundBySun = true
                sendCabalDm(
                    sunPlayer,
                    "비밀결사 ${selectedTarget.member.effectiveName}님을 찾았습니다."
                )
                if (newlyFoundMoon) {
                    sendCabalDm(selectedTarget, "비밀결사의 표식이 발견되었습니다.")
                }
            } else {
                sendCabalDm(sunPlayer, "밀사 결과: 아니다.")
            }
        }
    }

    private fun resolveCabalSpecialWinReadiness(game: Game) {
        val aliveOrDeadCabals = game.playerDatas
            .mapNotNull { player ->
                val cabal = player.job as? Cabal ?: return@mapNotNull null
                player to cabal
            }
        val sun = aliveOrDeadCabals.firstOrNull { (_, cabal) -> cabal.role == CabalRole.SUN } ?: return
        val moon = aliveOrDeadCabals.firstOrNull { (_, cabal) -> cabal.role == CabalRole.MOON } ?: return

        val sunPlayer = sun.first
        val sunCabal = sun.second
        val moonPlayer = moon.first
        val moonCabal = moon.second

        val rolesStillCabal = sunPlayer.job is Cabal && moonPlayer.job is Cabal
        val moonMarkedSun = moonCabal.moonMarkedSunTonight && moonCabal.selectedTargetId == sunPlayer.member.id
        val canTrigger = rolesStillCabal && sunCabal.hasFoundMoon && moonCabal.wasFoundBySun && moonMarkedSun

        sunCabal.cabalSpecialWinReady = canTrigger
        moonCabal.cabalSpecialWinReady = canTrigger
    }

    private fun isCabalSpecialWinReady(game: Game): Boolean {
        return game.playerDatas.any { player ->
            val cabal = player.job as? Cabal ?: return@any false
            cabal.cabalSpecialWinReady
        }
    }

    private fun resolveProphetPioneerSpecialWinReadiness(game: Game, summary: NightResolutionSummary) {
        val shouldTrigger = summary.deaths.any { player ->
            if (player.state.isDead.not()) return@any false
            if (player.job !is Prophet) return@any false
            if (player.allAbilities.none { it is Pioneer }) return@any false
            if (player.member.id in game.probationOriginalJobsByPlayer) return@any false

            val day4RevelationReady = game.dayCount >= 4
            val apostleRevelationReady = player.allAbilities.any { it is Apostle } &&
                game.playerDatas.none { candidate ->
                    !candidate.state.isDead &&
                        candidate.job !is Evil
                }

            day4RevelationReady || apostleRevelationReady
        }

        if (!shouldTrigger) return
        game.prophetSpecialWinScheduledTeam = Team.CITIZEN
    }

    private fun resolveProphetSpecialWin(game: Game): Team? {
        game.prophetSpecialWinScheduledTeam?.let { return it }

        val aliveProphets = game.playerDatas.filter { !it.state.isDead && it.job is Prophet }
        if (aliveProphets.isEmpty()) return null

        if (game.dayCount >= 4) {
            return Team.CITIZEN
        }

        val aliveCitizens = game.playerDatas.filter { !it.state.isDead && it.job !is Evil }
        val isApostleTriggered = aliveProphets.any { prophet ->
            prophet.allAbilities.any { it is Apostle } &&
                aliveCitizens.size == 1 &&
                aliveCitizens.first().member.id == prophet.member.id
        }

        return if (isApostleTriggered) Team.CITIZEN else null
    }

    private fun sendCabalDm(target: PlayerData, message: String) {
        cabalNotificationScope.launch {
            runCatching {
                target.member.getDmChannel().createMessage(message)
            }
        }
    }

    private fun buildDawnPresentation(
        game: Game,
        deaths: List<PlayerData>,
        poisonedDeaths: List<PlayerData> = emptyList()
    ): DawnPresentation {
        if (game.concealmentForcedQuietNight) {
            return DawnPresentation(
                imageUrl = SystemImage.QUIET_NIGHT.imageUrl,
                message = "조용하게 밤이 넘어갔습니다."
            )
        }

        val attacks = game.nightAttacks.values.toList()
        val presentationEvent = GameEvent.ResolveDawnPresentation(
            dayCount = game.dayCount,
            attacks = attacks,
            deaths = deaths,
            presentation = buildDefaultDawnPresentation(
                attacks = attacks,
                deaths = deaths,
                poisonedDeaths = poisonedDeaths,
                game = game
            )
        )

        game.playerDatas
            .filter { !it.state.isDead }
            .forEach { player ->
                player.allAbilities
                     .filterIsInstance<PassiveAbility>()
                    .filterNot { FrogCurseManager.shouldSuppressPassive(player) }
                    .sortedByDescending(PassiveAbility::priority)
                    .forEach { passive ->
                        passive.onEventObserved(game, player, presentationEvent)
                    }
            }

        return presentationEvent.presentation
    }

    private fun buildDefaultDawnPresentation(
        attacks: List<AttackEvent>,
        deaths: List<PlayerData>,
        poisonedDeaths: List<PlayerData>,
        game: Game // Game 파라미터 추가 필요 (호출부에도 game을 넘겨주어야 함)
    ): DawnPresentation {

        // 연인이 희생해서 죽은 사람이 있는지 확인
        val hasCoupleSacrifice = deaths.any { it.member.id in game.coupleSacrificeMap }

        if (hasCoupleSacrifice) {
            // 이미 announceCoupleSacrificeReveal에서 화려하게 이미지를 띄웠으므로
            // 여기서는 조용히 넘어가거나, 아주 간략한 요약만 반환하게 합니다.
            return DawnPresentation(imageUrl = "", message = "")
        }

        // 기존 마피아 킬 로직
        val mafiaKillVictim = attacks
            .firstOrNull { it.attacker.job?.name == "마피아" }
            ?.target
            ?.takeIf { it in deaths }
        val beastKillVictim = attacks
            .firstOrNull { it.attacker.job is Beastman }
            ?.target
            ?.takeIf { it in deaths }
        val poisonedDeathVictim = poisonedDeaths.firstOrNull()
        val vigilanteKillVictim = attacks
            .firstOrNull { it.attacker.job is Vigilante }
            ?.target
            ?.takeIf { it in deaths && it.job is Evil }
        val godfatherKillVictim = attacks
            .firstOrNull { it.attacker.job is Godfather }
            ?.target
            ?.takeIf { it in deaths }

        val doctorSavedTarget = game.doctorSavedTargetTonight

        return if (vigilanteKillVictim != null) {
            vigilanteKillVictim.state.isJobPubliclyRevealed = true
            val revealedJob = vigilanteKillVictim.job
            DawnPresentation(
                imageUrl = VIGILANTE_EXECUTION_IMAGE_URL,
                message = "${vigilanteKillVictim.member.effectiveName}가 살해당하였습니다." +
                    if (revealedJob != null) "\n${vigilanteKillVictim.member.effectiveName}님의 직업은 ${revealedJob.name}입니다." else ""
            )
        } else if (godfatherKillVictim != null) {
            DawnPresentation(
                imageUrl = GODFATHER_EXECUTION_IMAGE_URL,
                message = "${godfatherKillVictim.member.effectiveName}가 살해당하였습니다."
            )
        } else if (beastKillVictim != null) {
            DawnPresentation(
                imageUrl = BEASTMAN_ATTACK_IMAGE_URL,
                message = "${beastKillVictim.member.effectiveName}님이 짐승에게 습격당하였습니다."
            )
        } else if (mafiaKillVictim == null) {
            if (poisonedDeathVictim != null) {
                DawnPresentation(
                    imageUrl = SystemImage.DEATH_BY_POISON.imageUrl,
                    message = "${poisonedDeathVictim.member.effectiveName}님이 중독으로 사망했습니다."
                )
            } else if (doctorSavedTarget != null) {
                game.publiclyRevealedAbilityTargetIds += doctorSavedTarget.member.id
                DawnPresentation(
                    imageUrl = SystemImage.DOCTOR_HEAL.imageUrl,
                    message = "${doctorSavedTarget.member.effectiveName}님이 의사의 치료를 받고 살아났습니다!"
                )
            } else {
                DawnPresentation(
                    imageUrl = SystemImage.QUIET_NIGHT.imageUrl,
                    message = "조용하게 밤이 넘어갔습니다."
                )
            }
        } else {
            DawnPresentation(
                imageUrl = SystemImage.DEATH_BY_MAFIA.imageUrl,
                message = "${mafiaKillVictim.member.effectiveName}이(가) 살해당했습니다."
            )
        }
    }

    private fun dispatchEvents(game: Game): List<GameEvent> {
        val processedEvents = mutableListOf<GameEvent>()

        while (game.nightEvents.isNotEmpty()) {
            val eventsToProcess = game.nightEvents.toList()
            game.nightEvents.clear()
            processedEvents += eventsToProcess

            val observers = game.playerDatas
                .filter { !it.state.isDead }
                .mapNotNull { player ->
                    val passives = player.allAbilities
                         .filterIsInstance<PassiveAbility>()
                        .filterNot { FrogCurseManager.shouldSuppressPassive(player) }
                        .sortedByDescending(PassiveAbility::priority)
                    if (passives.isEmpty()) null else player to passives
                }

            eventsToProcess.forEach { event ->
                applyNurseDoctorInheritanceOnDeath(game, event)
                when (event) {
                    is GameEvent.JobDiscovered -> {
                        FrogCurseManager.displayedJob(event.target)?.let { event.revealedJob = it }
                    }
                    is GameEvent.PoliceJobRevealed -> {
                        FrogCurseManager.displayedJob(event.target)?.let { event.revealedJob = it }
                    }
                    else -> Unit
                }
                observers.forEach { (player, passives) ->
                    passives.forEach { passive ->
                        passive.onEventObserved(game, player, event)
                    }
                }
            }
        }

        return processedEvents
    }

    private fun applyNurseDoctorInheritanceOnDeath(game: Game, event: GameEvent) {
        val deathEvent = event as? GameEvent.PlayerDied ?: return
        if (deathEvent.victim.job !is Doctor) return

        game.playerDatas.forEach { nursePlayer ->
            val nurseJob = nursePlayer.job as? Nurse ?: return@forEach
            if (!nurseJob.hasContactedDoctor) return@forEach

            nurseJob.canUseInheritedHeal = true
            if (nursePlayer.job?.abilities?.none { it is DoctorAbility } == true) {
                nursePlayer.job?.abilities?.add(DoctorAbility())
            }
        }
    }

    private fun applyPoliceAutopsy(game: Game, victim: PlayerData) {
        game.playerDatas.forEach { policePlayer ->
            if (policePlayer.state.isDead) return@forEach
            if (policePlayer.member.id == victim.member.id) return@forEach
            if (policePlayer.allAbilities.none { it is Autopsy }) return@forEach

            val policeJob = policePlayer.job as? Police ?: return@forEach
            policeJob.eavesdroppingTargetId = victim.member.id
            policeJob.searchedTargets += victim.member.id

            votePresentationScope.launch {
                runCatching {
                    policePlayer.member.getDmChannel().createMessage(
                        "[부검] ${victim.member.effectiveName}님은 ${if (victim.job is Evil) "마피아 팀" else "시민 팀"}입니다."
                    )
                }
            }
        }
    }

    private fun applyPoliceConfidentialInvestigation(game: Game) {
        if (game.dayCount != 2) return

        game.playerDatas.forEach { policePlayer ->
            if (policePlayer.state.isDead) return@forEach
            if (policePlayer.allAbilities.none { it is Confidential }) return@forEach

            val policeJob = policePlayer.job as? Police ?: return@forEach
            if (policeJob.hasUsedConfidential) return@forEach

            val candidates = game.playerDatas.filter {
                !it.state.isDead && it.member.id != policePlayer.member.id
            }
            val selectedTarget = candidates.randomOrNull() ?: return@forEach

            policeJob.hasUsedConfidential = true
            policeJob.searchedTargets += selectedTarget.member.id

            votePresentationScope.launch {
                runCatching {
                    policePlayer.member.getDmChannel().createMessage(
                        "[기밀] ${selectedTarget.member.effectiveName}님 자동 조사 결과: ${if (selectedTarget.job is Evil) "마피아 팀" else "시민 팀"}"
                    )
                }
            }
        }
    }

    private suspend fun announceSourceMafiaCountAtNightStart(game: Game) {
        if (game.dayCount <= 1) return

        val hasAliveSource = game.playerDatas.any { player ->
            !player.state.isDead && player.allAbilities.any { it is Source }
        }
        if (!hasAliveSource) return

        val aliveMafiaTeamCount = game.playerDatas.count { player ->
            !player.state.isDead && player.job is Evil
        }
        game.sendMainChannerMessage("정보원에 의해 현재 ${aliveMafiaTeamCount}명의 마피아팀이 살아남은 것이 밝혀졌습니다.")
    }

    private suspend fun revealBelongingsIfNeeded(game: Game, victim: PlayerData) {
        if (victim.state.isJobPubliclyRevealed) return
        if (victim.allAbilities.none { it is Belongings }) return
        if (game.probationOriginalJobsByPlayer.containsKey(victim.member.id) && victim.job is Citizen) return

        val revealedJob = victim.job ?: return
        victim.state.isJobPubliclyRevealed = true
        game.sendMainChannelMessageWithImage(
            imageLink = BELONGINGS_REVEAL_IMAGE_URL,
            message = "${victim.member.effectiveName}님의 유품을 통해 직업이 ${revealedJob.name}(이)라고 밝혀졌습니다!"
        )
    }

    private fun resolveMercenaryAttackOrder(
        game: Game,
        blockedAttacks: List<AttackEvent>,
        playersToDie: MutableSet<PlayerData>
    ) {
        val mafiaAttack = game.nightAttacks["MAFIA_TEAM"] ?: return
        if (mafiaAttack in blockedAttacks) return

        val cancelledAttackKeys = mutableListOf<String>()
        val mercenaryAttackEntries = game.nightAttacks
            .filterKeys { it.startsWith("MERCENARY_") }
            .toList()
        if (mercenaryAttackEntries.isEmpty()) return

        mercenaryAttackEntries.forEach { (attackKey, mercenaryAttack) ->
            val mercenaryAttacker = mercenaryAttack.attacker
            if (mafiaAttack.target != mercenaryAttacker) return@forEach
            if (mercenaryAttack in blockedAttacks) return@forEach

            val hasResolute = mercenaryAttacker.allAbilities.any { it is Resolute }
            if (hasResolute) return@forEach

            cancelledAttackKeys += attackKey
            val target = mercenaryAttack.target
            val hasOtherUnblockedAttack = game.nightAttacks.any { (otherKey, otherAttack) ->
                otherKey != attackKey &&
                    otherAttack.target == target &&
                    otherAttack !in blockedAttacks
            }
            if (!hasOtherUnblockedAttack) {
                playersToDie.remove(target)
            }
        }

        cancelledAttackKeys.forEach { attackKey ->
            val cancelledAttack = game.nightAttacks.remove(attackKey) ?: return@forEach
            game.nightDeathCandidates.remove(cancelledAttack.target)
        }
    }


    private fun resolveVigilanteAttackOrder(
        game: Game,
        blockedAttacks: List<AttackEvent>,
        playersToDie: MutableSet<PlayerData>
    ) {
        val mafiaAttack = game.nightAttacks["MAFIA_TEAM"] ?: return
        if (mafiaAttack in blockedAttacks) return

        val cancelledAttackKeys = mutableListOf<String>()
        val vigilanteAttackEntries = game.nightAttacks
            .filterKeys { it.startsWith("VIGILANTE_") }
            .toList()
        if (vigilanteAttackEntries.isEmpty()) return

        vigilanteAttackEntries.forEach { (attackKey, vigilanteAttack) ->
            val vigilanteAttacker = vigilanteAttack.attacker
            if (vigilanteAttacker.job !is Vigilante) return@forEach
            if (mafiaAttack.target != vigilanteAttacker) return@forEach
            if (vigilanteAttack in blockedAttacks) return@forEach

            val hasResolute = vigilanteAttacker.allAbilities.any { it is Resolute }
            if (hasResolute) return@forEach

            cancelledAttackKeys += attackKey
            val target = vigilanteAttack.target
            val hasOtherUnblockedAttack = game.nightAttacks.any { (otherKey, otherAttack) ->
                otherKey != attackKey &&
                    otherAttack.target == target &&
                    otherAttack !in blockedAttacks
            }
            if (!hasOtherUnblockedAttack) {
                playersToDie.remove(target)
            }
        }

        cancelledAttackKeys.forEach { attackKey ->
            val cancelledAttack = game.nightAttacks.remove(attackKey) ?: return@forEach
            game.nightDeathCandidates.remove(cancelledAttack.target)
        }
    }
    private fun resolveMercenaryContractDeaths(
        game: Game,
        blockedAttacks: List<AttackEvent>,
        playersToDie: MutableSet<PlayerData>
    ) {
        val unblockedAttacks = game.nightAttacks.values.filterNot { it in blockedAttacks }

        game.playerDatas.forEach { mercenaryPlayer ->
            val mercenary = mercenaryPlayer.job as? Mercenary ?: return@forEach
            val clientId = mercenary.clientPlayerId ?: return@forEach
            val client = game.getPlayer(clientId) ?: return@forEach

            if (!mercenary.hasReceivedContract || mercenary.hasExecutionAuthority) return@forEach
            if (client !in playersToDie) return@forEach

            val killingAttack = unblockedAttacks.firstOrNull { it.target == client } ?: return@forEach
            mercenary.hasExecutionAuthority = true
            mercenary.clientKilledByPlayerId = killingAttack.attacker.member.id
        }
    }

    private fun notifyMercenaryContractReception(game: Game) {
        game.playerDatas.forEach { mercenaryPlayer ->
            val mercenary = mercenaryPlayer.job as? Mercenary ?: return@forEach
            if (mercenary.hasReceivedContract) return@forEach
            if (mercenaryPlayer.state.isDead) return@forEach

            val clientId = mercenary.clientPlayerId ?: return@forEach
            val client = game.getPlayer(clientId) ?: return@forEach
            if (client.state.isDead) return@forEach

            mercenary.hasReceivedContract = true
            sendCabalDm(
                mercenaryPlayer,
                "누군가에게 의뢰를 받았습니다"
            )
            sendCabalDm(client, "용병 ${mercenaryPlayer.member.effectiveName}님에게 의뢰를 했습니다")
        }
    }

    private suspend fun notifyMercenaryClientsAtFirstNight(game: Game) {
        if (game.dayCount != 1) return

        game.playerDatas.forEach { mercenaryPlayer ->
            val mercenary = mercenaryPlayer.job as? Mercenary ?: return@forEach
            val clientId = mercenary.clientPlayerId ?: return@forEach
            val client = game.getPlayer(clientId) ?: return@forEach

            runCatching {
                client.member.getDmChannel().createMessage("의뢰인으로 지정되었습니다.")
            }
        }
    }

    suspend fun notifyNurseDoctorContactImmediately(game: Game) {
        val doctorPlayer = game.playerDatas.firstOrNull { it.job is Doctor } ?: return
        val doctorJob = doctorPlayer.job as? Doctor ?: return

        game.playerDatas.forEach { nursePlayer ->
            if (nursePlayer.state.isDead) return@forEach
            val nurseJob = nursePlayer.job as? Nurse ?: return@forEach

            val targetId = nurseJob.prescribedTargetId ?: return@forEach
            val target = game.getPlayer(targetId) ?: return@forEach
            if (target.state.isDead) return@forEach

            val contactedByNurseTarget = target.member.id == doctorPlayer.member.id
            val contactedByDoctorTarget = doctorJob.currentHealTarget == nursePlayer.member.id
            if (!contactedByNurseTarget && !contactedByDoctorTarget) return@forEach

            val firstContact = !nurseJob.hasContactedDoctor
            nurseJob.hasContactedDoctor = true
            nurseJob.contactedDoctorId = doctorPlayer.member.id
            doctorJob.hasContactedNurse = true

            if (!firstContact) return@forEach

            runCatching {
                nursePlayer.member.getDmChannel().createMessage(
                    "$NURSE_DOCTOR_CONTACT_IMAGE_URL\n의사 (${doctorPlayer.member.effectiveName})님과 접선했습니다."
                )
            }
            runCatching {
                doctorPlayer.member.getDmChannel().createMessage(
                    "$NURSE_DOCTOR_CONTACT_IMAGE_URL\n간호사 (${nursePlayer.member.effectiveName})님과 접선했습니다."
                )
            }
        }
    }

    private suspend fun resolveNursePrescriptions(game: Game) {
        val doctorPlayer = game.playerDatas.firstOrNull { it.job is Doctor } ?: return
        val doctorJob = doctorPlayer.job as? Doctor ?: return

        game.playerDatas.forEach { nursePlayer ->
            if (nursePlayer.state.isDead) return@forEach
            val nurseJob = nursePlayer.job as? Nurse ?: return@forEach

            val targetId = nurseJob.prescribedTargetId ?: return@forEach
            val target = game.getPlayer(targetId) ?: return@forEach
            if (target.state.isDead) return@forEach

            if (target.job is Doctor) {
                val targetJob = target.job ?: return@forEach
                game.nightEvents += GameEvent.JobDiscovered(
                    discoverer = nursePlayer,
                    target = target,
                    actualJob = targetJob,
                    revealedJob = targetJob,
                    sourceAbilityName = "처방",
                    resolvedAt = DiscoveryStep.NIGHT,
                    notifyTarget = false
                )
            }

            val contactedByNurseTarget = target.member.id == doctorPlayer.member.id
            val contactedByDoctorTarget = doctorJob.currentHealTarget == nursePlayer.member.id
            if (contactedByNurseTarget || contactedByDoctorTarget) {
                nurseJob.hasContactedDoctor = true
                nurseJob.contactedDoctorId = doctorPlayer.member.id
                doctorJob.hasContactedNurse = true
            }
        }
    }

    private fun resolveDoctorHeals(game: Game) {
        val healers = game.playerDatas.filter { player ->
            val isDoctor = player.job is Doctor
            val isInheritedNurse = (player.job as? Nurse)?.canUseInheritedHeal == true
            isDoctor || isInheritedNurse
        }

        healers.forEach { player ->
            if (player.state.isDead) return@forEach

            val doctorJob = player.job as? Doctor
            val nurseJob = player.job as? Nurse
            val targetId = doctorJob?.currentHealTarget ?: nurseJob?.currentHealTarget ?: return@forEach
            val target = game.getPlayer(targetId) ?: run {
                doctorJob?.currentHealTarget = null
                nurseJob?.currentHealTarget = null
                return@forEach
            }

            val isAbsoluteHeal = doctorJob?.hasContactedNurse == true || nurseJob?.hasContactedDoctor == true
            val healEvent = GameEvent.PlayerHealed(
                healer = player,
                target = target,
                defenseTier = if (isAbsoluteHeal) DefenseTier.ABSOLUTE else DefenseTier.NORMAL
            )

            if (!isAbsoluteHeal) {
                player.job?.abilities
                    ?.filterIsInstance<PassiveAbility>()
                    ?.filterNot { FrogCurseManager.shouldSuppressPassive(player) }
                    ?.forEach { passive ->
                        passive.onEventObserved(game, player, healEvent)
                    }
            }

            target.state.healTier = maxOf(target.state.healTier, healEvent.defenseTier)

            if (player.allAbilities.any { it is Calm }) {
                // NOTE: 현재는 마피아의 독살(중독)만 해로운 효과로 구현되어 있어 해당 상태만 해제한다.
                // 이후 해로운 효과(예: 저주, 봉인, 추가 상태이상 등)가 확장되면 여기에서 함께 정리한다.
                target.state.isPoisoned = false
                target.state.poisonedDeathDay = null
                target.state.isThreatened = false
                game.activeThreatenedVoters.remove(target.member.id)
                game.playerDatas.forEach { gangsterOwner ->
                    val gangsterJob = gangsterOwner.job as? Gangster ?: return@forEach
                    gangsterJob.threatenedTargetIdsTonight.remove(target.member.id)
                }
            }

            game.nightEvents += healEvent
            doctorJob?.currentHealTarget = null
            nurseJob?.currentHealTarget = null
        }
    }

    private fun resolveGangsterThreats(game: Game) {
        game.activeThreatenedVoters.clear()
        game.playerDatas.forEach { player ->
            val gangster = player.job as? Gangster ?: return@forEach
            gangster.threatenedTargetIdsTonight.toList().forEach { targetId ->
                val target = game.getPlayer(targetId) ?: return@forEach
                if (target.state.isDead) return@forEach
                if (shouldIgnoreHarmfulEffectByMentalStrength(target)) {
                    gangster.threatenedTargetIdsTonight.remove(targetId)
                    return@forEach
                }
                target.state.isThreatened = true
                game.activeThreatenedVoters[targetId] = player.member.id
            }
        }
    }

    private fun applyTravelCompanionPenalty(
        game: Game,
        playersToDie: Set<PlayerData>,
        mafiaAttack: AttackEvent?
    ) {
        val attack = mafiaAttack ?: return
        if (attack.target !in playersToDie) return

        val deadGangster = attack.target
        val gangsterJob = deadGangster.job as? Gangster ?: return
        if (deadGangster.allAbilities.none { it is TravelCompanion }) return

        val killerId = attack.attacker.member.id
        if (killerId !in gangsterJob.threatenedTargetIdsTonight) return

        game.permanentlyDisenfranchisedVoters += killerId
        game.activeThreatenedVoters.remove(killerId)
    }

    private fun resolveAdministratorInvestigations(game: Game) {
        game.playerDatas.forEach { player ->
            val administratorJob = player.job as? Administrator ?: return@forEach
            val selectedJobName = administratorJob.selectedInvestigationJobName ?: return@forEach
            val selectedJob = org.beobma.mafia42discordproject.job.JobManager.findByName(selectedJobName) ?: run {
                administratorJob.investigationResultPlayerId = null
                return@forEach
            }

            val alivePlayers = game.playerDatas.filter { !it.state.isDead }
            val spoofedTarget = alivePlayers.firstOrNull { candidate ->
                AdministratorInvestigationPolicy.shouldApplyHypocrisySpoof(game.dayCount, selectedJob, candidate)
            }

            val target = spoofedTarget ?: alivePlayers.firstOrNull { candidate ->
                candidate.job?.name == selectedJob.name
            }
            administratorJob.investigationResultPlayerId = target?.member?.id
        }
    }


    private suspend fun resolveHackerHacks(game: Game) {
        game.playerDatas.forEach { player ->
            if (player.state.isDead) return@forEach

            val hacker = player.job as? Hacker ?: return@forEach
            if (hacker.hasResolvedHackDiscovery) return@forEach

            val hackedTargetId = hacker.hackedTargetId ?: return@forEach
            val target = game.getPlayer(hackedTargetId) ?: return@forEach
            if (target.state.isDead) {
                runCatching {
                    player.member.getDmChannel().createMessage(
                        "해킹에 실패했습니다.\nhttps://cdn.discordapp.com/attachments/1483977619258212392/1485044168127545386/Qyrssa_FCaE6cR1Zdm5w8EtHCtIOXJY8WPL6oS8XKOgDV-ISBsasQdNU7-fFubk06GpxmxQrV1u0CSrqetNj95tnQzz1RiVByQZVvnPhp8D6whxpv42-Pn7FN20qFmT14RzSxvkLjbbUZ09hYKFmug.webp?ex=69c06ea8&is=69bf1d28&hm=47936e6f642cf86983e5a5b88db180c51ef4cb441df66e6c0d741a1968fde93c&"
                    )
                }
                hacker.hasResolvedHackDiscovery = true
                return@forEach
            }

            val targetJob = target.job ?: return@forEach
            val shouldNotifyTarget =
                player.allAbilities.any { it is Synchronization } &&
                    targetJob !is Evil

            game.nightEvents += GameEvent.JobDiscovered(
                discoverer = player,
                target = target,
                actualJob = targetJob,
                revealedJob = targetJob,
                sourceAbilityName = "해킹",
                resolvedAt = DiscoveryStep.NIGHT,
                notifyTarget = shouldNotifyTarget
            )
            hacker.hasResolvedHackDiscovery = true
        }
    }

    private fun resolveReporterScoops(game: Game) {
        game.playerDatas.forEach { player ->
            val reporter = player.job as? Reporter ?: return@forEach
            if (player.state.isDead) return@forEach
            if (!reporter.hasUsedScoop) return@forEach
            if (reporter.articlePublishDay != null) return@forEach

            val targetId = reporter.selectedTargetId ?: return@forEach
            val target = game.getPlayer(targetId) ?: return@forEach
            val targetJob = target.job ?: return@forEach

            game.nightEvents += GameEvent.JobDiscovered(
                discoverer = player,
                target = target,
                actualJob = targetJob,
                revealedJob = targetJob,
                sourceAbilityName = "특종",
                resolvedAt = DiscoveryStep.NIGHT,
                notifyTarget = false
            )

            val hasBreakingNews = player.allAbilities.any { it is BreakingNews }
            val targetExecutedTonight = game.nightAttacks.values.any { attack ->
                attack.attacker.member.id == target.member.id
            }
            val isEmbargoBypassed = hasBreakingNews && targetExecutedTonight
            reporter.articlePublishDay = if (game.dayCount == 1 && !isEmbargoBypassed) {
                2
            } else {
                game.dayCount
            }
        }
    }

    private fun cacheReporterDiscoveryResults(events: List<GameEvent>) {
        events
            .filterIsInstance<GameEvent.JobDiscovered>()
            .filter { event ->
                event.sourceAbilityName == "특종" && !event.isCancelled
            }
            .forEach { event ->
                val reporter = event.discoverer.job as? Reporter ?: return@forEach
                reporter.discoveredJobName = event.revealedJob.name
                reporter.discoveredImageUrl = event.imageUrl ?: event.revealedJob.jobImage
            }
    }

    private suspend fun publishReporterArticles(game: Game) {
        game.playerDatas.forEach { player ->
            val reporter = player.job as? Reporter ?: return@forEach
            if (player.state.isDead) return@forEach
            if (reporter.hasPublishedArticle) return@forEach

            val discoveredJobName = reporter.discoveredJobName ?: return@forEach
            val targetId = reporter.selectedTargetId ?: return@forEach
            val publishDay = reporter.articlePublishDay ?: return@forEach
            if (game.dayCount < publishDay) return@forEach

            val target = game.getPlayer(targetId) ?: return@forEach
            val canPublishOnDeadTarget = player.allAbilities.any { it is Obituary }
            if (target.state.isDead && !canPublishOnDeadTarget) {
                reporter.hasPublishedArticle = true
                runCatching {
                    player.member.getDmChannel().createMessage(
                        "취재 대상(${target.member.effectiveName})이 사망하여 기사를 발행하지 못했습니다."
                    )
                }
                return@forEach
            }

            val discoveredJob = org.beobma.mafia42discordproject.job.JobManager.findByName(discoveredJobName)
                ?: target.job
                ?: return@forEach

            val event = GameEvent.JobDiscovered(
                discoverer = player,
                target = target,
                actualJob = discoveredJob,
                revealedJob = discoveredJob,
                sourceAbilityName = "특종",
                resolvedAt = DiscoveryStep.DAY,
                isPublicReveal = true,
                notifyTarget = false
            ).apply {
                imageUrl = reporter.discoveredImageUrl
            }

            JobDiscoveryNotificationManager.notifyDiscoveredTargets(listOf(event), game)
            reporter.hasPublishedArticle = true
        }
    }

    private fun applyMafiaExecutionFailureEffects(game: Game, mafiaAttack: AttackEvent) {
        val attacker = mafiaAttack.attacker
        val target = mafiaAttack.target

        if (attacker.allAbilities.any { it is Concealment }) {
            game.concealmentForcedQuietNight = true
        }

        if (attacker.allAbilities.any { it is Poisoning }) {
            if (shouldIgnoreHarmfulEffectByMentalStrength(target)) return
            target.state.isPoisoned = true
            target.state.poisonedDeathDay = game.dayCount + 1
            game.pendingPoisonNotifications[target.member.id] = attacker.member.id
        }
    }

    private fun shouldIgnoreHarmfulEffectByMentalStrength(target: PlayerData): Boolean {
        if (target.allAbilities.none { it is MentalStrength }) return false

        cabalNotificationScope.launch {
            runCatching {
                target.member.getDmChannel().createMessage("정신력의 힘으로 해로운 효과를 이겨냈습니다.")
            }
        }
        return true
    }

    private fun shouldNotifyAtDayStart(event: GameEvent): Boolean {
        val discoveredEvent = event as? GameEvent.JobDiscovered ?: return false
        return discoveredEvent.sourceAbilityName == "암시"
    }

    private fun notifyPendingPoisonEffects(game: Game) {
        if (game.pendingPoisonNotifications.isEmpty()) return

        val poisonNotifications = game.pendingPoisonNotifications.toMap()
        game.pendingPoisonNotifications.clear()

        poisonNotifications.forEach { (targetId, attackerId) ->
            val target = game.getPlayer(targetId) ?: return@forEach
            val attacker = game.getPlayer(attackerId)

            cabalNotificationScope.launch {
                runCatching {
                    target.member.getDmChannel().createMessage("중독 상태가 되었습니다.")
                }
            }

            if (attacker != null) {
                cabalNotificationScope.launch {
                    runCatching {
                        attacker.member.getDmChannel().createMessage("${target.member.effectiveName}님이 중독 상태가 되었습니다.")
                    }
                }
            }
        }
    }

    private fun applyMafiaExecutionSuccessEffects(game: Game, mafiaAttack: AttackEvent) {
        val attacker = mafiaAttack.attacker
        val target = mafiaAttack.target

        if (
            attacker.allAbilities.any { it is Exorcism } &&
            target.job !is Evil &&
            target.allAbilities.none { it is EarthboundSpirit }
        ) {
            target.state.isShamaned = true
        }

        if (attacker.allAbilities.any { it is Probation } && target.job !is Evil) {
            val originalJob = target.job ?: return
            game.nightEvents += GameEvent.JobDiscovered(
                discoverer = attacker,
                target = target,
                actualJob = originalJob,
                revealedJob = originalJob,
                sourceAbilityName = "수습",
                resolvedAt = DiscoveryStep.NIGHT,
                imageUrl = PROBATION_DISCOVERY_IMAGE_URL
            )
            game.probationOriginalJobsByPlayer[target.member.id] = originalJob
            target.job = Citizen()
        }
    }

    private fun resolvePoliceSearches(game: Game) {
        game.playerDatas.forEach { player ->
            val policeJob = player.job as? Police ?: return@forEach
            val targetId = policeJob.currentSearchTarget ?: return@forEach
            val target = game.getPlayer(targetId) ?: run {
                policeJob.currentSearchTarget = null
                return@forEach
            }

            val isRepeatedSearch = targetId in policeJob.searchedTargets
            game.nightEvents += GameEvent.PoliceSearchResolved(
                police = player,
                target = target,
                isMafia = target.job is Mafia,
                isRepeatedSearch = isRepeatedSearch
            )

            val warrant = player.allAbilities.filterIsInstance<Warrant>().firstOrNull()
            if (warrant?.shouldRevealJob(targetId, policeJob.searchedTargets) == true) {
                val actualJob = target.job
                if (actualJob != null) {
                    game.nightEvents += GameEvent.PoliceJobRevealed(
                        police = player,
                        target = target,
                        actualJob = actualJob,
                        revealedJob = actualJob,
                        resolvedAt = DiscoveryStep.NIGHT
                    )
                }
            }

            policeJob.searchedTargets += targetId
            policeJob.currentSearchTarget = null
        }
    }

    private fun applyInnateNightDefense(game: Game, target: PlayerData, attackEvent: AttackEvent) {
        // 1. 피격 직전(BeforeAttackEvaluated) 이벤트를 생성합니다.
        val event = GameEvent.BeforeAttackEvaluated(attackEvent)

        // 2. 타겟이 가진 패시브 능력들에게 이벤트를 전파하여 '방탄' 등이 스스로 방어(healTier 상승)하도록 합니다.
        target.allAbilities
            .filterIsInstance<PassiveAbility>()
            .filterNot { FrogCurseManager.shouldSuppressPassive(target) }
            .sortedByDescending(PassiveAbility::priority)
            .forEach { passive ->
                passive.onEventObserved(game, target, event)
            }
    }
}
