package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.data.model.*
import com.example.data.repository.QEaseRepository
import com.example.widget.WidgetStateBridge
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class MainNavTab {
    EXPLORE,
    CENTRES,
    SERVICES,
    MY_QUEUE,
    LIVE_TRACKER,
    REVIEWS_FEEDBACK,
    AI_ASSISTANT,
    STAFF_DESK,
    STAFF_PROPOSALS,
    STAFF_ALERTS,
    ADMIN_MONITOR,
    ADMIN_APPROVALS,
    ADMIN_OPS,
    ADMIN_ANALYTICS,
    SAVED_CENTRES
}

data class StaffProposalPrefill(
    val serviceId: String? = null,
    val proposalType: ProposalType = ProposalType.SERVICE_INFO,
    val fee: String? = null,
    val durationMin: Int? = null,
    val cautionNote: String? = null,
    val docName: String? = null,
    val docDescription: String? = null,
    val rationale: String = ""
)

class QEaseViewModel(application: Application) : AndroidViewModel(application) {

    val repository = QEaseRepository(application)

    // User & Session RBAC
    val currentUser = repository.currentUser
    val currentRole = repository.currentRole
    val isAuthenticated = repository.isAuthenticated
    val activeFirebaseUid = repository.activeFirebaseUid

    // Data flows
    val services = repository.services
    val centres = repository.centres
    val counters = repository.counters
    val tokens = repository.tokens
    val activeUserToken = repository.activeUserToken
    val staffAccounts = repository.staffAccounts
    val proposals = repository.proposals
    val notifications = repository.notifications
    val systemSettings = repository.systemSettings
    val crowdReports = repository.crowdReports
    val reviews = repository.reviews
    val liveQueueUpdates = repository.liveQueueUpdates
    val auditLogs = repository.auditLogs
    val savedCentreIds = repository.savedCentreIds
    val chatMessages = repository.chatMessages
    val isAiThinking = repository.isAiThinking
    val selectedStaffCentreId = repository.selectedStaffCentreId
    val selectedStaffCounterId = repository.selectedStaffCounterId

    // Fast Data Sync
    val syncStatus = repository.syncStatus
    val isSyncing = repository.isSyncing

    // UI Nav & Flow States
    private val _currentTab = MutableStateFlow(MainNavTab.EXPLORE)
    val currentTab: StateFlow<MainNavTab> = _currentTab.asStateFlow()

    private val _staffSubTab = MutableStateFlow(0)
    val staffSubTab: StateFlow<Int> = _staffSubTab.asStateFlow()

    private val _adminSubTab = MutableStateFlow(0)
    val adminSubTab: StateFlow<Int> = _adminSubTab.asStateFlow()

    private val _staffProposalPrefill = MutableStateFlow<StaffProposalPrefill?>(null)
    val staffProposalPrefill: StateFlow<StaffProposalPrefill?> = _staffProposalPrefill.asStateFlow()

    private var pendingWidgetDestination: String? = null

    private val _showWelcomeScreen = MutableStateFlow(true)
    val showWelcomeScreen: StateFlow<Boolean> = _showWelcomeScreen.asStateFlow()

    private val _showAuthDialog = MutableStateFlow(false)
    val showAuthDialog: StateFlow<Boolean> = _showAuthDialog.asStateFlow()

    private val _showLogoutConfirm = MutableStateFlow(false)
    val showLogoutConfirm: StateFlow<Boolean> = _showLogoutConfirm.asStateFlow()

    private val _authTargetRole = MutableStateFlow(UserRole.CUSTOMER)
    val authTargetRole: StateFlow<UserRole> = _authTargetRole.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<ServiceCategory?>(null)
    val selectedCategory: StateFlow<ServiceCategory?> = _selectedCategory.asStateFlow()

    private val _selectedCentre = MutableStateFlow<ServiceCentre?>(null)
    val selectedCentre: StateFlow<ServiceCentre?> = _selectedCentre.asStateFlow()

