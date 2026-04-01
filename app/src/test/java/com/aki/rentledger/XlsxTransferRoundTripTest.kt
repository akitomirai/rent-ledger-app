package com.aki.rentledger

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.time.YearMonth
import java.util.zip.ZipInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XlsxTransferRoundTripTest {
    @Test
    fun `exported workbook round trips apartment ledger data`() {
        val billingMonth = YearMonth.of(2026, 3)
        val previousMonth = billingMonth.minusMonths(1)
        val apartments = listOf(
            ApartmentUiState(
                name = "忆佳公寓",
                waterFee = "3.50",
                electricFee = "0.88",
                floors = listOf(2, 3),
                roomsByFloor = mapOf(
                    2 to listOf(
                        RoomUiState(
                            roomNumber = 201,
                            monthlyValues = mapOf(
                                previousMonth.toString() to RoomMonthlyValue(
                                    rent = "550",
                                    waterMeter = "12",
                                    electricMeter = "320"
                                ),
                                billingMonth.toString() to RoomMonthlyValue(
                                    rent = "",
                                    waterMeter = "18",
                                    electricMeter = "335"
                                )
                            ),
                            monthlyStatuses = mapOf(
                                billingMonth.toString() to RoomCardStatus.Unpaid
                            ),
                            status = RoomCardStatus.Unpaid
                        ),
                        RoomUiState(
                            roomNumber = 202,
                            monthlyValues = mapOf(
                                billingMonth.toString() to RoomMonthlyValue(
                                    rent = "620",
                                    waterMeter = "25",
                                    electricMeter = "410"
                                )
                            ),
                            monthlyStatuses = mapOf(
                                billingMonth.toString() to RoomCardStatus.Paid
                            ),
                            status = RoomCardStatus.Paid
                        )
                    ),
                    3 to listOf(
                        RoomUiState(
                            roomNumber = 301,
                            monthlyValues = mapOf(
                                billingMonth.toString() to RoomMonthlyValue(
                                    rent = "700",
                                    waterMeter = "31",
                                    electricMeter = "450"
                                )
                            ),
                            monthlyStatuses = mapOf(
                                billingMonth.toString() to RoomCardStatus.Unfilled
                            ),
                            status = RoomCardStatus.Unfilled
                        )
                    )
                )
            )
        )

        val workbookBytes = RentLedgerXlsxTransfer.exportWorkbookBytesForTest(
            apartments = apartments,
            selectedApartmentName = "忆佳公寓"
        )
        val zipEntries = unzipEntries(workbookBytes)
        val workbookXml = String(zipEntries.getValue("xl/workbook.xml"), StandardCharsets.UTF_8)

        assertTrue(workbookXml.contains("""sheet name="基础资料""""))
        assertTrue(workbookXml.contains("""sheet name="历史账本""""))
        assertTrue(workbookXml.contains("""sheet name="应收已收""""))

        val importedState = RentLedgerXlsxTransfer.importWorkbookBytesForTest(
            workbookBytes = workbookBytes,
            defaultApartments = emptyList()
        )

        assertEquals("忆佳公寓", importedState.selectedApartmentName)
        val apartment = importedState.apartments.single()
        assertEquals("3.50", apartment.waterFee)
        assertEquals("0.88", apartment.electricFee)
        assertEquals(listOf(2, 3), apartment.floors)

        val room201 = apartment.roomsByFloor.getValue(2).first { it.roomNumber == 201 }
        assertEquals("18", room201.monthlyValues.getValue(billingMonth.toString()).waterMeter)
        assertEquals("335", room201.monthlyValues.getValue(billingMonth.toString()).electricMeter)
        assertEquals(RoomCardStatus.Unpaid, room201.monthlyStatuses.getValue(billingMonth.toString()))

        val room301 = apartment.roomsByFloor.getValue(3).first { it.roomNumber == 301 }
        assertEquals(RoomCardStatus.Unfilled, room301.monthlyStatuses.getValue(billingMonth.toString()))
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
}
