package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuditLog
import com.example.data.model.ServiceItem
import com.example.ui.components.AdaptiveGridRow
import com.example.ui.components.QEaseResponsiveContainer
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.ui.viewmodel.QEaseViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdminPanelScreen(
    viewModel: QEaseViewModel,
    modifier: Modifier = Modifier
) {
    val services by viewModel.services.collectAsState()
    val centres by viewModel.centres.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()

    var selectedCentreId by remember(centres) { mutableStateOf(centres.firstOrNull()?.id ?: "") }
    var noticeText by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_panel_screen"),
        containerColor = BackgroundPolish,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Column {
                        Text(
                            text = "System Administration",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = DeepForestDark
                        )
                        Text(
                            text = "Service status, alerts broadcast & audit trails",
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
    ) { padding ->
        QEaseResponsiveContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            maxContentWidth = 1120.dp
        ) { metrics ->
            val columns = metrics.gridColumns
            val serviceRows = remember(services, columns) { services.chunked(columns) }
            val recentLogs = remember(auditLogs) { auditLogs.take(15) }
            val logRows = remember(recentLogs, columns) { recentLogs.chunked(columns) }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                // Live Broadcast Notice Manager
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfacePolish),
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderPolish),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = metrics.horizontalPadding, vertical = 6.dp)
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
                                                .heightIn(min = 36.dp)
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

                // Services Availability Management
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader(
                        title = "Service Availability Controls",
                        subtitle = "Enable or suspend specific citizen services",
                        horizontalPadding = metrics.horizontalPadding
                    )
                }

                items(serviceRows) { rowItems ->
                    Box(modifier = Modifier.padding(horizontal = metrics.horizontalPadding, vertical = 4.dp)) {
                        AdaptiveGridRow(
                            items = rowItems,
                            columns = columns
                        ) { srv, cardModifier ->
                            AdminServiceControlCard(
                                service = srv,
                                onToggle = { viewModel.toggleServiceAvailability(srv.id) },
                                modifier = cardModifier
                            )
                        }
                    }
                }

                // Audit Logs Feed
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    SectionHeader(
                        title = "Deterministic Audit Trail (${auditLogs.size})",
                        subtitle = "Immutable event log of state machine transitions",
                        horizontalPadding = metrics.horizontalPadding
                    )
                }

                items(logRows) { rowItems ->
                    Box(modifier = Modifier.padding(horizontal = metrics.horizontalPadding, vertical = 3.dp)) {
                        AdaptiveGridRow(
                            items = rowItems,
                            columns = columns
                        ) { log, cardModifier ->
                            AuditLogCard(
                                log = log,
                                modifier = cardModifier
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminServiceControlCard(
    service: ServiceItem,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfacePolish),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderPolish),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(service.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Text("Code: ${service.code} • Fee: ${service.officialFee}", fontSize = 11.sp, color = TextMuted)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = service.isAvailable,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = DeepForestDark
                )
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AuditLogCard(
    log: AuditLog,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceVariantPolish,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderPolish),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = log.action,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepForestDark
                )
                Text(
                    text = log.actorName,
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = log.details,
                fontSize = 11.sp,
                color = TextDark
            )
        }
    }
}
