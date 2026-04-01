package com.aki.rentledger

import android.content.Context
import android.net.Uri
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.charset.StandardCharsets
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory

private const val XLSX_MIME_TYPE =
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

private const val BASE_SHEET_NAME = "基础资料"
private const val HISTORY_SHEET_NAME = "历史账本"
private const val COLLECTION_SHEET_NAME = "应收已收"

private enum class BaseColumn(vararg aliases: String) {
    RecordType("记录类型", "record_type"),
    ApartmentName("公寓名称", "apartment_name"),
    Selected("是否当前选中", "selected"),
    FloorNumber("楼层号", "楼层", "floor_number"),
    RoomNumber("房间号", "room_number"),
    RoomStatus("房间当前状态", "房间状态", "status"),
    WaterFee("水价", "water_fee"),
    ElectricFee("电价", "electric_fee");

    val aliasesNormalized: Set<String> = aliases.map(::normalizeToken).toSet()
}

private enum class HistoryColumn(vararg aliases: String) {
    RecordType("记录类型", "record_type"),
    ApartmentName("公寓名称", "apartment_name"),
    FloorNumber("楼层号", "楼层", "floor_number"),
    RoomNumber("房间号", "room_number"),
    Month("月份", "month"),
    MonthStatus("月份状态", "房间状态", "month_status"),
    Rent("房租", "rent"),
    WaterMeter("水表", "water_meter"),
    ElectricMeter("电表", "electric_meter");

    val aliasesNormalized: Set<String> = aliases.map(::normalizeToken).toSet()
}

private enum class CollectionColumn(vararg aliases: String) {
    Category("分类", "category"),
    ApartmentName("公寓名称", "apartment_name"),
    Month("月份", "month"),
    FloorNumber("楼层号", "楼层", "floor_number"),
    RoomNumber("房间号", "room_number"),
    RoomStatus("房间状态", "status"),
    Amount("金额", "amount");

    val aliasesNormalized: Set<String> = aliases.map(::normalizeToken).toSet()
}

private enum class RecordType(vararg aliases: String) {
    Note("说明", "note"),
    Apartment("公寓", "apartment"),
    Floor("楼层", "floor"),
    Room("房间", "room"),
    Monthly("月账单", "monthly");

    val aliasesNormalized: Set<String> = aliases.map(::normalizeToken).toSet()
}

private data class WorkbookSheet(
    val name: String,
    val rows: List<List<String>>
)

object RentLedgerXlsxTransfer {

    fun exportToUri(
        context: Context,
        uri: Uri,
        apartments: List<ApartmentUiState>,
        selectedApartmentName: String?
    ) {
        val workbookBytes = buildWorkbookBytes(
            listOf(
                WorkbookSheet(
                    name = BASE_SHEET_NAME,
                    rows = buildBaseRows(apartments, selectedApartmentName)
                ),
                WorkbookSheet(
                    name = HISTORY_SHEET_NAME,
                    rows = buildHistoryRows(apartments)
                ),
                WorkbookSheet(
                    name = COLLECTION_SHEET_NAME,
                    rows = buildCollectionRows(apartments)
                )
            )
        )

        context.contentResolver.openOutputStream(uri)?.use { output ->
            output.write(workbookBytes)
            output.flush()
        } ?: error("无法写入导出文件")
    }

    fun exportTemplateToUri(
        context: Context,
        uri: Uri
    ) {
        val workbookBytes = buildWorkbookBytes(
            listOf(
                WorkbookSheet(BASE_SHEET_NAME, buildBaseTemplateRows()),
                WorkbookSheet(HISTORY_SHEET_NAME, buildHistoryTemplateRows()),
                WorkbookSheet(COLLECTION_SHEET_NAME, buildCollectionTemplateRows())
            )
        )

        context.contentResolver.openOutputStream(uri)?.use { output ->
            output.write(workbookBytes)
            output.flush()
        } ?: error("无法写入模板文件")
    }

    fun importFromUri(
        context: Context,
        uri: Uri,
        defaultApartments: List<ApartmentUiState>
    ): PersistedAppState {
        val workbookBytes = context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes()
        } ?: error("无法读取导入文件")

        return parseWorkbookBytes(workbookBytes, defaultApartments)
    }

    fun suggestedExportFileName(): String {
        return "rent-ledger-export-${todayStamp()}.xlsx"
    }

    fun suggestedTemplateFileName(): String {
        return "rent-ledger-template-${todayStamp()}.xlsx"
    }

    internal fun exportWorkbookBytesForTest(
        apartments: List<ApartmentUiState>,
        selectedApartmentName: String?
    ): ByteArray {
        return buildWorkbookBytes(
            listOf(
                WorkbookSheet(
                    name = BASE_SHEET_NAME,
                    rows = buildBaseRows(apartments, selectedApartmentName)
                ),
                WorkbookSheet(
                    name = HISTORY_SHEET_NAME,
                    rows = buildHistoryRows(apartments)
                ),
                WorkbookSheet(
                    name = COLLECTION_SHEET_NAME,
                    rows = buildCollectionRows(apartments)
                )
            )
        )
    }

    internal fun importWorkbookBytesForTest(
        workbookBytes: ByteArray,
        defaultApartments: List<ApartmentUiState>
    ): PersistedAppState {
        return parseWorkbookBytes(workbookBytes, defaultApartments)
    }
}

