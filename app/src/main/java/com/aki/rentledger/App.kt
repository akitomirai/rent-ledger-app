package com.aki.rentledger

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.aki.rentledger.ui.navigation.AppTab
import com.aki.rentledger.ui.screens.BuildingScreen
import com.aki.rentledger.ui.screens.RentScreen
import com.aki.rentledger.ui.screens.SystemScreen
import java.time.YearMonth

data class ApartmentUiState(
    val name: String,
    val waterFee: String,
    val electricFee: String,
    val floors: List<Int> = emptyList(),
    val roomsByFloor: Map<Int, List<RoomUiState>> = emptyMap()
)

data class RoomUiState(
    val roomNumber: Int,
    val monthlyValues: Map<String, RoomMonthlyValue> = emptyMap(),
    val monthlyStatuses: Map<String, RoomCardStatus> = emptyMap(),
    val status: RoomCardStatus = RoomCardStatus.Vacant
)

data class RoomMonthlyValue(
    val rent: String = "",
    val waterMeter: String = "",
    val electricMeter: String = ""
)

enum class RoomCardStatus {
    Paid,
    Unfilled,
    Unpaid,
    Overdue,
    Vacant
}

fun RoomUiState.valuesForMonth(month: YearMonth): RoomMonthlyValue {
    return monthlyValues[month.toString()] ?: RoomMonthlyValue()
}

fun RoomUiState.effectiveValuesForMonth(month: YearMonth): RoomMonthlyValue {
    val currentValue = valuesForMonth(month)
    if (currentValue.rent.isNotBlank()) {
        return currentValue
    }

    val previousRent = valuesForMonth(month.minusMonths(1)).rent
    return if (previousRent.isBlank()) {
        currentValue
    } else {
        currentValue.copy(rent = previousRent)
    }
}

fun RoomUiState.updateValuesForMonth(
    month: YearMonth,
    transform: (RoomMonthlyValue) -> RoomMonthlyValue
): RoomUiState {
    val monthKey = month.toString()
    val updatedValue = transform(valuesForMonth(month))
    return copy(monthlyValues = monthlyValues + (monthKey to updatedValue))
}

fun RoomUiState.statusForMonth(month: YearMonth): RoomCardStatus {
    monthlyStatuses[month.toString()]?.let { return it }

    val currentMonth = YearMonth.now()
    val latestExplicitMonth = monthlyStatuses.keys
        .mapNotNull { rawMonth -> runCatching { YearMonth.parse(rawMonth) }.getOrNull() }
        .filter { !it.isAfter(month) }
        .maxOrNull()

    if (latestExplicitMonth != null) {
        val anchorMonth = latestExplicitMonth
        var derivedStatus = monthlyStatuses.getValue(anchorMonth.toString())
        var cursorMonth: YearMonth = anchorMonth
        while (cursorMonth.isBefore(month)) {
            derivedStatus = derivedStatus.nextMonthDefaultStatus()
            cursorMonth = cursorMonth.plusMonths(1)
        }
        return derivedStatus
    }

    if (month == currentMonth) {
        return status
    }

    if (month.isAfter(currentMonth)) {
        var derivedStatus = monthlyStatuses[currentMonth.toString()] ?: status
        var cursorMonth = currentMonth
        while (cursorMonth.isBefore(month)) {
            derivedStatus = derivedStatus.nextMonthDefaultStatus()
            cursorMonth = cursorMonth.plusMonths(1)
        }
        return derivedStatus
    }

    return RoomCardStatus.Vacant
}

fun RoomUiState.updateStatusForMonth(
    month: YearMonth,
    updatedStatus: RoomCardStatus
): RoomUiState {
    val updatedMonthlyStatuses = monthlyStatuses + (month.toString() to updatedStatus)
    val updatedRoom = copy(monthlyStatuses = updatedMonthlyStatuses)
    return updatedRoom.copy(status = updatedRoom.statusForMonth(YearMonth.now()))
}

private fun RoomCardStatus.nextMonthDefaultStatus(): RoomCardStatus {
    return when (this) {
        RoomCardStatus.Paid -> RoomCardStatus.Unfilled
        RoomCardStatus.Unfilled -> RoomCardStatus.Unfilled
        RoomCardStatus.Unpaid -> RoomCardStatus.Overdue
        RoomCardStatus.Overdue -> RoomCardStatus.Overdue
        RoomCardStatus.Vacant -> RoomCardStatus.Vacant
    }
}

fun nextRoomNumberForFloor(
    floorNumber: Int,
    floorRooms: List<RoomUiState>
): Int {
    val roomPrefix = "${floorNumber}0"
    val nextRoomIndex = floorRooms
        .mapNotNull { room ->
            room.roomNumber
                .toString()
                .takeIf { it.startsWith(roomPrefix) }
                ?.removePrefix(roomPrefix)
                ?.toIntOrNull()
        }
        .maxOrNull()
        ?.plus(1)
        ?: 1

    return "$roomPrefix$nextRoomIndex".toInt()
}

