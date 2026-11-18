package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.QEaseViewModel

private data class StaffDeskScopeData(
    val currentUser: UserAccount,
    val currentCentre: ServiceCentre?,
    val currentCounter: Counter?,
    val centreServices: List<ServiceItem>,
    val waitingTokens: List<QueueToken>,
    val completedCount: Int,
    val activeDeskToken: QueueToken?,
    val staffNotifications: List<StaffNotification>
)

@Composable
private fun rememberStaffDeskScopeData(viewModel: QEaseViewModel): StaffDeskScopeData {
    val currentUser by viewModel.currentUser.collectAsState()
    val centres by viewModel.centres.collectAsState()
    val counters by viewModel.counters.collectAsState()
    val services by viewModel.services.collectAsState()
    val tokens by viewModel.tokens.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val selectedCentreId by viewModel.selectedStaffCentreId.collectAsState()
    val selectedCounterId by viewModel.selectedStaffCounterId.collectAsState()

    val lockedCentreId = if (currentUser.role == UserRole.STAFF && !currentUser.assignedCentreId.isNullOrBlank()) {
        currentUser.assignedCentreId!!
    } else {
        selectedCentreId
    }
    val lockedCounterId = if (currentUser.role == UserRole.STAFF && !currentUser.assignedCounterId.isNullOrBlank()) {
        currentUser.assignedCounterId!!
    } else {
        selectedCounterId
    }

    val currentCentre = centres.find { it.id == lockedCentreId } ?: centres.firstOrNull()
    val currentCounter = counters.find { it.id == lockedCounterId }
        ?: counters.firstOrNull { it.centreId == currentCentre?.id }

    val centreServices = remember(services, currentCentre) {
        val ids = currentCentre?.availableServiceIds.orEmpty()
        if (ids.isEmpty()) services else services.filter { ids.contains(it.id) }
    }

    val centreTokens = tokens.filter { it.centreId == currentCentre?.id }
    val waitingTokens = centreTokens
        .filter { it.status == TokenStatus.WAITING }
        .sortedBy { it.joinedAtTimestamp }
    val completedCount = centreTokens.count { it.status == TokenStatus.COMPLETED }
    val activeDeskToken = centreTokens.find {
        it.counterNumber == currentCounter?.counterNumber &&
            (it.status == TokenStatus.CALLED || it.status == TokenStatus.SERVING)
    }

    val staffNotifications = remember(notifications, currentCentre) {
        notifications.filter {
            it.targetRole == UserRole.STAFF &&
                (it.centreId == null || it.centreId == currentCentre?.id)
        }
    }

    return StaffDeskScopeData(
        currentUser = currentUser,
        currentCentre = currentCentre,
        currentCounter = currentCounter,
        centreServices = centreServices,
        waitingTokens = waitingTokens,
        completedCount = completedCount,
        activeDeskToken = activeDeskToken,
        staffNotifications = staffNotifications
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StaffLockedTopBar(
    title: String,
    scopeData: StaffDeskScopeData,
    onLogoutClick: () -> Unit
) {
    TopAppBar(
        windowInsets = WindowInsets(0, 0, 0, 0),
        title = {
            Column {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = DeepForestDark
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Assigned Counter Locked",
                        tint = PolishGreen,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "${scopeData.currentCentre?.name ?: "Desk"} • Counter ${scopeData.currentCounter?.counterNumber ?: 1} (Locked)",
                        fontSize = 11.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        actions = {
            IconButton(
                onClick = onLogoutClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("staff_logout_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Sign Out",
                    tint = PolishRed
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPolish)
    )
}

@Composable
fun StaffDeskScreen(
    viewModel: QEaseViewModel,
    modifier: Modifier = Modifier
) {
    val scopeData = rememberStaffDeskScopeData(viewModel)
    val allServices by viewModel.services.collectAsState()
    var showWalkInDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("staff_desk_screen"),
        containerColor = BackgroundPolish,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            StaffLockedTopBar(
                title = "Counter Desk",
                scopeData = scopeData,
                onLogoutClick = { viewModel.requestLogoutConfirmation() }
            )
        }
    ) { padding ->
        QEaseResponsiveContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            maxContentWidth = 1120.dp
        ) { metrics ->
            StaffQueueDeskTab(
                metrics = metrics,
                currentCentre = scopeData.currentCentre,
                currentCounter = scopeData.currentCounter,
                activeDeskToken = scopeData.activeDeskToken,
                waitingTokens = scopeData.waitingTokens,
                completedCount = scopeData.completedCount,
                officerName = scopeData.currentUser.name.ifBlank { scopeData.currentCounter?.assignedStaffName ?: "Desk Officer" },
                onToggleCounter = { viewModel.toggleCounter(it) },
                onMarkServing = { viewModel.markStaffServing(it) },
                onComplete = { viewModel.completeStaffService(it) },
                onNoShow = { viewModel.markStaffNoShow(it) },
                onRequeue = { viewModel.requeueStaffToken(it, "Citizen temporarily stepped out") },
                onCallNext = {
                    scopeData.currentCounter?.let {
                        viewModel.callNextStaffCustomer(
                            it.id,
                            scopeData.currentUser.name.ifBlank { it.assignedStaffName }
                        )
                    }
                },
                onOpenWalkIn = { showWalkInDialog = true }
            )
        }
    }

    val currentCentre = scopeData.currentCentre
    if (showWalkInDialog && currentCentre != null) {
        WalkInTokenDialog(
            centreName = currentCentre.name,
            services = scopeData.centreServices.ifEmpty { allServices },
            onDismiss = { showWalkInDialog = false },
            onIssue = { serviceId, citizenName, citizenPhone ->
                val res = viewModel.issueWalkInToken(currentCentre.id, serviceId, citizenName, citizenPhone)
                if (res.isSuccess) showWalkInDialog = false
                res.exceptionOrNull()?.message
            }
        )
    }
}

@Composable
fun StaffProposalsScreen(
    viewModel: QEaseViewModel,
    modifier: Modifier = Modifier
) {
    val scopeData = rememberStaffDeskScopeData(viewModel)
    val proposals by viewModel.proposals.collectAsState()
    val staffSubTab by viewModel.staffSubTab.collectAsState()

    val pendingProposalCount = remember(proposals) {
        proposals.count { it.status == ProposalStatus.PENDING }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("staff_proposals_screen"),
        containerColor = BackgroundPolish,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            StaffLockedTopBar(
                title = "Catalog & Doc Proposals",
                scopeData = scopeData,
                onLogoutClick = { viewModel.requestLogoutConfirmation() }
            )
        }
    ) { padding ->
        QEaseResponsiveContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            maxContentWidth = 1120.dp
        ) { metrics ->
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = metrics.horizontalPadding, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StaffSubTabChip(
                        label = "Service Info",
                        icon = Icons.Default.Tune,
                        selected = staffSubTab == 0,
                        badgeCount = pendingProposalCount,
                        testTag = "staff_subtab_services",
                        onClick = { viewModel.setStaffSubTab(0) }
                    )
                    StaffSubTabChip(
                        label = "Required Docs",
                        icon = Icons.AutoMirrored.Filled.FactCheck,
                        selected = staffSubTab == 1,
                        badgeCount = 0,
                        testTag = "staff_subtab_docs",
                        onClick = { viewModel.setStaffSubTab(1) }
                    )
                }

                if (staffSubTab == 0) {
                    StaffServiceInfoTab(
                        viewModel = viewModel,
                        centreServices = scopeData.centreServices,
                        proposals = proposals,
                        assignedCounterNumber = scopeData.currentCounter?.counterNumber ?: 1,
                        horizontalPadding = metrics.horizontalPadding
                    )
                } else {
                    StaffRequiredDocumentsTab(
                        viewModel = viewModel,
                        centreServices = scopeData.centreServices,
                        proposals = proposals,
                        assignedCounterNumber = scopeData.currentCounter?.counterNumber ?: 1,
                        horizontalPadding = metrics.horizontalPadding
                    )
                }
            }
        }
    }
}