private fun buildBaseRows(
    apartments: List<ApartmentUiState>,
    selectedApartmentName: String?
): List<List<String>> {
    val rows = mutableListOf(baseHeaderRow())
    apartments.forEach { apartment ->
        rows += buildBaseRow(
            recordType = RecordType.Apartment,
            apartmentName = apartment.name,
            selected = apartment.name == selectedApartmentName,
            waterFee = apartment.waterFee,
            electricFee = apartment.electricFee
        )

        apartment.floors.sorted().forEach { floorNumber ->
            rows += buildBaseRow(
                recordType = RecordType.Floor,
                apartmentName = apartment.name,
                floorNumber = floorNumber.toString()
            )

            apartment.roomsByFloor[floorNumber].orEmpty()
                .sortedBy { it.roomNumber }
                .forEach { room ->
                    rows += buildBaseRow(
                        recordType = RecordType.Room,
                        apartmentName = apartment.name,
                        floorNumber = floorNumber.toString(),
                        roomNumber = room.roomNumber.toString(),
                        roomStatus = statusLabel(room.statusForMonth(YearMonth.now()))
                    )
                }
        }
    }
    return rows
}

private fun buildHistoryRows(apartments: List<ApartmentUiState>): List<List<String>> {
    val rows = mutableListOf(historyHeaderRow())
    apartments.forEach { apartment ->
        apartment.floors.sorted().forEach { floorNumber ->
            apartment.roomsByFloor[floorNumber].orEmpty()
                .sortedBy { it.roomNumber }
                .forEach { room ->
                    val exportMonths = (room.monthlyValues.keys + room.monthlyStatuses.keys)
                        .filter { it.isNotBlank() }
                        .distinct()
                        .sorted()

                    exportMonths.forEach { monthKey ->
                        val monthlyValue = room.monthlyValues[monthKey] ?: RoomMonthlyValue()
                        val monthStatus = room.monthlyStatuses[monthKey]
                        rows += buildHistoryRow(
                            apartmentName = apartment.name,
                            floorNumber = floorNumber.toString(),
                            roomNumber = room.roomNumber.toString(),
                            month = monthKey,
                            monthStatus = monthStatus?.let(::statusLabel).orEmpty(),
                            rent = monthlyValue.rent,
                            waterMeter = monthlyValue.waterMeter,
                            electricMeter = monthlyValue.electricMeter
                        )
                    }
                }
        }
    }
    return rows
}

private fun buildCollectionRows(apartments: List<ApartmentUiState>): List<List<String>> {
    val rows = mutableListOf(collectionHeaderRow())
    apartments.forEach { apartment ->
        apartment.floors.sorted().forEach { floorNumber ->
            apartment.roomsByFloor[floorNumber].orEmpty()
                .sortedBy { it.roomNumber }
                .forEach { room ->
                    exportMonthsForSummary(room).forEach monthLoop@ { month ->
                        val roomStatus = room.statusForMonth(month)
                        if (roomStatus == RoomCardStatus.Vacant) return@monthLoop

                        val amount = calculateRoomCharge(
                            room = room,
                            month = month,
                            waterUnitPrice = apartment.waterFee.toDoubleOrNull() ?: 0.0,
                            electricUnitPrice = apartment.electricFee.toDoubleOrNull() ?: 0.0
                        )

                        rows += buildCollectionRow(
                            category = summaryCategoryLabel(roomStatus),
                            apartmentName = apartment.name,
                            month = month.toString(),
                            floorNumber = floorNumber.toString(),
                            roomNumber = room.roomNumber.toString(),
                            roomStatus = statusLabel(roomStatus),
                            amount = amount.toInt().toString()
                        )
                    }
                }
        }
    }
    return rows
}

private fun buildBaseTemplateRows(): List<List<String>> {
    return listOf(
        baseHeaderRow(),
        buildBaseRow(
            recordType = RecordType.Note,
            apartmentName = "填写说明：这一行只作说明，不会被导入",
            selected = false,
            floorNumber = "楼层记录填楼层号",
            roomNumber = "房间记录填房间号",
            roomStatus = "房间状态可填：已租且交租/未填写/已租未交租/已租拖欠/未租",
            waterFee = "公寓记录填水价",
            electricFee = "公寓记录填电价"
        ),
        buildBaseRow(
            recordType = RecordType.Apartment,
            apartmentName = "示例公寓",
            selected = true,
            waterFee = "3.00",
            electricFee = "0.80"
        ),
        buildBaseRow(
            recordType = RecordType.Floor,
            apartmentName = "示例公寓",
            floorNumber = "2"
        ),
        buildBaseRow(
            recordType = RecordType.Room,
            apartmentName = "示例公寓",
            floorNumber = "2",
            roomNumber = "201",
            roomStatus = statusLabel(RoomCardStatus.Unfilled)
        )
    )
}