@Composable
fun RentLedgerApp() {
    val context = LocalContext.current.applicationContext
    val storage = remember(context) { RentLedgerStorage(context) }
    var selectedTab by remember { mutableStateOf(AppTab.Rent) }
    var apartmentTabResetTick by remember { mutableStateOf(0) }
    val defaultApartments = remember { listOf(defaultSampleApartment()) }
    val initialState = remember(storage) { storage.loadState(defaultApartments) }
    var apartments by remember { mutableStateOf(initialState.apartments) }
    var selectedApartmentName by remember { mutableStateOf(initialState.selectedApartmentName) }

    LaunchedEffect(apartments, selectedApartmentName) {
        storage.saveState(apartments, selectedApartmentName)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = {
                            if (tab == AppTab.Rent) {
                                apartmentTabResetTick += 1
                            }
                            selectedTab = tab
                        },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        when (selectedTab) {
            AppTab.Rent -> RentScreen(
                modifier = contentModifier,
                apartmentTabResetTick = apartmentTabResetTick,
                apartments = apartments,
                selectedApartmentName = selectedApartmentName,
                onSelectApartment = { selectedApartmentName = it },
                onAddApartment = { apartmentName ->
                    val trimmedName = apartmentName.trim()
                    if (trimmedName.isNotEmpty() && apartments.none { it.name == trimmedName }) {
                        apartments = apartments + ApartmentUiState(
                            name = trimmedName,
                            waterFee = "0.00",
                            electricFee = "0.00",
                            floors = emptyList(),
                            roomsByFloor = emptyMap()
                        )
                        selectedApartmentName = trimmedName
                    }
                },
                onUpdateApartmentFees = { apartmentName, waterFee, electricFee ->
                    apartments = apartments.map { apartment ->
                        if (apartment.name == apartmentName) {
                            apartment.copy(
                                waterFee = waterFee,
                                electricFee = electricFee
                            )
                        } else {
                            apartment
                        }
                    }
                },
                onRenameApartment = { oldName, newName ->
                    apartments = apartments.map { apartment ->
                        if (apartment.name == oldName) {
                            apartment.copy(name = newName)
                        } else {
                            apartment
                        }
                    }
                    if (selectedApartmentName == oldName) {
                        selectedApartmentName = newName
                    }
                },
                onAddFloor = { apartmentName ->
                    apartments = apartments.map { apartment ->
                        if (apartment.name == apartmentName) {
                            val nextFloorNumber = apartment.floors.maxOrNull()?.plus(1) ?: 2
                            apartment.copy(
                                floors = apartment.floors + nextFloorNumber,
                                roomsByFloor = apartment.roomsByFloor + (nextFloorNumber to emptyList())
                            )
                        } else {
                            apartment
                        }
                    }
                },
                onDeleteFloor = { apartmentName, floorNumber ->
                    apartments = apartments.map { apartment ->
                        if (apartment.name == apartmentName) {
                            apartment.copy(
                                floors = apartment.floors.filterNot { it == floorNumber },
                                roomsByFloor = apartment.roomsByFloor - floorNumber
                            )
                        } else {
                            apartment
                        }
                    }
                },
                onAddRoom = { apartmentName, floorNumber ->
                    apartments = apartments.map { apartment ->
                        if (apartment.name == apartmentName) {
                            val floorRooms = apartment.roomsByFloor[floorNumber].orEmpty()
                            val nextRoomNumber = nextRoomNumberForFloor(floorNumber, floorRooms)
                            apartment.copy(
                                roomsByFloor = apartment.roomsByFloor + (
                                    floorNumber to (floorRooms + RoomUiState(roomNumber = nextRoomNumber))
                                )
                            )
                        } else {
                            apartment
                        }
                    }
                },
                onUpdateRoom = { apartmentName, floorNumber, updatedRoom ->
                    apartments = apartments.map { apartment ->
                        if (apartment.name == apartmentName) {
                            val floorRooms = apartment.roomsByFloor[floorNumber].orEmpty()
                            apartment.copy(
                                roomsByFloor = apartment.roomsByFloor + (
                                    floorNumber to floorRooms.map { room ->
                                        if (room.roomNumber == updatedRoom.roomNumber) updatedRoom else room
                                    }
                                )
                            )
                        } else {
                            apartment
                        }
                    }
                },
                onUpdateRoomStatus = { apartmentName, floorNumber, roomNumber, month, status ->
                    apartments = apartments.map { apartment ->
                        if (apartment.name == apartmentName) {
                            val floorRooms = apartment.roomsByFloor[floorNumber].orEmpty()
                            apartment.copy(
                                roomsByFloor = apartment.roomsByFloor + (
                                    floorNumber to floorRooms.map { room ->
                                        if (room.roomNumber == roomNumber) {
                                            room.updateStatusForMonth(month, status)
                                        } else {
                                            room
                                        }
                                    }
                                )
                            )
                        } else {
                            apartment
                        }
                    }
                },
                onDeleteRoom = { apartmentName, floorNumber, roomNumber ->
                    apartments = apartments.map { apartment ->
                        if (apartment.name == apartmentName) {
                            val floorRooms = apartment.roomsByFloor[floorNumber].orEmpty()
                            apartment.copy(
                                roomsByFloor = apartment.roomsByFloor + (
                                    floorNumber to floorRooms.filterNot { it.roomNumber == roomNumber }
                                )
                            )
                        } else {
                            apartment
                        }
                    }
                },
                onReplaceAppState = { importedState ->
                    apartments = importedState.apartments
                    selectedApartmentName = importedState.selectedApartmentName
                }
            )

            AppTab.Building -> BuildingScreen(modifier = contentModifier)
            AppTab.System -> SystemScreen(modifier = contentModifier)
        }
    }
}
