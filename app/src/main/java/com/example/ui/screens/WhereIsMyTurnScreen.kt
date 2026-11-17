package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.QEaseResponsiveContainer
import com.example.ui.components.ResponsiveDialogShell
import com.example.ui.theme.*
import com.example.ui.viewmodel.QEaseViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class QueueStationState {
    PASSED,
    CURRENT,
    UPCOMING
}

data class QueueStation(
    val id: String,
    val title: String,
    val description: String,
    val estimatedTime: String,
    val state: QueueStationState,
    val icon: ImageVector,
    val note: String? = null
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WhereIsMyTurnScreen(
    viewModel: QEaseViewModel,
    onNavigateCentres: () -> Unit
) {
    val activeToken by viewModel.activeUserToken.collectAsState()
    val centres by viewModel.centres.collectAsState()
    val liveUpdates by viewModel.liveQueueUpdates.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    var showBroadcastDialog by remember { mutableStateOf(false) }

    val matchedCentre = remember(activeToken, centres) {
        activeToken?.let { token -> centres.find { it.id == token.centreId } } ?: centres.firstOrNull()
    }

    // Build dynamic stations based on token status
    val stations = remember(activeToken) {
        val status = activeToken?.status ?: TokenStatus.WAITING
        val isCalled = status == TokenStatus.CALLED
        val isServing = status == TokenStatus.SERVING
        val isDone = status == TokenStatus.COMPLETED

        listOf(
            QueueStation(
                id = "st_1",
                title = "1. Token Issued & Verification QR Ready",
                description = "Digital queue token generated. Security QR ready for intake.",
                estimatedTime = "09:02 AM",
                state = QueueStationState.PASSED,
                icon = Icons.Default.QrCodeScanner,
                note = activeToken?.qrVerificationCode ?: "QEASE-TKN-DEMO"
            ),
            QueueStation(
                id = "st_2",
                title = "2. Travel Distance & Transit Radar",
                description = "1.2 km away • Recommended departure: Leave now to arrive 10 min early.",
                estimatedTime = "In Transit",
                state = if (isCalled || isServing || isDone) QueueStationState.PASSED else QueueStationState.CURRENT,
                icon = Icons.Default.DirectionsCar,
                note = "Arrive at Gate 2 Entrance"
            ),
            QueueStation(
                id = "st_3",
                title = "3. Document Verification Checklist",
                description = "Keep original ID and self-attested photocopies handy.",
                estimatedTime = "~5 mins before call",
                state = if (isCalled || isServing || isDone) QueueStationState.PASSED else QueueStationState.UPCOMING,
                icon = Icons.AutoMirrored.Filled.FactCheck
            ),
            QueueStation(
                id = "st_4",
                title = "4. Waiting Hall & Buffer Zone",
                description = "Audio announcements active. Live counter display synced.",
                estimatedTime = "~${activeToken?.estimatedWaitMin ?: 8} mins",
                state = if (isCalled || isServing || isDone) QueueStationState.PASSED else QueueStationState.UPCOMING,
                icon = Icons.Default.Chair
            ),
            QueueStation(
                id = "st_5",
                title = "5. Counter Scrutiny & Officer Desk",
                description = if (activeToken?.counterNumber != null) "Proceed to Counter ${activeToken?.counterNumber} (${activeToken?.counterName})" else "Counter will be assigned shortly",
                estimatedTime = if (isCalled) "NOW CALLING" else "Upcoming",
                state = if (isServing || isDone) QueueStationState.PASSED else if (isCalled) QueueStationState.CURRENT else QueueStationState.UPCOMING,
                icon = Icons.Default.Badge
            ),
            QueueStation(
                id = "st_6",
                title = "6. Biometrics, Photo & Scrutiny",
                description = "Digital fingerprinting, iris capture and officer clearance.",
                estimatedTime = if (isServing) "IN PROGRESS" else "Upcoming",
                state = if (isDone) QueueStationState.PASSED else if (isServing) QueueStationState.CURRENT else QueueStationState.UPCOMING,
                icon = Icons.Default.Fingerprint
            ),
            QueueStation(
                id = "st_7",
                title = "7. Exit Clearance & Dispatch",
                description = "Acknowledgement slip issued with tracking URN.",
                estimatedTime = if (isDone) "COMPLETED" else "Final Step",
                state = if (isDone) QueueStationState.PASSED else QueueStationState.UPCOMING,
                icon = Icons.Default.DoneAll
            )
        )
    }

    Scaffold(
        containerColor = BackgroundPolish,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showBroadcastDialog = true },
                containerColor = DeepForestDark,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("broadcast_live_status_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Report Live Status",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { padding ->
        QEaseResponsiveContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            maxContentWidth = 1120.dp
        ) { metrics ->
            val stackRadarStats = metrics.effectiveWidthDp < 320f || (metrics.isExpanded && metrics.effectiveWidthDp < 740f)

            if (metrics.isExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = metrics.horizontalPadding)
                        .testTag("where_is_my_turn_screen"),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Left Pane: Radar Hero + 7-Station Progression Timeline
                    LazyColumn(
                        modifier = Modifier
                            .weight(1.05f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)
                    ) {
                        item {
                            RadarHeroCard(
                                activeToken = activeToken,
                                syncStatus = syncStatus,
                                stackStats = stackRadarStats,
                                onNavigateCentres = onNavigateCentres
                            )
                        }
                        item {
                            Text(
                                text = "Live Progression Stations",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepForestDark
                            )
                        }
                        items(stations) { station ->
                            QueueStationItem(station = station)
                        }
                    }

                    // Right Pane: Crowdsourced Live Status Feed
                    LazyColumn(
                        modifier = Modifier
                            .weight(0.95f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)
                    ) {
                        item {
                            LiveUpdatesHeader(count = liveUpdates.size)
                        }
                        items(liveUpdates) { update ->
                            LiveUpdateCard(
                                update = update,
                                onUpvote = { viewModel.upvoteLiveUpdate(update.id) }
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = metrics.horizontalPadding)
                        .testTag("where_is_my_turn_screen"),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp)
                ) {
                    item {
                        RadarHeroCard(
                            activeToken = activeToken,
                            syncStatus = syncStatus,
                            stackStats = stackRadarStats,
                            onNavigateCentres = onNavigateCentres
                        )
                    }

                    item {
                        Text(
                            text = "Live Progression Stations",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepForestDark
                        )
                    }

                    items(stations) { station ->
                        QueueStationItem(station = station)
                    }

                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        LiveUpdatesHeader(count = liveUpdates.size)
                    }

                    items(liveUpdates) { update ->
                        LiveUpdateCard(
                            update = update,
                            onUpvote = { viewModel.upvoteLiveUpdate(update.id) }
                        )
                    }
                }
            }
        }
    }

    if (showBroadcastDialog) {
        BroadcastLiveStatusDialog(
            centreId = matchedCentre?.id ?: "centre_central_psk",
            centreName = matchedCentre?.name ?: "Central Passport Seva Kendra",
            onDismiss = { showBroadcastDialog = false },
            onSubmit = { tag, msg, est ->
                viewModel.submitLiveQueueUpdate(
                    centreId = matchedCentre?.id ?: "centre_central_psk",
                    updateTag = tag,
                    message = msg,
                    estimate = est
                )
                showBroadcastDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RadarHeroCard(
    activeToken: QueueToken?,
    syncStatus: String,
    stackStats: Boolean,
    onNavigateCentres: () -> Unit
) {
    Surface(
        color = DeepForestDark,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = PolishGreen,
                        shape = CircleShape,
                        modifier = Modifier.size(10.dp)
                    ) {}
                    Text(
                        text = "LIVE QUEUE RADAR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = SageContainer,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = syncStatus.split("•").firstOrNull() ?: "Live",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (activeToken != null) {
                Text(
                    text = "Token ${activeToken.tokenNumber}",
                    fontSize = 30.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "${activeToken.serviceName} • ${activeToken.centreName}",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (stackRadarStatsOrNot(stackStats)) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadarStatTile(
                            label = "PEOPLE AHEAD",
                            value = "${activeToken.peopleAhead} in front",
                            modifier = Modifier.fillMaxWidth()
                        )
                        RadarStatTile(
                            label = "ESTIMATED TURN",
                            value = "~${activeToken.estimatedWaitMin}-${activeToken.estimatedWaitMax} mins",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RadarStatTile(
                            label = "PEOPLE AHEAD",
                            value = "${activeToken.peopleAhead} in front",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        RadarStatTile(
                            label = "ESTIMATED TURN",
                            value = "~${activeToken.estimatedWaitMin}-${activeToken.estimatedWaitMax} mins",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            } else {
                Text(
                    text = "No Active Queue Token",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Join a virtual queue from any service centre to track your live station-by-station journey.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onNavigateCentres,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SageContainer),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(
                        "Browse Service Centres",
                        color = DeepForestDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

private fun stackRadarStatsOrNot(stackStats: Boolean): Boolean = stackStats

@Composable
private fun RadarStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = SageContainer
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LiveUpdatesHeader(count: Int) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Crowdsourced Live Updates",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = DeepForestDark
        )
        Text(
            text = "$count live reports",
            fontSize = 11.sp,
            color = TextMuted
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QueueStationItem(station: QueueStation) {
    val (lineColor, iconBg, iconFg) = when (station.state) {
        QueueStationState.PASSED -> Triple(PolishGreen, SageContainer, DeepForestDark)
        QueueStationState.CURRENT -> Triple(PolishAmber, PolishAmberBg, PolishAmber)
        QueueStationState.UPCOMING -> Triple(BorderPolish, SurfaceContainerNav, TextMuted)
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = iconBg,
                border = androidx.compose.foundation.BorderStroke(
                    if (station.state == QueueStationState.CURRENT) 2.dp else 1.dp,
                    if (station.state == QueueStationState.CURRENT) PolishAmber else BorderPolish
                ),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = station.icon,
                        contentDescription = null,
                        tint = iconFg,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(42.dp)
                    .background(lineColor)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Surface(
            color = if (station.state == QueueStationState.CURRENT) Color(0xFFF9FDF7) else SurfaceGlass,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(
                if (station.state == QueueStationState.CURRENT) 1.5.dp else 1.dp,
                if (station.state == QueueStationState.CURRENT) PolishGreen else BorderPolish
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = station.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepForestDark,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Surface(
                        color = when (station.state) {
                            QueueStationState.PASSED -> PolishGreenBg
                            QueueStationState.CURRENT -> PolishAmberBg
                            QueueStationState.UPCOMING -> SurfaceContainerNav
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = station.estimatedTime,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (station.state) {
                                QueueStationState.PASSED -> PolishGreen
                                QueueStationState.CURRENT -> PolishAmber
                                QueueStationState.UPCOMING -> TextMuted
                            },
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = station.description,
                    fontSize = 11.sp,
                    color = TextMuted,
                    lineHeight = 15.sp
                )

                if (station.note != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = SageContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "ℹ️ ${station.note}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DeepForestDark,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LiveUpdateCard(
    update: LiveQueueStatusUpdate,
    onUpvote: () -> Unit
) {
    val (tagColor, tagBg, tagText) = when (update.updateTag) {
        "FAST_MOVING" -> Triple(PolishGreen, PolishGreenBg, "⚡ Fast Moving")
        "SLIGHT_DELAY" -> Triple(PolishAmber, PolishAmberBg, "⏳ Slight Delay")
        "DOCS_ACTIVE" -> Triple(DeepForestDark, SageContainer, "📋 Docs Active")
        "COUNTER_ADDED" -> Triple(PolishGreen, PolishGreenBg, "➕ Desk Added")
        else -> Triple(DeepForestDark, SageContainer, "📢 Live Update")
    }

    Surface(
        color = SurfaceGlass,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderPolish),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = tagBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = tagText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = tagColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = update.reporterName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepForestDark
                    )
                }

                Text(
                    text = "Serving ${update.currentServingEstimate}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PolishAmber
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = update.message,
                fontSize = 12.sp,
                color = TextDark,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(update.timestamp)),
                    fontSize = 10.sp,
                    color = TextMuted
                )

                Surface(
                    color = SurfaceContainerNav,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .heightIn(min = 36.dp)
                        .clickable(onClick = onUpvote)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = "Upvote",
                            tint = DeepForestDark,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "${update.upvotes} helpful",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepForestDark
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BroadcastLiveStatusDialog(
    centreId: String,
    centreName: String,
    onDismiss: () -> Unit,
    onSubmit: (tag: String, message: String, servingEstimate: String) -> Unit
) {
    var selectedTag by remember { mutableStateOf("FAST_MOVING") }
    var messageInput by remember { mutableStateOf("") }
    var servingEstimateInput by remember { mutableStateOf("C-042") }

    ResponsiveDialogShell(onDismissRequest = onDismiss) {
        Text(
            text = "Broadcast Live Station Status",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = DeepForestDark
        )
        Text(
            text = centreName,
            fontSize = 11.sp,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text("Select Live Status Tag", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DeepForestDark)
        Spacer(modifier = Modifier.height(6.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "FAST_MOVING" to "⚡ Fast",
                "SLIGHT_DELAY" to "⏳ Delay",
                "DOCS_ACTIVE" to "📋 Docs"
            ).forEach { (tag, label) ->
                val isSelected = selectedTag == tag
                Surface(
                    color = if (isSelected) DeepForestDark else SurfaceContainerNav,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .widthIn(min = 90.dp)
                        .heightIn(min = 38.dp)
                        .clickable { selectedTag = tag }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("Current Token Being Served (Roughly)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DeepForestDark)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = servingEstimateInput,
            onValueChange = { servingEstimateInput = it },
            placeholder = { Text("e.g. C-041", fontSize = 12.sp) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text("Live Observation Note", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DeepForestDark)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = messageInput,
            onValueChange = { messageInput = it },
            placeholder = { Text("e.g. Counter 3 is processing fast. Xerox machine is operational.", fontSize = 12.sp) },
            shape = RoundedCornerShape(10.dp),
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(18.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f, fill = true)
                    .widthIn(min = 100.dp)
                    .heightIn(min = 48.dp)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = {
                    val msg = messageInput.ifBlank { "Everything moving according to live schedule." }
                    onSubmit(selectedTag, msg, servingEstimateInput)
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepForestDark),
                modifier = Modifier
                    .weight(1.4f, fill = true)
                    .widthIn(min = 140.dp)
                    .heightIn(min = 48.dp)
            ) {
                Text("Broadcast Live", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
