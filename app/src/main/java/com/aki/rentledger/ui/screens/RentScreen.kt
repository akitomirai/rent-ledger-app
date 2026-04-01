package com.aki.rentledger.ui.screens

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.aki.rentledger.ApartmentUiState
import com.aki.rentledger.displayValuesForMonth
import com.aki.rentledger.PersistedAppState
import com.aki.rentledger.RentLedgerXlsxTransfer
import com.aki.rentledger.referenceValuesBeforeMonth
import com.aki.rentledger.RoomMonthlyValue
import com.aki.rentledger.RoomCardStatus
import com.aki.rentledger.RoomUiState
import com.aki.rentledger.SAMPLE_APARTMENT_NAME
import com.aki.rentledger.defaultSampleApartment
import com.aki.rentledger.effectiveValuesForMonth
import com.aki.rentledger.nextRoomNumberForFloor
import com.aki.rentledger.statusForMonth
import com.aki.rentledger.updateValuesForMonth
import com.aki.rentledger.valuesForMonth
import java.time.YearMonth
import java.util.Locale
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val HISTORY_EDIT_WINDOW_MILLIS = 5 * 60 * 1000L

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RentScreen(
    apartmentTabResetTick: Int,
    apartments: List<ApartmentUiState>,
    selectedApartmentName: String?,
    onSelectApartment: (String) -> Unit,
    onAddApartment: (String) -> Unit,
    onUpdateApartmentFees: (String, String, String) -> Unit,
    onRenameApartment: (String, String) -> Unit,
    onAddFloor: (String) -> Unit,
    onDeleteFloor: (String, Int) -> Unit,
    onAddRoom: (String, Int) -> Unit,
    onUpdateRoom: (String, Int, RoomUiState) -> Unit,
    onUpdateRoomStatus: (String, Int, Int, YearMonth, RoomCardStatus) -> Unit,
    onDeleteRoom: (String, Int, Int) -> Unit,
    onReplaceAppState: (PersistedAppState) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isCompactPhoneWidth = rememberIsCompactPhoneWidth()
    val pageHorizontalPadding = if (isCompactPhoneWidth) 12.dp else 16.dp
    val pageVerticalPadding = if (isCompactPhoneWidth) 16.dp else 20.dp
    val topCardSpacing = if (isCompactPhoneWidth) 8.dp else 12.dp
    val topCardHeight = if (isCompactPhoneWidth) 124.dp else 136.dp
    val roomGridPadding = if (isCompactPhoneWidth) 14.dp else 20.dp
    val roomGridSpacing = if (isCompactPhoneWidth) 10.dp else 14.dp
    val floorDrawerGestureWidth = if (isCompactPhoneWidth) 26.dp else 30.dp
    val floorDrawerHandleWidth = if (isCompactPhoneWidth) 24.dp else 28.dp
    val floorDrawerHandleHeight = if (isCompactPhoneWidth) 88.dp else 96.dp
    val floorDrawerHandleOffset = if (isCompactPhoneWidth) (-10).dp else (-12).dp
    val floorDrawerWidth = if (isCompactPhoneWidth) 102.dp else 114.dp
    val floorDrawerHeight = if (isCompactPhoneWidth) 388.dp else 420.dp
    val floorDrawerHiddenOffset = -(floorDrawerWidth + if (isCompactPhoneWidth) 12.dp else 18.dp)
    var showApartmentSheet by rememberSaveable { mutableStateOf(false) }
    var newApartmentName by rememberSaveable { mutableStateOf("") }
    var selectedFloorNumber by rememberSaveable { mutableStateOf<Int?>(null) }
    var roomDialogTarget by remember { mutableStateOf<RoomDialogTarget?>(null) }
    var floorPendingDelete by remember { mutableStateOf<Int?>(null) }
    var apartmentPendingEdit by remember { mutableStateOf<ApartmentUiState?>(null) }
    var roomPendingAction by remember { mutableStateOf<RoomUiState?>(null) }
    var roomPendingDelete by remember { mutableStateOf<RoomUiState?>(null) }
    var roomPendingSettlement by remember { mutableStateOf<PendingSettlement?>(null) }
    var settlementDialogBounds by remember { mutableStateOf<Rect?>(null) }
    var settlementDialogView by remember { mutableStateOf<View?>(null) }
    var roomPendingAdd by remember { mutableStateOf(false) }
    var waterFeeInput by rememberSaveable { mutableStateOf("") }
    var electricFeeInput by rememberSaveable { mutableStateOf("") }
    var apartmentNameInput by rememberSaveable { mutableStateOf("") }
    var apartmentNameEditing by rememberSaveable { mutableStateOf(false) }
    var apartmentTransferTarget by remember { mutableStateOf<ApartmentUiState?>(null) }
    var apartmentPendingClear by remember { mutableStateOf<ApartmentUiState?>(null) }
    var apartmentClearConfirmInput by rememberSaveable { mutableStateOf("") }
    var apartmentPendingDelete by remember { mutableStateOf<ApartmentUiState?>(null) }
    var apartmentDeleteConfirmInput by rememberSaveable { mutableStateOf("") }
    var showFloorDrawer by rememberSaveable { mutableStateOf(false) }
    var showMeterEntryPage by rememberSaveable { mutableStateOf(false) }
    var showApartmentOverviewPage by rememberSaveable { mutableStateOf(false) }
    var showCollectionStatusPage by rememberSaveable { mutableStateOf(false) }
    val floorDrawerOffset by animateDpAsState(
        targetValue = if (showFloorDrawer) 0.dp else floorDrawerHiddenOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "floorDrawerOffset"
    )

    val selectedApartment = apartments.firstOrNull { it.name == selectedApartmentName }
    val showTopCardHints = selectedApartmentName == SAMPLE_APARTMENT_NAME && !isCompactPhoneWidth
    val floorNumbers = selectedApartment?.floors.orEmpty()
    val selectedFloorRooms = selectedFloorNumber?.let { floor ->
        selectedApartment?.roomsByFloor?.get(floor).orEmpty()
    }.orEmpty()
    val dialogRoom = roomDialogTarget?.let { target ->
        selectedApartment
            ?.roomsByFloor
            ?.get(target.floorNumber)
            ?.firstOrNull { it.roomNumber == target.roomNumber }
    }
    val currentMonth = YearMonth.now()
    val monthlyRoomCharges = selectedApartment
        ?.roomsByFloor
        .orEmpty()
        .values
        .flatten()
        .map { room ->
            room to calculateRoomCharge(
                room = room,
                month = currentMonth,
                waterUnitPrice = selectedApartment?.waterFee?.toDoubleOrNull() ?: 0.0,
                electricUnitPrice = selectedApartment?.electricFee?.toDoubleOrNull() ?: 0.0
            )
        }
    val totalReceivable = monthlyRoomCharges
        .filter { (room, _) -> countsTowardReceivable(room.statusForMonth(currentMonth)) }
        .sumOf { (_, charge) -> charge }
    val totalReceived = monthlyRoomCharges
        .filter { (room, _) -> room.statusForMonth(currentMonth) == RoomCardStatus.Paid }
        .sumOf { (_, charge) -> charge }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        runCatching {
            val targetApartment = apartmentTransferTarget ?: error("未选择需要导出的公寓")
            RentLedgerXlsxTransfer.exportToUri(
                context = context,
                uri = uri,
                apartments = listOf(targetApartment),
                selectedApartmentName = targetApartment.name
            )
        }.onSuccess {
            apartmentTransferTarget = null
            Toast.makeText(context, "已导出 xlsx 数据", Toast.LENGTH_SHORT).show()
        }.onFailure { throwable ->
            Toast.makeText(
                context,
                buildTransferFailureMessage("导出失败", throwable, "请重试"),
                Toast.LENGTH_LONG
            ).show()
        }
    }
    val apartmentTemplateExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        runCatching {
            RentLedgerXlsxTransfer.exportTemplateToUri(
                context = context,
                uri = uri
            )
        }.onSuccess {
            apartmentTransferTarget = null
            Toast.makeText(context, "已导出导入模板", Toast.LENGTH_SHORT).show()
        }.onFailure { throwable ->
            Toast.makeText(
                context,
                buildTransferFailureMessage("模板导出失败", throwable, "请重试"),
                Toast.LENGTH_LONG
            ).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        runCatching {
            RentLedgerXlsxTransfer.importFromUri(
                context = context,
                uri = uri,
                defaultApartments = emptyList()
            )
        }.onSuccess { importedState ->
            val targetApartment = apartmentTransferTarget ?: error("未选择需要导入的公寓")
            val importedApartment = importedState.apartments
                .firstOrNull { it.name == targetApartment.name }
                ?: importedState.apartments.firstOrNull()
                ?: error("导入文件里没有可用的公寓数据")
            val mergedApartments = apartments.map { apartment ->
                if (apartment.name == targetApartment.name) {
                    importedApartment.copy(name = targetApartment.name)
                } else {
                    apartment
                }
            }
            onReplaceAppState(
                PersistedAppState(
                    apartments = mergedApartments,
                    selectedApartmentName = selectedApartmentName
                )
            )
            apartmentTransferTarget = null
            Toast.makeText(context, "已导入 xlsx 数据", Toast.LENGTH_SHORT).show()
        }.onFailure { throwable ->
            Toast.makeText(
                context,
                buildTransferFailureMessage("导入失败", throwable, "请确认文件格式正确"),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    LaunchedEffect(selectedApartmentName, apartments) {
        selectedFloorNumber = when {
            floorNumbers.isEmpty() -> null
            selectedFloorNumber in floorNumbers -> selectedFloorNumber
            else -> floorNumbers.first()
        }
        if (roomDialogTarget != null && dialogRoom == null) {
            roomDialogTarget = null
        }
    }

    LaunchedEffect(apartmentTabResetTick) {
        showMeterEntryPage = false
        showApartmentOverviewPage = false
        showCollectionStatusPage = false
    }

    LaunchedEffect(apartmentPendingEdit?.name) {
        apartmentNameInput = apartmentPendingEdit?.name.orEmpty()
        apartmentNameEditing = false
    }

    LaunchedEffect(roomPendingSettlement) {
        if (roomPendingSettlement == null) {
            settlementDialogBounds = null
            settlementDialogView = null
        }
    }

    if (showMeterEntryPage) {
        HistoryBillPage(
            modifier = modifier,
            apartment = selectedApartment,
            apartments = apartments,
            selectedApartmentName = selectedApartmentName,
            initialFloorNumber = selectedFloorNumber,
            onUpdateRoom = onUpdateRoom,
            onBack = { showMeterEntryPage = false },
            onReplaceAppState = onReplaceAppState
        )
        return
    }

    if (showCollectionStatusPage) {
        CollectionStatusPage(
            modifier = modifier,
            apartment = selectedApartment,
            apartments = apartments,
            selectedApartmentName = selectedApartmentName,
            onBack = { showCollectionStatusPage = false },
            onUpdateRoomStatus = onUpdateRoomStatus,
            onReplaceAppState = onReplaceAppState
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (roomPendingSettlement != null) Modifier.blur(12.dp) else Modifier)
                .padding(horizontal = pageHorizontalPadding, vertical = pageVerticalPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(topCardSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CollectionSummaryTopCard(
                    modifier = Modifier
                        .weight(0.95f)
                        .height(topCardHeight)
                        .combinedClickable(
                            onClick = {
                                showFloorDrawer = false
                                showCollectionStatusPage = true
                            },
                            onLongClick = {
                                showFloorDrawer = false
                                showCollectionStatusPage = true
                            }
                        ),
                    receivable = formatMoneyTruncated(totalReceivable),
                    received = formatMoneyTruncated(totalReceived),
                    compactLayout = isCompactPhoneWidth
                )

                Card(
                    modifier = Modifier
                        .weight(1.45f)
                        .height(topCardHeight)
                        .combinedClickable(
                            onClick = {
                                showFloorDrawer = false
                                showApartmentSheet = true
                            },
                            onLongClick = {
                                showFloorDrawer = false
                                selectedApartment?.let { apartment ->
                                    waterFeeInput = apartment.waterFee
                                    electricFeeInput = apartment.electricFee
                                    apartmentPendingEdit = apartment
                                } ?: run {
                                    showApartmentSheet = true
                                }
                            }
                        ),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                    ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (showTopCardHints) {
                            Text(
                                text = "\u5207\u6362\u516c\u5bd3",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        Text(
                            text = selectedApartmentName ?: "\u6682\u672a\u9009\u62e9\u516c\u5bd3",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (showTopCardHints) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (selectedApartmentName == null) "\u70b9\u51fb\u9009\u62e9\u516c\u5bd3" else "\u70b9\u51fb\u5207\u6362\u516c\u5bd3",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(0.95f)
                        .height(topCardHeight)
                        .combinedClickable(
                            onClick = {
                                showFloorDrawer = false
                                showMeterEntryPage = true
                            },
                            onLongClick = {
                                showFloorDrawer = false
                                showMeterEntryPage = true
                            }
                        ),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                    ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "\u5386\u53f2\u8d26\u5355",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        if (showTopCardHints) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "\u70b9\u51fb\u6216\u957f\u6309\u67e5\u770b",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            if (false) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { showApartmentSheet = true }
                    ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = selectedApartmentName ?: "\u6682\u672a\u9009\u62e9\u516c\u5bd3",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (selectedApartmentName == null) "\u957f\u6309\u9009\u62e9\u516c\u5bd3" else "\u957f\u6309\u5207\u6362\u516c\u5bd3",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (selectedApartment != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "\u6c34\u4ef7 ${selectedApartment.waterFee}  \u7535\u4ef7 ${selectedApartment.electricFee}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(30.dp),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
                    ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    if (showApartmentOverviewPage) {
                        ApartmentSituationPanel(
                            apartment = selectedApartment,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(roomGridPadding)
                        )
                    } else {
                        LazyVerticalGrid(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(roomGridPadding),
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(roomGridSpacing),
                            verticalArrangement = Arrangement.spacedBy(roomGridSpacing)
                        ) {
                            if (selectedApartment != null && selectedFloorNumber != null && selectedFloorRooms.isNotEmpty()) {
                                val currentFloorNumber = selectedFloorNumber!!
                                items(selectedFloorRooms, key = { it.roomNumber }) { room ->
                                    RoomGridCard(
                                        room = room,
                                        status = room.statusForMonth(currentMonth),
                                        onCardClick = {
                                            showFloorDrawer = false
                                            roomDialogTarget = RoomDialogTarget(
                                                floorNumber = currentFloorNumber,
                                                roomNumber = room.roomNumber
                                            )
                                        },
                                        onCardLongPress = {
                                            showFloorDrawer = false
                                            roomPendingAction = room
                                        }
                                    )
                                }
                            }

                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Button(
                                    onClick = {
                                        showFloorDrawer = false
                                        roomPendingAdd = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = selectedApartment != null && selectedFloorNumber != null
                                ) {
                                    Text(text = "\u65b0\u589e\u623f\u95f4")
                                }
                            }

                            if (selectedApartment != null && selectedFloorNumber != null && selectedFloorRooms.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Text(
                                        text = "\u5f53\u524d\u697c\u5c42\u8fd8\u6ca1\u6709\u623f\u95f4\uff0c\u70b9\u4e0a\u9762\u7684\u6309\u94ae\u65b0\u589e\u4e00\u4e2a\u3002",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (!showFloorDrawer) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(floorDrawerGestureWidth)
                            .align(Alignment.CenterStart)
                            .zIndex(1f)
                            .pointerInput(floorDrawerGestureWidth) {
                                detectHorizontalDragGestures { change, dragAmount ->
                                    if (change.position.x <= floorDrawerGestureWidth.toPx() && dragAmount > 12f) {
                                        showFloorDrawer = true
                                    }
                                }
                            }
                    )
                    DrawerEdgeHandle(
                        modifier = Modifier
                            .width(floorDrawerHandleWidth)
                            .height(floorDrawerHandleHeight)
                            .align(Alignment.CenterStart)
                            .offset(x = floorDrawerHandleOffset)
                            .zIndex(2.2f),
                        onOpen = { showFloorDrawer = true }
                    )
                }

                if (showFloorDrawer) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.08f))
                            .zIndex(1.5f)
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    showFloorDrawer = false
                                }
                            }
                    )
                }

                Surface(
                    modifier = Modifier
                        .width(floorDrawerWidth)
                        .height(floorDrawerHeight)
                        .align(Alignment.CenterStart)
                        .offset(x = floorDrawerOffset)
                        .zIndex(2f)
                        .pointerInput(showFloorDrawer) {
                            detectHorizontalDragGestures { _, dragAmount ->
                                if (showFloorDrawer && dragAmount < -12f) {
                                    showFloorDrawer = false
                                }
                            }
                        },
                    shape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.94f),
                    tonalElevation = 4.dp,
                    shadowElevation = 3.dp
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                    ) {
                        if (selectedApartment != null) {
                            item {
                                FloorNavItem(
                                    label = "\u60c5\u51b5\u9875",
                                    selected = showApartmentOverviewPage,
                                    onClick = {
                                        showFloorDrawer = false
                                        showApartmentOverviewPage = true
                                    },
                                    onLongClick = {
                                        showFloorDrawer = false
                                        showApartmentOverviewPage = true
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                        items(floorNumbers, key = { it }) { floorNumber ->
                            FloorNavItem(
                                label = floorLabel(floorNumber),
                                selected = !showApartmentOverviewPage && selectedFloorNumber == floorNumber,
                                onClick = {
                                    selectedFloorNumber = floorNumber
                                    showApartmentOverviewPage = false
                                    showFloorDrawer = false
                                },
                                onLongClick = { floorPendingDelete = floorNumber }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        item {
                            if (selectedApartment == null) {
                                Text(
                                    text = "\u5148\u9009\u516c\u5bd3",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 14.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Button(
                                    onClick = {
                                        val nextFloorNumber = floorNumbers.maxOrNull()?.plus(1) ?: 2
                                        selectedFloorNumber = nextFloorNumber
                                        showFloorDrawer = false
                                        onAddFloor(selectedApartment.name)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = "\u65b0\u589e\u697c\u5c42")
                                }
                            }
                        }
                    }
                }
            }

            if (roomPendingSettlement != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.08f))
                )
            }
        }
    }

    if (showApartmentSheet) {
        ModalBottomSheet(onDismissRequest = { showApartmentSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "\u9009\u62e9\u516c\u5bd3",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (apartments.isEmpty()) {
                    Text(
                        text = "\u8fd8\u6ca1\u6709\u516c\u5bd3\uff0c\u5148\u5728\u4e0b\u9762\u6dfb\u52a0\u4e00\u4e2a\u3002",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((apartments.size.coerceAtMost(4) * 80).dp)
                    ) {
                        items(apartments, key = { it.name }) { apartment ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (apartment.name == selectedApartmentName) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .combinedClickable(
                                                onClick = {
                                                    onSelectApartment(apartment.name)
                                                    showApartmentSheet = false
                                                    showApartmentOverviewPage = false
                                                },
                                                onLongClick = {
                                                    apartmentPendingEdit = apartment
                                                    waterFeeInput = apartment.waterFee
                                                    electricFeeInput = apartment.electricFee
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(text = apartment.name)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "\u6c34\u4ef7 ${formatPrice(apartment.waterFee)}  \u7535\u4ef7 ${formatPrice(apartment.electricFee)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            apartmentTransferTarget = apartment
                                            showApartmentSheet = false
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Settings,
                                            contentDescription = "${apartment.name}数据导入导出"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = newApartmentName,
                    onValueChange = { newApartmentName = it },
                    label = { Text("\u65b0\u516c\u5bd3\u540d\u79f0") }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val trimmedName = newApartmentName.trim()
                        if (trimmedName.isNotEmpty() && apartments.none { it.name == trimmedName }) {
                            onAddApartment(trimmedName)
                            newApartmentName = ""
                            showApartmentSheet = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "\u6dfb\u52a0\u516c\u5bd3")
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    apartmentTransferTarget?.let { targetApartment ->
        ModalBottomSheet(onDismissRequest = { apartmentTransferTarget = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "${targetApartment.name} 数据导入导出",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "支持获取中文导入模板，并单独导入或导出这个公寓的楼层、房间、状态和月份账单数据（xlsx）",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "导出文件包含：基础资料 / 历史账本 / 应收已收（查看用）",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                OutlinedButton(
                    onClick = {
                        exportLauncher.launch(suggestApartmentExportFileName(targetApartment.name))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "导出这个公寓")
                }
                OutlinedButton(
                    onClick = {
                        apartmentTemplateExportLauncher.launch(
                            RentLedgerXlsxTransfer.suggestedTemplateFileName()
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "获取导入模板")
                }
                Button(
                    onClick = {
                        importLauncher.launch(
                            arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "导入到这个公寓")
                }
                Text(
                    text = "导入会覆盖这个公寓当前的数据，但不会影响其他公寓。",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        apartmentClearConfirmInput = ""
                        apartmentPendingClear = targetApartment
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text(text = "清空这个公寓全部数据")
                }
                Text(
                    text = "会清空这个公寓的水价、电价、楼层、房间、状态和月份账单数据。",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = {
                        apartmentDeleteConfirmInput = ""
                        apartmentPendingDelete = targetApartment
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(text = "删除这个公寓")
                }
                Text(
                    text = "会删除这个公寓本身及它的全部水价、电价、楼层、房间、状态和月份账单数据。",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    apartmentPendingClear?.let { apartment ->
        val canConfirmClear = apartmentClearConfirmInput.trim() == apartment.name
        AlertDialog(
            modifier = Modifier.width(adaptiveDialogWidth(360.dp)),
            onDismissRequest = {
                apartmentPendingClear = null
                apartmentClearConfirmInput = ""
            },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        modifier = Modifier.width(220.dp),
                        text = "确认清空公寓数据",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        modifier = Modifier.width(240.dp),
                        text = "此操作会清空“${apartment.name}”的全部楼层、房间、状态和月份账单数据。",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        modifier = Modifier.width(240.dp),
                        text = "请输入公寓名称确认：${apartment.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    OutlinedTextField(
                        modifier = Modifier.width(220.dp),
                        value = apartmentClearConfirmInput,
                        onValueChange = { apartmentClearConfirmInput = it },
                        singleLine = true,
                        label = { Text(text = "输入公寓名称") }
                    )
                }
            },
            confirmButton = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.width(220.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                apartmentPendingClear = null
                                apartmentClearConfirmInput = ""
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "取消")
                        }
                        Button(
                            onClick = {
                                val clearedApartment = apartment.copy(
                                    waterFee = "0.00",
                                    electricFee = "0.00",
                                    floors = emptyList(),
                                    roomsByFloor = emptyMap()
                                )
                                onReplaceAppState(
                                    PersistedAppState(
                                        apartments = apartments.map { currentApartment ->
                                            if (currentApartment.name == apartment.name) {
                                                clearedApartment
                                            } else {
                                                currentApartment
                                            }
                                        },
                                        selectedApartmentName = selectedApartmentName
                                    )
                                )
                                selectedFloorNumber = null
                                roomDialogTarget = null
                                showFloorDrawer = false
                                showMeterEntryPage = false
                                showApartmentOverviewPage = false
                                showCollectionStatusPage = false
                                apartmentTransferTarget = null
                                apartmentPendingClear = null
                                apartmentClearConfirmInput = ""
                                Toast.makeText(
                                    context,
                                    "已清空 ${apartment.name} 的全部数据",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            enabled = canConfirmClear,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text(text = "确认清空")
                        }
                    }
                }
            },
            dismissButton = {}
        )
    }

    apartmentPendingDelete?.let { apartment ->
        val canConfirmDelete = apartmentDeleteConfirmInput.trim() == apartment.name
        AlertDialog(
            modifier = Modifier.width(adaptiveDialogWidth(360.dp)),
            onDismissRequest = {
                apartmentPendingDelete = null
                apartmentDeleteConfirmInput = ""
            },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        modifier = Modifier.width(220.dp),
                        text = "确认删除公寓",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        modifier = Modifier.width(240.dp),
                        text = "此操作会删除“${apartment.name}”这个公寓，以及它的全部楼层、房间、状态和月份账单数据。",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        modifier = Modifier.width(240.dp),
                        text = "请输入公寓名称确认：${apartment.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    OutlinedTextField(
                        modifier = Modifier.width(220.dp),
                        value = apartmentDeleteConfirmInput,
                        onValueChange = { apartmentDeleteConfirmInput = it },
                        singleLine = true,
                        label = { Text(text = "输入公寓名称") }
                    )
                }
            },
            confirmButton = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.width(220.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                apartmentPendingDelete = null
                                apartmentDeleteConfirmInput = ""
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "取消")
                        }
                        Button(
                            onClick = {
                                val remainingApartments = apartments.filterNot { it.name == apartment.name }
                                val nextSelectedApartmentName = if (selectedApartmentName == apartment.name) {
                                    remainingApartments.firstOrNull()?.name
                                } else {
                                    selectedApartmentName
                                }
                                onReplaceAppState(
                                    PersistedAppState(
                                        apartments = remainingApartments,
                                        selectedApartmentName = nextSelectedApartmentName
                                    )
                                )
                                selectedFloorNumber = null
                                roomDialogTarget = null
                                showFloorDrawer = false
                                showMeterEntryPage = false
                                showApartmentOverviewPage = false
                                showCollectionStatusPage = false
                                apartmentPendingEdit = null
                                apartmentTransferTarget = null
                                apartmentPendingClear = null
                                apartmentClearConfirmInput = ""
                                apartmentPendingDelete = null
                                apartmentDeleteConfirmInput = ""
                                Toast.makeText(
                                    context,
                                    "已删除公寓 ${apartment.name}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            enabled = canConfirmDelete,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text(text = "确认删除")
                        }
                    }
                }
            },
            dismissButton = {}
        )
    }

    apartmentPendingEdit?.let { apartment ->
        AlertDialog(
            modifier = Modifier.width(adaptiveDialogWidth(360.dp)),
            onDismissRequest = {
                apartmentPendingEdit = null
                apartmentNameEditing = false
            },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        modifier = Modifier.width(220.dp),
                        text = "设置公寓信息",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (apartmentNameEditing) {
                        OutlinedTextField(
                            modifier = Modifier.width(240.dp),
                            value = apartmentNameInput,
                            onValueChange = { apartmentNameInput = it },
                            label = { Text("公寓名称") },
                            singleLine = true
                        )
                    } else {
                        Box(
                            modifier = Modifier.pointerInput(apartment.name) {
                                detectTapGestures(
                                    onLongPress = {
                                        apartmentNameEditing = true
                                    }
                                )
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = apartment.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Text(
                        text = "长按公寓名称可修改名称",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    OutlinedTextField(
                        modifier = Modifier.width(240.dp),
                        value = waterFeeInput,
                        onValueChange = { waterFeeInput = it },
                        label = { Text("\u6c34\u4ef7") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        modifier = Modifier.width(240.dp),
                        value = electricFeeInput,
                        onValueChange = { electricFeeInput = it },
                        label = { Text("\u7535\u4ef7") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.width(220.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                apartmentPendingEdit = null
                                apartmentNameEditing = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "\u53d6\u6d88")
                        }
                        Button(
                            onClick = {
                                val trimmedApartmentName = apartmentNameInput.trim()
                                if (trimmedApartmentName.isBlank()) {
                                    Toast.makeText(context, "公寓名称不能为空", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (trimmedApartmentName != apartment.name &&
                                    apartments.any { it.name == trimmedApartmentName }
                                ) {
                                    Toast.makeText(context, "公寓名称已存在，请换一个", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val formattedWaterPrice = formatPriceInput(waterFeeInput)
                                val formattedElectricPrice = formatPriceInput(electricFeeInput)
                                if (trimmedApartmentName != apartment.name) {
                                    onRenameApartment(apartment.name, trimmedApartmentName)
                                }
                                onUpdateApartmentFees(
                                    trimmedApartmentName,
                                    formattedWaterPrice,
                                    formattedElectricPrice
                                )
                                waterFeeInput = formattedWaterPrice
                                electricFeeInput = formattedElectricPrice
                                apartmentNameInput = trimmedApartmentName
                                apartmentNameEditing = false
                                apartmentPendingEdit = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "\u4fdd\u5b58")
                        }
                    }
                }
            },
            dismissButton = {}
        )
    }

    floorPendingDelete?.let { floorNumber ->
        AlertDialog(
            modifier = Modifier.width(adaptiveDialogWidth(320.dp)),
            onDismissRequest = { floorPendingDelete = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        modifier = Modifier.width(220.dp),
                        text = "\u786e\u8ba4\u5220\u9664\u697c\u5c42",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        modifier = Modifier.width(220.dp),
                        text = "\u662f\u5426\u5220\u9664\u201c${floorLabel(floorNumber)}\u201d\uff1f",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.width(220.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { floorPendingDelete = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "\u53d6\u6d88")
                        }
                        Button(
                            onClick = {
                                selectedApartment?.let { apartment ->
                                    val remainingFloors = apartment.floors.filterNot { it == floorNumber }
                                    onDeleteFloor(apartment.name, floorNumber)
                                    if (selectedFloorNumber == floorNumber) {
                                        selectedFloorNumber = remainingFloors.firstOrNull()
                                    }
                                }
                                floorPendingDelete = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "\u786e\u8ba4")
                        }
                    }
                }
            },
            dismissButton = {}
        )
    }

    roomPendingAction?.let { room ->
        val currentRoomStatus = room.statusForMonth(currentMonth)
        ModalBottomSheet(onDismissRequest = { roomPendingAction = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "\u623f\u95f4",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = roomCardColor(currentRoomStatus)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = room.roomNumber.toString(),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                RoomCardStatus.entries.forEach { status ->
                    FilterChip(
                        selected = currentRoomStatus == status,
                        onClick = {
                            selectedApartment?.name?.let { apartmentName ->
                                selectedFloorNumber?.let { floorNumber ->
                                    onUpdateRoomStatus(
                                        apartmentName,
                                        floorNumber,
                                        room.roomNumber,
                                        currentMonth,
                                        status
                                    )
                                }
                            }
                            roomPendingAction = null
                        },
                        label = { Text(statusLabel(status)) }
                    )
                }
                OutlinedButton(
                    onClick = {
                        roomPendingDelete = room
                        roomPendingAction = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = null)
                    Text(text = "\u5220\u9664\u623f\u95f4", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }

    roomPendingDelete?.let { room ->
        AlertDialog(
            modifier = Modifier.width(adaptiveDialogWidth(320.dp)),
            onDismissRequest = { roomPendingDelete = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        modifier = Modifier.width(220.dp),
                        text = "\u786e\u8ba4\u5220\u9664\u623f\u95f4",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        modifier = Modifier.width(220.dp),
                        text = "\u662f\u5426\u5220\u9664\u623f\u95f4\u201c${room.roomNumber}\u201d\uff1f",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.width(220.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { roomPendingDelete = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "\u53d6\u6d88")
                        }
                        Button(
                            onClick = {
                                selectedApartment?.name?.let { apartmentName ->
                                    selectedFloorNumber?.let { floorNumber ->
                                        onDeleteRoom(apartmentName, floorNumber, room.roomNumber)
                                        if (roomDialogTarget?.floorNumber == floorNumber && roomDialogTarget?.roomNumber == room.roomNumber) {
                                            roomDialogTarget = null
                                        }
                                    }
                                }
                                roomPendingDelete = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "\u786e\u8ba4")
                        }
                    }
                }
            },
            dismissButton = {}
        )
    }

    dialogRoom?.let { room ->
        val dialogFloorNumber = roomDialogTarget?.floorNumber
        if (selectedApartment != null && dialogFloorNumber != null) {
            RoomDetailDialog(
                room = room,
                monthValue = room.valuesForMonth(currentMonth),
                placeholderValue = room.referenceValuesBeforeMonth(currentMonth),
                onDismiss = { roomDialogTarget = null },
                onRoomLongPress = {
                    roomDialogTarget = null
                    roomPendingAction = room
                },
                onRentChange = { rent ->
                    onUpdateRoom(
                        selectedApartment.name,
                        dialogFloorNumber,
                        room.updateValuesForMonth(currentMonth) { it.copy(rent = rent) }
                    )
                },
                onWaterMeterChange = { meter ->
                    onUpdateRoom(
                        selectedApartment.name,
                        dialogFloorNumber,
                        room.updateValuesForMonth(currentMonth) { it.copy(waterMeter = meter) }
                    )
                },
                onElectricMeterChange = { meter ->
                    onUpdateRoom(
                        selectedApartment.name,
                        dialogFloorNumber,
                        room.updateValuesForMonth(currentMonth) { it.copy(electricMeter = meter) }
                    )
                },
                onSettleCurrentMonth = {
                    roomDialogTarget = null
                    roomPendingSettlement = PendingSettlement(
                        floorNumber = dialogFloorNumber,
                        room = room
                    )
                }
            )
        }
    }

    roomPendingSettlement?.let { pendingSettlement ->
        val settlementRoom = pendingSettlement.room
        val rawCurrentMonthValue = settlementRoom.valuesForMonth(currentMonth)
        val currentMonthValue = settlementRoom.effectiveValuesForMonth(currentMonth)
        val previousMonthValue = settlementRoom.referenceValuesBeforeMonth(currentMonth)
        val rentAmount = currentMonthValue.rent.toDoubleOrNull() ?: 0.0
        val waterUnitPrice = selectedApartment?.waterFee?.toDoubleOrNull() ?: 0.0
        val electricUnitPrice = selectedApartment?.electricFee?.toDoubleOrNull() ?: 0.0
        val currentWaterMeter = currentMonthValue.waterMeter.toDoubleOrNull() ?: 0.0
        val previousWaterMeter = previousMonthValue.waterMeter.toDoubleOrNull() ?: 0.0
        val currentElectricMeter = currentMonthValue.electricMeter.toDoubleOrNull() ?: 0.0
        val previousElectricMeter = previousMonthValue.electricMeter.toDoubleOrNull() ?: 0.0
        val waterUsage = currentWaterMeter - previousWaterMeter
        val electricUsage = currentElectricMeter - previousElectricMeter
        val missingRent = currentMonthValue.rent.isBlank()
        val missingWaterMeter = rawCurrentMonthValue.waterMeter.isBlank()
        val missingElectricMeter = rawCurrentMonthValue.electricMeter.isBlank()
        val hasIncompleteFields = missingRent || missingWaterMeter || missingElectricMeter
        val invalidPreviousWaterMeter = previousWaterMeter <= 0.0
        val invalidPreviousElectricMeter = previousElectricMeter <= 0.0
        val hasInvalidPreviousMeters = invalidPreviousWaterMeter || invalidPreviousElectricMeter
        val invalidWaterUsage = waterUsage < 0.0
        val invalidElectricUsage = electricUsage < 0.0
        val hasInvalidUsage = invalidWaterUsage || invalidElectricUsage || hasInvalidPreviousMeters
        val waterCharge = waterUsage * waterUnitPrice
        val electricCharge = electricUsage * electricUnitPrice
        val totalCharge = rentAmount + waterCharge + electricCharge
        val settlementWarning = buildString {
            if (hasIncompleteFields) {
                append("请先填写完整后再结算：")
                val missingLabels = buildList {
                    if (missingRent) add("房租")
                    if (missingWaterMeter) add("本月水表")
                    if (missingElectricMeter) add("本月电表")
                }
                append(missingLabels.joinToString("、"))
            }
            if (hasInvalidPreviousMeters) {
                if (isNotEmpty()) append("\n")
                append("检测到")
                val invalidPreviousLabels = buildList {
                    if (invalidPreviousWaterMeter) add("上月水表")
                    if (invalidPreviousElectricMeter) add("上月电表")
                }
                append(invalidPreviousLabels.joinToString("、"))
                append("数据不符常理，请进行数据核查。")
            }
            if (invalidWaterUsage) {
                if (isNotEmpty()) append("\n")
                append("检测到水表数据不符常理，请进行数据核查。")
            }
            if (invalidElectricUsage) {
                if (isNotEmpty()) append("\n")
                append("检测到电表数据不符常理，请进行数据核查。")
            }
        }

        AlertDialog(
            modifier = Modifier
                .width(adaptiveDialogWidth(460.dp, widthFraction = 0.94f))
                .onGloballyPositioned { coordinates ->
                    settlementDialogBounds = coordinates.boundsInWindow()
                },
            onDismissRequest = { roomPendingSettlement = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = {
                val dialogView = LocalView.current
                LaunchedEffect(dialogView) {
                    settlementDialogView = dialogView
                }
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\u7ed3\u7b97",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "\u623f\u95f4 ${settlementRoom.roomNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    if (hasIncompleteFields || hasInvalidUsage) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f)
                        ) {
                            Text(
                                text = settlementWarning,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    SettlementValueRow(
                        label = "\u623f\u79df",
                        value = formatMoney(rentAmount),
                        remark = "${formatMonthLabel(currentMonth)}\u623f\u79df"
                    )
                    SettlementValueRow(
                        label = "\u6c34\u8d39",
                        value = "(${formatNumber(currentWaterMeter)} - ${formatNumber(previousWaterMeter)}) x ${formatNumber(waterUnitPrice)} = ${formatMoney(waterCharge)}",
                        supporting = "\u672c\u6708\u6c34\u8868      \u4e0a\u6708\u6c34\u8868",
                        remark = "\u6c34\u4ef7 ${formatPriceValue(waterUnitPrice)}/\u5428"
                    )
                    SettlementValueRow(
                        label = "\u7535\u8d39",
                        value = "(${formatNumber(currentElectricMeter)} - ${formatNumber(previousElectricMeter)}) x ${formatNumber(electricUnitPrice)} = ${formatMoney(electricCharge)}",
                        supporting = "\u672c\u6708\u7535\u8868      \u4e0a\u6708\u7535\u8868",
                        remark = "\u7535\u4ef7 ${formatPriceValue(electricUnitPrice)}/\u5ea6"
                    )
                    SettlementValueRow(
                        label = "\u5408\u8ba1",
                        value = when {
                            hasIncompleteFields -> "请先填写完整房租和水电数据"
                            hasInvalidPreviousMeters -> "请核查上月水电表数据"
                            hasInvalidUsage -> "请核查水电表数据"
                            else -> formatMoneyTruncated(totalCharge)
                        },
                        remark = "\u5e94\u6536\u5408\u8ba1",
                        emphasize = true
                    )
                }
            },
            confirmButton = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.width(240.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { roomPendingSettlement = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "\u53d6\u6d88")
                        }
                        Button(
                            onClick = {
                                val saveResult = saveSettlementDialogImageToAlbum(
                                    context = context,
                                    sourceView = settlementDialogView,
                                    bounds = settlementDialogBounds,
                                    fileName = buildSettlementImageFileName(
                                        roomNumber = settlementRoom.roomNumber,
                                        month = currentMonth
                                    )
                                )
                                selectedApartment?.name?.let { apartmentName ->
                                    onUpdateRoomStatus(
                                        apartmentName,
                                        pendingSettlement.floorNumber,
                                        settlementRoom.roomNumber,
                                        currentMonth,
                                        RoomCardStatus.Unpaid
                                    )
                                }
                                roomPendingSettlement = null
                                settlementDialogBounds = null
                                settlementDialogView = null
                                saveResult.onSuccess { displayName ->
                                    Toast.makeText(
                                        context,
                                        "已保存结算图片到相册：$displayName",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }.onFailure { throwable ->
                                    Toast.makeText(
                                        context,
                                        buildTransferFailureMessage("图片保存失败", throwable, "请重试"),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !hasIncompleteFields && !hasInvalidUsage
                        ) {
                            Text(text = "\u786e\u8ba4")
                        }
                    }
                }
            },
            dismissButton = {}
        )
    }

    if (roomPendingAdd) {
        AlertDialog(
            modifier = Modifier.width(adaptiveDialogWidth(320.dp)),
            onDismissRequest = { roomPendingAdd = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        modifier = Modifier.width(220.dp),
                        text = "\u786e\u8ba4\u65b0\u589e\u623f\u95f4",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.width(220.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = selectedFloorNumber?.let { floorNumber ->
                                val nextRoomNumber = nextRoomNumberForFloor(
                                    floorNumber = floorNumber,
                                    floorRooms = selectedFloorRooms
                                )
                                "\u662f\u5426\u5728${floorLabel(floorNumber)}\u65b0\u589e\u623f\u95f4\u201c${nextRoomNumber}\u201d\uff1f"
                            } ?: "\u8bf7\u5148\u9009\u62e9\u697c\u5c42",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.width(220.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { roomPendingAdd = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "\u53d6\u6d88")
                        }
                        Button(
                            onClick = {
                                selectedApartment?.name?.let { apartmentName ->
                                    selectedFloorNumber?.let { floorNumber ->
                                        val nextRoomNumber = nextRoomNumberForFloor(
                                            floorNumber = floorNumber,
                                            floorRooms = selectedFloorRooms
                                        )
                                        onAddRoom(apartmentName, floorNumber)
                                        roomDialogTarget = RoomDialogTarget(
                                            floorNumber = floorNumber,
                                            roomNumber = nextRoomNumber
                                        )
                                    }
                                }
                                roomPendingAdd = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "\u786e\u8ba4")
                        }
                    }
                }
            },
            dismissButton = {}
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerEdgeHandle(
    modifier: Modifier = Modifier,
    onOpen: () -> Unit
) {
    Surface(
        modifier = modifier.combinedClickable(
            onClick = onOpen,
            onLongClick = onOpen
        ),
        shape = RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f),
        tonalElevation = 4.dp,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(22.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "楼\n层",
                style = MaterialTheme.typography.labelMedium.copy(lineHeight = 16.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FloorNavItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(32.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ApartmentStatusNavItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onClick
            ),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
        },
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RoomGridCard(
    room: RoomUiState,
    status: RoomCardStatus,
    onCardClick: () -> Unit,
    onCardLongPress: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .combinedClickable(
                onClick = onCardClick,
                onLongClick = onCardLongPress
            ),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
        ),
        colors = CardDefaults.cardColors(containerColor = roomCardColor(status)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = room.roomNumber.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RoomDetailDialog(
    room: RoomUiState,
    monthValue: RoomMonthlyValue,
    placeholderValue: RoomMonthlyValue,
    onDismiss: () -> Unit,
    onRoomLongPress: () -> Unit,
    onRentChange: (String) -> Unit,
    onWaterMeterChange: (String) -> Unit,
    onElectricMeterChange: (String) -> Unit,
    onSettleCurrentMonth: () -> Unit
) {
    val currentStatus = room.statusForMonth(YearMonth.now())
    var rentDraft by remember(room.roomNumber, monthValue.rent) { mutableStateOf(monthValue.rent) }
    var waterMeterDraft by remember(room.roomNumber, monthValue.waterMeter) { mutableStateOf(monthValue.waterMeter) }
    var electricMeterDraft by remember(room.roomNumber, monthValue.electricMeter) { mutableStateOf(monthValue.electricMeter) }
    val waterMeterInvalid = isMeterLowerThanPrevious(waterMeterDraft, placeholderValue.waterMeter)
    val electricMeterInvalid = isMeterLowerThanPrevious(electricMeterDraft, placeholderValue.electricMeter)

    LaunchedEffect(monthValue.rent) {
        if (rentDraft != monthValue.rent) {
            rentDraft = monthValue.rent
        }
    }
    LaunchedEffect(monthValue.waterMeter) {
        if (waterMeterDraft != monthValue.waterMeter) {
            waterMeterDraft = monthValue.waterMeter
        }
    }
    LaunchedEffect(monthValue.electricMeter) {
        if (electricMeterDraft != monthValue.electricMeter) {
            electricMeterDraft = monthValue.electricMeter
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onDismiss() }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {}
                        )
                    },
                colors = CardDefaults.cardColors(containerColor = roomCardColor(currentStatus)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .width(118.dp)
                                .height(118.dp)
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = onRoomLongPress
                                ),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = room.roomNumber.toString(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RoomValueRow(
                                label = "房租",
                                value = rentDraft,
                                placeholder = placeholderValue.rent,
                                onValueChange = {
                                    rentDraft = it
                                    onRentChange(it)
                                }
                            )
                            RoomValueRow(
                                label = "水表",
                                value = waterMeterDraft,
                                placeholder = placeholderValue.waterMeter,
                                isError = waterMeterInvalid,
                                onValueChange = {
                                    waterMeterDraft = it
                                    onWaterMeterChange(it)
                                }
                            )
                            RoomValueRow(
                                label = "电表",
                                value = electricMeterDraft,
                                placeholder = placeholderValue.electricMeter,
                                isError = electricMeterInvalid,
                                onValueChange = {
                                    electricMeterDraft = it
                                    onElectricMeterChange(it)
                                }
                            )
                        }
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = onSettleCurrentMonth,
                            modifier = Modifier.width(220.dp)
                        ) {
                            Text(text = "结算本月")
                        }
                    }
                }
            }
        }
    }
}

private data class PendingSettlement(
    val floorNumber: Int,
    val room: RoomUiState
)

private data class RoomDialogTarget(
    val floorNumber: Int,
    val roomNumber: Int
)

private data class CollectionRoomTarget(
    val floorNumber: Int,
    val room: RoomUiState
)

private enum class ApartmentRoomCategory(val label: String) {
    Vacant("\u672a\u79df"),
    Paid("\u5df2\u79df"),
    Overdue("\u6b20\u8d39"),
    Unpaid("\u672a\u7f34\u8d39")
}

private enum class CollectionRoomCategory(val label: String) {
    Receivable("\u5e94\u6536"),
    Received("\u5df2\u6536"),
    Unreceived("\u672a\u6536"),
    Overdue("\u62d6\u6b20")
}

@Composable
private fun CollectionSummaryTopCard(
    modifier: Modifier = Modifier,
    receivable: String,
    received: String,
    compactLayout: Boolean = false
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (compactLayout) 8.dp else 12.dp,
                    vertical = if (compactLayout) 12.dp else 16.dp
                ),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            SummaryMetricRow(
                label = "\u5e94\u6536",
                value = receivable,
                compactLayout = compactLayout
            )
            Spacer(modifier = Modifier.height(if (compactLayout) 4.dp else 8.dp))
            SummaryMetricRow(
                label = "\u5df2\u6536",
                value = received,
                compactLayout = compactLayout
            )
        }
    }
}

@Composable
private fun SummaryMetricRow(
    label: String,
    value: String,
    compactLayout: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(if (compactLayout) 8.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.width(if (compactLayout) 34.dp else 38.dp),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = if (compactLayout) 13.sp else 14.sp
            ),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start
        )
        SummaryMetricCell(
            modifier = Modifier.weight(1f),
            text = value,
            compactLayout = compactLayout
        )
    }
}

@Composable
private fun SummaryMetricCell(
    modifier: Modifier = Modifier,
    text: String,
    compactLayout: Boolean
) {
    val fontSize = when {
        compactLayout && text.length >= 7 -> 10.sp
        compactLayout && text.length >= 6 -> 11.sp
        compactLayout && text.length >= 5 -> 12.sp
        compactLayout -> 13.sp
        text.length >= 8 -> 12.sp
        text.length >= 6 -> 13.sp
        else -> 15.sp
    }

    Box(
        modifier = modifier.padding(vertical = if (compactLayout) 1.dp else 2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = fontSize),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Start,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CollectionStatusPage(
    apartment: ApartmentUiState?,
    apartments: List<ApartmentUiState>,
    selectedApartmentName: String?,
    onBack: () -> Unit,
    onUpdateRoomStatus: (String, Int, Int, YearMonth, RoomCardStatus) -> Unit,
    onReplaceAppState: (PersistedAppState) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isCompactPhoneWidth = rememberIsCompactPhoneWidth()
    val pageHorizontalPadding = if (isCompactPhoneWidth) 12.dp else 16.dp
    val pageVerticalPadding = if (isCompactPhoneWidth) 16.dp else 20.dp
    val headerSpacing = if (isCompactPhoneWidth) 8.dp else 12.dp
    val headerButtonHeight = if (isCompactPhoneWidth) 50.dp else 56.dp
    val monthControlHeight = if (isCompactPhoneWidth) 46.dp else 50.dp
    val contentPadding = if (isCompactPhoneWidth) 12.dp else 20.dp
    val categoryRailWidth = if (isCompactPhoneWidth) 88.dp else 104.dp
    val roomListSpacing = if (isCompactPhoneWidth) 8.dp else 10.dp
    val roomRowHorizontalPadding = if (isCompactPhoneWidth) 18.dp else 26.dp
    val defaultApartments = remember { listOf(defaultSampleApartment()) }
    var monthOffset by rememberSaveable(apartment?.name) { mutableStateOf(0) }
    var selectedCategory by rememberSaveable(apartment?.name) {
        mutableStateOf(CollectionRoomCategory.Receivable)
    }
    var pendingRoomTarget by remember { mutableStateOf<CollectionRoomTarget?>(null) }
    var showTransferSheet by rememberSaveable { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        runCatching {
            RentLedgerXlsxTransfer.exportToUri(
                context = context,
                uri = uri,
                apartments = apartments,
                selectedApartmentName = selectedApartmentName
            )
        }.onSuccess {
            Toast.makeText(context, "已导出 xlsx 数据", Toast.LENGTH_SHORT).show()
        }.onFailure { throwable ->
            Toast.makeText(
                context,
                buildTransferFailureMessage("导出失败", throwable, "请重试"),
                Toast.LENGTH_LONG
            ).show()
        }
    }
    val templateExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        runCatching {
            RentLedgerXlsxTransfer.exportTemplateToUri(
                context = context,
                uri = uri
            )
        }.onSuccess {
            showTransferSheet = false
            Toast.makeText(context, "已导出导入模板", Toast.LENGTH_SHORT).show()
        }.onFailure { throwable ->
            Toast.makeText(
                context,
                buildTransferFailureMessage("模板导出失败", throwable, "请重试"),
                Toast.LENGTH_LONG
            ).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        runCatching {
            RentLedgerXlsxTransfer.importFromUri(
                context = context,
                uri = uri,
                defaultApartments = defaultApartments
            )
        }.onSuccess { importedState ->
            onReplaceAppState(importedState)
            pendingRoomTarget = null
            showTransferSheet = false
            Toast.makeText(context, "已导入 xlsx 数据", Toast.LENGTH_SHORT).show()
        }.onFailure { throwable ->
            Toast.makeText(
                context,
                buildTransferFailureMessage("导入失败", throwable, "请确认文件格式正确"),
                Toast.LENGTH_LONG
            ).show()
        }
    }
    val displayedMonth = YearMonth.now().minusMonths(monthOffset.toLong())
    val waterUnitPrice = apartment?.waterFee?.toDoubleOrNull() ?: 0.0
    val electricUnitPrice = apartment?.electricFee?.toDoubleOrNull() ?: 0.0
    val roomTargets = apartment?.roomsByFloor.orEmpty()
        .flatMap { (floorNumber, rooms) ->
            rooms.map { room -> CollectionRoomTarget(floorNumber, room) }
        }
        .sortedBy { it.room.roomNumber }
    val filteredTargets = roomTargets.filter { target ->
        val roomStatus = target.room.statusForMonth(displayedMonth)
        when (selectedCategory) {
            CollectionRoomCategory.Receivable -> countsTowardReceivable(roomStatus)
            CollectionRoomCategory.Received -> roomStatus == RoomCardStatus.Paid
            CollectionRoomCategory.Unreceived -> {
                roomStatus == RoomCardStatus.Unfilled || roomStatus == RoomCardStatus.Unpaid
            }
            CollectionRoomCategory.Overdue -> roomStatus == RoomCardStatus.Overdue
        }
    }
    val selectedCategoryTotal = filteredTargets.sumOf { target ->
        calculateRoomCharge(
            room = target.room,
            month = displayedMonth,
            waterUnitPrice = waterUnitPrice,
            electricUnitPrice = electricUnitPrice
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = pageHorizontalPadding, vertical = pageVerticalPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(headerSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.height(headerButtonHeight)
                ) {
                    Text(text = "\u8fd4\u56de")
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(headerButtonHeight),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    shadowElevation = 1.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = formatMonthLabel(displayedMonth),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .height(headerButtonHeight)
                        .width(headerButtonHeight),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    shadowElevation = 1.dp
                ) {
                    IconButton(onClick = { showTransferSheet = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "数据导入导出"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isCompactPhoneWidth) 12.dp else 16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(headerSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { if (monthOffset > 0) monthOffset -= 1 },
                    enabled = monthOffset > 0,
                    modifier = Modifier.height(monthControlHeight)
                ) {
                    Text(text = "\u4e0b\u4e2a\u6708")
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(monthControlHeight),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    tonalElevation = 1.dp,
                    shadowElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = apartment?.name ?: "\u6682\u672a\u9009\u62e9\u516c\u5bd3",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Button(
                    onClick = { monthOffset += 1 },
                    modifier = Modifier.height(monthControlHeight)
                ) {
                    Text(text = "\u4e0a\u4e2a\u6708")
                }
            }

            Spacer(modifier = Modifier.height(if (isCompactPhoneWidth) 12.dp else 16.dp))

            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                if (apartment == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "\u8bf7\u5148\u9009\u62e9\u516c\u5bd3",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding),
                        horizontalArrangement = Arrangement.spacedBy(if (isCompactPhoneWidth) 12.dp else 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.width(categoryRailWidth),
                            verticalArrangement = Arrangement.spacedBy(if (isCompactPhoneWidth) 8.dp else 10.dp)
                        ) {
                            CollectionRoomCategory.entries.forEach { category ->
                                ApartmentStatusNavItem(
                                    label = category.label,
                                    selected = selectedCategory == category,
                                    onClick = { selectedCategory = category }
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(if (isCompactPhoneWidth) 12.dp else 18.dp),
                                verticalArrangement = Arrangement.spacedBy(if (isCompactPhoneWidth) 10.dp else 12.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.large,
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                    tonalElevation = 1.dp,
                                    shadowElevation = 0.dp
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 14.dp, horizontal = 16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "${selectedCategory.label}合计",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = formatMoneyTruncated(selectedCategoryTotal),
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                if (filteredTargets.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${selectedCategory.label}\u6682\u65e0\u623f\u95f4",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(roomListSpacing)
                                    ) {
                                        items(filteredTargets, key = { it.room.roomNumber }) { target ->
                                            val roomStatus = target.room.statusForMonth(displayedMonth)
                                            val roomCharge = calculateRoomCharge(
                                                room = target.room,
                                                month = displayedMonth,
                                                waterUnitPrice = waterUnitPrice,
                                                electricUnitPrice = electricUnitPrice
                                            )
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .combinedClickable(
                                                        onClick = { pendingRoomTarget = target },
                                                        onLongClick = { pendingRoomTarget = target }
                                                    ),
                                                shape = MaterialTheme.shapes.large,
                                                color = roomCardColor(roomStatus)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(
                                                            horizontal = roomRowHorizontalPadding,
                                                            vertical = if (isCompactPhoneWidth) 18.dp else 20.dp
                                                        ),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = target.room.roomNumber.toString(),
                                                        style = MaterialTheme.typography.headlineSmall,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    Text(
                                                        text = formatMoneyTruncated(roomCharge),
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTransferSheet) {
        ModalBottomSheet(onDismissRequest = { showTransferSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "数据导入导出",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "支持获取中文导入模板，并导入或导出当前全部公寓、房间、月份账单数据（xlsx）",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "导出文件包含：基础资料 / 历史账本 / 应收已收（查看用）",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                OutlinedButton(
                    onClick = {
                        showTransferSheet = false
                        exportLauncher.launch(RentLedgerXlsxTransfer.suggestedExportFileName())
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "导出 xlsx")
                }
                OutlinedButton(
                    onClick = {
                        showTransferSheet = false
                        templateExportLauncher.launch(RentLedgerXlsxTransfer.suggestedTemplateFileName())
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "获取导入模板")
                }
                Button(
                    onClick = {
                        importLauncher.launch(
                            arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "导入 xlsx")
                }
                Text(
                    text = "导入会覆盖当前本地数据，请先自行备份。",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    pendingRoomTarget?.let { target ->
        val targetStatus = target.room.statusForMonth(displayedMonth)
        ModalBottomSheet(onDismissRequest = { pendingRoomTarget = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "\u623f\u95f4",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = roomCardColor(targetStatus)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = target.room.roomNumber.toString(),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                RoomCardStatus.entries.forEach { status ->
                    FilterChip(
                        selected = targetStatus == status,
                        onClick = {
                            apartment?.name?.let { apartmentName ->
                                onUpdateRoomStatus(
                                    apartmentName,
                                    target.floorNumber,
                                    target.room.roomNumber,
                                    displayedMonth,
                                    status
                                )
                            }
                            pendingRoomTarget = null
                        },
                        label = { Text(statusLabel(status)) }
                    )
                }
            }
        }
    }

}

@Composable
private fun ApartmentSituationPanel(
    apartment: ApartmentUiState?,
    modifier: Modifier = Modifier
) {
    val currentMonth = YearMonth.now()
    var selectedCategory by rememberSaveable(apartment?.name) {
        mutableStateOf(ApartmentRoomCategory.Vacant)
    }
    val allRooms = apartment?.roomsByFloor.orEmpty()
        .values
        .flatten()
        .sortedBy { it.roomNumber }
    val filteredRooms = allRooms.filter { room ->
        val roomStatus = room.statusForMonth(currentMonth)
        when (selectedCategory) {
            ApartmentRoomCategory.Vacant -> roomStatus == RoomCardStatus.Vacant
            ApartmentRoomCategory.Paid -> roomStatus == RoomCardStatus.Paid
            ApartmentRoomCategory.Overdue -> roomStatus == RoomCardStatus.Overdue
            ApartmentRoomCategory.Unpaid -> {
                roomStatus == RoomCardStatus.Unfilled || roomStatus == RoomCardStatus.Unpaid
            }
        }
    }

    if (apartment == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "\u8bf7\u5148\u9009\u62e9\u516c\u5bd3",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        Row(
            modifier = modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.width(104.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ApartmentRoomCategory.entries.forEach { category ->
                    ApartmentStatusNavItem(
                        label = category.label,
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category }
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                if (filteredRooms.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${selectedCategory.label}\u6682\u65e0\u623f\u95f4",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredRooms, key = { it.roomNumber }) { room ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(0.7f),
                                    shape = MaterialTheme.shapes.large,
                                    color = roomCardColor(room.statusForMonth(currentMonth)).copy(alpha = 0.95f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = room.roomNumber.toString(),
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ApartmentRoomStatusPage(
    apartment: ApartmentUiState?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.height(52.dp)
                ) {
                    Text(text = "\u8fd4\u56de")
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    shadowElevation = 1.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = apartment?.name ?: "\u6682\u672a\u9009\u62e9\u516c\u5bd3",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                ApartmentSituationPanel(
                    apartment = apartment,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun HistoryBillPage(
    apartment: ApartmentUiState?,
    apartments: List<ApartmentUiState>,
    selectedApartmentName: String?,
    initialFloorNumber: Int?,
    onUpdateRoom: (String, Int, RoomUiState) -> Unit,
    onBack: () -> Unit,
    onReplaceAppState: (PersistedAppState) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val isCompactPhoneWidth = rememberIsCompactPhoneWidth()
    val pageHorizontalPadding = if (isCompactPhoneWidth) 12.dp else 16.dp
    val pageVerticalPadding = if (isCompactPhoneWidth) 16.dp else 20.dp
    val headerSpacing = if (isCompactPhoneWidth) 8.dp else 12.dp
    val headerButtonHeight = if (isCompactPhoneWidth) 50.dp else 56.dp
    val pageSectionSpacing = if (isCompactPhoneWidth) 12.dp else 16.dp
    val historyCardPadding = if (isCompactPhoneWidth) 14.dp else 20.dp
    val historyDrawerGestureWidth = if (isCompactPhoneWidth) 26.dp else 30.dp
    val historyDrawerHandleWidth = if (isCompactPhoneWidth) 24.dp else 28.dp
    val historyDrawerHandleHeight = if (isCompactPhoneWidth) 88.dp else 96.dp
    val historyDrawerHandleOffset = if (isCompactPhoneWidth) (-10).dp else (-12).dp
    val historyDrawerWidth = if (isCompactPhoneWidth) 102.dp else 114.dp
    val historyDrawerHeight = if (isCompactPhoneWidth) 388.dp else 420.dp
    val historyDrawerHiddenOffset = -(historyDrawerWidth + if (isCompactPhoneWidth) 12.dp else 18.dp)
    val defaultApartments = remember { listOf(defaultSampleApartment()) }
    val floorNumbers = apartment?.floors.orEmpty()
    var monthOffset by rememberSaveable(apartment?.name) { mutableStateOf(0) }
    var selectedHistoryFloor by rememberSaveable(apartment?.name) {
        mutableStateOf(initialFloorNumber ?: floorNumbers.firstOrNull())
    }
    var showHistoryFloorDrawer by rememberSaveable(apartment?.name) { mutableStateOf(false) }
    var showTransferSheet by rememberSaveable { mutableStateOf(false) }
    var showHistoryEditConfirm by rememberSaveable(apartment?.name) { mutableStateOf(false) }
    var historyEditAccessKey by rememberSaveable(apartment?.name) { mutableStateOf<String?>(null) }
    var historyEditAccessExpiresAt by rememberSaveable(apartment?.name) { mutableStateOf(0L) }
    var historyEditNowMillis by remember(apartment?.name) { mutableStateOf(System.currentTimeMillis()) }
    val displayedMonth = YearMonth.now().minusMonths(monthOffset.toLong())
    val currentMonth = YearMonth.now()
    val isPastMonth = displayedMonth.isBefore(currentMonth)
    val currentHistoryEditKey = apartment?.name.orEmpty()
    val isHistoryEditingEnabled = !isPastMonth || (
        apartment != null &&
            historyEditAccessKey == currentHistoryEditKey &&
            historyEditAccessExpiresAt > historyEditNowMillis
        )
    val historyRooms = selectedHistoryFloor?.let { floorNumber ->
        apartment?.roomsByFloor?.get(floorNumber).orEmpty()
    }.orEmpty()
    val requestHistoryEditAccess = {
        focusManager.clearFocus(force = true)
        if (isPastMonth && !isHistoryEditingEnabled) {
            showHistoryEditConfirm = true
        }
    }
    val historyFloorDrawerOffset by animateDpAsState(
        targetValue = if (showHistoryFloorDrawer) 0.dp else historyDrawerHiddenOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "historyFloorDrawerOffset"
    )
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        runCatching {
            RentLedgerXlsxTransfer.exportToUri(
                context = context,
                uri = uri,
                apartments = apartments,
                selectedApartmentName = selectedApartmentName
            )
        }.onSuccess {
            showTransferSheet = false
            Toast.makeText(context, "\u5df2\u5bfc\u51fa xlsx \u6570\u636e", Toast.LENGTH_SHORT).show()
        }.onFailure { throwable ->
            Toast.makeText(
                context,
                buildTransferFailureMessage("导出失败", throwable, "请重试"),
                Toast.LENGTH_LONG
            ).show()
        }
    }
    val templateExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        runCatching {
            RentLedgerXlsxTransfer.exportTemplateToUri(
                context = context,
                uri = uri
            )
        }.onSuccess {
            showTransferSheet = false
            Toast.makeText(context, "已导出导入模板", Toast.LENGTH_SHORT).show()
        }.onFailure { throwable ->
            Toast.makeText(
                context,
                buildTransferFailureMessage("模板导出失败", throwable, "请重试"),
                Toast.LENGTH_LONG
            ).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        runCatching {
            RentLedgerXlsxTransfer.importFromUri(
                context = context,
                uri = uri,
                defaultApartments = defaultApartments
            )
        }.onSuccess { importedState ->
            onReplaceAppState(importedState)
            showTransferSheet = false
            Toast.makeText(context, "\u5df2\u5bfc\u5165 xlsx \u6570\u636e", Toast.LENGTH_SHORT).show()
        }.onFailure { throwable ->
            Toast.makeText(
                context,
                buildTransferFailureMessage("导入失败", throwable, "请确认文件格式正确"),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    LaunchedEffect(apartment?.name, initialFloorNumber, floorNumbers) {
        selectedHistoryFloor = when {
            floorNumbers.isEmpty() -> null
            selectedHistoryFloor in floorNumbers -> selectedHistoryFloor
            initialFloorNumber in floorNumbers -> initialFloorNumber
            else -> floorNumbers.first()
        }
    }

    LaunchedEffect(currentHistoryEditKey, historyEditAccessKey, historyEditAccessExpiresAt) {
        if (historyEditAccessKey == currentHistoryEditKey && historyEditAccessExpiresAt > 0L) {
            while (true) {
                val now = System.currentTimeMillis()
                historyEditNowMillis = now
                val remaining = historyEditAccessExpiresAt - now
                if (remaining <= 0L) {
                    focusManager.clearFocus(force = true)
                    Toast.makeText(
                        context,
                        "历史账单修改时限已结束，如需继续请重新确认",
                        Toast.LENGTH_SHORT
                    ).show()
                    break
                }
                delay(minOf(1000L, remaining))
            }
        } else {
            historyEditNowMillis = System.currentTimeMillis()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(apartment?.name, selectedHistoryFloor, monthOffset) {
                detectTapGestures {
                    focusManager.clearFocus(force = true)
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = pageHorizontalPadding, vertical = pageVerticalPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(headerSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onBack, modifier = Modifier.height(headerButtonHeight)) {
                    Text(text = "\u8fd4\u56de")
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(headerButtonHeight),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    shadowElevation = 1.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = formatMonthLabel(displayedMonth),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .height(headerButtonHeight)
                        .width(headerButtonHeight),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    shadowElevation = 1.dp
                ) {
                    IconButton(onClick = { showTransferSheet = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "\u5386\u53f2\u8d26\u5355\u5de5\u5177"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(pageSectionSpacing))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(headerSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { if (monthOffset > 0) monthOffset -= 1 },
                    modifier = Modifier.height(headerButtonHeight),
                    enabled = monthOffset > 0
                ) {
                    Text(text = "\u4e0b\u4e2a\u6708")
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(headerButtonHeight),
                    shape = MaterialTheme.shapes.large,
                    color = Color.Transparent,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = apartment?.name ?: "\u6682\u672a\u9009\u62e9\u516c\u5bd3",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Button(
                    onClick = { monthOffset += 1 },
                    modifier = Modifier.height(headerButtonHeight)
                ) {
                    Text(text = "\u4e0a\u4e2a\u6708")
                }
            }
            Spacer(modifier = Modifier.height(pageSectionSpacing))

            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                if (apartment == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "\u8bf7\u5148\u9009\u62e9\u516c\u5bd3",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(historyCardPadding)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    selectedHistoryFloor == null -> {
                                        Text(
                                            text = "\u8bf7\u9009\u62e9\u697c\u5c42",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    historyRooms.isEmpty() -> {
                                        Text(
                                            text = "${floorLabel(selectedHistoryFloor!!)}\u6682\u65e0\u5386\u53f2\u8d26\u5355",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    else -> {
                                        val historyFloorNumber = selectedHistoryFloor!!
                                        LazyVerticalGrid(
                                            modifier = Modifier.fillMaxSize(),
                                            columns = GridCells.Fixed(2),
                                            horizontalArrangement = Arrangement.spacedBy(if (isCompactPhoneWidth) 10.dp else 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(if (isCompactPhoneWidth) 10.dp else 12.dp)
                                        ) {
                                            items(historyRooms, key = { it.roomNumber }) { room ->
                                                val displayedMonthValue = if (displayedMonth == currentMonth) {
                                                    room.valuesForMonth(displayedMonth)
                                                } else {
                                                    room.displayValuesForMonth(displayedMonth)
                                                }
                                                val displayedMonthReference = room.referenceValuesBeforeMonth(displayedMonth)
                                                val displayedMonthPlaceholder = if (displayedMonth == currentMonth) {
                                                    displayedMonthReference
                                                } else {
                                                    RoomMonthlyValue()
                                                }
                                                HistoryBillRow(
                                                    room = room,
                                                    monthValue = displayedMonthValue,
                                                    referenceValue = displayedMonthReference,
                                                    placeholderValue = displayedMonthPlaceholder,
                                                    editable = isHistoryEditingEnabled,
                                                    onRequestEditAccess = requestHistoryEditAccess,
                                                    onRentChange = { rent ->
                                                        if (isHistoryEditingEnabled) {
                                                            onUpdateRoom(
                                                                apartment.name,
                                                                historyFloorNumber,
                                                                room.updateValuesForMonth(displayedMonth) {
                                                                    it.copy(rent = rent)
                                                                }
                                                            )
                                                        } else {
                                                            requestHistoryEditAccess()
                                                        }
                                                    },
                                                    onWaterMeterChange = { waterMeter ->
                                                        if (isHistoryEditingEnabled) {
                                                            onUpdateRoom(
                                                                apartment.name,
                                                                historyFloorNumber,
                                                                room.updateValuesForMonth(displayedMonth) {
                                                                    it.copy(waterMeter = waterMeter)
                                                                }
                                                            )
                                                        } else {
                                                            requestHistoryEditAccess()
                                                        }
                                                    },
                                                    onElectricMeterChange = { electricMeter ->
                                                        if (isHistoryEditingEnabled) {
                                                            onUpdateRoom(
                                                                apartment.name,
                                                                historyFloorNumber,
                                                                room.updateValuesForMonth(displayedMonth) {
                                                                    it.copy(electricMeter = electricMeter)
                                                                }
                                                            )
                                                        } else {
                                                            requestHistoryEditAccess()
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (!showHistoryFloorDrawer) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(historyDrawerGestureWidth)
                                    .align(Alignment.CenterStart)
                                    .zIndex(1f)
                                    .pointerInput(historyDrawerGestureWidth) {
                                        detectHorizontalDragGestures { change, dragAmount ->
                                            if (change.position.x <= historyDrawerGestureWidth.toPx() && dragAmount > 12f) {
                                                showHistoryFloorDrawer = true
                                            }
                                        }
                                    }
                            )
                            DrawerEdgeHandle(
                                modifier = Modifier
                                    .width(historyDrawerHandleWidth)
                                    .height(historyDrawerHandleHeight)
                                    .align(Alignment.CenterStart)
                                    .offset(x = historyDrawerHandleOffset)
                                    .zIndex(2.2f),
                                onOpen = { showHistoryFloorDrawer = true }
                            )
                        }

                        if (showHistoryFloorDrawer) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.08f))
                                    .zIndex(1.5f)
                                    .pointerInput(Unit) {
                                        detectTapGestures {
                                            showHistoryFloorDrawer = false
                                        }
                                    }
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .width(historyDrawerWidth)
                                .height(historyDrawerHeight)
                                .align(Alignment.CenterStart)
                                .offset(x = historyFloorDrawerOffset)
                                .zIndex(2f)
                                .pointerInput(showHistoryFloorDrawer) {
                                    detectHorizontalDragGestures { _, dragAmount ->
                                        if (showHistoryFloorDrawer && dragAmount < -12f) {
                                            showHistoryFloorDrawer = false
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.94f),
                            tonalElevation = 4.dp,
                            shadowElevation = 3.dp
                        ) {
                            if (floorNumbers.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "\u6682\u65e0\u697c\u5c42",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(floorNumbers, key = { it }) { floorNumber ->
                                        FloorNavItem(
                                            label = floorLabel(floorNumber),
                                            selected = selectedHistoryFloor == floorNumber,
                                            onClick = {
                                                selectedHistoryFloor = floorNumber
                                                showHistoryFloorDrawer = false
                                            },
                                            onLongClick = {}
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showTransferSheet) {
            ModalBottomSheet(onDismissRequest = { showTransferSheet = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "\u6570\u636e\u5bfc\u5165\u5bfc\u51fa",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "支持获取中文导入模板，并导入或导出当前全部公寓、房间、月份账单数据（xlsx）",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "导出文件包含：基础资料 / 历史账本 / 应收已收（查看用）",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    OutlinedButton(
                        onClick = {
                            showTransferSheet = false
                            exportLauncher.launch(RentLedgerXlsxTransfer.suggestedExportFileName())
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "\u5bfc\u51fa xlsx")
                    }
                    OutlinedButton(
                        onClick = {
                            showTransferSheet = false
                            templateExportLauncher.launch(RentLedgerXlsxTransfer.suggestedTemplateFileName())
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "获取导入模板")
                    }
                    Button(
                        onClick = {
                            importLauncher.launch(
                                arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "\u5bfc\u5165 xlsx")
                    }
                    Text(
                        text = "\u5bfc\u5165\u4f1a\u8986\u76d6\u5f53\u524d\u672c\u5730\u6570\u636e\uff0c\u8bf7\u5148\u81ea\u884c\u5907\u4efd\u3002",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    if (showHistoryEditConfirm) {
        AlertDialog(
            modifier = Modifier.width(340.dp),
            onDismissRequest = { showHistoryEditConfirm = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        modifier = Modifier.width(240.dp),
                        text = "确认修改历史账单",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        modifier = Modifier.width(250.dp),
                        text = "确认后，你将获得当前公寓历史账单 5 分钟的修改时限，超时后会自动锁定。",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.width(220.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showHistoryEditConfirm = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "取消")
                        }
                        Button(
                            onClick = {
                                historyEditAccessKey = currentHistoryEditKey
                                historyEditAccessExpiresAt = System.currentTimeMillis() + HISTORY_EDIT_WINDOW_MILLIS
                                historyEditNowMillis = System.currentTimeMillis()
                                showHistoryEditConfirm = false
                                Toast.makeText(
                                    context,
                                    "已开启 5 分钟历史账单修改时限",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "确认")
                        }
                    }
                }
            },
            dismissButton = {}
        )
    }

}

@Composable
private fun HistoryBillRow(
    room: RoomUiState,
    monthValue: RoomMonthlyValue,
    referenceValue: RoomMonthlyValue,
    placeholderValue: RoomMonthlyValue,
    editable: Boolean,
    onRequestEditAccess: () -> Unit,
    onRentChange: (String) -> Unit,
    onWaterMeterChange: (String) -> Unit,
    onElectricMeterChange: (String) -> Unit
) {
    val waterMeterInvalid = isMeterLowerThanPrevious(monthValue.waterMeter, referenceValue.waterMeter)
    val electricMeterInvalid = isMeterLowerThanPrevious(monthValue.electricMeter, referenceValue.electricMeter)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = room.roomNumber.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HistoryValueRow(
                    label = "\u623f\u79df",
                    value = monthValue.rent,
                    placeholder = placeholderValue.rent,
                    readOnly = !editable,
                    onValueChange = onRentChange
                )
                HistoryValueRow(
                    label = "\u6c34\u8868",
                    value = monthValue.waterMeter,
                    placeholder = placeholderValue.waterMeter,
                    isError = waterMeterInvalid,
                    readOnly = !editable,
                    onValueChange = onWaterMeterChange
                )
                HistoryValueRow(
                    label = "\u7535\u8868",
                    value = monthValue.electricMeter,
                    placeholder = placeholderValue.electricMeter,
                    isError = electricMeterInvalid,
                    readOnly = !editable,
                    onValueChange = onElectricMeterChange
                )
            }

            if (!editable) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(room.roomNumber) {
                            detectTapGestures(
                                onTap = { onRequestEditAccess() },
                                onLongPress = { onRequestEditAccess() }
                            )
                        }
                )
            }
        }
    }
}

@Composable
private fun HistoryValueRow(
    label: String,
    value: String,
    placeholder: String = "",
    isError: Boolean = false,
    readOnly: Boolean = false,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.width(48.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            UnifiedValueField(
                modifier = Modifier.fillMaxWidth(),
                value = value,
                placeholder = placeholder,
                isError = isError,
                readOnly = readOnly,
                fieldHeight = 54.dp,
                onValueChange = onValueChange
            )
        }
    }
}

@Composable
private fun SettlementValueRow(
    label: String,
    value: String,
    supporting: String? = null,
    remark: String? = null,
    emphasize: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.width(56.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Surface(
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (supporting.isNullOrBlank()) 0.dp else 4.dp)
            ) {
                if (!supporting.isNullOrBlank()) {
                    Text(
                        text = supporting,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                Text(
                    text = value,
                        style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
        if (!remark.isNullOrBlank()) {
            Surface(
                modifier = Modifier.width(84.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = remark,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun RoomValueRow(
    label: String,
    value: String,
    placeholder: String,
    isError: Boolean = false,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.width(58.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        UnifiedValueField(
            modifier = Modifier.weight(1f),
            value = value,
            placeholder = placeholder,
            isError = isError,
            onValueChange = onValueChange
        )
    }
}

@Composable
private fun UnifiedValueField(
    modifier: Modifier = Modifier,
    value: String,
    placeholder: String = "",
    isError: Boolean = false,
    readOnly: Boolean = false,
    fieldHeight: Dp = 66.dp,
    onValueChange: (String) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val displayValue = value

    var fieldValue by remember(displayValue) {
        mutableStateOf(
            TextFieldValue(
                text = displayValue,
                selection = TextRange(displayValue.length)
            )
        )
    }

    LaunchedEffect(displayValue) {
        if (fieldValue.text != displayValue) {
            fieldValue = TextFieldValue(
                text = displayValue,
                selection = TextRange(displayValue.length)
            )
        }
    }

    BasicTextField(
        modifier = modifier.onFocusChanged { focusState ->
            if (focusState.isFocused) {
                if (!isFocused) {
                    isFocused = true
                }
            } else if (isFocused) {
                isFocused = false
                val currentDraft = fieldValue.text
                if (currentDraft.isBlank()) {
                    fieldValue = TextFieldValue(
                        text = "",
                        selection = TextRange.Zero
                    )
                    if (value.isNotBlank()) {
                        onValueChange("")
                    }
                } else {
                    val normalizedValue = normalizeNumericInput(currentDraft)
                    fieldValue = TextFieldValue(
                        text = normalizedValue,
                        selection = TextRange(normalizedValue.length)
                    )
                    if (normalizedValue != value) {
                        onValueChange(normalizedValue)
                    }
                }
            }
        },
        value = fieldValue,
        onValueChange = { updatedFieldValue ->
            fieldValue = updatedFieldValue
            onValueChange(updatedFieldValue.text)
        },
        readOnly = readOnly,
        singleLine = true,
        textStyle = MaterialTheme.typography.titleLarge.copy(
            textAlign = TextAlign.Center,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(fieldHeight)
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (fieldValue.text.isBlank() && placeholder.isNotBlank()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
                            textAlign = TextAlign.Center
                        )
                    }
                    innerTextField()
                }
            }
        }
    )
}

private fun normalizeNumericInput(rawValue: String): String {
    val sanitizedValue = rawValue.trim().filter { it.isDigit() || it == '.' }
    if (sanitizedValue.isEmpty()) {
        return "0"
    }

    val segments = sanitizedValue.split('.')
    val integerPart = segments.firstOrNull().orEmpty().trimStart('0').ifEmpty { "0" }
    val decimalPart = segments.drop(1).joinToString("").filter { it.isDigit() }.trimEnd('0')

    return if (decimalPart.isEmpty()) integerPart else "$integerPart.$decimalPart"
}

private fun isMeterLowerThanPrevious(currentValue: String, previousValue: String): Boolean {
    if (currentValue.isBlank() || previousValue.isBlank()) {
        return false
    }

    val currentNumber = currentValue.toDoubleOrNull() ?: return false
    val previousNumber = previousValue.toDoubleOrNull() ?: return false
    return currentNumber < previousNumber
}

private fun countsTowardReceivable(status: RoomCardStatus): Boolean {
    return status == RoomCardStatus.Paid ||
        status == RoomCardStatus.Unpaid ||
        status == RoomCardStatus.Overdue
}

@Composable
private fun roomCardColor(status: RoomCardStatus): Color {
    return when (status) {
        RoomCardStatus.Paid -> Color(0xFFD8F4EC)
        RoomCardStatus.Unfilled -> Color(0xFFD7E2E8)
        RoomCardStatus.Unpaid -> Color(0xFFD9F0FF)
        RoomCardStatus.Overdue -> Color(0xFFE2D8D2)
        RoomCardStatus.Vacant -> MaterialTheme.colorScheme.surface
    }
}

private fun formatMoney(value: Double): String {
    val roundedValue = if (kotlin.math.abs(value - value.toInt()) < 0.0001) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", value)
    }
    return "\u00A5$roundedValue"
}

private fun formatMoneyTruncated(value: Double): String {
    return "\u00A5${value.toInt()}"
}

@Composable
private fun rememberIsCompactPhoneWidth(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp <= 430
}

@Composable
private fun adaptiveDialogWidth(maxWidth: Dp, widthFraction: Float = 0.88f): Dp {
    val configuration = LocalConfiguration.current
    return (configuration.screenWidthDp.dp * widthFraction).coerceAtMost(maxWidth)
}

private fun formatNumber(value: Double): String {
    return if (kotlin.math.abs(value - value.toInt()) < 0.0001) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", value)
    }
}

private fun formatPrice(value: String): String {
    return formatPriceInput(value)
}

private fun suggestApartmentExportFileName(apartmentName: String): String {
    val sanitizedApartmentName = apartmentName
        .trim()
        .ifBlank { "apartment" }
        .map { character ->
            when {
                character.isLetterOrDigit() -> character
                character == '-' || character == '_' -> character
                else -> '_'
            }
        }
        .joinToString("")

    return "${sanitizedApartmentName}-data.xlsx"
}

private fun formatPriceInput(rawValue: String): String {
    val normalizedValue = normalizeNumericInput(rawValue)
    val numericValue = normalizedValue.toDoubleOrNull() ?: 0.0
    return String.format(Locale.US, "%.2f", numericValue)
}

private fun formatPriceValue(value: Double): String {
    return String.format(Locale.US, "%.2f", value)
}

private fun formatDurationCountdown(durationMillis: Long): String {
    val totalSeconds = (durationMillis / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

private fun buildTransferFailureMessage(
    prefix: String,
    throwable: Throwable,
    fallbackDetail: String
): String {
    val detail = throwable.message?.trim().orEmpty()
    return if (detail.isNotBlank()) {
        "$prefix：$detail"
    } else {
        "$prefix，$fallbackDetail"
    }
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
    return rentAmount + (waterUsage * waterUnitPrice) + (electricUsage * electricUnitPrice)
}

private fun buildSettlementImageFileName(
    roomNumber: Int,
    month: YearMonth
): String {
    return "settlement-${month.year}-${month.monthValue.toString().padStart(2, '0')}-room-$roomNumber.png"
}

private fun saveSettlementDialogImageToAlbum(
    context: android.content.Context,
    sourceView: View?,
    bounds: Rect?,
    fileName: String
): Result<String> = runCatching {
    val dialogView = sourceView ?: error("弹窗尚未准备好")
    val dialogBounds = bounds ?: error("未获取到弹窗范围")
    val rootView = dialogView.rootView
    if (rootView.width <= 0 || rootView.height <= 0) {
        error("当前弹窗还未完成渲染")
    }

    val fullBitmap = Bitmap.createBitmap(
        rootView.width,
        rootView.height,
        Bitmap.Config.ARGB_8888
    )
    val fullCanvas = Canvas(fullBitmap)
    rootView.draw(fullCanvas)

    val left = dialogBounds.left.roundToInt().coerceIn(0, rootView.width - 1)
    val top = dialogBounds.top.roundToInt().coerceIn(0, rootView.height - 1)
    val right = dialogBounds.right.roundToInt().coerceIn(left + 1, rootView.width)
    val bottom = dialogBounds.bottom.roundToInt().coerceIn(top + 1, rootView.height)
    val croppedBitmap = Bitmap.createBitmap(fullBitmap, left, top, right - left, bottom - top)
    fullBitmap.recycle()

    val resolver = context.contentResolver
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/RentLedger")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        ?: error("无法创建图片文件")

    try {
        resolver.openOutputStream(uri)?.use { outputStream ->
            if (!croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                error("图片写入失败")
            }
        } ?: error("无法打开图片输出流")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val completedValues = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            resolver.update(uri, completedValues, null, null)
        }
    } catch (throwable: Throwable) {
        resolver.delete(uri, null, null)
        throw throwable
    } finally {
        croppedBitmap.recycle()
    }

    fileName
}

private fun formatMonthLabel(month: YearMonth): String {
    return "${month.year}\u5e74${month.monthValue.toString().padStart(2, '0')}\u6708"
}

private fun statusLabel(status: RoomCardStatus): String {
    return when (status) {
        RoomCardStatus.Paid -> "\u5df2\u79df\u4e14\u4ea4\u79df"
        RoomCardStatus.Unfilled -> "\u672a\u586b\u5199"
        RoomCardStatus.Unpaid -> "\u5df2\u79df\u672a\u4ea4\u79df"
        RoomCardStatus.Overdue -> "\u5df2\u79df\u62d6\u6b20"
        RoomCardStatus.Vacant -> "\u672a\u79df"
    }
}

private fun floorLabel(floorNumber: Int): String {
    return "${toChineseNumber(floorNumber)}\u697c"
}

private fun toChineseNumber(number: Int): String {
    val digits = listOf("\u96f6", "\u4e00", "\u4e8c", "\u4e09", "\u56db", "\u4e94", "\u516d", "\u4e03", "\u516b", "\u4e5d")
    return when {
        number < 10 -> digits[number]
        number == 10 -> "\u5341"
        number in 11..19 -> "\u5341${digits[number % 10]}"
        number % 10 == 0 -> "${digits[number / 10]}\u5341"
        else -> "${digits[number / 10]}\u5341${digits[number % 10]}"
    }
}