private fun buildHistoryTemplateRows(): List<List<String>> {
    return listOf(
        historyHeaderRow(),
        buildHistoryRow(
            apartmentName = "填写说明：这一行只作说明，不会被导入",
            floorNumber = "楼层号",
            roomNumber = "房间号",
            month = "月份示例：2026-03",
            monthStatus = "月份状态可填：已租且交租/未填写/已租未交租/已租拖欠/未租",
            rent = "房租填这里",
            waterMeter = "水表填这里",
            electricMeter = "电表填这里",
            recordType = RecordType.Note
        ),
        buildHistoryRow(
            apartmentName = "示例公寓",
            floorNumber = "2",
            roomNumber = "201",
            month = "2026-03",
            monthStatus = statusLabel(RoomCardStatus.Unfilled),
            rent = "500",
            waterMeter = "100",
            electricMeter = "200"
        )
    )
}

private fun buildCollectionTemplateRows(): List<List<String>> {
    return listOf(
        collectionHeaderRow(),
        buildCollectionRow(
            category = "查看说明",
            apartmentName = "这个工作表用于查看应收/已收/拖欠汇总，不参与导入",
            month = "",
            floorNumber = "",
            roomNumber = "",
            roomStatus = "",
            amount = ""
        )
    )
}

private fun baseHeaderRow(): List<String> = listOf(
    "记录类型",
    "公寓名称",
    "是否当前选中",
    "楼层号",
    "房间号",
    "房间当前状态",
    "水价",
    "电价"
)

private fun historyHeaderRow(): List<String> = listOf(
    "记录类型",
    "公寓名称",
    "楼层号",
    "房间号",
    "月份",
    "月份状态",
    "房租",
    "水表",
    "电表"
)

private fun collectionHeaderRow(): List<String> = listOf(
    "分类",
    "公寓名称",
    "月份",
    "楼层号",
    "房间号",
    "房间状态",
    "金额"
)

private fun buildBaseRow(
    recordType: RecordType,
    apartmentName: String,
    selected: Boolean = false,
    floorNumber: String = "",
    roomNumber: String = "",
    roomStatus: String = "",
    waterFee: String = "",
    electricFee: String = ""
): List<String> {
    return listOf(
        recordType.label(),
        apartmentName,
        if (selected) "是" else "",
        floorNumber,
        roomNumber,
        roomStatus,
        waterFee,
        electricFee
    )
}

private fun buildHistoryRow(
    apartmentName: String,
    floorNumber: String,
    roomNumber: String,
    month: String,
    monthStatus: String,
    rent: String,
    waterMeter: String,
    electricMeter: String,
    recordType: RecordType = RecordType.Monthly
): List<String> {
    return listOf(
        recordType.label(),
        apartmentName,
        floorNumber,
        roomNumber,
        month,
        monthStatus,
        rent,
        waterMeter,
        electricMeter
    )
}

private fun buildCollectionRow(
    category: String,
    apartmentName: String,
    month: String,
    floorNumber: String,
    roomNumber: String,
    roomStatus: String,
    amount: String
): List<String> {
    return listOf(category, apartmentName, month, floorNumber, roomNumber, roomStatus, amount)
}

private fun buildWorkbookBytes(sheets: List<WorkbookSheet>): ByteArray {
    val output = ByteArrayOutputStream()
    ZipOutputStream(output).use { zip ->
        putZipEntry(zip, "[Content_Types].xml", contentTypesXml(sheets.size))
        putZipEntry(zip, "_rels/.rels", rootRelationshipsXml())
        putZipEntry(zip, "xl/workbook.xml", workbookXml(sheets))
        putZipEntry(zip, "xl/_rels/workbook.xml.rels", workbookRelationshipsXml(sheets.size))
        putZipEntry(zip, "xl/styles.xml", stylesXml())

        sheets.forEachIndexed { index, sheet ->
            putZipEntry(
                zip,
                "xl/worksheets/sheet${index + 1}.xml",
                worksheetXml(sheet.rows)
            )
        }
    }
    return output.toByteArray()
}

private fun putZipEntry(zip: ZipOutputStream, path: String, content: String) {
    zip.putNextEntry(ZipEntry(path))
    zip.write(content.toByteArray(StandardCharsets.UTF_8))
    zip.closeEntry()
}

private fun contentTypesXml(sheetCount: Int): String {
    val sheetOverrides = buildString {
        repeat(sheetCount) { index ->
            append(
                """<Override PartName="/xl/worksheets/sheet${index + 1}.xml" ContentType="$XLSX_WORKSHEET_CONTENT_TYPE"/>"""
            )
        }
    }

    return """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
            <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
            <Default Extension="xml" ContentType="application/xml"/>
            <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
            <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
            $sheetOverrides
        </Types>
    """.trimIndent()
}

private fun rootRelationshipsXml(): String {
    return """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
            <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
        </Relationships>
    """.trimIndent()
}

private fun workbookXml(sheets: List<WorkbookSheet>): String {
    val sheetItems = sheets.mapIndexed { index, sheet ->
        """<sheet name="${escapeXml(sheet.name)}" sheetId="${index + 1}" r:id="rId${index + 1}"/>"""
    }.joinToString("")

    return """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
            <sheets>$sheetItems</sheets>
        </workbook>
    """.trimIndent()
}