    private val _selectedService = MutableStateFlow<ServiceItem?>(null)
    val selectedService: StateFlow<ServiceItem?> = _selectedService.asStateFlow()

    private val _comparisonCentreIds = MutableStateFlow<Set<String>>(emptySet())
    val comparisonCentreIds: StateFlow<Set<String>> = _comparisonCentreIds.asStateFlow()

    private val _checkedDocumentIds = MutableStateFlow<Set<String>>(emptySet())
    val checkedDocumentIds: StateFlow<Set<String>> = _checkedDocumentIds.asStateFlow()

    private val _turnAlertEnabled = MutableStateFlow(true)
    val turnAlertEnabled: StateFlow<Boolean> = _turnAlertEnabled.asStateFlow()

    private val _userFeedbackMessage = MutableStateFlow<String?>(null)
    val userFeedbackMessage: StateFlow<String?> = _userFeedbackMessage.asStateFlow()

    fun setStaffSubTab(index: Int) {
        _staffSubTab.value = index.coerceIn(0, 1)
    }

    fun setAdminSubTab(index: Int) {
        _adminSubTab.value = index.coerceIn(0, 2)
    }

    fun consumeStaffProposalPrefill() {
        _staffProposalPrefill.value = null
    }

    fun handleWidgetDeepLink(destination: String?) {
        if (destination.isNullOrBlank()) return
        val isStaffDest = destination == WidgetStateBridge.DEST_STAFF_DESK ||
            destination == WidgetStateBridge.DEST_STAFF_ALERTS
        val isAdminDest = destination == WidgetStateBridge.DEST_ADMIN_MONITOR ||
            destination == WidgetStateBridge.DEST_ADMIN_APPROVALS

        if (!isAuthenticated.value) {
            pendingWidgetDestination = destination
            _showWelcomeScreen.value = true
            _authTargetRole.value = when {
                isStaffDest -> UserRole.STAFF
                isAdminDest -> UserRole.ADMIN
                else -> UserRole.CUSTOMER
            }
            _showAuthDialog.value = true
            return
        }

        if (isStaffDest) {
            if (currentRole.value != UserRole.STAFF) {
                _userFeedbackMessage.value = "Staff Desk access requires logging out first and signing in as Staff."
                return
            }
            _currentTab.value = if (destination == WidgetStateBridge.DEST_STAFF_ALERTS) {
                MainNavTab.STAFF_ALERTS
            } else {
                MainNavTab.STAFF_DESK
            }
        } else if (isAdminDest) {
            if (currentRole.value != UserRole.ADMIN) {
                _userFeedbackMessage.value = "Admin Console access requires logging out first and signing in as Admin."
                return
            }
            _currentTab.value = if (destination == WidgetStateBridge.DEST_ADMIN_APPROVALS) {
                MainNavTab.ADMIN_APPROVALS
            } else {
                MainNavTab.ADMIN_MONITOR
            }
        }
    }

    fun openAuthDialog(targetRole: UserRole) {
        if (isAuthenticated.value) {
            _showLogoutConfirm.value = true
            return
        }
        _authTargetRole.value = targetRole
        _showAuthDialog.value = true
    }

    fun dismissAuthDialog() {
        _showAuthDialog.value = false
        pendingWidgetDestination = null
    }

    fun requestCitizenOtp(mobileInput: String, fullName: String): Result<String> =
        repository.requestCitizenOtp(mobileInput, fullName)

    fun verifyCitizenOtp(mobileInput: String, otpInput: String, fullName: String): Result<UserAccount> {
        val result = repository.verifyCitizenOtp(mobileInput, otpInput, fullName)
        if (result.isSuccess) {
            _showAuthDialog.value = false
            _showWelcomeScreen.value = false
            _currentTab.value = MainNavTab.EXPLORE
            pendingWidgetDestination = null
            _userFeedbackMessage.value = "Welcome, ${result.getOrNull()?.name}! (Citizen Session Verified)"
        }
        return result
    }

