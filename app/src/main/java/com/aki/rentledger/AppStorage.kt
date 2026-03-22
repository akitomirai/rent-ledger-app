package com.aki.rentledger

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.YearMonth

const val SAMPLE_APARTMENT_NAME = "\u793A\u4F8B\u516C\u5BD3"

fun defaultSampleApartment(): ApartmentUiState {
    return ApartmentUiState(
        name = SAMPLE_APARTMENT_NAME,
        waterFee = "3.00",
        electricFee = "0.80",
        floors = listOf(2, 3),
        roomsByFloor = mapOf(
            2 to emptyList(),
            3 to emptyList()
        )
    )
}

data class PersistedAppState(
    val apartments: List<ApartmentUiState>,
    val selectedApartmentName: String?
)

class RentLedgerStorage(context: Context) {
    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadState(defaultApartments: List<ApartmentUiState>): PersistedAppState {
        val rawState = sharedPreferences.getString(KEY_APP_STATE, null)
        if (rawState.isNullOrBlank()) {
            return PersistedAppState(
                apartments = defaultApartments,
                selectedApartmentName = defaultApartments.firstOrNull()?.name
            )
        }

        return try {
            val root = JSONObject(rawState)
            val apartments = root.optJSONArray(KEY_APARTMENTS)
                ?.toApartmentList()
                ?.ifEmpty { defaultApartments }
                ?: defaultApartments
            val selectedApartmentName = root.opt(KEY_SELECTED_APARTMENT_NAME)
                ?.takeIf { it != JSONObject.NULL }
                ?.toString()
                ?.takeIf { it.isNotBlank() && apartments.any { apartment -> apartment.name == it } }
                ?: apartments.firstOrNull()?.name

            PersistedAppState(
                apartments = apartments,
                selectedApartmentName = selectedApartmentName
            )
        } catch (_: Exception) {
            PersistedAppState(
                apartments = defaultApartments,
                selectedApartmentName = defaultApartments.firstOrNull()?.name
            )
        }
    }

    fun saveState(apartments: List<ApartmentUiState>, selectedApartmentName: String?) {
        val root = JSONObject().apply {
            put(KEY_APARTMENTS, apartments.toApartmentJsonArray())
            put(KEY_SELECTED_APARTMENT_NAME, selectedApartmentName ?: JSONObject.NULL)
        }
        sharedPreferences.edit()
            .putString(KEY_APP_STATE, root.toString())
            .apply()
    }

    private fun JSONArray.toApartmentList(): List<ApartmentUiState> {
        return buildList {
            for (index in 0 until length()) {
                val apartmentObject = optJSONObject(index) ?: continue
                val floors = apartmentObject.optJSONArray(KEY_FLOORS)?.toIntList().orEmpty()
                val roomsByFloor = apartmentObject.optJSONObject(KEY_ROOMS_BY_FLOOR)
                    ?.toRoomsByFloorMap()
                    .orEmpty()
                add(
                    ApartmentUiState(
                        name = apartmentObject.optString(KEY_NAME),
                        waterFee = apartmentObject.optString(KEY_WATER_FEE, "0.00"),
                        electricFee = apartmentObject.optString(KEY_ELECTRIC_FEE, "0.00"),
                        floors = floors,
                        roomsByFloor = floors.associateWith { floor -> roomsByFloor[floor].orEmpty() }
                    )
                )
            }
        }.filter { it.name.isNotBlank() }
    }

    private fun JSONArray.toIntList(): List<Int> {
        return buildList {
            for (index in 0 until length()) {
                optInt(index).takeIf { it > 0 }?.let(::add)
            }
        }
    }

    private fun JSONObject.toRoomsByFloorMap(): Map<Int, List<RoomUiState>> {
        return keys().asSequence()
            .mapNotNull { floorKey ->
                floorKey.toIntOrNull()?.let { floorNumber ->
                    floorNumber to optJSONArray(floorKey).toRoomList()
                }
            }
            .toMap()
    }

    private fun JSONArray?.toRoomList(): List<RoomUiState> {
        if (this == null) return emptyList()

        return buildList {
            for (index in 0 until length()) {
                val roomObject = optJSONObject(index) ?: continue
                val monthlyValuesObject = roomObject.optJSONObject(KEY_MONTHLY_VALUES)
                val legacyStatus = safeRoomCardStatus(roomObject.optString(KEY_STATUS))
                val monthlyStatuses = roomObject.optJSONObject(KEY_MONTHLY_STATUSES)
                    .toMonthlyStatusMap()
                    .ifEmpty { mapOf(YearMonth.now().toString() to legacyStatus) }
                add(
                    RoomUiState(
                        roomNumber = roomObject.optInt(KEY_ROOM_NUMBER),
                        monthlyValues = monthlyValuesObject.toMonthlyValuesMap(),
                        monthlyStatuses = monthlyStatuses,
                        status = monthlyStatuses[YearMonth.now().toString()] ?: legacyStatus
                    )
                )
            }
        }.filter { it.roomNumber > 0 }
    }