private fun workbookRelationshipsXml(sheetCount: Int): String {
    val sheetRelationships = (1..sheetCount).joinToString("") { index ->
        """<Relationship Id="rId$index" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet$index.xml"/>"""
    }
    val styleRelationship = """<Relationship Id="rId${sheetCount + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>"""

    return """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
            $sheetRelationships
            $styleRelationship
        </Relationships>
    """.trimIndent()
}

private fun stylesXml(): String {
    return """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
            <fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts>
            <fills count="2">
                <fill><patternFill patternType="none"/></fill>
                <fill><patternFill patternType="gray125"/></fill>
            </fills>
            <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
            <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
            <cellXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/></cellXfs>
            <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
        </styleSheet>
    """.trimIndent()
}

private fun worksheetXml(rows: List<List<String>>): String {
    val rowXml = rows.mapIndexed { rowIndex, row ->
        val cells = row.mapIndexed { columnIndex, value ->
            val cellReference = columnName(columnIndex) + (rowIndex + 1)
            """<c r="$cellReference" t="inlineStr"><is><t xml:space="preserve">${escapeXml(value)}</t></is></c>"""
        }.joinToString("")
        """<row r="${rowIndex + 1}">$cells</row>"""
    }.joinToString("")

    return """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
            <sheetData>$rowXml</sheetData>
        </worksheet>
    """.trimIndent()
}

private fun columnName(index: Int): String {
    var current = index
    val builder = StringBuilder()
    do {
        builder.append(('A'.code + (current % 26)).toChar())
        current = current / 26 - 1
    } while (current >= 0)
    return builder.reverse().toString()
}

private fun escapeXml(rawValue: String): String {
    return rawValue
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}

private const val XLSX_WORKSHEET_CONTENT_TYPE =
    "application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"

private fun parseWorkbookBytes(
    workbookBytes: ByteArray,
    defaultApartments: List<ApartmentUiState>
): PersistedAppState {
    val zipEntries = unzipEntries(workbookBytes)
    val sharedStrings = parseSharedStrings(zipEntries["xl/sharedStrings.xml"])
    val workbookSheets = parseWorkbookSheets(zipEntries)
    val apartmentDrafts = linkedMapOf<String, ApartmentDraft>()

    val baseSheetBytes = workbookSheets[BASE_SHEET_NAME]
    val historySheetBytes = workbookSheets[HISTORY_SHEET_NAME]

    if (baseSheetBytes != null || historySheetBytes != null) {
        if (baseSheetBytes == null) {
            error("导入文件缺少“基础资料”工作表")
        }
        if (historySheetBytes == null) {
            error("导入文件缺少“历史账本”工作表")
        }
        parseBaseSheet(baseSheetBytes, apartmentDrafts, sharedStrings)
        parseHistorySheet(historySheetBytes, apartmentDrafts, sharedStrings)
    } else {
        val legacySheetBytes = workbookSheets.values.firstOrNull()
            ?: zipEntries["xl/worksheets/sheet1.xml"]
            ?: error("导入文件里没有可识别的工作表")
        parseLegacyCombinedSheet(legacySheetBytes, apartmentDrafts, sharedStrings)
    }

    val apartments = apartmentDrafts.values.mapNotNull { it.toApartment() }
    if (apartments.isEmpty()) {
        error("导入文件里没有可用的公寓数据")
    }

    val selectedApartmentName = apartments.firstOrNull { apartmentDrafts[it.name]?.isSelected == true }?.name
        ?: apartments.firstOrNull()?.name
        ?: defaultApartments.firstOrNull()?.name

    return PersistedAppState(
        apartments = apartments,
        selectedApartmentName = selectedApartmentName
    )
}

private fun unzipEntries(bytes: ByteArray): Map<String, ByteArray> {
    val entries = linkedMapOf<String, ByteArray>()
    ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
        generateSequence { zip.nextEntry }.forEach { entry ->
            if (!entry.isDirectory) {
                entries[entry.name] = zip.readBytes()
            }
            zip.closeEntry()
        }
    }
    return entries
}

private fun parseWorkbookSheets(zipEntries: Map<String, ByteArray>): Map<String, ByteArray> {
    val workbookBytes = zipEntries["xl/workbook.xml"] ?: return emptyMap()
    val relsBytes = zipEntries["xl/_rels/workbook.xml.rels"] ?: return emptyMap()

    val workbookDocument = parseXmlDocument(workbookBytes)
    val relationshipsDocument = parseXmlDocument(relsBytes)
    val relationshipTargets = relationshipsDocument.getElementsByTagName("Relationship")
        .elements()
        .associate { element ->
            element.getAttribute("Id") to normalizeSheetEntryPath(element.getAttribute("Target"))
        }

    return workbookDocument.getElementsByTagName("sheet")
        .elements()
        .mapNotNull { sheetElement ->
            val sheetName = sheetElement.getAttribute("name")
            val relationId = sheetElement.getAttribute("r:id")
            val targetPath = relationshipTargets[relationId] ?: return@mapNotNull null
            zipEntries[targetPath]?.let { sheetName to it }
        }
        .toMap(linkedMapOf())
}

private fun normalizeSheetEntryPath(target: String): String {
    return when {
        target.startsWith("xl/") -> target
        target.startsWith("/") -> target.removePrefix("/")
        else -> "xl/$target"
    }
}

