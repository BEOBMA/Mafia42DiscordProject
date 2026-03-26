package org.beobma.mafia42discordproject.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.behavior.interaction.suggestString
import dev.kord.core.entity.interaction.StringOptionValue
import dev.kord.core.event.interaction.GuildAutoCompleteInteractionCreateEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.string
import io.ktor.client.request.forms.*
import io.ktor.utils.io.*
import org.beobma.mafia42discordproject.discord.DiscordMessageManager
import org.beobma.mafia42discordproject.job.Job
import org.beobma.mafia42discordproject.job.JobManager
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

object JobInfoImageCommand : DiscordCommand {
    override val name: String = "jobinfo"
    override val description: String = "직업 및 능력 정보를 이미지로 확인합니다."
    override val koreanName: String = "직업정보"
    override val aliases: Set<String> = setOf("직업정보", "직업능력")

    private const val jobOptionName = "job"
    private const val maxAutoCompleteChoices = 25

    override suspend fun handleAutoComplete(event: GuildAutoCompleteInteractionCreateEvent) {
        val focusedEntry = event.interaction.command.options.entries.firstOrNull { it.value.focused } ?: return
        if (focusedEntry.key != jobOptionName) return

        val query = (focusedEntry.value as? StringOptionValue)?.value?.trim().orEmpty()
        val suggestions = JobManager.getAll()
            .map(Job::name)
            .filter { query.isBlank() || it.contains(query, ignoreCase = true) }
            .take(maxAutoCompleteChoices)

        event.interaction.suggestString {
            suggestions.forEach { jobName ->
                choice(jobName, jobName)
            }
        }
    }