@Composable
fun StaffAlertsScreen(
    viewModel: QEaseViewModel,
    modifier: Modifier = Modifier
) {
    val scopeData = rememberStaffDeskScopeData(viewModel)
    val reports by viewModel.crowdReports.collectAsState()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("staff_alerts_screen"),
        containerColor = BackgroundPolish,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            StaffLockedTopBar(
                title = "Desk Alerts & Crowd",
                scopeData = scopeData,
                onLogoutClick = { viewModel.requestLogoutConfirmation() }
            )
        }
    ) { padding ->
        QEaseResponsiveContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            maxContentWidth = 1120.dp
        ) { metrics ->
            StaffAlertsAndCrowdTab(
                horizontalPadding = metrics.horizontalPadding,
                notifications = scopeData.staffNotifications,
                reports = reports,
                officerName = scopeData.currentUser.name.ifBlank { scopeData.currentCounter?.assignedStaffName ?: "Staff" },
                onMarkRead = { viewModel.markNotificationRead(it) },
                onMarkAllRead = {
                    viewModel.markAllNotificationsRead(UserRole.STAFF, scopeData.currentCentre?.id)
                },
                onVerifyReport = { repId, name -> viewModel.verifyStaffReport(repId, name) }
            )
        }
    }
}

@Composable
private fun StaffSubTabChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    badgeCount: Int,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) DeepForestDark else SurfacePolish,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (selected) DeepForestDark else BorderPolish),
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else DeepForestDark,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else DeepForestDark
            )
            if (badgeCount > 0) {
                Surface(
                    color = if (selected) SageContainer else PolishAmberBg,
                    shape = CircleShape
                ) {
                    Text(
                        text = badgeCount.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = if (selected) DeepForestDark else PolishAmber,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StaffQueueDeskTab(
    metrics: QEaseLayoutMetrics,
    currentCentre: ServiceCentre?,
    currentCounter: Counter?,
    activeDeskToken: QueueToken?,
    waitingTokens: List<QueueToken>,
    completedCount: Int,
    officerName: String,
    onToggleCounter: (String) -> Unit,
    onMarkServing: (String) -> Unit,
    onComplete: (String) -> Unit,
    onNoShow: (String) -> Unit,
    onRequeue: (String) -> Unit,
    onCallNext: () -> Unit,
    onOpenWalkIn: () -> Unit
) {
    val columns = metrics.gridColumns
    val waitingRows = remember(waitingTokens, columns) { waitingTokens.chunked(columns) }

    if (metrics.isExpanded) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = metrics.horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
            ) {
                item {
                    DeskControlCard(
                        currentCentre = currentCentre,
                        currentCounter = currentCounter,
                        officerName = officerName,
                        waitingCount = waitingTokens.size,
                        completedCount = completedCount,
                        onToggleCounter = onToggleCounter
                    )
                }
                item {
                    CurrentCustomerDeskCard(
                        activeDeskToken = activeDeskToken,
                        waitingCount = waitingTokens.size,
                        onMarkServing = onMarkServing,
                        onComplete = onComplete,
                        onNoShow = onNoShow,
                        onRequeue = onRequeue,
                        onCallNext = onCallNext,
                        onOpenWalkIn = onOpenWalkIn
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1.05f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
            ) {
                item {
                    SectionHeader(
                        title = "Waiting Queue (${waitingTokens.size})",
                        subtitle = "FIFO tokens scoped to ${currentCentre?.name ?: "assigned centre"}",
                        horizontalPadding = 0.dp
                    )
                }
                if (waitingTokens.isEmpty()) {
                    item {
                        Text(
                            text = "No citizens currently waiting in virtual queue.",
                            fontSize = 12.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                } else {
                    items(waitingTokens, key = { it.id }) { tkn ->
                        WaitingTokenCard(token = tkn)
                    }
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item {
                DeskControlCard(
                    currentCentre = currentCentre,
                    currentCounter = currentCounter,
                    officerName = officerName,
                    waitingCount = waitingTokens.size,
                    completedCount = completedCount,
                    onToggleCounter = onToggleCounter,
                    modifier = Modifier.padding(horizontal = metrics.horizontalPadding, vertical = 6.dp)
                )
            }

            item {
                CurrentCustomerDeskCard(
                    activeDeskToken = activeDeskToken,
                    waitingCount = waitingTokens.size,
                    onMarkServing = onMarkServing,
                    onComplete = onComplete,
                    onNoShow = onNoShow,
                    onRequeue = onRequeue,
                    onCallNext = onCallNext,
                    onOpenWalkIn = onOpenWalkIn,
                    modifier = Modifier.padding(horizontal = metrics.horizontalPadding, vertical = 6.dp)
                )
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                SectionHeader(
                    title = "Waiting Queue (${waitingTokens.size})",
                    subtitle = "FIFO queue for ${currentCentre?.name ?: "Assigned Desk"}",
                    horizontalPadding = metrics.horizontalPadding
                )
            }

            if (waitingTokens.isEmpty()) {
                item {
                    Text(
                        text = "No citizens currently waiting in virtual queue.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = metrics.horizontalPadding, vertical = 8.dp)
                    )
                }
            } else {
                items(waitingRows) { rowItems ->
                    Box(modifier = Modifier.padding(horizontal = metrics.horizontalPadding, vertical = 4.dp)) {
                        AdaptiveGridRow(items = rowItems, columns = columns) { tkn, cardModifier ->
                            WaitingTokenCard(token = tkn, modifier = cardModifier)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StaffAlertsAndCrowdTab(
    horizontalPadding: androidx.compose.ui.unit.Dp,
    notifications: List<StaffNotification>,
    reports: List<CrowdReport>,
    officerName: String,
    onMarkRead: (String) -> Unit,
    onMarkAllRead: () -> Unit,
    onVerifyReport: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Incoming Desk Notifications", fontSize = 15.sp, fontWeight = FontWeight.Black, color = DeepForestDark)
                    Text("Queue joins, Admin approvals & broadcasts", fontSize = 11.sp, color = TextMuted)
                }
                if (notifications.any { !it.isRead }) {
                    TextButton(
                        onClick = onMarkAllRead,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Mark All Read", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PolishGreen)
                    }
                }
            }
        }

        if (notifications.isEmpty()) {
            item {
                Text(
                    text = "No incoming notifications for your counter.",
                    fontSize = 12.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(horizontal = horizontalPadding)
                )
            }
        } else {
            items(notifications, key = { it.id }) { notif ->
                val (bg, accent) = when (notif.severity) {
                    NotificationSeverity.ACTION_REQUIRED -> PolishAmberBg to PolishAmber
                    NotificationSeverity.APPROVED -> PolishGreenBg to PolishGreen
                    NotificationSeverity.ALERT -> PolishRedBg to PolishRed
                    NotificationSeverity.INFO -> SurfaceContainerNav to DeepForestDark
                }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (notif.isRead) SurfacePolish else bg.copy(alpha = 0.65f)
                    ),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, if (notif.isRead) BorderPolish else accent.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding)
                        .clickable { onMarkRead(notif.id) }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = notif.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepForestDark,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    text = if (notif.isRead) "READ" else notif.severity.label.uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accent,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(notif.message, fontSize = 12.sp, color = TextDark)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            SectionHeader(
                title = "Citizen Crowd Reports (${reports.size})",
                subtitle = "Verify field inputs to lock telemetry confidence to 100%",
                horizontalPadding = horizontalPadding
            )
        }

        items(reports, key = { it.id }) { rep ->
            StaffCrowdReportCard(
                report = rep,
                onVerify = { onVerifyReport(rep.id, officerName) },
                modifier = Modifier.padding(horizontal = horizontalPadding)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeskControlCard(
    currentCentre: ServiceCentre?,
    currentCounter: Counter?,
    officerName: String,
    waitingCount: Int,
    completedCount: Int,
    onToggleCounter: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfacePolish),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, BorderPolish),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = currentCounter?.name ?: "Assigned Counter Desk",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        text = "$officerName • ${currentCentre?.name ?: "Centre"}",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                currentCounter?.let { cnt ->
                    Button(
                        onClick = { onToggleCounter(cnt.id) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (cnt.isActive) SageContainer else PolishRedBg,
                            contentColor = if (cnt.isActive) DeepForestDark else PolishRed
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .testTag("btn_toggle_counter")
                    ) {
                        Text(
                            text = if (cnt.isActive) "COUNTER ACTIVE" else "COUNTER PAUSED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = SurfaceContainerNav,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("WAITING", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Text("$waitingCount", fontSize = 16.sp, fontWeight = FontWeight.Black, color = DeepForestDark)
                    }
                }
                Surface(
                    color = SurfaceContainerNav,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("SERVED TODAY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Text("$completedCount", fontSize = 16.sp, fontWeight = FontWeight.Black, color = PolishGreen)
                    }
                }
                Surface(
                    color = SurfaceContainerNav,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("LOCKED DESK", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Text("#${currentCounter?.counterNumber ?: 1}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = DeepForestDark)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CurrentCustomerDeskCard(
    activeDeskToken: QueueToken?,
    waitingCount: Int,
    onMarkServing: (String) -> Unit,
    onComplete: (String) -> Unit,
    onNoShow: (String) -> Unit,
    onRequeue: (String) -> Unit,
    onCallNext: () -> Unit,
    onOpenWalkIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (activeDeskToken != null) SageContainer else SurfacePolish),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, BorderMedium),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "CURRENT CUSTOMER AT DESK",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = if (activeDeskToken != null) DeepForestDark.copy(alpha = 0.7f) else TextMuted
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (activeDeskToken != null) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = activeDeskToken.tokenNumber,
                            fontSize = 36.sp,
                            lineHeight = 40.sp,
                            fontWeight = FontWeight.Black,
                            color = DeepForestDark
                        )
                        Text(
                            text = "${activeDeskToken.userName} • ${activeDeskToken.serviceName}",
                            fontSize = 12.sp,
                            color = TextDark,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    TokenStatusBadge(status = activeDeskToken.status)
                }

                Spacer(modifier = Modifier.height(16.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (activeDeskToken.status == TokenStatus.CALLED) {
                        Button(
                            onClick = { onMarkServing(activeDeskToken.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepForestDark),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f, fill = true)
                                .widthIn(min = 104.dp)
                                .heightIn(min = 48.dp)
                        ) {
                            Text("Mark Serving", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = { onComplete(activeDeskToken.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = PolishGreen),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f, fill = true)
                            .widthIn(min = 104.dp)
                            .heightIn(min = 48.dp)
                    ) {
                        Text("Complete", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { onRequeue(activeDeskToken.id) },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, DeepForestMedium),
                        modifier = Modifier
                            .weight(1f, fill = true)
                            .widthIn(min = 96.dp)
                            .heightIn(min = 48.dp)
                    ) {
                        Text("Re-queue", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DeepForestDark)
                    }

                    OutlinedButton(
                        onClick = { onNoShow(activeDeskToken.id) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PolishRed),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, PolishRed),
                        modifier = Modifier
                            .weight(1f, fill = true)
                            .widthIn(min = 96.dp)
                            .heightIn(min = 48.dp)
                    ) {
                        Text("No-Show", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Text(
                    text = "Desk is idle. Call next citizen or register a walk-in token.",
                    fontSize = 13.sp,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onCallNext,
                    colors = ButtonDefaults.buttonColors(containerColor = DeepForestDark),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("btn_call_next_customer")
                ) {
                    Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Call Next Waiting Customer ($waitingCount)", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onOpenWalkIn,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, DeepForestMedium),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("btn_issue_walkin_token")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = DeepForestDark, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Issue Walk-In Citizen Token", fontWeight = FontWeight.Bold, color = DeepForestDark)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WaitingTokenCard(
    token: QueueToken,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfacePolish),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, BorderPolish),
        modifier = modifier.fillMaxWidth()
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(SageContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${token.positionInQueue}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepForestDark
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(token.tokenNumber, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Text("${token.userName} • ${token.serviceName}", fontSize = 11.sp, color = TextMuted)
                }
            }
            Text(
                text = "~${token.estimatedWaitMin}m wait",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = PolishGreen
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StaffCrowdReportCard(
    report: CrowdReport,
    onVerify: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfacePolish),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, BorderPolish),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = report.centreName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    modifier = Modifier.weight(1f, fill = false)
                )
                CrowdBadge(level = report.crowdLevel, confidencePercent = report.confidenceScore)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Reported: ~${report.approxPeopleCount} people • ${report.reportedWaitMinutes}m wait • \"${report.notes}\"",
                fontSize = 11.sp,
                color = TextMuted
            )
            if (!report.isStaffVerified) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onVerify,
                    colors = ButtonDefaults.buttonColors(containerColor = DeepForestDark),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.heightIn(min = 44.dp)
                ) {
                    Text("Verify as Staff Official (Boost to 100%)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