private fun parseBaseSheet(
    sheetBytes: ByteArray,
    apartmentDrafts: MutableMap<String, ApartmentDraft>,
    sharedStrings: List<String>
) {
    val rows = parseSheetRows(sheetBytes, sharedStrings)
    if (rows.isEmpty()) return

    val headerIndex = resolveHeaderIndex(
        rows.first(),
        BaseColumn.entries.associateWith { it.aliasesNormalized }
    )
    requireColumns(
        sheetName = BASE_SHEET_NAME,
        headerIndex = headerIndex,
        requiredColumns = mapOf(
            BaseColumn.RecordType to "记录类型",
            BaseColumn.ApartmentName to "公寓名称"
        )
    )

    rows.drop(1).forEach { row ->
        val apartmentName = row.valueAt(headerIndex[BaseColumn.ApartmentName]).trim()
        if (apartmentName.isBlank()) return@forEach

        val recordType = row.valueAt(headerIndex[BaseColumn.RecordType]).toRecordTypeOrNull()
            ?: inferBaseRecordType(row, headerIndex)
            ?: return@forEach
        if (recordType == RecordType.Note) return@forEach

        val draft = apartmentDrafts.getOrPut(apartmentName) { ApartmentDraft(apartmentName) }
        when (recordType) {
            RecordType.Apartment -> {
                draft.isSelected = parseSelected(row.valueAt(headerIndex[BaseColumn.Selected]))
                draft.waterFee = normalizeImportedPrice(row.valueAt(headerIndex[BaseColumn.WaterFee]))
                draft.electricFee = normalizeImportedPrice(row.valueAt(headerIndex[BaseColumn.ElectricFee]))
            }

            RecordType.Floor -> {
                val floorNumber = row.valueAt(headerIndex[BaseColumn.FloorNumber]).toIntOrNull() ?: return@forEach
                draft.ensureFloor(floorNumber)
            }

            RecordType.Room -> {
                val floorNumber = row.valueAt(headerIndex[BaseColumn.FloorNumber]).toIntOrNull() ?: return@forEach
                val roomNumber = row.valueAt(headerIndex[BaseColumn.RoomNumber]).toIntOrNull() ?: return@forEach
                val roomDraft = draft.ensureRoom(floorNumber, roomNumber)
                parseRoomStatus(row.valueAt(headerIndex[BaseColumn.RoomStatus]))?.let { roomDraft.status = it }
            }

            RecordType.Monthly,
            RecordType.Note -> Unit
        }
    }
}

private fun parseHistorySheet(
    sheetBytes: ByteArray,
    apartmentDrafts: MutableMap<String, ApartmentDraft>,
    sharedStrings: List<String>
) {
    val rows = parseSheetRows(sheetBytes, sharedStrings)
    if (rows.isEmpty()) return

    val headerIndex = resolveHeaderIndex(
        rows.first(),
        HistoryColumn.entries.associateWith { it.aliasesNormalized }
    )
    requireColumns(
        sheetName = HISTORY_SHEET_NAME,
        headerIndex = headerIndex,
        requiredColumns = mapOf(
            HistoryColumn.ApartmentName to "公寓名称",
            HistoryColumn.FloorNumber to "楼层号",
            HistoryColumn.RoomNumber to "房间号",
            HistoryColumn.Month to "月份"
        )
    )

    rows.drop(1).forEach { row ->
        val recordType = row.valueAt(headerIndex[HistoryColumn.RecordType]).toRecordTypeOrNull()
            ?: RecordType.Monthly
        if (recordType == RecordType.Note) return@forEach

        val apartmentName = row.valueAt(headerIndex[HistoryColumn.ApartmentName]).trim()
        val floorNumber = row.valueAt(headerIndex[HistoryColumn.FloorNumber]).toIntOrNull()
        val roomNumber = row.valueAt(headerIndex[HistoryColumn.RoomNumber]).toIntOrNull()
        val monthKey = normalizeImportedMonth(row.valueAt(headerIndex[HistoryColumn.Month]))

        if (apartmentName.isBlank() || floorNumber == null || roomNumber == null || monthKey == null) {
            return@forEach
        }

        val draft = apartmentDrafts.getOrPut(apartmentName) { ApartmentDraft(apartmentName) }
        val roomDraft = draft.ensureRoom(floorNumber, roomNumber)

        roomDraft.monthlyValues[monthKey] = RoomMonthlyValue(
            rent = normalizeImportedNumeric(row.valueAt(headerIndex[HistoryColumn.Rent])),
            waterMeter = normalizeImportedNumeric(row.valueAt(headerIndex[HistoryColumn.WaterMeter])),
            electricMeter = normalizeImportedNumeric(row.valueAt(headerIndex[HistoryColumn.ElectricMeter]))
        )

        parseRoomStatus(row.valueAt(headerIndex[HistoryColumn.MonthStatus]))?.let { status ->
            roomDraft.monthlyStatuses[monthKey] = status
        }
    }
}

