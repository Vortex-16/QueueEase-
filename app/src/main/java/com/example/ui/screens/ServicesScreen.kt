package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.QEaseViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ServicesScreen(
    viewModel: QEaseViewModel,
    modifier: Modifier = Modifier
) {
    val services by viewModel.services.collectAsState()
    val selectedService by viewModel.selectedService.collectAsState()
    val checkedDocIds by viewModel.checkedDocumentIds.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showServiceDetailModal by remember { mutableStateOf(false) }

    val filteredServices = remember(services, searchQuery, selectedCategory) {
        services.filter { srv ->
            val matchesQuery = searchQuery.isBlank() ||
                    srv.title.contains(searchQuery, ignoreCase = true) ||
                    srv.description.contains(searchQuery, ignoreCase = true)
            val matchesCat = selectedCategory == null || srv.category == selectedCategory
            matchesQuery && matchesCat
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("services_screen"),
        containerColor = BackgroundPolish,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Column {
                        Text(
                            text = "Verified Service Catalog",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = DeepForestDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Mandatory document checklists & official fee schedules",
                            fontSize = 11.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
            val chunkedServices = remember(filteredServices, columns) {
                filteredServices.chunked(columns)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                // Search Input
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
                                    "Search services, passport, license, NOC...",
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
                                .testTag("services_search_input")
                        )
                    }
                }

                // Categories Filter
                item {
                    CategoryFilterRow(
                        selectedCategory = selectedCategory,
                        onSelect = { viewModel.selectCategory(it) },
                        horizontalPadding = metrics.horizontalPadding
                    )
                }

                // Responsive Services Grid
                items(chunkedServices) { rowServices ->
                    AdaptiveGridRow(
                        items = rowServices,
                        columns = columns,
                        horizontalSpacing = 12.dp,
                        modifier = Modifier.padding(
                            horizontal = metrics.horizontalPadding,
                            vertical = 6.dp
                        )
                    ) { srv, cardMod ->
                        ServiceCatalogCard(
                            service = srv,
                            fillHeight = columns > 1,
                            onClick = {
                                viewModel.selectService(srv)
                                showServiceDetailModal = true
                            },
                            modifier = cardMod
                        )
                    }
                }
            }
        }
    }

    // Full Service Detail & Document Verification Sheet
    if (showServiceDetailModal && selectedService != null) {
        val srv = selectedService!!
        val mandatoryCount = srv.requiredDocuments.count { it.isMandatory }
        val checkedMandatoryCount = srv.requiredDocuments.count { it.isMandatory && checkedDocIds.contains(it.id) }
        val isReadyToVisit = checkedMandatoryCount >= mandatoryCount

        ModalBottomSheet(
            onDismissRequest = { showServiceDetailModal = false },
            containerColor = SurfacePolish,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                val sheetMetrics = rememberQEaseLayoutMetrics(maxWidth)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp)
                        .navigationBarsPadding()
                        .padding(horizontal = sheetMetrics.horizontalPadding, vertical = 10.dp)
                ) {
                    item {
                        Surface(
                            color = SageContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = srv.code,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepForestDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = srv.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = DeepForestDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = srv.description,
                            fontSize = 12.sp,
                            color = TextMuted
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            color = if (isReadyToVisit) SageContainer else PolishAmberBg,
                            shape = RoundedCornerShape(18.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isReadyToVisit) BorderMedium else PolishAmber
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isReadyToVisit) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isReadyToVisit) DeepForestDark else PolishAmber,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isReadyToVisit) "Ready to Visit! ($checkedMandatoryCount/$mandatoryCount proofs verified)" else "Documents Incomplete ($checkedMandatoryCount/$mandatoryCount proofs checked)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isReadyToVisit) DeepForestDark else PolishAmber
                                    )
                                    Text(
                                        text = if (isReadyToVisit) "You have all mandatory original documents required." else "Please check off all required originals before visiting centre.",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Interactive Document Verification Checklist",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = "Tap to verify you have the original documents ready:",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    items(srv.requiredDocuments) { doc ->
                        DocumentItemRow(
                            doc = doc,
                            isChecked = checkedDocIds.contains(doc.id),
                            onToggle = { viewModel.toggleDocumentCheck(doc.id) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Step-by-Step Counter Procedure",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        srv.proceduralSteps.forEachIndexed { idx, step ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(DeepForestDark, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = step,
                                    fontSize = 12.sp,
                                    color = TextDark,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        srv.cautionNote?.let { note ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = PolishRedBg,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "⚠️ Caution: $note",
                                    color = PolishRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { showServiceDetailModal = false },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepForestDark),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .padding(bottom = 16.dp)
                        ) {
                            Text("Done & Return", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ServiceCatalogCard(
    service: ServiceItem,
    fillHeight: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = SurfacePolish),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderPolish),
        modifier = modifier
            .fillMaxWidth()
            .testTag("service_item_${service.id}")
    ) {
        Column(
            modifier = Modifier
                .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier)
                .padding(18.dp)
        ) {
            Surface(
                color = SageContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = service.code,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepForestDark,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = service.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = service.description,
                fontSize = 12.sp,
                color = TextMuted,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Fee and Time Pills wrap cleanly at 125%+ scaling
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = SurfaceVariantPolish,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = 120.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Fee: ${service.officialFee}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark
                        )
                    }
                }

                Surface(
                    color = SurfaceVariantPolish,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = 120.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "~${service.avgProcessingDurationMin} min desk time",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark
                        )
                    }
                }
            }

            if (fillHeight) {
                Spacer(modifier = Modifier.weight(1f, fill = true))
            }
            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "📄 ${service.requiredDocuments.size} Required Proof Documents",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepForestDark,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                Text(
                    text = "Open Checklist →",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PolishBlue,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }
    }
}

