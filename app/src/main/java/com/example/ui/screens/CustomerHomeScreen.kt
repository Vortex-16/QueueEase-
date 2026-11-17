package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainNavTab
import com.example.ui.viewmodel.QEaseViewModel

private data class HomeShortcutItem(
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val iconBg: Color,
    val iconTint: Color,
    val tag: String,
    val onClick: () -> Unit
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomerHomeScreen(
    viewModel: QEaseViewModel,
    modifier: Modifier = Modifier
) {
    val centres by viewModel.centres.collectAsState()
    val services by viewModel.services.collectAsState()
    val savedIds by viewModel.savedCentreIds.collectAsState()
    val activeToken by viewModel.activeUserToken.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val comparisonIds by viewModel.comparisonCentreIds.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    val filteredCentres = remember(centres, searchQuery, selectedCategory) {
        centres.filter { centre ->
            val matchesQuery = searchQuery.isBlank() ||
                    centre.name.contains(searchQuery, ignoreCase = true) ||
                    centre.address.contains(searchQuery, ignoreCase = true)
            val matchesCat = selectedCategory == null || centre.category == selectedCategory
            matchesQuery && matchesCat
        }
    }

    val filteredServices = remember(services, searchQuery, selectedCategory) {
        services.filter { srv ->
            val matchesQuery = searchQuery.isBlank() ||
                    srv.title.contains(searchQuery, ignoreCase = true) ||
                    srv.description.contains(searchQuery, ignoreCase = true)
            val matchesCat = selectedCategory == null || srv.category == selectedCategory
            matchesQuery && matchesCat
        }
    }

    val shortcuts = remember(viewModel) {
        listOf(
            HomeShortcutItem(
                title = "Where is my Turn",
                subtitle = "Live station radar",
                icon = Icons.Default.GpsFixed,
                iconBg = SageContainer,
                iconTint = DeepForestDark,
                tag = "where_is_my_turn_card",
                onClick = { viewModel.setNavTab(MainNavTab.LIVE_TRACKER) }
            ),
            HomeShortcutItem(
                title = "Citizen Reviews",
                subtitle = "4.8★ (342 reviews)",
                icon = Icons.Default.RateReview,
                iconBg = PolishAmberBg,
                iconTint = PolishAmber,
                tag = "citizen_reviews_card",
                onClick = { viewModel.setNavTab(MainNavTab.REVIEWS_FEEDBACK) }
            )
        )
    }

    QEaseResponsiveContainer(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundPolish)
            .testTag("customer_home_screen"),
        maxContentWidth = 1120.dp
    ) { metrics ->
        val columns = metrics.gridColumns
        val shortcutColumns = if (metrics.effectiveWidthDp < 280f) 1 else 2
        val chunkedCentres = remember(filteredCentres, columns) {
            filteredCentres.take(4).chunked(columns)
        }
        val chunkedServices = remember(filteredServices, columns) {
            filteredServices.take(4).chunked(columns)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Top Header with user badge and fast sync telemetry
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = metrics.horizontalPadding, vertical = 10.dp)
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .align(Alignment.CenterVertically)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SageContainer,
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .size(40.dp)
                                    .border(1.dp, PolishGreen.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_qease_minimal_logo),
                                    contentDescription = "QEase Minimalist Logo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }

                            Column {
                                Text(
                                    text = "QEase",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Black,
                                    color = DeepForestDark,
                                    letterSpacing = (-0.5).sp
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "SERVICE INTELLIGENCE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = "•",
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = syncStatus.split("•").firstOrNull()?.trim() ?: "Live Sync",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PolishGreen
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = { viewModel.setNavTab(MainNavTab.AI_ASSISTANT) },
                                color = SurfaceContainerNav,
                                shape = CircleShape,
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                                    .size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                        contentDescription = "AI Help Guide",
                                        tint = DeepForestDark,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }

                            Surface(
                                onClick = { viewModel.requestLogoutConfirmation() },
                                color = SageContainer,
                                shape = CircleShape,
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, DeepForestMedium),
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                                    .size(42.dp)
                                    .testTag("user_avatar_btn")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = currentUser.name.split(" ")
                                            .mapNotNull { it.firstOrNull()?.toString() }
                                            .take(2)
                                            .joinToString("")
                                            .ifBlank { "AS" },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepForestDark
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = {
                            Text(
                                "Search services, passports, RTO, hospital...",
                                color = TextMuted,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
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
                            .testTag("home_search_input")
                    )
                }
            }

            // Active Token Hero Section
            activeToken?.let { token ->
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = metrics.horizontalPadding, vertical = 6.dp)
                    ) {
                        ActiveTokenHeroCard(
                            token = token,
                            onLeaveQueue = { viewModel.cancelActiveToken() },
                            onClick = { viewModel.setNavTab(MainNavTab.LIVE_TRACKER) }
                        )
                    }
                }
            }

            // Quick Intelligence Shortcuts Row ("Where is my Turn" & "Reviews")
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = metrics.horizontalPadding, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    shortcuts.chunked(shortcutColumns).forEach { rowItems ->
                        AdaptiveGridRow(
                            items = rowItems,
                            columns = shortcutColumns,
                            horizontalSpacing = 10.dp
                        ) { shortcut, cardMod ->
                            Card(
                                onClick = shortcut.onClick,
                                colors = CardDefaults.cardColors(containerColor = SurfacePolish),
                                shape = RoundedCornerShape(18.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderPolish),
                                modifier = cardMod.testTag(shortcut.tag)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = shortcut.iconBg,
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = shortcut.icon,
                                                contentDescription = null,
                                                tint = shortcut.iconTint,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = shortcut.title,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepForestDark
                                        )
                                        Text(
                                            text = shortcut.subtitle,
                                            fontSize = 10.sp,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Category Filter Row
            item {
                CategoryFilterRow(
                    selectedCategory = selectedCategory,
                    onSelect = { viewModel.selectCategory(it) },
                    horizontalPadding = metrics.horizontalPadding
                )
            }

            // Quick AI Grounded Assistant Card
            item {
                Card(
                    onClick = { viewModel.setNavTab(MainNavTab.AI_ASSISTANT) },
                    colors = CardDefaults.cardColors(containerColor = SurfacePolish),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderPolish),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = metrics.horizontalPadding, vertical = 6.dp)
                        .testTag("ai_assistant_quick_card")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(SurfaceVariantPolish, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = DeepForestDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Grounded Service AI",
                                color = TextDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Ask required documents, fees & best time to visit.",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Section: Service Intelligence cards
            item {
                Spacer(modifier = Modifier.height(6.dp))
                SectionHeader(
                    title = "Service Intelligence",
                    subtitle = "Document requirements & live queue loads",
                    actionText = "View All (${centres.size})",
                    onAction = { viewModel.setNavTab(MainNavTab.CENTRES) },
                    horizontalPadding = metrics.horizontalPadding
                )
            }

            // Centres Responsive Grid
            items(chunkedCentres) { rowCentres ->
                AdaptiveGridRow(
                    items = rowCentres,
                    columns = columns,
                    horizontalSpacing = 12.dp,
                    modifier = Modifier.padding(horizontal = metrics.horizontalPadding, vertical = 5.dp)
                ) { centre, cardMod ->
                    CentreCardPolish(
                        centre = centre,
                        isSaved = savedIds.contains(centre.id),
                        isComparing = comparisonIds.contains(centre.id),
                        fillHeight = columns > 1,
                        onSelect = {
                            viewModel.selectCentre(centre)
                            viewModel.setNavTab(MainNavTab.CENTRES)
                        },
                        onToggleSave = { viewModel.toggleSaveCentre(centre) },
                        onToggleCompare = { viewModel.toggleCompareCentre(centre.id) },
                        modifier = cardMod
                    )
                }
            }

            // Section: Popular Services & Document Checklists
            item {
                Spacer(modifier = Modifier.height(10.dp))
                SectionHeader(
                    title = "Verified Document Requirements",
                    subtitle = "Check mandatory proofs before leaving home",
                    actionText = "Catalog (${services.size})",
                    onAction = { viewModel.setNavTab(MainNavTab.SERVICES) },
                    horizontalPadding = metrics.horizontalPadding
                )
            }

            items(chunkedServices) { rowServices ->
                AdaptiveGridRow(
                    items = rowServices,
                    columns = columns,
                    horizontalSpacing = 12.dp,
                    modifier = Modifier.padding(horizontal = metrics.horizontalPadding, vertical = 5.dp)
                ) { srv, cardMod ->
                    ServiceCardPolish(
                        service = srv,
                        fillHeight = columns > 1,
                        onSelect = {
                            viewModel.selectService(srv)
                            viewModel.setNavTab(MainNavTab.SERVICES)
                        },
                        modifier = cardMod
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CentreCardPolish(
    centre: ServiceCentre,
    isSaved: Boolean,
    isComparing: Boolean,
    onSelect: () -> Unit,
    onToggleSave: () -> Unit,
    onToggleCompare: () -> Unit,
    modifier: Modifier = Modifier,
    fillHeight: Boolean = false
) {
    Card(
        onClick = onSelect,
        colors = CardDefaults.cardColors(containerColor = SurfacePolish),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderPolish),
        modifier = modifier
            .fillMaxWidth()
            .testTag("centre_card_${centre.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier)
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(SurfaceVariantPolish, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = centre.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .align(Alignment.CenterVertically)
                    )
                    CrowdBadge(
                        level = centre.currentCrowdLevel,
                        confidencePercent = centre.crowdConfidencePercent,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Est. Wait: ${centre.estimatedWaitMin}-${centre.estimatedWaitMax} min • ${centre.activeCountersCount}/${centre.totalCountersCount} counters active",
                    fontSize = 11.sp,
                    color = TextMuted,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "💡 ${centre.bestTimeToVisit}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PolishGreen
                )
            }
        }
    }
}

@Composable
fun ServiceCardPolish(
    service: ServiceItem,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    fillHeight: Boolean = false
) {
    Card(
        onClick = onSelect,
        colors = CardDefaults.cardColors(containerColor = SurfacePolish),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderPolish),
        modifier = modifier
            .fillMaxWidth()
            .testTag("service_card_${service.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(SurfaceVariantPolish, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = service.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${service.requiredDocuments.size} Required Proofs • Official Fee: ${service.officialFee}",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