private fun parseLegacyCombinedSheet(
    sheetBytes: ByteArray,
    apartmentDrafts: MutableMap<String, ApartmentDraft>,
    sharedStrings: List<String>
) {
    val rows = parseSheetRows(sheetBytes, sharedStrings)
    if (rows.isEmpty()) return

    val aliases = linkedMapOf(
        "recordType" to setOf("记录类型", "record_type"),
        "apartmentName" to setOf("公寓名称", "apartment_name"),
        "selected" to setOf("是否当前选中", "selected"),
        "floorNumber" to setOf("楼层号", "楼层", "floor_number"),
        "roomNumber" to setOf("房间号", "room_number"),
        "roomStatus" to setOf("房间状态", "房间当前状态", "status"),
        "month" to setOf("月份", "month"),
        "monthStatus" to setOf("月份状态", "month_status"),
        "rent" to setOf("房租", "rent"),
        "waterMeter" to setOf("水表", "water_meter"),
        "electricMeter" to setOf("电表", "electric_meter"),
        "waterFee" to setOf("水价", "water_fee"),
        "electricFee" to setOf("电价", "electric_fee")
    ).mapValues { (_, labels) -> labels.map(::normalizeToken).toSet() }

    val headerIndex = resolveHeaderIndex(rows.first(), aliases)

    rows.drop(1).forEach { row ->
        val apartmentName = row.valueAt(headerIndex["apartmentName"]).trim()
        if (apartmentName.isBlank()) return@forEach

        val recordType = row.valueAt(headerIndex["recordType"]).toRecordTypeOrNull()
            ?: inferLegacyRecordType(row, headerIndex)
            ?: return@forEach
        if (recordType == RecordType.Note) return@forEach

        val draft = apartmentDrafts.getOrPut(apartmentName) { ApartmentDraft(apartmentName) }
        when (recordType) {
            RecordType.Apartment -> {
                draft.isSelected = parseSelected(row.valueAt(headerIndex["selected"]))
                draft.waterFee = normalizeImportedPrice(row.valueAt(headerIndex["waterFee"]))
                draft.electricFee = normalizeImportedPrice(row.valueAt(headerIndex["electricFee"]))
            }

            RecordType.Floor -> {
                row.valueAt(headerIndex["floorNumber"]).toIntOrNull()?.let(draft::ensureFloor)
            }

            RecordType.Room -> {
                val floorNumber = row.valueAt(headerIndex["floorNumber"]).toIntOrNull() ?: return@forEach
                val roomNumber = row.valueAt(headerIndex["roomNumber"]).toIntOrNull() ?: return@forEach
                val roomDraft = draft.ensureRoom(floorNumber, roomNumber)
                parseRoomStatus(row.valueAt(headerIndex["roomStatus"]))?.let { roomDraft.status = it }
            }

            RecordType.Monthly -> {
                val floorNumber = row.valueAt(headerIndex["floorNumber"]).toIntOrNull() ?: return@forEach
                val roomNumber = row.valueAt(headerIndex["roomNumber"]).toIntOrNull() ?: return@forEach
                val monthKey = normalizeImportedMonth(row.valueAt(headerIndex["month"])) ?: return@forEach
                val roomDraft = draft.ensureRoom(floorNumber, roomNumber)
                roomDraft.monthlyValues[monthKey] = RoomMonthlyValue(
                    rent = normalizeImportedNumeric(row.valueAt(headerIndex["rent"])),
                    waterMeter = normalizeImportedNumeric(row.valueAt(headerIndex["waterMeter"])),
                    electricMeter = normalizeImportedNumeric(row.valueAt(headerIndex["electricMeter"]))
                )
                parseRoomStatus(row.valueAt(headerIndex["monthStatus"]))?.let { status ->
                    roomDraft.monthlyStatuses[monthKey] = status
                }
            }

            RecordType.Note -> Unit
        }
    }
}

private fun parseXmlDocument(bytes: ByteArray) = DocumentBuilderFactory.newInstance().apply {
    isNamespaceAware = false
}.newDocumentBuilder().parse(ByteArrayInputStream(bytes))

private fun parseSharedStrings(sharedStringsBytes: ByteArray?): List<String> {
    if (sharedStringsBytes == null) return emptyList()
    val document = parseXmlDocument(sharedStringsBytes)
    return document.getElementsByTagName("si")
        .elements()
        .map { stringItem ->
            val plainTexts = stringItem.childElements("t")
            if (plainTexts.isNotEmpty()) {
                plainTexts.joinToString("") { it.textContent.orEmpty() }
            } else {
                stringItem.childElements("r").joinToString("") { richText ->
                    richText.childElements("t").firstOrNull()?.textContent.orEmpty()
                }
            }
        }
}

private fun parseSheetRows(sheetBytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
    val document = parseXmlDocument(sheetBytes)
    return document.getElementsByTagName("row")
        .elements()
        .map { rowElement ->
            val values = mutableListOf<String>()
            rowElement.childElements("c").forEach { cellElement ->
                val columnIndex = cellReferenceColumnIndex(cellElement.getAttribute("r"))
                while (values.size <= columnIndex) {
                    values += ""
                }
                values[columnIndex] = readCellText(cellElement, sharedStrings)
            }
            values.toList()
        }
}

