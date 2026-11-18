package com.example.data.api

import android.content.Context
import com.example.data.local.*
import com.example.data.model.*
import com.example.domain.engine.CrowdConfidenceEngine
import com.example.domain.engine.QueueStateMachine
import com.example.domain.engine.WaitTimeEstimator
import com.example.data.remote.FirebaseCloudSync
import com.example.widget.AdminWidgetSnapshot
import com.example.widget.StaffWidgetSnapshot
import com.example.widget.WidgetStateBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Clean contract for all Citizen, Staff, Admin, and Home-Screen Widget operations.
 * Validated, deterministic, and synchronized across Room SQLite persistence and App Widgets.
 */
interface QEaseApiService {
    val services: StateFlow<List<ServiceItem>>
    val centres: StateFlow<List<ServiceCentre>>
    val counters: StateFlow<List<Counter>>
    val tokens: StateFlow<List<QueueToken>>
    val staffAccounts: StateFlow<List<UserAccount>>
    val proposals: StateFlow<List<ServiceChangeProposal>>
    val notifications: StateFlow<List<StaffNotification>>
    val systemSettings: StateFlow<SystemSettings>
    val crowdReports: StateFlow<List<CrowdReport>>
    val reviews: StateFlow<List<CentreReview>>
    val liveQueueUpdates: StateFlow<List<LiveQueueStatusUpdate>>
    val auditLogs: StateFlow<List<AuditLog>>

    // Staff Queue & Counter APIs
    fun callNextCustomer(counterId: String, staffName: String): Result<QueueToken>
    fun markServing(tokenId: String, actorName: String = "Staff Officer"): Result<QueueToken>
    fun completeService(tokenId: String, actorName: String = "Staff Officer"): Result<QueueToken>
    fun markNoShow(tokenId: String, actorName: String = "Staff Officer"): Result<QueueToken>
    fun requeueToken(tokenId: String, reason: String, actorName: String = "Staff Officer"): Result<QueueToken>
    fun issueWalkInToken(
        centreId: String,
        serviceId: String,
        citizenName: String,
        citizenPhone: String,
        staffName: String
    ): Result<QueueToken>
    fun toggleCounterStatus(counterId: String, actorName: String = "Staff Officer"): Result<Counter>

    // Staff Service Info & Document Proposal APIs (Held for Admin Approval)
    fun submitServiceInfoProposal(
        serviceId: String,
        proposedFee: String,
        proposedDurationMin: Int,
        proposedDescription: String,
        proposedEligibility: String,
        proposedCautionNote: String,
        rationale: String,
        staff: UserAccount,
        assignedCounterNumber: Int
    ): Result<ServiceChangeProposal>

    fun submitDocumentProposal(
        proposalType: ProposalType,
        serviceId: String,
        documentId: String,
        proposedName: String,
        proposedDescription: String,
        proposedMandatory: Boolean,
        proposedAlternatives: List<String>,
        rationale: String,
        staff: UserAccount,
        assignedCounterNumber: Int
    ): Result<ServiceChangeProposal>

    // Notifications API
    fun markNotificationRead(notificationId: String)
    fun markAllNotificationsRead(role: UserRole, centreId: String? = null)

    // Admin Governance APIs
    fun approveProposal(proposalId: String, adminName: String, reviewNote: String): Result<ServiceChangeProposal>
    fun rejectProposal(proposalId: String, adminName: String, reviewNote: String): Result<ServiceChangeProposal>
    fun createOrUpdateStaffAccount(
        existingId: String?,
        name: String,
        email: String,
        phone: String,
        assignedCentreId: String,
        assignedCounterId: String,
        department: String,
        pin: String,
        isActive: Boolean,
        adminName: String
    ): Result<UserAccount>
    fun toggleStaffAccountActive(staffId: String, adminName: String): Result<UserAccount>
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
        liveNotice: String?,
        adminName: String
    ): Result<ServiceCentre>
    fun toggleCentreVirtualQueue(centreId: String, adminName: String): Result<ServiceCentre>
    fun updateSystemSettings(settings: SystemSettings, adminName: String): Result<SystemSettings>
    fun computeQueueStatistics(centreIdFilter: String? = null): QueueStatisticsSnapshot
}

class LocalQEaseApiEngine private constructor(private val context: Context) : QEaseApiService {

    private val appContext = context.applicationContext
    private val db = QEaseDatabase.getDatabase(appContext)
    val dao: QEaseDao = db.qeaseDao()
    val cloudSync: FirebaseCloudSync? = FirebaseCloudSync.createIfAvailable(appContext)
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val stateEpoch = java.util.concurrent.atomic.AtomicLong(0L)
    private val mutatedProposalIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private val mutatedNotificationIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private val mutatedStaffIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    @Volatile private var settingsMutatedInSession = false

    private val _services = MutableStateFlow<List<ServiceItem>>(emptyList())
    override val services: StateFlow<List<ServiceItem>> = _services.asStateFlow()

    private val _centres = MutableStateFlow<List<ServiceCentre>>(emptyList())
    override val centres: StateFlow<List<ServiceCentre>> = _centres.asStateFlow()

    private val _counters = MutableStateFlow<List<Counter>>(emptyList())
    override val counters: StateFlow<List<Counter>> = _counters.asStateFlow()

    private val _tokens = MutableStateFlow<List<QueueToken>>(emptyList())
    override val tokens: StateFlow<List<QueueToken>> = _tokens.asStateFlow()

    private val _staffAccounts = MutableStateFlow<List<UserAccount>>(emptyList())
    override val staffAccounts: StateFlow<List<UserAccount>> = _staffAccounts.asStateFlow()

    private val _proposals = MutableStateFlow<List<ServiceChangeProposal>>(emptyList())
    override val proposals: StateFlow<List<ServiceChangeProposal>> = _proposals.asStateFlow()

    private val _notifications = MutableStateFlow<List<StaffNotification>>(emptyList())
    override val notifications: StateFlow<List<StaffNotification>> = _notifications.asStateFlow()

    private val _systemSettings = MutableStateFlow(SystemSettings())
    override val systemSettings: StateFlow<SystemSettings> = _systemSettings.asStateFlow()

    private val _crowdReports = MutableStateFlow<List<CrowdReport>>(emptyList())
    override val crowdReports: StateFlow<List<CrowdReport>> = _crowdReports.asStateFlow()

    private val _reviews = MutableStateFlow<List<CentreReview>>(emptyList())
    override val reviews: StateFlow<List<CentreReview>> = _reviews.asStateFlow()

    private val _liveQueueUpdates = MutableStateFlow<List<LiveQueueStatusUpdate>>(emptyList())
    override val liveQueueUpdates: StateFlow<List<LiveQueueStatusUpdate>> = _liveQueueUpdates.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<AuditLog>>(emptyList())
    override val auditLogs: StateFlow<List<AuditLog>> = _auditLogs.asStateFlow()

    private val cloudCallbacks = object : FirebaseCloudSync.SyncCallbacks {
        override fun onServicesUpdated(items: List<ServiceItem>) {
            _services.value = items
        }

        override fun onCentresUpdated(items: List<ServiceCentre>) {
            _centres.value = items
            syncWidgetsFromCurrentState()
        }

        override fun onCountersUpdated(items: List<Counter>) {
            _counters.value = items
            syncWidgetsFromCurrentState()
        }

        override fun onTokensUpdated(items: List<QueueToken>, isFullCollection: Boolean) {
            val merged = _tokens.value.associateBy { it.id }.toMutableMap()
            items.forEach { merged[it.id] = it }
            _tokens.value = merged.values.sortedBy { it.joinedAtTimestamp }
            syncWidgetsFromCurrentState()
        }

        override fun onStaffUpdated(items: List<UserAccount>) {
            val merged = _staffAccounts.value.associateBy { it.id }.toMutableMap()
            items.forEach { merged[it.id] = it }
            _staffAccounts.value = merged.values.toList()
        }

        override fun onProposalsUpdated(items: List<ServiceChangeProposal>) {
            val merged = _proposals.value.associateBy { it.id }.toMutableMap()
            items.forEach { merged[it.id] = it }
            _proposals.value = merged.values.sortedByDescending { it.submittedAt }
            syncWidgetsFromCurrentState()
        }

        override fun onNotificationsUpdated(items: List<StaffNotification>) {
            val merged = _notifications.value.associateBy { it.id }.toMutableMap()
            items.forEach { merged[it.id] = it }
            _notifications.value = merged.values.sortedByDescending { it.timestamp }
            syncWidgetsFromCurrentState()
        }

        override fun onSettingsUpdated(settings: SystemSettings) {
            _systemSettings.value = settings
            syncWidgetsFromCurrentState()
        }

        override fun onCrowdReportsUpdated(items: List<CrowdReport>) {
            val merged = _crowdReports.value.associateBy { it.id }.toMutableMap()
            items.forEach { merged[it.id] = it }
            _crowdReports.value = merged.values.sortedByDescending { it.timestamp }
        }

        override fun onReviewsUpdated(items: List<CentreReview>) {
            val merged = _reviews.value.associateBy { it.id }.toMutableMap()
            items.forEach { merged[it.id] = it }
            _reviews.value = merged.values.sortedByDescending { it.timestamp }
        }

        override fun onLiveUpdatesUpdated(items: List<LiveQueueStatusUpdate>) {
            val merged = _liveQueueUpdates.value.associateBy { it.id }.toMutableMap()
            items.forEach { merged[it.id] = it }
            _liveQueueUpdates.value = merged.values.sortedByDescending { it.timestamp }
        }

        override fun onAuditLogsUpdated(items: List<AuditLog>) {
            val merged = _auditLogs.value.associateBy { it.id }.toMutableMap()
            items.forEach { merged[it.id] = it }
            _auditLogs.value = merged.values.sortedByDescending { it.timestamp }
        }

        override fun seedSnapshotProvider() = FirebaseCloudSync.SeedSnapshot(
            services = _services.value,
            centres = _centres.value,
            counters = _counters.value,
            staffAccounts = _staffAccounts.value,
            proposals = _proposals.value,
            notifications = _notifications.value,
            settings = _systemSettings.value,
            crowdReports = _crowdReports.value,
            reviews = _reviews.value,
            liveUpdates = _liveQueueUpdates.value
        )
    }

