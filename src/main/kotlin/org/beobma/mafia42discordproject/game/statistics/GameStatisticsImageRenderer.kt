package org.beobma.mafia42discordproject.game.statistics

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO

object GameStatisticsImageRenderer {
    private const val WIDTH = 1800
    private const val MAX_PAGE_HEIGHT = 5600
    private const val SIDE_PADDING = 58
    private const val HEADER_HEIGHT = 300
    private const val PAGE_HEADER_HEIGHT = 116
    private const val BOTTOM_PADDING = 54
    private const val SECTION_HEIGHT = 76
    private const val TABLE_HEADER_HEIGHT = 56
    private const val ROW_HEIGHT = 58
    private const val SECTION_GAP = 28
    private const val CELL_PADDING = 18

    private val background = Color(18, 20, 23)
    private val headerBackground = Color(10, 11, 13)
    private val band = Color(29, 32, 36)
    private val tableHeader = Color(38, 43, 51)
    private val rowA = Color(46, 50, 58)
    private val rowB = Color(39, 43, 50)
    private val border = Color(78, 84, 96)
    private val text = Color(238, 240, 244)
    private val muted = Color(178, 185, 196)
    private val accent = Color(223, 63, 63)
    private val gold = Color(235, 177, 58)
    private val green = Color(78, 174, 122)

    private val titleFont = Font("SansSerif", Font.BOLD, 60)
    private val subtitleFont = Font("SansSerif", Font.BOLD, 32)
    private val sectionFont = Font("SansSerif", Font.BOLD, 34)
    private val headerFont = Font("SansSerif", Font.BOLD, 25)
    private val bodyFont = Font("SansSerif", Font.PLAIN, 25)
    private val smallFont = Font("SansSerif", Font.PLAIN, 22)