private fun resolveHeaderIndex(
    headerRow: List<String>,
    aliasesByKey: Map<*, Set<String>>
): Map<Any, Int?> {
    val normalizedHeaders = headerRow.map(::normalizeToken)
    return aliasesByKey.mapKeys { it.key as Any }.mapValues { (_, aliases) ->
        normalizedHeaders.indexOfFirst { it in aliases }.takeIf { it >= 0 }
    }
}

private fun requireColumns(
    sheetName: String,
    headerIndex: Map<Any, Int?>,
    requiredColumns: Map<out Any, String>
) {
    val missingColumns = requiredColumns
        .filter { (columnKey, _) -> headerIndex[columnKey] == null }
        .values
    if (missingColumns.isNotEmpty()) {
        error("“$sheetName”缺少字段：${missingColumns.joinToString("、")}")
    }
}

private fun readCellText(cellElement: Element, sharedStrings: List<String>): String {
    return when (cellElement.getAttribute("t")) {
        "inlineStr" -> cellElement.childElements("is")
            .firstOrNull()
            ?.childElements("t")
            ?.firstOrNull()
            ?.textContent
            .orEmpty()

        "s" -> cellElement.childElements("v")
            .firstOrNull()
            ?.textContent
            ?.toIntOrNull()
            ?.let { sharedStrings.getOrNull(it) }
            .orEmpty()

        else -> cellElement.childElements("v").firstOrNull()?.textContent.orEmpty()
    }
}

private fun cellReferenceColumnIndex(reference: String): Int {
    val letters = reference.takeWhile { it.isLetter() }.uppercase(Locale.US)
    if (letters.isEmpty()) return 0
    var index = 0
    letters.forEach { letter ->
        index = index * 26 + (letter.code - 'A'.code + 1)
    }
    return index - 1
}

private fun inferBaseRecordType(
    row: List<String>,
    headerIndex: Map<Any, Int?>
): RecordType? {
    return when {
        row.valueAt(headerIndex[BaseColumn.RoomNumber]).isNotBlank() -> RecordType.Room
        row.valueAt(headerIndex[BaseColumn.FloorNumber]).isNotBlank() -> RecordType.Floor
        row.valueAt(headerIndex[BaseColumn.ApartmentName]).isNotBlank() -> RecordType.Apartment
        else -> null
    }
}

private fun inferLegacyRecordType(
    row: List<String>,
    headerIndex: Map<Any, Int?>
): RecordType? {
    return when {
        row.valueAt(headerIndex["month"]).isNotBlank() -> RecordType.Monthly
        row.valueAt(headerIndex["roomNumber"]).isNotBlank() -> RecordType.Room
        row.valueAt(headerIndex["floorNumber"]).isNotBlank() -> RecordType.Floor
        row.valueAt(headerIndex["apartmentName"]).isNotBlank() -> RecordType.Apartment
        else -> null
    }
}

private fun List<String>.valueAt(index: Int?): String {
    if (index == null || index !in indices) return ""
    return this[index].trim()
}

private fun normalizeImportedNumeric(rawValue: String): String {
    val trimmed = rawValue.trim()
    if (trimmed.isBlank()) return ""
    val numeric = trimmed.toDoubleOrNull() ?: return trimmed
    return if (kotlin.math.abs(numeric - numeric.toInt()) < 0.0001) {
        numeric.toInt().toString()
    } else {
        BigDecimal(trimmed).stripTrailingZeros().toPlainString()
    }
}

private fun normalizeImportedPrice(rawValue: String): String {
    val numeric = rawValue.trim().toDoubleOrNull() ?: 0.0
    return String.format(Locale.US, "%.2f", numeric)
}

private fun normalizeImportedMonth(rawValue: String): String? {
    val trimmed = rawValue.trim()
    if (trimmed.isBlank()) return null
    return runCatching { YearMonth.parse(trimmed) }.getOrNull()?.toString()
        ?: runCatching {
            val normalized = trimmed
                .replace("年", "-")
                .replace("月", "")
            YearMonth.parse(normalized, DateTimeFormatter.ofPattern("yyyy-M"))
        }.getOrNull()?.toString()
}

private fun parseSelected(rawValue: String): Boolean {
    return normalizeToken(rawValue) in setOf("是", "true", "yes", "1", "selected")
}

private fun String.toRecordTypeOrNull(): RecordType? {
    val normalized = normalizeToken(this)
    return RecordType.entries.firstOrNull { normalized in it.aliasesNormalized }
}

private fun parseRoomStatus(rawValue: String): RoomCardStatus? {
    return when (normalizeToken(rawValue)) {
        normalizeToken("已租且交租"), normalizeToken("paid") -> RoomCardStatus.Paid
        normalizeToken("未填写"), normalizeToken("unfilled"), normalizeToken("pending") -> RoomCardStatus.Unfilled
        normalizeToken("已租未交租"), normalizeToken("unpaid") -> RoomCardStatus.Unpaid
        normalizeToken("已租拖欠"), normalizeToken("拖欠"), normalizeToken("overdue") -> RoomCardStatus.Overdue
        normalizeToken("未租"), normalizeToken("vacant") -> RoomCardStatus.Vacant
        else -> null
    }
}

