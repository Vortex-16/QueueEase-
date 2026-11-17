package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.QEaseViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StaffDeskScreen(
    viewModel: QEaseViewModel,
    modifier: Modifier = Modifier
) {
    val centres by viewModel.centres.collectAsState()
    val counters by viewModel.counters.collectAsState()
    val tokens by viewModel.tokens.collectAsState()
    val reports by viewModel.crowdReports.collectAsState()
    val selectedCentreId by viewModel.selectedStaffCentreId.collectAsState()
    val selectedCounterId by viewModel.selectedStaffCounterId.collectAsState()

    val currentCentre = centres.find { it.id == selectedCentreId } ?: centres.firstOrNull()
    val currentCounter = counters.find { it.id == selectedCounterId } ?: counters.firstOrNull { it.centreId == selectedCentreId }

    val centreTokens = tokens.filter { it.centreId == selectedCentreId }
    val waitingTokens = centreTokens.filter { it.status == TokenStatus.WAITING }
    val activeDeskToken = centreTokens.find {
        it.counterNumber == currentCounter?.counterNumber && (it.status == TokenStatus.CALLED || it.status == TokenStatus.SERVING)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("staff_desk_screen"),
        containerColor = BackgroundPolish,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Column {
                        Text(
                            text = "Staff Desk Console",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = DeepForestDark
                        )
                        Text(
                            text = "${currentCentre?.name ?: "All Centres"} • Counter ${currentCounter?.counterNumber ?: 1}",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.requestLogoutConfirmation() },
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
    ) { padding ->
        QEaseResponsiveContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            maxContentWidth = 1120.dp
        ) { metrics ->
            val columns = metrics.gridColumns
            val waitingRows = remember(waitingTokens, columns) { waitingTokens.chunked(columns) }
            val recentReports = remember(reports) { reports.take(3) }

            if (metrics.isExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = metrics.horizontalPadding),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Left Pane: Desk Control + Current Customer at Desk
                    LazyColumn(
                        modifier = Modifier
                            .weight(0.95f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp)
                    ) {
                        item {
                            DeskControlCard(
                                currentCounter = currentCounter,
                                onToggleCounter = { viewModel.toggleCounter(it) }
                            )
                        }
                        item {
                            CurrentCustomerDeskCard(
                                activeDeskToken = activeDeskToken,
                                waitingCount = waitingTokens.size,
                                onMarkServing = { viewModel.markStaffServing(it) },
                                onComplete = { viewModel.completeStaffService(it) },
                                onNoShow = { viewModel.markStaffNoShow(it) },
                                onCallNext = {
                                    currentCounter?.let {
                                        viewModel.callNextStaffCustomer(it.id, it.assignedStaffName)
                                    }
                                }
                            )
                        }
                    }

                    // Right Pane: Waiting Queue + Citizen Crowd Reports
                    LazyColumn(
                        modifier = Modifier
                            .weight(1.05f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp)
                    ) {
                        item {
                            SectionHeader(
                                title = "Waiting Queue (${waitingTokens.size})",
                                subtitle = "Real-time FIFO queue line",
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
                            items(waitingTokens) { tkn ->
                                WaitingTokenCard(token = tkn)
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            SectionHeader(
                                title = "Citizen Crowd Reports",
                                subtitle = "Verify community inputs to boost signal confidence to 100%",
                                horizontalPadding = 0.dp
                            )
                        }
                        items(recentReports) { rep ->
                            StaffCrowdReportCard(
                                report = rep,
                                onVerify = {
                                    viewModel.verifyStaffReport(rep.id, currentCounter?.assignedStaffName ?: "Staff")
                                }
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    item {
                        DeskControlCard(
                            currentCounter = currentCounter,
                            onToggleCounter = { viewModel.toggleCounter(it) },
                            modifier = Modifier.padding(horizontal = metrics.horizontalPadding, vertical = 6.dp)
                        )
                    }

                    item {
                        CurrentCustomerDeskCard(
                            activeDeskToken = activeDeskToken,
                            waitingCount = waitingTokens.size,
                            onMarkServing = { viewModel.markStaffServing(it) },
                            onComplete = { viewModel.completeStaffService(it) },
                            onNoShow = { viewModel.markStaffNoShow(it) },
                            onCallNext = {
                                currentCounter?.let {
                                    viewModel.callNextStaffCustomer(it.id, it.assignedStaffName)
                                }
                            },
                            modifier = Modifier.padding(horizontal = metrics.horizontalPadding, vertical = 6.dp)
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader(
                            title = "Waiting Queue (${waitingTokens.size})",
                            subtitle = "Real-time FIFO queue line",
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
                                AdaptiveGridRow(
                                    items = rowItems,
                                    columns = columns
                                ) { tkn, cardModifier ->
                                    WaitingTokenCard(
                                        token = tkn,
                                        modifier = cardModifier
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(14.dp))
                        SectionHeader(
                            title = "Citizen Crowd Reports",
                            subtitle = "Verify community inputs to boost signal confidence to 100%",
                            horizontalPadding = metrics.horizontalPadding
                        )
                    }

                    items(recentReports) { rep ->
                        StaffCrowdReportCard(
                            report = rep,
                            onVerify = {
                                viewModel.verifyStaffReport(rep.id, currentCounter?.assignedStaffName ?: "Staff")
                            },
                            modifier = Modifier.padding(horizontal = metrics.horizontalPadding, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeskControlCard(
    currentCounter: Counter?,
    onToggleCounter: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfacePolish),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderPolish),
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
                        text = currentCounter?.name ?: "Counter Desk",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        text = "Operator: ${currentCounter?.assignedStaffName ?: "Staff Officer"}",
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
                        modifier = Modifier.heightIn(min = 40.dp)
                    ) {
                        Text(
                            text = if (cnt.isActive) "STATUS: ACTIVE" else "STATUS: CLOSED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
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
    onCallNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (activeDeskToken != null) SageContainer else SurfacePolish),
        shape = RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderMedium),
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
                        onClick = { onNoShow(activeDeskToken.id) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PolishRed),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PolishRed),
                        modifier = Modifier
                            .weight(1f, fill = true)
                            .widthIn(min = 104.dp)
                            .heightIn(min = 48.dp)
                    ) {
                        Text("No-Show", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Text(
                    text = "Desk is idle. Ready to call next customer.",
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
                    Text("Call Next Waiting Customer ($waitingCount in queue)", fontWeight = FontWeight.Bold)
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
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderPolish),
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
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderPolish),
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
                    modifier = Modifier.heightIn(min = 40.dp)
                ) {
                    Text("Verify as Staff Official (Boost to 100%)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
