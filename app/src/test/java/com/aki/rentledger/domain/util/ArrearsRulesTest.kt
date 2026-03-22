package com.aki.rentledger.domain.util

import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArrearsRulesTest {
    @Test
    fun `needs follow up when there is no follow up record`() {
        val result = ArrearsRules.needsFollowUp(
            unreceivedAmount = 100_00,
            latestFollowUpId = null,
            latestResultStatus = null,
            latestNextFollowUpDate = null,
            today = LocalDate.of(2026, 3, 15)
        )

        assertTrue(result)
    }

    @Test
    fun `needs follow up when latest next date is due and bill not settled`() {
        val result = ArrearsRules.needsFollowUp(
            unreceivedAmount = 100_00,
            latestFollowUpId = 1L,
            latestResultStatus = "已联系",
            latestNextFollowUpDate = LocalDate.of(2026, 3, 15),
            today = LocalDate.of(2026, 3, 15)
        )

        assertTrue(result)
    }

    @Test
    fun `does not need follow up when latest result is settled`() {
        val result = ArrearsRules.needsFollowUp(
            unreceivedAmount = 100_00,
            latestFollowUpId = 1L,
            latestResultStatus = "已结清",
            latestNextFollowUpDate = LocalDate.of(2026, 3, 20),
            today = LocalDate.of(2026, 3, 15)
        )

        assertFalse(result)
    }
}