private fun summaryCategoryLabel(status: RoomCardStatus): String {
    return when (status) {
        RoomCardStatus.Paid -> "已收"
        RoomCardStatus.Unfilled -> "未填写"
        RoomCardStatus.Unpaid -> "应收"
        RoomCardStatus.Overdue -> "拖欠"
        RoomCardStatus.Vacant -> "未租"
    }
}

private fun exportMonthsForSummary(room: RoomUiState): List<YearMonth> {
    val monthKeys = (room.monthlyValues.keys + room.monthlyStatuses.keys)
        .mapNotNull { runCatching { YearMonth.parse(it) }.getOrNull() }
        .toMutableSet()

    val currentMonth = YearMonth.now()
    if (room.statusForMonth(currentMonth) != RoomCardStatus.Vacant || room.monthlyValues[currentMonth.toString()] != null) {
        monthKeys += currentMonth
    }

    return monthKeys.toList().sorted()
}

private fun calculateRoomCharge(
    room: RoomUiState,
    month: YearMonth,
    waterUnitPrice: Double,
    electricUnitPrice: Double
): Double {
    val currentMonthValue = room.effectiveValuesForMonth(month)
    val previousMonthValue = room.referenceValuesBeforeMonth(month)
    val rentAmount = currentMonthValue.rent.toDoubleOrNull() ?: 0.0
    val waterUsage = ((currentMonthValue.waterMeter.toDoubleOrNull() ?: 0.0) -
        (previousMonthValue.waterMeter.toDoubleOrNull() ?: 0.0)).coerceAtLeast(0.0)
    val electricUsage = ((currentMonthValue.electricMeter.toDoubleOrNull() ?: 0.0) -
        (previousMonthValue.electricMeter.toDoubleOrNull() ?: 0.0)).coerceAtLeast(0.0)
    return rentAmount + waterUsage * waterUnitPrice + electricUsage * electricUnitPrice
}

private fun statusLabel(status: RoomCardStatus): String {
    return when (status) {
        RoomCardStatus.Paid -> "已租且交租"
        RoomCardStatus.Unfilled -> "未填写"
        RoomCardStatus.Unpaid -> "已租未交租"
        RoomCardStatus.Overdue -> "已租拖欠"
        RoomCardStatus.Vacant -> "未租"
    }
}

private fun RecordType.label(): String {
    return when (this) {
        RecordType.Note -> "说明"
        RecordType.Apartment -> "公寓"
        RecordType.Floor -> "楼层"
        RecordType.Room -> "房间"
        RecordType.Monthly -> "月账单"
    }
}

private fun normalizeToken(rawValue: String): String {
    return rawValue.trim()
        .lowercase(Locale.US)
        .replace("_", "")
        .replace(" ", "")
}

private fun todayStamp(): String {
    val now = java.time.LocalDate.now()
    return "%04d-%02d-%02d".format(Locale.US, now.year, now.monthValue, now.dayOfMonth)
}

private fun org.w3c.dom.NodeList.elements(): List<Element> {
    return buildList {
        for (index in 0 until length) {
            val item = item(index)
            if (item is Element) add(item)
        }
    }
}

private fun Element.childElements(tagName: String): List<Element> {
    return childNodes.let { childNodes ->
        buildList {
            for (index in 0 until childNodes.length) {
                val item = childNodes.item(index)
                if (item is Element && item.tagName.endsWith(tagName)) {
                    add(item)
                }
            }
        }
    }
}

private data class ApartmentDraft(
    val name: String,
    var waterFee: String = "0.00",
    var electricFee: String = "0.00",
    var isSelected: Boolean = false,
    val floors: LinkedHashSet<Int> = linkedSetOf(),
    val roomsByFloor: LinkedHashMap<Int, LinkedHashMap<Int, RoomDraft>> = linkedMapOf()
) {
    fun ensureFloor(floorNumber: Int) {
        floors += floorNumber
        roomsByFloor.getOrPut(floorNumber) { linkedMapOf() }
    }

    fun ensureRoom(floorNumber: Int, roomNumber: Int): RoomDraft {
        ensureFloor(floorNumber)
        return roomsByFloor.getValue(floorNumber).getOrPut(roomNumber) { RoomDraft(roomNumber) }
    }

    fun toApartment(): ApartmentUiState? {
        if (name.isBlank()) return null
        val sortedFloors = floors.toList().sorted()
        return ApartmentUiState(
            name = name,
            waterFee = normalizeImportedPrice(waterFee),
            electricFee = normalizeImportedPrice(electricFee),
            floors = sortedFloors,
            roomsByFloor = sortedFloors.associateWith { floorNumber ->
                roomsByFloor[floorNumber]
                    .orEmpty()
                    .values
                    .sortedBy { it.roomNumber }
                    .map { room ->
                        RoomUiState(
                            roomNumber = room.roomNumber,
                            monthlyValues = room.monthlyValues.toMap(),
                            monthlyStatuses = room.monthlyStatuses.toMap(),
                            status = room.status
                        )
                    }
            }
        )
    }
}

private data class RoomDraft(
    val roomNumber: Int,
    var status: RoomCardStatus = RoomCardStatus.Vacant,
    val monthlyValues: LinkedHashMap<String, RoomMonthlyValue> = linkedMapOf(),
    val monthlyStatuses: LinkedHashMap<String, RoomCardStatus> = linkedMapOf()
)