    fun authenticateUser(emailOrPhone: String, pin: String, role: UserRole): Result<UserAccount> {
        val result = repository.authenticate(emailOrPhone, pin, role)
        if (result.isSuccess) {
            _showAuthDialog.value = false
            _showWelcomeScreen.value = false
            _userFeedbackMessage.value = "Welcome back, ${result.getOrNull()?.name}! (${role.name} Access Granted)"
            when (role) {
                UserRole.CUSTOMER -> _currentTab.value = MainNavTab.EXPLORE
                UserRole.STAFF -> {
                    _staffSubTab.value = 0
                    _currentTab.value = if (pendingWidgetDestination == WidgetStateBridge.DEST_STAFF_ALERTS) {
                        MainNavTab.STAFF_ALERTS
                    } else {
                        MainNavTab.STAFF_DESK
                    }
                }
                UserRole.ADMIN -> {
                    _adminSubTab.value = 0
                    _currentTab.value = if (pendingWidgetDestination == WidgetStateBridge.DEST_ADMIN_APPROVALS) {
                        MainNavTab.ADMIN_APPROVALS
                    } else {
                        MainNavTab.ADMIN_MONITOR
                    }
                }
            }
            pendingWidgetDestination = null
        }
        return result
    }

    fun requestLogoutConfirmation() {
        _showLogoutConfirm.value = true
    }

    fun dismissLogoutConfirmation() {
        _showLogoutConfirm.value = false
    }

    fun onFirebaseAuthChanged(firebaseUser: FirebaseUser?) {
        repository.syncGoogleAuthUser(firebaseUser)
    }

    fun prepareGoogleSignOut() {
        repository.stopCloudSyncBeforeSignOut()
        if (isAuthenticated.value) {
            confirmLogout()
        }
    }

    fun showFeedbackMessage(message: String) {
        _userFeedbackMessage.value = message
    }

    fun confirmLogout() {
        _showLogoutConfirm.value = false
        _showAuthDialog.value = false
        pendingWidgetDestination = null
        _staffProposalPrefill.value = null
        repository.logout()
        _currentTab.value = MainNavTab.EXPLORE
        _staffSubTab.value = 0
        _adminSubTab.value = 0
        _showWelcomeScreen.value = true
        _userFeedbackMessage.value = "Signed out securely. Choose a portal to sign in."
    }

    fun setNavTab(tab: MainNavTab) {
        if (!isAuthenticated.value) {
            _showWelcomeScreen.value = true
            return
        }
        val staffTabs = setOf(
            MainNavTab.STAFF_DESK,
            MainNavTab.STAFF_PROPOSALS,
            MainNavTab.STAFF_ALERTS,
            MainNavTab.AI_ASSISTANT
        )
        val adminTabs = setOf(
            MainNavTab.ADMIN_MONITOR,
            MainNavTab.ADMIN_APPROVALS,
            MainNavTab.ADMIN_OPS,
            MainNavTab.ADMIN_ANALYTICS,
            MainNavTab.AI_ASSISTANT
        )
        val citizenTabs = setOf(
            MainNavTab.EXPLORE,
            MainNavTab.CENTRES,
            MainNavTab.SERVICES,
            MainNavTab.MY_QUEUE,
            MainNavTab.LIVE_TRACKER,
            MainNavTab.REVIEWS_FEEDBACK,
            MainNavTab.SAVED_CENTRES,
            MainNavTab.AI_ASSISTANT
        )

        val allowed = when (currentRole.value) {
            UserRole.CUSTOMER -> tab in citizenTabs
            UserRole.STAFF -> tab in staffTabs
            UserRole.ADMIN -> tab in adminTabs
        }
        if (!allowed) {
            _userFeedbackMessage.value = when (currentRole.value) {
                UserRole.CUSTOMER -> "Staff and Admin sections require logging out and signing in with an officer account."
                UserRole.STAFF -> "Citizen discovery tabs are hidden in Staff Desk mode."
                UserRole.ADMIN -> "Citizen discovery tabs are hidden in Admin Governance mode."
            }
            return
        }
        _currentTab.value = tab
    }

