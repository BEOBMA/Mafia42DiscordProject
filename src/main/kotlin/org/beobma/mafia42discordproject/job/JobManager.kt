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

    fun register(job: Job) {
        println("[JobManager] register start name=${job.name}")
        require(jobs.none { it.name == job.name }) {
            "이미 등록된 직업입니다: ${job.name}"
        }
        jobs.add(job)
        println("[JobManager] register success name=${job.name}")
    }

    fun getAll(): List<Job> = jobs.toList()

    fun findByName(name: String): Job? {
        println("[JobManager] findByName start name=$name size=${jobs.size}")
        val result = jobs.firstOrNull { it.name == name }
        println("[JobManager] findByName end name=$name result=${result?.name}")
        return result
    }

    fun registerAll() {
        println("[JobManager] registerAll start")

        listOf(
            logCreate("Mafia") { Mafia() },
            logCreate("Godfather") { Godfather() },
            logCreate("Spy") { Spy() },
            logCreate("HitMan") { HitMan() },
            logCreate("Thief") { Thief() },
            logCreate("Beastman") { Beastman() },
            logCreate("Villain") { Villain() },
            logCreate("Hostess") { Hostess() },
            logCreate("Swindler") { Swindler() },
            logCreate("Witch") { Witch() },
            logCreate("MadScientist") { MadScientist() },

            logCreate("Citizen") { Citizen() },
            logCreate("Doctor") { Doctor() },
            logCreate("Police") { Police() },
            logCreate("Detective") { Detective() },
            logCreate("Soldier") { Soldier() },
            logCreate("Nurse") { Nurse() },
            logCreate("Priest") { Priest() },
            logCreate("Shaman") { Shaman() },
            logCreate("Fortuneteller") { Fortuneteller() },
            logCreate("Ghoul") { Ghoul() },
            logCreate("Judge") { Judge() },
            logCreate("Politician") { Politician() },
            logCreate("Agent") { Agent() },
            logCreate("Couple") { Couple() },
            logCreate("Vigilante") { Vigilante() },
            logCreate("Reporter") { Reporter() },
            logCreate("Hacker") { Hacker() },
            logCreate("Magician") { Magician() },
            logCreate("Martyr") { Martyr() },
            logCreate("Prophet") { Prophet() },
            logCreate("Mercenary") { Mercenary() },
            logCreate("Gangster") { Gangster() },
            logCreate("Cabal") { Cabal() },
            logCreate("Mentalist") { Mentalist() },
            logCreate("Hypnotist") { Hypnotist() },
            logCreate("Paparazzi") { Paparazzi() },
            logCreate("Administrator") { Administrator() }
        ).forEach(::register)

        println("[JobManager] registerAll end")
    }

    private fun logCreate(name: String, block: () -> Job): Job {
        println("[JobManager] create start class=$name")
        val job = block()
        println("[JobManager] create success class=$name jobName=${job.name}")
        return job
    }
}