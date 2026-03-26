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
    private val jobsByName = mutableMapOf<String, Job>()
    private var jobsSnapshot: List<Job> = emptyList()
    private val jobFactories = mutableMapOf<String, () -> Job>()

    fun register(job: Job) {
        println("[JobManager] register start name=${job.name}")
        require(jobs.none { it.name == job.name }) {
            "이미 등록된 직업입니다: ${job.name}"
        }
        jobs.add(job)
        jobsByName[job.name] = job
        jobsSnapshot = jobs.toList()
        println("[JobManager] register success name=${job.name}")
    }

    fun getAll(): List<Job> = jobsSnapshot

    fun findByName(name: String): Job? {
        println("[JobManager] findByName start name=$name size=${jobs.size}")
        val result = jobsByName[name]
        println("[JobManager] findByName end name=$name result=${result?.name}")
        return result
    }

    fun createByName(name: String): Job? {
        println("[JobManager] createByName start name=$name")
        val factory = jobFactories[name]
        val job = factory?.invoke()
        println("[JobManager] createByName end name=$name result=${job?.name}")
        return job
    }

    fun registerAll() {
        println("[JobManager] registerAll start")

        listOf<Pair<String, () -> Job>>(
            "Mafia" to { Mafia() },
            "Godfather" to { Godfather() },
            "Spy" to { Spy() },
            "HitMan" to { HitMan() },
            "Thief" to { Thief() },
            "Beastman" to { Beastman() },
            "Villain" to { Villain() },
            "Hostess" to { Hostess() },
            "Swindler" to { Swindler() },
            "Witch" to { Witch() },
            "MadScientist" to { MadScientist() },

            "Citizen" to { Citizen() },
            "Doctor" to { Doctor() },
            "Police" to { Police() },
            "Detective" to { Detective() },
            "Soldier" to { Soldier() },
            "Nurse" to { Nurse() },
            "Priest" to { Priest() },
            "Shaman" to { Shaman() },
            "Fortuneteller" to { Fortuneteller() },
            "Ghoul" to { Ghoul() },
            "Judge" to { Judge() },
            "Politician" to { Politician() },
            "Agent" to { Agent() },
            "Couple" to { Couple() },
            "Vigilante" to { Vigilante() },
            "Reporter" to { Reporter() },
            "Hacker" to { Hacker() },
            "Magician" to { Magician() },
            "Martyr" to { Martyr() },
            "Prophet" to { Prophet() },
            "Mercenary" to { Mercenary() },
            "Gangster" to { Gangster() },
            "Cabal" to { Cabal() },
            "Mentalist" to { Mentalist() },
            "Hypnotist" to { Hypnotist() },
            "Paparazzi" to { Paparazzi() },
            "Administrator" to { Administrator() }
        ).forEach { (className, factory) ->
            val job = logCreate(className, factory)
            register(job)
            registerFactory(job.name, className, factory)
        }

        println("[JobManager] registerAll end")
    }

    private fun logCreate(name: String, block: () -> Job): Job {
        println("[JobManager] create start class=$name")
        val job = block()
        println("[JobManager] create success class=$name jobName=${job.name}")
        return job
    }

    private fun registerFactory(name: String, className: String, factory: () -> Job) {
        println("[JobManager] registerFactory start name=$name class=$className")
        require(name !in jobFactories) {
            "이미 등록된 직업 팩토리입니다: $name"
        }
        jobFactories[name] = factory
        println("[JobManager] registerFactory success name=$name class=$className")
    }
}
