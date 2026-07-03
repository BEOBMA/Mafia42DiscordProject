package org.beobma.mafia42discordproject.game.replay

import org.beobma.mafia42discordproject.game.Game
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO
import kotlin.math.ceil
import kotlin.math.roundToInt

object GameReplayImageRenderer {
    private const val WIDTH = 1800
    private const val MAX_PAGE_HEIGHT = 6000
    private const val SIDE_PADDING = 56
    private const val CARD_PADDING = 34
    private const val CARD_GAP = 26
    private const val HEADER_HEIGHT = 430
    private const val PAGE_HEADER_HEIGHT = 150
    private const val BOTTOM_PADDING = 62
    private const val BODY_LINE_HEIGHT = 42
    private const val CARD_MIN_HEIGHT = 170
    private const val IMAGE_TOP_GAP = 18
    private const val IMAGE_GAP = 22
    private const val IMAGE_MAX_HEIGHT = 820
    private const val CHIP_WIDTH = 210
    private const val CHIP_HEIGHT = 38

    private val background = Color(18, 20, 23)
    private val band = Color(29, 32, 36)
    private val card = Color(41, 44, 50)
    private val border = Color(78, 83, 92)
    private val text = Color(238, 240, 243)
    private val muted = Color(174, 180, 189)
    private val accent = Color(223, 63, 63)
    private val gold = Color(235, 177, 58)
    private val chip = Color(58, 64, 74)

    private val titleFont = Font("SansSerif", Font.BOLD, 64)
    private val subtitleFont = Font("SansSerif", Font.BOLD, 34)
    private val bodyFont = Font("SansSerif", Font.PLAIN, 32)
    private val smallFont = Font("SansSerif", Font.PLAIN, 24)
    private val boldFont = Font("SansSerif", Font.BOLD, 32)
    private val urlRegex = Regex("""https?://\S+""")