    fun triggerFastSync() {
        repository.triggerFastSync()
        _userFeedbackMessage.value = "Database synchronised in real-time."
    }

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
    }

    fun selectCategory(cat: ServiceCategory?) {
        _selectedCategory.value = cat
    }

    fun selectCentre(centre: ServiceCentre?) {
        _selectedCentre.value = centre
    }

    fun selectService(service: ServiceItem?) {
        _selectedService.value = service
        service?.let { _checkedDocumentIds.value = emptySet() }
    }

    fun toggleDocumentCheck(docId: String) {
        val current = _checkedDocumentIds.value
        _checkedDocumentIds.value = if (current.contains(docId)) current - docId else current + docId
    }

    fun toggleCompareCentre(centreId: String) {
        val current = _comparisonCentreIds.value
        _comparisonCentreIds.value = if (current.contains(centreId)) {
            current - centreId
        } else {
            if (current.size >= 3) current else current + centreId
        }
    }

    fun clearComparison() {
        _comparisonCentreIds.value = emptySet()
    }

    fun toggleSaveCentre(centre: ServiceCentre) {
        repository.toggleSaveCentre(centre)
    }

    fun toggleTurnAlert() {
        _turnAlertEnabled.value = !_turnAlertEnabled.value
    }

    // Citizen Queue Actions
    fun joinQueue(centreId: String, serviceId: String, name: String, phone: String) {
        val result = repository.joinQueue(centreId, serviceId, name, phone)
        if (result.isSuccess) {
            _userFeedbackMessage.value = "Virtual Token ${result.getOrNull()?.tokenNumber} issued successfully!"
            _currentTab.value = MainNavTab.MY_QUEUE
        } else {
            _userFeedbackMessage.value = "Failed to join queue: ${result.exceptionOrNull()?.message}"
        }
    }

    fun cancelActiveToken() {
        val current = activeUserToken.value ?: return
        if (repository.cancelUserToken(current.id)) {
            _userFeedbackMessage.value = "Token ${current.tokenNumber} cancelled."
        }
    }

    // Staff Desk Queue Actions
    fun callNextStaffCustomer(counterId: String, staffName: String) {
        val res = repository.callNextCustomer(counterId, staffName)
        _userFeedbackMessage.value = if (res.isSuccess) {
            "Calling Customer: ${res.getOrNull()?.tokenNumber}"
        } else {
            res.exceptionOrNull()?.message ?: "No customers in waiting queue"
        }
    }

    fun markStaffServing(tokenId: String) {
        if (repository.markServing(tokenId)) {
            _userFeedbackMessage.value = "Customer marked as SERVING"
        }
    }

    fun completeStaffService(tokenId: String) {
        if (repository.completeService(tokenId)) {
            _userFeedbackMessage.value = "Service COMPLETED successfully"
        }
    }

    fun markStaffNoShow(tokenId: String) {
        if (repository.markNoShow(tokenId)) {
            _userFeedbackMessage.value = "Token marked as NO_SHOW"
        }
    }

    fun requeueStaffToken(tokenId: String, reason: String) {
        val res = repository.api.requeueToken(tokenId, reason, currentUser.value.name)
        _userFeedbackMessage.value = if (res.isSuccess) {
            "Token ${res.getOrNull()?.tokenNumber} moved back to waiting queue."
        } else {
            res.exceptionOrNull()?.message ?: "Unable to re-queue token"
        }
    }

    fun issueWalkInToken(centreId: String, serviceId: String, citizenName: String, citizenPhone: String): Result<QueueToken> {
        val res = repository.api.issueWalkInToken(centreId, serviceId, citizenName, citizenPhone, currentUser.value.name)
        if (res.isSuccess) {
            _userFeedbackMessage.value = "Walk-in token ${res.getOrNull()?.tokenNumber} issued for $citizenName"
        }
        return res
    }

    fun toggleCounter(counterId: String) {
        repository.toggleCounterStatus(counterId)
    }

    fun setStaffScope(centreId: String, counterId: String) {
        repository.setStaffScope(centreId, counterId)
    }

    // Staff Proposals & Notifications
    fun submitServiceInfoProposal(
        serviceId: String,
        proposedFee: String,
        proposedDurationMin: Int,
        proposedDescription: String,
        proposedEligibility: String,
        proposedCautionNote: String,
        rationale: String,
        assignedCounterNumber: Int
    ): Result<ServiceChangeProposal> {
        val res = repository.api.submitServiceInfoProposal(
            serviceId = serviceId,
            proposedFee = proposedFee,
            proposedDurationMin = proposedDurationMin,
            proposedDescription = proposedDescription,
            proposedEligibility = proposedEligibility,
            proposedCautionNote = proposedCautionNote,
            rationale = rationale,
            staff = currentUser.value,
            assignedCounterNumber = assignedCounterNumber
        )
        if (res.isSuccess) {
            _userFeedbackMessage.value = "Service edit sent to Admin for approval."
        }
        return res
    }

    fun submitDocumentProposal(
        proposalType: ProposalType,
        serviceId: String,
        documentId: String,
        proposedName: String,
        proposedDescription: String,
        proposedMandatory: Boolean,
        proposedAlternatives: List<String>,
        rationale: String,
        assignedCounterNumber: Int
    ): Result<ServiceChangeProposal> {
        val res = repository.api.submitDocumentProposal(
            proposalType = proposalType,
            serviceId = serviceId,
            documentId = documentId,
            proposedName = proposedName,
            proposedDescription = proposedDescription,
            proposedMandatory = proposedMandatory,
            proposedAlternatives = proposedAlternatives,
            rationale = rationale,
            staff = currentUser.value,
            assignedCounterNumber = assignedCounterNumber
        )
        if (res.isSuccess) {
            _userFeedbackMessage.value = "Document change queued for Admin approval."
        }
        return res
    }

    fun markNotificationRead(notificationId: String) {
        repository.api.markNotificationRead(notificationId)
    }

    fun markAllNotificationsRead(role: UserRole, centreId: String? = null) {
        repository.api.markAllNotificationsRead(role, centreId)
        _userFeedbackMessage.value = "All notifications marked as read."
    }

    // Admin Governance Actions
    fun approveProposal(proposalId: String, reviewNote: String): Result<ServiceChangeProposal> {
        val res = repository.api.approveProposal(proposalId, currentUser.value.name, reviewNote)
        _userFeedbackMessage.value = if (res.isSuccess) {
            "Change approved & published to live catalog."
        } else {
            res.exceptionOrNull()?.message ?: "Failed to approve proposal"
        }
        return res
    }

    fun rejectProposal(proposalId: String, reviewNote: String): Result<ServiceChangeProposal> {
        val res = repository.api.rejectProposal(proposalId, currentUser.value.name, reviewNote)
        _userFeedbackMessage.value = if (res.isSuccess) {
            "Proposal rejected and submitting staff notified."
        } else {
            res.exceptionOrNull()?.message ?: "Failed to reject proposal"
        }
        return res
    }

    fun createOrUpdateStaffAccount(
        existingId: String?,
        name: String,
        email: String,
        phone: String,
        assignedCentreId: String,
        assignedCounterId: String,
        department: String,
        pin: String,
        isActive: Boolean
    ): Result<UserAccount> {
        val res = repository.api.createOrUpdateStaffAccount(
            existingId = existingId,
            name = name,
            email = email,
            phone = phone,
            assignedCentreId = assignedCentreId,
            assignedCounterId = assignedCounterId,
            department = department,
            pin = pin,
            isActive = isActive,
            adminName = currentUser.value.name
        )
        if (res.isSuccess) {
            _userFeedbackMessage.value = "Staff officer ${res.getOrNull()?.name} saved."
        }
        return res
    }

    fun toggleStaffAccountActive(staffId: String) {
        val res = repository.api.toggleStaffAccountActive(staffId, currentUser.value.name)
        res.getOrNull()?.let {
            _userFeedbackMessage.value = "${it.name} is now ${if (it.isActive) "Active" else "Suspended"}."
        }
    }

    fun createOrUpdateServiceCentre(
        existingId: String?,
        name: String,
        category: ServiceCategory,
        address: String,
        operatingHours: String,
        phone: String,
        totalCountersCount: Int,
        bestTimeToVisit: String,
        isVirtualQueueOpen: Boolean,
        availableServiceIds: List<String>,
        liveNotice: String?
    ): Result<ServiceCentre> {
        val res = repository.api.createOrUpdateServiceCentre(
            existingId = existingId,
            name = name,
            category = category,
            address = address,
            operatingHours = operatingHours,
            phone = phone,
            totalCountersCount = totalCountersCount,
            bestTimeToVisit = bestTimeToVisit,
            isVirtualQueueOpen = isVirtualQueueOpen,
            availableServiceIds = availableServiceIds,
            liveNotice = liveNotice,
            adminName = currentUser.value.name
        )
        if (res.isSuccess) {
            _userFeedbackMessage.value = "${res.getOrNull()?.name} saved."
        }
        return res
    }

    fun toggleCentreVirtualQueue(centreId: String) {
        val res = repository.api.toggleCentreVirtualQueue(centreId, currentUser.value.name)
        res.getOrNull()?.let {
            _userFeedbackMessage.value = "${it.name}: Queue ${if (it.isVirtualQueueOpen) "Opened" else "Paused"}."
        }
    }

    fun updateSystemSettings(settings: SystemSettings): Result<SystemSettings> {
        val res = repository.api.updateSystemSettings(settings, currentUser.value.name)
        _userFeedbackMessage.value = if (res.isSuccess) {
            "System settings updated across all portals."
        } else {
            res.exceptionOrNull()?.message ?: "Failed to update settings"
        }
        return res
    }

    fun computeQueueStatistics(centreIdFilter: String? = null): QueueStatisticsSnapshot =
        repository.api.computeQueueStatistics(centreIdFilter)

    // Community Reports, Reviews & AI
    fun submitCrowdReport(centreId: String, level: CrowdLevel, people: Int, wait: Int, notes: String, name: String) {
        repository.submitCrowdReport(centreId, level, people, wait, notes, name)
        _userFeedbackMessage.value = "Community Crowd Report submitted with 95% initial confidence."
    }

    fun verifyStaffReport(reportId: String, staffName: String) {
        repository.verifyCrowdReport(reportId, staffName)
        _userFeedbackMessage.value = "Report verified by staff. Confidence boosted to 100%."
    }

    fun submitReview(centreId: String, rating: Int, reviewText: String, visitTag: String, queueSpeedRating: String) {
        val res = repository.submitReview(centreId, rating, reviewText, visitTag, queueSpeedRating)
        _userFeedbackMessage.value = if (res.isSuccess) {
            "Review submitted! Thank you for helping citizens."
        } else {
            "Failed to submit review."
        }
    }

    fun upvoteReview(reviewId: String) {
        repository.upvoteReview(reviewId)
        _userFeedbackMessage.value = "Marked as helpful"
    }

    fun submitLiveQueueUpdate(centreId: String, updateTag: String, message: String, estimate: String) {
        val res = repository.submitLiveQueueUpdate(centreId, updateTag, message, estimate)
        if (res.isSuccess) {
            _userFeedbackMessage.value = "Live status broadcasted to all citizens!"
        }
    }

    fun upvoteLiveUpdate(updateId: String) {
        repository.upvoteLiveUpdate(updateId)
        _userFeedbackMessage.value = "Update upvoted!"
    }

    fun sendAiMessage(query: String) {
        repository.sendChatMessage(query)
    }

    fun executeAiWorkflowAction(action: AiWorkflowAction) {
        when (action.type) {
            AiActionType.OPEN_SERVICE_CHECKLIST -> {
                if (currentRole.value == UserRole.CUSTOMER) {
                    val srv = action.targetServiceId?.let { id -> services.value.find { it.id == id } }
                    if (srv != null) {
                        selectService(srv)
                        setNavTab(MainNavTab.SERVICES)
                    }
                }
            }
            AiActionType.PREFILL_STAFF_SERVICE_PROPOSAL -> {
                if (currentRole.value == UserRole.STAFF) {
                    _staffProposalPrefill.value = StaffProposalPrefill(
                        serviceId = action.targetServiceId,
                        proposalType = ProposalType.SERVICE_INFO,
                        fee = action.prefillFee,
                        durationMin = action.prefillDurationMin,
                        cautionNote = action.prefillCautionNote,
                        rationale = action.prefillRationale.orEmpty()
                    )
                    setStaffSubTab(0)
                    setNavTab(MainNavTab.STAFF_PROPOSALS)
                }
            }
            AiActionType.PREFILL_STAFF_DOC_PROPOSAL -> {
                if (currentRole.value == UserRole.STAFF) {
                    _staffProposalPrefill.value = StaffProposalPrefill(
                        serviceId = action.targetServiceId,
                        proposalType = ProposalType.REQUIRED_DOCUMENT_ADD,
                        docName = action.prefillDocName,
                        docDescription = action.prefillDocDescription,
                        rationale = action.prefillRationale.orEmpty()
                    )
                    setStaffSubTab(1)
                    setNavTab(MainNavTab.STAFF_PROPOSALS)
                }
            }
            AiActionType.OPEN_STAFF_ALERTS -> {
                if (currentRole.value == UserRole.STAFF) {
                    setNavTab(MainNavTab.STAFF_ALERTS)
                }
            }
            AiActionType.VERIFY_LATEST_CROWD_REPORT -> {
                if (currentRole.value == UserRole.STAFF) {
                    action.targetReportId?.let { repId ->
                        verifyStaffReport(repId, currentUser.value.name.ifBlank { "Staff Officer" })
                    }
                    setNavTab(MainNavTab.STAFF_ALERTS)
                }
            }
            AiActionType.OPEN_STAFF_DESK -> {
                if (currentRole.value == UserRole.STAFF) {
                    setNavTab(MainNavTab.STAFF_DESK)
                }
            }
            AiActionType.OPEN_ADMIN_APPROVALS -> {
                if (currentRole.value == UserRole.ADMIN) {
                    setNavTab(MainNavTab.ADMIN_APPROVALS)
                }
            }
            AiActionType.OPEN_ADMIN_OPS_STAFF -> {
                if (currentRole.value == UserRole.ADMIN) {
                    setAdminSubTab(0)
                    setNavTab(MainNavTab.ADMIN_OPS)
                }
            }
            AiActionType.OPEN_ADMIN_OPS_CENTRES -> {
                if (currentRole.value == UserRole.ADMIN) {
                    setAdminSubTab(1)
                    setNavTab(MainNavTab.ADMIN_OPS)
                }
            }
            AiActionType.OPEN_ADMIN_OPS_SETTINGS -> {
                if (currentRole.value == UserRole.ADMIN) {
                    setAdminSubTab(2)
                    setNavTab(MainNavTab.ADMIN_OPS)
                }
            }
            AiActionType.OPEN_ADMIN_ANALYTICS -> {
                if (currentRole.value == UserRole.ADMIN) {
                    setNavTab(MainNavTab.ADMIN_ANALYTICS)
                }
            }
        }
    }

    fun toggleServiceAvailability(serviceId: String) {
        repository.toggleServiceAvailability(serviceId)
    }

    fun updateNotice(centreId: String, notice: String) {
        repository.updateCentreNotice(centreId, notice)
        _userFeedbackMessage.value = "Centre notice updated."
    }

    fun clearFeedbackMessage() {
        _userFeedbackMessage.value = null
    }
}
