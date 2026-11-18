package com.example.data.repository

import android.content.Context
import com.example.data.api.LocalQEaseApiEngine
import com.example.data.api.QEaseApiService
import com.example.data.local.*
import com.example.data.model.*
import com.example.data.remote.GeminiApiClient
import com.example.domain.engine.CrowdConfidenceEngine
import com.example.domain.engine.QueueStateMachine
import com.example.domain.engine.RagKnowledgeEngine
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class QEaseRepository(private val context: Context) {

    private val engine: LocalQEaseApiEngine = LocalQEaseApiEngine.getInstance(context)
    val api: QEaseApiService = engine
    private val dao = engine.dao
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Preconfigured Citizen & Admin Base Accounts (Staff directory lives dynamically in engine.staffAccounts)
    private val citizenDefaultAccount = UserAccount(
        id = "usr_client_me",
        name = "Dr. Ananya Sharma",
        email = "ananya.sharma@gov.in",
        phone = "+91 9876543210",
        role = UserRole.CUSTOMER,
        department = "Citizen Self-Service Portal",
        pin = "1111",
        isLoggedIn = false
    )

    private val adminDefaultAccount = UserAccount(
        id = "usr_admin_1",
        name = "Chief Officer Vikramaditya",
        email = "admin@qease.gov.in",
        phone = "+91 9811008800",
        role = UserRole.ADMIN,
        pin = "8899",
        department = "District E-Governance & Queue Administration",
        isLoggedIn = false
    )

    // Active Authenticated Session State
    private val _currentUser = MutableStateFlow(citizenDefaultAccount)
    val currentUser: StateFlow<UserAccount> = _currentUser.asStateFlow()

    private val _currentRole = MutableStateFlow(UserRole.CUSTOMER)
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _activeFirebaseUid = MutableStateFlow<String?>(null)
    val activeFirebaseUid: StateFlow<String?> = _activeFirebaseUid.asStateFlow()

    private var pendingCitizenMobile: String? = null
    private var pendingCitizenOtp: String? = null

    // Staff Scoped Context (locked to signed-in officer's assigned centre & counter)
    private val _selectedStaffCentreId = MutableStateFlow("centre_central_psk")
    val selectedStaffCentreId: StateFlow<String> = _selectedStaffCentreId.asStateFlow()

    private val _selectedStaffCounterId = MutableStateFlow("cnt_psk_1")
    val selectedStaffCounterId: StateFlow<String> = _selectedStaffCounterId.asStateFlow()

    // Delegated Operational State Flows
    val services: StateFlow<List<ServiceItem>> = engine.services
    val centres: StateFlow<List<ServiceCentre>> = engine.centres
    val counters: StateFlow<List<Counter>> = engine.counters
    val tokens: StateFlow<List<QueueToken>> = engine.tokens
    val staffAccounts: StateFlow<List<UserAccount>> = engine.staffAccounts
    val proposals: StateFlow<List<ServiceChangeProposal>> = engine.proposals
    val notifications: StateFlow<List<StaffNotification>> = engine.notifications
    val systemSettings: StateFlow<SystemSettings> = engine.systemSettings
    val crowdReports: StateFlow<List<CrowdReport>> = engine.crowdReports
    val reviews: StateFlow<List<CentreReview>> = engine.reviews
    val liveQueueUpdates: StateFlow<List<LiveQueueStatusUpdate>> = engine.liveQueueUpdates
    val auditLogs: StateFlow<List<AuditLog>> = engine.auditLogs

    private val _activeUserToken = MutableStateFlow<QueueToken?>(
        engine.tokens.value.firstOrNull { it.userId == "usr_client_me" && it.status == TokenStatus.WAITING }
    )
    val activeUserToken: StateFlow<QueueToken?> = _activeUserToken.asStateFlow()

    private val _savedCentreIds = MutableStateFlow<Set<String>>(emptySet())
    val savedCentreIds: StateFlow<Set<String>> = _savedCentreIds.asStateFlow()

    private val _chatMessages = MutableStateFlow(listOf(defaultWelcomeMessageForRole(UserRole.CUSTOMER)))
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _syncStatus = MutableStateFlow("Live Sync Active • 14ms")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        observeSavedCentres()
        observeDatabaseEntities()
        observeTokenChanges()
        startFastDataSyncEngine()
    }

    // ==========================================
    // RBAC AUTHENTICATION & SESSION MANAGEMENT
    // ==========================================

    fun requestCitizenOtp(mobileInput: String, fullName: String): Result<String> {
        val cleanName = fullName.trim()
        if (cleanName.length < 2) {
            return Result.failure(
                IllegalArgumentException("Please enter your full name (at least 2 characters).")
            )
        }
        val digits = mobileInput.filter { it.isDigit() }.takeLast(10)
        if (digits.length != 10 || digits.first() !in '6'..'9') {
            return Result.failure(
                IllegalArgumentException("Enter a valid 10-digit Indian mobile number starting with 6–9.")
            )
        }
        val otp = if (digits == "9876543210") "482910" else (100000..999999).random().toString()
        pendingCitizenMobile = digits
        pendingCitizenOtp = otp
        engine.appendAuditLog(
            "AUTH_OTP_SENT",
            "6-digit govt verification OTP dispatched to +91 $digits",
            UserRole.CUSTOMER,
            cleanName
        )
        return Result.success(otp)
    }

    fun verifyCitizenOtp(mobileInput: String, otpInput: String, fullName: String): Result<UserAccount> {
        val cleanName = fullName.trim()
        if (cleanName.length < 2) {
            return Result.failure(IllegalArgumentException("Please enter your full name (at least 2 characters)."))
        }
        val digits = mobileInput.filter { it.isDigit() }.takeLast(10)
        val cleanOtp = otpInput.filter { it.isDigit() }
        if (digits.length != 10 || digits.first() !in '6'..'9') {
            return Result.failure(IllegalArgumentException("Enter a valid 10-digit Indian mobile number."))
        }
        val expectedOtp = pendingCitizenOtp
        if (expectedOtp == null || pendingCitizenMobile != digits) {
            return Result.failure(IllegalArgumentException("Please request a verification OTP for +91 $digits first."))
        }
        if (cleanOtp != expectedOtp) {
            return Result.failure(IllegalArgumentException("Incorrect 6-digit OTP. Please verify and try again."))
        }

        val citizenDigits = citizenDefaultAccount.phone.filter { it.isDigit() }.takeLast(10)
        val cloudUid = _activeFirebaseUid.value
        val account = if (digits == citizenDigits) {
            citizenDefaultAccount.copy(
                id = cloudUid ?: citizenDefaultAccount.id,
                name = cleanName,
                isLoggedIn = true
            )
        } else {
            UserAccount(
                id = cloudUid ?: "usr_${UUID.randomUUID().toString().take(6)}",
                name = cleanName,
                email = "citizen.$digits@qease.gov.in",
                phone = "+91 $digits",
                role = UserRole.CUSTOMER,
                department = "Citizen Self-Service Portal",
                pin = "",
                isLoggedIn = true
            )
        }

        pendingCitizenMobile = null
        pendingCitizenOtp = null

        _currentUser.value = account
        _currentRole.value = UserRole.CUSTOMER
        _isAuthenticated.value = true
        resetChatForRole(UserRole.CUSTOMER)
        engine.startCloudSessionSync(UserRole.CUSTOMER)

        engine.appendAuditLog(
            "AUTH_LOGIN",
            "Citizen ${account.name} verified via Mobile +91 $digits OTP",
            UserRole.CUSTOMER,
            account.name
        )
        return Result.success(account)
    }

    fun authenticate(emailOrPhone: String, pin: String, role: UserRole): Result<UserAccount> {
        if (_isAuthenticated.value && _currentRole.value != role) {
            return Result.failure(IllegalStateException("Active session detected. Logout before switching roles."))
        }
        val trimmedInput = emailOrPhone.trim().lowercase()
        if (trimmedInput.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter your Officer ID or Email."))
        }

        val account = when (role) {
            UserRole.CUSTOMER -> citizenDefaultAccount.copy(
                email = emailOrPhone.trim(),
                isLoggedIn = true
            )
            UserRole.STAFF -> {
                val matchedStaff = engine.staffAccounts.value.find {
                    it.email.lowercase() == trimmedInput ||
                        it.phone.replace(Regex("[^0-9]"), "") == trimmedInput.replace(Regex("[^0-9]"), "")
                }
                if (matchedStaff != null) {
                    if (!matchedStaff.isActive) {
                        return Result.failure(IllegalStateException("This Staff account is suspended. Contact System Admin."))
                    }
                    if (matchedStaff.pin != pin) {
                        return Result.failure(IllegalArgumentException("Incorrect Staff PIN for ${matchedStaff.name}."))
                    }
                    matchedStaff
                } else {
                    if (pin != "1234") {
                        return Result.failure(IllegalArgumentException("Invalid Staff PIN. (Demo PIN: 1234)"))
                    }
                    UserAccount(
                        id = "usr_staff_custom",
                        name = "Desk Officer (${emailOrPhone.take(10)})",
                        email = emailOrPhone.trim(),
                        phone = "+91 9811009941",
                        role = UserRole.STAFF,
                        assignedCentreId = "centre_central_psk",
                        assignedCounterId = "cnt_psk_1",
                        pin = pin,
                        department = "Passport & Civil Registry Desk",
                        isLoggedIn = true,
                        isActive = true
                    )
                }
            }
            UserRole.ADMIN -> {
                if (pin != "8899") {
                    return Result.failure(IllegalArgumentException("Invalid Admin Security Key. (Demo PIN: 8899)"))
                }
                if (trimmedInput == adminDefaultAccount.email.lowercase()) {
                    adminDefaultAccount
                } else {
                    adminDefaultAccount.copy(
                        id = "usr_admin_custom",
                        name = "System Admin (${emailOrPhone.take(10)})",
                        email = emailOrPhone.trim()
                    )
                }
            }
        }

        val activeAccount = account.copy(isLoggedIn = true)
        _currentUser.value = activeAccount
        _currentRole.value = activeAccount.role
        _isAuthenticated.value = true
        engine.startCloudSessionSync(activeAccount.role)

        if (activeAccount.role == UserRole.STAFF) {
            val lockedCentre = activeAccount.assignedCentreId ?: "centre_central_psk"
            val lockedCounter = activeAccount.assignedCounterId ?: "cnt_psk_1"
            _selectedStaffCentreId.value = lockedCentre
            _selectedStaffCounterId.value = lockedCounter
            engine.syncWidgetsFromCurrentState(lockedCentre, lockedCounter, activeAccount.name)
        }
        resetChatForRole(activeAccount.role)

        engine.appendAuditLog(
            "AUTH_LOGIN",
            "User ${activeAccount.name} logged in with role ${activeAccount.role.name}",
            activeAccount.role,
            activeAccount.name,
            activeAccount.assignedCentreId
        )
        return Result.success(activeAccount)
    }

    fun syncGoogleAuthUser(firebaseUser: FirebaseUser?) {
        val uid = firebaseUser?.uid
        _activeFirebaseUid.value = uid
        if (firebaseUser == null) {
            engine.stopCloudSessionSync()
            return
        }
        val activeRole = if (_isAuthenticated.value) _currentRole.value else UserRole.CUSTOMER
        if (_isAuthenticated.value && _currentRole.value == UserRole.CUSTOMER) {
            _currentUser.value = _currentUser.value.copy(
                id = uid ?: _currentUser.value.id,
                email = firebaseUser.email ?: _currentUser.value.email
            )
        }
        engine.startCloudSessionSync(activeRole)
    }

    fun stopCloudSyncBeforeSignOut() {
        engine.stopCloudSessionSync()
    }

    fun logout() {
        val current = _currentUser.value
        engine.appendAuditLog(
            "AUTH_LOGOUT",
            "User ${current.name} logged out from ${current.role.name}",
            current.role,
            current.name,
            current.assignedCentreId
        )
        engine.stopCloudSessionSync()
        pendingCitizenMobile = null
        pendingCitizenOtp = null
        _currentUser.value = citizenDefaultAccount.copy(isLoggedIn = false)
        _currentRole.value = UserRole.CUSTOMER
        _isAuthenticated.value = false
        resetChatForRole(UserRole.CUSTOMER)
    }

    fun setStaffScope(centreId: String, counterId: String) {
        // Locked to assigned officer credentials when Staff is authenticated
        val current = _currentUser.value
        if (_isAuthenticated.value && current.role == UserRole.STAFF && current.assignedCounterId != null) {
            _selectedStaffCentreId.value = current.assignedCentreId ?: centreId
            _selectedStaffCounterId.value = current.assignedCounterId
            return
        }
        _selectedStaffCentreId.value = centreId
        _selectedStaffCounterId.value = counterId
    }

    // ==========================================
    // SYNC & ROOM OBSERVATION
    // ==========================================

    private fun startFastDataSyncEngine() {
        scope.launch {
            while (isActive) {
                delay(12000)
                try {
                    _isSyncing.value = true
                    val latency = (8..22).random()
                    _syncStatus.value = "Live Sync Connected • ${latency}ms"

                    val updatedCentres = engine.centres.value.map { centre ->
                        val reports = engine.crowdReports.value.filter { it.centreId == centre.id }
                        val isVerified = reports.any { it.isStaffVerified }
                        val maxUpvotes = reports.maxOfOrNull { it.upvotesCount } ?: 1
                        val result = CrowdConfidenceEngine.computeConfidence(
                            minutesAgo = centre.lastCrowdUpdateMinutesAgo,
                            isStaffVerified = isVerified,
                            upvotes = maxUpvotes
                        )
                        centre.copy(
                            crowdConfidencePercent = result.confidencePercent,
                            lastCrowdUpdateMinutesAgo = (centre.lastCrowdUpdateMinutesAgo + 1).coerceAtMost(30)
                        )
                    }
                    engine.updateCentres(updatedCentres)
                    _isSyncing.value = false
                } catch (_: Exception) {
                    _syncStatus.value = "Offline Mode (Cached DB)"
                    _isSyncing.value = false
                }
            }
        }
    }

    fun triggerFastSync() {
        scope.launch {
            _isSyncing.value = true
            _syncStatus.value = "Syncing Database..."
            delay(350)
            _syncStatus.value = "Synced Just Now • 6ms"
            _isSyncing.value = false
            engine.recalculateQueuePositions(_selectedStaffCentreId.value)
            engine.syncWidgetsFromCurrentState()
        }
    }

    private fun observeSavedCentres() {
        scope.launch {
            dao.getAllSavedCentres().collect { list ->
                _savedCentreIds.value = list.map { it.id }.toSet()
            }
        }
    }

    private fun observeTokenChanges() {
        scope.launch {
            engine.tokens.collect { allTokens ->
                val userId = _currentUser.value.id
                val firebaseUid = _activeFirebaseUid.value
                val currentActiveId = _activeUserToken.value?.id
                val refreshed = allTokens.firstOrNull {
                    it.id == currentActiveId &&
                        (it.status == TokenStatus.WAITING || it.status == TokenStatus.CALLED || it.status == TokenStatus.SERVING)
                } ?: allTokens.firstOrNull {
                    (it.userId == userId || (firebaseUid != null && it.userId == firebaseUid)) &&
                        (it.status == TokenStatus.WAITING || it.status == TokenStatus.CALLED || it.status == TokenStatus.SERVING)
                }
                _activeUserToken.value = refreshed
            }
        }
    }

    private fun observeDatabaseEntities() {
        scope.launch {
            dao.getAllReviews().collect { entities ->
                if (entities.isNotEmpty()) {
                    engine.updateReviews(
                        entities.map {
                            CentreReview(
                                id = it.id,
                                centreId = it.centreId,
                                centreName = it.centreName,
                                userId = it.userId,
                                userName = it.userName,
                                rating = it.rating,
                                reviewText = it.reviewText,
                                visitTag = it.visitTag,
                                queueSpeedRating = it.queueSpeedRating,
                                timestamp = it.timestamp,
                                helpfulCount = it.helpfulCount
                            )
                        }
                    )
                }
            }
        }

        scope.launch {
            dao.getAllLiveQueueUpdates().collect { entities ->
                if (entities.isNotEmpty()) {
                    engine.updateLiveQueueUpdates(
                        entities.map {
                            LiveQueueStatusUpdate(
                                id = it.id,
                                centreId = it.centreId,
                                centreName = it.centreName,
                                reporterName = it.reporterName,
                                updateTag = it.updateTag,
                                message = it.message,
                                timestamp = it.timestamp,
                                currentServingEstimate = it.currentServingEstimate,
                                upvotes = it.upvotes
                            )
                        }
                    )
                }
            }
        }
    }

    fun toggleSaveCentre(centre: ServiceCentre) {
        scope.launch {
            if (_savedCentreIds.value.contains(centre.id)) {
                dao.deleteSavedCentre(centre.id)
            } else {
                dao.saveCentre(
                    SavedCentreEntity(
                        id = centre.id,
                        name = centre.name,
                        address = centre.address,
                        category = centre.category.name,
                        distanceKm = centre.distanceKm
                    )
                )
            }
        }
    }

    // ==========================================
    // CITIZEN REVIEWS, CROWD REPORTS & LIVE FEED
    // ==========================================

    fun submitReview(
        centreId: String,
        rating: Int,
        reviewText: String,
        visitTag: String,
        queueSpeedRating: String
    ): Result<CentreReview> {
        val centre = centres.value.find { it.id == centreId }
            ?: return Result.failure(IllegalArgumentException("Centre not found"))

        val newReview = CentreReview(
            id = UUID.randomUUID().toString(),
            centreId = centreId,
            centreName = centre.name,
            userId = _currentUser.value.id,
            userName = _currentUser.value.name,
            rating = rating.coerceIn(1, 5),
            reviewText = reviewText.ifBlank { "Smooth experience and organized counters." },
            visitTag = visitTag.ifBlank { "General Visit" },
            queueSpeedRating = queueSpeedRating,
            timestamp = System.currentTimeMillis(),
            helpfulCount = 0
        )

        val updatedReviews = listOf(newReview) + reviews.value
        engine.updateReviews(updatedReviews)

        val allCentreReviews = updatedReviews.filter { it.centreId == centreId }
        val newAvg = if (allCentreReviews.isNotEmpty()) allCentreReviews.map { it.rating }.average() else centre.rating
        val roundedAvg = String.format("%.1f", newAvg).toDoubleOrNull() ?: 4.8
        val newReviewCount = centre.totalReviewsCount + 1
        engine.updateCentres(
            centres.value.map {
                if (it.id == centreId) {
                    it.copy(
                        rating = roundedAvg,
                        totalReviewsCount = newReviewCount
                    )
                } else it
            }
        )
        engine.cloudSync?.pushReviewCreated(newReview)
        engine.cloudSync?.pushCentreRatingUpdated(centreId, roundedAvg, newReviewCount)

        scope.launch {
            dao.insertReview(
                CentreReviewEntity(
                    id = newReview.id,
                    centreId = newReview.centreId,
                    centreName = newReview.centreName,
                    userId = newReview.userId,
                    userName = newReview.userName,
                    rating = newReview.rating,
                    reviewText = newReview.reviewText,
                    visitTag = newReview.visitTag,
                    queueSpeedRating = newReview.queueSpeedRating,
                    timestamp = newReview.timestamp,
                    helpfulCount = 0
                )
            )
        }

        engine.appendAuditLog(
            "REVIEW_SUBMITTED",
            "User ${_currentUser.value.name} gave $rating stars to ${centre.name}",
            UserRole.CUSTOMER,
            _currentUser.value.name,
            centreId
        )
        return Result.success(newReview)
    }

    fun upvoteReview(reviewId: String) {
        val target = reviews.value.find { it.id == reviewId }
        val newHelpfulCount = (target?.helpfulCount ?: 0) + 1
        engine.updateReviews(
            reviews.value.map { if (it.id == reviewId) it.copy(helpfulCount = newHelpfulCount) else it }
        )
        engine.cloudSync?.pushReviewUpvoted(reviewId, newHelpfulCount)
        scope.launch { dao.upvoteReview(reviewId) }
    }

    fun submitLiveQueueUpdate(
        centreId: String,
        updateTag: String,
        message: String,
        currentServingEstimate: String
    ): Result<LiveQueueStatusUpdate> {
        val centre = centres.value.find { it.id == centreId }
            ?: return Result.failure(IllegalArgumentException("Centre not found"))

        val newUpdate = LiveQueueStatusUpdate(
            id = UUID.randomUUID().toString(),
            centreId = centreId,
            centreName = centre.name,
            reporterName = _currentUser.value.name,
            updateTag = updateTag,
            message = message,
            timestamp = System.currentTimeMillis(),
            currentServingEstimate = currentServingEstimate,
            upvotes = 1
        )

        engine.updateLiveQueueUpdates(listOf(newUpdate) + liveQueueUpdates.value)
        engine.cloudSync?.pushLiveUpdateCreated(newUpdate)
        scope.launch {
            dao.insertLiveQueueUpdate(
                LiveQueueUpdateEntity(
                    id = newUpdate.id,
                    centreId = newUpdate.centreId,
                    centreName = newUpdate.centreName,
                    reporterName = newUpdate.reporterName,
                    updateTag = newUpdate.updateTag,
                    message = newUpdate.message,
                    timestamp = newUpdate.timestamp,
                    currentServingEstimate = newUpdate.currentServingEstimate,
                    upvotes = 1
                )
            )
        }

        engine.appendAuditLog(
            "LIVE_QUEUE_BROADCAST",
            "Crowdsourced update ($updateTag) posted for ${centre.name}",
            UserRole.CUSTOMER,
            _currentUser.value.name,
            centreId
        )
        return Result.success(newUpdate)
    }

    fun upvoteLiveUpdate(updateId: String) {
        val target = liveQueueUpdates.value.find { it.id == updateId }
        val newUpvotes = (target?.upvotes ?: 0) + 1
        engine.updateLiveQueueUpdates(
            liveQueueUpdates.value.map { if (it.id == updateId) it.copy(upvotes = newUpvotes) else it }
        )
        engine.cloudSync?.pushLiveUpdateUpvoted(updateId, newUpvotes)
        scope.launch { dao.upvoteLiveUpdate(updateId) }
    }

    fun submitCrowdReport(
        centreId: String,
        level: CrowdLevel,
        peopleCount: Int,
        reportedWait: Int,
        notes: String,
        reporterName: String
    ) {
        val centre = centres.value.find { it.id == centreId } ?: return
        val report = CrowdReport(
            id = UUID.randomUUID().toString(),
            centreId = centreId,
            centreName = centre.name,
            crowdLevel = level,
            approxPeopleCount = peopleCount,
            reportedWaitMinutes = reportedWait,
            notes = notes,
            reporterName = reporterName.ifBlank { _currentUser.value.name },
            timestamp = System.currentTimeMillis()
        )
        engine.updateCrowdReports(listOf(report) + crowdReports.value)
        val updatedCentre = centre.copy(
            currentCrowdLevel = level,
            lastCrowdUpdateMinutesAgo = 1,
            crowdConfidencePercent = 95,
            estimatedWaitMin = (reportedWait * 0.85).toInt(),
            estimatedWaitMax = (reportedWait * 1.25).toInt()
        )
        engine.updateCentres(
            centres.value.map { if (it.id == centreId) updatedCentre else it }
        )
        engine.cloudSync?.pushCrowdReportCreated(report)
        scope.launch {
            dao.insertCrowdReport(
                OfflineCrowdReportEntity(
                    id = report.id,
                    centreId = report.centreId,
                    centreName = report.centreName,
                    crowdLevel = report.crowdLevel.name,
                    approxPeopleCount = report.approxPeopleCount,
                    reportedWaitMinutes = report.reportedWaitMinutes,
                    notes = report.notes,
                    timestamp = report.timestamp
                )
            )
        }
        engine.appendAuditLog(
            "CROWD_REPORTED",
            "Citizen report: ${level.label} (~$peopleCount people) for ${centre.name}",
            UserRole.CUSTOMER,
            report.reporterName,
            centreId
        )
    }

    fun verifyCrowdReport(reportId: String, staffName: String) {
        val report = crowdReports.value.find { it.id == reportId } ?: return
        val verified = report.copy(isStaffVerified = true, confidenceScore = 100)
        engine.updateCrowdReports(crowdReports.value.map { if (it.id == reportId) verified else it })
        val updatedCentre = centres.value.find { it.id == report.centreId }
            ?.copy(crowdConfidencePercent = 100, lastCrowdUpdateMinutesAgo = 1)
        if (updatedCentre != null) {
            engine.updateCentres(
                centres.value.map { if (it.id == report.centreId) updatedCentre else it }
            )
            engine.cloudSync?.pushCentreCrowdMetricsUpdated(updatedCentre)
        }
        engine.cloudSync?.pushCrowdReportVerified(reportId, 100)
        engine.appendAuditLog(
            "REPORT_STAFF_VERIFIED",
            "Staff $staffName verified crowd report for ${report.centreName}",
            UserRole.STAFF,
            staffName,
            report.centreId
        )
    }

    // ==========================================
    // QUEUE & STAFF / ADMIN DELEGATED APIS
    // ==========================================

    fun joinQueue(
        centreId: String,
        serviceId: String,
        userName: String,
        userPhone: String
    ): Result<QueueToken> {
        val res = engine.joinQueue(
            centreId = centreId,
            serviceId = serviceId,
            userId = _activeFirebaseUid.value ?: _currentUser.value.id,
            userName = userName.ifBlank { _currentUser.value.name },
            userPhone = userPhone.ifBlank { _currentUser.value.phone },
            actorRole = _currentRole.value
        )
        res.getOrNull()?.let { _activeUserToken.value = it }
        return res
    }

    fun cancelUserToken(tokenId: String): Boolean {
        val token = tokens.value.find { it.id == tokenId } ?: return false
        if (!QueueStateMachine.canTransition(token.status, TokenStatus.CANCELLED, _currentRole.value)) {
            return false
        }
        val updated = token.copy(status = TokenStatus.CANCELLED)
        engine.updateTokens(tokens.value.map { if (it.id == tokenId) updated else it })
        if (_activeUserToken.value?.id == tokenId) _activeUserToken.value = null
        engine.recalculateQueuePositions(token.centreId)
        engine.cloudSync?.pushTokenCancelled(tokenId)
        engine.appendAuditLog(
            "TOKEN_CANCELLED",
            "Token ${token.tokenNumber} cancelled by user",
            _currentRole.value,
            _currentUser.value.name,
            token.centreId
        )
        return true
    }

    fun callNextCustomer(counterId: String, staffName: String): Result<QueueToken> =
        api.callNextCustomer(counterId, staffName)

    fun markServing(tokenId: String): Boolean =
        api.markServing(tokenId, _currentUser.value.name).isSuccess

    fun completeService(tokenId: String): Boolean =
        api.completeService(tokenId, _currentUser.value.name).isSuccess

    fun markNoShow(tokenId: String): Boolean =
        api.markNoShow(tokenId, _currentUser.value.name).isSuccess

    fun toggleCounterStatus(counterId: String) {
        api.toggleCounterStatus(counterId, _currentUser.value.name)
    }

    fun toggleServiceAvailability(serviceId: String) {
        val srv = services.value.find { it.id == serviceId } ?: return
        val updated = srv.copy(isAvailable = !srv.isAvailable)
        engine.updateServices(services.value.map { if (it.id == serviceId) updated else it })
        engine.cloudSync?.pushServiceUpdated(updated)
        engine.appendAuditLog(
            "SERVICE_AVAILABILITY_CHANGED",
            "${srv.title} availability set to ${updated.isAvailable}",
            UserRole.ADMIN,
            _currentUser.value.name
        )
    }

    fun updateCentreNotice(centreId: String, notice: String) {
        val centre = centres.value.find { it.id == centreId } ?: return
        val updated = centre.copy(liveNotice = notice.ifBlank { null })
        engine.updateCentres(centres.value.map { if (it.id == centreId) updated else it })
        engine.cloudSync?.pushCentreUpserted(updated, false)
        engine.appendAuditLog(
            "NOTICE_UPDATED",
            "Updated notice for ${centre.name}: '$notice'",
            UserRole.ADMIN,
            _currentUser.value.name,
            centreId
        )
    }

    // ==========================================
    // GROUNDED AI / ROLE-AWARE ACTION ASSISTANT
    // ==========================================

    private fun defaultWelcomeMessageForRole(role: UserRole): ChatMessage {
        return when (role) {
            UserRole.CUSTOMER -> ChatMessage(
                id = "welcome_ai_customer",
                isUser = false,
                message = "## Welcome to QEase Service Intelligence\nI provide official document checklists, fee structures, eligibility criteria, and estimated waiting times across service centres.\n\nTry asking:\n- What documents are required for Passport renewal?\n- What is the fee for Driving License renewal?\n- How long is the wait at Central Passport Seva Kendra?",
                groundedSources = listOf("QEase Verified Knowledge Base")
            )
            UserRole.STAFF -> ChatMessage(
                id = "welcome_ai_staff",
                isUser = false,
                message = "## Counter Desk AI Copilot\nI assist with your assigned counter queue, SOP document verification rules, and drafting Service Info or Required Document change proposals for Admin approval.\n\nQuick actions:\n- Draft a service duration or caution update proposal\n- Suggest a required document checklist clarification\n- Check live waiting load and unverified crowd reports",
                groundedSources = listOf("Assigned Desk Telemetry", "QEase SOP Guidelines")
            )
            UserRole.ADMIN -> ChatMessage(
                id = "welcome_ai_admin",
                isUser = false,
                message = "## Governance & Audit AI Assistant\nI audit pending Staff change proposals, highlight cross-centre queue bottlenecks, and summarize system policy and audit trail events.\n\nQuick audits:\n- Audit pending Staff proposals awaiting review\n- Identify high-wait bottleneck centres and counters\n- Review global maintenance mode and policy settings",
                groundedSources = listOf("Live Governance Telemetry", "System Audit Trail")
            )
        }
    }

    fun resetChatForRole(role: UserRole) {
        _isAiThinking.value = false
        _chatMessages.value = listOf(defaultWelcomeMessageForRole(role))
    }

    fun sendChatMessage(query: String) {
        val role = _currentRole.value
        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            message = query,
            timestamp = System.currentTimeMillis()
        )
        _chatMessages.value = _chatMessages.value + userMsg
        _isAiThinking.value = true

        scope.launch {
            val retrievedDocs = RagKnowledgeEngine.searchGroundedKnowledge(query)
            val matchedService = services.value.firstOrNull { srv ->
                query.contains(srv.title, ignoreCase = true) ||
                    query.contains(srv.code, ignoreCase = true) ||
                    srv.requiredDocuments.any { query.contains(it.name, ignoreCase = true) }
            } ?: retrievedDocs.firstOrNull()?.serviceId?.let { id -> services.value.find { it.id == id } }

            val q = query.lowercase()

            when (role) {
                UserRole.STAFF -> {
                    val assignedCentreId = _currentUser.value.assignedCentreId ?: _selectedStaffCentreId.value
                    val assignedCounterId = _currentUser.value.assignedCounterId ?: _selectedStaffCounterId.value
                    val centre = centres.value.find { it.id == assignedCentreId } ?: centres.value.firstOrNull()
                    val counter = counters.value.find { it.id == assignedCounterId } ?: counters.value.firstOrNull()
                    val waitingCount = tokens.value.count { it.centreId == centre?.id && it.status == TokenStatus.WAITING }
                    val pendingProposals = proposals.value.filter { it.status == ProposalStatus.PENDING }
                    val unverifiedReports = crowdReports.value.filter { !it.isStaffVerified }
                    val scopedService = matchedService
                        ?: centre?.availableServiceIds?.firstOrNull()?.let { id -> services.value.find { it.id == id } }
                        ?: services.value.firstOrNull()

                    val staffTelemetry = buildString {
                        appendLine("STAFF ASSIGNED DESK SNAPSHOT:")
                        appendLine("- Officer: ${_currentUser.value.name}")
                        appendLine("- Centre: ${centre?.name ?: "Assigned Centre"}")
                        appendLine("- Locked Counter: #${counter?.counterNumber ?: 1} (${counter?.name ?: "Desk"})")
                        appendLine("- Waiting Queue Count: $waitingCount")
                        appendLine("- Pending Proposals in Review: ${pendingProposals.size}")
                        appendLine("- Unverified Field Crowd Reports: ${unverifiedReports.size}")
                        scopedService?.let {
                            appendLine("- Primary Service: ${it.title} (${it.officialFee}, ${it.avgProcessingDurationMin} mins)")
                        }
                        if (retrievedDocs.isNotEmpty()) {
                            appendLine()
                            appendLine(retrievedDocs.joinToString("\n\n") { "${it.title}:\n${it.content}" })
                        }
                    }

                    val geminiResult = GeminiApiClient.callGeminiGrounded(
                        userQuery = query,
                        groundedContext = staffTelemetry,
                        role = UserRole.STAFF
                    )

                    val aiResponse = geminiResult.getOrNull()?.takeIf { it.isNotBlank() }
                        ?: RagKnowledgeEngine.generateStaffCopilotAnswer(
                            query = query,
                            retrievedDocs = retrievedDocs,
                            centreName = centre?.name ?: "Assigned Centre",
                            counterNumber = counter?.counterNumber ?: 1,
                            waitingCount = waitingCount,
                            pendingProposalsCount = pendingProposals.size,
                            unverifiedReportsCount = unverifiedReports.size,
                            primaryServiceTitle = scopedService?.title ?: "Service Desk",
                            primaryServiceFee = scopedService?.officialFee ?: "Standard",
                            primaryServiceDuration = scopedService?.avgProcessingDurationMin ?: 15
                        )

                    val workflowAction = when {
                        q.contains("doc") || q.contains("checklist") || q.contains("proof") || q.contains("alternative") -> {
                            AiWorkflowAction(
                                type = AiActionType.PREFILL_STAFF_DOC_PROPOSAL,
                                label = "Draft Doc Proposal (${scopedService?.code ?: "Service"})",
                                targetServiceId = scopedService?.id,
                                prefillDocName = "Self-Attested Address Proof (Utility / Rental)",
                                prefillDocDescription = "Must be issued within last 90 days and match applicant address.",
                                prefillRationale = "Clarify valid 90-day address proof rule based on Counter ${counter?.counterNumber ?: 1} intake checks."
                            )
                        }
                        q.contains("crowd") || q.contains("verify") || q.contains("report") || q.contains("alert") -> {
                            val targetRep = unverifiedReports.firstOrNull { it.centreId == centre?.id }
                                ?: unverifiedReports.firstOrNull()
                            if (targetRep != null) {
                                AiWorkflowAction(
                                    type = AiActionType.VERIFY_LATEST_CROWD_REPORT,
                                    label = "Verify Field Crowd Report (${targetRep.centreName.take(18)})",
                                    targetReportId = targetRep.id
                                )
                            } else {
                                AiWorkflowAction(
                                    type = AiActionType.OPEN_STAFF_ALERTS,
                                    label = "Open Desk Alerts & Crowd"
                                )
                            }
                        }
                        q.contains("propos") || q.contains("fee") || q.contains("duration") || q.contains("caution") || q.contains("delay") || q.contains("update") -> {
                            val suggestedDur = ((scopedService?.avgProcessingDurationMin ?: 15) - 2).coerceAtLeast(5)
                            AiWorkflowAction(
                                type = AiActionType.PREFILL_STAFF_SERVICE_PROPOSAL,
                                label = "Prefill Service Update Proposal",
                                targetServiceId = scopedService?.id,
                                prefillFee = scopedService?.officialFee,
                                prefillDurationMin = suggestedDur,
                                prefillCautionNote = "Carry original ID & self-attested copies to avoid counter delay.",
                                prefillRationale = "Recalibrate average handling time to ${suggestedDur}m based on Counter ${counter?.counterNumber ?: 1} throughput."
                            )
                        }
                        else -> AiWorkflowAction(
                            type = AiActionType.OPEN_STAFF_DESK,
                            label = "Return to Queue Desk"
                        )
                    }

                    val assistantMsg = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        isUser = false,
                        message = aiResponse,
                        timestamp = System.currentTimeMillis(),
                        groundedSources = listOf("Assigned Desk Telemetry") + retrievedDocs.map { it.title },
                        suggestedServiceId = scopedService?.id,
                        workflowAction = workflowAction
                    )

                    withContext(Dispatchers.Main) {
                        _chatMessages.value = _chatMessages.value + assistantMsg
                        _isAiThinking.value = false
                    }
                }

                UserRole.ADMIN -> {
                    val stats = api.computeQueueStatistics(null)
                    val pendingList = proposals.value.filter { it.status == ProposalStatus.PENDING }
                    val pendingSummaries = pendingList.map {
                        "${it.serviceTitle} (${it.proposalType.label}) by ${it.submittedByStaffName}: ${it.rationale}"
                    }
                    val bottlenecks = centres.value.sortedByDescending { it.estimatedWaitMax }.map { ctr ->
                        val waitCnt = tokens.value.count { it.centreId == ctr.id && it.status == TokenStatus.WAITING }
                        "${ctr.name}: $waitCnt waiting, est ${ctr.estimatedWaitMin}-${ctr.estimatedWaitMax}m, ${ctr.activeCountersCount}/${ctr.totalCountersCount} counters (${if (ctr.isVirtualQueueOpen) "Open" else "Paused"})"
                    }
                    val auditSummary = auditLogs.value.take(5).map { "${it.action} by ${it.actorName}: ${it.details}" }
                    val settings = systemSettings.value

                    val adminContext = buildString {
                        appendLine("ADMIN GOVERNANCE TELEMETRY SNAPSHOT:")
                        appendLine("- Waiting Citizens: ${stats.waitingCount}, Completion Rate: ${stats.completionRatePercent}%, Avg Wait: ${stats.avgWaitMinutes}m")
                        appendLine("- Counters Active: ${stats.activeCountersCount}/${stats.totalCountersCount}")
                        appendLine("- Maintenance Mode: ${settings.maintenanceMode} (NoShow Grace: ${settings.autoNoShowTimeoutMinutes}m, Max Tokens: ${settings.maxActiveTokensPerCitizen})")
                        appendLine("- Pending Proposals (${pendingList.size}):")
                        pendingSummaries.forEach { appendLine("  * $it") }
                        appendLine("- Centre Loads:")
                        bottlenecks.forEach { appendLine("  * $it") }
                        appendLine("- Recent Audit Logs:")
                        auditSummary.forEach { appendLine("  * $it") }
                    }

                    val geminiResult = GeminiApiClient.callGeminiGrounded(
                        userQuery = query,
                        groundedContext = adminContext,
                        role = UserRole.ADMIN
                    )

                    val aiResponse = geminiResult.getOrNull()?.takeIf { it.isNotBlank() }
                        ?: RagKnowledgeEngine.generateAdminAuditAnswer(
                            query = query,
                            pendingProposalsSummary = pendingSummaries,
                            bottleneckSummary = bottlenecks,
                            statsWaiting = stats.waitingCount,
                            activeCounters = stats.activeCountersCount,
                            totalCounters = stats.totalCountersCount,
                            maintenanceMode = settings.maintenanceMode,
                            recentAuditSummary = auditSummary
                        )

                    val workflowAction = when {
                        q.contains("propos") || q.contains("approv") || q.contains("review") || q.contains("risk") -> {
                            AiWorkflowAction(
                                type = AiActionType.OPEN_ADMIN_APPROVALS,
                                label = "Review Pending Approvals (${pendingList.size})",
                                targetProposalId = pendingList.firstOrNull()?.id
                            )
                        }
                        q.contains("maint") || q.contains("policy") || q.contains("setting") || q.contains("timeout") -> {
                            AiWorkflowAction(
                                type = AiActionType.OPEN_ADMIN_OPS_SETTINGS,
                                label = "Open System Policy & Settings"
                            )
                        }
                        q.contains("staff") || q.contains("officer") || q.contains("roster") -> {
                            AiWorkflowAction(
                                type = AiActionType.OPEN_ADMIN_OPS_STAFF,
                                label = "Manage Staff Roster in Ops"
                            )
                        }
                        q.contains("bottleneck") || q.contains("wait") || q.contains("centre") || q.contains("center") -> {
                            AiWorkflowAction(
                                type = AiActionType.OPEN_ADMIN_OPS_CENTRES,
                                label = "Manage Centres & Counters",
                                targetCentreId = centres.value.maxByOrNull { it.estimatedWaitMax }?.id
                            )
                        }
                        else -> {
                            AiWorkflowAction(
                                type = AiActionType.OPEN_ADMIN_ANALYTICS,
                                label = "Inspect Analytics & Audit Trail"
                            )
                        }
                    }

                    val assistantMsg = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        isUser = false,
                        message = aiResponse,
                        timestamp = System.currentTimeMillis(),
                        groundedSources = listOf("Live Governance Telemetry", "System Audit Trail"),
                        workflowAction = workflowAction
                    )

                    withContext(Dispatchers.Main) {
                        _chatMessages.value = _chatMessages.value + assistantMsg
                        _isAiThinking.value = false
                    }
                }

                UserRole.CUSTOMER -> {
                    val contextText = retrievedDocs.joinToString("\n\n") {
                        "${it.title} (${it.officialAuthority}):\n${it.content}"
                    }

                    val geminiResult = GeminiApiClient.callGeminiGrounded(
                        userQuery = query,
                        groundedContext = contextText,
                        role = UserRole.CUSTOMER
                    )

                    val aiResponse = if (geminiResult.isSuccess && !geminiResult.getOrNull().isNullOrBlank()) {
                        geminiResult.getOrNull()!!
                    } else {
                        RagKnowledgeEngine.generateGroundedAnswer(query = query, retrievedDocs = retrievedDocs)
                    }

                    val targetServiceId = matchedService?.id ?: retrievedDocs.firstOrNull()?.serviceId
                    val assistantMsg = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        isUser = false,
                        message = aiResponse,
                        timestamp = System.currentTimeMillis(),
                        groundedSources = retrievedDocs.map { it.title },
                        suggestedServiceId = targetServiceId,
                        workflowAction = targetServiceId?.let {
                            AiWorkflowAction(
                                type = AiActionType.OPEN_SERVICE_CHECKLIST,
                                label = "View Verified Document Checklist",
                                targetServiceId = it
                            )
                        }
                    )

                    withContext(Dispatchers.Main) {
                        _chatMessages.value = _chatMessages.value + assistantMsg
                        _isAiThinking.value = false
                    }
                }
            }
        }
    }
}
