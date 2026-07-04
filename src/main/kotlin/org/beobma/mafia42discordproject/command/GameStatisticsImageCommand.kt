package org.beobma.mafia42discordproject.command

import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.utils.io.ByteReadChannel
import org.beobma.mafia42discordproject.game.statistics.GameStatisticsImageRenderer
import org.beobma.mafia42discordproject.game.statistics.GameStatisticsManager

object GameStatisticsImageCommand : DiscordCommand {
    override val name: String = "gamestatsimage"
    override val description: String = "저장된 게임 통계를 이미지 리포트로 생성해 보냅니다."
    override val koreanName: String = "통계이미지"
    override val aliases: Set<String> = setOf("statsimage", "통계이미지", "통계사진", "통계리포트")

    override suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent) {
        val response = event.interaction.deferPublicResponse()
        val generation = GameStatisticsManager.generate()
        val images = GameStatisticsImageRenderer.render(generation.outputPath)
        val firstImage = images.firstOrNull()

        if (firstImage == null) {
            response.respond {
                content = "통계 이미지로 만들 데이터가 없습니다."
            }
            return
        }

        response.respond {
            content = buildPageMessage(generation, 1, images.size)
            addFile(firstImage.fileName, imageProvider(firstImage))
        }

        val channel = event.interaction.channel
        images.drop(1).forEachIndexed { index, image ->
            channel.createMessage {
                content = buildPageMessage(generation, index + 2, images.size)
                addFile(image.fileName, imageProvider(image))
            }
        }
    }

    override suspend fun handleMessage(event: MessageCreateEvent, args: List<String>) {
        val generation = GameStatisticsManager.generate()
        val images = GameStatisticsImageRenderer.render(generation.outputPath)

        if (images.isEmpty()) {
            event.message.channel.createMessage("통계 이미지로 만들 데이터가 없습니다.")
            return
        }

        images.forEachIndexed { index, image ->
            event.message.channel.createMessage {
                content = buildPageMessage(generation, index + 1, images.size)
                addFile(image.fileName, imageProvider(image))
            }
        }
    }

    private fun buildPageMessage(
        generation: GameStatisticsManager.GenerationResult,
        page: Int,
        totalPages: Int,
    ): String {
        val pageLabel = if (totalPages == 1) "" else " ($page/$totalPages)"
        return "게임 통계 이미지 리포트$pageLabel\n반영된 고유 게임: ${generation.processedArchiveCount}개 / 새로 반영: ${generation.newArchiveCount}개"
    }

    private fun imageProvider(
        image: GameStatisticsImageRenderer.RenderedStatisticsImage,
    ): ChannelProvider = ChannelProvider(image.bytes.size.toLong()) {
        ByteReadChannel(image.bytes)
    }
}
