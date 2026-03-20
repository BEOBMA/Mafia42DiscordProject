package org.beobma.mafia42discordproject.listener
import dev.kord.core.Kord

interface InteractionListener {
    fun register(kord: Kord)
}