    private fun JSONObject?.toMonthlyValuesMap(): Map<String, RoomMonthlyValue> {
        if (this == null) return emptyMap()

        return keys().asSequence()
            .mapNotNull { monthKey ->
                optJSONObject(monthKey)?.let { monthObject ->
                    monthKey to RoomMonthlyValue(
                        rent = monthObject.optString(KEY_RENT),
                        waterMeter = monthObject.optString(KEY_WATER_METER),
                        electricMeter = monthObject.optString(KEY_ELECTRIC_METER)
                    )
                }
            }
            .toMap()
    }

    private fun JSONObject?.toMonthlyStatusMap(): Map<String, RoomCardStatus> {
        if (this == null) return emptyMap()

        return keys().asSequence()
            .mapNotNull { monthKey ->
                optString(monthKey)
                    .takeIf { it.isNotBlank() }
                    ?.let(::safeRoomCardStatus)
                    ?.let { status -> monthKey to status }
            }
            .toMap()
    }

    private fun List<ApartmentUiState>.toApartmentJsonArray(): JSONArray {
        return JSONArray().apply {
            forEach { apartment ->
                put(
                    JSONObject().apply {
                        put(KEY_NAME, apartment.name)
                        put(KEY_WATER_FEE, apartment.waterFee)
                        put(KEY_ELECTRIC_FEE, apartment.electricFee)
                        put(KEY_FLOORS, JSONArray().apply {
                            apartment.floors.forEach(::put)
                        })
                        put(KEY_ROOMS_BY_FLOOR, JSONObject().apply {
                            apartment.floors.forEach { floorNumber ->
                                put(floorNumber.toString(), apartment.roomsByFloor[floorNumber].orEmpty().toRoomJsonArray())
                            }
                        })
                    }
                )
            }
        }
    }

    private fun List<RoomUiState>.toRoomJsonArray(): JSONArray {
        val currentMonth = YearMonth.now()
        return JSONArray().apply {
            forEach { room ->
                put(
                    JSONObject().apply {
                        put(KEY_ROOM_NUMBER, room.roomNumber)
                        put(KEY_STATUS, room.statusForMonth(currentMonth).name)
                        put(KEY_MONTHLY_VALUES, JSONObject().apply {
                            room.monthlyValues.forEach { (monthKey, monthValue) ->
                                put(
                                    monthKey,
                                    JSONObject().apply {
                                        put(KEY_RENT, monthValue.rent)
                                        put(KEY_WATER_METER, monthValue.waterMeter)
                                        put(KEY_ELECTRIC_METER, monthValue.electricMeter)
                                    }
                                )
                            }
                        })
                        put(KEY_MONTHLY_STATUSES, JSONObject().apply {
                            room.monthlyStatuses.forEach { (monthKey, status) ->
                                put(monthKey, status.name)
                            }
                        })
                    }
                )
            }
        }
    }

    private fun safeRoomCardStatus(rawValue: String): RoomCardStatus {
        return RoomCardStatus.entries.firstOrNull { it.name == rawValue } ?: RoomCardStatus.Vacant
    }

    private companion object {
        const val PREFS_NAME = "rent_ledger_storage"
        const val KEY_APP_STATE = "app_state"
        const val KEY_APARTMENTS = "apartments"
        const val KEY_SELECTED_APARTMENT_NAME = "selectedApartmentName"
        const val KEY_NAME = "name"
        const val KEY_WATER_FEE = "waterFee"
        const val KEY_ELECTRIC_FEE = "electricFee"
        const val KEY_FLOORS = "floors"
        const val KEY_ROOMS_BY_FLOOR = "roomsByFloor"
        const val KEY_ROOM_NUMBER = "roomNumber"
        const val KEY_STATUS = "status"
        const val KEY_MONTHLY_VALUES = "monthlyValues"
        const val KEY_MONTHLY_STATUSES = "monthlyStatuses"
        const val KEY_RENT = "rent"
        const val KEY_WATER_METER = "waterMeter"
        const val KEY_ELECTRIC_METER = "electricMeter"
    }
}