    private val json = Json { ignoreUnknownKeys = true }
    private val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.of("Asia/Seoul"))

    data class RenderedStatisticsImage(
        val fileName: String,
        val bytes: ByteArray,
    )

    private data class Column(
        val title: String,
        val weight: Int,
        val alignRight: Boolean = false,
    )

    private sealed interface RenderBlock {
        val height: Int
    }

    private data class SectionBlock(
        val title: String,
        val subtitle: String? = null,
        override val height: Int = SECTION_HEIGHT,
    ) : RenderBlock

    private data class TableHeaderBlock(
        val columns: List<Column>,
        override val height: Int = TABLE_HEADER_HEIGHT,
    ) : RenderBlock

    private data class TableRowBlock(
        val columns: List<Column>,
        val cells: List<String>,
        val index: Int,
        override val height: Int = ROW_HEIGHT,
    ) : RenderBlock

    private data class GapBlock(
        override val height: Int = SECTION_GAP,
    ) : RenderBlock

    private data class Section(
        val title: String,
        val subtitle: String? = null,
        val columns: List<Column>,
        val rows: List<List<String>>,
    )

    init {
        ImageIO.scanForPlugins()
    }

    fun render(dataPath: Path): List<RenderedStatisticsImage> {
        val root = json.parseToJsonElement(Files.readString(dataPath)) as? JsonObject
            ?: error("statistics data is not a JSON object")
        val sections = buildSections(root)
        val blocks = buildBlocks(sections)
        val pages = paginate(blocks)
        val totalPages = pages.size.coerceAtLeast(1)

        return pages.mapIndexed { index, pageBlocks ->
            RenderedStatisticsImage(
                fileName = "game-statistics-${index + 1}.png",
                bytes = renderPage(root, pageBlocks, index + 1, totalPages),
            )
        }.ifEmpty {
            listOf(RenderedStatisticsImage("game-statistics-1.png", renderPage(root, emptyList(), 1, 1)))
        }
    }

    private fun buildSections(root: JsonObject): List<Section> {
        return buildList {
            add(overviewSection(root))
            add(counterSection("승리팀 분포", root.obj("overview")?.obj("winningTeams")))
            add(counterSection("종료 사유", root.obj("overview")?.obj("endReasons")))
            add(counterSection("모드", root.obj("overview")?.obj("modes")))
            add(counterSection("인원 분포", root.obj("overview")?.obj("initialPlayerCounts")))
            add(counterSection("일차 분포", root.obj("overview")?.obj("dayCounts")))
            add(bucketSection("팀별 통계", root.obj("byTeam"), "팀"))
            add(bucketSection("직업별 통계", root.obj("byJob"), "직업"))
            add(bucketSection("표시 직업별 통계", root.obj("byDisplayedJob"), "표시 직업"))
            add(bucketSection("능력별 통계", root.obj("byAbility"), "능력"))
            add(playerSection(root.obj("byPlayer")))
            add(jobAbilitySection(root.obj("byJobAbility")))
            add(playerJobSection(root.obj("byPlayerJob")))
            add(playerAbilitySection(root.obj("byPlayerAbility")))

            val usage = root.obj("abilityUsage")
            add(usageSection("능력 사용 통계", usage?.obj("byAbility"), "능력"))
            add(usageJobSection(usage?.obj("byAbilityAndJob")))
            add(usagePlayerSection(usage?.obj("byAbilityAndPlayer")))
        }
    }

    private fun buildBlocks(sections: List<Section>): List<RenderBlock> {
        val blocks = mutableListOf<RenderBlock>()
        sections.forEachIndexed { sectionIndex, section ->
            if (sectionIndex > 0) {
                blocks += GapBlock()
            }
            blocks += SectionBlock(section.title, section.subtitle)
            blocks += TableHeaderBlock(section.columns)
            val rows = section.rows.ifEmpty {
                listOf(listOf("데이터 없음") + List(section.columns.size - 1) { "" })
            }
            rows.forEachIndexed { rowIndex, row ->
                blocks += TableRowBlock(section.columns, row, rowIndex)
            }
        }
        return blocks
    }

    private fun paginate(blocks: List<RenderBlock>): List<List<RenderBlock>> {
        val pages = mutableListOf<MutableList<RenderBlock>>()
        var current = mutableListOf<RenderBlock>()
        var used = HEADER_HEIGHT + BOTTOM_PADDING

        blocks.forEach { block ->
            val reservedHeight = if (pages.isEmpty()) HEADER_HEIGHT else PAGE_HEADER_HEIGHT
            if (current.isNotEmpty() && used + block.height > MAX_PAGE_HEIGHT) {
                pages += current
                current = mutableListOf()
                used = PAGE_HEADER_HEIGHT + BOTTOM_PADDING
            } else if (current.isEmpty()) {
                used = reservedHeight + BOTTOM_PADDING
            }
            current += block
            used += block.height
        }

        if (current.isNotEmpty()) {
            pages += current
        }
        return pages
    }

    private fun renderPage(root: JsonObject, blocks: List<RenderBlock>, pageNumber: Int, totalPages: Int): ByteArray {
        val reservedHeight = if (pageNumber == 1) HEADER_HEIGHT else PAGE_HEADER_HEIGHT
        val height = (reservedHeight + blocks.sumOf { it.height } + BOTTOM_PADDING).coerceAtLeast(720)
        val image = BufferedImage(WIDTH, height, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics().withHints()
        g.color = background
        g.fillRect(0, 0, WIDTH, height)

        var y = if (pageNumber == 1) {
            drawMainHeader(g, root, pageNumber, totalPages)
            HEADER_HEIGHT
        } else {
            drawPageHeader(g, pageNumber, totalPages)
            PAGE_HEADER_HEIGHT
        }

        blocks.forEach { block ->
            when (block) {
                is GapBlock -> Unit
                is SectionBlock -> drawSection(g, block, y)
                is TableHeaderBlock -> drawTableHeader(g, block, y)
                is TableRowBlock -> drawTableRow(g, block, y)
            }
            y += block.height
        }

        g.dispose()
        val output = ByteArrayOutputStream()
        ImageIO.write(image, "png", output)
        return output.toByteArray()
    }

    private fun drawMainHeader(g: Graphics2D, root: JsonObject, pageNumber: Int, totalPages: Int): Int {
        g.color = headerBackground
        g.fillRect(0, 0, WIDTH, HEADER_HEIGHT)
        g.color = band
        g.fillRect(0, 96, WIDTH, 150)
        g.color = accent
        g.stroke = BasicStroke(5f)
        g.drawLine(0, 246, WIDTH, 246)

        g.font = titleFont
        g.color = text
        g.drawString("게임 통계 리포트", SIDE_PADDING, 74)

        val overview = root.obj("overview")
        g.font = subtitleFont
        g.color = gold
        g.drawString("총 ${overview?.int("totalGames") ?: 0}게임 / 참가 기록 ${overview?.int("totalPlayerEntries") ?: 0}건", SIDE_PADDING, 156)

        g.color = muted
        val generatedAt = root.string("generatedAt")?.let { formatInstant(it) } ?: "알 수 없음"
        g.drawString("생성: $generatedAt", SIDE_PADDING, 206)
        g.drawString("페이지 $pageNumber/$totalPages", WIDTH - 280, 74)

        g.font = smallFont
        g.color = Color(212, 217, 225)
        val source = root.string("sourceArchiveDirectory") ?: "data/game-archives"
        val processed = root.int("processedArchiveCount") ?: 0
        val newCount = root.int("newArchiveCount") ?: 0
        g.drawString("원본: $source / 반영된 고유 게임 ${processed}개 / 새로 반영 ${newCount}개", SIDE_PADDING, 282)
        return HEADER_HEIGHT
    }

    private fun drawPageHeader(g: Graphics2D, pageNumber: Int, totalPages: Int) {
        g.color = headerBackground
        g.fillRect(0, 0, WIDTH, PAGE_HEADER_HEIGHT)
        g.font = titleFont.deriveFont(42f)
        g.color = text
        g.drawString("게임 통계 리포트", SIDE_PADDING, 68)
        g.font = subtitleFont
        g.color = muted
        g.drawString("페이지 $pageNumber/$totalPages", WIDTH - 280, 68)
    }

    private fun drawSection(g: Graphics2D, block: SectionBlock, y: Int) {
        g.color = Color(14, 15, 17)
        g.fillRect(0, y, WIDTH, block.height)
        g.font = sectionFont
        g.color = accent
        g.drawString(block.title, SIDE_PADDING, y + 48)
        block.subtitle?.let {
            g.font = smallFont
            g.color = muted
            g.drawString(it, SIDE_PADDING + 360, y + 48)
        }
    }

    private fun drawTableHeader(g: Graphics2D, block: TableHeaderBlock, y: Int) {
        val x = SIDE_PADDING
        val width = WIDTH - SIDE_PADDING * 2
        g.color = tableHeader
        g.fillRoundRect(x, y, width, block.height, 12, 12)
        g.color = border
        g.drawRoundRect(x, y, width, block.height, 12, 12)

        g.font = headerFont
        g.color = Color.WHITE
        drawCells(g, block.columns, block.columns.map { it.title }, y, block.height)
    }

    private fun drawTableRow(g: Graphics2D, block: TableRowBlock, y: Int) {
        val x = SIDE_PADDING
        val width = WIDTH - SIDE_PADDING * 2
        g.color = if (block.index % 2 == 0) rowA else rowB
        g.fillRect(x, y, width, block.height)
        g.color = border
        g.drawLine(x, y + block.height, x + width, y + block.height)

        g.font = bodyFont
        g.color = text
        drawCells(g, block.columns, block.cells, y, block.height)
    }

    private fun drawCells(g: Graphics2D, columns: List<Column>, cells: List<String>, y: Int, height: Int) {
        val tableX = SIDE_PADDING
        val tableWidth = WIDTH - SIDE_PADDING * 2
        val widths = columnWidths(columns, tableWidth)
        var x = tableX
        val baseline = y + (height + g.fontMetrics.ascent - g.fontMetrics.descent) / 2

        columns.forEachIndexed { index, column ->
            val cellWidth = widths[index]
            val rawText = cells.getOrNull(index).orEmpty()
            val fitted = fitText(g, rawText, cellWidth - CELL_PADDING * 2)
            val textWidth = g.fontMetrics.stringWidth(fitted)
            val drawX = if (column.alignRight) {
                x + cellWidth - CELL_PADDING - textWidth
            } else {
                x + CELL_PADDING
            }
            g.drawString(fitted, drawX, baseline)
            g.color = Color(64, 70, 80)
            g.drawLine(x + cellWidth, y + 10, x + cellWidth, y + height - 10)
            g.color = text
            x += cellWidth
        }
    }

    private fun columnWidths(columns: List<Column>, totalWidth: Int): List<Int> {
        val totalWeight = columns.sumOf { it.weight }.coerceAtLeast(1)
        var used = 0
        return columns.mapIndexed { index, column ->
            if (index == columns.lastIndex) {
                totalWidth - used
            } else {
                (totalWidth * column.weight / totalWeight).also { used += it }
            }
        }
    }

    private fun overviewSection(root: JsonObject): Section {
        val overview = root.obj("overview")
        val rows = listOf(
            listOf("전체 게임", "${overview?.int("totalGames") ?: 0}", "게임"),
            listOf("전체 참가 기록", "${overview?.int("totalPlayerEntries") ?: 0}", "건"),
            listOf("평균 진행 일차", formatNumber(overview?.double("averageDayCount")), "일"),
            listOf("평균 시작 인원", formatNumber(overview?.double("averageInitialPlayerCount")), "명"),
            listOf("아카이브 파일", "${root.int("sourceArchiveFileCount") ?: 0}", "개"),
            listOf("중복 제외 파일", "${root.int("duplicateArchiveFileCount") ?: 0}", "개"),
        )
        return Section(
            title = "개요",
            columns = listOf(Column("항목", 5), Column("값", 2, alignRight = true), Column("단위", 2)),
            rows = rows,
        )
    }

    private fun counterSection(title: String, counter: JsonObject?): Section {
        val total = counter?.values?.sumOf { it.jsonPrimitive.intOrNull ?: 0 } ?: 0
        val rows = counter.entriesSortedByCount().map { (key, value) ->
            val count = value.jsonPrimitive.intOrNull ?: 0
            listOf(key, count.toString(), "${percent(count, total)}%")
        }
        return Section(
            title = title,
            columns = listOf(Column("항목", 6), Column("건수", 2, alignRight = true), Column("비율", 2, alignRight = true)),
            rows = rows,
        )
    }

    private fun bucketSection(title: String, data: JsonObject?, nameColumn: String): Section {
        return Section(
            title = title,
            columns = bucketColumns(nameColumn),
            rows = data.bucketRows { key, _ -> listOf(key) },
        )
    }

    private fun playerSection(data: JsonObject?): Section {
        val rows = data.entriesSortedByGames().map { (playerId, value) ->
            val obj = value as? JsonObject
            val name = obj?.string("name") ?: playerId
            listOf(
                name,
                obj.games(),
                obj.wins(),
                obj.losses(),
                obj.winRate(),
                obj.survivalRate(),
                summarizeNestedNames(obj?.obj("jobs")),
                summarizeNestedNames(obj?.obj("abilities")),
            )
        }
        return Section(
            title = "참가자별 통계",
            columns = listOf(
                Column("참가자", 4),
                Column("게임", 1, true),
                Column("승", 1, true),
                Column("패", 1, true),
                Column("승률", 1, true),
                Column("생존률", 1, true),
                Column("주요 직업", 3),
                Column("주요 능력", 3),
            ),
            rows = rows,
        )
    }

    private fun jobAbilitySection(data: JsonObject?): Section {
        val rows = data.entriesSortedByGames().mapNotNull { (_, value) ->
            val obj = value as? JsonObject ?: return@mapNotNull null
            listOf(
                obj.string("job") ?: "UNKNOWN",
                obj.string("ability") ?: "UNKNOWN",
            ) + bucketValues(obj)
        }
        return Section(
            title = "직업 + 능력 조합",
            columns = listOf(Column("직업", 3), Column("능력", 4)) + statColumns(),
            rows = rows,
        )
    }

    private fun playerJobSection(data: JsonObject?): Section {
        val rows = data.entriesSortedByGames().mapNotNull { (_, value) ->
            val obj = value as? JsonObject ?: return@mapNotNull null
            listOf(
                obj.string("playerName") ?: "UNKNOWN",
                obj.string("job") ?: "UNKNOWN",
            ) + bucketValues(obj)
        }
        return Section(
            title = "참가자 + 직업 통계",
            columns = listOf(Column("참가자", 4), Column("직업", 3)) + statColumns(),
            rows = rows,
        )
    }

    private fun playerAbilitySection(data: JsonObject?): Section {
        val rows = data.entriesSortedByGames().mapNotNull { (_, value) ->
            val obj = value as? JsonObject ?: return@mapNotNull null
            listOf(
                obj.string("playerName") ?: "UNKNOWN",
                obj.string("ability") ?: "UNKNOWN",
            ) + bucketValues(obj)
        }
        return Section(
            title = "참가자 + 능력 통계",
            columns = listOf(Column("참가자", 4), Column("능력", 4)) + statColumns(),
            rows = rows,
        )
    }

    private fun usageSection(title: String, data: JsonObject?, nameColumn: String): Section {
        val rows = data.entriesSortedByUses().mapNotNull { (key, value) ->
            val obj = value as? JsonObject ?: return@mapNotNull null
            listOf(key) + usageValues(obj)
        }
        return Section(
            title = title,
            columns = usageColumns(nameColumn),
            rows = rows,
        )
    }

    private fun usageJobSection(data: JsonObject?): Section {
        val rows = data.entriesSortedByUses().mapNotNull { (_, value) ->
            val obj = value as? JsonObject ?: return@mapNotNull null
            listOf(
                obj.string("ability") ?: "UNKNOWN",
                obj.string("job") ?: "UNKNOWN",
            ) + usageValues(obj)
        }
        return Section(
            title = "능력 사용: 능력 + 직업",
            columns = listOf(Column("능력", 4), Column("직업", 3)) + usageStatColumns(),
            rows = rows,
        )
    }

    private fun usagePlayerSection(data: JsonObject?): Section {
        val rows = data.entriesSortedByUses().mapNotNull { (_, value) ->
            val obj = value as? JsonObject ?: return@mapNotNull null
            listOf(
                obj.string("playerName") ?: "UNKNOWN",
                obj.string("ability") ?: "UNKNOWN",
            ) + usageValues(obj)
        }
        return Section(
            title = "능력 사용: 참가자 + 능력",
            columns = listOf(Column("참가자", 4), Column("능력", 4)) + usageStatColumns(),
            rows = rows,
        )
    }

    private fun bucketColumns(nameColumn: String): List<Column> = listOf(Column(nameColumn, 5)) + statColumns()

    private fun statColumns(): List<Column> = listOf(
        Column("게임", 1, true),
        Column("승", 1, true),
        Column("패", 1, true),
        Column("무효", 1, true),
        Column("승률", 1, true),
        Column("생존률", 1, true),
    )

    private fun usageColumns(nameColumn: String): List<Column> = listOf(Column(nameColumn, 5)) + usageStatColumns()

    private fun usageStatColumns(): List<Column> = listOf(
        Column("사용", 1, true),
        Column("성공", 1, true),
        Column("실패", 1, true),
        Column("미확인", 1, true),
        Column("성공률", 1, true),
        Column("결과", 4),
    )

    private fun JsonObject?.bucketRows(prefix: (String, JsonObject) -> List<String>): List<List<String>> {
        return entriesSortedByGames().mapNotNull { (key, value) ->
            val obj = value as? JsonObject ?: return@mapNotNull null
            prefix(key, obj) + bucketValues(obj)
        }
    }

    private fun bucketValues(obj: JsonObject): List<String> = listOf(
        obj.games(),
        obj.wins(),
        obj.losses(),
        obj.noContest(),
        obj.winRate(),
        obj.survivalRate(),
    )

    private fun usageValues(obj: JsonObject): List<String> = listOf(
        "${obj.int("uses") ?: 0}",
        "${obj.int("successes") ?: 0}",
        "${obj.int("failures") ?: 0}",
        "${obj.int("unknownResults") ?: 0}",
        "${formatNumber(obj.double("successRate"))}%",
        summarizeCounter(obj.obj("results")),
    )

    private fun summarizeNestedNames(obj: JsonObject?, limit: Int = 3): String {
        return obj.entriesSortedByGames()
            .take(limit)
            .joinToString(", ") { (key, value) ->
                val games = (value as? JsonObject)?.int("games") ?: 0
                "$key $games"
            }
            .ifBlank { "-" }
    }

    private fun summarizeCounter(obj: JsonObject?, limit: Int = 3): String {
        return obj.entriesSortedByCount()
            .take(limit)
            .joinToString(", ") { (key, value) -> "$key ${value.jsonPrimitive.intOrNull ?: 0}" }
            .ifBlank { "-" }
    }

    private fun JsonObject?.entriesSortedByGames(): List<Map.Entry<String, kotlinx.serialization.json.JsonElement>> {
        return this?.entries
            ?.sortedWith(compareByDescending<Map.Entry<String, kotlinx.serialization.json.JsonElement>> {
                (it.value as? JsonObject)?.int("games") ?: 0
            }.thenBy { it.key })
            .orEmpty()
    }

    private fun JsonObject?.entriesSortedByUses(): List<Map.Entry<String, kotlinx.serialization.json.JsonElement>> {
        return this?.entries
            ?.sortedWith(compareByDescending<Map.Entry<String, kotlinx.serialization.json.JsonElement>> {
                (it.value as? JsonObject)?.int("uses") ?: 0
            }.thenBy { it.key })
            .orEmpty()
    }

    private fun JsonObject?.entriesSortedByCount(): List<Map.Entry<String, kotlinx.serialization.json.JsonElement>> {
        return this?.entries
            ?.sortedWith(compareByDescending<Map.Entry<String, kotlinx.serialization.json.JsonElement>> {
                it.value.jsonPrimitive.intOrNull ?: 0
            }.thenBy { it.key })
            .orEmpty()
    }

    private fun JsonObject?.games(): String = "${this?.int("games") ?: 0}"

    private fun JsonObject?.wins(): String = "${this?.int("wins") ?: 0}"

    private fun JsonObject?.losses(): String = "${this?.int("losses") ?: 0}"

    private fun JsonObject?.noContest(): String = "${this?.int("noContest") ?: 0}"

    private fun JsonObject?.winRate(): String = "${formatNumber(this?.double("winRate"))}%"

    private fun JsonObject?.survivalRate(): String = "${formatNumber(this?.double("survivalRate"))}%"

    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.int(key: String): Int? = this[key]?.jsonPrimitive?.intOrNull

    private fun JsonObject.double(key: String): Double? = this[key]?.jsonPrimitive?.doubleOrNull

    private fun percent(count: Int, total: Int): Double {
        if (total <= 0) return 0.0
        return kotlin.math.round(count * 10000.0 / total) / 100.0
    }

    private fun formatNumber(value: Double?): String {
        val number = value ?: 0.0
        return if (number % 1.0 == 0.0) {
            number.toInt().toString()
        } else {
            String.format("%.2f", number)
        }
    }

    private fun formatInstant(value: String): String {
        return runCatching { timeFormatter.format(Instant.parse(value)) }.getOrDefault(value)
    }

    private fun fitText(g: Graphics2D, value: String, maxWidth: Int): String {
        if (g.fontMetrics.stringWidth(value) <= maxWidth) {
            return value
        }

        val suffix = "..."
        var end = value.length
        while (end > 0) {
            val candidate = value.take(end) + suffix
            if (g.fontMetrics.stringWidth(candidate) <= maxWidth) {
                return candidate
            }
            end--
        }
        return suffix
    }

    private fun Graphics2D.withHints(): Graphics2D {
        setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        return this
    }
}
