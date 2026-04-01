package com.aki.rentledger

import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomMonthlyContinuationTest {

    @Test
    fun `effective rent keeps following the latest non blank month across gaps`() {
        val room = RoomUiState(
            roomNumber = 203,
            monthlyValues = mapOf(
                "2026-01" to RoomMonthlyValue(rent = "450"),
                "2026-02" to RoomMonthlyValue(),
                "2026-03" to RoomMonthlyValue()
            )
        )

        assertEquals("450", room.effectiveValuesForMonth(YearMonth.of(2026, 4)).rent)
    }

    @Test
    fun `history display values inherit latest known rent and meters before current month`() {
        val room = RoomUiState(
            roomNumber = 203,
            monthlyValues = mapOf(
                "2026-01" to RoomMonthlyValue(rent = "420", waterMeter = "12", electricMeter = "200"),
                "2026-03" to RoomMonthlyValue(rent = "450", waterMeter = "38", electricMeter = "733"),
                "2026-04" to RoomMonthlyValue()
            )
        )

        val referenceValue = room.referenceValuesBeforeMonth(YearMonth.of(2026, 4))
        val displayValue = room.displayValuesForMonth(YearMonth.of(2026, 4))

        assertEquals("450", referenceValue.rent)
        assertEquals("38", referenceValue.waterMeter)
        assertEquals("733", referenceValue.electricMeter)
        assertEquals("450", displayValue.rent)
        assertEquals("38", displayValue.waterMeter)
        assertEquals("733", displayValue.electricMeter)
    }
}
