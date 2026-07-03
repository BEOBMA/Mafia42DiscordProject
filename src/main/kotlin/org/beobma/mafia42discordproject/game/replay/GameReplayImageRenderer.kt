package org.beobma.mafia42discordproject.game.replay

import org.beobma.mafia42discordproject.game.Game
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO
import kotlin.math.ceil

object GameReplayImageRenderer {
    private const val WIDTH = 900
    private const val MAX_PAGE_HEIGHT = 9000
    private const val SIDE_PADDING = 28
    private const val CARD_PADDING = 18
    private const val CARD_GAP = 14
    private const val HEADER_HEIGHT = 250
    private const val PAGE_HEADER_HEIGHT = 86
    private const val BOTTOM_PADDING = 34

    private val background = Color(18, 20, 23)
    private val band = Color(29, 32, 36)
    private val card = Color(41, 44, 50)
    private val border = Color(78, 83, 92)
    private val text = Color(238, 240, 243)
    private val muted = Color(174, 180, 189)
    private val accent = Color(223, 63, 63)
    private val gold = Color(235, 177, 58)
    private val chip = Color(58, 64, 74)

    private val titleFont = Font("SansSerif", Font.BOLD, 36)
    private val subtitleFont = Font("SansSerif", Font.BOLD, 20)
    private val bodyFont = Font("SansSerif", Font.PLAIN, 18)
    private val smallFont = Font("SansSerif", Font.PLAIN, 14)
    private val boldFont = Font("SansSerif", Font.BOLD, 18)

