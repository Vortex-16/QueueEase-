package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.QEaseResponsiveContainer
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.ui.viewmodel.QEaseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminTopBar(
    title: String,
    subtitle: String,
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
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        },
        actions = {
            IconButton(
                onClick = onLogoutClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("admin_logout_button")
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
fun AdminMonitorScreen(
    viewModel: QEaseViewModel,
    modifier: Modifier = Modifier
) {
    val centres by viewModel.centres.collectAsState()
    val counters by viewModel.counters.collectAsState()
    val tokens by viewModel.tokens.collectAsState()
    val systemSettings by viewModel.systemSettings.collectAsState()

    val openCounters = remember(counters) { counters.count { it.isActive } }
    val waitingTotal = remember(tokens) { tokens.count { it.status == TokenStatus.WAITING } }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_monitor_screen"),
        containerColor = BackgroundPolish,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AdminTopBar(
                title = "Live Queue Monitor",
                subtitle = "${centres.size} Centres • $openCounters/${counters.size} Counters Open • $waitingTotal Waiting",
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
            AdminMonitorTab(
                viewModel = viewModel,
                centres = centres,
                counters = counters,
                tokens = tokens,
                systemSettings = systemSettings,
                horizontalPadding = metrics.horizontalPadding
            )
        }
    }
}

@Composable
fun AdminApprovalsScreen(
    viewModel: QEaseViewModel,
    modifier: Modifier = Modifier
) {
    val proposals by viewModel.proposals.collectAsState()
    val pendingCount = remember(proposals) { proposals.count { it.status == ProposalStatus.PENDING } }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_approvals_screen"),
        containerColor = BackgroundPolish,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AdminTopBar(
                title = "Proposal Approvals",
                subtitle = "$pendingCount Pending Review • ${proposals.size} Total Submissions",
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
            AdminApprovalsTab(
                viewModel = viewModel,
                proposals = proposals,
                horizontalPadding = metrics.horizontalPadding
            )
        }
    }
}

@Composable
fun AdminOpsScreen(
    viewModel: QEaseViewModel,
    modifier: Modifier = Modifier
) {
    val services by viewModel.services.collectAsState()
    val centres by viewModel.centres.collectAsState()
    val counters by viewModel.counters.collectAsState()
    val staffAccounts by viewModel.staffAccounts.collectAsState()
    val systemSettings by viewModel.systemSettings.collectAsState()
    val adminSubTab by viewModel.adminSubTab.collectAsState()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_ops_screen"),
        containerColor = BackgroundPolish,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AdminTopBar(
                title = "Operations & Config",
                subtitle = "${staffAccounts.count { it.isActive }} Active Staff • ${centres.size} Centres • ${services.size} Services",
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
                    AdminSubTabChip(
                        label = "Staff",
                        icon = Icons.Default.Groups,
                        selected = adminSubTab == 0,
                        badgeCount = staffAccounts.count { it.isActive },
                        testTag = "admin_subtab_staff",
                        onClick = { viewModel.setAdminSubTab(0) }
                    )
                    AdminSubTabChip(
                        label = "Centres",
                        icon = Icons.Default.Domain,
                        selected = adminSubTab == 1,
                        badgeCount = centres.size,
                        testTag = "admin_subtab_centres",
                        onClick = { viewModel.setAdminSubTab(1) }
                    )
                    AdminSubTabChip(
                        label = "Settings",
                        icon = Icons.Default.Settings,
                        selected = adminSubTab == 2,
                        badgeCount = 0,
                        testTag = "admin_subtab_settings",
                        onClick = { viewModel.setAdminSubTab(2) }
                    )
                }

                when (adminSubTab) {
                    0 -> AdminStaffManagementTab(
                        viewModel = viewModel,
                        staffAccounts = staffAccounts,
                        centres = centres,
                        counters = counters,
                        horizontalPadding = metrics.horizontalPadding
                    )
                    1 -> AdminCentresAndServicesTab(
                        viewModel = viewModel,
                        centres = centres,
                        services = services,
                        columns = metrics.gridColumns,
                        horizontalPadding = metrics.horizontalPadding
                    )
                    else -> AdminSystemSettingsTab(
                        viewModel = viewModel,
                        currentSettings = systemSettings,
                        horizontalPadding = metrics.horizontalPadding
                    )
                }
            }
        }
    }
}

