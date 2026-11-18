package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.ResponsiveDialogShell
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.ui.viewmodel.QEaseViewModel

@Composable
internal fun qeaseFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextDark,
    unfocusedTextColor = TextDark,
    cursorColor = DeepForestDark,
    focusedPlaceholderColor = TextSubtle,
    unfocusedPlaceholderColor = TextSubtle,
    focusedBorderColor = DeepForestMedium,
    unfocusedBorderColor = BorderMedium,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StaffServiceInfoTab(
    viewModel: QEaseViewModel,
    centreServices: List<ServiceItem>,
    proposals: List<ServiceChangeProposal>,
    assignedCounterNumber: Int,
    horizontalPadding: androidx.compose.ui.unit.Dp
) {
    val allServices = centreServices.ifEmpty { viewModel.services.collectAsState().value }
    val prefill by viewModel.staffProposalPrefill.collectAsState()
    var selectedServiceId by remember(allServices) {
        mutableStateOf(allServices.firstOrNull()?.id ?: "")
    }
    var showProposalDialog by remember { mutableStateOf(false) }
    var activePrefill by remember { mutableStateOf<com.example.ui.viewmodel.StaffProposalPrefill?>(null) }

    LaunchedEffect(prefill) {
        val currentPrefill = prefill
        if (currentPrefill != null && currentPrefill.proposalType == ProposalType.SERVICE_INFO) {
            currentPrefill.serviceId?.takeIf { id -> allServices.any { it.id == id } }?.let {
                selectedServiceId = it
            }
            activePrefill = currentPrefill
            showProposalDialog = true
            viewModel.consumeStaffProposalPrefill()
        }
    }

    val selectedService = allServices.find { it.id == selectedServiceId } ?: allServices.firstOrNull()

    val infoProposals = remember(proposals) {
        proposals.filter { it.proposalType == ProposalType.SERVICE_INFO }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Surface(
                color = PolishAmberBg,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, PolishAmber.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(12.dp)
                ) {
                    Icon(Icons.Default.Approval, contentDescription = null, tint = PolishAmber, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Service updates are held in review until verified and approved by a System Admin.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )
                }
            }
        }

        if (allServices.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = horizontalPadding),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allServices.forEach { srv ->
                        val isSelected = srv.id == selectedService?.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedServiceId = srv.id },
                            label = {
                                Text(
                                    text = srv.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DeepForestDark,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.heightIn(min = 40.dp)
                        )
                    }
                }
            }
        }

        selectedService?.let { srv ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfacePolish),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, BorderPolish),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(srv.code, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PolishGreen)
                                Text(srv.title, fontSize = 16.sp, fontWeight = FontWeight.Black, color = DeepForestDark)
                            }
                            Surface(
                                color = if (srv.isAvailable) PolishGreenBg else PolishRedBg,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (srv.isAvailable) "ACTIVE" else "SUSPENDED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (srv.isAvailable) PolishGreen else PolishRed,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(srv.description, fontSize = 12.sp, color = TextDark, lineHeight = 18.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        HorizontalDivider(color = BorderPolish)
                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Official Fee: ${srv.officialFee}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Avg Processing Time: ${srv.avgProcessingDurationMin} mins", fontSize = 12.sp, color = TextMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Eligibility: ${srv.eligibility}", fontSize = 12.sp, color = TextMuted)
                        if (srv.cautionNote.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Desk Caution: ${srv.cautionNote}", fontSize = 11.sp, color = PolishAmber, fontWeight = FontWeight.Medium)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { showProposalDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepForestDark),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("btn_propose_service_edit")
                        ) {
                            Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Propose Service Info Update", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "Service Edit Proposals (${infoProposals.size})",
                subtitle = "Tracked from submission through Admin approval",
                horizontalPadding = horizontalPadding
            )
        }

        if (infoProposals.isEmpty()) {
            item {
                Text(
                    text = "No service info proposals submitted yet.",
                    fontSize = 12.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(horizontal = horizontalPadding)
                )
            }
        } else {
            items(infoProposals, key = { it.id }) { prop ->
                StaffProposalStatusCard(
                    proposal = prop,
                    modifier = Modifier.padding(horizontal = horizontalPadding)
                )
            }
        }
    }

    if (showProposalDialog && selectedService != null) {
        ProposeServiceInfoDialog(
            service = selectedService,
            prefill = activePrefill,
            onDismiss = {
                showProposalDialog = false
                activePrefill = null
            },
            onSubmit = { fee, duration, desc, elig, caution, rationale ->
                val res = viewModel.submitServiceInfoProposal(
                    serviceId = selectedService.id,
                    proposedFee = fee,
                    proposedDurationMin = duration,
                    proposedDescription = desc,
                    proposedEligibility = elig,
                    proposedCautionNote = caution,
                    rationale = rationale,
                    assignedCounterNumber = assignedCounterNumber
                )
                if (res.isSuccess) {
                    showProposalDialog = false
                    activePrefill = null
                }
                res.exceptionOrNull()?.message
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StaffRequiredDocumentsTab(
    viewModel: QEaseViewModel,
    centreServices: List<ServiceItem>,
    proposals: List<ServiceChangeProposal>,
    assignedCounterNumber: Int,
    horizontalPadding: androidx.compose.ui.unit.Dp
) {
    val allServices = centreServices.ifEmpty { viewModel.services.collectAsState().value }
    val prefill by viewModel.staffProposalPrefill.collectAsState()
    var selectedServiceId by remember(allServices) {
        mutableStateOf(allServices.firstOrNull()?.id ?: "")
    }

    var activeDocDialogMode by remember { mutableStateOf<ProposalType?>(null) }
    var editingDocument by remember { mutableStateOf<RequiredDocument?>(null) }
    var activeDocPrefill by remember { mutableStateOf<com.example.ui.viewmodel.StaffProposalPrefill?>(null) }

    LaunchedEffect(prefill) {
        val currentPrefill = prefill
        if (currentPrefill != null && currentPrefill.proposalType != ProposalType.SERVICE_INFO) {
            currentPrefill.serviceId?.takeIf { id -> allServices.any { it.id == id } }?.let {
                selectedServiceId = it
            }
            editingDocument = null
            activeDocPrefill = currentPrefill
            activeDocDialogMode = currentPrefill.proposalType
            viewModel.consumeStaffProposalPrefill()
        }
    }

    val selectedService = allServices.find { it.id == selectedServiceId } ?: allServices.firstOrNull()

    val docProposals = remember(proposals) {
        proposals.filter { it.proposalType != ProposalType.SERVICE_INFO }
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
                allServices.forEach { srv ->
                    FilterChip(
                        selected = srv.id == selectedService?.id,
                        onClick = { selectedServiceId = srv.id },
                        label = { Text(srv.title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DeepForestDark,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.heightIn(min = 40.dp)
                    )
                }
            }
        }

        selectedService?.let { srv ->
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Checklist (${srv.requiredDocuments.size} documents)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepForestDark
                        )
                        Text(
                            text = "Add, modify, or retire checklist rules (Admin approved)",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                    Button(
                        onClick = {
                            editingDocument = null
                            activeDocDialogMode = ProposalType.REQUIRED_DOCUMENT_ADD
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepForestDark),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("btn_propose_doc_add")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Doc", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(srv.requiredDocuments, key = { it.id }) { doc ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfacePolish),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, BorderPolish),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = doc.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepForestDark,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Surface(
                                color = if (doc.isMandatory) SageContainer else SurfaceContainerNav,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (doc.isMandatory) "MANDATORY" else "OPTIONAL",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepForestDark,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(doc.description, fontSize = 12.sp, color = TextMuted)
                        if (doc.acceptableAlternatives.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Alternatives: ${doc.acceptableAlternatives.joinToString(", ")}",
                                fontSize = 11.sp,
                                color = PolishGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    editingDocument = doc
                                    activeDocDialogMode = ProposalType.REQUIRED_DOCUMENT_UPDATE
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.heightIn(min = 42.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Propose Edit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    editingDocument = doc
                                    activeDocDialogMode = ProposalType.REQUIRED_DOCUMENT_REMOVE
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PolishRed),
                                border = BorderStroke(1.dp, PolishRed.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.heightIn(min = 42.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Propose Remove", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "Document Change Proposals (${docProposals.size})",
                subtitle = "Pending and resolved checklist requests",
                horizontalPadding = horizontalPadding
            )
        }

        items(docProposals, key = { it.id }) { prop ->
            StaffProposalStatusCard(
                proposal = prop,
                modifier = Modifier.padding(horizontal = horizontalPadding)
            )
        }
    }

    val dialogMode = activeDocDialogMode
    if (dialogMode != null && selectedService != null) {
        ProposeDocumentDialog(
            mode = dialogMode,
            serviceTitle = selectedService.title,
            existingDoc = editingDocument,
            prefill = activeDocPrefill,
            onDismiss = {
                activeDocDialogMode = null
                activeDocPrefill = null
            },
            onSubmit = { name, desc, mandatory, alternatives, rationale ->
                val res = viewModel.submitDocumentProposal(
                    proposalType = dialogMode,
                    serviceId = selectedService.id,
                    documentId = editingDocument?.id ?: "",
                    proposedName = name,
                    proposedDescription = desc,
                    proposedMandatory = mandatory,
                    proposedAlternatives = alternatives,
                    rationale = rationale,
                    assignedCounterNumber = assignedCounterNumber
                )
                if (res.isSuccess) {
                    activeDocDialogMode = null
                    activeDocPrefill = null
                }
                res.exceptionOrNull()?.message
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StaffProposalStatusCard(
    proposal: ServiceChangeProposal,
    modifier: Modifier = Modifier
) {
    val (badgeBg, badgeColor) = when (proposal.status) {
        ProposalStatus.PENDING -> PolishAmberBg to PolishAmber
        ProposalStatus.APPROVED -> PolishGreenBg to PolishGreen
        ProposalStatus.REJECTED -> PolishRedBg to PolishRed
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfacePolish),
        shape = RoundedCornerShape(18.dp),
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
                    text = "${proposal.proposalType.label} • ${proposal.serviceTitle}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepForestDark,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Surface(color = badgeBg, shape = RoundedCornerShape(8.dp)) {
                    Text(
                        text = proposal.status.label.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text("Reason: ${proposal.rationale}", fontSize = 11.sp, color = TextDark)
            if (proposal.adminReviewNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Admin Note (${proposal.reviewedByAdminName ?: "Admin"}): ${proposal.adminReviewNote}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = badgeColor
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProposeServiceInfoDialog(
    service: ServiceItem,
    prefill: com.example.ui.viewmodel.StaffProposalPrefill? = null,
    onDismiss: () -> Unit,
    onSubmit: (fee: String, duration: Int, desc: String, elig: String, caution: String, rationale: String) -> String?
) {
    var fee by remember(service, prefill) { mutableStateOf(prefill?.fee ?: service.officialFee) }
    var durationStr by remember(service, prefill) { mutableStateOf((prefill?.durationMin ?: service.avgProcessingDurationMin).toString()) }
    var desc by remember(service) { mutableStateOf(service.description) }
    var eligibility by remember(service) { mutableStateOf(service.eligibility) }
    var caution by remember(service, prefill) { mutableStateOf(prefill?.cautionNote ?: service.cautionNote) }
    var rationale by remember(prefill) { mutableStateOf(prefill?.rationale ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    ResponsiveDialogShell(onDismissRequest = onDismiss, maxWidth = 520.dp) {
        Text("Propose Service Update", fontSize = 18.sp, fontWeight = FontWeight.Black, color = DeepForestDark)
        Text("Held until Admin reviews & approves: ${service.title}", fontSize = 11.sp, color = TextMuted)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = fee,
            onValueChange = { fee = it; errorMsg = null },
            label = { Text("Official Fee Structure") },
            singleLine = true,
            colors = qeaseFieldColors(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = durationStr,
            onValueChange = { durationStr = it.filter { c -> c.isDigit() }.take(3); errorMsg = null },
            label = { Text("Avg Processing Duration (minutes)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = qeaseFieldColors(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = desc,
            onValueChange = { desc = it; errorMsg = null },
            label = { Text("Service Description") },
            colors = qeaseFieldColors(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = eligibility,
            onValueChange = { eligibility = it },
            label = { Text("Citizen Eligibility") },
            colors = qeaseFieldColors(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = caution,
            onValueChange = { caution = it },
            label = { Text("Desk Caution Note") },
            colors = qeaseFieldColors(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = rationale,
            onValueChange = { rationale = it; errorMsg = null },
            label = { Text("Reason for Change (Required) *") },
            placeholder = { Text("e.g. Updated government circular or counter timing") },
            colors = qeaseFieldColors(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("proposal_rationale_input")
        )

        errorMsg?.let {
            Spacer(modifier = Modifier.height(6.dp))
            Text(it, fontSize = 11.sp, color = PolishRed, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(14.dp))
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
                    val dur = durationStr.toIntOrNull() ?: 0
                    val err = onSubmit(fee, dur, desc, eligibility, caution, rationale)
                    if (err != null) errorMsg = err
                },
                colors = ButtonDefaults.buttonColors(containerColor = DeepForestDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1.4f)
                    .heightIn(min = 48.dp)
                    .testTag("submit_proposal_button")
            ) {
                Text("Submit for Approval", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProposeDocumentDialog(
    mode: ProposalType,
    serviceTitle: String,
    existingDoc: RequiredDocument?,
    prefill: com.example.ui.viewmodel.StaffProposalPrefill? = null,
    onDismiss: () -> Unit,
    onSubmit: (name: String, desc: String, mandatory: Boolean, alternatives: List<String>, rationale: String) -> String?
) {
    var docName by remember(existingDoc, prefill) { mutableStateOf(existingDoc?.name ?: prefill?.docName ?: "") }
    var docDesc by remember(existingDoc, prefill) { mutableStateOf(existingDoc?.description ?: prefill?.docDescription ?: "") }
    var isMandatory by remember(existingDoc) { mutableStateOf(existingDoc?.isMandatory ?: true) }
    var alternativesStr by remember(existingDoc) { mutableStateOf(existingDoc?.acceptableAlternatives?.joinToString(", ") ?: "") }
    var rationale by remember(prefill) { mutableStateOf(prefill?.rationale ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    ResponsiveDialogShell(onDismissRequest = onDismiss, maxWidth = 500.dp) {
        Text(mode.label, fontSize = 18.sp, fontWeight = FontWeight.Black, color = DeepForestDark)
        Text("Target service: $serviceTitle (held for Admin review)", fontSize = 11.sp, color = TextMuted)
        Spacer(modifier = Modifier.height(12.dp))

        if (mode == ProposalType.REQUIRED_DOCUMENT_REMOVE) {
            Surface(color = PolishRedBg, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Proposing removal of \"${existingDoc?.name}\" from mandatory checklist.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PolishRed,
                    modifier = Modifier.padding(12.dp)
                )
            }
        } else {
            OutlinedTextField(
                value = docName,
                onValueChange = { docName = it; errorMsg = null },
                label = { Text("Document Name *") },
                singleLine = true,
                colors = qeaseFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = docDesc,
                onValueChange = { docDesc = it },
                label = { Text("Verification Instructions") },
                colors = qeaseFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = alternativesStr,
                onValueChange = { alternativesStr = it },
                label = { Text("Acceptable Alternatives (comma separated)") },
                colors = qeaseFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isMandatory = !isMandatory }
                    .padding(vertical = 4.dp)
            ) {
                Text("Mandatory Document", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                Switch(checked = isMandatory, onCheckedChange = { isMandatory = it })
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = rationale,
            onValueChange = { rationale = it; errorMsg = null },
            label = { Text("Reason for Admin Approval *") },
            colors = qeaseFieldColors(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        errorMsg?.let {
            Spacer(modifier = Modifier.height(6.dp))
            Text(it, fontSize = 11.sp, color = PolishRed, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(14.dp))
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
                    val alts = alternativesStr.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    val err = onSubmit(docName, docDesc, isMandatory, alts, rationale)
                    if (err != null) errorMsg = err
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (mode == ProposalType.REQUIRED_DOCUMENT_REMOVE) PolishRed else DeepForestDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1.4f).heightIn(min = 48.dp)
            ) {
                Text("Submit Proposal", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WalkInTokenDialog(
    centreName: String,
    services: List<ServiceItem>,
    onDismiss: () -> Unit,
    onIssue: (serviceId: String, name: String, phone: String) -> String?
) {
    var selectedServiceId by remember(services) { mutableStateOf(services.firstOrNull()?.id ?: "") }
    var citizenName by remember { mutableStateOf("") }
    var citizenPhone by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    ResponsiveDialogShell(onDismissRequest = onDismiss, maxWidth = 460.dp) {
        Text("Issue Walk-In Queue Token", fontSize = 18.sp, fontWeight = FontWeight.Black, color = DeepForestDark)
        Text("Desk registration for $centreName", fontSize = 11.sp, color = TextMuted)
        Spacer(modifier = Modifier.height(12.dp))

        if (services.isNotEmpty()) {
            Text("Select Service", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DeepForestDark)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                services.forEach { srv ->
                    FilterChip(
                        selected = srv.id == selectedServiceId,
                        onClick = { selectedServiceId = srv.id },
                        label = { Text(srv.title, fontSize = 11.sp) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        OutlinedTextField(
            value = citizenName,
            onValueChange = { citizenName = it; errorMsg = null },
            label = { Text("Citizen Full Name *") },
            singleLine = true,
            colors = qeaseFieldColors(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = citizenPhone,
            onValueChange = { citizenPhone = it.filter { c -> c.isDigit() }.take(10) },
            label = { Text("Mobile Number (Optional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            colors = qeaseFieldColors(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        errorMsg?.let {
            Spacer(modifier = Modifier.height(6.dp))
            Text(it, fontSize = 11.sp, color = PolishRed, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(14.dp))
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
                    val err = onIssue(selectedServiceId, citizenName, citizenPhone)
                    if (err != null) errorMsg = err
                },
                colors = ButtonDefaults.buttonColors(containerColor = DeepForestDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1.4f).heightIn(min = 48.dp)
            ) {
                Text("Generate Token", fontWeight = FontWeight.Bold)
            }
        }
    }
}
