package org.beobma.mafia42discordproject.game

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.beobma.mafia42discordproject.game.player.PlayerData
import org.beobma.mafia42discordproject.game.system.AttackEvent
import org.beobma.mafia42discordproject.game.system.GameEvent
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object GameArchiveManager {
    private val json = Json { prettyPrint = true }
    private val archiveDir: Path = Path.of("data", "game-archives")
    private val fileNameFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC)

    fun archive(game: Game, endReason: String, winningTeamName: String?) {
        if (game.hasArchivedSnapshot) {
            return
        }

        runCatching {
            if (!Files.exists(archiveDir)) {
                Files.createDirectories(archiveDir)
            }

            val now = Instant.now()
            val root = buildSnapshot(game, endReason, winningTeamName, now)
            val fileName = "game-${fileNameFormatter.format(now)}-${game.guild.id.value}.json"
            val outputPath = archiveDir.resolve(fileName)
            Files.writeString(outputPath, json.encodeToString(JsonObject.serializer(), root))
            game.hasArchivedSnapshot = true
            println("[GameArchiveManager] 게임 데이터 저장 완료: $outputPath")
        }.onFailure {
            println("[GameArchiveManager] 게임 데이터 저장 실패: ${it.message}")
        }
    }

    private fun buildSnapshot(game: Game, endReason: String, winningTeamName: String?, archivedAt: Instant): JsonObject {
        return buildJsonObject {
            put("archivedAt", archivedAt.toString())
            put("endReason", endReason)
            winningTeamName?.let { put("winningTeam", it) }
            put("guildId", game.guild.id.value.toString())
            put("guildName", game.guild.name)
            put("dayCount", game.dayCount)
            put("initialPlayerCount", game.initialPlayerCount)
            put("currentPhase", game.currentPhase.name)
            put("isRunning", game.isRunning)
            putNullable("voiceChannelId", game.voiceChannelId?.value?.toString())
            put("nightPhaseStartedAtMillis", game.nightPhaseStartedAtMillis)
            putNullable("prophetSpecialWinScheduledTeam", game.prophetSpecialWinScheduledTeam?.name)

            put("channels", buildJsonObject {
                putNullable("main", game.mainChannel?.id?.value?.toString())
                putNullable("mafia", game.mafiaChannel?.id?.value?.toString())
                putNullable("couple", game.coupleChannel?.id?.value?.toString())
                putNullable("dead", game.deadChannel?.id?.value?.toString())
            })

            put("players", buildJsonArray {
                game.playerDatas.forEach { player ->
                    add(buildPlayerSnapshot(player))
                }
            })

            put("nightAttacks", buildJsonObject {
                game.nightAttacks.toSortedMap().forEach { (attackGroup, attackEvent) ->
                    put(attackGroup, buildAttackEventSnapshot(attackEvent))
                }
            })

            put("nightDeathCandidates", buildJsonArray {
                game.nightDeathCandidates.forEach { add(playerRef(it)) }
            })

            put("nightEvents", buildJsonArray {
                game.nightEvents.forEach { add(eventSummary(it)) }
            })

            put("lastNightSummary", buildJsonObject {
                put("processedEvents", buildJsonArray {
                    game.lastNightSummary.processedEvents.forEach { add(eventSummary(it)) }
                })
                put("deaths", buildJsonArray {
                    game.lastNightSummary.deaths.forEach { add(playerRef(it)) }
                })
                put("blockedAttacks", buildJsonArray {
                    game.lastNightSummary.blockedAttacks.forEach { add(buildAttackEventSnapshot(it)) }
                })
                put("dawnPresentation", buildJsonObject {
                    putNullable("imageUrl", game.lastNightSummary.dawnPresentation?.imageUrl)
                    putNullable("message", game.lastNightSummary.dawnPresentation?.message)
                })
            })

            put("votes", buildJsonObject {
                put("main", buildJsonObject {
                    game.currentMainVotes.entries.sortedBy { it.key.value }.forEach { (voterId, targetLabel) ->
                        put(voterId.value.toString(), targetLabel)
                    }
                })
                put("fake", buildJsonObject {
                    game.currentFakeVotes.entries.sortedBy { it.key.value }.forEach { (voterId, targetId) ->
                        put(voterId.value.toString(), targetId.value.toString())
                    }
                })
                put("prosCons", buildJsonObject {
                    game.currentProsConsVotes.entries.sortedBy { it.key.value }.forEach { (voterId, isPros) ->
                        put(voterId.value.toString(), isPros)
                    }
                })
                putNullable("defenseTargetId", game.defenseTargetId?.value?.toString())
                putNullable("unwrittenRuleBlockedTargetIdTonight", game.unwrittenRuleBlockedTargetIdTonight?.value?.toString())
            })

            put("sets", buildJsonObject {
                put("pendingEscapedPlayerIds", toSortedIdArray(game.pendingEscapedPlayerIds))
                put("pendingNightDeathPlayerIds", toSortedIdArray(game.pendingNightDeathPlayerIds))
                put("publiclyRevealedAbilityTargetIds", toSortedIdArray(game.publiclyRevealedAbilityTargetIds))
                put("usedMegaphonePlayerIds", toSortedIdArray(game.usedMegaphonePlayerIds))
                put("usedSecretLetterPlayerIds", toSortedIdArray(game.usedSecretLetterPlayerIds))
                put("ghostTriggeredGhouls", toSortedIdArray(game.ghostTriggeredGhouls))
                put("pendingBeastmanTameIds", toSortedIdArray(game.pendingBeastmanTameIds))
                put("abilityUsersThisPhase", toSortedIdArray(game.abilityUsersThisPhase))
                put("dayTimeAdjustmentUsedPlayers", toSortedIdArray(game.dayTimeAdjustmentUsedPlayers))
                put("permanentlyDisenfranchisedVoters", toSortedIdArray(game.permanentlyDisenfranchisedVoters))
            })

            put("flags", buildJsonObject {
                put("mafiaAttackFailedPreviousNight", game.mafiaAttackFailedPreviousNight)
                put("concealmentForcedQuietNight", game.concealmentForcedQuietNight)
                put("mafiaExecutionSucceededLastNight", game.mafiaExecutionSucceededLastNight)
                put("megaphoneUsedTonight", game.megaphoneUsedTonight)
            })

            put("maps", buildJsonObject {
                put("coupleSacrificeMap", toIdMap(game.coupleSacrificeMap))
                put("willByPlayerId", toStringMap(game.willByPlayerId))
                put("activeThreatenedVoters", toIdMap(game.activeThreatenedVoters))
                put("graveRobTargetsByGhoul", toIdMap(game.graveRobTargetsByGhoul))
                put("pendingPoisonNotifications", toIdMap(game.pendingPoisonNotifications))
                put("pendingWitchCurseByCaster", toIdMap(game.pendingWitchCurseByCaster))
                put("pendingOblivionCurseByCaster", toIdMap(game.pendingOblivionCurseByCaster))
                put("abilityTargetByUserThisPhase", toIdMap(game.abilityTargetByUserThisPhase))
                put("hostessFirstVoteTargetByDay", toIdMap(game.hostessFirstVoteTargetByDay))
            })

            put("pendingLettersByRecipient", buildJsonObject {
                game.pendingLettersByRecipient.entries.sortedBy { it.key.value }.forEach { (recipientId, deliveries) ->
                    put(recipientId.value.toString(), buildJsonArray {
                        deliveries.forEach { delivery ->
                            add(buildJsonObject {
                                put("title", delivery.title)
                                put("content", delivery.content)
                            })
                        }
                    })
                }
            })

            put("dayStartDiscoveries", buildJsonArray {
                game.pendingDayStartDiscoveries.forEach { add(eventSummary(it)) }
            })

            put("seductionStatusByTarget", buildJsonObject {
                game.seductionStatusByTarget.entries.sortedBy { it.key.value }.forEach { (targetId, status) ->
                    put(targetId.value.toString(), buildJsonObject {
                        put("hostessId", status.hostessId.value.toString())
                        put("minimumReleaseDay", status.minimumReleaseDay)
                        put("isPermanent", status.isPermanent)
                    })
                }
            })
        }
    }

    private fun buildPlayerSnapshot(player: PlayerData): JsonObject {
        return buildJsonObject {
            put("id", player.member.id.value.toString())
            put("name", player.member.effectiveName)
            putNullable("job", player.job?.name)
            putNullable("team", player.job?.team?.displayName)
            put("abilities", buildJsonArray {
                player.allAbilities.forEach { ability ->
                    add(JsonPrimitive(ability.name))
                }
            })
            put("state", buildJsonObject {
                put("isDead", player.state.isDead)
                put("hasUsedOneTimeAbility", player.state.hasUsedOneTimeAbility)
                put("hasUsedDailyAbility", player.state.hasUsedDailyAbility)
                put("isTamed", player.state.isTamed)
                put("isJobPubliclyRevealed", player.state.isJobPubliclyRevealed)
                put("hasAnnouncedGodfatherContact", player.state.hasAnnouncedGodfatherContact)
                put("hasAnnouncedHitmanContact", player.state.hasAnnouncedHitmanContact)
                put("hasAnnouncedMadScientistContact", player.state.hasAnnouncedMadScientistContact)
                put("hasAnnouncedThiefContact", player.state.hasAnnouncedThiefContact)
                put("hasContactedMafiaByInformant", player.state.hasContactedMafiaByInformant)
                put("hasContactedMafiaOnDeath", player.state.hasContactedMafiaOnDeath)
                put("hasUsedMadScientistRegeneration", player.state.hasUsedMadScientistRegeneration)
                putNullable("pendingMadScientistRevivalNight", player.state.pendingMadScientistRevivalNight)
                putNullable("pendingMadScientistPublicRevealNight", player.state.pendingMadScientistPublicRevealNight)
                put("isMadScientistDistortionHidden", player.state.isMadScientistDistortionHidden)
                putNullable("madScientistLynchedVoteTargetId", player.state.madScientistLynchedVoteTargetId?.value?.toString())
                putNullable("madScientistAnalysisEligibleDay", player.state.madScientistAnalysisEligibleDay)
                put("hasUsedMadScientistAnalysis", player.state.hasUsedMadScientistAnalysis)
                putNullable("lastPaparazziIssueDay", player.state.lastPaparazziIssueDay)
                put("healTier", player.state.healTier.name)
                put("isSilenced", player.state.isSilenced)
                put("isThreatened", player.state.isThreatened)
                put("isShamaned", player.state.isShamaned)
                put("isPoisoned", player.state.isPoisoned)
                putNullable("poisonedDeathDay", player.state.poisonedDeathDay)
                put("isFrogCursed", player.state.isFrogCursed)
                putNullable("frogCurseExpiresAfterDay", player.state.frogCurseExpiresAfterDay)
            })
        }
    }

    private fun playerRef(player: PlayerData): JsonObject {
        return buildJsonObject {
            put("id", player.member.id.value.toString())
            put("name", player.member.effectiveName)
            putNullable("job", player.job?.name)
        }
    }

    private fun buildAttackEventSnapshot(event: AttackEvent): JsonObject {
        return buildJsonObject {
            put("attacker", playerRef(event.attacker))
            put("target", playerRef(event.target))
            put("attackTier", event.attackTier.name)
        }
    }

    private fun eventSummary(event: GameEvent): JsonObject {
        return buildJsonObject {
            putNullable("type", event::class.simpleName)
            put("payload", event.toString())
        }
    }

    private fun JsonObjectBuilder.putNullable(key: String, value: String?) {
        if (value == null) {
            put(key, JsonNull)
            return
        }
        put(key, value)
    }

    private fun JsonObjectBuilder.putNullable(key: String, value: Int?) {
        if (value == null) {
            put(key, JsonNull)
            return
        }
        put(key, value)
    }

    private fun toSortedIdArray(ids: Set<dev.kord.common.entity.Snowflake>) = buildJsonArray {
        ids.map { it.value.toString() }
            .sorted()
            .forEach { add(JsonPrimitive(it)) }
    }

    private fun toIdMap(map: Map<dev.kord.common.entity.Snowflake, dev.kord.common.entity.Snowflake>): JsonObject {
        return buildJsonObject {
            map.entries.sortedBy { it.key.value }.forEach { (key, value) ->
                put(key.value.toString(), value.value.toString())
            }
        }
    }

    private fun toStringMap(map: Map<dev.kord.common.entity.Snowflake, String>): JsonObject {
        return buildJsonObject {
            map.entries.sortedBy { it.key.value }.forEach { (key, value) ->
                put(key.value.toString(), value)
            }
        }
    }
}
