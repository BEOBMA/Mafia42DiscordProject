package org.beobma.mafia42discordproject.job

import org.beobma.mafia42discordproject.job.definition.list.Administrator
import org.beobma.mafia42discordproject.job.definition.list.Agent
import org.beobma.mafia42discordproject.job.definition.list.Cabal
import org.beobma.mafia42discordproject.job.definition.list.Citizen
import org.beobma.mafia42discordproject.job.definition.list.Couple
import org.beobma.mafia42discordproject.job.definition.list.Detective
import org.beobma.mafia42discordproject.job.definition.list.Doctor
import org.beobma.mafia42discordproject.job.definition.list.Fortuneteller
import org.beobma.mafia42discordproject.job.definition.list.Gangster
import org.beobma.mafia42discordproject.job.definition.list.Ghoul
import org.beobma.mafia42discordproject.job.definition.list.Hacker
import org.beobma.mafia42discordproject.job.definition.list.Hypnotist
import org.beobma.mafia42discordproject.job.definition.list.Judge
import org.beobma.mafia42discordproject.job.definition.list.Magician
import org.beobma.mafia42discordproject.job.definition.list.Martyr
import org.beobma.mafia42discordproject.job.definition.list.Mentalist
import org.beobma.mafia42discordproject.job.definition.list.Mercenary
import org.beobma.mafia42discordproject.job.definition.list.Nurse
import org.beobma.mafia42discordproject.job.definition.list.Paparazzi
import org.beobma.mafia42discordproject.job.definition.list.Police
import org.beobma.mafia42discordproject.job.definition.list.Politician
import org.beobma.mafia42discordproject.job.definition.list.Prophet
import org.beobma.mafia42discordproject.job.definition.list.Priest
import org.beobma.mafia42discordproject.job.definition.list.Reporter
import org.beobma.mafia42discordproject.job.definition.list.Shaman
import org.beobma.mafia42discordproject.job.definition.list.Soldier
import org.beobma.mafia42discordproject.job.definition.list.Vigilante
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

object JobManager {
    private val jobs = mutableListOf<Job>()

    init {
        registerAll()
    }

    fun register(job: Job) {
        require(jobs.none { it.name == job.name }) {
            "이미 등록된 직업입니다: ${job.name}"
        }
        jobs.add(job)
    }

    fun getAll(): List<Job> = jobs.toList()

    fun findByName(name: String): Job? = jobs.firstOrNull { it.name == name }

    private fun registerAll() {
        listOf(
            Mafia(), Godfather(), Spy(), HitMan(), Thief(), Beastman(), Villain(), Hostess(),
            Swindler(), Witch(), MadScientist(),
            Citizen(), Doctor(), Police(), Detective(), Soldier(), Nurse(), Priest(), Shaman(),
            Fortuneteller(), Ghoul(), Judge(), Politician(), Agent(), Couple(), Vigilante(),
            Reporter(), Hacker(), Magician(), Martyr(), Prophet(), Mercenary(), Gangster(),
            Cabal(), Mentalist(), Hypnotist(), Paparazzi(), Administrator()
        ).forEach(::register)
    }
}
