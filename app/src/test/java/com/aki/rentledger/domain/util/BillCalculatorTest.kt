package com.aki.rentledger.domain.util

import com.aki.rentledger.data.local.entity.BillStatus
import com.aki.rentledger.domain.model.BillDraft
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class BillCalculatorTest {
    @Test
    fun `calculate clamps negative meter usage to zero`() {
        val draft = BillDraft(
            roomId = 1,
            billingMonth = "2026-03",
            rentAmount = "1000",
            waterStartReading = "12",
            waterEndReading = "10",
            electricStartReading = "30",
            electricEndReading = "20",
            waterPrice = "3",
            electricPrice = "5",
            extraAmount = "0",
            discountAmount = "0",
            receivedAmount = "0",
            dueDate = LocalDate.now().plusDays(1)
        )

        val result = BillCalculator.calculate(draft)

        assertEquals(0L, result.waterFee)
        assertEquals(0L, result.electricFee)
        assertEquals(100_000L, result.receivableAmount)
        assertEquals(BillStatus.PENDING, result.status)
    }

    @Test
    fun `calculate marks bill overdue when unpaid and due date has passed`() {
        val draft = BillDraft(
            roomId = 1,
            billingMonth = "2026-03",
            rentAmount = "1000",
            waterStartReading = "0",
            waterEndReading = "5",
            electricStartReading = "0",
            electricEndReading = "10",
            waterPrice = "2",
            electricPrice = "3",
            extraAmount = "0",
            discountAmount = "0",
            receivedAmount = "0",
            dueDate = LocalDate.now().minusDays(1)
        )

        val result = BillCalculator.calculate(draft)

        assertEquals(BillStatus.OVERDUE, result.status)
        assertEquals(104_000L, result.receivableAmount)
        assertEquals(104_000L, result.unreceivedAmount)
    }
}