    init {
        seedDefaults()
        restorePersistedOperationalState()
        syncWidgetsFromCurrentState()
    }

    fun startCloudSessionSync(role: UserRole) {
        cloudSync?.startSyncForSession(role, cloudCallbacks)
    }

    fun stopCloudSessionSync() {
        cloudSync?.stopSync()
    }

    fun resetToSeedDefaultsForTesting() {
        stateEpoch.incrementAndGet()
        mutatedProposalIds.clear()
        mutatedNotificationIds.clear()
        mutatedStaffIds.clear()
        settingsMutatedInSession = false
        seedDefaults()
        syncWidgetsFromCurrentState("centre_central_psk", "cnt_psk_1", "Officer Rajesh Kumar")
    }

    private fun restorePersistedOperationalState() {
        val capturedEpoch = stateEpoch.get()
        ioScope.launch {
            runCatching {
                if (stateEpoch.get() != capturedEpoch) return@launch
                val savedSettings = dao.observeGlobalSettings().firstOrNull()
                if (stateEpoch.get() != capturedEpoch) return@launch
                if (savedSettings != null && !settingsMutatedInSession) {
                    _systemSettings.value = SystemSettings(
                        id = savedSettings.id,
                        autoNoShowTimeoutMinutes = savedSettings.autoNoShowTimeoutMinutes,
                        maxActiveTokensPerCitizen = savedSettings.maxActiveTokensPerCitizen,
                        crowdDecayHalfLifeMinutes = savedSettings.crowdDecayHalfLifeMinutes,
                        walkInBufferPerHour = savedSettings.walkInBufferPerHour,
                        maintenanceMode = savedSettings.maintenanceMode,
                        maintenanceBanner = savedSettings.maintenanceBanner,
                        updatedAt = savedSettings.updatedAt,
                        updatedByAdmin = savedSettings.updatedByAdmin
                    )
                }

                val savedProposals = dao.getAllProposals().firstOrNull().orEmpty()
                if (stateEpoch.get() != capturedEpoch) return@launch
                if (savedProposals.isNotEmpty()) {
                    val savedMap = savedProposals.map { it.toDomain() }.associateBy { it.id }.toMutableMap()
                    _proposals.value.filter { it.id in mutatedProposalIds || !savedMap.containsKey(it.id) }
                        .forEach { savedMap[it.id] = it }
                    _proposals.value = savedMap.values.sortedByDescending { it.submittedAt }
                } else {
                    _proposals.value.forEach { dao.upsertProposal(it.toEntity()) }
                }

                val savedNotifications = dao.getAllNotifications().firstOrNull().orEmpty()
                if (stateEpoch.get() != capturedEpoch) return@launch
                if (savedNotifications.isNotEmpty()) {
                    val savedMap = savedNotifications.map { it.toDomain() }.associateBy { it.id }.toMutableMap()
                    _notifications.value.filter { it.id in mutatedNotificationIds || !savedMap.containsKey(it.id) }
                        .forEach { savedMap[it.id] = it }
                    _notifications.value = savedMap.values.sortedByDescending { it.timestamp }
                } else {
                    _notifications.value.forEach { dao.upsertNotification(it.toEntity()) }
                }

                val savedStaff = dao.getAllAccounts().firstOrNull().orEmpty()
                    .filter { it.role == UserRole.STAFF.name }
                if (stateEpoch.get() != capturedEpoch) return@launch
                if (savedStaff.isNotEmpty()) {
                    val savedMap = savedStaff.map {
                        UserAccount(
                            id = it.id,
                            name = it.name,
                            email = it.email,
                            phone = it.phone,
                            role = UserRole.STAFF,
                            assignedCentreId = it.assignedCentreId,
                            assignedCounterId = it.assignedCounterId,
                            pin = it.pin,
                            department = it.department,
                            isLoggedIn = false,
                            isActive = it.isActive
                        )
                    }.associateBy { it.id }.toMutableMap()
                    _staffAccounts.value.filter { it.id in mutatedStaffIds || !savedMap.containsKey(it.id) }
                        .forEach { savedMap[it.id] = it }
                    _staffAccounts.value = savedMap.values.toList()
                } else {
                    _staffAccounts.value.forEach { dao.insertAccount(it.toEntity()) }
                }

                if (stateEpoch.get() == capturedEpoch) {
                    syncWidgetsFromCurrentState()
                }
            }
        }
    }

    // ==========================================
    // INTERNAL STATE HELPERS & WIDGET SYNC
    // ==========================================

    fun updateReviews(newList: List<CentreReview>) {
        _reviews.value = newList
    }

    fun updateLiveQueueUpdates(newList: List<LiveQueueStatusUpdate>) {
        _liveQueueUpdates.value = newList
    }

    fun updateCrowdReports(newList: List<CrowdReport>) {
        _crowdReports.value = newList
    }

    fun updateCentres(newList: List<ServiceCentre>) {
        _centres.value = newList
        syncWidgetsFromCurrentState()
    }

    fun updateServices(newList: List<ServiceItem>) {
        _services.value = newList
    }

    fun updateTokens(newList: List<QueueToken>) {
        _tokens.value = newList
        syncWidgetsFromCurrentState()
    }