    override suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent) {
        val selectedJobName = event.interaction.command.strings[jobOptionName]
        val selectedJob = selectedJobName?.let { name ->
            JobManager.getAll().firstOrNull { it.name == name }
        }

        if (selectedJob == null) {
            val jobs = JobManager.getAll().joinToString(", ") { it.name }
            DiscordMessageManager.respondEphemeral(event, "직업을 찾지 못했습니다. 가능한 직업: $jobs")
            return
        }

        val imageBytes = renderJobInfoImage(selectedJob)
        val response = event.interaction.deferEphemeralResponse()
        response.respond {
            content = "${selectedJob.name} 직업 정보입니다."
            addFile(
                "${selectedJob.name}-info.png",
                ChannelProvider(imageBytes.size.toLong()) {
                    ByteReadChannel(imageBytes)
                }
            )
        }
    }

    override suspend fun registerGlobal(kord: Kord) {
        kord.createGlobalChatInputCommand(name, description) {
            applyKoreanLocalization(this)
            string(jobOptionName, "정보를 확인할 직업") {
                required = true
                autocomplete = true
            }
        }
    }

    override suspend fun registerGuild(kord: Kord, guildId: Snowflake) {
        kord.createGuildChatInputCommand(guildId, name, description) {
            applyKoreanLocalization(this)
            string(jobOptionName, "정보를 확인할 직업") {
                required = true
                autocomplete = true
            }
        }
    }

    private fun renderJobInfoImage(job: Job): ByteArray {
        val width = 1180
        val sidePadding = 52
        val titleFont = Font("SansSerif", Font.BOLD, 52)
        val headingFont = Font("SansSerif", Font.BOLD, 36)
        val bodyFont = Font("SansSerif", Font.PLAIN, 28)
        val lineGap = 10
        val sectionGap = 26

        val tempImage = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val tempGraphics = tempImage.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        }

        val lines = mutableListOf<LineSpec>()
        lines += LineSpec(job.name, titleFont, Color(24, 24, 24))
        lines += LineSpec("직업 설명", headingFont, Color(45, 45, 45), addTopSpacing = sectionGap)
        splitIntoLines(job.description, bodyFont, width - sidePadding * 2, tempGraphics)
            .forEach { line -> lines += LineSpec(line, bodyFont, Color(58, 58, 58), addTopSpacing = lineGap) }

        val allAbilities = (job.abilities + job.extraAbilities).distinctBy { it.name }
        lines += LineSpec("능력 정보", headingFont, Color(45, 45, 45), addTopSpacing = sectionGap)
        if (allAbilities.isEmpty()) {
            lines += LineSpec("등록된 능력 정보가 없습니다.", bodyFont, Color(58, 58, 58), addTopSpacing = lineGap)
        } else {
            allAbilities.forEach { ability ->
                lines += LineSpec("• ${ability.name}", bodyFont.deriveFont(Font.BOLD.toFloat()), Color(36, 36, 36), addTopSpacing = lineGap)
                splitIntoLines(ability.description, bodyFont, width - sidePadding * 2 - 24, tempGraphics)
                    .forEach { line -> lines += LineSpec("  $line", bodyFont, Color(58, 58, 58), addTopSpacing = 6) }
            }
        }

        val totalHeight = calculateHeight(lines, tempGraphics, topPadding = 56, bottomPadding = 56)
        tempGraphics.dispose()

        val image = BufferedImage(width, totalHeight, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics().apply {
            color = Color(248, 249, 250)
            fillRect(0, 0, width, totalHeight)
            setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        }

        var y = 56
        lines.forEach { line ->
            y += line.addTopSpacing
            g.font = line.font
            g.color = line.color
            val ascent = g.fontMetrics.ascent
            g.drawString(line.text, sidePadding, y + ascent)
            y += g.fontMetrics.height
        }

        val output = ByteArrayOutputStream()
        ImageIO.write(image, "png", output)
        g.dispose()
        return output.toByteArray()
    }

    private fun calculateHeight(lines: List<LineSpec>, graphics: Graphics2D, topPadding: Int, bottomPadding: Int): Int {
        var height = topPadding + bottomPadding
        lines.forEach { line ->
            graphics.font = line.font
            height += line.addTopSpacing
            height += graphics.fontMetrics.height
        }
        return height
    }

    private fun splitIntoLines(text: String, font: Font, maxWidth: Int, graphics: Graphics2D): List<String> {
        val normalized = text.replace("\n", " \n ")
        val words = normalized.split(Regex("\\s+"))
        if (words.isEmpty()) return listOf(text)

        graphics.font = font
        val lines = mutableListOf<String>()
        val current = StringBuilder()

        words.forEach { word ->
            if (word == "\\n") {
                lines += current.toString().trim().ifBlank { " " }
                current.clear()
                return@forEach
            }

            val candidate = if (current.isEmpty()) word else "${current} $word"
            if (graphics.fontMetrics.stringWidth(candidate) <= maxWidth) {
                current.clear()
                current.append(candidate)
            } else {
                if (current.isNotBlank()) {
                    lines += current.toString().trimEnd()
                    current.clear()
                }

                if (graphics.fontMetrics.stringWidth(word) <= maxWidth) {
                    current.append(word)
                } else {
                    lines += breakLongWord(word, maxWidth, graphics)
                }
            }
        }

        if (current.isNotBlank()) {
            lines += current.toString().trimEnd()
        }

        return lines.ifEmpty { listOf(" ") }
    }

    private fun breakLongWord(word: String, maxWidth: Int, graphics: Graphics2D): List<String> {
        val chunks = mutableListOf<String>()
        var current = StringBuilder()

        word.forEach { ch ->
            val candidate = "$current$ch"
            if (graphics.fontMetrics.stringWidth(candidate) <= maxWidth) {
                current.append(ch)
            } else {
                if (current.isNotEmpty()) {
                    chunks += current.toString()
                }
                current = StringBuilder(ch.toString())
            }
        }

        if (current.isNotEmpty()) {
            chunks += current.toString()
        }

        return chunks
    }

    private data class LineSpec(
        val text: String,
        val font: Font,
        val color: Color,
        val addTopSpacing: Int = 0
    )
}