@Composable
fun AdminAnalyticsScreen(
    viewModel: QEaseViewModel,
    modifier: Modifier = Modifier
) {
    val centres by viewModel.centres.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_analytics_screen"),
        containerColor = BackgroundPolish,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AdminTopBar(
                title = "Analytics & Audit",
                subtitle = "${auditLogs.size} Audit Events • Cross-Centre SLA Metrics",
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
            AdminAnalyticsTab(
                viewModel = viewModel,
                centres = centres,
                auditLogs = auditLogs,
                columns = metrics.gridColumns,
                horizontalPadding = metrics.horizontalPadding
            )
        }
    }
}

@Composable
private fun AdminSubTabChip(
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
private fun AdminMonitorTab(
    viewModel: QEaseViewModel,
    centres: List<ServiceCentre>,
    counters: List<Counter>,
    tokens: List<QueueToken>,
    systemSettings: SystemSettings,
    horizontalPadding: Dp
) {
    var selectedCentreId by remember(centres) { mutableStateOf(centres.firstOrNull()?.id ?: "") }
    var noticeText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (systemSettings.maintenanceMode) {
            item {
                Surface(
                    color = PolishRedBg,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, PolishRed.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = PolishRed, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Maintenance Mode Active: ${systemSettings.maintenanceBanner}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolishRed
                        )
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "Live Queue Status Across Centres",
                subtitle = "Real-time waiting load, active desks & serving tokens",
                horizontalPadding = horizontalPadding
            )
        }

        items(centres, key = { it.id }) { centre ->
            val centreTokens = tokens.filter { it.centreId == centre.id }
            val waitingCount = centreTokens.count { it.status == TokenStatus.WAITING }
            val servingTokens = centreTokens.filter { it.status == TokenStatus.CALLED || it.status == TokenStatus.SERVING }
            val centreCounters = counters.filter { it.centreId == centre.id }

            Card(
                colors = CardDefaults.cardColors(containerColor = SurfacePolish),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, BorderPolish),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(centre.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DeepForestDark)
                            Text(
                                text = "$waitingCount Waiting • ${centre.activeCountersCount}/${centre.totalCountersCount} Counters Open • Est ${centre.estimatedWaitMin}-${centre.estimatedWaitMax}m",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                        Surface(
                            color = if (centre.isVirtualQueueOpen) PolishGreenBg else PolishRedBg,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (centre.isVirtualQueueOpen) "QUEUE OPEN" else "PAUSED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (centre.isVirtualQueueOpen) PolishGreen else PolishRed,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (centreCounters.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            centreCounters.forEach { cnt ->
                                Surface(
                                    color = if (cnt.isActive) SageContainer else PolishRedBg,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "C${cnt.counterNumber}: ${cnt.currentServingToken ?: if (cnt.isActive) "Idle" else "Closed"}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (cnt.isActive) DeepForestDark else PolishRed,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (servingTokens.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Active at Desk: ${servingTokens.joinToString { "${it.tokenNumber} (${it.userName})" }}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PolishGreen
                        )
                    }
                }
            }
        }

        // Broadcast Live Facility Notice
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfacePolish),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderPolish),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding, vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "BROADCAST LIVE FACILITY NOTICE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextMuted
                    )
                    if (centres.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            centres.forEach { centre ->
                                val isSelected = centre.id == selectedCentreId
                                Surface(
                                    color = if (isSelected) DeepForestDark else SurfaceContainerNav,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .heightIn(min = 38.dp)
                                        .clickable { selectedCentreId = centre.id }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = centre.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = noticeText,
                        onValueChange = { noticeText = it },
                        placeholder = { Text("e.g. Biometric server maintenance from 4 PM") },
                        colors = qeaseFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (selectedCentreId.isNotBlank()) {
                                viewModel.updateNotice(selectedCentreId, noticeText)
                                noticeText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepForestDark),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text("Broadcast Notice to Centre Citizens", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminApprovalsTab(
    viewModel: QEaseViewModel,
    proposals: List<ServiceChangeProposal>,
    horizontalPadding: Dp
) {
    var statusFilter by remember { mutableStateOf<ProposalStatus?>(ProposalStatus.PENDING) }
    val filtered = remember(proposals, statusFilter) {
        if (statusFilter == null) proposals else proposals.filter { it.status == statusFilter }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = horizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = statusFilter == ProposalStatus.PENDING,
                    onClick = { statusFilter = ProposalStatus.PENDING },
                    label = { Text("Pending (${proposals.count { it.status == ProposalStatus.PENDING }})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                FilterChip(
                    selected = statusFilter == ProposalStatus.APPROVED,
                    onClick = { statusFilter = ProposalStatus.APPROVED },
                    label = { Text("Approved", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = statusFilter == ProposalStatus.REJECTED,
                    onClick = { statusFilter = ProposalStatus.REJECTED },
                    label = { Text("Rejected", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { statusFilter = null },
                    label = { Text("All (${proposals.size})", fontSize = 11.sp) }
                )
            }
        }

        if (filtered.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfacePolish),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, BorderPolish),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding)
                ) {
                    Text(
                        text = "No proposals match this filter. Staff service and document edits appear here for Admin verification.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            items(filtered, key = { it.id }) { prop ->
                AdminProposalReviewCard(
                    proposal = prop,
                    onApprove = { note -> viewModel.approveProposal(prop.id, note) },
                    onReject = { note -> viewModel.rejectProposal(prop.id, note) },
                    modifier = Modifier.padding(horizontal = horizontalPadding)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminProposalReviewCard(
    proposal: ServiceChangeProposal,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var reviewNote by remember(proposal.id) { mutableStateOf("") }
    val (badgeBg, badgeColor) = when (proposal.status) {
        ProposalStatus.PENDING -> PolishAmberBg to PolishAmber
        ProposalStatus.APPROVED -> PolishGreenBg to PolishGreen
        ProposalStatus.REJECTED -> PolishRedBg to PolishRed
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfacePolish),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, BorderPolish),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(proposal.serviceTitle, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DeepForestDark)
                    Text(
                        text = "${proposal.proposalType.label} • By ${proposal.submittedByStaffName} (Counter ${proposal.assignedCounterNumber})",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
                Surface(color = badgeBg, shape = RoundedCornerShape(8.dp)) {
                    Text(
                        text = proposal.status.label.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("Staff Rationale: \"${proposal.rationale}\"", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
            Spacer(modifier = Modifier.height(10.dp))

            // Structured Before -> Proposed Diff Box
            Surface(
                color = SurfaceContainerNav,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderPolish),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (proposal.proposalType == ProposalType.SERVICE_INFO) {
                        if (proposal.previousFee != proposal.proposedFee) {
                            DiffLine("Official Fee", proposal.previousFee, proposal.proposedFee)
                        }
                        if (proposal.previousDurationMin != proposal.proposedDurationMin) {
                            DiffLine("Duration", "${proposal.previousDurationMin}m", "${proposal.proposedDurationMin}m")
                        }
                        if (proposal.previousDescription != proposal.proposedDescription) {
                            DiffLine("Description", proposal.previousDescription, proposal.proposedDescription)
                        }
                        if (proposal.previousCautionNote != proposal.proposedCautionNote) {
                            DiffLine("Caution", proposal.previousCautionNote, proposal.proposedCautionNote)
                        }
                    } else {
                        DiffLine("Document", proposal.previousDocName.ifBlank { "(New)" }, proposal.proposedDocName.ifBlank { "(Removed)" })
                        if (proposal.proposedDocDescription.isNotBlank()) {
                            DiffLine("Notes", proposal.previousDocDescription.ifBlank { "-" }, proposal.proposedDocDescription)
                        }
                        DiffLine(
                            "Requirement",
                            if (proposal.previousDocMandatory) "Mandatory" else "Optional",
                            if (proposal.proposedDocMandatory) "Mandatory" else "Optional"
                        )
                    }
                }
            }

            if (proposal.status == ProposalStatus.PENDING) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = reviewNote,
                    onValueChange = { reviewNote = it },
                    placeholder = { Text("Admin decision note (optional)") },
                    singleLine = true,
                    colors = qeaseFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onReject(reviewNote) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PolishRed),
                        border = BorderStroke(1.dp, PolishRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("btn_reject_proposal_${proposal.id}")
                    ) {
                        Text("Reject", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onApprove(reviewNote) },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepForestDark),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .heightIn(min = 48.dp)
                            .testTag("btn_approve_proposal_${proposal.id}")
                    ) {
                        Text("Approve & Publish", fontWeight = FontWeight.Bold)
                    }
                }
            } else if (proposal.adminReviewNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Reviewed by ${proposal.reviewedByAdminName ?: "Admin"}: ${proposal.adminReviewNote}",
                    fontSize = 11.sp,
                    color = badgeColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun DiffLine(label: String, before: String, after: String) {
    Column {
        Text(label.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
        Text("Before: $before", fontSize = 11.sp, color = TextMuted)
        Text("Proposed: $after", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepForestDark)
    }
}
