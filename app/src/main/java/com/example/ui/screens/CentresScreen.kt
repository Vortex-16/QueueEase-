package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.QEaseViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CentresScreen(
    viewModel: QEaseViewModel,
    modifier: Modifier = Modifier
) {
    val centres by viewModel.centres.collectAsState()
    val services by viewModel.services.collectAsState()
    val savedIds by viewModel.savedCentreIds.collectAsState()
    val comparisonIds by viewModel.comparisonCentreIds.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showJoinDialog by remember { mutableStateOf<ServiceCentre?>(null) }
    var showReportDialog by remember { mutableStateOf<ServiceCentre?>(null) }
    var showComparisonSheet by remember { mutableStateOf(false) }

    val filteredCentres = remember(centres, searchQuery, selectedCategory) {
        centres.filter { centre ->
            val matchesQuery = searchQuery.isBlank() ||
                    centre.name.contains(searchQuery, ignoreCase = true) ||
                    centre.address.contains(searchQuery, ignoreCase = true)
            val matchesCat = selectedCategory == null || centre.category == selectedCategory
            matchesQuery && matchesCat
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("centres_screen"),
        containerColor = BackgroundPolish,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Column {
                        Text(
                            text = "Service Centres",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = DeepForestDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${filteredCentres.size} facilities with live queue tracking",
                            fontSize = 11.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    if (comparisonIds.size >= 2) {
                        IconButton(
                            onClick = { showComparisonSheet = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("btn_open_compare")
                        ) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = DeepForestDark,
                                        contentColor = Color.White
                                    ) {
                                        Text("${comparisonIds.size}")
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                                    contentDescription = "Compare Centres",
                                    tint = DeepForestDark
                                )
                            }
                        }
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
            val cardAvailableDp = ((metrics.availableWidth.value.coerceAtMost(1120f) -
                metrics.horizontalPadding.value * 2f - 12f * (columns - 1)) / columns)
            val isTightCard = (cardAvailableDp / metrics.fontScale.coerceAtLeast(1f)) < 330f

            val chunkedCentres = remember(filteredCentres, columns) {
                filteredCentres.chunked(columns)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                // Search & Category Filters
                item {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = metrics.horizontalPadding,
                            vertical = 6.dp
                        )
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = {
                                Text(
                                    "Filter centres by name or area...",
                                    color = TextMuted,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfacePolish,
                                unfocusedContainerColor = SurfacePolish,
                                focusedBorderColor = DeepForestDark,
                                unfocusedBorderColor = BorderPolish,
                                focusedTextColor = TextDark,
                                unfocusedTextColor = TextDark
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("centres_search_input")
                        )
                    }
                }

                item {
                    CategoryFilterRow(
                        selectedCategory = selectedCategory,
                        onSelect = { viewModel.selectCategory(it) },
                        horizontalPadding = metrics.horizontalPadding
                    )
                }

                // Comparison banner if active
                if (comparisonIds.isNotEmpty()) {
                    item {
                        Surface(
                            color = SageContainer,
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderMedium),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = metrics.horizontalPadding, vertical = 6.dp)
                        ) {
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${comparisonIds.size} selected for side-by-side comparison",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepForestDark,
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .align(Alignment.CenterVertically)
                                )
                                Row(
                                    modifier = Modifier.align(Alignment.CenterVertically),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = { viewModel.clearComparison() },
                                        modifier = Modifier.heightIn(min = 40.dp)
                                    ) {
                                        Text("Clear", color = TextMuted, fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = { showComparisonSheet = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = DeepForestDark),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                        modifier = Modifier.heightIn(min = 40.dp)
                                    ) {
                                        Text("Compare", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Empty State
                if (filteredCentres.isEmpty()) {
                    item {
                        Surface(
                            color = SurfacePolish,
                            shape = RoundedCornerShape(24.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderPolish),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = metrics.horizontalPadding, vertical = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationSearching,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No matching centres found",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepForestDark
                                )
                                Text(
                                    text = "Try clearing the search query or choosing All Services.",
                                    fontSize = 12.sp,
                                    color = TextMuted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    // Responsive Centre Cards (1 col Compact, 2 cols Medium/Expanded)
                    items(chunkedCentres) { rowCentres ->
                        AdaptiveGridRow(
                            items = rowCentres,
                            columns = columns,
                            horizontalSpacing = 12.dp,
                            modifier = Modifier.padding(
                                horizontal = metrics.horizontalPadding,
                                vertical = 6.dp
                            )
                        ) { centre, cardModifier ->
                            CentreDirectoryCard(
                                centre = centre,
                                isSaved = savedIds.contains(centre.id),
                                isComparing = comparisonIds.contains(centre.id),
                                isTightCard = isTightCard,
                                fillHeight = columns > 1,
                                onSelect = { viewModel.selectCentre(centre) },
                                onToggleCompare = { viewModel.toggleCompareCentre(centre.id) },
                                onToggleSave = { viewModel.toggleSaveCentre(centre) },
                                onReportCrowd = { showReportDialog = centre },
                                onJoinQueue = { showJoinDialog = centre },
                                modifier = cardModifier
                            )
                        }
                    }
                }
            }
        }
    }

    // Join Queue Dialog
    showJoinDialog?.let { centre ->
        JoinQueueDialog(
            centre = centre,
            services = services.filter { centre.availableServiceIds.contains(it.id) || it.category == centre.category },
            onDismiss = { showJoinDialog = null },
            onConfirm = { serviceId, name, phone ->
                viewModel.joinQueue(centre.id, serviceId, name, phone)
                showJoinDialog = null
            }
        )
    }

    // Crowd Report Dialog
    showReportDialog?.let { centre ->
        CrowdReportDialog(
            centre = centre,
            onDismiss = { showReportDialog = null },
            onSubmit = { level, people, wait, notes, name ->
                viewModel.submitCrowdReport(centre.id, level, people, wait, notes, name)
                showReportDialog = null
            }
        )
    }

    // Comparison Bottom Sheet
    if (showComparisonSheet) {
        ModalBottomSheet(
            onDismissRequest = { showComparisonSheet = false },
            containerColor = SurfacePolish,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            val comparingCentres = centres.filter { comparisonIds.contains(it.id) }
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                val sheetMetrics = rememberQEaseLayoutMetrics(maxWidth)
                val sheetColumns = if (sheetMetrics.isWide && sheetMetrics.effectiveWidthDp >= 540f) 2 else 1

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 820.dp)
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(horizontal = sheetMetrics.horizontalPadding, vertical = 16.dp)
                ) {
                    Text(
                        text = "Side-by-Side Centre Comparison",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = DeepForestDark
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        comparingCentres.chunked(sheetColumns).forEach { rowItems ->
                            AdaptiveGridRow(
                                items = rowItems,
                                columns = sheetColumns,
                                horizontalSpacing = 10.dp
                            ) { c, cardMod ->
                                Surface(
                                    color = SurfaceVariantPolish,
                                    shape = RoundedCornerShape(18.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderPolish),
                                    modifier = cardMod
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = c.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = TextDark,
                                                modifier = Modifier
                                                    .weight(1f, fill = false)
                                                    .align(Alignment.CenterVertically)
                                            )
                                            CrowdBadge(
                                                level = c.currentCrowdLevel,
                                                confidencePercent = c.crowdConfidencePercent,
                                                modifier = Modifier.align(Alignment.CenterVertically)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "📍 Distance: ${c.distanceKm} km • ⏱ Wait: ${c.estimatedWaitMin}-${c.estimatedWaitMax} min",
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "🏢 Active Counters: ${c.activeCountersCount}/${c.totalCountersCount} • 💡 Best: ${c.bestTimeToVisit}",
                                            fontSize = 11.sp,
                                            color = SageAccent
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = { showComparisonSheet = false },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepForestDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text("Close Comparison", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CentreDirectoryCard(
    centre: ServiceCentre,
    isSaved: Boolean,
    isComparing: Boolean,
    isTightCard: Boolean,
    fillHeight: Boolean,
    onSelect: () -> Unit,
    onToggleCompare: () -> Unit,
    onToggleSave: () -> Unit,
    onReportCrowd: () -> Unit,
    onJoinQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onSelect,
        colors = CardDefaults.cardColors(containerColor = SurfacePolish),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderPolish),
        modifier = modifier
            .fillMaxWidth()
            .testTag("centre_detail_card_${centre.id}")
    ) {
        Column(
            modifier = Modifier
                .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier)
                .padding(18.dp)
        ) {
            // Title & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = centre.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${centre.distanceKm} km away • ${centre.address}",
                        fontSize = 11.sp,
                        color = TextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = onToggleCompare,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = if (isComparing) Icons.Default.CheckCircle else Icons.AutoMirrored.Outlined.CompareArrows,
                            contentDescription = "Compare",
                            tint = if (isComparing) DeepForestDark else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onToggleSave,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Save",
                            tint = if (isSaved) DeepForestDark else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats Grid: adapts cleanly between 3-col and wrapping 2-up at 125%+ scaling
            if (isTightCard) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 2
                ) {
                    CentreStatSubcard(
                        label = "CROWD LOAD",
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(min = 120.dp)
                    ) {
                        CrowdBadge(
                            level = centre.currentCrowdLevel,
                            confidencePercent = centre.crowdConfidencePercent
                        )
                    }

                    CentreStatSubcard(
                        label = "EST. WAIT",
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(min = 110.dp)
                    ) {
                        Text(
                            text = "${centre.estimatedWaitMin}-${centre.estimatedWaitMax} min",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    }

                    CentreStatSubcard(
                        label = "ACTIVE DESKS",
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${centre.activeCountersCount}/${centre.totalCountersCount} open",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CentreStatSubcard(
                        label = "CROWD LOAD",
                        modifier = Modifier
                            .weight(1.25f)
                            .fillMaxHeight()
                    ) {
                        CrowdBadge(
                            level = centre.currentCrowdLevel,
                            confidencePercent = centre.crowdConfidencePercent
                        )
                    }

                    CentreStatSubcard(
                        label = "EST. WAIT",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Text(
                            text = "${centre.estimatedWaitMin}-${centre.estimatedWaitMax} min",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    }

                    CentreStatSubcard(
                        label = "ACTIVE DESKS",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Text(
                            text = "${centre.activeCountersCount}/${centre.totalCountersCount} open",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Best Time Recommendation
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "💡 Best Time: ${centre.bestTimeToVisit}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PolishGreen
                )
            }

            if (centre.liveNotice != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = PolishAmberBg,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "📢 Notice: ${centre.liveNotice}",
                        color = PolishAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            if (fillHeight) {
                Spacer(modifier = Modifier.weight(1f, fill = true))
            }
            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: wrap gracefully at 125%+ scaling
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onReportCrowd,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderMedium),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = 124.dp)
                        .heightIn(min = 48.dp)
                        .testTag("btn_report_crowd_${centre.id}")
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Report Crowd",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    onClick = onJoinQueue,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeepForestDark,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = 124.dp)
                        .heightIn(min = 48.dp)
                        .testTag("btn_join_queue_${centre.id}")
                ) {
                    Icon(Icons.Default.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Get Token",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun CentreStatSubcard(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = SurfaceVariantPolish,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JoinQueueDialog(
    centre: ServiceCentre,
    services: List<ServiceItem>,
    onDismiss: () -> Unit,
    onConfirm: (serviceId: String, name: String, phone: String) -> Unit
) {
    var selectedServiceId by remember { mutableStateOf(services.firstOrNull()?.id ?: "") }
    var userName by remember { mutableStateOf("") }
    var userPhone by remember { mutableStateOf("") }

    ResponsiveDialogShell(onDismissRequest = onDismiss) {
        Text(
            text = "Get Virtual Queue Token",
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = DeepForestDark
        )
        Text(
            text = centre.name,
            fontSize = 12.sp,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text("Select Service:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(modifier = Modifier.height(6.dp))

        services.forEach { srv ->
            Surface(
                onClick = { selectedServiceId = srv.id },
                shape = RoundedCornerShape(12.dp),
                color = if (selectedServiceId == srv.id) SageContainer else SurfaceVariantPolish,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (selectedServiceId == srv.id) DeepForestDark else BorderPolish
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedServiceId == srv.id,
                        onClick = { selectedServiceId = srv.id },
                        colors = RadioButtonDefaults.colors(selectedColor = DeepForestDark)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(srv.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                        Text("Fee: ${srv.officialFee}", fontSize = 10.sp, color = TextMuted)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = userName,
            onValueChange = { userName = it },
            label = { Text("Your Full Name") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = userPhone,
            onValueChange = { userPhone = it },
            label = { Text("Mobile (For SMS & Turn Alert)") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(18.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancel", color = TextMuted)
            }
            Button(
                onClick = { onConfirm(selectedServiceId, userName, userPhone) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepForestDark),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Issue Token", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CrowdReportDialog(
    centre: ServiceCentre,
    onDismiss: () -> Unit,
    onSubmit: (level: CrowdLevel, people: Int, wait: Int, notes: String, name: String) -> Unit
) {
    var selectedLevel by remember { mutableStateOf(CrowdLevel.MODERATE) }
    var peopleCount by remember { mutableStateOf(20) }
    var waitMinutes by remember { mutableStateOf(15) }
    var notes by remember { mutableStateOf("") }
    var reporterName by remember { mutableStateOf("") }

    ResponsiveDialogShell(onDismissRequest = onDismiss) {
        Text(
            text = "Report Live Crowd Density",
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = DeepForestDark
        )
        Text(
            text = "${centre.name} • Help your community with verified crowd signals",
            fontSize = 11.sp,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text("Observed Crowd Level:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CrowdLevel.entries.forEach { level ->
                val isSelected = selectedLevel == level
                Surface(
                    onClick = { selectedLevel = level },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) DeepForestDark else SurfaceVariantPolish,
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = 72.dp)
                        .heightIn(min = 38.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = level.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else TextDark,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Approx. People Waiting: $peopleCount", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
        Slider(
            value = peopleCount.toFloat(),
            onValueChange = { peopleCount = it.toInt() },
            valueRange = 1f..100f,
            colors = SliderDefaults.colors(thumbColor = DeepForestDark, activeTrackColor = DeepForestDark)
        )

        Text("Observed Wait Time: $waitMinutes min", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
        Slider(
            value = waitMinutes.toFloat(),
            onValueChange = { waitMinutes = it.toInt() },
            valueRange = 2f..90f,
            colors = SliderDefaults.colors(thumbColor = DeepForestDark, activeTrackColor = DeepForestDark)
        )

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (e.g. counter 2 is fastest)") },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(18.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancel", color = TextMuted)
            }
            Button(
                onClick = { onSubmit(selectedLevel, peopleCount, waitMinutes, notes, reporterName) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepForestDark),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Submit Report", fontWeight = FontWeight.Bold)
            }
        }
    }
}

