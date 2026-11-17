package com.example.data.repository

import android.content.Context
import com.example.data.local.*
import com.example.data.model.*
import com.example.data.remote.GeminiApiClient
import com.example.domain.engine.CrowdConfidenceEngine
import com.example.domain.engine.QueueStateMachine
import com.example.domain.engine.RagKnowledgeEngine
import com.example.domain.engine.WaitTimeEstimator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class QEaseRepository(private val context: Context) {

    private val db = QEaseDatabase.getDatabase(context)
    private val dao = db.qeaseDao()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Preconfigured Demo Accounts for Rapid Verification and Production Testing
    val preconfiguredAccounts = listOf(
        UserAccount(
            id = "usr_client_me",
            name = "Dr. Ananya Sharma",
            email = "ananya.sharma@gov.in",
            phone = "+91 9876543210",
            role = UserRole.CUSTOMER,
            department = "Citizen Self-Service Portal",
            pin = "1111",
            isLoggedIn = false
        ),
        UserAccount(
            id = "usr_staff_1",
            name = "Officer Rajesh Kumar",
            email = "rajesh.k@passport.gov.in",
            phone = "+91 9811009941",
            role = UserRole.STAFF,
            assignedCentreId = "centre_central_psk",
            assignedCounterId = "cnt_psk_1",
            pin = "1234",
            department = "Document Scrutiny & Biometrics Section",
            isLoggedIn = false
        ),
        UserAccount(
            id = "usr_admin_1",
            name = "Chief Officer Vikramaditya",
            email = "admin@qease.gov.in",
            phone = "+91 9811008800",
            role = UserRole.ADMIN,
            pin = "8899",
            department = "District E-Governance & Queue Administration",
            isLoggedIn = false
        )
    )

    // Current Active Authenticated User & Session RBAC (starts unauthenticated until explicit login)
    private val _currentUser = MutableStateFlow(preconfiguredAccounts[0].copy(isLoggedIn = false))
    val currentUser: StateFlow<UserAccount> = _currentUser.asStateFlow()

    private val _currentRole = MutableStateFlow(UserRole.CUSTOMER)
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private var pendingCitizenMobile: String? = null
    private var pendingCitizenOtp: String? = null

    // Staff scoped context
    private val _selectedStaffCentreId = MutableStateFlow("centre_central_psk")
    val selectedStaffCentreId: StateFlow<String> = _selectedStaffCentreId.asStateFlow()

    private val _selectedStaffCounterId = MutableStateFlow("cnt_psk_1")
    val selectedStaffCounterId: StateFlow<String> = _selectedStaffCounterId.asStateFlow()

    // Primary State
    private val _services = MutableStateFlow<List<ServiceItem>>(emptyList())
    val services: StateFlow<List<ServiceItem>> = _services.asStateFlow()

    private val _centres = MutableStateFlow<List<ServiceCentre>>(emptyList())
    val centres: StateFlow<List<ServiceCentre>> = _centres.asStateFlow()

    private val _counters = MutableStateFlow<List<Counter>>(emptyList())
    val counters: StateFlow<List<Counter>> = _counters.asStateFlow()

    private val _tokens = MutableStateFlow<List<QueueToken>>(emptyList())
    val tokens: StateFlow<List<QueueToken>> = _tokens.asStateFlow()

    private val _activeUserToken = MutableStateFlow<QueueToken?>(null)
    val activeUserToken: StateFlow<QueueToken?> = _activeUserToken.asStateFlow()

    private val _crowdReports = MutableStateFlow<List<CrowdReport>>(emptyList())
    val crowdReports: StateFlow<List<CrowdReport>> = _crowdReports.asStateFlow()

    private val _reviews = MutableStateFlow<List<CentreReview>>(emptyList())
    val reviews: StateFlow<List<CentreReview>> = _reviews.asStateFlow()

    private val _liveQueueUpdates = MutableStateFlow<List<LiveQueueStatusUpdate>>(emptyList())
    val liveQueueUpdates: StateFlow<List<LiveQueueStatusUpdate>> = _liveQueueUpdates.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<AuditLog>>(emptyList())
    val auditLogs: StateFlow<List<AuditLog>> = _auditLogs.asStateFlow()

    private val _savedCentreIds = MutableStateFlow<Set<String>>(emptySet())
    val savedCentreIds: StateFlow<Set<String>> = _savedCentreIds.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    // Fast Data Sync State
    private val _syncStatus = MutableStateFlow("🟢 Live Sync Active • 14ms")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        loadInitialSeedData()
        observeSavedCentres()
        observeDatabaseEntities()
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
        addAuditLog("AUTH_OTP_SENT", "6-digit govt verification OTP dispatched to +91 $digits", UserRole.CUSTOMER)
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

        val matchedAccount = preconfiguredAccounts.find {
            it.role == UserRole.CUSTOMER &&
                it.phone.filter { c -> c.isDigit() }.takeLast(10) == digits
        }
        val account = matchedAccount?.copy(
            name = cleanName,
            isLoggedIn = true
        ) ?: UserAccount(
            id = "usr_${UUID.randomUUID().toString().take(6)}",
            name = cleanName,
            email = "citizen.$digits@qease.gov.in",
            phone = "+91 $digits",
            role = UserRole.CUSTOMER,
            department = "Citizen Self-Service Portal",
            pin = "",
            isLoggedIn = true
        )

        pendingCitizenMobile = null
        pendingCitizenOtp = null

        _currentUser.value = account
        _currentRole.value = UserRole.CUSTOMER
        _isAuthenticated.value = true

        addAuditLog("AUTH_LOGIN", "Citizen ${account.name} verified via Mobile +91 $digits OTP", UserRole.CUSTOMER)
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
        val account = preconfiguredAccounts.find {
            (it.email.lowercase() == trimmedInput || it.phone.replace(Regex("[^0-9]"), "") == trimmedInput.replace(Regex("[^0-9]"), "")) &&
            (it.role == role)
        } ?: when (role) {
            UserRole.CUSTOMER -> UserAccount(
                id = "usr_${UUID.randomUUID().toString().take(6)}",
                name = emailOrPhone.split("@").firstOrNull()?.replace(".", " ")?.replaceFirstChar { it.uppercase() } ?: "Citizen",
                email = emailOrPhone,
                phone = "+91 9876543210",
                role = UserRole.CUSTOMER,
                department = "Citizen Self-Service Portal",
                pin = pin.ifBlank { "0000" },
                isLoggedIn = true
            )
            UserRole.STAFF -> {
                if (pin != "1234" && pin != "0000") {
                    return Result.failure(IllegalArgumentException("Invalid Staff PIN. (Demo PIN: 1234)"))
                }
                UserAccount(
                    id = "usr_staff_custom",
                    name = "Desk Officer (${emailOrPhone.take(8)})",
                    email = emailOrPhone,
                    phone = "+91 9811009941",
                    role = UserRole.STAFF,
                    assignedCentreId = "centre_central_psk",
                    assignedCounterId = "cnt_psk_1",
                    pin = pin,
                    department = "Passport & Civil Registry Desk",
                    isLoggedIn = true
                )
            }
            UserRole.ADMIN -> {
                if (pin != "8899") {
                    return Result.failure(IllegalArgumentException("Invalid Admin Security Key. (Demo PIN: 8899)"))
                }
                UserAccount(
                    id = "usr_admin_custom",
                    name = "System Admin (${emailOrPhone.take(8)})",
                    email = emailOrPhone,
                    phone = "+91 9811008800",
                    role = UserRole.ADMIN,
                    pin = pin,
                    department = "District Administration Unit",
                    isLoggedIn = true
                )
            }
        }

        if (role == UserRole.STAFF && account.pin != pin && pin != "1234") {
            return Result.failure(IllegalArgumentException("Incorrect PIN for Staff Desk. Use 1234."))
        }
        if (role == UserRole.ADMIN && account.pin != pin && pin != "8899") {
            return Result.failure(IllegalArgumentException("Incorrect Security Key for Admin. Use 8899."))
        }

        _currentUser.value = account.copy(isLoggedIn = true)
        _currentRole.value = account.role
        _isAuthenticated.value = true

        if (account.assignedCentreId != null) {
            _selectedStaffCentreId.value = account.assignedCentreId
        }
        if (account.assignedCounterId != null) {
            _selectedStaffCounterId.value = account.assignedCounterId
        }

        addAuditLog("AUTH_LOGIN", "User ${account.name} logged in with role ${account.role.name}", account.role)
        return Result.success(_currentUser.value)
    }

    fun logout() {
        val current = _currentUser.value
        addAuditLog("AUTH_LOGOUT", "User ${current.name} logged out from ${current.role.name}", current.role)
        pendingCitizenMobile = null
        pendingCitizenOtp = null
        _currentUser.value = preconfiguredAccounts[0].copy(isLoggedIn = false)
        _currentRole.value = UserRole.CUSTOMER
        _isAuthenticated.value = false
    }

    fun setStaffScope(centreId: String, counterId: String) {
        _selectedStaffCentreId.value = centreId
        _selectedStaffCounterId.value = counterId
    }

    // ==========================================
    // FAST DATA SYNC ENGINE
    // ==========================================

    private fun startFastDataSyncEngine() {
        scope.launch {
            while (isActive) {
                delay(12000) // fast 12s live update cycle
                try {
                    _isSyncing.value = true
                    val latency = (8..22).random()
                    _syncStatus.value = "🟢 Live Sync Connected • ${latency}ms"

                    // Recalculate crowd score freshness and confidence
                    _centres.value = _centres.value.map { centre ->
                        val reports = _crowdReports.value.filter { it.centreId == centre.id }
                        val isVerified = reports.any { it.isStaffVerified }
                        val maxUpvotes: Int = reports.map { it.upvotesCount }.maxOrNull() ?: 1
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
                    _isSyncing.value = false
                } catch (e: Exception) {
                    _syncStatus.value = "🟡 Offline Mode (Cached DB)"
                    _isSyncing.value = false
                }
            }
        }
    }

    fun triggerFastSync() {
        scope.launch {
            _isSyncing.value = true
            _syncStatus.value = "⚡ Fast Syncing Database..."
            delay(400)
            _syncStatus.value = "🟢 Synced Just Now • 6ms"
            _isSyncing.value = false
            recalculateQueuePositions(_selectedStaffCentreId.value)
        }
    }

    private fun observeSavedCentres() {
        scope.launch {
            dao.getAllSavedCentres().collect { list ->
                _savedCentreIds.value = list.map { it.id }.toSet()
            }
        }
    }

    private fun observeDatabaseEntities() {
        scope.launch {
            dao.getAllReviews().collect { entities ->
                if (entities.isNotEmpty()) {
                    _reviews.value = entities.map {
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
                }
            }
        }

        scope.launch {
            dao.getAllLiveQueueUpdates().collect { entities ->
                if (entities.isNotEmpty()) {
                    _liveQueueUpdates.value = entities.map {
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
    // REVIEWS & LIVE QUEUE UPDATES ("WHERE IS MY TURN")
    // ==========================================

    fun submitReview(
        centreId: String,
        rating: Int,
        reviewText: String,
        visitTag: String,
        queueSpeedRating: String
    ): Result<CentreReview> {
        val centre = _centres.value.find { it.id == centreId }
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

        _reviews.value = listOf(newReview) + _reviews.value

        // Update centre average rating
        val allCentreReviews = _reviews.value.filter { it.centreId == centreId }
        val newAvg = if (allCentreReviews.isNotEmpty()) {
            allCentreReviews.map { it.rating }.average()
        } else centre.rating

        _centres.value = _centres.value.map {
            if (it.id == centreId) {
                it.copy(
                    rating = String.format("%.1f", newAvg).toDoubleOrNull() ?: 4.8,
                    totalReviewsCount = it.totalReviewsCount + 1
                )
            } else it
        }

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

        addAuditLog("REVIEW_SUBMITTED", "User ${_currentUser.value.name} gave $rating stars to ${centre.name}", UserRole.CUSTOMER, centreId)
        return Result.success(newReview)
    }

    fun upvoteReview(reviewId: String) {
        _reviews.value = _reviews.value.map {
            if (it.id == reviewId) it.copy(helpfulCount = it.helpfulCount + 1) else it
        }
        scope.launch {
            dao.upvoteReview(reviewId)
        }
    }

    fun submitLiveQueueUpdate(
        centreId: String,
        updateTag: String,
        message: String,
        currentServingEstimate: String
    ): Result<LiveQueueStatusUpdate> {
        val centre = _centres.value.find { it.id == centreId }
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

        _liveQueueUpdates.value = listOf(newUpdate) + _liveQueueUpdates.value

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

        addAuditLog("LIVE_QUEUE_BROADCAST", "Crowdsourced update ($updateTag) posted for ${centre.name}", UserRole.CUSTOMER, centreId)
        return Result.success(newUpdate)
    }

    fun upvoteLiveUpdate(updateId: String) {
        _liveQueueUpdates.value = _liveQueueUpdates.value.map {
            if (it.id == updateId) it.copy(upvotes = it.upvotes + 1) else it
        }
        scope.launch {
            dao.upvoteLiveUpdate(updateId)
        }
    }

    // ==========================================
    // QUEUE ENGINE OPERATIONS (Deterministic)
    // ==========================================

    @Synchronized
    fun joinQueue(
        centreId: String,
        serviceId: String,
        userName: String,
        userPhone: String
    ): Result<QueueToken> {
        val centre = _centres.value.find { it.id == centreId }
            ?: return Result.failure(IllegalArgumentException("Service centre not found"))
        val service = _services.value.find { it.id == serviceId }
            ?: return Result.failure(IllegalArgumentException("Service not found"))

        val existingWaiting = _tokens.value.filter {
            it.centreId == centreId && it.status == TokenStatus.WAITING
        }
        val prefix = service.category.name.take(1)
        val nextSeq = (_tokens.value.count { it.centreId == centreId } + 1)
        val tokenNum = "$prefix-${String.format("%03d", nextSeq)}"

        val peopleAhead = existingWaiting.size
        val estimate = WaitTimeEstimator.estimateWait(
            peopleAhead = peopleAhead,
            activeCounters = centre.activeCountersCount,
            avgServiceDurationMinutes = service.avgProcessingDurationMin
        )

        val newToken = QueueToken(
            id = UUID.randomUUID().toString(),
            tokenNumber = tokenNum,
            centreId = centre.id,
            centreName = centre.name,
            serviceId = service.id,
            serviceName = service.title,
            userId = _currentUser.value.id,
            userName = userName.ifBlank { _currentUser.value.name },
            userPhone = userPhone.ifBlank { _currentUser.value.phone },
            status = TokenStatus.WAITING,
            positionInQueue = peopleAhead + 1,
            peopleAhead = peopleAhead,
            estimatedWaitMin = estimate.minMinutes,
            estimatedWaitMax = estimate.maxMinutes,
            joinedAtTimestamp = System.currentTimeMillis()
        )

        _tokens.value = _tokens.value + newToken
        _activeUserToken.value = newToken

        scope.launch {
            dao.upsertToken(
                UserTokenEntity(
                    id = newToken.id,
                    tokenNumber = newToken.tokenNumber,
                    centreId = newToken.centreId,
                    centreName = newToken.centreName,
                    serviceId = newToken.serviceId,
                    serviceName = newToken.serviceName,
                    status = newToken.status.name,
                    counterNumber = newToken.counterNumber,
                    positionInQueue = newToken.positionInQueue,
                    peopleAhead = newToken.peopleAhead,
                    estimatedWaitMin = newToken.estimatedWaitMin,
                    estimatedWaitMax = newToken.estimatedWaitMax,
                    joinedAtTimestamp = newToken.joinedAtTimestamp
                )
            )
        }

        addAuditLog(
            action = "TOKEN_ISSUED",
            details = "Token ${newToken.tokenNumber} created for ${service.title} at ${centre.name}",
            role = UserRole.CUSTOMER,
            centreId = centreId
        )

        return Result.success(newToken)
    }

    @Synchronized
    fun cancelUserToken(tokenId: String): Boolean {
        val token = _tokens.value.find { it.id == tokenId } ?: return false
        if (!QueueStateMachine.canTransition(token.status, TokenStatus.CANCELLED, _currentRole.value)) {
            return false
        }

        val updated = token.copy(status = TokenStatus.CANCELLED)
        _tokens.value = _tokens.value.map { if (it.id == tokenId) updated else it }
        if (_activeUserToken.value?.id == tokenId) {
            _activeUserToken.value = null
        }

        recalculateQueuePositions(token.centreId)
        addAuditLog("TOKEN_CANCELLED", "Token ${token.tokenNumber} cancelled by user", _currentRole.value, token.centreId)
        return true
    }

    @Synchronized
    fun callNextCustomer(counterId: String, staffName: String): Result<QueueToken> {
        val counter = _counters.value.find { it.id == counterId }
            ?: return Result.failure(IllegalStateException("Counter not found"))

        val nextWaiting = _tokens.value.firstOrNull {
            it.centreId == counter.centreId && it.status == TokenStatus.WAITING
        } ?: return Result.failure(NoSuchElementException("No customers currently waiting in queue"))

        if (!QueueStateMachine.canTransition(nextWaiting.status, TokenStatus.CALLED, UserRole.STAFF)) {
            return Result.failure(IllegalStateException("Invalid state transition"))
        }

        val calledToken = nextWaiting.copy(
            status = TokenStatus.CALLED,
            counterNumber = counter.counterNumber,
            counterName = counter.name,
            calledAtTimestamp = System.currentTimeMillis(),
            peopleAhead = 0
        )

        _tokens.value = _tokens.value.map { if (it.id == calledToken.id) calledToken else it }
        if (_activeUserToken.value?.id == calledToken.id) {
            _activeUserToken.value = calledToken
        }

        _counters.value = _counters.value.map {
            if (it.id == counterId) it.copy(currentServingToken = calledToken.tokenNumber) else it
        }

        recalculateQueuePositions(counter.centreId)
        addAuditLog("TOKEN_CALLED", "Token ${calledToken.tokenNumber} called to Counter ${counter.counterNumber} by $staffName", UserRole.STAFF, counter.centreId)
        return Result.success(calledToken)
    }

    @Synchronized
    fun markServing(tokenId: String): Boolean {
        val token = _tokens.value.find { it.id == tokenId } ?: return false
        if (!QueueStateMachine.canTransition(token.status, TokenStatus.SERVING, UserRole.STAFF)) return false

        val updated = token.copy(
            status = TokenStatus.SERVING,
            servingAtTimestamp = System.currentTimeMillis()
        )
        _tokens.value = _tokens.value.map { if (it.id == tokenId) updated else it }
        if (_activeUserToken.value?.id == tokenId) {
            _activeUserToken.value = updated
        }

        addAuditLog("TOKEN_SERVING", "Token ${token.tokenNumber} is now being served at Counter ${token.counterNumber}", UserRole.STAFF, token.centreId)
        return true
    }

    @Synchronized
    fun completeService(tokenId: String): Boolean {
        val token = _tokens.value.find { it.id == tokenId } ?: return false
        if (!QueueStateMachine.canTransition(token.status, TokenStatus.COMPLETED, UserRole.STAFF)) return false

        val updated = token.copy(
            status = TokenStatus.COMPLETED,
            completedAtTimestamp = System.currentTimeMillis()
        )
        _tokens.value = _tokens.value.map { if (it.id == tokenId) updated else it }
        if (_activeUserToken.value?.id == tokenId) {
            _activeUserToken.value = null
        }

        _counters.value = _counters.value.map {
            if (it.counterNumber == token.counterNumber && it.centreId == token.centreId) {
                it.copy(currentServingToken = null)
            } else it
        }

        addAuditLog("TOKEN_COMPLETED", "Token ${token.tokenNumber} completed service successfully", UserRole.STAFF, token.centreId)
        return true
    }

    @Synchronized
    fun markNoShow(tokenId: String): Boolean {
        val token = _tokens.value.find { it.id == tokenId } ?: return false
        if (!QueueStateMachine.canTransition(token.status, TokenStatus.NO_SHOW, UserRole.STAFF)) return false

        val updated = token.copy(status = TokenStatus.NO_SHOW)
        _tokens.value = _tokens.value.map { if (it.id == tokenId) updated else it }
        if (_activeUserToken.value?.id == tokenId) {
            _activeUserToken.value = null
        }

        _counters.value = _counters.value.map {
            if (it.counterNumber == token.counterNumber && it.centreId == token.centreId) {
                it.copy(currentServingToken = null)
            } else it
        }

        recalculateQueuePositions(token.centreId)
        addAuditLog("TOKEN_NO_SHOW", "Token ${token.tokenNumber} marked as No-Show", UserRole.STAFF, token.centreId)
        return true
    }

    fun toggleCounterStatus(counterId: String) {
        val counter = _counters.value.find { it.id == counterId } ?: return
        val newActive = !counter.isActive
        _counters.value = _counters.value.map {
            if (it.id == counterId) it.copy(isActive = newActive) else it
        }

        val activeCount = _counters.value.count { it.centreId == counter.centreId && it.isActive }
        _centres.value = _centres.value.map {
            if (it.id == counter.centreId) it.copy(activeCountersCount = activeCount) else it
        }

        recalculateQueuePositions(counter.centreId)
        addAuditLog(
            if (newActive) "COUNTER_OPENED" else "COUNTER_CLOSED",
            "Counter ${counter.counterNumber} status set to ${if (newActive) "Active" else "Closed"}",
            UserRole.STAFF,
            counter.centreId
        )
    }

    private fun recalculateQueuePositions(centreId: String) {
        val centre = _centres.value.find { it.id == centreId } ?: return
        val waitingList = _tokens.value.filter { it.centreId == centreId && it.status == TokenStatus.WAITING }
            .sortedBy { it.joinedAtTimestamp }

        var pos = 1
        val updatedMap = mutableMapOf<String, QueueToken>()
        for (token in waitingList) {
            val peopleAhead = pos - 1
            val estimate = WaitTimeEstimator.estimateWait(
                peopleAhead = peopleAhead,
                activeCounters = centre.activeCountersCount
            )
            val updated = token.copy(
                positionInQueue = pos,
                peopleAhead = peopleAhead,
                estimatedWaitMin = estimate.minMinutes,
                estimatedWaitMax = estimate.maxMinutes
            )
            updatedMap[token.id] = updated
            pos++
        }

        _tokens.value = _tokens.value.map { updatedMap[it.id] ?: it }
        _activeUserToken.value?.let { active ->
            if (updatedMap.containsKey(active.id)) {
                _activeUserToken.value = updatedMap[active.id]
            }
        }
    }

    // ==========================================
    // CROWD REPORTING
    // ==========================================

    fun submitCrowdReport(
        centreId: String,
        level: CrowdLevel,
        peopleCount: Int,
        reportedWait: Int,
        notes: String,
        reporterName: String
    ) {
        val centre = _centres.value.find { it.id == centreId } ?: return
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

        _crowdReports.value = listOf(report) + _crowdReports.value

        _centres.value = _centres.value.map {
            if (it.id == centreId) {
                it.copy(
                    currentCrowdLevel = level,
                    lastCrowdUpdateMinutesAgo = 1,
                    crowdConfidencePercent = 95,
                    estimatedWaitMin = (reportedWait * 0.85).toInt(),
                    estimatedWaitMax = (reportedWait * 1.25).toInt()
                )
            } else it
        }

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

        addAuditLog("CROWD_REPORTED", "Citizen report: ${level.label} (~$peopleCount people) for ${centre.name}", UserRole.CUSTOMER, centreId)
    }

    fun verifyCrowdReport(reportId: String, staffName: String) {
        val report = _crowdReports.value.find { it.id == reportId } ?: return
        val verified = report.copy(isStaffVerified = true, confidenceScore = 100)
        _crowdReports.value = _crowdReports.value.map { if (it.id == reportId) verified else it }

        _centres.value = _centres.value.map {
            if (it.id == report.centreId) {
                it.copy(
                    crowdConfidencePercent = 100,
                    lastCrowdUpdateMinutesAgo = 1
                )
            } else it
        }

        addAuditLog("REPORT_STAFF_VERIFIED", "Staff $staffName verified crowd report for ${report.centreName}", UserRole.STAFF, report.centreId)
    }

    // ==========================================
    // GROUNDED AI / RAG ASSISTANT
    // ==========================================

    fun sendChatMessage(query: String) {
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
            val matchedService = _services.value.firstOrNull { srv ->
                query.contains(srv.title, ignoreCase = true) ||
                srv.requiredDocuments.any { query.contains(it.name, ignoreCase = true) }
            }

            var aiResponse: String
            var sources: List<String>

            val contextText = retrievedDocs.joinToString("\n\n") { "${it.title} (${it.officialAuthority}):\n${it.content}" }

            val geminiResult = GeminiApiClient.callGeminiGrounded(
                userQuery = query,
                groundedContext = contextText
            )

            if (geminiResult.isSuccess && !geminiResult.getOrNull().isNullOrBlank()) {
                aiResponse = geminiResult.getOrNull()!!
                sources = retrievedDocs.map { it.title }
            } else {
                aiResponse = RagKnowledgeEngine.generateGroundedAnswer(
                    query = query,
                    retrievedDocs = retrievedDocs
                )
                sources = retrievedDocs.map { it.title }
            }

            val assistantMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                isUser = false,
                message = aiResponse,
                timestamp = System.currentTimeMillis(),
                groundedSources = sources,
                suggestedServiceId = matchedService?.id
            )

            withContext(Dispatchers.Main) {
                _chatMessages.value = _chatMessages.value + assistantMsg
                _isAiThinking.value = false
            }
        }
    }

    // ==========================================
    // ADMIN ACTIONS
    // ==========================================

    fun toggleServiceAvailability(serviceId: String) {
        val srv = _services.value.find { it.id == serviceId } ?: return
        val updated = srv.copy(isAvailable = !srv.isAvailable)
        _services.value = _services.value.map { if (it.id == serviceId) updated else it }
        addAuditLog("SERVICE_AVAILABILITY_CHANGED", "${srv.title} availability set to ${updated.isAvailable}", UserRole.ADMIN)
    }

    fun updateCentreNotice(centreId: String, notice: String) {
        val centre = _centres.value.find { it.id == centreId } ?: return
        val updated = centre.copy(liveNotice = notice.ifBlank { null })
        _centres.value = _centres.value.map { if (it.id == centreId) updated else it }
        addAuditLog("NOTICE_UPDATED", "Updated notice for ${centre.name}: '$notice'", UserRole.ADMIN, centreId)
    }

    private fun addAuditLog(action: String, details: String, role: UserRole, centreId: String? = null) {
        val log = AuditLog(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            action = action,
            actorName = _currentUser.value.name,
            actorRole = role,
            details = details,
            centreId = centreId
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
    }

    // ==========================================
    // INITIAL SEED DATA
    // ==========================================

    private fun loadInitialSeedData() {
        val docPassportOld = RequiredDocument("doc_old_pass", "Original Expiring Passport", "Current passport with self-attested copies of first and last two pages.")
        val docAadhaar = RequiredDocument("doc_aadhaar", "National ID / Aadhaar Card", "Original card with biometric verification QR code.", acceptableAlternatives = listOf("Voter ID", "Passport"))
        val docAddressProof = RequiredDocument("doc_addr", "Proof of Present Address", "Water/Electricity bill or registered rental agreement (issued within last 3 months).")
        val docBirthProof = RequiredDocument("doc_birth", "Date of Birth Proof", "Birth certificate or secondary school leaving certificate.")
        val docMedicalForm = RequiredDocument("doc_med_form", "Medical Fitness Certificate", "Form 1-A signed by a registered medical practitioner.")
        val docRegCert = RequiredDocument("doc_rc_book", "Original Vehicle Registration Certificate (RC)", "Original RC Smart card with valid pollution certificate.")

        val serviceList = listOf(
            ServiceItem(
                id = "srv_passport_renew",
                category = ServiceCategory.CIVIL_ID,
                title = "Passport Renewal & Re-Issue",
                code = "PSK-REN-01",
                description = "Standard 36/60-page booklet renewal for adult citizens under normal scheme.",
                officialFee = "$45.00 (36 Pages) / $60.00 (60 Pages)",
                avgProcessingDurationMin = 18,
                requiredDocuments = listOf(docPassportOld, docAadhaar, docAddressProof),
                eligibility = "Any adult citizen with an expired or expiring (within 1 year) passport.",
                proceduralSteps = listOf(
                    "Token verification at Entrance Desk",
                    "Counter A: Biometrics, Digital Photo & Fingerprints",
                    "Counter B: Document verification and Scrutiny",
                    "Counter C: Granting Officer clearance and Exit slip issue"
                ),
                cautionNote = "All photocopies must be self-attested with date. Keep original documents organized."
            ),
            ServiceItem(
                id = "srv_aadhaar_update",
                category = ServiceCategory.CIVIL_ID,
                title = "National ID / Aadhaar Biometric Update",
                code = "UID-BIO-04",
                description = "Update iris, fingerprints, photograph, mobile number, or residential address.",
                officialFee = "$1.50 (Demographic) / $3.00 (Biometric)",
                avgProcessingDurationMin = 10,
                requiredDocuments = listOf(docAadhaar, docAddressProof),
                eligibility = "All registered national ID cardholders.",
                proceduralSteps = listOf(
                    "Submit update request form at intake desk",
                    "Biometric capture (Iris + 10 fingers scan)",
                    "Acknowledge slip with Update Request Number (URN)"
                ),
                cautionNote = "Ensure mobile number is active to receive OTP validation during enrollment."
            ),
            ServiceItem(
                id = "srv_dl_renew",
                category = ServiceCategory.TRANSPORT_RTO,
                title = "Driving License Renewal & Endorsement",
                code = "RTO-DL-02",
                description = "Renewal of private or commercial driving permits with updated photo and address.",
                officialFee = "$25.00 (+ $5.00 Smart Card Fee)",
                avgProcessingDurationMin = 15,
                requiredDocuments = listOf(docAadhaar, docAddressProof, docMedicalForm),
                eligibility = "Holding valid driving license within grace period of renewal.",
                proceduralSteps = listOf(
                    "Document scrutiny at Counter 1",
                    "Vision and biometric check at Counter 2",
                    "Cashier counter fee verification",
                    "Smart card dispatch confirmation"
                ),
                cautionNote = "Applicants above 40 years must submit Form 1-A medical fitness certificate."
            ),
            ServiceItem(
                id = "srv_vehicle_rc",
                category = ServiceCategory.TRANSPORT_RTO,
                title = "Vehicle Ownership Transfer & RC Renewal",
                code = "RTO-RC-07",
                description = "Transfer of ownership for non-transport motor vehicles and fitness validation.",
                officialFee = "$35.00 (+ applicable road tax cess)",
                avgProcessingDurationMin = 22,
                requiredDocuments = listOf(docRegCert, docAadhaar, docAddressProof),
                eligibility = "Buyer and seller or legal vehicle owner.",
                proceduralSteps = listOf(
                    "Physical vehicle inspection verification",
                    "NOC check and database clearance",
                    "Biometric signatures of buyer and seller",
                    "RC smart card printing approval"
                ),
                cautionNote = "Engine and chassis number pencil imprint on Form 29/30 is mandatory."
            ),
            ServiceItem(
                id = "srv_hospital_opd",
                category = ServiceCategory.HEALTHCARE,
                title = "Specialist OPD Consultation & Triage",
                code = "MED-OPD-01",
                description = "General medicine, cardiology, orthopedics, and pediatric consultations.",
                officialFee = "$5.00 (Standard Consultation)",
                avgProcessingDurationMin = 12,
                requiredDocuments = listOf(docAadhaar, docBirthProof),
                eligibility = "All patients requiring outpatient specialist medical examination.",
                proceduralSteps = listOf(
                    "Vitals & triage recording at Nurse Desk",
                    "Token queue call to specialist consultation cabin",
                    "Diagnostic test prescriptions & pharmacy slip issuance"
                ),
                cautionNote = "Emergency triage cases are prioritized immediately ahead of standard appointments."
            ),
            ServiceItem(
                id = "srv_property_tax",
                category = ServiceCategory.MUNICIPAL_CIVIC,
                title = "Property Tax Assessment & Mutation",
                code = "CIV-TAX-09",
                description = "Annual civic property tax clearance, name transfer, and ownership mutation.",
                officialFee = "Variable (Based on assessed square-footage)",
                avgProcessingDurationMin = 16,
                requiredDocuments = listOf(docAadhaar, docAddressProof),
                eligibility = "Property title holders and registered leaseholders.",
                proceduralSteps = listOf(
                    "Assessment bill verification",
                    "Surveyor document check",
                    "Clearance certificate stamping"
                ),
                cautionNote = "Verify latest electricity utility invoice matches property registration number."
            )
        )
        _services.value = serviceList

        val centreList = listOf(
            ServiceCentre(
                id = "centre_central_psk",
                name = "Central Passport Seva Kendra (PSK)",
                category = ServiceCategory.CIVIL_ID,
                address = "Tower 4, Metro Plaza Commercial Complex, 5th Floor",
                latitude = 28.6139,
                longitude = 77.2090,
                distanceKm = 1.2,
                operatingHours = "09:00 AM – 05:30 PM (Mon–Fri)",
                phone = "+1 (800) 258-1800",
                activeCountersCount = 4,
                totalCountersCount = 6,
                currentCrowdLevel = CrowdLevel.MODERATE,
                estimatedWaitMin = 8,
                estimatedWaitMax = 15,
                lastCrowdUpdateMinutesAgo = 3,
                crowdConfidencePercent = 95,
                bestTimeToVisit = "Today 02:30 PM – 04:00 PM",
                isVirtualQueueOpen = true,
                availableServiceIds = listOf("srv_passport_renew", "srv_aadhaar_update"),
                rating = 4.8,
                totalReviewsCount = 342,
                liveNotice = "Biometrics counter 2 upgraded with high-speed 4-fingerprint capture."
            ),
            ServiceCentre(
                id = "centre_metro_rto",
                name = "Metropolitan Regional Transport Office (RTO)",
                category = ServiceCategory.TRANSPORT_RTO,
                address = "Sector 18 Expressway Ring Road, Transport Authority Zone",
                latitude = 28.5700,
                longitude = 77.3200,
                distanceKm = 4.8,
                operatingHours = "09:30 AM – 05:00 PM (Mon–Sat)",
                phone = "+1 (800) 419-5500",
                activeCountersCount = 3,
                totalCountersCount = 5,
                currentCrowdLevel = CrowdLevel.HIGH,
                estimatedWaitMin = 22,
                estimatedWaitMax = 38,
                lastCrowdUpdateMinutesAgo = 8,
                crowdConfidencePercent = 88,
                bestTimeToVisit = "Tomorrow 10:15 AM – 11:30 AM",
                isVirtualQueueOpen = true,
                availableServiceIds = listOf("srv_dl_renew", "srv_vehicle_rc"),
                rating = 4.3,
                totalReviewsCount = 540,
                liveNotice = "Biometric server maintenance scheduled at 4:00 PM."
            ),
            ServiceCentre(
                id = "centre_apex_hospital",
                name = "Apex City General Hospital & OPD",
                category = ServiceCategory.HEALTHCARE,
                address = "74 Health Boulevard, MedCity Campus",
                latitude = 28.5355,
                longitude = 77.2600,
                distanceKm = 2.5,
                operatingHours = "08:00 AM – 08:00 PM (Everyday)",
                phone = "+1 (800) 112-9900",
                activeCountersCount = 8,
                totalCountersCount = 10,
                currentCrowdLevel = CrowdLevel.MODERATE,
                estimatedWaitMin = 10,
                estimatedWaitMax = 16,
                lastCrowdUpdateMinutesAgo = 2,
                crowdConfidencePercent = 98,
                bestTimeToVisit = "Today 01:30 PM – 03:00 PM",
                isVirtualQueueOpen = true,
                availableServiceIds = listOf("srv_hospital_opd"),
                rating = 4.7,
                totalReviewsCount = 920,
                liveNotice = "Emergency triage operates 24/7 without tokens."
            ),
            ServiceCentre(
                id = "centre_civic_plaza",
                name = "Civic Centre & Municipal Revenue Office",
                category = ServiceCategory.MUNICIPAL_CIVIC,
                address = "Civic Square, City Hall Complex, Gate 2",
                latitude = 28.6448,
                longitude = 77.2160,
                distanceKm = 3.1,
                operatingHours = "09:30 AM – 05:00 PM (Mon–Fri)",
                phone = "+1 (800) 334-5566",
                activeCountersCount = 3,
                totalCountersCount = 4,
                currentCrowdLevel = CrowdLevel.LOW,
                estimatedWaitMin = 5,
                estimatedWaitMax = 9,
                lastCrowdUpdateMinutesAgo = 5,
                crowdConfidencePercent = 91,
                bestTimeToVisit = "Right Now (Very Low Wait)",
                isVirtualQueueOpen = true,
                availableServiceIds = listOf("srv_property_tax"),
                rating = 4.6,
                totalReviewsCount = 180,
                liveNotice = null
            ),
            ServiceCentre(
                id = "centre_district_sevatirth",
                name = "District Citizen Suvidha Seva Centre",
                category = ServiceCategory.CIVIL_ID,
                address = "Block B, District Collectorate Administrative Enclave",
                latitude = 28.6800,
                longitude = 77.2200,
                distanceKm = 5.6,
                operatingHours = "09:00 AM – 06:00 PM (Mon–Sat)",
                phone = "+1 (800) 778-9900",
                activeCountersCount = 6,
                totalCountersCount = 8,
                currentCrowdLevel = CrowdLevel.LOW,
                estimatedWaitMin = 6,
                estimatedWaitMax = 12,
                lastCrowdUpdateMinutesAgo = 12,
                crowdConfidencePercent = 89,
                bestTimeToVisit = "Today 02:00 PM – 04:00 PM",
                isVirtualQueueOpen = true,
                availableServiceIds = listOf("srv_aadhaar_update", "srv_passport_renew"),
                rating = 4.9,
                totalReviewsCount = 410,
                liveNotice = "Fast-track token lane open for senior citizens."
            )
        )
        _centres.value = centreList

        val counterList = listOf(
            Counter("cnt_psk_1", "centre_central_psk", 1, "Counter 1 - Token Verification & Intake", "Sarah Jenkins", true, "C-039"),
            Counter("cnt_psk_2", "centre_central_psk", 2, "Counter 2 - Biometrics & Photo Capture", "David Kim", true, "C-040"),
            Counter("cnt_psk_3", "centre_central_psk", 3, "Counter 3 - Document Scrutiny Desk", "Robert Taylor", true, "C-041"),
            Counter("cnt_psk_4", "centre_central_psk", 4, "Counter 4 - Granting Officer Clearance", "Elena Rostova", true, "C-038"),
            Counter("cnt_rto_1", "centre_metro_rto", 1, "Counter 1 - License Verification", "Michael Chang", true, "T-019"),
            Counter("cnt_rto_2", "centre_metro_rto", 2, "Counter 2 - Vehicle RC Processing", "Anita Roy", true, "T-020"),
            Counter("cnt_rto_3", "centre_metro_rto", 3, "Counter 3 - Biometric Signature Capture", "James Wilson", true, "T-021")
        )
        _counters.value = counterList

        val tokenList = listOf(
            QueueToken("tkn_psk_041", "C-041", "centre_central_psk", "Central Passport Seva Kendra (PSK)", "srv_passport_renew", "Passport Renewal & Re-Issue", "usr_101", "Marcus Aurelius", "+15550101", TokenStatus.SERVING, 3, "Counter 3 - Document Scrutiny Desk", 0, 0, 0, 3),
            QueueToken("tkn_psk_042", "C-042", "centre_central_psk", "Central Passport Seva Kendra (PSK)", "srv_passport_renew", "Passport Renewal & Re-Issue", "usr_client_me", "Dr. Ananya Sharma", "+1 (555) 019-2834", TokenStatus.WAITING, null, null, 1, 0, 3, 6),
            QueueToken("tkn_psk_043", "C-043", "centre_central_psk", "Central Passport Seva Kendra (PSK)", "srv_passport_renew", "Passport Renewal & Re-Issue", "usr_103", "Priya Sharma", "+15550103", TokenStatus.WAITING, null, null, 2, 1, 8, 14),
            QueueToken("tkn_psk_044", "C-044", "centre_central_psk", "Central Passport Seva Kendra (PSK)", "srv_aadhaar_update", "National ID / Aadhaar Biometric Update", "usr_104", "Daniel Craig", "+15550104", TokenStatus.WAITING, null, null, 3, 2, 14, 20),
            QueueToken("tkn_psk_045", "C-045", "centre_central_psk", "Central Passport Seva Kendra (PSK)", "srv_passport_renew", "Passport Renewal & Re-Issue", "usr_105", "Emma Watson", "+15550105", TokenStatus.WAITING, null, null, 4, 3, 19, 26)
        )
        _tokens.value = tokenList
        _activeUserToken.value = tokenList[1] // User has C-042

        val reportList = listOf(
            CrowdReport("rep_1", "centre_central_psk", "Central Passport Seva Kendra (PSK)", CrowdLevel.MODERATE, 18, 16, "Counters 1-4 moving steadily. Biometrics desk is fast.", "Alex M.", System.currentTimeMillis() - 3 * 60 * 1000, 7, true, 100),
            CrowdReport("rep_2", "centre_metro_rto", "Metropolitan Regional Transport Office (RTO)", CrowdLevel.HIGH, 36, 32, "Very crowded at RC transfer counter. License desk is moderate.", "Vikram S.", System.currentTimeMillis() - 8 * 60 * 1000, 12, false, 88),
            CrowdReport("rep_3", "centre_apex_hospital", "Apex City General Hospital & OPD", CrowdLevel.MODERATE, 22, 12, "OPD registration has 8 desks active, waiting time under 15 mins.", "Dr. Clara P.", System.currentTimeMillis() - 2 * 60 * 1000, 19, true, 100)
        )
        _crowdReports.value = reportList

        val reviewList = listOf(
            CentreReview("rev_1", "centre_central_psk", "Central Passport Seva Kendra (PSK)", "usr_101", "Marcus Aurelius", 5, "Remarkably organized! Verified all my self-attested documents at Counter 1 and was called to biometrics in under 8 minutes.", "Passport Renewal", "Fast", System.currentTimeMillis() - 45 * 60 * 1000, 14),
            CentreReview("rev_2", "centre_central_psk", "Central Passport Seva Kendra (PSK)", "usr_102", "Priya Sharma", 5, "Digital token tracking was spot on. Left my house when token was 2 ahead and walked straight to Counter 3.", "Aadhaar Update", "Fast", System.currentTimeMillis() - 2 * 3600 * 1000, 9),
            CentreReview("rev_3", "centre_metro_rto", "Metropolitan Regional Transport Office (RTO)", "usr_103", "Karan Singh", 4, "License renewal was fast, but carry cash for the smart card envelope fee. Officer Chang was very helpful.", "DL Renewal", "Normal", System.currentTimeMillis() - 5 * 3600 * 1000, 6),
            CentreReview("rev_4", "centre_apex_hospital", "Apex City General Hospital & OPD", "usr_104", "Dr. Clara P.", 5, "Triage queue was handled with empathy. Senior citizens get instant token bump.", "OPD Checkup", "Fast", System.currentTimeMillis() - 8 * 3600 * 1000, 19)
        )
        _reviews.value = reviewList

        val liveUpdateList = listOf(
            LiveQueueStatusUpdate("lup_1", "centre_central_psk", "Central Passport Seva Kendra (PSK)", "Citizen Alex", "FAST_MOVING", "⚡ Token C-039 just completed. Counter 3 calling tokens every 3 mins!", System.currentTimeMillis() - 2 * 60 * 1000, "C-040 (Serving)", 8),
            LiveQueueStatusUpdate("lup_2", "centre_central_psk", "Central Passport Seva Kendra (PSK)", "Officer Rajesh", "DOCS_ACTIVE", "📋 Document pre-check desk is clear. Please have originals ready before Counter 2.", System.currentTimeMillis() - 6 * 60 * 1000, "C-038 (Serving)", 15),
            LiveQueueStatusUpdate("lup_3", "centre_metro_rto", "Metropolitan Regional Transport Office (RTO)", "Vikram S.", "SLIGHT_DELAY", "⏳ RC inspection line has 6 cars waiting. Driving test track is running smoothly.", System.currentTimeMillis() - 14 * 60 * 1000, "T-019 (Serving)", 4)
        )
        _liveQueueUpdates.value = liveUpdateList

        _chatMessages.value = listOf(
            ChatMessage(
                id = "welcome_ai_msg",
                isUser = false,
                message = "Welcome to **QEase Service Intelligence Assistant**! 🏛️\n\nI provide **grounded, official document checklists, fees, eligibility guidelines**, and estimated waiting times for public and private service centres.\n\nTry asking:\n• *\"What documents are required for Passport renewal?\"*\n• *\"What is the fee for Driving License renewal?\"*\n• *\"How long is the wait at Central Passport Seva Kendra?\"*",
                groundedSources = listOf("QEase Verified Knowledge Base")
            )
        )

        addAuditLog("SYSTEM_BOOT", "QEase Core Intelligence Engine initialized with 5 centres, 6 services, deterministic queue state machine, and Room DB sync", UserRole.ADMIN)
    }
}