    private val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.of("Asia/Seoul"))

    data class RenderedReplayImage(
        val fileName: String,
        val bytes: ByteArray
    )

    private data class RenderBlock(
        val entry: ReplayLogEntry?,
        val lines: List<String>,
        val height: Int,
        val isHeader: Boolean = false
    )

    fun render(game: Game, endReason: String, winningTeamName: String?): List<RenderedReplayImage> {
        val snapshotLogs = synchronized(game) { game.replayLogs.toList().sortedBy { it.sequence } }
        val blocks = buildBlocks(snapshotLogs)
        val pages = paginate(blocks, firstPageReservedHeight = HEADER_HEIGHT, nextPageReservedHeight = PAGE_HEADER_HEIGHT)
        val totalPages = pages.size.coerceAtLeast(1)

        return pages.mapIndexed { index, page ->
            val bytes = renderPage(
                game = game,
                pageBlocks = page,
                pageNumber = index + 1,
                totalPages = totalPages,
                endReason = endReason,
                winningTeamName = winningTeamName
            )
            RenderedReplayImage("game-replay-${index + 1}.png", bytes)
        }.ifEmpty {
            listOf(
                RenderedReplayImage(
                    "game-replay-1.png",
                    renderPage(game, emptyList(), 1, 1, endReason, winningTeamName)
                )
            )
        }
    }

    private fun buildBlocks(logs: List<ReplayLogEntry>): List<RenderBlock> {
        val temp = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics().withHints()
        val blocks = mutableListOf<RenderBlock>()
        var lastPhaseKey: String? = null

        logs.forEach { entry ->
            val phaseKey = "${entry.dayCount}-${entry.phase.name}"
            if (entry.type == ReplayLogType.PHASE_START || phaseKey != lastPhaseKey) {
                val label = if (entry.dayCount > 0) "${entry.dayCount}일차 ${entry.phase.name}" else entry.phase.name
                blocks += RenderBlock(
                    entry = null,
                    lines = listOf(label),
                    height = 54,
                    isHeader = true
                )
                lastPhaseKey = phaseKey
            }

            val bodyLines = wrap(entry.body.ifBlank { " " }, bodyFont, WIDTH - SIDE_PADDING * 2 - CARD_PADDING * 2, temp)
            val extraLines = entry.imageUrls.map { "[이미지] $it" }
            val lineCount = bodyLines.size + extraLines.size
            val baseHeight = 78 + lineCount * 24 + if (entry.recipients.isNotEmpty()) 22 else 0
            blocks += RenderBlock(entry, bodyLines + extraLines, baseHeight.coerceAtLeast(92))
        }

        temp.dispose()
        return blocks
    }

    private fun paginate(
        blocks: List<RenderBlock>,
        firstPageReservedHeight: Int,
        nextPageReservedHeight: Int
    ): List<List<RenderBlock>> {
        val pages = mutableListOf<MutableList<RenderBlock>>()
        var current = mutableListOf<RenderBlock>()
        var used = firstPageReservedHeight + BOTTOM_PADDING

        blocks.forEach { block ->
            val blockHeight = block.height + CARD_GAP
            val limit = MAX_PAGE_HEIGHT
            if (current.isNotEmpty() && used + blockHeight > limit) {
                pages += current
                current = mutableListOf()
                used = nextPageReservedHeight + BOTTOM_PADDING
            }
            current += block
            used += blockHeight
        }

        if (current.isNotEmpty()) pages += current
        return pages
    }

    private fun renderPage(
        game: Game,
        pageBlocks: List<RenderBlock>,
        pageNumber: Int,
        totalPages: Int,
        endReason: String,
        winningTeamName: String?
    ): ByteArray {
        val contentHeight = pageBlocks.sumOf { it.height + CARD_GAP }
        val reservedHeight = if (pageNumber == 1) HEADER_HEIGHT else PAGE_HEADER_HEIGHT
        val height = (reservedHeight + contentHeight + BOTTOM_PADDING).coerceAtLeast(620)

        val image = BufferedImage(WIDTH, height, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics().withHints()
        g.color = background
        g.fillRect(0, 0, WIDTH, height)

        var y = if (pageNumber == 1) {
            drawMainHeader(g, game, endReason, winningTeamName, pageNumber, totalPages)
            HEADER_HEIGHT
        } else {
            drawPageHeader(g, pageNumber, totalPages)
            PAGE_HEADER_HEIGHT
        }

        pageBlocks.forEach { block ->
            if (block.isHeader) {
                drawPhaseHeader(g, block.lines.first(), y)
            } else {
                drawLogCard(g, block, y)
            }
            y += block.height + CARD_GAP
        }

        g.dispose()
        val output = ByteArrayOutputStream()
        ImageIO.write(image, "png", output)
        return output.toByteArray()
    }

    private fun drawMainHeader(
        g: Graphics2D,
        game: Game,
        endReason: String,
        winningTeamName: String?,
        pageNumber: Int,
        totalPages: Int
    ): Int {
        g.color = Color(10, 11, 13)
        g.fillRect(0, 0, WIDTH, HEADER_HEIGHT)
        g.color = band
        g.fillRect(0, 54, WIDTH, 128)
        g.color = accent
        g.stroke = BasicStroke(3f)
        g.drawLine(0, 182, WIDTH, 182)

        g.font = titleFont
        g.color = text
        g.drawString("리플레이", SIDE_PADDING, 46)

        g.font = subtitleFont
        g.color = gold
        g.drawString(winningTeamName ?: endReason, SIDE_PADDING, 98)

        g.color = muted
        val startedAt = timeFormatter.format(Instant.ofEpochMilli(game.replayStartedAtMillis))
        g.drawString("시작: $startedAt", SIDE_PADDING, 132)
        g.drawString("인원: ${game.initialPlayerCount}명 / ${game.dayCount}일차 / 페이지 $pageNumber/$totalPages", SIDE_PADDING, 160)

        val playerSummary = game.playerDatas.joinToString("   ") { player ->
            val job = game.probationOriginalJobsByPlayer[player.member.id]?.name ?: player.job?.name ?: "?"
            val state = if (player.state.isDead) "사망" else "생존"
            "${player.member.effectiveName}($job/$state)"
        }
        g.font = smallFont
        g.color = Color(210, 214, 220)
        wrap(playerSummary, smallFont, WIDTH - SIDE_PADDING * 2, g).take(2).forEachIndexed { index, line ->
            g.drawString(line, SIDE_PADDING, 212 + index * 20)
        }
        return HEADER_HEIGHT
    }

    private fun drawPageHeader(g: Graphics2D, pageNumber: Int, totalPages: Int) {
        g.color = Color(10, 11, 13)
        g.fillRect(0, 0, WIDTH, PAGE_HEADER_HEIGHT)
        g.font = titleFont.deriveFont(26f)
        g.color = text
        g.drawString("리플레이", SIDE_PADDING, 42)
        g.font = subtitleFont
        g.color = muted
        g.drawString("페이지 $pageNumber/$totalPages", WIDTH - 150, 42)
    }

    private fun drawPhaseHeader(g: Graphics2D, label: String, y: Int) {
        g.color = Color(14, 15, 17)
        g.fillRect(0, y, WIDTH, 42)
        g.font = subtitleFont
        g.color = accent
        val x = (WIDTH - g.fontMetrics.stringWidth(label)) / 2
        g.drawString(label, x, y + 28)
    }

    private fun drawLogCard(g: Graphics2D, block: RenderBlock, y: Int) {
        val entry = block.entry ?: return
        val x = SIDE_PADDING
        val w = WIDTH - SIDE_PADDING * 2
        g.color = card
        g.fillRoundRect(x, y, w, block.height, 8, 8)
        g.color = border
        g.drawRoundRect(x, y, w, block.height, 8, 8)

        val time = timeFormatter.format(Instant.ofEpochMilli(entry.timestampMillis)).substring(11)
        g.font = smallFont
        drawChip(g, visibilityLabel(entry.visibility), x + CARD_PADDING, y + 18, visibilityColor(entry.visibility))
        drawChip(g, entry.type.name, x + CARD_PADDING + 122, y + 18, chip)

        g.color = muted
        g.drawString(time, x + w - 76, y + 34)

        g.font = boldFont
        g.color = text
        val actor = entry.actorName?.let { "$it${entry.actorJobName?.let { job -> " ($job)" } ?: ""}" }
        val title = listOfNotNull(entry.title, actor).filter(String::isNotBlank).joinToString(" - ")
        g.drawString(title.take(80), x + CARD_PADDING, y + 58)

        if (entry.recipients.isNotEmpty()) {
            g.font = smallFont
            g.color = muted
            val recipients = entry.recipients.joinToString(", ") { "${visibilityLabel(it.scope)} -> ${it.name}" }
            g.drawString(recipients.take(100), x + CARD_PADDING, y + 80)
        }

        g.font = bodyFont
        g.color = Color(238, 240, 243)
        var lineY = y + if (entry.recipients.isNotEmpty()) 106 else 84
        block.lines.forEach { line ->
            g.drawString(line, x + CARD_PADDING, lineY)
            lineY += 24
        }
    }

    private fun drawChip(g: Graphics2D, label: String, x: Int, y: Int, color: Color) {
        g.color = color
        g.fillRoundRect(x, y, 110, 24, 8, 8)
        g.font = smallFont
        g.color = Color.WHITE
        val drawX = x + ((110 - g.fontMetrics.stringWidth(label.take(14))) / 2).coerceAtLeast(6)
        g.drawString(label.take(14), drawX, y + 17)
    }

    private fun visibilityLabel(visibility: ReplayVisibility): String = when (visibility) {
        ReplayVisibility.PUBLIC -> "공개"
        ReplayVisibility.MAFIA_CHANNEL -> "마피아"
        ReplayVisibility.COUPLE_CHANNEL -> "연인"
        ReplayVisibility.DEAD_CHANNEL -> "사망자"
        ReplayVisibility.DIRECT_MESSAGE -> "DM"
        ReplayVisibility.EPHEMERAL -> "개인응답"
        ReplayVisibility.SYSTEM_INTERNAL -> "시스템"
    }

    private fun visibilityColor(visibility: ReplayVisibility): Color = when (visibility) {
        ReplayVisibility.PUBLIC -> Color(69, 116, 196)
        ReplayVisibility.MAFIA_CHANNEL -> Color(168, 47, 47)
        ReplayVisibility.COUPLE_CHANNEL -> Color(171, 83, 143)
        ReplayVisibility.DEAD_CHANNEL -> Color(90, 95, 105)
        ReplayVisibility.DIRECT_MESSAGE -> Color(59, 134, 105)
        ReplayVisibility.EPHEMERAL -> Color(108, 91, 180)
        ReplayVisibility.SYSTEM_INTERNAL -> Color(181, 137, 47)
    }

    private fun wrap(textValue: String, font: Font, maxWidth: Int, g: Graphics2D): List<String> {
        g.font = font
        val lines = mutableListOf<String>()

        textValue.replace("\r", "").split("\n").forEach { paragraph ->
            val words = paragraph.trim().split(Regex("\\s+")).filter(String::isNotBlank)
            if (words.isEmpty()) {
                lines += " "
                return@forEach
            }

            var current = StringBuilder()
            words.forEach { word ->
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (g.fontMetrics.stringWidth(candidate) <= maxWidth) {
                    current = StringBuilder(candidate)
                } else {
                    if (current.isNotBlank()) lines += current.toString()
                    current = StringBuilder()
                    if (g.fontMetrics.stringWidth(word) <= maxWidth) {
                        current.append(word)
                    } else {
                        lines += breakLongWord(word, maxWidth, g)
                    }
                }
            }
            if (current.isNotBlank()) lines += current.toString()
        }

        return lines.ifEmpty { listOf(" ") }
    }

    private fun breakLongWord(word: String, maxWidth: Int, g: Graphics2D): List<String> {
        val charsPerChunk = ceil(word.length / (g.fontMetrics.stringWidth(word).toDouble() / maxWidth)).toInt().coerceAtLeast(1)
        return word.chunked(charsPerChunk)
    }

    private fun Graphics2D.withHints(): Graphics2D {
        setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        return this
    }
}