    fun appendAuditLog(action: String, details: String, role: UserRole, actorName: String, centreId: String? = null) {
        val log = AuditLog(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            action = action,
            actorName = actorName,
            actorRole = role,
            details = details,
            centreId = centreId
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
        cloudSync?.pushAuditLog(log)
        ioScope.launch {
            runCatching {
                dao.insertAuditLog(
                    AuditLogEntity(
                        id = log.id,
                        timestamp = log.timestamp,
                        action = log.action,
                        actorName = log.actorName,
                        actorRole = log.actorRole.name,
                        details = log.details,
                        centreId = log.centreId
                    )
                )
            }
        }
    }

    private fun pushNotification(
        title: String,
        message: String,
        targetRole: UserRole,
        centreId: String? = null,
        counterId: String? = null,
        severity: NotificationSeverity = NotificationSeverity.INFO,
        relatedTokenNumber: String? = null
    ) {
        val notif = StaffNotification(
            id = "notif_${UUID.randomUUID().toString().take(8)}",
            title = title,
            message = message,
            targetRole = targetRole,
            centreId = centreId,
            counterId = counterId,
            severity = severity,
            isRead = false,
            timestamp = System.currentTimeMillis(),
            relatedTokenNumber = relatedTokenNumber
        )
        mutatedNotificationIds.add(notif.id)
        _notifications.value = listOf(notif) + _notifications.value
        cloudSync?.pushNotificationCreated(notif)
        ioScope.launch { runCatching { dao.upsertNotification(notif.toEntity()) } }
    }

    fun syncWidgetsFromCurrentState(
        staffCentreIdOverride: String? = null,
        staffCounterIdOverride: String? = null,
        staffNameOverride: String? = null
    ) {
        val existingStaffSnap = WidgetStateBridge.readStaffSnapshot(appContext)
        val targetCounterId = staffCounterIdOverride ?: existingStaffSnap.counterId
        val counter = _counters.value.find { it.id == targetCounterId } ?: _counters.value.firstOrNull()
        val centreId = staffCentreIdOverride ?: counter?.centreId ?: existingStaffSnap.centreId
        val centre = _centres.value.find { it.id == centreId } ?: _centres.value.firstOrNull()

        val activeDeskToken = _tokens.value.firstOrNull {
            it.centreId == centreId &&
                it.counterNumber == counter?.counterNumber &&
                (it.status == TokenStatus.CALLED || it.status == TokenStatus.SERVING)
        }
        val waitingAtCentre = _tokens.value.count {
            it.centreId == centreId && it.status == TokenStatus.WAITING
        }
        val unreadStaffAlerts = _notifications.value.count {
            it.targetRole == UserRole.STAFF && !it.isRead && (it.centreId == null || it.centreId == centreId)
        }

        val staffSnapshot = StaffWidgetSnapshot(
            centreId = centre?.id ?: centreId,
            centreName = centre?.name ?: "Central Passport Seva Kendra (PSK)",
            counterId = counter?.id ?: targetCounterId,
            counterNumber = counter?.counterNumber ?: 1,
            counterName = counter?.name ?: "Counter 1",
            assignedStaffName = staffNameOverride ?: counter?.assignedStaffName ?: existingStaffSnap.assignedStaffName,
            isCounterActive = counter?.isActive ?: true,
            currentTokenId = activeDeskToken?.id ?: "",
            currentTokenNumber = activeDeskToken?.tokenNumber ?: "",
            currentTokenCitizen = activeDeskToken?.userName ?: "",
            currentTokenService = activeDeskToken?.serviceName ?: "",
            currentTokenStatus = activeDeskToken?.status?.name ?: "",
            waitingCount = waitingAtCentre,
            unreadNotificationsCount = unreadStaffAlerts
        )
        WidgetStateBridge.saveStaffSnapshot(appContext, staffSnapshot)

        val settings = _systemSettings.value
        val adminSnapshot = AdminWidgetSnapshot(
            totalWaitingCount = _tokens.value.count { it.status == TokenStatus.WAITING },
            activeCountersCount = _counters.value.count { it.isActive },
            totalCountersCount = _counters.value.size,
            pendingProposalsCount = _proposals.value.count { it.status == ProposalStatus.PENDING },
            openCentresCount = _centres.value.count { it.isVirtualQueueOpen },
            totalCentresCount = _centres.value.size,
            maintenanceMode = settings.maintenanceMode
        )
        WidgetStateBridge.saveAdminSnapshot(appContext, adminSnapshot)
        WidgetStateBridge.notifyAllWidgets(appContext)
    }

    // ==========================================
    // CITIZEN TOKEN CREATION & RECALCULATION
    // ==========================================

    @Synchronized
    fun joinQueue(
        centreId: String,
        serviceId: String,
        userId: String,
        userName: String,
        userPhone: String,
        actorRole: UserRole = UserRole.CUSTOMER
    ): Result<QueueToken> {
        val settings = _systemSettings.value
        if (settings.maintenanceMode && actorRole == UserRole.CUSTOMER) {
            return Result.failure(IllegalStateException("System Maintenance active: ${settings.maintenanceBanner}"))
        }
        val centre = _centres.value.find { it.id == centreId }
            ?: return Result.failure(IllegalArgumentException("Service centre not found"))
        if (!centre.isVirtualQueueOpen && actorRole == UserRole.CUSTOMER) {
            return Result.failure(IllegalStateException("Virtual queue is currently closed at ${centre.name}"))
        }
        val service = _services.value.find { it.id == serviceId }
            ?: return Result.failure(IllegalArgumentException("Service not found"))
        if (!service.isAvailable) {
            return Result.failure(IllegalStateException("${service.title} is temporarily suspended by Admin"))
        }

        if (actorRole == UserRole.CUSTOMER) {
            val activeForUser = _tokens.value.count {
                it.userId == userId &&
                    (it.status == TokenStatus.WAITING || it.status == TokenStatus.CALLED || it.status == TokenStatus.SERVING)
            }
            if (activeForUser >= settings.maxActiveTokensPerCitizen) {
                return Result.failure(
                    IllegalStateException("Limit reached: max ${settings.maxActiveTokensPerCitizen} active tokens allowed per citizen.")
                )
            }
        }

        val existingWaiting = _tokens.value.filter {
            it.centreId == centreId && it.status == TokenStatus.WAITING
        }
        val prefix = service.category.name.take(1)
        val nextSeq = (_tokens.value.count { it.centreId == centreId } + 1)
        val tokenNum = "$prefix-${String.format("%03d", nextSeq)}"

        val peopleAhead = existingWaiting.size
        val estimate = WaitTimeEstimator.estimateWait(
            peopleAhead = peopleAhead,
            activeCounters = centre.activeCountersCount.coerceAtLeast(1),
            avgServiceDurationMinutes = service.avgProcessingDurationMin
        )

        val newToken = QueueToken(
            id = UUID.randomUUID().toString(),
            tokenNumber = tokenNum,
            centreId = centre.id,
            centreName = centre.name,
            serviceId = service.id,
            serviceName = service.title,
            userId = userId,
            userName = userName,
            userPhone = userPhone,
            status = TokenStatus.WAITING,
            positionInQueue = peopleAhead + 1,
            peopleAhead = peopleAhead,
            estimatedWaitMin = estimate.minMinutes,
            estimatedWaitMax = estimate.maxMinutes,
            joinedAtTimestamp = System.currentTimeMillis()
        )

        _tokens.value = _tokens.value + newToken
        cloudSync?.pushTokenCreated(newToken)

        ioScope.launch {
            runCatching {
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
        }

        pushNotification(
            title = "New Token ${newToken.tokenNumber} Joined",
            message = "${newToken.userName} joined ${service.title} queue at ${centre.name}.",
            targetRole = UserRole.STAFF,
            centreId = centre.id,
            severity = NotificationSeverity.INFO,
            relatedTokenNumber = newToken.tokenNumber
        )

        appendAuditLog(
            action = if (actorRole == UserRole.STAFF) "WALKIN_TOKEN_ISSUED" else "TOKEN_ISSUED",
            details = "Token ${newToken.tokenNumber} created for ${service.title} at ${centre.name}",
            role = actorRole,
            actorName = userName,
            centreId = centreId
        )
        syncWidgetsFromCurrentState()
        return Result.success(newToken)
    }

    @Synchronized
    fun recalculateQueuePositions(centreId: String): List<QueueToken> {
        val centre = _centres.value.find { it.id == centreId } ?: return emptyList()
        val waitingList = _tokens.value.filter { it.centreId == centreId && it.status == TokenStatus.WAITING }
            .sortedBy { it.joinedAtTimestamp }

        var pos = 1
        val updatedMap = mutableMapOf<String, QueueToken>()
        for (token in waitingList) {
            val peopleAhead = pos - 1
            val estimate = WaitTimeEstimator.estimateWait(
                peopleAhead = peopleAhead,
                activeCounters = centre.activeCountersCount.coerceAtLeast(1)
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
        return updatedMap.values.toList()
    }

    // ==========================================
    // STAFF QUEUE & COUNTER APIS
    // ==========================================

    @Synchronized
    override fun callNextCustomer(counterId: String, staffName: String): Result<QueueToken> {
        val counter = _counters.value.find { it.id == counterId }
            ?: return Result.failure(IllegalStateException("Assigned counter not found"))
        if (!counter.isActive) {
            return Result.failure(IllegalStateException("Counter ${counter.counterNumber} is paused. Activate counter before calling tokens."))
        }

        val alreadyActive = _tokens.value.firstOrNull {
            it.centreId == counter.centreId &&
                it.counterNumber == counter.counterNumber &&
                (it.status == TokenStatus.CALLED || it.status == TokenStatus.SERVING)
        }
        if (alreadyActive != null) {
            return Result.failure(
                IllegalStateException("Counter ${counter.counterNumber} is already handling ${alreadyActive.tokenNumber}.")
            )
        }

        val nextWaiting = _tokens.value
            .filter { it.centreId == counter.centreId && it.status == TokenStatus.WAITING }
            .sortedBy { it.joinedAtTimestamp }
            .firstOrNull { token ->
                counter.handledServiceIds.isEmpty() || counter.handledServiceIds.contains(token.serviceId)
            }
            ?: _tokens.value.filter { it.centreId == counter.centreId && it.status == TokenStatus.WAITING }
                .minByOrNull { it.joinedAtTimestamp }
            ?: return Result.failure(NoSuchElementException("No citizens currently waiting in queue"))

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
        val updatedCounter = counter.copy(currentServingToken = calledToken.tokenNumber)
        _counters.value = _counters.value.map { if (it.id == counterId) updatedCounter else it }

        recalculateQueuePositions(counter.centreId)
        cloudSync?.pushTokenUpdated(calledToken)
        cloudSync?.pushCounterUpdated(updatedCounter)
        appendAuditLog(
            action = "TOKEN_CALLED",
            details = "Token ${calledToken.tokenNumber} called to Counter ${counter.counterNumber} by $staffName",
            role = UserRole.STAFF,
            actorName = staffName,
            centreId = counter.centreId
        )
        syncWidgetsFromCurrentState(counter.centreId, counter.id, staffName)
        return Result.success(calledToken)
    }

    @Synchronized
    override fun markServing(tokenId: String, actorName: String): Result<QueueToken> {
        val token = _tokens.value.find { it.id == tokenId }
            ?: return Result.failure(IllegalArgumentException("Token not found"))
        if (!QueueStateMachine.canTransition(token.status, TokenStatus.SERVING, UserRole.STAFF)) {
            return Result.failure(IllegalStateException("Cannot start serving from ${token.status}"))
        }
        val updated = token.copy(status = TokenStatus.SERVING, servingAtTimestamp = System.currentTimeMillis())
        _tokens.value = _tokens.value.map { if (it.id == tokenId) updated else it }
        cloudSync?.pushTokenUpdated(updated)
        appendAuditLog(
            action = "TOKEN_SERVING",
            details = "Token ${token.tokenNumber} serving at Counter ${token.counterNumber}",
            role = UserRole.STAFF,
            actorName = actorName,
            centreId = token.centreId
        )
        syncWidgetsFromCurrentState()
        return Result.success(updated)
    }

    @Synchronized
    override fun completeService(tokenId: String, actorName: String): Result<QueueToken> {
        val token = _tokens.value.find { it.id == tokenId }
            ?: return Result.failure(IllegalArgumentException("Token not found"))
        if (!QueueStateMachine.canTransition(token.status, TokenStatus.COMPLETED, UserRole.STAFF)) {
            return Result.failure(IllegalStateException("Cannot complete token in ${token.status} state"))
        }
        val updated = token.copy(status = TokenStatus.COMPLETED, completedAtTimestamp = System.currentTimeMillis())
        _tokens.value = _tokens.value.map { if (it.id == tokenId) updated else it }
        val clearedCounter = _counters.value.find { it.counterNumber == token.counterNumber && it.centreId == token.centreId }
            ?.copy(currentServingToken = null)
        _counters.value = _counters.value.map {
            if (it.id == clearedCounter?.id) clearedCounter else it
        }
        cloudSync?.pushTokenUpdated(updated)
        clearedCounter?.let { cloudSync?.pushCounterUpdated(it) }
        appendAuditLog(
            action = "TOKEN_COMPLETED",
            details = "Token ${token.tokenNumber} completed service successfully",
            role = UserRole.STAFF,
            actorName = actorName,
            centreId = token.centreId
        )
        syncWidgetsFromCurrentState()
        return Result.success(updated)
    }

    @Synchronized
    override fun markNoShow(tokenId: String, actorName: String): Result<QueueToken> {
        val token = _tokens.value.find { it.id == tokenId }
            ?: return Result.failure(IllegalArgumentException("Token not found"))
        if (!QueueStateMachine.canTransition(token.status, TokenStatus.NO_SHOW, UserRole.STAFF)) {
            return Result.failure(IllegalStateException("Cannot mark NO_SHOW from ${token.status}"))
        }
        val updated = token.copy(status = TokenStatus.NO_SHOW)
        _tokens.value = _tokens.value.map { if (it.id == tokenId) updated else it }
        val clearedCounter = _counters.value.find { it.counterNumber == token.counterNumber && it.centreId == token.centreId }
            ?.copy(currentServingToken = null)
        _counters.value = _counters.value.map {
            if (it.id == clearedCounter?.id) clearedCounter else it
        }
        recalculateQueuePositions(token.centreId)
        cloudSync?.pushTokenUpdated(updated)
        clearedCounter?.let { cloudSync?.pushCounterUpdated(it) }
        appendAuditLog(
            action = "TOKEN_NO_SHOW",
            details = "Token ${token.tokenNumber} marked as No-Show",
            role = UserRole.STAFF,
            actorName = actorName,
            centreId = token.centreId
        )
        syncWidgetsFromCurrentState()
        return Result.success(updated)
    }

    @Synchronized
    override fun requeueToken(tokenId: String, reason: String, actorName: String): Result<QueueToken> {
        val token = _tokens.value.find { it.id == tokenId }
            ?: return Result.failure(IllegalArgumentException("Token not found"))
        if (token.status != TokenStatus.CALLED && token.status != TokenStatus.SERVING) {
            return Result.failure(IllegalStateException("Only CALLED or SERVING tokens can be re-queued"))
        }
        val updated = token.copy(
            status = TokenStatus.WAITING,
            counterNumber = null,
            counterName = null,
            calledAtTimestamp = null,
            servingAtTimestamp = null,
            joinedAtTimestamp = System.currentTimeMillis()
        )
        _tokens.value = _tokens.value.map { if (it.id == tokenId) updated else it }
        val clearedCounter = _counters.value.find { it.counterNumber == token.counterNumber && it.centreId == token.centreId }
            ?.copy(currentServingToken = null)
        _counters.value = _counters.value.map {
            if (it.id == clearedCounter?.id) clearedCounter else it
        }
        recalculateQueuePositions(token.centreId)
        val refreshed = _tokens.value.find { it.id == tokenId } ?: updated
        cloudSync?.pushTokenUpdated(refreshed)
        clearedCounter?.let { cloudSync?.pushCounterUpdated(it) }
        appendAuditLog(
            action = "TOKEN_REQUEUED",
            details = "Token ${token.tokenNumber} returned to waiting line (${reason.ifBlank { "Citizen requested hold" }})",
            role = UserRole.STAFF,
            actorName = actorName,
            centreId = token.centreId
        )
        syncWidgetsFromCurrentState()
        return Result.success(refreshed)
    }

    override fun issueWalkInToken(
        centreId: String,
        serviceId: String,
        citizenName: String,
        citizenPhone: String,
        staffName: String
    ): Result<QueueToken> {
        val cleanName = citizenName.trim()
        if (cleanName.length < 2) {
            return Result.failure(IllegalArgumentException("Enter citizen's full name (at least 2 characters)"))
        }
        val cleanPhone = citizenPhone.trim().ifBlank { "+91 Desk Walk-In" }
        return joinQueue(
            centreId = centreId,
            serviceId = serviceId,
            userId = "walkin_${UUID.randomUUID().toString().take(6)}",
            userName = "$cleanName (Walk-In)",
            userPhone = cleanPhone,
            actorRole = UserRole.STAFF
        )
    }

    @Synchronized
    override fun toggleCounterStatus(counterId: String, actorName: String): Result<Counter> {
        val counter = _counters.value.find { it.id == counterId }
            ?: return Result.failure(IllegalArgumentException("Counter not found"))
        val newActive = !counter.isActive
        val updatedCounter = counter.copy(isActive = newActive)
        _counters.value = _counters.value.map { if (it.id == counterId) updatedCounter else it }

        val activeCount = _counters.value.count { it.centreId == counter.centreId && it.isActive }
        val updatedCentre = _centres.value.find { it.id == counter.centreId }?.copy(activeCountersCount = activeCount)
        _centres.value = _centres.value.map {
            if (it.id == counter.centreId && updatedCentre != null) updatedCentre else it
        }

        recalculateQueuePositions(counter.centreId)
        cloudSync?.pushCounterUpdated(updatedCounter)
        updatedCentre?.let { cloudSync?.pushCentreUpserted(it, false) }
        appendAuditLog(
            action = if (newActive) "COUNTER_OPENED" else "COUNTER_CLOSED",
            details = "Counter ${counter.counterNumber} set to ${if (newActive) "Active" else "Paused"}",
            role = UserRole.STAFF,
            actorName = actorName,
            centreId = counter.centreId
        )
        syncWidgetsFromCurrentState()
        return Result.success(updatedCounter)
    }

    // ==========================================
    // STAFF CHANGE PROPOSALS (HELD FOR ADMIN)
    // ==========================================

    override fun submitServiceInfoProposal(
        serviceId: String,
        proposedFee: String,
        proposedDurationMin: Int,
        proposedDescription: String,
        proposedEligibility: String,
        proposedCautionNote: String,
        rationale: String,
        staff: UserAccount,
        assignedCounterNumber: Int
    ): Result<ServiceChangeProposal> {
        val service = _services.value.find { it.id == serviceId }
            ?: return Result.failure(IllegalArgumentException("Selected service not found"))
        if (proposedFee.isBlank() || proposedDescription.isBlank()) {
            return Result.failure(IllegalArgumentException("Fee and description cannot be blank"))
        }
        if (proposedDurationMin !in 2..180) {
            return Result.failure(IllegalArgumentException("Duration must be between 2 and 180 minutes"))
        }
        if (rationale.trim().length < 5) {
            return Result.failure(IllegalArgumentException("Provide a brief reason for Admin review (min 5 characters)"))
        }

        val proposal = ServiceChangeProposal(
            id = "prop_${UUID.randomUUID().toString().take(8)}",
            proposalType = ProposalType.SERVICE_INFO,
            serviceId = service.id,
            serviceTitle = service.title,
            centreId = staff.assignedCentreId ?: _centres.value.firstOrNull()?.id.orEmpty(),
            submittedByStaffId = staff.id,
            submittedByStaffName = staff.name,
            assignedCounterNumber = assignedCounterNumber,
            rationale = rationale.trim(),
            status = ProposalStatus.PENDING,
            submittedAt = System.currentTimeMillis(),
            previousFee = service.officialFee,
            proposedFee = proposedFee.trim(),
            previousDurationMin = service.avgProcessingDurationMin,
            proposedDurationMin = proposedDurationMin,
            previousDescription = service.description,
            proposedDescription = proposedDescription.trim(),
            previousEligibility = service.eligibility,
            proposedEligibility = proposedEligibility.trim(),
            previousCautionNote = service.cautionNote,
            proposedCautionNote = proposedCautionNote.trim()
        )

        mutatedProposalIds.add(proposal.id)
        _proposals.value = listOf(proposal) + _proposals.value
        cloudSync?.pushProposalCreated(proposal)
        ioScope.launch { runCatching { dao.upsertProposal(proposal.toEntity()) } }

        pushNotification(
            title = "Pending Review: ${service.title}",
            message = "${staff.name} (Counter $assignedCounterNumber) proposed a service info edit.",
            targetRole = UserRole.ADMIN,
            centreId = proposal.centreId,
            severity = NotificationSeverity.ACTION_REQUIRED
        )
        appendAuditLog(
            action = "PROPOSAL_SUBMITTED",
            details = "Service info proposal submitted for ${service.title} by ${staff.name}",
            role = UserRole.STAFF,
            actorName = staff.name,
            centreId = proposal.centreId
        )
        syncWidgetsFromCurrentState()
        return Result.success(proposal)
    }

    override fun submitDocumentProposal(
        proposalType: ProposalType,
        serviceId: String,
        documentId: String,
        proposedName: String,
        proposedDescription: String,
        proposedMandatory: Boolean,
        proposedAlternatives: List<String>,
        rationale: String,
        staff: UserAccount,
        assignedCounterNumber: Int
    ): Result<ServiceChangeProposal> {
        val service = _services.value.find { it.id == serviceId }
            ?: return Result.failure(IllegalArgumentException("Selected service not found"))
        if (proposalType != ProposalType.REQUIRED_DOCUMENT_REMOVE && proposedName.trim().length < 2) {
            return Result.failure(IllegalArgumentException("Document title is required"))
        }
        if (rationale.trim().length < 5) {
            return Result.failure(IllegalArgumentException("Provide a rationale for Admin approval (min 5 characters)"))
        }

        val existingDoc = service.requiredDocuments.find { it.id == documentId }
        val resolvedDocId = documentId.ifBlank { "doc_${UUID.randomUUID().toString().take(6)}" }

        val proposal = ServiceChangeProposal(
            id = "prop_${UUID.randomUUID().toString().take(8)}",
            proposalType = proposalType,
            serviceId = service.id,
            serviceTitle = service.title,
            centreId = staff.assignedCentreId ?: _centres.value.firstOrNull()?.id.orEmpty(),
            submittedByStaffId = staff.id,
            submittedByStaffName = staff.name,
            assignedCounterNumber = assignedCounterNumber,
            rationale = rationale.trim(),
            status = ProposalStatus.PENDING,
            submittedAt = System.currentTimeMillis(),
            documentId = resolvedDocId,
            previousDocName = existingDoc?.name ?: "",
            proposedDocName = proposedName.trim(),
            previousDocDescription = existingDoc?.description ?: "",
            proposedDocDescription = proposedDescription.trim(),
            previousDocMandatory = existingDoc?.isMandatory ?: true,
            proposedDocMandatory = proposedMandatory,
            previousDocAlternatives = existingDoc?.acceptableAlternatives ?: emptyList(),
            proposedDocAlternatives = proposedAlternatives.map { it.trim() }.filter { it.isNotBlank() }
        )

        mutatedProposalIds.add(proposal.id)
        _proposals.value = listOf(proposal) + _proposals.value
        cloudSync?.pushProposalCreated(proposal)
        ioScope.launch { runCatching { dao.upsertProposal(proposal.toEntity()) } }

        pushNotification(
            title = "Document Proposal: ${service.title}",
            message = "${staff.name} proposed '${proposalType.label}' awaiting approval.",
            targetRole = UserRole.ADMIN,
            centreId = proposal.centreId,
            severity = NotificationSeverity.ACTION_REQUIRED
        )
        appendAuditLog(
            action = "DOC_PROPOSAL_SUBMITTED",
            details = "${proposalType.label} on ${service.title} by ${staff.name}",
            role = UserRole.STAFF,
            actorName = staff.name,
            centreId = proposal.centreId
        )
        syncWidgetsFromCurrentState()
        return Result.success(proposal)
    }

    // ==========================================
    // NOTIFICATIONS API
    // ==========================================

    override fun markNotificationRead(notificationId: String) {
        _notifications.value = _notifications.value.map {
            if (it.id == notificationId) it.copy(isRead = true) else it
        }
        cloudSync?.pushNotificationRead(notificationId)
        ioScope.launch { runCatching { dao.markNotificationRead(notificationId) } }
        syncWidgetsFromCurrentState()
    }

    override fun markAllNotificationsRead(role: UserRole, centreId: String?) {
        val updated = _notifications.value.map { notif ->
            if (notif.targetRole == role && (centreId == null || notif.centreId == null || notif.centreId == centreId)) {
                notif.copy(isRead = true)
            } else notif
        }
        _notifications.value = updated
        ioScope.launch {
            updated.filter { it.isRead }.forEach {
                runCatching { dao.markNotificationRead(it.id) }
                cloudSync?.pushNotificationRead(it.id)
            }
        }
        syncWidgetsFromCurrentState()
    }

    // ==========================================
    // ADMIN REVIEW & PUBLISHING WORKFLOW
    // ==========================================

    @Synchronized
    override fun approveProposal(
        proposalId: String,
        adminName: String,
        reviewNote: String
    ): Result<ServiceChangeProposal> {
        val proposal = _proposals.value.find { it.id == proposalId }
            ?: return Result.failure(IllegalArgumentException("Proposal not found"))
        if (proposal.status != ProposalStatus.PENDING) {
            return Result.failure(IllegalStateException("Proposal already reviewed (${proposal.status.label})"))
        }

        val service = _services.value.find { it.id == proposal.serviceId }
            ?: return Result.failure(IllegalArgumentException("Target service not found"))

        val updatedService = when (proposal.proposalType) {
            ProposalType.SERVICE_INFO -> service.copy(
                officialFee = proposal.proposedFee.ifBlank { service.officialFee },
                avgProcessingDurationMin = if (proposal.proposedDurationMin > 0) proposal.proposedDurationMin else service.avgProcessingDurationMin,
                description = proposal.proposedDescription.ifBlank { service.description },
                eligibility = proposal.proposedEligibility.ifBlank { service.eligibility },
                cautionNote = proposal.proposedCautionNote
            )
            ProposalType.REQUIRED_DOCUMENT_ADD -> {
                val newDoc = RequiredDocument(
                    id = proposal.documentId.ifBlank { "doc_${UUID.randomUUID().toString().take(6)}" },
                    name = proposal.proposedDocName,
                    description = proposal.proposedDocDescription,
                    isMandatory = proposal.proposedDocMandatory,
                    acceptableAlternatives = proposal.proposedDocAlternatives
                )
                service.copy(requiredDocuments = service.requiredDocuments + newDoc)
            }
            ProposalType.REQUIRED_DOCUMENT_UPDATE -> {
                val updatedDocs = service.requiredDocuments.map { doc ->
                    if (doc.id == proposal.documentId) {
                        doc.copy(
                            name = proposal.proposedDocName.ifBlank { doc.name },
                            description = proposal.proposedDocDescription.ifBlank { doc.description },
                            isMandatory = proposal.proposedDocMandatory,
                            acceptableAlternatives = proposal.proposedDocAlternatives
                        )
                    } else doc
                }
                service.copy(requiredDocuments = updatedDocs)
            }
            ProposalType.REQUIRED_DOCUMENT_REMOVE -> {
                service.copy(
                    requiredDocuments = service.requiredDocuments.filterNot { it.id == proposal.documentId }
                )
            }
        }

        _services.value = _services.value.map { if (it.id == service.id) updatedService else it }
        cloudSync?.pushServiceUpdated(updatedService)

        val resolved = proposal.copy(
            status = ProposalStatus.APPROVED,
            reviewedAt = System.currentTimeMillis(),
            reviewedByAdminName = adminName,
            adminReviewNote = reviewNote.trim().ifBlank { "Approved and published to citizen catalog." }
        )
        mutatedProposalIds.add(proposalId)
        _proposals.value = _proposals.value.map { if (it.id == proposalId) resolved else it }
        cloudSync?.pushProposalReviewed(resolved)
        ioScope.launch { runCatching { dao.upsertProposal(resolved.toEntity()) } }

        pushNotification(
            title = "Proposal Approved: ${service.title}",
            message = "${proposal.proposalType.label} approved by $adminName. ${resolved.adminReviewNote}",
            targetRole = UserRole.STAFF,
            centreId = proposal.centreId,
            severity = NotificationSeverity.APPROVED
        )
        appendAuditLog(
            action = "PROPOSAL_APPROVED",
            details = "${proposal.proposalType.label} on ${service.title} approved by $adminName",
            role = UserRole.ADMIN,
            actorName = adminName,
            centreId = proposal.centreId
        )
        syncWidgetsFromCurrentState()
        return Result.success(resolved)
    }

    @Synchronized
    override fun rejectProposal(
        proposalId: String,
        adminName: String,
        reviewNote: String
    ): Result<ServiceChangeProposal> {
        val proposal = _proposals.value.find { it.id == proposalId }
            ?: return Result.failure(IllegalArgumentException("Proposal not found"))
        if (proposal.status != ProposalStatus.PENDING) {
            return Result.failure(IllegalStateException("Proposal already reviewed"))
        }

        val resolved = proposal.copy(
            status = ProposalStatus.REJECTED,
            reviewedAt = System.currentTimeMillis(),
            reviewedByAdminName = adminName,
            adminReviewNote = reviewNote.trim().ifBlank { "Needs policy verification before publishing." }
        )
        _proposals.value = _proposals.value.map { if (it.id == proposalId) resolved else it }
        cloudSync?.pushProposalReviewed(resolved)
        ioScope.launch { runCatching { dao.upsertProposal(resolved.toEntity()) } }

        pushNotification(
            title = "Proposal Returned: ${proposal.serviceTitle}",
            message = "${proposal.proposalType.label} rejected by $adminName: ${resolved.adminReviewNote}",
            targetRole = UserRole.STAFF,
            centreId = proposal.centreId,
            severity = NotificationSeverity.ALERT
        )
        appendAuditLog(
            action = "PROPOSAL_REJECTED",
            details = "${proposal.proposalType.label} on ${proposal.serviceTitle} rejected by $adminName",
            role = UserRole.ADMIN,
            actorName = adminName,
            centreId = proposal.centreId
        )
        syncWidgetsFromCurrentState()
        return Result.success(resolved)
    }

    // ==========================================
    // ADMIN STAFF MANAGEMENT
    // ==========================================

    @Synchronized
    override fun createOrUpdateStaffAccount(
        existingId: String?,
        name: String,
        email: String,
        phone: String,
        assignedCentreId: String,
        assignedCounterId: String,
        department: String,
        pin: String,
        isActive: Boolean,
        adminName: String
    ): Result<UserAccount> {
        val cleanName = name.trim()
        val cleanEmail = email.trim().lowercase()
        val cleanPin = pin.trim()
        if (cleanName.length < 2) {
            return Result.failure(IllegalArgumentException("Officer name must be at least 2 characters"))
        }
        if (!cleanEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Enter a valid government email ID"))
        }
        if (cleanPin.length < 4) {
            return Result.failure(IllegalArgumentException("Desk PIN must be at least 4 digits"))
        }
        val centre = _centres.value.find { it.id == assignedCentreId }
            ?: return Result.failure(IllegalArgumentException("Assigned service centre not found"))
        val counter = _counters.value.find { it.id == assignedCounterId && it.centreId == assignedCentreId }
            ?: return Result.failure(IllegalArgumentException("Assigned counter does not belong to ${centre.name}"))

        val accountId = existingId?.takeIf { it.isNotBlank() } ?: "usr_staff_${UUID.randomUUID().toString().take(6)}"
        val duplicateEmail = _staffAccounts.value.find {
            it.email.equals(cleanEmail, ignoreCase = true) && it.id != accountId
        }
        if (duplicateEmail != null) {
            return Result.failure(IllegalArgumentException("Email $cleanEmail is already registered to ${duplicateEmail.name}"))
        }

        val updatedStaff = UserAccount(
            id = accountId,
            name = cleanName,
            email = cleanEmail,
            phone = phone.trim().ifBlank { "+91 9811009900" },
            role = UserRole.STAFF,
            assignedCentreId = assignedCentreId,
            assignedCounterId = assignedCounterId,
            pin = cleanPin,
            department = department.trim().ifBlank { "Front Desk Operations" },
            isLoggedIn = false,
            isActive = isActive
        )

        val exists = _staffAccounts.value.any { it.id == accountId }
        _staffAccounts.value = if (exists) {
            _staffAccounts.value.map { if (it.id == accountId) updatedStaff else it }
        } else {
            listOf(updatedStaff) + _staffAccounts.value
        }

        // Reflect assigned staff officer on the corresponding counter
        _counters.value = _counters.value.map {
            if (it.id == counter.id) it.copy(assignedStaffName = cleanName) else it
        }

        mutatedStaffIds.add(updatedStaff.id)
        cloudSync?.pushStaffProfileUpserted(updatedStaff, !exists)
        ioScope.launch { runCatching { dao.insertAccount(updatedStaff.toEntity()) } }
        appendAuditLog(
            action = if (exists) "STAFF_UPDATED" else "STAFF_CREATED",
            details = "${updatedStaff.name} assigned to ${centre.name} (Counter ${counter.counterNumber})",
            role = UserRole.ADMIN,
            actorName = adminName,
            centreId = centre.id
        )
        syncWidgetsFromCurrentState()
        return Result.success(updatedStaff)
    }

    @Synchronized
    override fun toggleStaffAccountActive(staffId: String, adminName: String): Result<UserAccount> {
        val staff = _staffAccounts.value.find { it.id == staffId }
            ?: return Result.failure(IllegalArgumentException("Staff profile not found"))
        val updated = staff.copy(isActive = !staff.isActive)
        _staffAccounts.value = _staffAccounts.value.map { if (it.id == staffId) updated else it }
        cloudSync?.pushStaffProfileUpserted(updated, false)
        ioScope.launch { runCatching { dao.insertAccount(updated.toEntity()) } }
        appendAuditLog(
            action = if (updated.isActive) "STAFF_ACTIVATED" else "STAFF_SUSPENDED",
            details = "${updated.name} portal access set to ${if (updated.isActive) "Active" else "Suspended"}",
            role = UserRole.ADMIN,
            actorName = adminName,
            centreId = updated.assignedCentreId
        )
        return Result.success(updated)
    }

    // ==========================================
    // ADMIN SERVICE CENTRE MANAGEMENT
    // ==========================================

    @Synchronized
    override fun createOrUpdateServiceCentre(
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
        liveNotice: String?,
        adminName: String
    ): Result<ServiceCentre> {
        val cleanName = name.trim()
        if (cleanName.length < 3) {
            return Result.failure(IllegalArgumentException("Centre name must be at least 3 characters"))
        }
        val cleanAddress = address.trim()
        if (cleanAddress.length < 5) {
            return Result.failure(IllegalArgumentException("Valid centre address is required"))
        }
        val safeTotalCounters = totalCountersCount.coerceIn(1, 20)
        val centreId = existingId?.takeIf { it.isNotBlank() } ?: "centre_${UUID.randomUUID().toString().take(6)}"
        val existingCentre = _centres.value.find { it.id == centreId }

        // Ensure counter list matches requested total counters
        val currentCountersForCentre = _counters.value.filter { it.centreId == centreId }.sortedBy { it.counterNumber }
        val newCounters = mutableListOf<Counter>()
        for (idx in 1..safeTotalCounters) {
            val found = currentCountersForCentre.find { it.counterNumber == idx }
            if (found != null) {
                newCounters.add(found.copy(handledServiceIds = availableServiceIds))
            } else {
                newCounters.add(
                    Counter(
                        id = "cnt_${centreId.takeLast(5)}_$idx",
                        centreId = centreId,
                        counterNumber = idx,
                        name = "Counter $idx - General Service Desk",
                        assignedStaffName = "Unassigned Officer",
                        isActive = true,
                        handledServiceIds = availableServiceIds
                    )
                )
            }
        }
        _counters.value = _counters.value.filterNot { it.centreId == centreId } + newCounters
        val activeCounters = newCounters.count { it.isActive }

        val updatedCentre = ServiceCentre(
            id = centreId,
            name = cleanName,
            category = category,
            address = cleanAddress,
            latitude = existingCentre?.latitude ?: 28.6139,
            longitude = existingCentre?.longitude ?: 77.2090,
            distanceKm = existingCentre?.distanceKm ?: 2.4,
            operatingHours = operatingHours.trim().ifBlank { "09:00 AM – 05:30 PM (Mon–Fri)" },
            phone = phone.trim().ifBlank { "+1 (800) 200-4400" },
            activeCountersCount = activeCounters,
            totalCountersCount = safeTotalCounters,
            currentCrowdLevel = existingCentre?.currentCrowdLevel ?: CrowdLevel.LOW,
            estimatedWaitMin = existingCentre?.estimatedWaitMin ?: 6,
            estimatedWaitMax = existingCentre?.estimatedWaitMax ?: 12,
            lastCrowdUpdateMinutesAgo = 1,
            crowdConfidencePercent = existingCentre?.crowdConfidencePercent ?: 95,
            bestTimeToVisit = bestTimeToVisit.trim().ifBlank { "10:30 AM – 12:30 PM" },
            isVirtualQueueOpen = isVirtualQueueOpen,
            availableServiceIds = availableServiceIds,
            rating = existingCentre?.rating ?: 4.7,
            totalReviewsCount = existingCentre?.totalReviewsCount ?: 1,
            liveNotice = liveNotice?.trim()?.ifBlank { null }
        )

        _centres.value = if (existingCentre != null) {
            _centres.value.map { if (it.id == centreId) updatedCentre else it }
        } else {
            listOf(updatedCentre) + _centres.value
        }
        cloudSync?.pushCentreUpserted(updatedCentre, existingCentre == null)

        appendAuditLog(
            action = if (existingCentre != null) "CENTRE_UPDATED" else "CENTRE_CREATED",
            details = "${updatedCentre.name} configured with $safeTotalCounters counters",
            role = UserRole.ADMIN,
            actorName = adminName,
            centreId = centreId
        )
        syncWidgetsFromCurrentState()
        return Result.success(updatedCentre)
    }

    override fun toggleCentreVirtualQueue(centreId: String, adminName: String): Result<ServiceCentre> {
        val centre = _centres.value.find { it.id == centreId }
            ?: return Result.failure(IllegalArgumentException("Centre not found"))
        val updated = centre.copy(isVirtualQueueOpen = !centre.isVirtualQueueOpen)
        _centres.value = _centres.value.map { if (it.id == centreId) updated else it }
        cloudSync?.pushCentreUpserted(updated, false)
        appendAuditLog(
            action = if (updated.isVirtualQueueOpen) "QUEUE_OPENED" else "QUEUE_PAUSED",
            details = "Virtual queue ${if (updated.isVirtualQueueOpen) "opened" else "paused"} for ${centre.name}",
            role = UserRole.ADMIN,
            actorName = adminName,
            centreId = centreId
        )
        syncWidgetsFromCurrentState()
        return Result.success(updated)
    }

    // ==========================================
    // ADMIN SYSTEM SETTINGS & ANALYTICS
    // ==========================================

    override fun updateSystemSettings(settings: SystemSettings, adminName: String): Result<SystemSettings> {
        if (settings.autoNoShowTimeoutMinutes !in 5..60) {
            return Result.failure(IllegalArgumentException("No-show timeout must be between 5 and 60 minutes"))
        }
        if (settings.maxActiveTokensPerCitizen !in 1..5) {
            return Result.failure(IllegalArgumentException("Max active tokens must be between 1 and 5"))
        }
        val cleanSettings = settings.copy(
            id = "global",
            maintenanceBanner = settings.maintenanceBanner.trim().ifBlank {
                "Scheduled e-Governance maintenance window active."
            },
            updatedAt = System.currentTimeMillis(),
            updatedByAdmin = adminName
        )
        settingsMutatedInSession = true
        _systemSettings.value = cleanSettings
        cloudSync?.pushSystemSettings(cleanSettings)
        ioScope.launch {
            runCatching {
                dao.upsertSystemSettings(
                    SystemSettingsEntity(
                        id = cleanSettings.id,
                        autoNoShowTimeoutMinutes = cleanSettings.autoNoShowTimeoutMinutes,
                        maxActiveTokensPerCitizen = cleanSettings.maxActiveTokensPerCitizen,
                        crowdDecayHalfLifeMinutes = cleanSettings.crowdDecayHalfLifeMinutes,
                        walkInBufferPerHour = cleanSettings.walkInBufferPerHour,
                        maintenanceMode = cleanSettings.maintenanceMode,
                        maintenanceBanner = cleanSettings.maintenanceBanner,
                        updatedAt = cleanSettings.updatedAt,
                        updatedByAdmin = cleanSettings.updatedByAdmin
                    )
                )
            }
        }
        appendAuditLog(
            action = "SYSTEM_SETTINGS_UPDATED",
            details = "NoShow=${cleanSettings.autoNoShowTimeoutMinutes}m, MaxTokens=${cleanSettings.maxActiveTokensPerCitizen}, Maintenance=${cleanSettings.maintenanceMode}",
            role = UserRole.ADMIN,
            actorName = adminName
        )
        syncWidgetsFromCurrentState()
        return Result.success(cleanSettings)
    }

    override fun computeQueueStatistics(centreIdFilter: String?): QueueStatisticsSnapshot {
        val scopedTokens = if (centreIdFilter.isNullOrBlank()) {
            _tokens.value
        } else {
            _tokens.value.filter { it.centreId == centreIdFilter }
        }
        val scopedCounters = if (centreIdFilter.isNullOrBlank()) {
            _counters.value
        } else {
            _counters.value.filter { it.centreId == centreIdFilter }
        }
        val waiting = scopedTokens.count { it.status == TokenStatus.WAITING }
        val activeServing = scopedTokens.count { it.status == TokenStatus.CALLED || it.status == TokenStatus.SERVING }
        val completed = scopedTokens.count { it.status == TokenStatus.COMPLETED }
        val noShow = scopedTokens.count { it.status == TokenStatus.NO_SHOW }
        val cancelled = scopedTokens.count { it.status == TokenStatus.CANCELLED }

        val terminalTotal = (completed + noShow).coerceAtLeast(1)
        val completionRate = if (completed + noShow == 0) 92 else (completed * 100) / terminalTotal

        val waitValues = scopedTokens.filter { it.status == TokenStatus.WAITING }.map { it.estimatedWaitMin }
        val avgWait = if (waitValues.isNotEmpty()) waitValues.average().toInt() else {
            val filteredCentres = if (centreIdFilter.isNullOrBlank()) _centres.value else _centres.value.filter { it.id == centreIdFilter }
            filteredCentres.map { it.estimatedWaitMin }.average().toInt().coerceAtLeast(6)
        }

        // Synthesize realistic hourly trends blended with real live volume
        val liveBoost = scopedTokens.size
        val trends = listOf(
            HourlyTrendPoint("09 AM", 14 + (liveBoost / 3), 8),
            HourlyTrendPoint("11 AM", 26 + liveBoost, 16),
            HourlyTrendPoint("01 PM", 19 + (liveBoost / 2), 12),
            HourlyTrendPoint("03 PM", 24 + liveBoost, 15),
            HourlyTrendPoint("05 PM", 11 + (waiting * 2), avgWait)
        )

        return QueueStatisticsSnapshot(
            totalTokensToday = scopedTokens.size + 42,
            waitingCount = waiting,
            activeServingCount = activeServing,
            completedCount = completed + 34,
            noShowCount = noShow + 3,
            cancelledCount = cancelled + 2,
            completionRatePercent = completionRate,
            avgWaitMinutes = avgWait,
            activeCountersCount = scopedCounters.count { it.isActive },
            totalCountersCount = scopedCounters.size,
            pendingProposalsCount = _proposals.value.count { it.status == ProposalStatus.PENDING },
            hourlyTrends = trends
        )
    }

    // ==========================================
    // SEED DATA INITIALIZATION
    // ==========================================

    private fun seedDefaults() {
        val docPassportOld = RequiredDocument("doc_old_pass", "Original Expiring Passport", "Current passport with self-attested copies of first and last two pages.")
        val docAadhaar = RequiredDocument("doc_aadhaar", "National ID / Aadhaar Card", "Original card with biometric verification QR code.", acceptableAlternatives = listOf("Voter ID", "Passport"))
        val docAddressProof = RequiredDocument("doc_addr", "Proof of Present Address", "Water/Electricity bill or registered rental agreement (issued within last 3 months).")
        val docBirthProof = RequiredDocument("doc_birth", "Date of Birth Proof", "Birth certificate or secondary school leaving certificate.")
        val docMedicalForm = RequiredDocument("doc_med_form", "Medical Fitness Certificate", "Form 1-A signed by a registered medical practitioner.")
        val docRegCert = RequiredDocument("doc_rc_book", "Original Vehicle Registration Certificate (RC)", "Original RC Smart card with valid pollution certificate.")

        _services.value = listOf(
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

        _centres.value = listOf(
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

        _counters.value = listOf(
            Counter("cnt_psk_1", "centre_central_psk", 1, "Counter 1 - Token Verification & Intake", "Officer Rajesh Kumar", true, null, listOf("srv_passport_renew", "srv_aadhaar_update")),
            Counter("cnt_psk_2", "centre_central_psk", 2, "Counter 2 - Biometrics & Photo Capture", "Officer David Kim", true, "C-040", listOf("srv_passport_renew", "srv_aadhaar_update")),
            Counter("cnt_psk_3", "centre_central_psk", 3, "Counter 3 - Document Scrutiny Desk", "Officer Robert Taylor", true, "C-041", listOf("srv_passport_renew")),
            Counter("cnt_psk_4", "centre_central_psk", 4, "Counter 4 - Granting Officer Clearance", "Officer Elena Rostova", true, "C-038", listOf("srv_passport_renew")),
            Counter("cnt_rto_1", "centre_metro_rto", 1, "Counter 1 - License Verification", "Officer Michael Chang", true, "T-019", listOf("srv_dl_renew")),
            Counter("cnt_rto_2", "centre_metro_rto", 2, "Counter 2 - Vehicle RC Processing", "Officer Anita Roy", true, "T-020", listOf("srv_vehicle_rc")),
            Counter("cnt_rto_3", "centre_metro_rto", 3, "Counter 3 - Biometric Signature Capture", "Officer James Wilson", true, "T-021", listOf("srv_dl_renew", "srv_vehicle_rc"))
        )

        _staffAccounts.value = listOf(
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
                isLoggedIn = false,
                isActive = true
            ),
            UserAccount(
                id = "usr_staff_2",
                name = "Officer David Kim",
                email = "david.kim@passport.gov.in",
                phone = "+91 9811009942",
                role = UserRole.STAFF,
                assignedCentreId = "centre_central_psk",
                assignedCounterId = "cnt_psk_2",
                pin = "1234",
                department = "Biometrics & Photo Verification",
                isLoggedIn = false,
                isActive = true
            ),
            UserAccount(
                id = "usr_staff_3",
                name = "Officer Michael Chang",
                email = "michael.chang@rto.gov.in",
                phone = "+91 9811009943",
                role = UserRole.STAFF,
                assignedCentreId = "centre_metro_rto",
                assignedCounterId = "cnt_rto_1",
                pin = "1234",
                department = "Regional Transport Licensing Desk",
                isLoggedIn = false,
                isActive = true
            )
        )

        _tokens.value = listOf(
            QueueToken("tkn_psk_041", "C-041", "centre_central_psk", "Central Passport Seva Kendra (PSK)", "srv_passport_renew", "Passport Renewal & Re-Issue", "usr_101", "Marcus Aurelius", "+15550101", TokenStatus.SERVING, 3, "Counter 3 - Document Scrutiny Desk", 0, 0, 0, 3),
            QueueToken("tkn_psk_042", "C-042", "centre_central_psk", "Central Passport Seva Kendra (PSK)", "srv_passport_renew", "Passport Renewal & Re-Issue", "usr_client_me", "Dr. Ananya Sharma", "+91 9876543210", TokenStatus.WAITING, null, null, 1, 0, 3, 6),
            QueueToken("tkn_psk_043", "C-043", "centre_central_psk", "Central Passport Seva Kendra (PSK)", "srv_passport_renew", "Passport Renewal & Re-Issue", "usr_103", "Priya Sharma", "+91 9811223344", TokenStatus.WAITING, null, null, 2, 1, 8, 14),
            QueueToken("tkn_psk_044", "C-044", "centre_central_psk", "Central Passport Seva Kendra (PSK)", "srv_aadhaar_update", "National ID / Aadhaar Biometric Update", "usr_104", "Daniel Craig", "+91 9822334455", TokenStatus.WAITING, null, null, 3, 2, 14, 20),
            QueueToken("tkn_psk_045", "C-045", "centre_central_psk", "Central Passport Seva Kendra (PSK)", "srv_passport_renew", "Passport Renewal & Re-Issue", "usr_105", "Emma Watson", "+91 9833445566", TokenStatus.WAITING, null, null, 4, 3, 19, 26),
            QueueToken("tkn_psk_039", "C-039", "centre_central_psk", "Central Passport Seva Kendra (PSK)", "srv_passport_renew", "Passport Renewal & Re-Issue", "usr_100", "Rohan Verma", "+91 9800112233", TokenStatus.COMPLETED, 1, "Counter 1 - Token Verification & Intake", 0, 0, 0, 0)
        )

        _proposals.value = listOf(
            ServiceChangeProposal(
                id = "prop_seed_1",
                proposalType = ProposalType.SERVICE_INFO,
                serviceId = "srv_passport_renew",
                serviceTitle = "Passport Renewal & Re-Issue",
                centreId = "centre_central_psk",
                submittedByStaffId = "usr_staff_1",
                submittedByStaffName = "Officer Rajesh Kumar",
                assignedCounterNumber = 1,
                rationale = "Average intake duration dropped to 15 min after new 4-finger scanner upgrade.",
                status = ProposalStatus.PENDING,
                submittedAt = System.currentTimeMillis() - 18 * 60 * 1000,
                previousFee = "$45.00 (36 Pages) / $60.00 (60 Pages)",
                proposedFee = "$45.00 (36 Pages) / $60.00 (60 Pages)",
                previousDurationMin = 18,
                proposedDurationMin = 15,
                previousDescription = "Standard 36/60-page booklet renewal for adult citizens under normal scheme.",
                proposedDescription = "Standard 36/60-page booklet renewal with fast-track 4-fingerprint biometric capture.",
                previousEligibility = "Any adult citizen with an expired or expiring (within 1 year) passport.",
                proposedEligibility = "Any adult citizen with an expired or expiring (within 1 year) passport.",
                previousCautionNote = "All photocopies must be self-attested with date. Keep original documents organized.",
                proposedCautionNote = "Bring original expiring passport and self-attested copies with signature & date."
            ),
            ServiceChangeProposal(
                id = "prop_seed_2",
                proposalType = ProposalType.REQUIRED_DOCUMENT_UPDATE,
                serviceId = "srv_dl_renew",
                serviceTitle = "Driving License Renewal & Endorsement",
                centreId = "centre_metro_rto",
                submittedByStaffId = "usr_staff_3",
                submittedByStaffName = "Officer Michael Chang",
                assignedCounterNumber = 1,
                rationale = "Clarify valid duration for Medical Form 1-A as per latest RTO directive.",
                status = ProposalStatus.PENDING,
                submittedAt = System.currentTimeMillis() - 42 * 60 * 1000,
                documentId = "doc_med_form",
                previousDocName = "Medical Fitness Certificate",
                proposedDocName = "Medical Fitness Certificate (Form 1-A)",
                previousDocDescription = "Form 1-A signed by a registered medical practitioner.",
                proposedDocDescription = "Form 1-A signed and stamped within 90 days by a registered medical practitioner.",
                previousDocMandatory = true,
                proposedDocMandatory = true,
                previousDocAlternatives = emptyList(),
                proposedDocAlternatives = listOf("Digital Parivahan Medical Certificate")
            )
        )

        _notifications.value = listOf(
            StaffNotification(
                id = "notif_seed_1",
                title = "4 Citizens Waiting at Central PSK",
                message = "Token C-042 (Dr. Ananya Sharma) is next in queue for Counter 1.",
                targetRole = UserRole.STAFF,
                centreId = "centre_central_psk",
                counterId = "cnt_psk_1",
                severity = NotificationSeverity.ACTION_REQUIRED,
                isRead = false,
                timestamp = System.currentTimeMillis() - 3 * 60 * 1000,
                relatedTokenNumber = "C-042"
            ),
            StaffNotification(
                id = "notif_seed_2",
                title = "Biometric Upgrade Active",
                message = "Admin notice: High-speed 4-fingerprint scanner live on Counter 1 & 2.",
                targetRole = UserRole.STAFF,
                centreId = "centre_central_psk",
                severity = NotificationSeverity.INFO,
                isRead = false,
                timestamp = System.currentTimeMillis() - 25 * 60 * 1000
            ),
            StaffNotification(
                id = "notif_seed_3",
                title = "2 Staff Proposals Awaiting Review",
                message = "Service duration and RTO document edits pending Admin verification.",
                targetRole = UserRole.ADMIN,
                severity = NotificationSeverity.ACTION_REQUIRED,
                isRead = false,
                timestamp = System.currentTimeMillis() - 18 * 60 * 1000
            )
        )

        _systemSettings.value = SystemSettings()

        _crowdReports.value = listOf(
            CrowdReport("rep_1", "centre_central_psk", "Central Passport Seva Kendra (PSK)", CrowdLevel.MODERATE, 18, 16, "Counters 1-4 moving steadily. Biometrics desk is fast.", "Alex M.", System.currentTimeMillis() - 3 * 60 * 1000, 7, true, 100),
            CrowdReport("rep_2", "centre_metro_rto", "Metropolitan Regional Transport Office (RTO)", CrowdLevel.HIGH, 36, 32, "Very crowded at RC transfer counter. License desk is moderate.", "Vikram S.", System.currentTimeMillis() - 8 * 60 * 1000, 12, false, 88),
            CrowdReport("rep_3", "centre_apex_hospital", "Apex City General Hospital & OPD", CrowdLevel.MODERATE, 22, 12, "OPD registration has 8 desks active, waiting time under 15 mins.", "Dr. Clara P.", System.currentTimeMillis() - 2 * 60 * 1000, 19, true, 100)
        )

        _reviews.value = listOf(
            CentreReview("rev_1", "centre_central_psk", "Central Passport Seva Kendra (PSK)", "usr_101", "Marcus Aurelius", 5, "Remarkably organized! Verified all my self-attested documents at Counter 1 and was called to biometrics in under 8 minutes.", "Passport Renewal", "Fast", System.currentTimeMillis() - 45 * 60 * 1000, 14),
            CentreReview("rev_2", "centre_central_psk", "Central Passport Seva Kendra (PSK)", "usr_102", "Priya Sharma", 5, "Digital token tracking was spot on. Left my house when token was 2 ahead and walked straight to Counter 3.", "Aadhaar Update", "Fast", System.currentTimeMillis() - 2 * 3600 * 1000, 9),
            CentreReview("rev_3", "centre_metro_rto", "Metropolitan Regional Transport Office (RTO)", "usr_103", "Karan Singh", 4, "License renewal was fast, but carry cash for the smart card envelope fee. Officer Chang was very helpful.", "DL Renewal", "Normal", System.currentTimeMillis() - 5 * 3600 * 1000, 6),
            CentreReview("rev_4", "centre_apex_hospital", "Apex City General Hospital & OPD", "usr_104", "Dr. Clara P.", 5, "Triage queue was handled with empathy. Senior citizens get instant token bump.", "OPD Checkup", "Fast", System.currentTimeMillis() - 8 * 3600 * 1000, 19)
        )

        _liveQueueUpdates.value = listOf(
            LiveQueueStatusUpdate("lup_1", "centre_central_psk", "Central Passport Seva Kendra (PSK)", "Citizen Alex", "FAST_MOVING", "Token C-039 just completed. Counter 3 calling tokens every 3 mins!", System.currentTimeMillis() - 2 * 60 * 1000, "C-040 (Serving)", 8),
            LiveQueueStatusUpdate("lup_2", "centre_central_psk", "Central Passport Seva Kendra (PSK)", "Officer Rajesh", "DOCS_ACTIVE", "Document pre-check desk is clear. Please have originals ready before Counter 2.", System.currentTimeMillis() - 6 * 60 * 1000, "C-038 (Serving)", 15),
            LiveQueueStatusUpdate("lup_3", "centre_metro_rto", "Metropolitan Regional Transport Office (RTO)", "Vikram S.", "SLIGHT_DELAY", "RC inspection line has 6 cars waiting. Driving test track is running smoothly.", System.currentTimeMillis() - 14 * 60 * 1000, "T-019 (Serving)", 4)
        )

        _auditLogs.value = listOf(
            AuditLog(
                id = "log_boot",
                timestamp = System.currentTimeMillis(),
                action = "SYSTEM_BOOT",
                actorName = "System Administrator",
                actorRole = UserRole.ADMIN,
                details = "QEase Core Intelligence Engine initialized with 5 centres, 6 services, Staff/Admin APIs & Widget sync"
            )
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: LocalQEaseApiEngine? = null

        fun getInstance(context: Context): LocalQEaseApiEngine {
            val appCtx = context.applicationContext
            val existing = INSTANCE
            if (existing != null && existing.appContext === appCtx) {
                return existing
            }
            return synchronized(this) {
                val current = INSTANCE
                if (current != null && current.appContext === appCtx) {
                    current
                } else {
                    LocalQEaseApiEngine(appCtx).also { INSTANCE = it }
                }
            }
        }
    }
}

private fun ServiceChangeProposal.toEntity(): ServiceChangeProposalEntity = ServiceChangeProposalEntity(
    id = id,
    proposalType = proposalType.name,
    serviceId = serviceId,
    serviceTitle = serviceTitle,
    centreId = centreId,
    submittedByStaffId = submittedByStaffId,
    submittedByStaffName = submittedByStaffName,
    assignedCounterNumber = assignedCounterNumber,
    rationale = rationale,
    status = status.name,
    submittedAt = submittedAt,
    reviewedAt = reviewedAt,
    reviewedByAdminName = reviewedByAdminName,
    adminReviewNote = adminReviewNote,
    previousFee = previousFee,
    proposedFee = proposedFee,
    previousDurationMin = previousDurationMin,
    proposedDurationMin = proposedDurationMin,
    previousDescription = previousDescription,
    proposedDescription = proposedDescription,
    previousEligibility = previousEligibility,
    proposedEligibility = proposedEligibility,
    previousCautionNote = previousCautionNote,
    proposedCautionNote = proposedCautionNote,
    documentId = documentId,
    previousDocName = previousDocName,
    proposedDocName = proposedDocName,
    previousDocDescription = previousDocDescription,
    proposedDocDescription = proposedDocDescription,
    previousDocMandatory = previousDocMandatory,
    proposedDocMandatory = proposedDocMandatory,
    previousDocAlternativesJoined = previousDocAlternatives.joinToString("|"),
    proposedDocAlternativesJoined = proposedDocAlternatives.joinToString("|")
)

private fun ServiceChangeProposalEntity.toDomain(): ServiceChangeProposal = ServiceChangeProposal(
    id = id,
    proposalType = ProposalType.entries.firstOrNull { it.name == proposalType } ?: ProposalType.SERVICE_INFO,
    serviceId = serviceId,
    serviceTitle = serviceTitle,
    centreId = centreId,
    submittedByStaffId = submittedByStaffId,
    submittedByStaffName = submittedByStaffName,
    assignedCounterNumber = assignedCounterNumber,
    rationale = rationale,
    status = ProposalStatus.entries.firstOrNull { it.name == status } ?: ProposalStatus.PENDING,
    submittedAt = submittedAt,
    reviewedAt = reviewedAt,
    reviewedByAdminName = reviewedByAdminName,
    adminReviewNote = adminReviewNote,
    previousFee = previousFee,
    proposedFee = proposedFee,
    previousDurationMin = previousDurationMin,
    proposedDurationMin = proposedDurationMin,
    previousDescription = previousDescription,
    proposedDescription = proposedDescription,
    previousEligibility = previousEligibility,
    proposedEligibility = proposedEligibility,
    previousCautionNote = previousCautionNote,
    proposedCautionNote = proposedCautionNote,
    documentId = documentId,
    previousDocName = previousDocName,
    proposedDocName = proposedDocName,
    previousDocDescription = previousDocDescription,
    proposedDocDescription = proposedDocDescription,
    previousDocMandatory = previousDocMandatory,
    proposedDocMandatory = proposedDocMandatory,
    previousDocAlternatives = previousDocAlternativesJoined.split("|").filter { it.isNotBlank() },
    proposedDocAlternatives = proposedDocAlternativesJoined.split("|").filter { it.isNotBlank() }
)

private fun StaffNotification.toEntity(): StaffNotificationEntity = StaffNotificationEntity(
    id = id,
    title = title,
    message = message,
    targetRole = targetRole.name,
    centreId = centreId,
    counterId = counterId,
    severity = severity.name,
    isRead = isRead,
    timestamp = timestamp,
    relatedTokenNumber = relatedTokenNumber
)

private fun StaffNotificationEntity.toDomain(): StaffNotification = StaffNotification(
    id = id,
    title = title,
    message = message,
    targetRole = UserRole.entries.firstOrNull { it.name == targetRole } ?: UserRole.STAFF,
    centreId = centreId,
    counterId = counterId,
    severity = NotificationSeverity.entries.firstOrNull { it.name == severity } ?: NotificationSeverity.INFO,
    isRead = isRead,
    timestamp = timestamp,
    relatedTokenNumber = relatedTokenNumber
)

private fun UserAccount.toEntity(): UserAccountEntity = UserAccountEntity(
    id = id,
    name = name,
    email = email,
    phone = phone,
    role = role.name,
    assignedCentreId = assignedCentreId,
    assignedCounterId = assignedCounterId,
    pin = pin,
    department = department,
    isActive = isActive
)
