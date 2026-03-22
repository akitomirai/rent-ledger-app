package com.aki.rentledger.data.repository

import com.aki.rentledger.data.local.entity.AppSettingsEntity
import com.aki.rentledger.data.local.entity.ArrearsFollowUpEntity
import com.aki.rentledger.data.local.entity.BillExtraItemEntity
import com.aki.rentledger.data.local.entity.BillExtraItemType
import com.aki.rentledger.data.local.entity.BillStatus
import com.aki.rentledger.data.local.entity.BuildingEntity
import com.aki.rentledger.data.local.entity.FloorEntity
import com.aki.rentledger.data.local.entity.MonthlyBillEntity
import com.aki.rentledger.data.local.entity.PaymentRecordEntity
import com.aki.rentledger.data.local.entity.RoomEntity
import com.aki.rentledger.data.local.entity.RoomOccupancyEntity
import com.aki.rentledger.data.local.entity.RoomStatus
import com.aki.rentledger.data.local.entity.TenantEntity
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupJsonCodecTest {
    @Test
    fun `encode includes all critical collections`() {
        val payload = BackupPayload(
            exportedAt = LocalDate.of(2026, 3, 15),
            buildings = listOf(BuildingEntity(1, "A栋", 1, 1L, 2L)),
            floors = listOf(FloorEntity(1, 1, "1层", 1, 1L, 2L)),
            rooms = listOf(RoomEntity(1, 1, "A101", 100_000, 500, 800, RoomStatus.RENTED, "", 1L, 2L)),
            tenants = listOf(TenantEntity(1, "张三", "13800138000", "", 1L, 2L)),
            roomOccupancies = listOf(
                RoomOccupancyEntity(
                    id = 1,
                    roomId = 1,
                    tenantId = 1,
                    startDate = LocalDate.of(2026, 3, 1),
                    leaseEndDate = LocalDate.of(2026, 9, 30),
                    endDate = null,
                    depositAmount = 200_000,
                    monthlyRentSnapshot = 100_000,
                    note = "",
                    finalWaterReading = null,
                    finalElectricReading = null,
                    rentDeductionAmount = 0,
                    utilityDeductionAmount = 0,
                    otherDeductionAmount = 0,
                    depositRefundAmount = 0,
                    settlementNote = "",
                    settledAt = null,
                    createdAt = 1L,
                    updatedAt = 2L
                )
            ),
            monthlyBills = listOf(
                MonthlyBillEntity(
                    id = 1,
                    roomId = 1,
                    billingMonth = "2026-03",
                    rentAmount = 100_000,
                    waterStartReading = "0",
                    waterEndReading = "10",
                    electricStartReading = "0",
                    electricEndReading = "20",
                    waterPrice = 500,
                    electricPrice = 800,
                    waterFee = 5_000,
                    electricFee = 16_000,
                    extraAmount = 0,
                    discountAmount = 0,
                    receivableAmount = 121_000,
                    receivedAmount = 0,
                    unreceivedAmount = 121_000,
                    billStatus = BillStatus.PENDING,
                    dueDate = LocalDate.of(2026, 3, 31),
                    note = "",
                    createdAt = 1L,
                    updatedAt = 2L
                )
            ),
            paymentRecords = listOf(
                PaymentRecordEntity(
                    id = 1,
                    billId = 1,
                    amount = 50_000,
                    paymentDate = LocalDate.of(2026, 3, 15),
                    paymentMethod = "现金",
                    note = "",
                    createdAt = 1L,
                    updatedAt = 2L
                )
            ),
            extraItems = listOf(
                BillExtraItemEntity(1, 1, "垃圾费", 500, BillExtraItemType.CHARGE, 1L, 2L)
            ),
            arrearsFollowUps = listOf(
                ArrearsFollowUpEntity(
                    id = 1,
                    billId = 1,
                    tenantId = 1,
                    roomId = 1,
                    billingMonth = "2026-03",
                    followUpDate = LocalDate.of(2026, 3, 16),
                    followUpMethod = "电话",
                    resultStatus = "已联系",
                    nextFollowUpDate = LocalDate.of(2026, 3, 18),
                    note = "",
                    createdAt = 1L,
                    updatedAt = 2L
                )
            ),
            settings = AppSettingsEntity(1, 500, 800, 5, "CNY", 1L, 2L)
        )

        val root = BackupJsonCodec.encode(payload)

        assertTrue(root.has("buildings"))
        assertTrue(root.has("floors"))
        assertTrue(root.has("rooms"))
        assertTrue(root.has("tenants"))
        assertTrue(root.has("roomOccupancies"))
        assertTrue(root.has("monthlyBills"))
        assertTrue(root.has("paymentRecords"))
        assertTrue(root.has("extraItems"))
        assertTrue(root.has("arrearsFollowUps"))
        assertTrue(root.has("settings"))
    }

    @Test
    fun `decode tolerates old backup without newer collections`() {
        val root = JSONObject().apply {
            put("version", 1)
            put("exportedAt", "2026-03-15")
            put("buildings", JSONArray())
            put("floors", JSONArray())
            put("rooms", JSONArray())
            put("monthlyBills", JSONArray())
            put("paymentRecords", JSONArray())
            put("extraItems", JSONArray())
            put("settings", JSONObject.NULL)
        }

        val payload = BackupJsonCodec.decode(root)

        assertTrue(payload.tenants.isEmpty())
        assertTrue(payload.roomOccupancies.isEmpty())
        assertTrue(payload.arrearsFollowUps.isEmpty())
        assertEquals(1, payload.version)
    }
}
