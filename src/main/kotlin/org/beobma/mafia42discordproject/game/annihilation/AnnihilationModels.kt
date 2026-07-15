package org.beobma.mafia42discordproject.game.annihilation

import dev.kord.common.entity.Snowflake
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.evil.Evil

class Capo : Job(), Evil {
    override val name: String = "카포"
    override val description: String = "말살 모드의 마피아 지휘자입니다."
}

class Soldato : Job(), Evil {
    override val name: String = "솔다토"
    override val description: String = "말살 모드의 마피아 행동대원입니다."
}

enum class AnnihilationLocation(val displayName: String) {
    SQUARE("광장"),
    ARCHIVE("자료실"),
    POLICE_STATION("경찰서"),
    MARKET("상가"),
    HOSPITAL("병원"),
    CONVENIENCE_STORE("편의점"),
    RESIDENTIAL_AREA("주택가"),
    ALLEY("골목길");

    fun isConnectedTo(other: AnnihilationLocation): Boolean {
        if (this == other) return true
        if (this == SQUARE || other == SQUARE) return true
        return setOf(this, other) in directConnections
    }

    companion object {
        private val directConnections = setOf(
            setOf(POLICE_STATION, ARCHIVE),
            setOf(MARKET, HOSPITAL),
            setOf(CONVENIENCE_STORE, RESIDENTIAL_AREA)
        )

        fun parse(raw: String?): AnnihilationLocation? {
            val normalized = raw?.trim() ?: return null
            return entries.firstOrNull { location ->
                location.name.equals(normalized, ignoreCase = true) ||
                    location.displayName == normalized
            }
        }
    }
}

data class SecretIdentity(
    val code: String,
    var isPubliclyRevealed: Boolean = false,
    var confirmedCitizen: Boolean = false
)

enum class CitizenMissionType {
    GO_LOCATION,
    MEET_PLAYER,
    BE_ALONE,
    LOST_ITEM,
    NPC_INQUIRY,
    JOINT_INVESTIGATION
}

data class CitizenMission(
    val id: Int,
    val type: CitizenMissionType,
    val description: String,
    val reward: Int = 1,
    val actorId: Snowflake? = null,
    val targetId: Snowflake? = null,
    val location: AnnihilationLocation? = null,
    val secondLocation: AnnihilationLocation? = null,
    var holderId: Snowflake? = null,
    var isCompleted: Boolean = false
)

data class CapoForcedMission(
    val day: Int,
    val movementPhase: Int,
    val locations: Set<AnnihilationLocation>,
    var resolved: Boolean = false
)

data class SoldatoForcedMission(
    val day: Int,
    val movementPhase: Int,
    val location: AnnihilationLocation,
    var resolved: Boolean = false
)

data class AgentProofPlan(
    val movementPhase: Int,
    val location: AnnihilationLocation,
    var resolved: Boolean = false
)

data class AnnihilationGameState(
    val identities: MutableMap<Snowflake, SecretIdentity> = mutableMapOf(),
    val locations: MutableMap<Snowflake, AnnihilationLocation> = mutableMapOf(),
    val previousLocations: MutableMap<Snowflake, AnnihilationLocation> = mutableMapOf(),
    val samePlaceStreaks: MutableMap<String, Int> = mutableMapOf(),
    val stolenIdentityHolderByOwner: MutableMap<Snowflake, Snowflake> = mutableMapOf(),
    val knownIdentityOwnerIdsByMafia: MutableSet<Snowflake> = mutableSetOf(),
    val movementSelections: MutableMap<Snowflake, AnnihilationLocation> = mutableMapOf(),
    val citizenMissions: MutableList<CitizenMission> = mutableListOf(),
    val mafiaNpcLocations: MutableSet<AnnihilationLocation> = mutableSetOf(),
    val mafiaMissionUseCountByPlayer: MutableMap<Snowflake, Int> = mutableMapOf(),
    val mafiaMissionDoneKeys: MutableSet<String> = mutableSetOf(),
    val mafiaMissionLocationsToday: MutableSet<AnnihilationLocation> = mutableSetOf(),
    val mafiaExecutionLocationsToday: MutableSet<AnnihilationLocation> = mutableSetOf(),
    val visitorsByLocationToday: MutableMap<AnnihilationLocation, MutableSet<Snowflake>> = mutableMapOf(),
    val notebookEntriesByOwner: MutableMap<Snowflake, MutableList<String>> = mutableMapOf(),
    val droppedNotebookLocationsByOwner: MutableMap<Snowflake, AnnihilationLocation> = mutableMapOf(),
    val notebookOwnerIdsByHolder: MutableMap<Snowflake, MutableSet<Snowflake>> = mutableMapOf(),
    val votes: MutableMap<Snowflake, String> = mutableMapOf(),
    var movementPhaseNumber: Int = 0,
    var isMovementPhaseActive: Boolean = false,
    var citizenProgress: Int = 0,
    var mafiaProgress: Int = 0,
    var missionDay: Int = 0,
    var nextMissionId: Int = 1,
    var extraMissionGrantedDay: Int? = null,
    var voteSuspectId: Snowflake? = null,
    var voteEndAtMillis: Long = 0L,
    var agentInvestigationChoiceId: Snowflake? = null,
    var agentProofUsed: Boolean = false,
    var agentImpersonationUsed: Boolean = false,
    var agentJointInvestigationDay: Int = 0,
    var executionBlockedMovementPhase: Int? = null,
    var pendingProofPlan: AgentProofPlan? = null,
    var pendingCapoMission: CapoForcedMission? = null,
    var pendingSoldatoMission: SoldatoForcedMission? = null,
    var capoMissionLastUsedDay: Int? = null,
    var soldatoMissionLastUsedDay: Int? = null,
    var lastCapoExecutionAtMillis: Long = 0L
)
