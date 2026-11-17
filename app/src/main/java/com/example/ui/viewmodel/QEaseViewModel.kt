package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.QEaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class MainNavTab {
    EXPLORE,
    CENTRES,
    SERVICES,
    MY_QUEUE,
    LIVE_TRACKER,      // "Where is my train" style live station radar
    REVIEWS_FEEDBACK,  // Community feedback, reviews & ratings
    AI_ASSISTANT,
    STAFF_DESK,
    ADMIN_PANEL,
    SAVED_CENTRES
}

class QEaseViewModel(application: Application) : AndroidViewModel(application) {

    val repository = QEaseRepository(application)

    // User & Session RBAC
    val currentUser = repository.currentUser
    val currentRole = repository.currentRole
    val isAuthenticated = repository.isAuthenticated

    // Data flows
    val services = repository.services
    val centres = repository.centres
    val counters = repository.counters
    val tokens = repository.tokens
    val activeUserToken = repository.activeUserToken
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
    }

    fun requestCitizenOtp(mobileInput: String, fullName: String): Result<String> {
        return repository.requestCitizenOtp(mobileInput, fullName)
    }

    fun verifyCitizenOtp(mobileInput: String, otpInput: String, fullName: String): Result<UserAccount> {
        val result = repository.verifyCitizenOtp(mobileInput, otpInput, fullName)
        if (result.isSuccess) {
            _showAuthDialog.value = false
            _showWelcomeScreen.value = false
            _currentTab.value = MainNavTab.EXPLORE
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
                UserRole.STAFF -> _currentTab.value = MainNavTab.STAFF_DESK
                UserRole.ADMIN -> _currentTab.value = MainNavTab.ADMIN_PANEL
            }
        }
        return result
    }

    fun requestLogoutConfirmation() {
        _showLogoutConfirm.value = true
    }

    fun dismissLogoutConfirmation() {
        _showLogoutConfirm.value = false
    }

    fun confirmLogout() {
        _showLogoutConfirm.value = false
        _showAuthDialog.value = false
        repository.logout()
        _currentTab.value = MainNavTab.EXPLORE
        _showWelcomeScreen.value = true
        _userFeedbackMessage.value = "Signed out securely. Choose a portal to sign in."
    }

    fun setNavTab(tab: MainNavTab) {
        if (!isAuthenticated.value) {
            _showWelcomeScreen.value = true
            return
        }
        // Strict RBAC Gate: switching to Staff/Admin requires explicit logout first
        if (tab == MainNavTab.STAFF_DESK && currentRole.value != UserRole.STAFF) {
            _userFeedbackMessage.value = "Staff Desk access requires logging out first and signing in as Staff."
            return
        }
        if (tab == MainNavTab.ADMIN_PANEL && currentRole.value != UserRole.ADMIN) {
            _userFeedbackMessage.value = "Admin Console access requires logging out first and signing in as Admin."
            return
        }
        _currentTab.value = tab
    }

    fun triggerFastSync() {
        repository.triggerFastSync()
        _userFeedbackMessage.value = "⚡ Database synchronised in real-time."
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
        service?.let {
            _checkedDocumentIds.value = emptySet()
        }
    }

    fun toggleDocumentCheck(docId: String) {
        val current = _checkedDocumentIds.value
        _checkedDocumentIds.value = if (current.contains(docId)) {
            current - docId
        } else {
            current + docId
        }
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

    // Actions
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
        val success = repository.cancelUserToken(current.id)
        if (success) {
            _userFeedbackMessage.value = "Token ${current.tokenNumber} cancelled."
        }
    }

    fun callNextStaffCustomer(counterId: String, staffName: String) {
        val res = repository.callNextCustomer(counterId, staffName)
        if (res.isSuccess) {
            _userFeedbackMessage.value = "Calling Customer: ${res.getOrNull()?.tokenNumber}"
        } else {
            _userFeedbackMessage.value = res.exceptionOrNull()?.message ?: "No customers in waiting queue"
        }
    }

    fun markStaffServing(tokenId: String) {
        val success = repository.markServing(tokenId)
        if (success) _userFeedbackMessage.value = "Customer marked as SERVING"
    }

    fun completeStaffService(tokenId: String) {
        val success = repository.completeService(tokenId)
        if (success) _userFeedbackMessage.value = "Service COMPLETED successfully"
    }

    fun markStaffNoShow(tokenId: String) {
        val success = repository.markNoShow(tokenId)
        if (success) _userFeedbackMessage.value = "Token marked as NO_SHOW"
    }

    fun toggleCounter(counterId: String) {
        repository.toggleCounterStatus(counterId)
    }

    fun setStaffScope(centreId: String, counterId: String) {
        repository.setStaffScope(centreId, counterId)
    }

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
        if (res.isSuccess) {
            _userFeedbackMessage.value = "⭐ Review submitted! Thank you for helping citizens."
        } else {
            _userFeedbackMessage.value = "Failed to submit review."
        }
    }

    fun upvoteReview(reviewId: String) {
        repository.upvoteReview(reviewId)
        _userFeedbackMessage.value = "Marked as helpful 👍"
    }

    fun submitLiveQueueUpdate(centreId: String, updateTag: String, message: String, estimate: String) {
        val res = repository.submitLiveQueueUpdate(centreId, updateTag, message, estimate)
        if (res.isSuccess) {
            _userFeedbackMessage.value = "📡 Live status broadcasted to all citizens!"
        }
    }

    fun upvoteLiveUpdate(updateId: String) {
        repository.upvoteLiveUpdate(updateId)
        _userFeedbackMessage.value = "Update upvoted!"
    }

    fun sendAiMessage(query: String) {
        repository.sendChatMessage(query)
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
