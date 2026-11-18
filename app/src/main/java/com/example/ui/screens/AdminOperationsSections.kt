package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.AdaptiveGridRow
import com.example.ui.components.ResponsiveDialogShell
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.ui.viewmodel.QEaseViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminStaffManagementTab(
    viewModel: QEaseViewModel,
    staffAccounts: List<UserAccount>,
    centres: List<ServiceCentre>,
    counters: List<Counter>,
    horizontalPadding: Dp
) {
    var editingStaff by remember { mutableStateOf<UserAccount?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

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
                    Text("Staff Officer Directory (${staffAccounts.size})", fontSize = 15.sp, fontWeight = FontWeight.Black, color = DeepForestDark)
                    Text("Assign counters, rotate desk PINs & manage portal access", fontSize = 11.sp, color = TextMuted)
                }
                Button(
                    onClick = {
                        editingStaff = null
                        showCreateDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepForestDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("btn_add_staff_officer")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Staff", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(staffAccounts, key = { it.id }) { staff ->
            val assignedCentre = centres.find { it.id == staff.assignedCentreId }
            val assignedCounter = counters.find { it.id == staff.assignedCounterId }

            Card(
                colors = CardDefaults.cardColors(containerColor = SurfacePolish),
                shape = RoundedCornerShape(20.dp),
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
                            Text(staff.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DeepForestDark)
                            Text("${staff.email} • ${staff.phone}", fontSize = 11.sp, color = TextMuted)
                        }
                        Surface(
                            color = if (staff.isActive) PolishGreenBg else PolishRedBg,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (staff.isActive) "ACTIVE" else "SUSPENDED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (staff.isActive) PolishGreen else PolishRed,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Centre: ${assignedCentre?.name ?: "Unassigned"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )
                    Text(
                        text = "Counter: ${assignedCounter?.let { "#${it.counterNumber} (${it.name})" } ?: "Unassigned"} • PIN: ${staff.pin}",
                        fontSize = 11.sp,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                editingStaff = staff
                                showCreateDialog = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.heightIn(min = 44.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit / Reassign", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { viewModel.toggleStaffAccountActive(staff.id) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (staff.isActive) PolishRed else PolishGreen
                            ),
                            border = BorderStroke(
                                1.dp,
                                (if (staff.isActive) PolishRed else PolishGreen).copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.heightIn(min = 44.dp)
                        ) {
                            Text(
                                text = if (staff.isActive) "Suspend Access" else "Activate Access",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        StaffEditorDialog(
            existingStaff = editingStaff,
            centres = centres,
            counters = counters,
            onDismiss = { showCreateDialog = false },
            onSave = { name, email, phone, centreId, counterId, dept, pin, active ->
                val res = viewModel.createOrUpdateStaffAccount(
                    existingId = editingStaff?.id,
                    name = name,
                    email = email,
                    phone = phone,
                    assignedCentreId = centreId,
                    assignedCounterId = counterId,
                    department = dept,
                    pin = pin,
                    isActive = active
                )
                if (res.isSuccess) showCreateDialog = false
                res.exceptionOrNull()?.message
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StaffEditorDialog(
    existingStaff: UserAccount?,
    centres: List<ServiceCentre>,
    counters: List<Counter>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String, Boolean) -> String?
) {
    var name by remember { mutableStateOf(existingStaff?.name ?: "") }
    var email by remember { mutableStateOf(existingStaff?.email ?: "") }
    var phone by remember { mutableStateOf(existingStaff?.phone ?: "+91 9811009950") }
    var department by remember { mutableStateOf(existingStaff?.department ?: "Front Desk & Scrutiny") }
    var pin by remember { mutableStateOf(existingStaff?.pin ?: "1234") }
    var isActive by remember { mutableStateOf(existingStaff?.isActive ?: true) }
    var selectedCentreId by remember {
        mutableStateOf(existingStaff?.assignedCentreId ?: centres.firstOrNull()?.id.orEmpty())
    }
    val centreCounters = remember(selectedCentreId, counters) {
        counters.filter { it.centreId == selectedCentreId }
    }
    var selectedCounterId by remember(selectedCentreId) {
        mutableStateOf(
            existingStaff?.assignedCounterId?.takeIf { id -> centreCounters.any { it.id == id } }
                ?: centreCounters.firstOrNull()?.id.orEmpty()
        )
    }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    ResponsiveDialogShell(onDismissRequest = onDismiss, maxWidth = 520.dp) {
        Text(
            text = if (existingStaff == null) "Create Staff Officer Account" else "Edit Staff Assignment",
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = DeepForestDark
        )
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it; errorMsg = null },
            label = { Text("Officer Full Name *") },
            singleLine = true,
            colors = qeaseFieldColors(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("staff_editor_name")
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; errorMsg = null },
            label = { Text("Government Email *") },
            singleLine = true,
            colors = qeaseFieldColors(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("staff_editor_email")
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone") },
                singleLine = true,
                colors = qeaseFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1.2f)
            )
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter { c -> c.isDigit() }.take(6); errorMsg = null },
                label = { Text("Desk PIN *") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = qeaseFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(0.8f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = department,
            onValueChange = { department = it },
            label = { Text("Department") },
            singleLine = true,
            colors = qeaseFieldColors(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))

        Text("Assigned Service Centre", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DeepForestDark)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            centres.forEach { centre ->
                FilterChip(
                    selected = centre.id == selectedCentreId,
                    onClick = { selectedCentreId = centre.id },
                    label = { Text(centre.name, fontSize = 11.sp) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Text("Locked Counter Assignment", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DeepForestDark)
        Spacer(modifier = Modifier.height(4.dp))
        if (centreCounters.isEmpty()) {
            Text("No counters configured for this centre.", fontSize = 11.sp, color = PolishRed)
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                centreCounters.forEach { cnt ->
                    FilterChip(
                        selected = cnt.id == selectedCounterId,
                        onClick = { selectedCounterId = cnt.id },
                        label = { Text("Counter ${cnt.counterNumber}", fontSize = 11.sp) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Portal Login Enabled", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
            Switch(checked = isActive, onCheckedChange = { isActive = it })
        }

        errorMsg?.let {
            Spacer(modifier = Modifier.height(6.dp))
            Text(it, fontSize = 11.sp, color = PolishRed, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    val err = onSave(name, email, phone, selectedCentreId, selectedCounterId, department, pin, isActive)
                    if (err != null) errorMsg = err
                },
                colors = ButtonDefaults.buttonColors(containerColor = DeepForestDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1.4f).heightIn(min = 48.dp).testTag("btn_save_staff")
            ) {
                Text("Save Staff Account", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminCentresAndServicesTab(
    viewModel: QEaseViewModel,
    centres: List<ServiceCentre>,
    services: List<ServiceItem>,
    columns: Int,
    horizontalPadding: Dp
) {
    var editingCentre by remember { mutableStateOf<ServiceCentre?>(null) }
    var showCentreDialog by remember { mutableStateOf(false) }

    val serviceRows = remember(services, columns) { services.chunked(columns) }

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
                    Text("Service Centres (${centres.size})", fontSize = 15.sp, fontWeight = FontWeight.Black, color = DeepForestDark)
                    Text("Configure branches, counters & virtual queue gates", fontSize = 11.sp, color = TextMuted)
                }
                Button(
                    onClick = {
                        editingCentre = null
                        showCentreDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepForestDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("btn_add_service_centre")
                ) {
                    Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Centre", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(centres, key = { it.id }) { centre ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfacePolish),
                shape = RoundedCornerShape(20.dp),
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
                            Text("${centre.category.displayName} • ${centre.address}", fontSize = 11.sp, color = TextMuted)
                        }
                        Switch(
                            checked = centre.isVirtualQueueOpen,
                            onCheckedChange = { viewModel.toggleCentreVirtualQueue(centre.id) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = DeepForestDark)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Hours: ${centre.operatingHours} • Counters: ${centre.activeCountersCount}/${centre.totalCountersCount} Active",
                        fontSize = 11.sp,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            editingCentre = centre
                            showCentreDialog = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.heightIn(min = 44.dp)
                    ) {
                        Icon(Icons.Default.EditLocationAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Centre Details", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            SectionHeader(
                title = "Live Service Availability (${services.size})",
                subtitle = "Immediately enable or suspend citizen-facing services",
                horizontalPadding = horizontalPadding
            )
        }

        items(serviceRows) { rowItems ->
            Box(modifier = Modifier.padding(horizontal = horizontalPadding)) {
                AdaptiveGridRow(items = rowItems, columns = columns) { srv, cardModifier ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfacePolish),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, BorderPolish),
                        modifier = cardModifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(srv.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                Text("Code: ${srv.code} • ${srv.officialFee}", fontSize = 11.sp, color = TextMuted)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = srv.isAvailable,
                                onCheckedChange = { viewModel.toggleServiceAvailability(srv.id) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = DeepForestDark)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCentreDialog) {
        CentreEditorDialog(
            existingCentre = editingCentre,
            allServices = services,
            onDismiss = { showCentreDialog = false },
            onSave = { name, category, address, hours, phone, totalCounters, bestTime, open, serviceIds, notice ->
                val res = viewModel.createOrUpdateServiceCentre(
                    existingId = editingCentre?.id,
                    name = name,
                    category = category,
                    address = address,
                    operatingHours = hours,
                    phone = phone,
                    totalCountersCount = totalCounters,
                    bestTimeToVisit = bestTime,
                    isVirtualQueueOpen = open,
                    availableServiceIds = serviceIds,
                    liveNotice = notice
                )
                if (res.isSuccess) showCentreDialog = false
                res.exceptionOrNull()?.message
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CentreEditorDialog(
    existingCentre: ServiceCentre?,
    allServices: List<ServiceItem>,
    onDismiss: () -> Unit,
    onSave: (String, ServiceCategory, String, String, String, Int, String, Boolean, List<String>, String?) -> String?
) {
    var name by remember { mutableStateOf(existingCentre?.name ?: "") }
    var category by remember { mutableStateOf(existingCentre?.category ?: ServiceCategory.CIVIL_ID) }
    var address by remember { mutableStateOf(existingCentre?.address ?: "") }
    var hours by remember { mutableStateOf(existingCentre?.operatingHours ?: "09:00 AM – 05:30 PM (Mon–Fri)") }
    var phone by remember { mutableStateOf(existingCentre?.phone ?: "+1 (800) 258-1800") }
    var totalCountersStr by remember { mutableStateOf((existingCentre?.totalCountersCount ?: 4).toString()) }
    var bestTime by remember { mutableStateOf(existingCentre?.bestTimeToVisit ?: "10:30 AM – 12:30 PM") }
    var liveNotice by remember { mutableStateOf(existingCentre?.liveNotice ?: "") }
    var selectedServiceIds by remember {
        mutableStateOf(
            existingCentre?.availableServiceIds?.toSet()
                ?: allServices.take(2).map { it.id }.toSet()
        )
    }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    ResponsiveDialogShell(onDismissRequest = onDismiss, maxWidth = 520.dp) {
        Text(
            text = if (existingCentre == null) "Add New Service Centre" else "Edit Service Centre",
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = DeepForestDark
        )
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it; errorMsg = null },
            label = { Text("Centre Name *") },
            singleLine = true,
            colors = qeaseFieldColors(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = address,
            onValueChange = { address = it; errorMsg = null },
            label = { Text("Full Street Address *") },
            colors = qeaseFieldColors(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = hours,
                onValueChange = { hours = it },
                label = { Text("Operating Hours") },
                singleLine = true,
                colors = qeaseFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1.3f)
            )
            OutlinedTextField(
                value = totalCountersStr,
                onValueChange = { totalCountersStr = it.filter { c -> c.isDigit() }.take(2) },
                label = { Text("Counters") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = qeaseFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(0.7f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = liveNotice,
            onValueChange = { liveNotice = it },
            label = { Text("Live Citizen Notice (Optional)") },
            singleLine = true,
            colors = qeaseFieldColors(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))

        Text("Department Category", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DeepForestDark)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ServiceCategory.entries.forEach { cat ->
                FilterChip(
                    selected = cat == category,
                    onClick = { category = cat },
                    label = { Text(cat.displayName, fontSize = 11.sp) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Text("Supported Citizen Services", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DeepForestDark)
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            allServices.forEach { srv ->
                val checked = selectedServiceIds.contains(srv.id)
                FilterChip(
                    selected = checked,
                    onClick = {
                        selectedServiceIds = if (checked) selectedServiceIds - srv.id else selectedServiceIds + srv.id
                    },
                    label = { Text(srv.code, fontSize = 11.sp) }
                )
            }
        }

        errorMsg?.let {
            Spacer(modifier = Modifier.height(6.dp))
            Text(it, fontSize = 11.sp, color = PolishRed, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    val count = totalCountersStr.toIntOrNull() ?: 4
                    val err = onSave(
                        name, category, address, hours, phone, count,
                        bestTime, existingCentre?.isVirtualQueueOpen ?: true,
                        selectedServiceIds.toList(), liveNotice.ifBlank { null }
                    )
                    if (err != null) errorMsg = err
                },
                colors = ButtonDefaults.buttonColors(containerColor = DeepForestDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1.4f).heightIn(min = 48.dp)
            ) {
                Text("Save Centre", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminAnalyticsTab(
    viewModel: QEaseViewModel,
    centres: List<ServiceCentre>,
    auditLogs: List<AuditLog>,
    columns: Int,
    horizontalPadding: Dp
) {
    var selectedCentreFilter by remember { mutableStateOf<String?>(null) }
    val tokens by viewModel.tokens.collectAsState()
    val proposals by viewModel.proposals.collectAsState()
    val counters by viewModel.counters.collectAsState()

    val stats = remember(selectedCentreFilter, tokens, proposals, counters, centres) {
        viewModel.computeQueueStatistics(selectedCentreFilter)
    }
    val maxTrendCount = remember(stats.hourlyTrends) {
        (stats.hourlyTrends.maxOfOrNull { it.tokenCount } ?: 1).coerceAtLeast(1)
    }
    val logRows = remember(auditLogs, columns) { auditLogs.take(12).chunked(columns) }

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
                    selected = selectedCentreFilter == null,
                    onClick = { selectedCentreFilter = null },
                    label = { Text("All Centres", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                centres.forEach { centre ->
                    FilterChip(
                        selected = selectedCentreFilter == centre.id,
                        onClick = { selectedCentreFilter = centre.id },
                        label = { Text(centre.name, fontSize = 11.sp) }
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfacePolish),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderPolish),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "QUEUE PERFORMANCE SNAPSHOT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatMetricTile("Total Today", "${stats.totalTokensToday}", DeepForestDark, Modifier.weight(1f).widthIn(min = 100.dp))
                        StatMetricTile("Completion", "${stats.completionRatePercent}%", PolishGreen, Modifier.weight(1f).widthIn(min = 100.dp))
                        StatMetricTile("Avg Wait", "${stats.avgWaitMinutes}m", PolishBlue, Modifier.weight(1f).widthIn(min = 100.dp))
                        StatMetricTile("Waiting Now", "${stats.waitingCount}", PolishAmber, Modifier.weight(1f).widthIn(min = 100.dp))
                        StatMetricTile("Counters Open", "${stats.activeCountersCount}/${stats.totalCountersCount}", DeepForestDark, Modifier.weight(1f).widthIn(min = 100.dp))
                        StatMetricTile("Pending Edits", "${stats.pendingProposalsCount}", PolishRed, Modifier.weight(1f).widthIn(min = 100.dp))
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfacePolish),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderPolish),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "HOURLY TOKEN ARRIVAL & WAIT TREND",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        stats.hourlyTrends.forEach { point ->
                            val ratio = (point.tokenCount.toFloat() / maxTrendCount).coerceIn(0.15f, 1f)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            ) {
                                Text(
                                    text = "${point.tokenCount}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepForestDark
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .fillMaxHeight(ratio * 0.65f)
                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                        .background(if (point.avgWaitMinutes >= 15) PolishAmber else DeepForestDark)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(point.hourLabel, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                                Text("${point.avgWaitMinutes}m", fontSize = 9.sp, color = PolishGreen)
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "Deterministic Audit Trail (${auditLogs.size})",
                subtitle = "Immutable event history across queues, staff & settings",
                horizontalPadding = horizontalPadding
            )
        }

        items(logRows) { rowItems ->
            Box(modifier = Modifier.padding(horizontal = horizontalPadding)) {
                AdaptiveGridRow(items = rowItems, columns = columns) { log, cardModifier ->
                    Surface(
                        color = SurfaceVariantPolish,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, BorderPolish),
                        modifier = cardModifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(log.action, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepForestDark)
                                Text(log.actorName, fontSize = 10.sp, color = TextMuted)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(log.details, fontSize = 11.sp, color = TextDark)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMetricTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceContainerNav,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = accent)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminSystemSettingsTab(
    viewModel: QEaseViewModel,
    currentSettings: SystemSettings,
    horizontalPadding: Dp
) {
    var autoNoShowMin by remember(currentSettings) { mutableIntStateOf(currentSettings.autoNoShowTimeoutMinutes) }
    var maxActiveTokens by remember(currentSettings) { mutableIntStateOf(currentSettings.maxActiveTokensPerCitizen) }
    var crowdHalfLife by remember(currentSettings) { mutableIntStateOf(currentSettings.crowdDecayHalfLifeMinutes) }
    var walkInBuffer by remember(currentSettings) { mutableIntStateOf(currentSettings.walkInBufferPerHour) }
    var maintenanceMode by remember(currentSettings) { mutableStateOf(currentSettings.maintenanceMode) }
    var maintenanceBanner by remember(currentSettings) { mutableStateOf(currentSettings.maintenanceBanner) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                        text = "GLOBAL QUEUE POLICY & SYSTEM SETTINGS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    SettingStepperRow(
                        title = "Auto No-Show Grace Window",
                        subtitle = "Minutes after token call before desk marks no-show",
                        valueText = "${autoNoShowMin}m",
                        onMinus = { if (autoNoShowMin > 5) autoNoShowMin -= 5 },
                        onPlus = { if (autoNoShowMin < 60) autoNoShowMin += 5 }
                    )
                    HorizontalDivider(color = BorderPolish, modifier = Modifier.padding(vertical = 10.dp))

                    SettingStepperRow(
                        title = "Max Active Tokens Per Citizen",
                        subtitle = "Prevents duplicate queue hoarding across centres",
                        valueText = "$maxActiveTokens",
                        onMinus = { if (maxActiveTokens > 1) maxActiveTokens -= 1 },
                        onPlus = { if (maxActiveTokens < 5) maxActiveTokens += 1 }
                    )
                    HorizontalDivider(color = BorderPolish, modifier = Modifier.padding(vertical = 10.dp))

                    SettingStepperRow(
                        title = "Crowd Report Decay Half-Life",
                        subtitle = "Minutes before unverified citizen reports lose confidence",
                        valueText = "${crowdHalfLife}m",
                        onMinus = { if (crowdHalfLife > 5) crowdHalfLife -= 5 },
                        onPlus = { if (crowdHalfLife < 60) crowdHalfLife += 5 }
                    )
                    HorizontalDivider(color = BorderPolish, modifier = Modifier.padding(vertical = 10.dp))

                    SettingStepperRow(
                        title = "Walk-In Desk Buffer Slots / Hour",
                        subtitle = "Reserved hourly intake slots for senior & offline citizens",
                        valueText = "$walkInBuffer",
                        onMinus = { if (walkInBuffer > 0) walkInBuffer -= 1 },
                        onPlus = { if (walkInBuffer < 12) walkInBuffer += 1 }
                    )
                    HorizontalDivider(color = BorderPolish, modifier = Modifier.padding(vertical = 10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Global Maintenance Mode", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DeepForestDark)
                            Text("Pauses new citizen bookings system-wide", fontSize = 11.sp, color = TextMuted)
                        }
                        Switch(
                            checked = maintenanceMode,
                            onCheckedChange = { maintenanceMode = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PolishRed
                            ),
                            modifier = Modifier.testTag("switch_maintenance_mode")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = maintenanceBanner,
                        onValueChange = { maintenanceBanner = it },
                        label = { Text("Maintenance Banner Message") },
                        colors = qeaseFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val defaults = SystemSettings()
                                viewModel.updateSystemSettings(defaults)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        ) {
                            Text("Reset Defaults", fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {
                                viewModel.updateSystemSettings(
                                    currentSettings.copy(
                                        autoNoShowTimeoutMinutes = autoNoShowMin,
                                        maxActiveTokensPerCitizen = maxActiveTokens,
                                        crowdDecayHalfLifeMinutes = crowdHalfLife,
                                        walkInBufferPerHour = walkInBuffer,
                                        maintenanceMode = maintenanceMode,
                                        maintenanceBanner = maintenanceBanner
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepForestDark),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.5f)
                                .heightIn(min = 48.dp)
                                .testTag("btn_save_system_settings")
                        ) {
                            Text("Save System Settings", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingStepperRow(
    title: String,
    subtitle: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Text(subtitle, fontSize = 11.sp, color = TextMuted)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilledTonalIconButton(onClick = onMinus, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease $title", modifier = Modifier.size(16.dp))
            }
            Text(
                text = valueText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = DeepForestDark,
                modifier = Modifier.widthIn(min = 38.dp)
            )
            FilledTonalIconButton(onClick = onPlus, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Increase $title", modifier = Modifier.size(16.dp))
            }
        }
    }
}