    private val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.of("Asia/Seoul"))

    data class RenderedReplayImage(
        val fileName: String,
        val bytes: ByteArray
    )

    private data class RenderBlock(
        val entry: ReplayLogEntry?,
        val lines: List<String>,
        val images: List<RenderImage> = emptyList(),
        val height: Int,
        val isHeader: Boolean = false
    )

    private data class RenderImage(
        val image: BufferedImage,
        val width: Int,
        val height: Int
    )

    init {
        ImageIO.scanForPlugins()
    }

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
        val imageCache = mutableMapOf<String, BufferedImage?>()
        var lastPhaseKey: String? = null

        logs.forEach { entry ->
            val phaseKey = "${entry.dayCount}-${entry.phase.name}"
            if (entry.type == ReplayLogType.PHASE_START || phaseKey != lastPhaseKey) {
                val label = if (entry.dayCount > 0) "${entry.dayCount}일차 ${entry.phase.name}" else entry.phase.name
                blocks += RenderBlock(
                    entry = null,
                    lines = listOf(label),
                    height = 80,
                    isHeader = true
                )
                lastPhaseKey = phaseKey
            }

            val contentWidth = WIDTH - SIDE_PADDING * 2 - CARD_PADDING * 2
            val imageItems = loadReplayImages(entry.imageUrls, imageCache, contentWidth)
            val body = removeImageUrls(entry.body, entry.imageUrls)
            val bodyLines = if (body.isBlank() && imageItems.isNotEmpty()) {
                emptyList()
            } else {
                wrap(body.ifBlank { " " }, bodyFont, contentWidth, temp)
            }
            val textStartOffset = if (entry.recipients.isNotEmpty()) 178 else 138
            val textHeight = bodyLines.size * BODY_LINE_HEIGHT
            val imageTopGap = if (imageItems.isNotEmpty() && bodyLines.isNotEmpty()) IMAGE_TOP_GAP else 0
            val imageHeight = imageItems.sumOf { it.height } + (imageItems.size - 1).coerceAtLeast(0) * IMAGE_GAP
            val baseHeight = textStartOffset + textHeight + imageTopGap + imageHeight + CARD_PADDING
            blocks += RenderBlock(
                entry = entry,
                lines = bodyLines,
                images = imageItems,
                height = baseHeight.coerceAtLeast(CARD_MIN_HEIGHT)
            )
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
        g.fillRect(0, 96, WIDTH, 230)
        g.color = accent
        g.stroke = BasicStroke(5f)
        g.drawLine(0, 326, WIDTH, 326)

        g.font = titleFont
        g.color = text
        g.drawString("리플레이", SIDE_PADDING, 80)

        g.font = subtitleFont
        g.color = gold
        g.drawString(winningTeamName ?: endReason, SIDE_PADDING, 166)

        g.color = muted
        val startedAt = timeFormatter.format(Instant.ofEpochMilli(game.replayStartedAtMillis))
        g.drawString("시작: $startedAt", SIDE_PADDING, 224)
        g.drawString("인원: ${game.initialPlayerCount}명 / ${game.dayCount}일차 / 페이지 $pageNumber/$totalPages", SIDE_PADDING, 276)

        val playerSummary = game.playerDatas.joinToString("   ") { player ->
            val job = game.probationOriginalJobsByPlayer[player.member.id]?.name ?: player.job?.name ?: "?"
            val state = if (player.state.isDead) "사망" else "생존"
            "${player.member.effectiveName}($job/$state)"
        }
        g.font = smallFont
        g.color = Color(210, 214, 220)
        wrap(playerSummary, smallFont, WIDTH - SIDE_PADDING * 2, g).take(2).forEachIndexed { index, line ->
            g.drawString(line, SIDE_PADDING, 370 + index * 32)
        }
        return HEADER_HEIGHT
    }

    private fun drawPageHeader(g: Graphics2D, pageNumber: Int, totalPages: Int) {
        g.color = Color(10, 11, 13)
        g.fillRect(0, 0, WIDTH, PAGE_HEADER_HEIGHT)
        g.font = titleFont.deriveFont(46f)
        g.color = text
        g.drawString("리플레이", SIDE_PADDING, 78)
        g.font = subtitleFont
        g.color = muted
        g.drawString("페이지 $pageNumber/$totalPages", WIDTH - 300, 78)
    }

    private fun drawPhaseHeader(g: Graphics2D, label: String, y: Int) {
        g.color = Color(14, 15, 17)
        g.fillRect(0, y, WIDTH, 62)
        g.font = subtitleFont
        g.color = accent
        val x = (WIDTH - g.fontMetrics.stringWidth(label)) / 2
        g.drawString(label, x, y + 42)
    }

    private fun drawLogCard(g: Graphics2D, block: RenderBlock, y: Int) {
        val entry = block.entry ?: return
        val x = SIDE_PADDING
        val w = WIDTH - SIDE_PADDING * 2
        g.color = card
        g.fillRoundRect(x, y, w, block.height, 16, 16)
        g.color = border
        g.drawRoundRect(x, y, w, block.height, 16, 16)

        val time = timeFormatter.format(Instant.ofEpochMilli(entry.timestampMillis)).substring(11)
        g.font = smallFont
        drawChip(g, visibilityLabel(entry.visibility), x + CARD_PADDING, y + 28, visibilityColor(entry.visibility))
        drawChip(g, entry.type.name, x + CARD_PADDING + 230, y + 28, chip)

        g.color = muted
        g.drawString(time, x + w - 140, y + 58)

        g.font = boldFont
        g.color = text
        val actor = entry.actorName?.let { "$it${entry.actorJobName?.let { job -> " ($job)" } ?: ""}" }
        val title = listOfNotNull(entry.title, actor).filter(String::isNotBlank).joinToString(" - ")
        g.drawString(title.take(80), x + CARD_PADDING, y + 104)

        if (entry.recipients.isNotEmpty()) {
            g.font = smallFont
            g.color = muted
            val recipients = entry.recipients.joinToString(", ") { "${visibilityLabel(it.scope)} -> ${it.name}" }
            g.drawString(recipients.take(100), x + CARD_PADDING, y + 138)
        }

        g.font = bodyFont
        g.color = Color(238, 240, 243)
        var lineY = y + if (entry.recipients.isNotEmpty()) 178 else 138
        block.lines.forEach { line ->
            g.drawString(line, x + CARD_PADDING, lineY)
            lineY += BODY_LINE_HEIGHT
        }

        if (block.images.isNotEmpty() && block.lines.isNotEmpty()) {
            lineY += IMAGE_TOP_GAP
        }

        block.images.forEachIndexed { index, image ->
            val drawX = x + CARD_PADDING + ((w - CARD_PADDING * 2 - image.width) / 2).coerceAtLeast(0)
            g.drawImage(image.image, drawX, lineY, image.width, image.height, null)
            g.color = border
            g.drawRoundRect(drawX, lineY, image.width, image.height, 12, 12)
            if (index != block.images.lastIndex) {
                lineY += image.height + IMAGE_GAP
            }
        }
    }

    private fun drawChip(g: Graphics2D, label: String, x: Int, y: Int, color: Color) {
        g.color = color
        g.fillRoundRect(x, y, CHIP_WIDTH, CHIP_HEIGHT, 12, 12)
        g.font = smallFont
        g.color = Color.WHITE
        val visibleLabel = label.take(18)
        val drawX = x + ((CHIP_WIDTH - g.fontMetrics.stringWidth(visibleLabel)) / 2).coerceAtLeast(10)
        g.drawString(visibleLabel, drawX, y + 27)
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

    private fun loadReplayImages(
        urls: List<String>,
        cache: MutableMap<String, BufferedImage?>,
        maxWidth: Int
    ): List<RenderImage> = urls.mapNotNull { url ->
        val image = if (cache.containsKey(url)) {
            cache[url]
        } else {
            downloadImage(url).also { cache[url] = it }
        } ?: return@mapNotNull null

        val scale = minOf(
            maxWidth.toDouble() / image.width,
            IMAGE_MAX_HEIGHT.toDouble() / image.height,
            2.0
        )
        RenderImage(
            image = image,
            width = (image.width * scale).roundToInt().coerceAtLeast(1),
            height = (image.height * scale).roundToInt().coerceAtLeast(1)
        )
    }

    private fun downloadImage(url: String): BufferedImage? = runCatching {
        val connection = URI(url).toURL().openConnection()
        connection.connectTimeout = 3_000
        connection.readTimeout = 5_000
        if (connection is HttpURLConnection) {
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "Mafia42DiscordProject/ReplayRenderer")
        }

        try {
            connection.getInputStream().use { input ->
                ImageIO.read(input) ?: error("unsupported image format")
            }
        } finally {
            if (connection is HttpURLConnection) {
                connection.disconnect()
            }
        }
    }.onFailure { error ->
        println("[GameReplayImageRenderer] image load failed: $url, reason=${error.message}")
    }.getOrNull()

    private fun removeImageUrls(body: String, urls: List<String>): String {
        var sanitized = body
        urls.forEach { url ->
            sanitized = sanitized.replace(url, "")
        }
        return urlRegex.replace(sanitized, "")
            .replace(Regex("""[ \t]+\n"""), "\n")
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()
    }

    private fun Graphics2D.withHints(): Graphics2D {
        setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        return this
    }
}
