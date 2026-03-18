package org.beobma.mafia42discordproject.job.ability

import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.definition.Definition
import org.beobma.mafia42discordproject.job.evil.Evil

object AbilityManager {
    private val extraAbilityPool = mutableListOf<Ability>()

    fun register(ability: Ability) {
        extraAbilityPool.add(ability)
    }

    fun registerAll() {
        if (extraAbilityPool.isNotEmpty()) return

        listOf(
            org.beobma.mafia42discordproject.job.ability.general.definition.list.Belongings(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.Source(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.administrator.Cooperation(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.administrator.Identification(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.administrator.Inspection(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.agent.Humint(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.cabal.Marker(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.couple.Dedication(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.couple.Resentment(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.detective.Trap(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.doctor.Calm(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.doctor.Philanthropy(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.doctor.Screening(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.fortuneteller.Arcana(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.gangster.CombinedAttack(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.gangster.TravelCompanion(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.ghoul.Ghost(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.ghoul.Succession(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.hacker.Synchronization(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.hypnotist.Hint(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.judge.GovernmentAuthority(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.magician.Assistant(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.magician.Xray(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.martyr.Explosion(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.martyr.Flash(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.mentalist.Profiling(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.mercenary.Tracking(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.nurse.Oath(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.other.Eavesdropping(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.other.Postmortem(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.other.Resolute(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.other.UnwrittenRule(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.paparazzi.Tact(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.police.Autopsy(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.police.Confidential(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.police.Warrant(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.politician.Dictatorship(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.priest.Blessing(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.priest.Exorcism(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.prophet.Apostle(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.prophet.Pioneer(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.reporter.BreakingNews(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.reporter.Obituary(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.shaman.Manifesto(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.soldier.Indomitable(),
            org.beobma.mafia42discordproject.job.ability.general.definition.list.soldier.MentalStrength(),
            org.beobma.mafia42discordproject.job.ability.general.evil.list.Instructions(),
            org.beobma.mafia42discordproject.job.ability.general.evil.list.Password(),
            org.beobma.mafia42discordproject.job.ability.general.evil.list.Terminal(),
            org.beobma.mafia42discordproject.job.ability.general.evil.list.assistance.TheInformant(),
            org.beobma.mafia42discordproject.job.ability.general.evil.list.beastman.Barbarism(),
            org.beobma.mafia42discordproject.job.ability.general.evil.list.beastman.Roar(),
            org.beobma.mafia42discordproject.job.ability.general.evil.list.godfather.Cleanup(),
            org.beobma.mafia42discordproject.job.ability.general.evil.list.hitman.Intuition(),
            org.beobma.mafia42discordproject.job.ability.general.evil.list.hostess.Debut(),
            org.beobma.mafia42discordproject.job.ability.general.evil.list.hostess.Deception(),
            org.beobma.mafia42discordproject.job.ability.general.evil.list.madscientist.Analysis(),
            org.beobma.mafia42discordproject.job.ability.general.evil.list.madscientist.Distortion(),
            org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.Concealment(),
            org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.Exorcism(),
            org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.Hypocrisy(),
            org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.NightRaid(),
            org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.Outlaw(),
            org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.Poisoning(),
            org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.Probation(),
            org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.Sniper(),
            org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.Wanted(),
            org.beobma.mafia42discordproject.job.ability.general.evil.list.mafia.WinOrDead(),
            org.beobma.mafia42discordproject.job.ability.general.evil.list.other.BeautyTrap(),
            org.beobma.mafia42discordproject.job.ability.general.evil.list.spy.Assassin(),
            org.beobma.mafia42discordproject.job.ability.general.evil.list.spy.Autopsy(),
            org.beobma.mafia42discordproject.job.ability.general.evil.list.thief.Condolences(),
            org.beobma.mafia42discordproject.job.ability.general.evil.list.thief.Successor(),
            org.beobma.mafia42discordproject.job.ability.general.evil.list.witch.Oblivion(),
            org.beobma.mafia42discordproject.job.ability.general.list.EarthboundSpirit(),
            org.beobma.mafia42discordproject.job.ability.general.list.Escape(),
            org.beobma.mafia42discordproject.job.ability.general.list.Innocence(),
            org.beobma.mafia42discordproject.job.ability.general.list.Jury(),
            org.beobma.mafia42discordproject.job.ability.general.list.Megaphone(),
            org.beobma.mafia42discordproject.job.ability.general.list.MindReading(),
            org.beobma.mafia42discordproject.job.ability.general.list.Perjury(),
            org.beobma.mafia42discordproject.job.ability.general.list.SecretLetter(),
            org.beobma.mafia42discordproject.job.ability.general.list.Will()
        ).forEach(::register)
    }

    // 특정 직업이 획득 가능한 능력만 필터링
    fun getAvailableExtraAbilitiesFor(job: Job): List<Ability> {
        return extraAbilityPool.filter { ability ->
            when (ability) {
                is CommonAbility -> true
                is EvilCommonAbility -> job is Evil
                is CitizenCommonAbility -> job is Definition
                is JobSpecificExtraAbility -> ability.targetJob.isInstance(job)
                else -> false
            }
        }
    }
}
